package simulador;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.TableCell;
import javafx.geometry.Point2D;

import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;

import gramatica.FilaTablaPredictiva;
import gramatica.FuncionError;
import gramatica.Gramatica;
import gramatica.TablaPredictivaPaso5;
import gramatica.Terminal;
import editor.ActualizableTextos;

public class PanelNuevaSimDescPaso5 implements PanelNuevaSimDescPaso, ActualizableTextos {

    @FXML private Label labelTitulo;
    @FXML private Label labelSeleccioneFuncion;
    @FXML private Button buttonAnterior;
    @FXML private Button buttonSiguiente;
    @FXML private Button buttonCancelar;
    @FXML private Button buttonEliminar;
    @FXML private Button buttonRellenar;
    @FXML private Button buttonGramatica;
    @FXML private Button buttonPrimero;
    @FXML private Button buttonSimulacion;
    @FXML private Button buttonRellenarEpsilon;
    @FXML private Button buttonResetearTabla;
    @FXML private ComboBox<FuncionError> comboBoxFuncionesError;
    @FXML private TableView<FilaTablaPredictiva> tablaPredictiva;
    @FXML private TableColumn<FilaTablaPredictiva, String> columnSimbolo;
    @FXML private TableColumn<FilaTablaPredictiva, String> columnAccion;

    private Parent root;
    private PanelSimuladorDesc panelPadre;
    private Gramatica gramatica;
    private ResourceBundle bundle;

    public PanelNuevaSimDescPaso5(PanelSimuladorDesc panelPadre) {
        this.panelPadre = panelPadre;
        this.gramatica = panelPadre.gramatica;
        this.bundle = panelPadre.getBundle();
        cargarFXML();
    }

    private void cargarFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/PanelNuevaSimDescPaso5.fxml"));
            loader.setController(this);
            loader.setResources(bundle);
            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Parent getRoot() {
        return root;
    }
    
    /**
     * Configura el manejador de eventos para desmarcar la selección cuando se hace clic fuera de la tabla
     */
    private void configurarManejadorClicFueraTabla() {
        // Usar Platform.runLater para asegurar que root esté disponible
        javafx.application.Platform.runLater(() -> {
            if (root != null && root.getScene() != null) {
                // Configurar el manejador en la escena completa para capturar todos los clics
                root.getScene().setOnMouseClicked(event -> {
                    // Obtener las coordenadas del clic en la escena
                    double sceneX = event.getSceneX();
                    double sceneY = event.getSceneY();
                    
                    // Convertir las coordenadas de la escena a coordenadas locales de la tabla
                    Point2D localPoint = tablaPredictiva.sceneToLocal(sceneX, sceneY);
                    
                    // Verificar si el clic fue dentro de la tabla
                    if (localPoint == null || 
                        localPoint.getX() < 0 || localPoint.getX() > tablaPredictiva.getWidth() ||
                        localPoint.getY() < 0 || localPoint.getY() > tablaPredictiva.getHeight()) {
                        // El clic fue fuera de la tabla, desmarcar la selección
                        tablaPredictiva.getSelectionModel().clearSelection();
                    }
                });
            }
        });
    }

    @FXML
    private void initialize() {
        // Deshabilitar el botón Siguiente en el paso 5
        buttonSiguiente.setDisable(true);
        
        // Configurar el botón de simulación
        buttonSimulacion.setOnAction(e -> iniciarSimulacion());

        // Verificar si hay una tabla predictiva básica en la gramática
        if (gramatica.getTPredictiva() == null) {
            return;
        }

        // Verificar las funciones de error
        List<FuncionError> funcionesError = gramatica.getTPredictiva().getFuncionesError();
        if (funcionesError == null || funcionesError.isEmpty()) {
            return;
        }

        // Construir la tabla predictiva usando la global o creando una nueva
        construirTablaPredictiva();

        // Llenar el ComboBox con las funciones de error
        actualizarComboBoxFuncionesError();
        
        // Configurar handlers para cada cambio en la tabla
        tablaPredictiva.setOnMouseClicked(event -> {
            // Guardar al hacer clic para capturar cambios inmediatamente
            if (event.getClickCount() == 2) {
                guardarTablaEnGlobal();
            }
        });

        if (buttonRellenarEpsilon != null) {
            buttonRellenarEpsilon.setOnAction(e -> handleRellenarEpsilon());
        }
        actualizarTextos(bundle);
        
        // Configurar manejador para desmarcar selección cuando se hace clic fuera de la tabla
        configurarManejadorClicFueraTabla();
    }

    private void iniciarSimulacion() {
        // Guardar la tabla local en la global antes de pasar al paso 6
        guardarTablaEnGlobal();
        
        // Avanzar al paso 6
        panelPadre.cambiarPaso(5);
    }

    private void construirTablaPredictiva() {
        if (gramatica.getProducciones().get(0).getNumero() == 0) {
            gramatica.numerarProducciones();
        }
        
        // Verificar si ya existe una tabla predictiva extendida global
        TablaPredictivaPaso5 tablaGlobal = panelPadre.getTablaPredictivaExtendidaGlobal();
        
        if (tablaGlobal == null) {            
            // Crear una nueva tabla predictiva extendida
            tablaGlobal = new TablaPredictivaPaso5(tablaPredictiva);
            tablaGlobal.setPanelPaso5(this);
            
            // Usar las funciones de error de la tabla básica
            tablaGlobal.setFuncionesError(gramatica.getTPredictiva().getFuncionesError());
            
            // Construir la tabla
            tablaGlobal.construir(gramatica);
            
            // Guardar la instancia en el panel padre
            panelPadre.setTablaPredictivaExtendidaGlobal(tablaGlobal);
            
        } else {
            // Si la tabla global ya tiene datos, asegurarnos de usarlos en la tabla local
            if (tablaGlobal.getTablaPredictiva().getItems() != null && 
                !tablaGlobal.getTablaPredictiva().getItems().isEmpty()) {
                
                // Limpiar la tabla local
                tablaPredictiva.getColumns().clear();
                
                // Crear las columnas en la tabla local
                crearColumnasEnTabla(tablaPredictiva);
                
                // Copiar los datos de la tabla global a la local
                tablaPredictiva.setItems(tablaGlobal.getTablaPredictiva().getItems());
                
            } else {
                // Crear una nueva tabla con la misma instancia UI
                TablaPredictivaPaso5 nuevaTabla = new TablaPredictivaPaso5(tablaPredictiva);
                nuevaTabla.setPanelPaso5(this);
                
                // Preservar funciones de error
                nuevaTabla.setFuncionesError(tablaGlobal.getFuncionesError());
                
                // Construir tabla
                nuevaTabla.construir(gramatica);
                
                // Reemplazar la tabla global
                panelPadre.setTablaPredictivaExtendidaGlobal(nuevaTabla);                
            }
        }
        
        // Asegurarnos de que las columnas estén bien
        if (tablaPredictiva.getColumns().isEmpty()) {
            crearColumnasEnTabla(tablaPredictiva);
        }
        
        // Configurar propiedades visuales de la tabla
        tablaPredictiva.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tablaPredictiva.setTableMenuButtonVisible(false);
        tablaPredictiva.setEditable(true);
        
        // Permitir scroll horizontal para ver todas las columnas
        tablaPredictiva.setPrefWidth(800);
        tablaPredictiva.setMinWidth(600);
        
        // Permitir que las columnas se redimensionen para mejor visualización
        for (TableColumn<FilaTablaPredictiva, ?> col : tablaPredictiva.getColumns()) {
            col.setResizable(true);
            col.setMinWidth(80);
        }
        
        // Forzar un refresh
        tablaPredictiva.refresh();
        
        // Mostrar información sobre las celdas para diagnóstico
        if (tablaPredictiva.getItems() != null && !tablaPredictiva.getItems().isEmpty()) {
            FilaTablaPredictiva primeraFila = tablaPredictiva.getItems().get(0);
            if (primeraFila != null) {
                for (TableColumn<FilaTablaPredictiva, ?> col : tablaPredictiva.getColumns()) {
                    if (col.getText().equals("Símbolo")) continue;                    
                }
            }
        }

        actualizarTablaPredictiva();
    }

    /**
     * Guarda la tabla local en la tabla global.
     * Este método debe llamarse antes de salir del paso 5.
     */
    private void guardarTablaEnGlobal() {
        TablaPredictivaPaso5 tablaGlobal = panelPadre.getTablaPredictivaExtendidaGlobal();
        
        if (tablaGlobal != null && tablaPredictiva.getItems() != null) {            
            try {
                // 1. Asegurar que la tabla global tenga columnas
                if (tablaGlobal.getTablaPredictiva().getColumns().isEmpty()) {
                    crearColumnasEnTabla(tablaGlobal.getTablaPredictiva());
                }
                
                // 2. Iterar por todas las filas y columnas para copiar los valores manualmente
                // Esto asegura que todos los valores se transfieran, incluyendo las funciones de error
                ObservableList<FilaTablaPredictiva> filasLocales = tablaPredictiva.getItems();
                ObservableList<FilaTablaPredictiva> filasGlobales = tablaGlobal.getTablaPredictiva().getItems();
                
                // Si no hay filas en la tabla global, copiar directamente
                if (filasGlobales == null || filasGlobales.isEmpty()) {
                    // Crear una copia directa de los elementos para evitar problemas de referencia
                    tablaGlobal.getTablaPredictiva().setItems(FXCollections.observableArrayList(filasLocales));
                } else {
                    // Copiar celda por celda para preservar los valores modificados
                    // Obtener todas las columnas excepto la primera (que es "Símbolo")
                    List<TableColumn<FilaTablaPredictiva, ?>> columnas = tablaPredictiva.getColumns();
                    
                    for (int i = 0; i < filasLocales.size() && i < filasGlobales.size(); i++) {
                        FilaTablaPredictiva filaLocal = filasLocales.get(i);
                        FilaTablaPredictiva filaGlobal = filasGlobales.get(i);
                        
                        // Verificar que se está copiando la misma fila (mismo símbolo)
                        if (filaLocal.getSimbolo().equals(filaGlobal.getSimbolo())) {
                            // Copiar cada celda de la fila
                            for (int j = 1; j < columnas.size(); j++) {
                                String nombreColumna = columnas.get(j).getText();
                                String valorLocal = filaLocal.getValor(nombreColumna).get();
                                
                                // Solo copiar si hay un valor
                                if (valorLocal != null && !valorLocal.isEmpty()) {
                                    filaGlobal.setValor(nombreColumna, valorLocal);
                                }
                            }
                        } 
                    }
                }
                
                // 3. Copiar también las funciones de error
                tablaGlobal.setFuncionesError(gramatica.getTPredictiva().getFuncionesError());
            
                // 4. Forzar un refresh de la tabla global
                tablaGlobal.getTablaPredictiva().refresh();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Crea las columnas en una tabla dada.
     */
    private void crearColumnasEnTabla(TableView<FilaTablaPredictiva> tabla) {
        // Limpiar columnas existentes
        tabla.getColumns().clear();
        
        // Columna para símbolos
        TableColumn<FilaTablaPredictiva, String> colSimbolo = new TableColumn<>("Símbolo");
        colSimbolo.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getSimbolo()));
        colSimbolo.setPrefWidth(80);
        colSimbolo.setMinWidth(60);
        colSimbolo.setMaxWidth(120);
        
        // Configurar celda factory para la columna de símbolos
        colSimbolo.setCellFactory(column -> {
            return new TableCell<FilaTablaPredictiva, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (empty || item == null) {
                        setText(null);
                        getStyleClass().setAll("empty-cell");
                        return;
                    }
                    
                    setText(item);
                    
                    // Aplicar estilo usando clases CSS en lugar de estilos inline
                    if (isSelected()) {
                        getStyleClass().setAll("selected-cell");
                    } else {
                        getStyleClass().setAll("symbol-cell");
                    }
                }
            };
        });
        
        // Añadir la columna de símbolos
        tabla.getColumns().add(colSimbolo);
        
        // Añadir columnas para cada terminal
        boolean existeDolar = false;
        for (Terminal t : gramatica.getTerminales()) {
            if (t.getNombre() == null || t.getNombre().isEmpty()) continue;
            if ("$".equals(t.getNombre())) {
                existeDolar = true;
            }
            TableColumn<FilaTablaPredictiva, String> colT = new TableColumn<>(t.getNombre());
            colT.setPrefWidth(100);
            colT.setMinWidth(80);
            colT.setMaxWidth(150);
            final String nombreTerminal = t.getNombre();
            colT.setCellValueFactory(cellData -> 
                cellData.getValue().getValor(nombreTerminal));
            
            // Configurar celda factory personalizada para cada columna
            colT.setCellFactory(column -> {
                return new TableCell<FilaTablaPredictiva, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        
                        if (empty || item == null) {
                            setText(null);
                            getStyleClass().setAll("empty-cell");
                            return;
                        }
                        // Mostrar solo Ex si es función de error
                        if (item.matches("E\\d+")) {
                            setText(item);
                            getStyleClass().setAll("error-cell");
                        } else if (item.startsWith("E")) {
                            int i = 1;
                            while (i < item.length() && Character.isDigit(item.charAt(i))) i++;
                            setText(item.substring(0, i));
                            getStyleClass().setAll("error-cell");
                        } else if (!item.isEmpty() && Character.isDigit(item.charAt(0))) {
                            setText(item);
                            getStyleClass().setAll("production-cell");
                        } else if (item.startsWith("ε_")) {
                            setText(item.substring(2)); // Quitar prefijo
                            getStyleClass().setAll("epsilon-cell");
                        } else {
                            setText(item);
                            getStyleClass().setAll("default-cell");
                        }
                        if (isSelected()) {
                            getStyleClass().setAll("selected-cell");
                        }
                    }
                };
            });
            
            tabla.getColumns().add(colT);
        }
        
        // Solo añadir la columna $ si no existe ya en los terminales
        if (!existeDolar) {
            TableColumn<FilaTablaPredictiva, String> colDolar = new TableColumn<>("$");
            colDolar.setPrefWidth(100);
            colDolar.setMinWidth(80);
            colDolar.setMaxWidth(150);
            colDolar.setCellValueFactory(cellData -> 
                cellData.getValue().getValor("$"));
            
            // Configurar celda factory para columna $
            colDolar.setCellFactory(column -> {
                return new TableCell<FilaTablaPredictiva, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        
                        if (empty || item == null) {
                            setText(null);
                            getStyleClass().setAll("empty-cell");
                            return;
                        }
                        
                        setText(item);
                        
                        // Aplicar estilo según tipo de contenido
                        if (isSelected()) {
                            getStyleClass().setAll("selected-cell");
                        } else if (item != null && !item.isEmpty()) {
                            if (item.startsWith("E")) {
                                getStyleClass().setAll("error-cell");
                            } else if (Character.isDigit(item.charAt(0))) {
                                getStyleClass().setAll("production-cell");
                            } else if (item.startsWith("ε_")) {
                                setText(item.substring(2)); // Quitar prefijo
                                getStyleClass().setAll("epsilon-cell");
                            } else {
                                getStyleClass().setAll("default-cell");
                            }
                        } else {
                            getStyleClass().setAll("empty-cell");
                        }
                    }
                };
            });
            
            tabla.getColumns().add(colDolar);
        }
        
        // Aplicar configuración global - usar CSS en lugar de estilos inline
        tabla.setStyle("");
    }

    private void actualizarComboBoxFuncionesError() {
        if (comboBoxFuncionesError == null) return;

        // Guardar la selección actual
        FuncionError seleccionActual = comboBoxFuncionesError.getValue();
        
        // Limpiar y actualizar el ComboBox
        comboBoxFuncionesError.getItems().clear();
        List<FuncionError> funcionesError = gramatica.getTPredictiva().getFuncionesError();
        
        if (funcionesError != null) {
            for (FuncionError funcion : funcionesError) {
                comboBoxFuncionesError.getItems().add(funcion);
            }
        }

        // Personalizar la visualización del ComboBox
        comboBoxFuncionesError.setCellFactory(listView -> new javafx.scene.control.ListCell<FuncionError>() {
            @Override
            protected void updateItem(FuncionError item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? "" : funcionErrorToString(item));
            }
        });
        comboBoxFuncionesError.setButtonCell(new javafx.scene.control.ListCell<FuncionError>() {
            @Override
            protected void updateItem(FuncionError item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? "" : funcionErrorToString(item));
            }
        });

        // Restaurar la selección si es posible
        if (seleccionActual != null) {
            comboBoxFuncionesError.setValue(seleccionActual);
        } else if (comboBoxFuncionesError.getItems().size() > 0) {
            comboBoxFuncionesError.getSelectionModel().selectFirst();
        }
    }

    private void actualizarTablaPredictiva() {
        if (tablaPredictiva == null) return;

        @SuppressWarnings("unchecked")
        TableColumn<FilaTablaPredictiva, String> columnaNoTerminal = 
            (TableColumn<FilaTablaPredictiva, String>) tablaPredictiva.getColumns().get(0);
        columnaNoTerminal.setText(bundle.getString("simulador.paso3.columna.noterminal"));
        // El resto de columnas mantienen su nombre original (el símbolo terminal)

        // Refrescar la tabla
        tablaPredictiva.refresh();
    }

    public String funcionErrorToString(FuncionError funcion) {
        if (funcion == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(funcion.getIdentificador());
        sb.append(" - ");
        sb.append(bundle.getString(funcion.getNombreAccion()));
        if (funcion.getSimbolo() != null) {
            sb.append(": ");
            sb.append(funcion.getSimbolo().getNombre());
        }
        return sb.toString();
    }

    @FXML
    private void handleAnterior() {
        // Guardar la tabla local en la global antes de retroceder
        guardarTablaEnGlobal();
        
        // Retroceder al paso 3
        panelPadre.cambiarPaso(3);
    }

    @FXML
    private void handleSiguiente() { //Finalizar
        // Validar las funciones de error
        if (validarFuncionesError()) {
            // Guardar la tabla local en la global antes de avanzar
            guardarTablaEnGlobal();
            
            // Avanzar al siguiente paso
            panelPadre.cambiarPaso(4);
        } else {
            // Mostrar un mensaje de error si la validación falla
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(bundle.getString("simulador.paso5.alert.error"));
            alert.setHeaderText(bundle.getString("simulador.paso5.alert.validacion.header"));
            alert.setContentText(bundle.getString("simulador.paso5.alert.validacion.content"));
            alert.showAndWait();
        }
    }

    private boolean validarFuncionesError() {
        // Implementar la lógica de validación de las funciones de error
        return true;
    }

    @FXML
    private void handleCancelar() {
        panelPadre.cancelarSimulacion();
    }

    @FXML
    private void handleEliminar() {
        // Obtener la celda seleccionada
        @SuppressWarnings("unchecked")
        TableColumn<FilaTablaPredictiva, String> column = 
            (TableColumn<FilaTablaPredictiva, String>) tablaPredictiva.getFocusModel().getFocusedCell().getTableColumn();
        if (column != null && !column.getText().equals("No Terminal")) {
            FilaTablaPredictiva fila = tablaPredictiva.getSelectionModel().getSelectedItem();
            if (fila != null) {
                String valorCelda = fila.getValor(column.getText()).get();
                
                // Verificar si la celda tiene una función de error o una producción épsilon añadida
                if (valorCelda != null && (valorCelda.startsWith("E") || valorCelda.startsWith("ε_"))) {
                    // Eliminar la función de error o la producción épsilon (dejar la celda vacía)
                    fila.setValor(column.getText(), "");
                    
                    // Guardar inmediatamente en la tabla global para evitar pérdida de datos
                    guardarTablaEnGlobal();
                    
                    // Solo refrescar la tabla sin recrear las columnas para mantener el formato
                    tablaPredictiva.refresh();
                } else {
                    // Mostrar alerta si la celda no tiene una función de error o una producción épsilon añadida
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle(bundle.getString("simulador.paso5.alert.error"));
                    alert.setHeaderText(bundle.getString("simulador.paso5.alert.celda.invalida.header"));
                    alert.setContentText(bundle.getString("simulador.paso5.alert.celda.invalida.content"));
                    alert.showAndWait();
                }
            }
        }
    }

    @FXML
    private void handleRellenar() {
        // Obtener la celda seleccionada
        @SuppressWarnings("unchecked")
        TableColumn<FilaTablaPredictiva, String> column = 
            (TableColumn<FilaTablaPredictiva, String>) tablaPredictiva.getFocusModel().getFocusedCell().getTableColumn();
        if (column != null && !column.getText().equals("No Terminal")) {
            FilaTablaPredictiva fila = tablaPredictiva.getSelectionModel().getSelectedItem();
            if (fila != null) {
                String valorCelda = fila.getValor(column.getText()).get();
                
                // Verificar si la celda está vacía
                if (valorCelda == null || valorCelda.isEmpty()) {
                    // Obtener la función de error seleccionada
                    FuncionError funcionErrorSeleccionada = (FuncionError) comboBoxFuncionesError.getValue();
                    if (funcionErrorSeleccionada != null) {
                        // Añadir la función de error a la celda en formato Ex
                        fila.setValor(column.getText(), "E" + funcionErrorSeleccionada.getIdentificador());
                        // Guardar inmediatamente en la tabla global para evitar pérdida de datos
                        guardarTablaEnGlobal();
                        // Solo refrescar la tabla sin recrear las columnas para mantener el formato
                        tablaPredictiva.refresh();
                    } else {
                        // Mostrar alerta si no se ha seleccionado una función de error
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle(bundle.getString("simulador.paso5.alert.error"));
                        alert.setHeaderText(bundle.getString("simulador.paso5.alert.no.funcion.header"));
                        alert.setContentText(bundle.getString("simulador.paso5.alert.no.funcion.content"));
                        alert.showAndWait();
                    }
                } else {
                    // Mostrar alerta si la celda ya tiene un valor
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle(bundle.getString("simulador.paso5.alert.error"));
                    alert.setHeaderText(bundle.getString("simulador.paso5.alert.celda.no.vacia.header"));
                    alert.setContentText(bundle.getString("simulador.paso5.alert.celda.no.vacia.content"));
                    alert.showAndWait();
                }
            }
        }
    }


    @FXML
    private void handleGramatica() {
        panelPadre.mostrarGramaticaOriginal();
    }

    @FXML
    private void handlePrimero() {
        // Guardar la tabla local en la global antes de retroceder
        guardarTablaEnGlobal();
        
        panelPadre.cambiarPaso(0);
    }



    /**
     * Refresca la vista del paso 5.
     * Reconstruye la tabla usando la global si existe.
     */
    public void refrescarVista() {
        construirTablaPredictiva();
        actualizarComboBoxFuncionesError();
    }

    /**
     * Guarda explícitamente los datos de la tabla en la tabla global.
     * Este método puede ser llamado desde fuera de la clase para forzar un guardado.
     */
    public void guardarDatosTabla() {
        guardarTablaEnGlobal();
    }

    /**
     * Handler para rellenar automáticamente las celdas vacías con épsilon.
     * Recorre todas las celdas de la tabla y añade épsilon en las celdas vacías
     * donde se podría añadir una función de error.
     */
    @FXML
    private void handleRellenarEpsilon() {
        if (comboBoxFuncionesError.getValue() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(bundle.getString("simulador.paso5.alert.error"));
            alert.setHeaderText(bundle.getString("simulador.paso5.alert.no.funcion.header"));
            alert.setContentText(bundle.getString("simulador.paso5.alert.no.funcion.content"));
            alert.showAndWait();
            return;
        }

        boolean seHizoAlgunCambio = false;
        
        // Obtener la tabla extendida global
        TablaPredictivaPaso5 tablaGlobal = panelPadre.getTablaPredictivaExtendidaGlobal();
        if (tablaGlobal == null) {
            return;
        }
        
        // Usar los items de la tabla global
        ObservableList<FilaTablaPredictiva> filas = tablaGlobal.getTablaPredictiva().getItems();
        if (filas == null || filas.isEmpty()) {
            return;
        }
                
        for (FilaTablaPredictiva fila : filas) {
            // Si es terminal y solo aparece en primera posición, saltar
            if (fila.getEsTerminal()) {
                Terminal terminalFila = gramatica.getTerminales().stream()
                    .filter(t -> t.getNombre().equals(fila.getSimbolo()))
                    .findFirst()
                    .orElse(null);

                // Usar el método privado de TablaPredictivaPaso5 mediante la instancia tablaGlobal
                boolean soloPrimeraPos = false;
                try {
                    java.lang.reflect.Method m = tablaGlobal.getClass().getDeclaredMethod("apareceSoloPrimeraPos", Terminal.class);
                    m.setAccessible(true);
                    soloPrimeraPos = (boolean) m.invoke(tablaGlobal, terminalFila);
                } catch (Exception e) {
                }

                if (terminalFila != null && soloPrimeraPos) {
                    continue;
                }
            }
            for (TableColumn<FilaTablaPredictiva, ?> col : tablaPredictiva.getColumns()) {
                String nombreCol = col.getText();
                if (nombreCol.equals("Símbolo")) continue;

                String valor = fila.getValor(nombreCol).get();
                // Si la celda está vacía
                if (valor == null || valor.isEmpty()) {
                    fila.setValor(nombreCol, "ε");
                    seHizoAlgunCambio = true;
                }
            }
        }
        
        if (seHizoAlgunCambio) {
            // Refrescar la tabla y guardar
            guardarTablaEnGlobal();
            tablaPredictiva.refresh();
        }
    }

    @FXML
    private void handleResetearTabla() {
        for (FilaTablaPredictiva fila : tablaPredictiva.getItems()) {
            for (TableColumn<FilaTablaPredictiva, ?> col : tablaPredictiva.getColumns()) {
                String nombreCol = col.getText();
                if (nombreCol.equals("Símbolo")) continue;
                String valor = fila.getValor(nombreCol).get();
                if (valor != null && (valor.startsWith("E") || valor.equals("ε") || valor.startsWith("ε_"))) {
                    fila.setValor(nombreCol, "");
                }
            }
        }
        guardarTablaEnGlobal();
        tablaPredictiva.refresh();
    }

    @Override
    public void actualizarTextos(ResourceBundle bundle) {
        this.bundle = bundle;
        
        // Actualizar textos de la interfaz
        if (labelTitulo != null) labelTitulo.setText(bundle.getString("simulador.paso5.titulo"));
        if (labelSeleccioneFuncion != null) labelSeleccioneFuncion.setText(bundle.getString("simulador.paso5.seleccione.funcion"));
        if (buttonEliminar != null) buttonEliminar.setText(bundle.getString("simulador.paso5.btn.eliminar"));
        if (buttonRellenarEpsilon != null) buttonRellenarEpsilon.setText(bundle.getString("simulador.paso5.btn.rellenar.epsilon"));
        if (buttonResetearTabla != null) buttonResetearTabla.setText(bundle.getString("simulador.paso5.btn.resetear"));
        if (buttonCancelar != null) buttonCancelar.setText(bundle.getString("simulador.paso5.btn.cancelar"));
        if (buttonGramatica != null) buttonGramatica.setText(bundle.getString("simulador.paso5.btn.gramatica"));
        if (buttonPrimero != null) buttonPrimero.setText(bundle.getString("simulador.paso5.btn.primero"));
        if (buttonAnterior != null) buttonAnterior.setText(bundle.getString("simulador.paso5.btn.anterior"));
        if (buttonSiguiente != null) buttonSiguiente.setText(bundle.getString("simulador.paso5.btn.siguiente"));
        if (buttonSimulacion != null) buttonSimulacion.setText(bundle.getString("simulador.paso5.btn.simulacion"));

        // Actualizar el ComboBox de funciones de error
        actualizarComboBoxFuncionesError();

        // Actualizar la tabla predictiva
        if (tablaPredictiva != null) {
            actualizarTablaPredictiva();
        }
    }

    public FuncionError getFuncionErrorSeleccionada() {
        return comboBoxFuncionesError != null ? comboBoxFuncionesError.getValue() : null;
    }

    public ResourceBundle getBundle() {
        return this.bundle;
    }
}