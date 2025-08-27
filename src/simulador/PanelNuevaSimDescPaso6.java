package simulador;

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
import editor.TabManager;
import java.util.MissingResourceException;

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
        
        // Configurar estilos CSS
        this.getStylesheets().add(getClass().getResource("/vistas/styles2.css").toExternalForm());
        
        // Cargar UI y datos
        cargarFXML();
        cargarDatos();
    }

    // Constructor anterior para compatibilidad
    public PanelNuevaSimDescPaso6(Gramatica gramatica) {
        this.gramatica = gramatica;
        this.panelSimuladorDesc = null;
        this.bundle = ResourceBundle.getBundle("messages");
        
        // Usar la tabla predictiva de la gramática
        this.tablaPredictiva = gramatica.getTPredictiva();
        
        // Obtener las funciones de error
        this.funcionesError = tablaPredictiva.getFuncionesError();
        
        // Inicializar componentes
        this.producciones = FXCollections.observableArrayList();
        this.cadenaEntrada = FXCollections.observableArrayList();
        
        // Configurar estilos CSS
        this.getStylesheets().add(getClass().getResource("/vistas/styles2.css").toExternalForm());
        
        // Cargar UI y datos
        cargarFXML();
        cargarDatos();
    }

    private void cargarFXML() {
        try {
            if (bundle == null) {
                // Si no hay bundle, usar el bundle por defecto
                bundle = ResourceBundle.getBundle("messages");
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/PanelNuevaSimDescPaso6.fxml"));
            loader.setController(this);
            loader.setResources(bundle);
            GridPane root = loader.load();
            
            // Configurar el GridPane para que ocupe todo el espacio disponible
            root.setPrefWidth(Double.MAX_VALUE);
            root.setPrefHeight(Double.MAX_VALUE);
            root.setMaxWidth(Double.MAX_VALUE);
            root.setMaxHeight(Double.MAX_VALUE);
            
            // Establecer el GridPane como contenido principal
            this.setCenter(root);
            
            // Actualizar los textos después de cargar el FXML
            actualizarTextos(bundle);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cargarDatos() {
        try {
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
                errores.add(bundle != null ? 
                    bundle.getString("simulador.paso6.error.sin.funciones") : 
                    "No se están usando funciones de error");
            }
            listFuncionesError.setItems(errores);

            // Cargar tabla predictiva
            if (tablaPredictiva instanceof TablaPredictivaPaso5) {
                
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
        } catch (MissingResourceException e) {
            e.printStackTrace();
        }
    }

    /**
     * Crea las columnas manualmente para la tabla predictiva.
     */
    private void crearColumnasManualmente() {
        // Columna para símbolos
        String tituloSimbolo = bundle != null ? bundle.getString("simulador.paso6.tabla.columna.simbolo") : "Símbolo";
        TableColumn<FilaTablaPredictiva, String> colSimbolo = new TableColumn<>(tituloSimbolo);
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
                        getStyleClass().clear();
                        return;
                    }
                    
                    setText(item);
                    
                                            // Aplicar clases CSS para columna de símbolos (mismo estilo que paso 5)
                        getStyleClass().clear();
                        if (isSelected()) {
                            getStyleClass().add("selected-cell");
                        } else {
                            getStyleClass().add("symbol-cell");
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
                            getStyleClass().clear();
                            return;
                        }
                        
                        setText(item);
                        
                        // Aplicar clases CSS según el tipo de contenido (mismo estilo que paso 5)
                        getStyleClass().clear();
                        if (isSelected()) {
                            getStyleClass().add("selected-cell");
                        } else if (item != null && !item.isEmpty() && item.startsWith("E")) {
                            // Estilo para funciones de error
                            getStyleClass().add("error-cell");
                        } else if (item != null && !item.isEmpty() && Character.isDigit(item.charAt(0))) {
                            // Estilo para producciones
                            getStyleClass().add("production-cell");
                        } else if (item != null && !item.isEmpty() && item.equals("ε")) {
                            // Estilo para epsilon
                            getStyleClass().add("epsilon-cell");
                        } else if (item != null && !item.isEmpty()) {
                            // Estilo para otros contenidos
                            getStyleClass().add("default-cell");
                        } else {
                            // Estilo para celdas vacías
                            getStyleClass().add("empty-cell");
                        }
                    }
                };
            });
            
            // Añadir la columna
            tablePredictiva.getColumns().add(colT);
        }
        
        // Añadir columna para $ si no existe
        boolean existeDolar = false;
        for (TableColumn<FilaTablaPredictiva, ?> column : tablePredictiva.getColumns()) {
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
                            getStyleClass().clear();
                            return;
                        }
                        
                        setText(item);
                        
                        // Aplicar clases CSS según el tipo de contenido (mismo estilo que paso 5)
                        getStyleClass().clear();
                        if (isSelected()) {
                            getStyleClass().add("selected-cell");
                        } else if (item != null && !item.isEmpty() && item.startsWith("E")) {
                            // Estilo para funciones de error
                            getStyleClass().add("error-cell");
                        } else if (item != null && !item.isEmpty() && Character.isDigit(item.charAt(0))) {
                            // Estilo para producciones
                            getStyleClass().add("production-cell");
                        } else if (item != null && !item.isEmpty() && item.equals("ε")) {
                            // Estilo para epsilon
                            getStyleClass().add("epsilon-cell");
                        } else if (item != null && !item.isEmpty()) {
                            // Estilo para otros contenidos
                            getStyleClass().add("default-cell");
                        } else {
                            // Estilo para celdas vacías
                            getStyleClass().add("empty-cell");
                        }
                    }
                };
            });
            
            tablePredictiva.getColumns().add(colDolar);
        }
        
        // Aplicar configuración global a la tabla
        // Los estilos se manejan a través de CSS en styles2.css
    }

    @FXML
    private void generarInforme() {
        if (this.gramatica == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(bundle.getString("editor.informe.error.titulo"));
            alert.setHeaderText(null);
            alert.setContentText(bundle.getString("editor.informe.error.sin.gramatica"));
            alert.showAndWait();
            return;
        }

        // Crear y configurar el FileChooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(bundle.getString("editor.informe.guardar.titulo"));
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Documentos PDF", "*.pdf")
        );
        
        // Sugerir nombre de archivo basado en el nombre del archivo fuente
        String nombreArchivo = this.gramatica.generarNombreArchivoPDF("simulador", this.bundle);
        fileChooser.setInitialFileName(nombreArchivo);

        // Mostrar diálogo de guardado
        File archivo = fileChooser.showSaveDialog(this.getScene().getWindow());
        if (archivo == null) {
            return; // Usuario canceló
        }

        try {
            // Obtener la gramática original del simulador
            Gramatica gramaticaOriginal = null;
            if (panelSimuladorDesc != null) {
                gramaticaOriginal = panelSimuladorDesc.getGramaticaOriginal();
            } else {
                gramaticaOriginal = this.gramatica;
            }
            
            // Generar el informe del simulador
            boolean exito = this.gramatica.generarInformeSimulador(
                archivo.getAbsolutePath(), 
                gramaticaOriginal, 
                this.tablaPredictiva, 
                this.funcionesError, 
                bundle
            );
            
            if (exito) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(bundle.getString("editor.informe.exito.titulo"));
                alert.setHeaderText(null);
                alert.setContentText(bundle.getString("editor.informe.exito.mensaje") + "\n" + archivo.getAbsolutePath());
                alert.showAndWait();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(bundle.getString("editor.informe.error.titulo"));
                alert.setHeaderText(null);
                alert.setContentText(bundle.getString("editor.informe.error.generacion"));
                alert.showAndWait();
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(bundle.getString("editor.informe.error.titulo"));
            alert.setHeaderText(null);
            alert.setContentText(bundle.getString("editor.informe.error.generacion") + "\n" + e.getMessage());
            alert.showAndWait();
            e.printStackTrace();
        }
    }

    @FXML
    private void iniciarSimulacion() {
        if (panelSimuladorDesc == null) return;

        // Crear la instancia de SimulacionFinal
        SimulacionFinal simulacionFinal = new SimulacionFinal(
            gramatica,
            (TablaPredictivaPaso5) panelSimuladorDesc.getTablaPredictivaExtendidaGlobal(),
            panelSimuladorDesc.tabPane,
            panelSimuladorDesc.getBundle()
        );

        // Obtener el grupo del simulador
        String simuladorId = panelSimuladorDesc.getSimuladorId();
        String grupoId = TabManager.obtenerGrupoDeElemento(panelSimuladorDesc.tabPane, simuladorId);
        int numeroGrupo = TabManager.obtenerNumeroGrupo(panelSimuladorDesc.tabPane, simuladorId);
        
        // Asignar el grupo a la simulación
        simulacionFinal.setGrupoId(grupoId);
        simulacionFinal.setNumeroGrupo(numeroGrupo);
        
        // Construir el título base
        String tituloBase = bundle.getString("simulador.paso6.simulacion");
        
        // Añadir el número de grupo si es válido y hay múltiples grupos
        boolean hayMultiplesGrupos = TabManager.contarGruposActivos(panelSimuladorDesc.tabPane) > 1;
        String tituloFinal = (numeroGrupo > 0 && hayMultiplesGrupos) ? 
            numeroGrupo + "-" + tituloBase : 
            tituloBase;

        // Generar un simulacionId único usando timestamp
        String simulacionId = "simulacion_" + simuladorId + "_" + System.currentTimeMillis();
        
        // Usar TabManager para crear la pestaña como hija del simulador
        Tab nuevaPestana = TabManager.getOrCreateTab(
            panelSimuladorDesc.tabPane,
            SimulacionFinal.class,
            tituloFinal,
            simulacionFinal,
            simuladorId,
            simulacionId
        );
        
        // Asignar el simulacionId correcto a la simulación
        simulacionFinal.setSimulacionId(simulacionId);
        
        // Seleccionar la nueva pestaña
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

    @Override
    public Parent getRoot() {
        return this;
    }

    @Override
    public void actualizarTextos(ResourceBundle bundle) {
        if (bundle == null) return;
        
        try {
            this.bundle = bundle;
            // Actualizar textos de los títulos y secciones
            if (labelTitulo != null) labelTitulo.setText(bundle.getString("simulador.paso6.titulo"));
            if (labelProducciones != null) labelProducciones.setText(bundle.getString("simulador.paso6.producciones.titulo"));
            if (labelFuncionesError != null) labelFuncionesError.setText(bundle.getString("simulador.paso6.funciones.error.titulo"));
            if (labelTabla != null) labelTabla.setText(bundle.getString("simulador.paso6.tabla.titulo"));
            // Actualizar textos de los botones
            if (btnSimular != null) btnSimular.setText(bundle.getString("simulador.paso6.btn.simular"));
            if (btnModificarErrores != null) btnModificarErrores.setText(bundle.getString("simulador.paso6.btn.editar"));
            if (btnGenerarInforme != null) btnGenerarInforme.setText(bundle.getString("simulacionfinal.btn.informe.pdf"));
            // Actualizar columna de símbolos si existe
            if (tablePredictiva != null && !tablePredictiva.getColumns().isEmpty()) {
                TableColumn<?, ?> colSimbolo = tablePredictiva.getColumns().get(0);
                if (colSimbolo != null) {
                    colSimbolo.setText(bundle.getString("simulador.paso6.tabla.columna.simbolo"));
                }
            }
            // Recargar datos para actualizar textos dinámicos y funciones de error
            cargarDatos();
        } catch (MissingResourceException e) {
            // Si falta alguna clave, intentar usar valores por defecto
            if (labelProducciones != null) labelProducciones.setText("Producciones");
            if (labelFuncionesError != null) labelFuncionesError.setText("Funciones de Error");
            if (labelTabla != null) labelTabla.setText("Tabla Predictiva");
        }
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

