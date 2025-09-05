package editor;

import bienvenida.MenuPrincipal;
import simulador.PanelSimuladorDesc;

import gramatica.Gramatica;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import utils.TabManager;
import utils.ActualizableTextos;

public class Editor extends VBox implements ActualizableTextos {

    // Modelo
    private Gramatica gramatica = crearGramatica();

    // Dependencias del sistema
    public TabPane tabPane;
    public MenuPrincipal menuPane;

    // Componentes inyectados desde el FXML
    @FXML private BorderPane rootPane;
    @FXML private Label labelTitulo;
    @FXML private Button btnAnadir;
    @FXML private Button btnAbrir;
    @FXML private Button btnGuardar;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;
    @FXML private Button btnValidar;
    @FXML private Button btnInforme;
    @FXML private Button btnSimular;
    @FXML private Button btnSalir;
    @FXML private TextField txtNombre;
    @FXML private TextField txtAreaDesc;
    @FXML private TextField txtSimInicial;
    @FXML private ListView<String> listNoTerminales;
    @FXML private ListView<String> listTerminales;
    @FXML private ListView<String> listProducciones;
    @FXML private Label labelPanelTitulo;
    @FXML private Label labelNombre;
    @FXML private Label labelSimboloInicial;
    @FXML private Label labelDescripcion;
    @FXML private Label labelNoTerminales;
    @FXML private Label labelTerminales;
    @FXML private Label labelProducciones;

    private ResourceBundle bundle;
    
    // Sistema de identificación para relaciones padre-hijo
    private String editorId;
    private static int contadorEditores = 0;
    private boolean listenerConfigured = false;

    // ==========================
    // CONSTRUCTORES
    // ==========================

    /**
     * Constructor vacio (requerido por JavaFX para cargar FXML).
     * ATENCION: Se deben asignar `tabPane` y `menuPane` despues de la carga.
     */
    public Editor() {
        this.gramatica = new Gramatica();
        this.editorId = "editor_" + System.currentTimeMillis() + "_" + (++contadorEditores);
        cargarFXML();
        configurarRelacionesPadreHijo();
    }

    /**
     * Constructor con TabPane y MenuPrincipal para uso manual.
     */
    public Editor(TabPane tabPane, MenuPrincipal menuPane) {
        this.tabPane = tabPane;
        this.menuPane = menuPane;
        this.gramatica = new Gramatica();
        this.editorId = "editor_" + System.currentTimeMillis() + "_" + (++contadorEditores);
        cargarFXML();
        configurarRelacionesPadreHijo();
    }

    /**
     * Constructor con gramatica preexistente.
     */
    public Editor(TabPane tabPane, Gramatica gramatica, MenuPrincipal menuPane) {
        this.tabPane = tabPane;
        this.menuPane = menuPane;
        this.gramatica = gramatica;
        this.editorId = "editor_" + System.currentTimeMillis() + "_" + (++contadorEditores);
        cargarFXML();
        configurarRelacionesPadreHijo();
    }

    /**
     * Constructor con TabPane, MenuPrincipal y ResourceBundle.
     */
    public Editor(TabPane tabPane, MenuPrincipal menuPane, ResourceBundle bundle) {
        this.tabPane = tabPane;
        this.menuPane = menuPane;
        this.gramatica = new Gramatica();
        this.bundle = bundle;
        this.editorId = "editor_" + System.currentTimeMillis() + "_" + (++contadorEditores);
        cargarFXML();
        configurarRelacionesPadreHijo();
    }

    // ==========================
    // MÉTODOS DE INICIALIZACIÓN
    // ==========================
    
    /**
     * Configura las relaciones padre-hijo para cerrar pestanas hijas cuando se cierre el editor.
     */
    public void configurarRelacionesPadreHijo() {
        if (tabPane != null && !listenerConfigured) {
            // Configurar listener para detectar cuando se cierran pestañas
            tabPane.getTabs().addListener((javafx.collections.ListChangeListener.Change<? extends Tab> change) -> {
                while (change.next()) {
                    if (change.wasRemoved()) {
                        for (Tab tab : change.getRemoved()) {
                            if (tab.getContent() == this && tab.getUserData() != null) {
                                String elementId = tab.getUserData().toString();
                                // Cerrar las pestañas hijas
                                TabManager.closeChildTabs(tabPane, elementId);
                                // Forzar renumeración de grupos
                                TabManager.reasignarNumerosGruposGramatica(tabPane);
                            }
                        }
                    }
                }
            });
            
            // Buscar la pestaña que contiene este editor
            for (Tab tab : tabPane.getTabs()) {
                if (tab.getContent() == this && tab.getUserData() != null) {
                    String elementId = tab.getUserData().toString();
                    // Registrar las relaciones padre-hijo existentes
                    Map<String, List<Tab>> relations = TabManager.getParentChildRelations(tabPane);
                    if (!relations.containsKey(elementId)) {
                        relations.put(elementId, new ArrayList<>());
                    }
                    break;
                }
            }
            
            listenerConfigured = true;
        }
    }

    /**
     * Carga la interfaz desde el archivo FXML.
     */
    private void cargarFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/Editor.fxml"));
            loader.setController(this);
            loader.setResources(bundle);
            Parent root = loader.load();
            this.getChildren().setAll(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Obtiene el ID unico de este editor.
     */
    public String getEditorId() {
        return editorId;
    }

    public Gramatica getGramatica() {
        return this.gramatica;
    }

    public void setGramatica(Gramatica gramatica) {
        this.gramatica = gramatica;
    }

    // Método de inicialización; se invoca automáticamente tras cargar el FXML.
    @FXML
    public void initialize() {
        actualizarTextos(bundle);
        configurarTooltips();
        // Configurar la vista según sea necesario
        btnEditar.setDisable(true);
        btnEliminar.setDisable(true);
        btnGuardar.setDisable(true);
        btnValidar.setDisable(true);
        btnInforme.setDisable(true);
        btnSimular.setDisable(true);
    }

    // Métodos para inyectar dependencias (si se crean desde MenuPrincipal)
    public void setTabPane(TabPane tabPane) {
        this.tabPane = tabPane;
        // Configurar relaciones padre-hijo si aún no se ha hecho
        configurarRelacionesPadreHijo();
    }

    public void setMenuPane(MenuPrincipal menuPane) {
        this.menuPane = menuPane;
    }

    // Método para obtener el nodo raíz del editor (para incluirlo en una pestaña)
    public Parent getRoot() {
        return rootPane;
    }

    // Método auxiliar para crear una gramática vacía
    private Gramatica crearGramatica() {
        return new Gramatica();
    }

    // ===============================
    // Métodos de manejo de eventos
    // ===============================

    @FXML
    private void onBtnAnadirAction() {
        // Crear una nueva pestaña de creación como hija del editor
        String childId = "creacion_" + editorId.replace("editor_", "");
        PanelCreacionGramatica asistente = new PanelCreacionGramatica(this, tabPane, null, menuPane, childId);
        TabManager.getOrCreateTab(tabPane, PanelCreacionGramatica.class, 
            bundle.getString("creacion.tab.paso1"), asistente, editorId, childId);
    }

    @FXML
    private void onBtnAbrirAction() {
        cargarGramatica();
    }

    @FXML
    private void onBtnGuardarAction() {
        grabarGramatica();
    }

    @FXML
    private void onBtnEditarAction() {
        if (this.tabPane == null || this.menuPane == null) {
            mostrarError("Error", "No se pueden iniciar la edición en este momento.");
            return;
        }
        // Crear una nueva pestaña de edición como hija del editor
        String childId = "creacion_" + editorId.replace("editor_", "");
        PanelCreacionGramatica asistente = new PanelCreacionGramatica(this, tabPane, this.gramatica, menuPane, childId);
        TabManager.getOrCreateTab(tabPane, PanelCreacionGramatica.class, 
            bundle.getString("creacion.tab.paso1"), asistente, editorId, childId);
    }

    @FXML
    private void onBtnEliminarAction() {
        ButtonType btnSi = new ButtonType(bundle.getString("button.si"), ButtonBar.ButtonData.YES);
        ButtonType btnNo = new ButtonType(bundle.getString("button.no"), ButtonBar.ButtonData.NO);
        Alert confirm = new Alert(AlertType.CONFIRMATION, 
            bundle.getString("editor.msg.confirmar.eliminar"), 
            btnSi, btnNo);
        confirm.setTitle(bundle.getString("editor.dialog.eliminar.titulo"));
        confirm.setHeaderText(bundle.getString("editor.header.eliminar"));
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == btnSi) {
            this.gramatica = new Gramatica();
            actualizarVisualizacion();
        }
    }

    @FXML
    private void onBtnValidarAction() {
        validarGramatica(this.gramatica);
    }

    @FXML
    private void onBtnInformeAction() {
        generarInformePDF();
    }

    @FXML
    private void onBtnSimularAction() {
        if (this.tabPane == null || this.menuPane == null) {
            mostrarError("Error", "No se puede iniciar la simulación en este momento.");
            return;
        }
        
        // Solo validar si no está ya validada
        if (this.gramatica.getEstado() != 1) {
            validarGramatica(this.gramatica);
            if (this.gramatica.getEstado() != 1) {
                return;
            }
        }
        
        // Verificar si ya existe un simulador para este editor
        String simuladorId = "simulador_" + editorId.replace("editor_", "");
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() != null && tab.getUserData().toString().equals(simuladorId)) {
                // Si existe, mostrar mensaje y seleccionar la pestaña
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle(bundle.getString("editor.simulador.existente.titulo"));
                alert.setHeaderText(null);
                alert.setContentText(bundle.getString("editor.simulador.existente.mensaje"));
                alert.showAndWait();
                tabPane.getSelectionModel().select(tab);
                return;
            }
        }
        
        // Crear una copia de la gramática para el simulador (para no modificar la original del editor)
        Gramatica gramaticaParaSimulador = new Gramatica(this.gramatica);

        // Crear un nuevo simulador como hijo del editor con la copia de la gramática
        PanelSimuladorDesc simulador = new PanelSimuladorDesc(gramaticaParaSimulador, this.tabPane, this.menuPane, simuladorId, bundle);
        
        // Asignar el simulador al mismo grupo que el editor
        TabManager.asignarSimuladorAGrupoDeEditor(tabPane, simuladorId, editorId);
        
        // Crear la pestaña del simulador con el título correcto (Asistente Simulador)
        String tituloBase = bundle.getString("simulador.asistente");
        int numeroGrupo = TabManager.obtenerNumeroGrupo(tabPane, simuladorId);
        String tituloFinal = numeroGrupo > 0 ? numeroGrupo + "-" + tituloBase : tituloBase;
        
        // Crear la pestaña y mostrar el primer paso
        Tab tab = new Tab(tituloFinal);
        tab.setContent(simulador.getRoot());
        tab.setUserData(simuladorId);
        
        // Insertar la pestaña en la posición correcta
        int posicion = TabManager.calcularPosicionInsercion(tabPane, PanelSimuladorDesc.class, editorId, simuladorId);
        tabPane.getTabs().add(posicion, tab);
        tabPane.getSelectionModel().select(tab);
    }

    @FXML
    private void onBtnSalirAction() {
        ButtonType btnSi = new ButtonType(bundle.getString("button.si"), ButtonBar.ButtonData.YES);
        ButtonType btnNo = new ButtonType(bundle.getString("button.no"), ButtonBar.ButtonData.NO);
        Alert confirm = new Alert(AlertType.CONFIRMATION,
                bundle.getString("msg.confirmar.salir"),
                btnSi, btnNo);
        confirm.setTitle(bundle.getString("editor.header.salir"));
        confirm.setHeaderText(bundle.getString("editor.header.salir"));
        confirm.showAndWait().ifPresent(response -> {
            if (response == btnSi) {
                Stage stage = (Stage) rootPane.getScene().getWindow();
                stage.close();
            }
        });
    }

    // ===============================
    // Métodos auxiliares (adaptados o comentados)
    // ===============================

    public void cargarGramatica() {
        if (this.gramatica == null) {
            this.gramatica = new Gramatica();  // Crear una nueva instancia si era null
        }

        Gramatica gr = this.gramatica.cargarGramatica(null);

        if (gr != null) {
            this.gramatica = gr;
            actualizarVisualizacion();
            validarGramatica(gramatica);
        } else {
            this.gramatica = null; // Si no se carga una nueva gramática, aseguramos que quede en null
            actualizarVisualizacion();
        }
    }

    public void grabarGramatica() {
        int i = gramatica.guardarGramatica(null); // Pasar null como ownerWindow
        if (i == 1) {
            Alert alert = new Alert(AlertType.INFORMATION, bundle.getString("editor.msg.guardar.exito"), ButtonType.OK);
            alert.setTitle(bundle.getString("editor.dialog.guardar.titulo"));
            alert.showAndWait();
        }
    }

    private void actualizarEstadoBotones() {
        boolean hayGramatica = (gramatica != null && gramatica.getNombre() != null && !gramatica.getNombre().isEmpty());

        btnEditar.setDisable(!hayGramatica);
        btnEliminar.setDisable(!hayGramatica);
        btnGuardar.setDisable(!hayGramatica);
        btnValidar.setDisable(!hayGramatica);
        btnInforme.setDisable(!hayGramatica);
        btnSimular.setDisable(!hayGramatica);
    }

    public void actualizarVisualizacion() {
        if (this.gramatica != null) {
            listNoTerminales.setItems(gramatica.getNoTerminalesModel());
            listTerminales.setItems(gramatica.getTerminalesModel());
            listProducciones.setItems(gramatica.getProduccionesModel());
            txtNombre.setText(gramatica.getNombre());
            txtAreaDesc.setText(gramatica.getDescripcion());
            txtSimInicial.setText(gramatica.getSimbInicial());
        } else {
            // Limpiar campos si no hay gramática
            listNoTerminales.setItems(FXCollections.observableArrayList());
            listTerminales.setItems(FXCollections.observableArrayList());
            listProducciones.setItems(FXCollections.observableArrayList());
            txtNombre.clear();
            txtAreaDesc.clear();
            txtSimInicial.clear();
        }

        actualizarEstadoBotones(); // Llamamos para activar/desactivar los botones segun corresponda
    }

    public void validarGramatica(Gramatica gramatica) {
        ObservableList<String> mensajesError = gramatica.validarGramatica();
        int estadoValidacion = gramatica.getEstado();

        if (estadoValidacion == 1) {
            gramatica.setEstado(1);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(bundle.getString("editor.dialog.validar.titulo"));
            alert.setHeaderText(null);
            alert.setContentText(bundle.getString("editor.msg.validar.exito"));
            alert.showAndWait();
        } else {
            gramatica.setEstado(-1);

            // Construcción del mensaje sin etiquetas HTML
            StringBuilder mensajeError = new StringBuilder(bundle.getString("editor.msg.validar.errores") + "\n\n");
            for (int i = 0; i < mensajesError.size(); i++) {
                mensajeError.append(i + 1).append(". ").append(mensajesError.get(i)).append("\n\n");
            }

            // Crear un alert de error con el formato correcto
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(bundle.getString("editor.dialog.validar.error.titulo"));
            alert.setHeaderText(bundle.getString("editor.dialog.validar.error.header"));
            alert.setContentText(mensajeError.toString());

            // Hacer que la alerta muestre todo el texto correctamente si es muy largo
            TextArea textArea = new TextArea(mensajeError.toString());
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);

            GridPane gridPane = new GridPane();
            gridPane.setMaxWidth(Double.MAX_VALUE);
            gridPane.add(textArea, 0, 0);

            alert.getDialogPane().setExpandableContent(gridPane);
            alert.showAndWait();
        }
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void generarInformePDF() {
        if (this.gramatica == null || this.gramatica.getNombre() == null || this.gramatica.getNombre().isEmpty()) {
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
        String nombreArchivo = this.gramatica.generarNombreArchivoPDF("editor", this.bundle);
        fileChooser.setInitialFileName(nombreArchivo);

        // Mostrar diálogo de guardado
        File archivo = fileChooser.showSaveDialog(rootPane.getScene().getWindow());
        if (archivo == null) {
            return; // Usuario canceló
        }

        try {
            // Generar el informe usando el método de la clase Gramatica
            boolean exito = this.gramatica.generarInforme(archivo.getAbsolutePath(), bundle);
            
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
                alert.setContentText(bundle.getString("editor.informe.error.gramatica.no.validada"));
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

    public void actualizarTextos(ResourceBundle nuevoBundle) {
        this.bundle = nuevoBundle;
        try {
            // Actualizar título
            if (labelTitulo != null) {
                labelTitulo.setText(bundle.getString("editor.title"));
            }
            // Actualizar labels de secciones y campos
            if (labelPanelTitulo != null) labelPanelTitulo.setText(bundle.getString("editor.label.titulo"));
            if (labelNombre != null) labelNombre.setText(bundle.getString("editor.label.nombre"));
            if (labelSimboloInicial != null) labelSimboloInicial.setText(bundle.getString("editor.label.simb.inicial"));
            if (labelDescripcion != null) labelDescripcion.setText(bundle.getString("editor.label.descripcion"));
            if (labelNoTerminales != null) labelNoTerminales.setText(bundle.getString("editor.label.no.terminales"));
            if (labelTerminales != null) labelTerminales.setText(bundle.getString("editor.label.terminales"));
            if (labelProducciones != null) labelProducciones.setText(bundle.getString("editor.label.producciones"));
            // Actualizar textos de los botones
            if (btnAnadir != null) btnAnadir.setText(bundle.getString("editor.btn.nueva"));
            if (btnAbrir != null) btnAbrir.setText(bundle.getString("editor.btn.abrir"));
            if (btnGuardar != null) btnGuardar.setText(bundle.getString("editor.btn.guardar"));
            if (btnEditar != null) btnEditar.setText(bundle.getString("editor.btn.editar"));
            if (btnEliminar != null) btnEliminar.setText(bundle.getString("editor.btn.eliminar"));
            if (btnValidar != null) btnValidar.setText(bundle.getString("editor.btn.validar"));
            if (btnInforme != null) btnInforme.setText(bundle.getString("editor.btn.informe"));
            if (btnSimular != null) btnSimular.setText(bundle.getString("editor.btn.simular"));
            if (btnSalir != null) btnSalir.setText(bundle.getString("btn.salir"));
            
            // Actualizar tooltips de los botones
            configurarTooltips();
            
            // Actualizar título de la ventana
            if (rootPane != null && rootPane.getScene() != null) {
                Stage stage = (Stage) rootPane.getScene().getWindow();
                stage.setTitle(bundle.getString("editor.title"));
            }

            // Actualizar el simulador si existe
            if (tabPane != null) {
                for (Tab tab : tabPane.getTabs()) {
                    Object userData = tab.getUserData();
                    if (userData instanceof simulador.PanelSimuladorDesc) {
                        ((simulador.PanelSimuladorDesc) userData).setBundle(bundle);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ResourceBundle getBundle() {
        return bundle;
    }

    public void setBundle(ResourceBundle bundle) {
        this.bundle = bundle;
        actualizarTextos(bundle);
    }
    
    /**
     * Configura los tooltips para todos los botones del editor
     */
    private void configurarTooltips() {
        if (bundle != null) {
            btnAnadir.setTooltip(new Tooltip(bundle.getString("editor.tooltip.nueva")));
            btnAbrir.setTooltip(new Tooltip(bundle.getString("editor.tooltip.abrir")));
            btnGuardar.setTooltip(new Tooltip(bundle.getString("editor.tooltip.guardar")));
            btnEditar.setTooltip(new Tooltip(bundle.getString("editor.tooltip.editar")));
            btnEliminar.setTooltip(new Tooltip(bundle.getString("editor.tooltip.eliminar")));
            btnValidar.setTooltip(new Tooltip(bundle.getString("editor.tooltip.validar")));
            btnInforme.setTooltip(new Tooltip(bundle.getString("editor.tooltip.informe")));
            btnSimular.setTooltip(new Tooltip(bundle.getString("editor.tooltip.simular")));
            btnSalir.setTooltip(new Tooltip(bundle.getString("editor.tooltip.salir")));
        }
    }
}