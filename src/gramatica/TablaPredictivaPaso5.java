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
import java.util.ArrayList;
import javafx.scene.control.TableRow;
import javafx.scene.input.MouseEvent;

/**
 * Versión extendida de TablaPredictiva específica para el paso 5,
 * que incluye filas para terminales con fondo rojo para las funciones de error.
 */
public class TablaPredictivaPaso5 extends TablaPredictiva {

    private PanelNuevaSimDescPaso5 panelPaso5;
    private List<FuncionError> funcionesError;

    public TablaPredictivaPaso5() {
        super();
        this.funcionesError = new ArrayList<>();
    }

    public TablaPredictivaPaso5(TableView<FilaTablaPredictiva> tabla) {
        super(tabla);
        this.funcionesError = new ArrayList<>();
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

        // Primero añadimos las filas de no terminales
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

        // Luego añadimos las filas de terminales
        for (Terminal t : gramatica.getTerminales()) {
            FilaTablaPredictiva fila = new FilaTablaPredictiva(t.getNombre(), "", true);
            filas.add(fila);
        }

        getTablaPredictiva().setItems(filas);

        // Aplicar el estilo a las celdas según si son terminales o no
        for (TableColumn<FilaTablaPredictiva, ?> column : getTablaPredictiva().getColumns()) {
            if (column.getText().equals("No Terminal")) continue;

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

                    // Verificar si esta celda está seleccionada
                    boolean isSelected = getTableRow().getIndex() == getTablaPredictiva().getFocusModel().getFocusedCell().getRow() 
                                      && getTableColumn() == getTablaPredictiva().getFocusModel().getFocusedCell().getTableColumn();

                    StringBuilder style = new StringBuilder();

                    // Aplicar colores de fondo según el tipo de celda
                    if (fila.getEsTerminal()) {
                        Terminal terminalFila = gramatica.getTerminales().stream()
                            .filter(t -> t.getNombre().equals(fila.getSimbolo()))
                            .findFirst()
                            .orElse(null);

                        if (terminalFila != null && apareceSoloPrimeraPos(terminalFila)) {
                            style.append("-fx-background-color: #ffcccc;"); // Color rojo claro
                        }
                    } else if (item != null && item.startsWith("ε_")) {
                        style.append("-fx-background-color: #e6f3ff;"); // Color azul claro para épsilon
                        setText(item.substring(2));
                    }

                    // Si la celda está seleccionada, añadir borde azul
                    if (isSelected) {
                        if (style.length() > 0) {
                            style.append("; ");
                        }
                        style.append("-fx-border-color: #0096c9; -fx-border-width: 1px;");
                    }

                    setStyle(style.toString());

                    // Hacer la celda no editable si contiene una producción
                    if (item != null && !item.isEmpty() && Character.isDigit(item.charAt(0))) {
                        setEditable(false);
                    } else {
                        setEditable(true);
                    }
                }
            });
        }

        // Configurar el comportamiento de selección
        getTablaPredictiva().getSelectionModel().setCellSelectionEnabled(true);
        getTablaPredictiva().getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        
        // Desactivar la selección de filas completas
        getTablaPredictiva().setRowFactory(tv -> {
            TableRow<FilaTablaPredictiva> row = new TableRow<>();
            row.setStyle("-fx-selection-bar: transparent; -fx-selection-bar-non-focused: transparent;");
            return row;
        });

        // Añadir manejador de clics para la tabla
        getTablaPredictiva().setOnMouseClicked(this::handleTableClick);

        getTablaPredictiva().refresh();
    }

    @Override
    public void construir(Gramatica gramatica) {
        // Guardar las funciones de error antes de construir
        List<FuncionError> funcionesErrorExistentes = this.funcionesError;
        
        // Construir la tabla
        super.construir(gramatica);
        
        // Restaurar las funciones de error después de construir
        this.funcionesError = funcionesErrorExistentes;
        
        // Cargar los datos específicos del paso 5
        cargarDatos();
    }

    @Override
    public List<FuncionError> getFuncionesError() {
        return this.funcionesError;
    }

    @Override
    public void setFuncionesError(List<FuncionError> funcionesError) {
        this.funcionesError = new ArrayList<>(funcionesError);
    }

    private void handleTableClick(MouseEvent event) {
        if (event.getClickCount() == 1) {
            TablePosition<FilaTablaPredictiva, ?> pos = getTablaPredictiva().getFocusModel().getFocusedCell();
            if (pos != null && pos.getColumn() > 0) { // Ignorar la columna "No Terminal"
                FilaTablaPredictiva fila = getTablaPredictiva().getItems().get(pos.getRow());
                String columna = getTablaPredictiva().getColumns().get(pos.getColumn()).getText();
                
                // Verificar primero si es una celda de terminal que aparece solo en primera posición
                if (fila.getEsTerminal()) {
                    Terminal terminalFila = gramatica.getTerminales().stream()
                        .filter(t -> t.getNombre().equals(fila.getSimbolo()))
                        .findFirst()
                        .orElse(null);

                    if (terminalFila != null && apareceSoloPrimeraPos(terminalFila)) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("Celda no válida");
                        alert.setContentText("No se puede añadir una función de error en una celda de terminal que aparece solo en primera posición.");
                        alert.showAndWait();
                        return;
                    }
                }

                String funcionErrorSeleccionada = panelPaso5.getFuncionErrorSeleccionada();
                if (funcionErrorSeleccionada != null) {
                    // Verificar si la celda contiene una producción (empieza por un número)
                    String valorCelda = fila.getValor(columna).get();
                    if (valorCelda != null && !valorCelda.isEmpty() && Character.isDigit(valorCelda.charAt(0))) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("Celda no válida");
                        alert.setContentText("No se puede añadir una función de error en una celda que contiene una producción.");
                        alert.showAndWait();
                        return;
                    }

                    // Extraer el número de la función de error
                    String numeroFuncion = funcionErrorSeleccionada.substring(0, funcionErrorSeleccionada.indexOf(" "));
                    
                    // Añadir o actualizar la función de error
                    fila.setValor(columna, "E" + numeroFuncion);
                    getTablaPredictiva().refresh();
                }
            }
        }
    }
} 