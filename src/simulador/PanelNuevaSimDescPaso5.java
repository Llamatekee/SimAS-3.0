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
import javafx.scene.control.TablePosition;
import javafx.scene.control.Tab;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.TableCell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import gramatica.FilaTablaPredictiva;
import gramatica.FuncionError;
import gramatica.Gramatica;
import gramatica.TablaPredictivaPaso5;
import gramatica.Terminal;

public class PanelNuevaSimDescPaso5 implements PanelNuevaSimDescPaso {

    @FXML private Label labelTitulo;
    @FXML private Button buttonAnterior;
    @FXML private Button buttonSiguiente;
    @FXML private Button buttonCancelar;
    @FXML private Button buttonEliminar;
    @FXML private Button buttonRellenar;
    @FXML private Button buttonGramatica;
    @FXML private Button buttonPrimero;
    @FXML private Button buttonUltimo;
    @FXML private Button buttonSimulacion;
    @FXML private Button buttonRellenarEpsilon;
    @FXML private ComboBox<String> comboBoxFuncionesError;
    @FXML private TableView<FilaTablaPredictiva> tablaPredictiva;
    @FXML private TableColumn<String[], String> columnSimbolo;
    @FXML private TableColumn<String[], String> columnAccion;

    private Parent root;
    private PanelSimuladorDesc panelPadre;
    private Gramatica gramatica;

    public PanelNuevaSimDescPaso5(PanelSimuladorDesc panelPadre) {
        this.panelPadre = panelPadre;
        this.gramatica = panelPadre.gramatica;
        cargarFXML();
        initialize();
    }

    private void cargarFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/PanelNuevaSimDescPaso5.fxml"));
            loader.setController(this);
            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Parent getRoot() {
        return root;
    }

    @FXML
    private void initialize() {
        // Deshabilitar los botones Siguiente y Último en el paso 5
        buttonSiguiente.setDisable(true);
        buttonUltimo.setDisable(true);
        
        // Configurar el botón de simulación
        buttonSimulacion.setOnAction(e -> iniciarSimulacion());

        // Verificar si hay una tabla predictiva básica en la gramática
        if (gramatica.getTPredictiva() == null) {
            System.out.println("Error: No hay tabla predictiva básica inicializada en la gramática");
            return;
        }

        // Verificar las funciones de error
        List<FuncionError> funcionesError = gramatica.getTPredictiva().getFuncionesError();
        if (funcionesError == null || funcionesError.isEmpty()) {
            System.out.println("Error: No hay funciones de error disponibles");
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
                System.out.println("Guardando cambios en doble clic");
                guardarTablaEnGlobal();
            }
        });

        if (buttonRellenarEpsilon != null) {
            buttonRellenarEpsilon.setOnAction(e -> handleRellenarEpsilon());
        }
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
            System.out.println("Creando una nueva tabla predictiva extendida desde cero");
            
            // Crear una nueva tabla predictiva extendida
            tablaGlobal = new TablaPredictivaPaso5(tablaPredictiva);
            tablaGlobal.setPanelPaso5(this);
            
            // Usar las funciones de error de la tabla básica
            tablaGlobal.setFuncionesError(gramatica.getTPredictiva().getFuncionesError());
            
            // Construir la tabla
            tablaGlobal.construir(gramatica);
            
            // Guardar la instancia en el panel padre
            panelPadre.setTablaPredictivaExtendidaGlobal(tablaGlobal);
            
            System.out.println("Tabla predictiva extendida creada con " + tablaPredictiva.getColumns().size() + " columnas");
        } else {
            System.out.println("Usando la tabla predictiva extendida global existente");
            
            // Mostrar el número de columnas en la tabla global
            int columnasOriginales = tablaGlobal.getTablaPredictiva().getColumns().size();
            System.out.println("La tabla global tiene " + columnasOriginales + " columnas");
            
            // Si la tabla global ya tiene datos, asegurarnos de usarlos en la tabla local
            if (tablaGlobal.getTablaPredictiva().getItems() != null && 
                !tablaGlobal.getTablaPredictiva().getItems().isEmpty()) {
                
                // Limpiar la tabla local
                tablaPredictiva.getColumns().clear();
                
                // Crear las columnas en la tabla local
                crearColumnasEnTabla(tablaPredictiva);
                
                // Copiar los datos de la tabla global a la local
                tablaPredictiva.setItems(tablaGlobal.getTablaPredictiva().getItems());
                
                System.out.println("Datos de la tabla global copiados a la tabla local");
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
                
                System.out.println("Tabla predictiva reconstruida con " + tablaPredictiva.getColumns().size() + " columnas");
            }
        }
        
        // Asegurarnos de que las columnas estén bien
        if (tablaPredictiva.getColumns().isEmpty()) {
            System.out.println("¡ALERTA! No hay columnas en la tabla - creándolas");
            crearColumnasEnTabla(tablaPredictiva);
        }
        
        // Configurar propiedades visuales de la tabla
        tablaPredictiva.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tablaPredictiva.setTableMenuButtonVisible(false);
        tablaPredictiva.setEditable(true);
        
        // Forzar un refresh
        tablaPredictiva.refresh();
        
        System.out.println("Tabla construida con éxito - Filas: " + 
                         (tablaPredictiva.getItems() != null ? tablaPredictiva.getItems().size() : 0) + 
                         ", Columnas: " + tablaPredictiva.getColumns().size());
        
        // Mostrar información sobre las celdas para diagnóstico
        if (tablaPredictiva.getItems() != null && !tablaPredictiva.getItems().isEmpty()) {
            FilaTablaPredictiva primeraFila = tablaPredictiva.getItems().get(0);
            if (primeraFila != null) {
                for (TableColumn<FilaTablaPredictiva, ?> col : tablaPredictiva.getColumns()) {
                    if (col.getText().equals("Símbolo")) continue;
                    
                    String valorCelda = primeraFila.getValor(col.getText()).get();
                    System.out.println("Celda [0," + col.getText() + "] = " + valorCelda);
                }
            }
        }
    }

    /**
     * Guarda la tabla local en la tabla global.
     * Este método debe llamarse antes de salir del paso 5.
     */
    private void guardarTablaEnGlobal() {
        TablaPredictivaPaso5 tablaGlobal = panelPadre.getTablaPredictivaExtendidaGlobal();
        
        if (tablaGlobal != null && tablaPredictiva.getItems() != null) {
            System.out.println("Guardando datos de la tabla local a la global");
            
            try {
                // 1. Asegurar que la tabla global tenga columnas
                if (tablaGlobal.getTablaPredictiva().getColumns().isEmpty()) {
                    System.out.println("La tabla global no tiene columnas - recreándolas");
                    crearColumnasEnTabla(tablaGlobal.getTablaPredictiva());
                }
                
                // 2. Iterar por todas las filas y columnas para copiar los valores manualmente
                // Esto asegura que todos los valores se transfieran, incluyendo las funciones de error
                ObservableList<FilaTablaPredictiva> filasLocales = tablaPredictiva.getItems();
                ObservableList<FilaTablaPredictiva> filasGlobales = tablaGlobal.getTablaPredictiva().getItems();
                
                // Si no hay filas en la tabla global, copiar directamente
                if (filasGlobales == null || filasGlobales.isEmpty()) {
                    System.out.println("No hay filas en la tabla global - copiando directamente");
                    // Crear una copia directa de los elementos para evitar problemas de referencia
                    tablaGlobal.getTablaPredictiva().setItems(FXCollections.observableArrayList(filasLocales));
                } else {
                    // Copiar celda por celda para preservar los valores modificados
                    System.out.println("Copiando valores celda por celda");
                    
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
                                    System.out.println("Copiado valor en [" + filaGlobal.getSimbolo() + "," + nombreColumna + "]: " + valorLocal);
                                }
                            }
                        } else {
                            System.out.println("ADVERTENCIA: Fila " + i + " tiene símbolos distintos: " + 
                                              filaLocal.getSimbolo() + " vs " + filaGlobal.getSimbolo());
                        }
                    }
                }
                
                // 3. Copiar también las funciones de error
                tablaGlobal.setFuncionesError(gramatica.getTPredictiva().getFuncionesError());
                
                System.out.println("Datos guardados - Global ahora tiene " + 
                                 (tablaGlobal.getTablaPredictiva().getItems() != null ? 
                                  tablaGlobal.getTablaPredictiva().getItems().size() : 0) + " filas");
                
                // 4. Forzar un refresh de la tabla global
                tablaGlobal.getTablaPredictiva().refresh();
            } catch (Exception e) {
                System.out.println("ERROR al guardar la tabla: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("No se pudo guardar la tabla local en la global - referencias nulas");
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
        colSimbolo.setPrefWidth(100);
        
        // Configurar celda factory para la columna de símbolos
        colSimbolo.setCellFactory(column -> {
            return new TableCell<FilaTablaPredictiva, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                        return;
                    }
                    
                    setText(item);
                    
                    // Aplicar estilo
                    if (isSelected()) {
                        // Estilo para celda seleccionada
                        setStyle("-fx-background-color: #E3F2FD; -fx-text-fill: black; -fx-font-weight: bold; -fx-border-color: #1976D2; -fx-border-width: 1px;");
                    } else {
                        // Estilo para símbolos
                        setStyle("-fx-background-color: #F8F9FA; -fx-text-fill: black; -fx-font-weight: bold;");
                    }
                }
            };
        });
        
        // Añadir la columna de símbolos
        tabla.getColumns().add(colSimbolo);
        
        // Añadir columnas para cada terminal
        for (Terminal t : gramatica.getTerminales()) {
            if (t.getNombre() == null || t.getNombre().isEmpty()) continue;
            
            TableColumn<FilaTablaPredictiva, String> colT = new TableColumn<>(t.getNombre());
            colT.setPrefWidth(100);
            
            // Usar variable final para capturar el nombre del terminal
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
                            setStyle("");
                            return;
                        }
                        
                        setText(item);
                        
                        // Aplicar estilo según tipo de contenido
                        if (isSelected()) {
                            // Estilo para celda seleccionada
                            setStyle("-fx-background-color: #E3F2FD; -fx-text-fill: black; -fx-font-weight: bold; -fx-border-color: #1976D2; -fx-border-width: 1px;");
                        } else if (item != null && !item.isEmpty()) {
                            if (item.startsWith("E")) {
                                // Estilo para funciones de error
                                setStyle("-fx-text-fill: #D32F2F; -fx-font-weight: bold;");
                            } else if (Character.isDigit(item.charAt(0))) {
                                // Estilo para producciones
                                setStyle("-fx-text-fill: black; -fx-font-weight: bold;");
                            } else if (item.startsWith("ε_")) {
                                // Estilo para épsilon
                                setText(item.substring(2)); // Quitar prefijo
                                setStyle("-fx-text-fill: #0D47A1; -fx-font-weight: bold;");
                            } else {
                                // Estilo predeterminado
                                setStyle("-fx-text-fill: black;");
                            }
                        } else {
                            // Estilo para celdas vacías
                            setStyle("-fx-text-fill: black;");
                        }
                    }
                };
            });
            
            tabla.getColumns().add(colT);
        }
        
        // Añadir columna para $
        TableColumn<FilaTablaPredictiva, String> colDolar = new TableColumn<>("$");
        colDolar.setPrefWidth(100);
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
                        setStyle("");
                        return;
                    }
                    
                    setText(item);
                    
                    // Aplicar estilo según tipo de contenido
                    if (isSelected()) {
                        // Estilo para celda seleccionada
                        setStyle("-fx-background-color: #E3F2FD; -fx-text-fill: black; -fx-font-weight: bold; -fx-border-color: #1976D2; -fx-border-width: 1px;");
                    } else if (item != null && !item.isEmpty()) {
                        if (item.startsWith("E")) {
                            // Estilo para funciones de error
                            setStyle("-fx-text-fill: #D32F2F; -fx-font-weight: bold;");
                        } else if (Character.isDigit(item.charAt(0))) {
                            // Estilo para producciones
                            setStyle("-fx-text-fill: black; -fx-font-weight: bold;");
                        } else if (item.startsWith("ε_")) {
                            // Estilo para épsilon
                            setText(item.substring(2)); // Quitar prefijo
                            setStyle("-fx-text-fill: #0D47A1; -fx-font-weight: bold;");
                        } else {
                            // Estilo predeterminado
                            setStyle("-fx-text-fill: black;");
                        }
                    } else {
                        // Estilo para celdas vacías
                        setStyle("-fx-text-fill: black;");
                    }
                }
            };
        });
        
        tabla.getColumns().add(colDolar);
        
        // Aplicar configuración global
        tabla.setStyle("-fx-background-color: white; -fx-table-cell-border-color: black;");
        
        System.out.println("Creadas " + tabla.getColumns().size() + " columnas manualmente");
    }

    private void actualizarComboBoxFuncionesError() {
        // Limpiar el ComboBox antes de llenarlo
        comboBoxFuncionesError.getItems().clear();
        
        // Obtener las funciones de error
        List<FuncionError> funcionesError = gramatica.getTPredictiva().getFuncionesError();
        
        // Llenar el ComboBox con las funciones de error
        for (FuncionError funcion : funcionesError) {
            StringBuilder descripcion = new StringBuilder();
            descripcion.append("E").append(funcion.getIdentificador()).append(" - ");
            switch (funcion.getAccion()) {
                case 1 -> descripcion.append("Insertar un Símbolo en la Entrada: ");
                case 2 -> descripcion.append("Borrar un Símbolo de la Entrada");
                case 3 -> descripcion.append("Modificar un Símbolo de la Entrada: ");
                case 4 -> descripcion.append("Insertar un Símbolo de la Pila: ");
                case 5 -> descripcion.append("Borrar un Símbolo de la Pila");
                case 6 -> descripcion.append("Modificar un Símbolo de la Pila: ");
                case 7 -> descripcion.append("Terminar el análisis");
            }
            if (funcion.getAccion() == 1 || funcion.getAccion() == 3 || funcion.getAccion() == 4 || funcion.getAccion() == 6) {
                if (funcion.getSimbolo() != null) {
                    descripcion.append(funcion.getSimbolo().getNombre());
                }
            }
            comboBoxFuncionesError.getItems().add(descripcion.toString());
        }
        
        // Seleccionar el primer elemento si hay elementos disponibles
        if (!comboBoxFuncionesError.getItems().isEmpty()) {
            comboBoxFuncionesError.getSelectionModel().selectFirst();
        } else {
            System.out.println("No se encontraron funciones de error para mostrar en el ComboBox");
        }
    }

    public String getFuncionErrorSeleccionada() {
        return comboBoxFuncionesError.getSelectionModel().getSelectedItem();
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
            alert.setTitle("Error");
            alert.setHeaderText("Validación fallida");
            alert.setContentText("Por favor, asegúrese de que todas las funciones de error estén correctamente definidas.");
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
        TableColumn<FilaTablaPredictiva, ?> column = tablaPredictiva.getFocusModel().getFocusedCell().getTableColumn();
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
                    
                    // Para forzar la actualización visual, recrear las columnas
                    System.out.println("Recreando columnas para asegurar la visualización correcta");
                    tablaPredictiva.getColumns().clear();
                    crearColumnasEnTabla(tablaPredictiva);
                    
                    // Refrescar la tabla local
                    tablaPredictiva.refresh();
                    System.out.println("Función de error eliminada y cambios guardados en columna " + column.getText());
                } else {
                    // Mostrar alerta si la celda no tiene una función de error o una producción épsilon añadida
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Celda no válida");
                    alert.setContentText("Esta celda no contiene una función de error o una producción épsilon añadida para eliminar.");
                    alert.showAndWait();
                }
            }
        }
    }

    @FXML
    private void handleRellenar() {
        // Obtener la celda seleccionada
        TableColumn<FilaTablaPredictiva, ?> column = tablaPredictiva.getFocusModel().getFocusedCell().getTableColumn();
        if (column != null && !column.getText().equals("No Terminal")) {
            FilaTablaPredictiva fila = tablaPredictiva.getSelectionModel().getSelectedItem();
            if (fila != null) {
                String valorCelda = fila.getValor(column.getText()).get();
                
                // Verificar si la celda está vacía
                if (valorCelda == null || valorCelda.isEmpty()) {
                    // Obtener la función de error seleccionada
                    String funcionErrorSeleccionada = getFuncionErrorSeleccionada();
                    if (funcionErrorSeleccionada != null) {
                        // Añadir la función de error a la celda
                        fila.setValor(column.getText(), funcionErrorSeleccionada);
                        
                        // Guardar inmediatamente en la tabla global para evitar pérdida de datos
                        guardarTablaEnGlobal();
                        
                        // Para forzar la actualización visual, recrear las columnas
                        System.out.println("Recreando columnas para asegurar la visualización correcta");
                        tablaPredictiva.getColumns().clear();
                        crearColumnasEnTabla(tablaPredictiva);
                        
                        // Refrescar la tabla local
                        tablaPredictiva.refresh();
                        System.out.println("Función de error añadida y guardada: " + funcionErrorSeleccionada + " en columna " + column.getText());
                    } else {
                        // Mostrar alerta si no se ha seleccionado una función de error
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("No se ha seleccionado una función de error");
                        alert.setContentText("Por favor, seleccione una función de error antes de rellenar la celda.");
                        alert.showAndWait();
                    }
                } else {
                    // Mostrar alerta si la celda ya tiene un valor
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Celda no vacía");
                    alert.setContentText("Esta celda ya tiene un valor. Por favor, seleccione una celda vacía.");
                    alert.showAndWait();
                }
            }
        }
    }

    private List<FuncionError> obtenerProduccionesEpsilon() {
        // Implementar la lógica para obtener las producciones épsilon
        return new ArrayList<>();
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

    @FXML
    private void handleUltimo() {
        // Ya estamos en el último paso, no hacer nada
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
        System.out.println("Guardado manual de datos de tabla ejecutado");
    }

    /**
     * Handler para rellenar automáticamente las celdas vacías con épsilon.
     * Recorre todas las celdas de la tabla y añade épsilon en las celdas vacías
     * donde se podría añadir una función de error.
     */
    @FXML
    private void handleRellenarEpsilon() {
        System.out.println("Rellenando celdas vacías con épsilon...");
        boolean seHizoAlgunCambio = false;
        
        // Obtener la tabla extendida global
        TablaPredictivaPaso5 tablaGlobal = panelPadre.getTablaPredictivaExtendidaGlobal();
        if (tablaGlobal == null) {
            System.out.println("ERROR: No se encontró la tabla predictiva extendida global");
            return;
        }
        
        // Usar los items de la tabla global
        ObservableList<FilaTablaPredictiva> filas = tablaGlobal.getTablaPredictiva().getItems();
        if (filas == null || filas.isEmpty()) {
            System.out.println("ERROR: La tabla global no tiene filas");
            return;
        }
        
        System.out.println("Procesando " + filas.size() + " filas de la tabla global");
        
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
                    System.out.println("No se pudo invocar apareceSoloPrimeraPos: " + e.getMessage());
                }

                if (terminalFila != null && soloPrimeraPos) {
                    System.out.println("Saltando terminal solo en primera posición: " + fila.getSimbolo());
                    continue;
                }
            }
            System.out.println("Procesando fila: " + fila.getSimbolo());
            for (TableColumn<FilaTablaPredictiva, ?> col : tablaPredictiva.getColumns()) {
                String nombreCol = col.getText();
                if (nombreCol.equals("Símbolo")) continue;

                String valor = fila.getValor(nombreCol).get();
                // Si la celda está vacía
                if (valor == null || valor.isEmpty()) {
                    fila.setValor(nombreCol, "ε");
                    seHizoAlgunCambio = true;
                    System.out.println("Añadido épsilon en [" + fila.getSimbolo() + "," + nombreCol + "]");
                }
            }
        }
        
        if (seHizoAlgunCambio) {
            // Refrescar la tabla y guardar
            guardarTablaEnGlobal();
            tablaPredictiva.refresh();
            System.out.println("Celdas vacías rellenadas con épsilon.");
        } else {
            System.out.println("No se encontraron celdas vacías para rellenar con épsilon.");
        }
    }
}