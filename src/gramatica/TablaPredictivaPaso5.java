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
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.util.Callback;
import java.util.ResourceBundle;

/**
 * Versión extendida de TablaPredictiva específica para el paso 5,
 * que incluye filas para terminales y manejo de funciones de error.
 */
public class TablaPredictivaPaso5 extends TablaPredictiva {

    private PanelNuevaSimDescPaso5 panelPaso5;
    private List<FuncionError> funcionesError;
    private boolean columnsCreated = false;

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
                    // Buscar la producción épsilon para este no terminal
                    Produccion prodEpsilon = gramatica.getProducciones().stream()
                        .filter(p -> p.getAntec().getSimboloNT().getNombre().equals(nt.getNombre()) && 
                                   (p.getConsec().isEmpty() || 
                                    (p.getConsec().size() == 1 && p.getConsec().get(0).getNombre().equals("ε"))))
                        .findFirst()
                        .orElse(null);

                    if (prodEpsilon != null) {
                        // Recorrer todas las columnas de terminales
                        for (TableColumn<FilaTablaPredictiva, ?> column : getTablaPredictiva().getColumns()) {
                            String columnName = column.getText();
                            if (!columnName.equals("Símbolo")) {  // Ignorar la columna de símbolos
                                String valorCelda = fila.getValor(columnName).get();
                                // Solo rellenar si la celda está vacía y no tiene producción
                                if ((valorCelda == null || valorCelda.isEmpty()) && 
                                    !gramatica.getProduccionesPorNoTerminalYTerminal(nt, 
                                        gramatica.getTerminales().stream()
                                            .filter(t -> t.getNombre().equals(columnName))
                                            .findFirst()
                                            .orElse(null)).isEmpty()) {
                                    fila.setValor(columnName, "ε_" + prodEpsilon.toString());
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
    public void construir(Gramatica gramatica) {
        super.construir(gramatica);
        
        // Verificar si las columnas se crearon correctamente
        if (getTablaPredictiva().getColumns().isEmpty()) {
            // Forzar la creación de columnas
            crearColumnas(gramatica);
        }
    }
    
    private void crearColumnas(Gramatica gramatica) {
        // Columna para símbolos
        TableColumn<FilaTablaPredictiva, String> colSimbolo = new TableColumn<>("Símbolo");
        colSimbolo.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getSimbolo()));
        colSimbolo.setPrefWidth(100);
        
        // Limpiar columnas existentes
        getTablaPredictiva().getColumns().clear();
        
        // Añadir la columna de símbolos
        getTablaPredictiva().getColumns().add(colSimbolo);
        
        // Añadir columnas para cada terminal
        for (Terminal t : gramatica.getTerminales()) {
            if (t.getNombre() == null || t.getNombre().isEmpty()) continue;
            
            TableColumn<FilaTablaPredictiva, String> colT = new TableColumn<>(t.getNombre());
            colT.setPrefWidth(100);
            
            colT.setCellValueFactory(cellData -> 
                cellData.getValue().getValor(t.getNombre()));
            
            getTablaPredictiva().getColumns().add(colT);
        }
        
        // Añadir columna para $
        TableColumn<FilaTablaPredictiva, String> colDolar = new TableColumn<>("$");
        colDolar.setPrefWidth(100);
        colDolar.setCellValueFactory(cellData -> 
            cellData.getValue().getValor("$"));
        getTablaPredictiva().getColumns().add(colDolar);
        
        columnsCreated = true;
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
        
        // Asegurar que tenemos columnas
        if (!columnsCreated && gramatica != null) {
            crearColumnas(gramatica);
        }
        
        configurarColumnas();
        configurarSeleccion();
        configurarManejadorClics();
    }

    private void configurarColumnas() {
        // Cambiar el título de la primera columna
        getTablaPredictiva().getColumns().get(0).setText("Símbolo");

        // Configurar el estilo base de la tabla
        getTablaPredictiva().setStyle("-fx-background-color: white; -fx-table-cell-border-color: black;");

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
                            StringBuilder textStyle = new StringBuilder();

                            // Color base para la columna y fila de símbolos
                            if (getTableColumn().getText().equals("Símbolo") || fila.getSimbolo().equals(getTableColumn().getText())) {
                                style.append("-fx-background-color: #F8F9FA;"); // Gris muy claro
                                textStyle.append("-fx-text-fill: black;");
                            }

                            // Color para celdas de terminales que solo aparecen en primera posición
                            if (fila.getEsTerminal()) {
                                Terminal terminalFila = gramatica.getTerminales().stream()
                                    .filter(t -> t.getNombre().equals(fila.getSimbolo()))
                                    .findFirst()
                                    .orElse(null);

                                if (terminalFila != null && apareceSoloPrimeraPos(terminalFila)) {
                                    if (!getTableColumn().getText().equals("Símbolo")) {
                                        style.append("-fx-background-color: #E9ECEF;"); // Gris más oscuro
                                        textStyle.append("-fx-text-fill: black;");
                                    }
                                }
                            }

                            // Estilo para épsilon
                            if (item != null && item.startsWith("ε_")) {
                                style.setLength(0);
                                style.append("-fx-background-color: white;");
                                textStyle.append("-fx-text-fill: #0D47A1;"); // Azul oscuro para épsilon
                            }

                            // Estilo para producciones (no editables)
                            if (item != null && !item.isEmpty() && Character.isDigit(item.charAt(0))) {
                                style.setLength(0);
                                style.append("-fx-background-color: white;");
                                textStyle.append("-fx-text-fill: black;");
                            }

                            // Estilo para funciones de error
                            if (item != null && !item.isEmpty() && item.startsWith("E")) {
                                style.setLength(0);
                                style.append("-fx-background-color: white;");
                                textStyle.append("-fx-text-fill: #1976D2;"); // Azul para funciones de error
                            }

                            // Estilo para celda seleccionada
                            if (isCellSelected()) {
                                style.append("; -fx-border-color: #1976D2; -fx-border-width: 2px;");
                                // Asegurar que el texto sea siempre visible cuando la celda está seleccionada
                                textStyle.setLength(0);
                                textStyle.append("-fx-text-fill: black; -fx-font-weight: bold;");
                            }

                            // Aplicar estilos
                            setStyle(style.toString() + "; " + textStyle.toString() + "; -fx-font-weight: bold;");
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
        
        getTablaPredictiva().setRowFactory(tv -> new TableRow<>());
    }

    private void configurarManejadorClics() {
        getTablaPredictiva().setOnMouseReleased(event -> {
            TableView.TableViewSelectionModel<FilaTablaPredictiva> selectionModel = getTablaPredictiva().getSelectionModel();
            ObservableList<TablePosition> selectedCells = selectionModel.getSelectedCells();
            if (selectedCells.isEmpty()) return;
            TablePosition pos = selectedCells.get(0);

            if (pos != null && pos.getColumn() > 0) {
                FilaTablaPredictiva fila = getTablaPredictiva().getItems().get(pos.getRow());
                String columna = getTablaPredictiva().getColumns().get(pos.getColumn()).getText();
                String valorCelda = fila.getValor(columna).get();

                // No permitir modificar celdas con producciones (contienen '→')
                if (valorCelda != null && valorCelda.contains("→")) {
                    mostrarError("produccion",
                               "Esta celda contiene una producción de la gramática",
                               "Las producciones son necesarias para el análisis sintáctico y no pueden ser modificadas.");
                    return;
                }

                // No permitir funciones de error en terminales de primera posición
                if (fila.getEsTerminal()) {
                    Terminal terminalFila = gramatica.getTerminales().stream()
                        .filter(t -> t.getNombre().equals(fila.getSimbolo()))
                        .findFirst()
                        .orElse(null);

                    if (terminalFila != null && apareceSoloPrimeraPos(terminalFila)) {
                        mostrarError("invalida",
                                   "No se permiten funciones de error",
                                   "Este terminal solo aparece en primera posición de las producciones.");
                        return;
                    }
                }

                // Permitir sobrescribir cualquier celda que no sea producción
                FuncionError funcionErrorSeleccionada = panelPaso5.getFuncionErrorSeleccionada();
                if (funcionErrorSeleccionada != null) {
                    String textoFuncion = "E" + funcionErrorSeleccionada.getIdentificador();
                    fila.setValor(columna, textoFuncion);
                    getTablaPredictiva().refresh();
                }
            }
        });
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
        ResourceBundle bundle = panelPaso5 != null ? panelPaso5.getBundle() : null;
        Alert alert = new Alert(Alert.AlertType.ERROR);
        if (bundle != null) {
            alert.setTitle(bundle.getString("simulador.paso5.alert.error"));
            if ("produccion".equals(titulo)) {
                alert.setHeaderText(bundle.getString("simulador.paso5.alert.celda.produccion.header"));
                alert.setContentText(bundle.getString("simulador.paso5.alert.celda.produccion.content"));
            } else if ("invalida".equals(titulo)) {
                alert.setHeaderText(bundle.getString("simulador.paso5.alert.celda.invalida.header"));
                alert.setContentText(bundle.getString("simulador.paso5.alert.celda.invalida.content"));
            } else {
                alert.setHeaderText(header);
                alert.setContentText(contenido);
            }
        } else {
            alert.setTitle(titulo);
            alert.setHeaderText(header);
            alert.setContentText(contenido);
        }
        alert.showAndWait();
    }

    /**
     * Obtiene la gramática asociada a esta tabla predictiva.
     * @return La gramática asociada.
     */
    public Gramatica getGramatica() {
        return this.gramatica;
    }

    /**
     * Establece la tabla UI que se utilizará para mostrar los datos.
     * @param tabla La tabla UI a utilizar.
     */
    public void setTablaPredictiva(TableView<FilaTablaPredictiva> tabla) {
        // En lugar de intentar modificar el campo privado directamente,
        // transferimos los datos entre las tablas
        
        // Si hay datos en nuestra tabla actual, los transferimos a la nueva tabla
        if (getTablaPredictiva() != null && getTablaPredictiva().getItems() != null) {
            ObservableList<FilaTablaPredictiva> items = getTablaPredictiva().getItems();
            tabla.setItems(items);
        }
        
        // Asegurar que la nueva tabla tenga columnas
        if (tabla.getColumns().isEmpty() && this.gramatica != null) {
            // Columna para símbolos
            TableColumn<FilaTablaPredictiva, String> colSimbolo = new TableColumn<>("Símbolo");
            colSimbolo.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getSimbolo()));
            colSimbolo.setPrefWidth(100);
            
            // Añadir la columna de símbolos
            tabla.getColumns().add(colSimbolo);
            
            // Añadir columnas para cada terminal
            for (Terminal t : gramatica.getTerminales()) {
                if (t.getNombre() == null || t.getNombre().isEmpty()) continue;
                
                TableColumn<FilaTablaPredictiva, String> colT = new TableColumn<>(t.getNombre());
                colT.setPrefWidth(100);
                
                colT.setCellValueFactory(cellData -> 
                    cellData.getValue().getValor(t.getNombre()));
                
                tabla.getColumns().add(colT);
            }
            
            // Añadir columna para $
            TableColumn<FilaTablaPredictiva, String> colDolar = new TableColumn<>("$");
            colDolar.setPrefWidth(100);
            colDolar.setCellValueFactory(cellData -> 
                cellData.getValue().getValor("$"));
            tabla.getColumns().add(colDolar);
        }
    }
} 