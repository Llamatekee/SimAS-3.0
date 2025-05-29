package simulador;

import com.itextpdf.text.DocumentException;
import gramatica.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.scene.layout.*;
import java.util.List;
import java.io.File;
import java.io.IOException;
import javafx.scene.Parent;
import editor.ActualizableTextos;
import java.util.ResourceBundle;

/**
 * Controlador para la simulación descendente en JavaFX.
 */
public class PanelNuevaSimDescPaso6 extends BorderPane implements PanelNuevaSimDescPaso, ActualizableTextos {

    @FXML private TableView<FilaTablaPredictiva> tablePredictiva;
    @FXML private ListView<String> listProducciones;
    @FXML private ListView<String> listFuncionesError;
    @FXML private Button btnSimular;
    @FXML private Button btnModificarErrores;
    @FXML private Button btnGenerarInforme;
    @FXML private Label labelTitulo;
    @FXML private Label labelProducciones;
    @FXML private Label labelFuncionesError;
    @FXML private Label labelTabla;

    private Gramatica gramatica;
    private TablaPredictiva tablaPredictiva;
    private List<FuncionError> funcionesError;
    private ObservableList<String> producciones;
    private ObservableList<Terminal> cadenaEntrada;
    private PanelSimuladorDesc panelSimuladorDesc;
    private ResourceBundle bundle;

    public PanelNuevaSimDescPaso6(Gramatica gramatica, PanelSimuladorDesc panelSimuladorDesc) {
        this.gramatica = gramatica;
        this.panelSimuladorDesc = panelSimuladorDesc;
        this.bundle = panelSimuladorDesc.getBundle();
        
        // Usar directamente la tabla predictiva extendida global
        if (panelSimuladorDesc != null && panelSimuladorDesc.getTablaPredictivaExtendidaGlobal() != null) {
            this.tablaPredictiva = panelSimuladorDesc.getTablaPredictivaExtendidaGlobal();
        } else {
            // Fallback por si no existe la global (no debería ocurrir)
            this.tablaPredictiva = gramatica.getTPredictiva();
        }
        
        // Obtener las funciones de error
        this.funcionesError = tablaPredictiva.getFuncionesError();
        
        // Inicializar componentes
        this.producciones = FXCollections.observableArrayList();
        this.cadenaEntrada = FXCollections.observableArrayList();
        
        // Cargar UI y datos
        cargarFXML();
        cargarDatos();
    }

    // Constructor anterior para compatibilidad
    public PanelNuevaSimDescPaso6(Gramatica gramatica) {
        this(gramatica, null);
    }

    private void cargarFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/PanelNuevaSimDescPaso6.fxml"));
            loader.setController(this);
            loader.setResources(bundle);
            BorderPane root = loader.load();
            this.setTop(root.getTop());
            this.setCenter(root.getCenter());
            this.setBottom(root.getBottom());
            
            // Inicializar los textos
            actualizarTextos(bundle);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cargarDatos() {
        // Cargar producciones
        producciones.setAll(gramatica.getProduccionesModel());
        listProducciones.setItems(producciones);

        // Cargar funciones de error
        ObservableList<String> errores = FXCollections.observableArrayList();
        if (funcionesError != null && !funcionesError.isEmpty()) {
            for (FuncionError fe : funcionesError) {
                errores.add(getDescripcionFuncionError(fe, bundle));
            }
        } else {
            errores.add(bundle.getString("simulador.paso6.error.sin.funciones"));
        }
        listFuncionesError.setItems(errores);

        // Cargar tabla predictiva
        if (tablaPredictiva instanceof TablaPredictivaPaso5) {
            TablaPredictivaPaso5 tablaPaso5 = (TablaPredictivaPaso5) tablaPredictiva;
            
            // Verificar si hay datos en la tabla global
            if (panelSimuladorDesc != null && panelSimuladorDesc.getTablaPredictivaExtendidaGlobal() != null) {
                TablaPredictivaPaso5 tablaGlobal = panelSimuladorDesc.getTablaPredictivaExtendidaGlobal();
                
                // Si la tabla no tiene columnas o filas, reconstruirla desde cero
                if (tablePredictiva.getColumns().isEmpty() || 
                    tablePredictiva.getItems() == null || 
                    tablePredictiva.getItems().isEmpty()) {
                    
                    // Limpiar la tabla
                    tablePredictiva.getColumns().clear();
                    
                    // Recrear columnas manualmente
                    crearColumnasManualmente();
                    
                    // Copiar filas si existen
                    if (tablaGlobal.getTablaPredictiva().getItems() != null) {
                        // Crear una copia de los items para asegurar que los datos se mantengan
                        ObservableList<FilaTablaPredictiva> items = tablaGlobal.getTablaPredictiva().getItems();
                        tablePredictiva.setItems(items);
                    }
                }
                
                // Configurar la tabla para mantener el estilo y comportamiento
                tablePredictiva.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
                tablePredictiva.setTableMenuButtonVisible(false);
                
                // Desactivar la edición (solo lectura)
                tablePredictiva.setEditable(false);
                
                // Refrescar la vista
                tablePredictiva.refresh();
            }
        } 
    }

    /**
     * Crea las columnas manualmente para la tabla predictiva.
     */
    private void crearColumnasManualmente() {
        // Verificar si hay una tabla global para comparar
        if (panelSimuladorDesc != null && panelSimuladorDesc.getTablaPredictivaExtendidaGlobal() != null) {
            TablaPredictivaPaso5 tablaGlobal = panelSimuladorDesc.getTablaPredictivaExtendidaGlobal();
            
            // Obtener el número de columnas en la tabla global para asegurar consistencia
            int columnasEnGlobal = tablaGlobal.getTablaPredictiva().getColumns().size();
        }
        
        // Columna para símbolos
        TableColumn<FilaTablaPredictiva, String> colSimbolo = new TableColumn<>("Símbolo");
        colSimbolo.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getSimbolo()));
        colSimbolo.setPrefWidth(100);
        
        // Configurar fábrica de celdas para personalizar el estilo de la columna de símbolos
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
                    
                    // Aplicar estilo para columna de símbolos
                    if (isSelected()) {
                        // Estilo para celda seleccionada
                        setStyle("-fx-background-color: #E3F2FD; -fx-text-fill: black; -fx-font-weight: bold; -fx-border-color: #1976D2; -fx-border-width: 1px;");
                    } else {
                        // Estilo para símbolos (terminales y no terminales)
                        setStyle("-fx-background-color: #F8F9FA; -fx-text-fill: black; -fx-font-weight: bold;");
                    }
                }
            };
        });
        
        // Añadir la columna de símbolos
        tablePredictiva.getColumns().add(colSimbolo);
        
        // Añadir columnas para cada terminal
        for (Terminal t : gramatica.getTerminales()) {
            if (t.getNombre() == null || t.getNombre().isEmpty()) continue;
            
            TableColumn<FilaTablaPredictiva, String> colT = new TableColumn<>(t.getNombre());
            colT.setPrefWidth(100);
            
            // Configurar la fábrica de valores para la columna
            final String nombreTerminal = t.getNombre(); // Capturar el nombre en una variable final
            colT.setCellValueFactory(cellData -> 
                cellData.getValue().getValor(nombreTerminal));
            
            // Configurar fábrica de celdas para personalizar el estilo
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
                        
                        // Aplicar estilo según el tipo de contenido
                        if (isSelected()) {
                            // Estilo para celda seleccionada
                            setStyle("-fx-background-color: #E3F2FD; -fx-text-fill: black; -fx-font-weight: bold; -fx-border-color: #1976D2; -fx-border-width: 1px;");
                        } else if (item != null && !item.isEmpty() && item.startsWith("E")) {
                            // Estilo para funciones de error
                            setStyle("-fx-text-fill: #1976D2; -fx-font-weight: bold;");
                        } else if (item != null && !item.isEmpty() && Character.isDigit(item.charAt(0))) {
                            // Estilo para producciones
                            setStyle("-fx-text-fill: black; -fx-font-weight: bold;");
                        } else {
                            // Estilo predeterminado
                            setStyle("-fx-text-fill: black;");
                        }
                    }
                };
            });
            
            // Añadir la columna
            tablePredictiva.getColumns().add(colT);
        }
        
        // Añadir columna para $ si no existe
        boolean existeDolar = false;
        for (TableColumn column : tablePredictiva.getColumns()) {
            if ("$".equals(column.getText())) {
                existeDolar = true;
                break;
            }
        }
        
        if (!existeDolar) {
            TableColumn<FilaTablaPredictiva, String> colDolar = new TableColumn<>("$");
            colDolar.setPrefWidth(100);
            colDolar.setCellValueFactory(cellData -> 
                cellData.getValue().getValor("$"));
                
            // Configurar fábrica de celdas para personalizar el estilo
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
                        
                        // Aplicar estilo según el tipo de contenido
                        if (isSelected()) {
                            // Estilo para celda seleccionada
                            setStyle("-fx-background-color: #E3F2FD; -fx-text-fill: black; -fx-font-weight: bold; -fx-border-color: #1976D2; -fx-border-width: 1px;");
                        } else if (item != null && !item.isEmpty() && item.startsWith("E")) {
                            // Estilo para funciones de error
                            setStyle("-fx-text-fill: #1976D2; -fx-font-weight: bold;");
                        } else if (item != null && !item.isEmpty() && Character.isDigit(item.charAt(0))) {
                            // Estilo para producciones
                            setStyle("-fx-text-fill: black; -fx-font-weight: bold;");
                        } else {
                            // Estilo predeterminado
                            setStyle("-fx-text-fill: black;");
                        }
                    }
                };
            });
            
            tablePredictiva.getColumns().add(colDolar);
        }
        
        // Aplicar configuración global a la tabla
        tablePredictiva.setStyle("-fx-background-color: white; -fx-table-cell-border-color: black;");
    }

    @FXML
    private void generarInforme() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(bundle.getString("simulador.paso6.informe.filtro"), "*.pdf"));
        fileChooser.setTitle(bundle.getString("simulador.paso6.informe.titulo"));
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try {
                boolean resultado = gramatica.generarInforme(file.getAbsolutePath());
                if (!resultado) {
                    mostrarAlerta(bundle.getString("simulador.paso6.error.titulo"), 
                                bundle.getString("simulador.paso6.error.informe"));
                }
            } catch (DocumentException e) {
                mostrarAlerta(bundle.getString("simulador.paso6.error.titulo"), 
                            bundle.getString("simulador.paso6.error.informe") + ": " + e.getMessage());
            }
        }
    }

    @FXML
    private void iniciarSimulacion() {
        if (panelSimuladorDesc == null) return;

        // Crear la instancia de SimulacionFinal
        SimulacionFinal simulacionFinal = new SimulacionFinal(
            gramatica,
            (TablaPredictivaPaso5) panelSimuladorDesc.getTablaPredictivaExtendidaGlobal(),
            panelSimuladorDesc.tabPane
        );

        // Crear una nueva pestaña y añadirla al TabPane principal
        Tab nuevaPestana = new Tab(bundle.getString("simulador.paso6.simulacion.final"));
        nuevaPestana.setContent(simulacionFinal);
        nuevaPestana.setClosable(true);

        panelSimuladorDesc.tabPane.getTabs().add(nuevaPestana);
        panelSimuladorDesc.tabPane.getSelectionModel().select(nuevaPestana);
    }

    @FXML
    private void modificarErrores() {
        // Volver a abrir el paso 5
        if (panelSimuladorDesc != null) {
            panelSimuladorDesc.cambiarPaso(4);
        }
    }

    public String actualizarVisualizacion() {
        StringBuilder cadena = new StringBuilder();
        for (Terminal terminal : cadenaEntrada) {
            cadena.append(terminal.getNombre()).append(" ");
        }
        return cadena.toString();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    @Override
    public Parent getRoot() {
        return this;
    }

    @Override
    public void actualizarTextos(ResourceBundle bundle) {
        this.bundle = bundle;
        // Actualizar textos de los títulos y secciones
        if (labelTitulo != null) labelTitulo.setText(bundle.getString("simulador.paso6.titulo"));
        if (labelProducciones != null) labelProducciones.setText(bundle.getString("simulador.paso6.producciones.titulo"));
        if (labelFuncionesError != null) labelFuncionesError.setText(bundle.getString("simulador.paso6.funciones.error.titulo"));
        if (labelTabla != null) labelTabla.setText(bundle.getString("simulador.paso6.tabla.titulo"));
        // Actualizar textos de los botones
        if (btnSimular != null) btnSimular.setText(bundle.getString("simulador.paso6.btn.simular"));
        if (btnModificarErrores != null) btnModificarErrores.setText(bundle.getString("simulador.paso6.btn.modificar.errores"));
        if (btnGenerarInforme != null) btnGenerarInforme.setText(bundle.getString("simulador.paso6.btn.generar.informe"));
        // Recargar datos para actualizar textos dinámicos y funciones de error
        cargarDatos();
    }

    private String getDescripcionFuncionError(FuncionError fe, ResourceBundle bundle) {
        StringBuilder string = new StringBuilder();
        string.append(fe.getIdentificador()).append(" - ");
        int accion = fe.getAccion();
        // Nombre internacionalizado de la acción
        string.append(bundle.getString(fe.getNombreAccion()));
        // Acciones que requieren símbolo
        if (accion == 1 || accion == 3 || accion == 4 || accion == 6) {
            if (fe.getSimbolo() != null) {
                string.append(": ").append(fe.getSimbolo().getNombre());
            }
        }
        return string.toString();
    }
}

