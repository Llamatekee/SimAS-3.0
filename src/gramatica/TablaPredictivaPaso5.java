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
import javafx.util.Callback;

/**
 * Versión extendida de TablaPredictiva específica para el paso 5,
 * que incluye filas para terminales y manejo de funciones de error.
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
        
        for (Produccion p : gramatica.getProducciones()) {
            List<Simbolo> consecuente = p.getConsec();
            if (consecuente.isEmpty()) continue;
            
            if (consecuente.get(0).getNombre().equals(terminal.getNombre())) {
                apareceEnPrimera = true;
            }
            
            for (int i = 1; i < consecuente.size(); i++) {
                if (consecuente.get(i).getNombre().equals(terminal.getNombre())) {
                    return false;
                }
            }
        }
        
        return apareceEnPrimera;
    }

    public void rellenarProduccionesEpsilon() {
        for (FilaTablaPredictiva fila : getTablaPredictiva().getItems()) {
            if (!fila.getEsTerminal()) {
                NoTerminal nt = gramatica.getNoTerminales().stream()
                    .filter(n -> n.getNombre().equals(fila.getSimbolo()))
                    .findFirst()
                    .orElse(null);

                if (nt != null) {
                    boolean tieneEpsilon = gramatica.getProducciones().stream()
                        .anyMatch(p -> p.getAntec().getSimboloNT().getNombre().equals(nt.getNombre()) && 
                                     (p.getConsec().isEmpty() || 
                                      (p.getConsec().size() == 1 && p.getConsec().get(0).getNombre().equals("ε"))));

                    if (tieneEpsilon) {
                        for (Terminal t : gramatica.getTerminales()) {
                            String valorCelda = fila.getValor(t.getNombre()).get();
                            if (valorCelda == null || valorCelda.isEmpty()) {
                                Produccion prodEpsilon = gramatica.getProducciones().stream()
                                    .filter(p -> p.getAntec().getSimboloNT().getNombre().equals(nt.getNombre()) && 
                                                (p.getConsec().isEmpty() || 
                                                 (p.getConsec().size() == 1 && p.getConsec().get(0).getNombre().equals("ε"))))
                                    .findFirst()
                                    .orElse(null);
                                
                                if (prodEpsilon != null) {
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

        // Cargar no terminales con sus producciones
        for (NoTerminal nt : gramatica.getNoTerminales()) {
            FilaTablaPredictiva fila = new FilaTablaPredictiva(nt.getNombre(), "", false);
            for (Terminal t : gramatica.getTerminales()) {
                List<String> producciones = gramatica.getProduccionesPorNoTerminalYTerminal(nt, t);
                if (!producciones.isEmpty()) {
                    fila.setValor(t.getNombre(), producciones.get(0));
                }
            }
            filas.add(fila);
        }

        // Cargar terminales
        for (Terminal t : gramatica.getTerminales()) {
            FilaTablaPredictiva fila = new FilaTablaPredictiva(t.getNombre(), "", true);
            filas.add(fila);
        }

        getTablaPredictiva().setItems(filas);
        configurarColumnas();
        configurarSeleccion();
        configurarManejadorClics();
    }

    private void configurarColumnas() {
        for (TableColumn<FilaTablaPredictiva, ?> column : getTablaPredictiva().getColumns()) {
            if (column instanceof TableColumn) {
                @SuppressWarnings("unchecked")
                TableColumn<FilaTablaPredictiva, String> stringColumn = (TableColumn<FilaTablaPredictiva, String>) column;

                stringColumn.setCellFactory(column1 -> {
                    TableCell<FilaTablaPredictiva, String> cell = new TableCell<>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            
                            if (empty) {
                                setText(null);
                                setStyle("");
                                return;
                            }

                            // Configurar el texto
                            if (item != null) {
                                if (item.startsWith("ε_")) {
                                    setText(item.substring(2));
                                } else {
                                    setText(item);
                                }
                            } else {
                                setText("");
                            }

                            // Configurar el estilo
                            FilaTablaPredictiva fila = getTableRow().getItem();
                            if (fila == null) return;

                            StringBuilder style = new StringBuilder();

                            // Estilo para terminales en primera posición
                            if (fila.getEsTerminal()) {
                                Terminal terminalFila = gramatica.getTerminales().stream()
                                    .filter(t -> t.getNombre().equals(fila.getSimbolo()))
                                    .findFirst()
                                    .orElse(null);

                                if (terminalFila != null && apareceSoloPrimeraPos(terminalFila)) {
                                    style.append("-fx-background-color: #ffcccc;"); // Rojo claro
                                }
                            }

                            // Estilo para épsilon
                            if (item != null && item.startsWith("ε_")) {
                                if (style.length() > 0) style.append("; ");
                                style.append("-fx-background-color: #e6f3ff;"); // Azul claro
                            }

                            // Estilo para producciones (no editables)
                            if (item != null && !item.isEmpty() && Character.isDigit(item.charAt(0))) {
                                if (style.length() > 0) style.append("; ");
                                style.append("-fx-background-color: #f0f0f0;"); // Gris claro
                                style.append("; -fx-opacity: 0.9;");
                            }

                            // Estilo para celda seleccionada
                            if (isCellSelected()) {
                                if (style.length() > 0) style.append("; ");
                                style.append("-fx-border-color: #0096c9; -fx-border-width: 1px;");
                            }

                            setStyle(style.toString());
                        }

                        private boolean isCellSelected() {
                            return getTableRow().getIndex() == getTablaPredictiva().getFocusModel().getFocusedCell().getRow() 
                                   && getTableColumn() == getTablaPredictiva().getFocusModel().getFocusedCell().getTableColumn();
                        }
                    };
                    return cell;
                });
            }
        }
    }

    private void configurarSeleccion() {
        getTablaPredictiva().getSelectionModel().setCellSelectionEnabled(true);
        getTablaPredictiva().getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        
        getTablaPredictiva().setRowFactory(tv -> {
            TableRow<FilaTablaPredictiva> row = new TableRow<>();
            row.setStyle("-fx-selection-bar: transparent; -fx-selection-bar-non-focused: transparent;");
            return row;
        });
    }

    private void configurarManejadorClics() {
        getTablaPredictiva().addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            System.out.println("[DEBUG] Clic detectado en la tabla");
            TablePosition<FilaTablaPredictiva, ?> pos = getTablaPredictiva().getFocusModel().getFocusedCell();
            if (pos != null && pos.getColumn() > 0) {
                FilaTablaPredictiva fila = getTablaPredictiva().getItems().get(pos.getRow());
                String columna = getTablaPredictiva().getColumns().get(pos.getColumn()).getText();
                String valorCelda = fila.getValor(columna).get();

                System.out.println("[DEBUG] Celda seleccionada - Fila: " + fila.getSimbolo() + 
                                 ", Columna: " + columna + 
                                 ", Valor: " + valorCelda);

                // No permitir modificar celdas con producciones
                if (valorCelda != null && !valorCelda.isEmpty() && Character.isDigit(valorCelda.charAt(0))) {
                    System.out.println("[DEBUG] Intento de modificar celda con producción");
                    mostrarError("No se puede modificar esta celda",
                               "Esta celda contiene una producción de la gramática",
                               "Las producciones son necesarias para el análisis sintáctico y no pueden ser modificadas.");
                    event.consume();
                    return;
                }

                // No permitir funciones de error en terminales de primera posición
                if (fila.getEsTerminal()) {
                    Terminal terminalFila = gramatica.getTerminales().stream()
                        .filter(t -> t.getNombre().equals(fila.getSimbolo()))
                        .findFirst()
                        .orElse(null);

                    if (terminalFila != null && apareceSoloPrimeraPos(terminalFila)) {
                        System.out.println("[DEBUG] Intento de modificar terminal de primera posición");
                        mostrarError("No se permiten funciones de error",
                                   "No se pueden añadir funciones de error para este terminal",
                                   "Este terminal solo aparece en primera posición de las producciones.");
                        event.consume();
                        return;
                    }
                }

                // Procesar función de error seleccionada
                String funcionErrorSeleccionada = panelPaso5.getFuncionErrorSeleccionada();
                if (funcionErrorSeleccionada != null) {
                    System.out.println("[DEBUG] Aplicando función de error: " + funcionErrorSeleccionada);
                    String numeroFuncion = funcionErrorSeleccionada.substring(0, funcionErrorSeleccionada.indexOf(" "));
                    fila.setValor(columna, "E" + numeroFuncion);
                    getTablaPredictiva().refresh();
                } else {
                    System.out.println("[DEBUG] No hay función de error seleccionada");
                }
            }
        });
    }

    @Override
    public void construir(Gramatica gramatica) {
        List<FuncionError> funcionesErrorExistentes = this.funcionesError;
        super.construir(gramatica);
        this.funcionesError = funcionesErrorExistentes;
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

    private void mostrarError(String titulo, String header, String contenido) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(header);
        alert.setContentText(contenido);
        alert.showAndWait();
    }
} 