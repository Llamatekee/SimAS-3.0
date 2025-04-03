package gramatica;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import java.util.List;
import simulador.PanelNuevaSimDescPaso5;
import javafx.scene.control.TablePosition;
import javafx.scene.control.SelectionMode;

/**
 * Versión extendida de TablaPredictiva específica para el paso 5,
 * que incluye filas para terminales con fondo rojo para las funciones de error.
 */
public class TablaPredictivaPaso5 extends TablaPredictiva {

    private PanelNuevaSimDescPaso5 panelPaso5;

    public TablaPredictivaPaso5() {
        super();
    }

    public TablaPredictivaPaso5(TableView<FilaTablaPredictiva> tabla) {
        super(tabla);
    }

    public void setPanelPaso5(PanelNuevaSimDescPaso5 panel) {
        this.panelPaso5 = panel;
    }

    private boolean apareceSoloPrimeraPos(Terminal terminal) {
        boolean apareceEnPrimera = false;
        
        // Verificar todas las apariciones del terminal
        for (Produccion p : gramatica.getProducciones()) {
            List<Simbolo> consecuente = p.getConsec();
            if (consecuente.isEmpty()) continue;
            
            // Verificar primera posición
            if (consecuente.get(0).getNombre().equals(terminal.getNombre())) {
                apareceEnPrimera = true;
            }
            
            // Verificar otras posiciones
            for (int i = 1; i < consecuente.size(); i++) {
                if (consecuente.get(i).getNombre().equals(terminal.getNombre())) {
                    return false; // Si aparece en otra posición, no cumple la condición
                }
            }
        }
        
        return apareceEnPrimera; // Solo retorna true si aparece en primera posición y nunca en otras
    }

    public void rellenarProduccionesEpsilon() {
        for (FilaTablaPredictiva fila : getTablaPredictiva().getItems()) {
            if (!fila.getEsTerminal()) {
                // Buscar el no terminal correspondiente
                NoTerminal nt = gramatica.getNoTerminales().stream()
                    .filter(n -> n.getNombre().equals(fila.getSimbolo()))
                    .findFirst()
                    .orElse(null);

                if (nt != null) {
                    // Verificar si el no terminal tiene una producción épsilon
                    boolean tieneEpsilon = gramatica.getProducciones().stream()
                        .anyMatch(p -> p.getAntec().getSimboloNT().getNombre().equals(nt.getNombre()) && 
                                     (p.getConsec().isEmpty() || 
                                      (p.getConsec().size() == 1 && p.getConsec().get(0).getNombre().equals("ε"))));

                    if (tieneEpsilon) {
                        // Para cada terminal en la fila
                        for (Terminal t : gramatica.getTerminales()) {
                            String valorCelda = fila.getValor(t.getNombre()).get();
                            // Solo rellenar si la celda está vacía y no tiene una función de error
                            if (valorCelda == null || valorCelda.isEmpty() || valorCelda.startsWith("E")) {
                                // Buscar la producción épsilon específica
                                Produccion prodEpsilon = gramatica.getProducciones().stream()
                                    .filter(p -> p.getAntec().getSimboloNT().getNombre().equals(nt.getNombre()) && 
                                                (p.getConsec().isEmpty() || 
                                                 (p.getConsec().size() == 1 && p.getConsec().get(0).getNombre().equals("ε"))))
                                    .findFirst()
                                    .orElse(null);
                                
                                if (prodEpsilon != null) {
                                    // Añadir un prefijo especial para identificar que es una producción épsilon añadida
                                    fila.setValor(t.getNombre(), "ε_" + prodEpsilon.toString());
                                }
                            }
                        }
                    }
                }
            }
        }
        getTablaPredictiva().refresh();
    }

    @Override
    protected void cargarDatos() {
        ObservableList<FilaTablaPredictiva> filas = FXCollections.observableArrayList();

        // Primero añadimos las filas de no terminales (igual que en la clase padre)
        for (NoTerminal nt : gramatica.getNoTerminales()) {
            FilaTablaPredictiva fila = new FilaTablaPredictiva(nt.getNombre(), "", false);

            for (Terminal t : gramatica.getTerminales()) {
                List<String> producciones = gramatica.getProduccionesPorNoTerminalYTerminal(nt, t);

                if (!producciones.isEmpty()) {
                    String primeraProduccion = producciones.get(0);
                    fila.setValor(t.getNombre(), primeraProduccion);
                }
            }

            filas.add(fila);
        }

        // Luego añadimos las filas de terminales (específico del paso 5)
        for (Terminal t : gramatica.getTerminales()) {
            FilaTablaPredictiva fila = new FilaTablaPredictiva(t.getNombre(), "", true);
            filas.add(fila);
        }

        getTablaPredictiva().setItems(filas);

        // Aplicar el estilo a las celdas según si son terminales o no
        for (TableColumn<FilaTablaPredictiva, ?> column : getTablaPredictiva().getColumns()) {
            if (column.getText().equals("No Terminal")) continue; // Saltamos la primera columna

            @SuppressWarnings("unchecked")
            TableColumn<FilaTablaPredictiva, String> stringColumn = (TableColumn<FilaTablaPredictiva, String>) column;

            stringColumn.setCellFactory(col -> new TableCell<FilaTablaPredictiva, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setText(null);
                        setStyle("");
                        return;
                    }

                    setText(item);
                    FilaTablaPredictiva fila = getTableRow().getItem();
                    if (fila == null) return;

                    if (fila.getEsTerminal()) {
                        // Buscar el terminal correspondiente a esta fila
                        Terminal terminalFila = gramatica.getTerminales().stream()
                            .filter(t -> t.getNombre().equals(fila.getSimbolo()))
                            .findFirst()
                            .orElse(null);

                        if (terminalFila != null && apareceSoloPrimeraPos(terminalFila)) {
                            setStyle("-fx-background-color: #ffcccc;"); // Color rojo claro
                        } else {
                            setStyle(""); // Celda normal
                        }
                    } else {
                        // Verificar si la celda tiene una función de error
                        if (item != null && item.startsWith("E")) {
                            setStyle("-fx-background-color: #e0e0e0;"); // Color gris claro
                        } else if (item != null && item.startsWith("ε_")) {
                            setStyle("-fx-background-color: #e6f3ff;"); // Color azul claro para producciones épsilon añadidas
                            setText(item.substring(2)); // Quitar el prefijo ε_ al mostrar
                        } else {
                            setStyle(""); // Celda normal
                        }
                    }
                }
            });

            // Añadir manejador de clics para las celdas
            stringColumn.setOnEditCommit(event -> {
                FilaTablaPredictiva fila = event.getRowValue();
                String columna = event.getTableColumn().getText();
                String valor = event.getNewValue();

                if (valor != null && valor.startsWith("E")) {
                    // Si es una función de error, mantener el estilo gris
                    event.getTableView().refresh();
                }
            });
        }

        // Añadir manejador de clics para la tabla
        getTablaPredictiva().setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                TablePosition<FilaTablaPredictiva, ?> pos = getTablaPredictiva().getFocusModel().getFocusedCell();
                if (pos != null && pos.getColumn() > 0) { // Ignorar la columna "No Terminal"
                    FilaTablaPredictiva fila = getTablaPredictiva().getItems().get(pos.getRow());
                    String columna = getTablaPredictiva().getColumns().get(pos.getColumn()).getText();
                    
                    String funcionErrorSeleccionada = panelPaso5.getFuncionErrorSeleccionada();
                    if (funcionErrorSeleccionada != null) {
                        // Extraer el número de la función de error
                        String numeroFuncion = funcionErrorSeleccionada.substring(0, funcionErrorSeleccionada.indexOf("."));
                        String valorCelda = fila.getValor(columna).get();

                        // Verificar si es una celda de terminal
                        if (fila.getEsTerminal()) {
                            Terminal terminalFila = gramatica.getTerminales().stream()
                                .filter(t -> t.getNombre().equals(fila.getSimbolo()))
                                .findFirst()
                                .orElse(null);

                            if (terminalFila != null && apareceSoloPrimeraPos(terminalFila)) {
                                // Mostrar alerta de error para celdas rojas
                                Alert alert = new Alert(Alert.AlertType.ERROR);
                                alert.setTitle("Error");
                                alert.setHeaderText("Celda no válida");
                                alert.setContentText("No se puede añadir una función de error en una celda de terminal que aparece solo en primera posición.");
                                alert.showAndWait();
                                return;
                            }
                        }
                        
                        // Verificar si la celda tiene una producción (no es válida para función de error)
                        if (valorCelda != null && !valorCelda.isEmpty() && !valorCelda.startsWith("E") && !valorCelda.startsWith("ε_")) {
                            // Mostrar alerta de error
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Error");
                            alert.setHeaderText("Celda no válida");
                            alert.setContentText("No se puede añadir una función de error en una celda que ya tiene una producción.");
                            alert.showAndWait();
                        } else {
                            // Añadir o actualizar la función de error
                            fila.setValor(columna, "E" + numeroFuncion);
                            getTablaPredictiva().refresh();
                        }
                    }
                }
            }
        });

        // Desactivar la selección de filas
        getTablaPredictiva().getSelectionModel().clearSelection();
        getTablaPredictiva().getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        getTablaPredictiva().refresh();
    }
} 