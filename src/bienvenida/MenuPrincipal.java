package bienvenida;

import editor.Editor;
import editor.EditorWindow;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;
import editor.ActualizableTextos;
import simulador.SimulacionFinal;
import editor.TabManager;
import gramatica.Gramatica;
import simulador.PanelSimuladorDesc;

public class MenuPrincipal extends Application {

    @FXML private TabPane tabPane;
    @FXML private Button btnCerrarTabs;
    @FXML private ComboBox<String> comboIdioma;
    @FXML private Button btnEditor;
    @FXML private Button btnSalir;
    @FXML private Button btnSimulador;
    @FXML private Button btnAyuda;
    @FXML private Button btnTutorial;
    @FXML private Label labelTitulo;
    @FXML private Label labelSubtitulo;
    @FXML private Label labelDesarrollado;
    private Tab lastSelectedTab;
    private ResourceBundle bundle;
    private Locale currentLocale = new Locale("es");

    @Override
    public void start(Stage primaryStage) {
        try {
            // Cargar el FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/MenuPrincipal.fxml"));
            loader.setController(this);
            Parent root = loader.load();
            
            // Configurar la escena
            Scene scene = new Scene(root);
            primaryStage.setTitle("SimAS 3.0");
            primaryStage.setScene(scene);
            
            // Configurar el tamaño de la ventana
            primaryStage.setWidth(800);
            primaryStage.setHeight(900);
            primaryStage.setMinWidth(600);
            primaryStage.setMinHeight(700);
            
            primaryStage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void initialize() {
        // Inicializar idiomas
        comboIdioma.getItems().addAll("Español", "English", "Français");
        comboIdioma.setValue("Español");
        comboIdioma.setOnAction(e -> cambiarIdioma());
        cargarBundle(currentLocale);
        
        // Guardar la última pestaña seleccionada
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                lastSelectedTab = newTab;
            }
        });
    }

    private void cambiarIdioma() {
        String idioma = comboIdioma.getValue();
        switch (idioma) {
            case "English":
                currentLocale = new Locale("en");
                break;
            case "Français":
                currentLocale = new Locale("fr");
                break;
            default:
                currentLocale = new Locale("es");
        }
        cargarBundle(currentLocale);
        actualizarTextos();
    }

    private void cargarBundle(Locale locale) {
        bundle = ResourceBundle.getBundle("messages", locale);
    }

    private void actualizarTextos() {
        try {
            // Actualizar ResourceBundle en TabManager
            TabManager.setResourceBundle(tabPane, bundle);
            
            // Actualizar textos de los botones principales
            if (btnEditor != null) btnEditor.setText(bundle.getString("btn.editor"));
            if (btnSalir != null) btnSalir.setText(bundle.getString("btn.salir"));
            if (btnSimulador != null) btnSimulador.setText(bundle.getString("btn.simulador"));
            if (btnAyuda != null) btnAyuda.setText(bundle.getString("btn.ayuda"));
            if (btnTutorial != null) btnTutorial.setText(bundle.getString("btn.tutorial"));
            if (btnCerrarTabs != null) {
                btnCerrarTabs.setText("✖");
                btnCerrarTabs.getTooltip().setText(bundle.getString("tooltip.cerrar"));
            }
            
            // Actualizar textos de las etiquetas
            if (comboIdioma != null && comboIdioma.getParent() != null) {
                Label labelIdioma = (Label) comboIdioma.getParent().getChildrenUnmodifiable().get(0);
                labelIdioma.setText(bundle.getString("label.idioma"));
            }
            if (labelTitulo != null) labelTitulo.setText(bundle.getString("label.titulo"));
            if (labelSubtitulo != null) labelSubtitulo.setText(bundle.getString("label.subtitulo"));
            if (labelDesarrollado != null) labelDesarrollado.setText(bundle.getString("label.desarrollado"));
            
            // Actualizar textos de las pestañas
            if (tabPane != null) {
                for (Tab tab : tabPane.getTabs()) {
                    // Título de la pestaña principal
                    if (tab.getText().equals("Menú Principal") || 
                        tab.getText().equals("Main Menu") || 
                        tab.getText().equals("Menu Principal")) {
                        tab.setText(bundle.getString("title.menu"));
                    }
                    // Actualizar textos de cualquier panel que implemente ActualizableTextos
                    if (tab.getContent() instanceof ActualizableTextos) {
                        ((ActualizableTextos) tab.getContent()).actualizarTextos(bundle);
                        // Si es Editor, actualiza el título
                        if (tab.getContent() instanceof editor.Editor) {
                            tab.setText(bundle.getString("editor.title"));
                        } else if (tab.getContent() instanceof editor.PanelSimbolosNoTerminales) {
                            tab.setText(bundle.getString("creacion2.tab.modificar.no.terminales"));
                        } else if (tab.getContent() instanceof editor.PanelSimbolosTerminales) {
                            tab.setText(bundle.getString("creacion2.tab.modificar.terminales"));
                        }
                    }
                }
                
                // Reasignar numeración de editores con nuevos títulos
                TabManager.reasignarNumerosGruposGramatica(tabPane);
            }
        } catch (Exception e) {
            System.err.println("Error al actualizar textos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onBtnCerrarTabsAction() {
        ButtonType btnCerrar = new ButtonType(bundle.getString("btn.cerrar"), ButtonBar.ButtonData.YES);
        ButtonType btnCancelar = new ButtonType(bundle.getString("button.cancelar"), ButtonBar.ButtonData.NO);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                bundle.getString("msg.confirmar.cerrar"),
                btnCerrar, btnCancelar);
        confirm.setTitle(bundle.getString("title.cerrar.pestanas"));
        confirm.setHeaderText(bundle.getString("title.cerrar.pestanas"));
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == btnCerrar) {
                tabPane.getSelectionModel().selectFirst();
                tabPane.getTabs().removeIf(Tab::isClosable);
            } else {
                if (lastSelectedTab != null) {
                    tabPane.getSelectionModel().select(lastSelectedTab);
                }
            }
        });
    }

    @FXML
    private void onBtnEditorAction() {
        // Establecer el ResourceBundle en TabManager para internacionalización
        TabManager.setResourceBundle(tabPane, bundle);
        
        // Crear un nuevo editor usando TabManager para posicionamiento correcto
        Editor editor = new Editor(tabPane, this, bundle);
        
        // CREAR NUEVO GRUPO: Editor independiente desde menú principal
        // parentId = editorId, childId = null → Esto creará un NUEVO GRUPO automáticamente
        Tab editorTab = TabManager.getOrCreateTab(tabPane, Editor.class, 
            bundle.getString("editor.title"), editor, editor.getEditorId(), null);
        
        // Asegurar que el editorId esté configurado como userData
        editorTab.setUserData(editor.getEditorId());
        
        // Reasignar numeración para reflejar los cambios
        TabManager.reasignarNumerosGruposGramatica(tabPane);
        
        System.out.println("MAIN MENU: Created new EDITOR with ID: " + editor.getEditorId());
    }

    @FXML
    private void onBtnAyudaAction() {
        File manual = new File("ManualDeUsuario.pdf");
        if (manual.exists()) {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(manual);
                } else {
                    onMostrarErrorAction(bundle.getString("msg.error.escritorio"));
                }
            } catch (IOException e) {
                onMostrarErrorAction(bundle.getString("msg.error.manual") + ": " + e.getMessage());
            }
        } else {
            onMostrarErrorAction(bundle.getString("msg.error.archivo"));
        }
    }

    @FXML
    private void onBtnTutorialAction() {
        File tutorial = new File("src/centroayuda/SimAS.html");
        if (tutorial.exists()) {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(tutorial.toURI());
                } else {
                    onMostrarErrorAction(bundle.getString("msg.error.escritorio"));
                }
            } catch (IOException e) {
                onMostrarErrorAction(bundle.getString("msg.error.tutorial") + ": " + e.getMessage());
            }
        } else {
            onMostrarErrorAction(bundle.getString("msg.error.archivo"));
        }
    }

    @FXML
    private void onBtnSimuladorAction() {
        // Implementar funcionalidad del simulador descendente directo
        cargarGramaticaYSimularDirectamente();
    }
    
    /**
     * Carga una gramática desde archivo y va directamente al paso 6 de la simulación.
     */
    private void cargarGramaticaYSimularDirectamente() {
        try {
            // Crear una nueva gramática
            Gramatica nuevaGramatica = new Gramatica();
            
            // Cargar gramática desde archivo (esto abrirá el selector de archivos)
            Gramatica gramaticaCargada = nuevaGramatica.cargarGramatica(null);
            
            if (gramaticaCargada != null) {
                // Validar la gramática cargada
                javafx.collections.ObservableList<String> errores = gramaticaCargada.validarGramatica();
                
                if (gramaticaCargada.getEstado() == 1) {
                    // Gramática válida - proceder con la simulación
                    crearSimuladorDirectoAlPaso6(gramaticaCargada);
                } else {
                    // Gramática inválida - mostrar errores
                    mostrarErroresValidacion(errores);
                }
            }
            // Si gramaticaCargada es null, significa que el usuario canceló la selección
            
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error", "No se pudo cargar la gramática: " + e.getMessage());
        }
    }
    
    /**
     * Crea un simulador y lo lleva directamente al paso 6.
     */
    private void crearSimuladorDirectoAlPaso6(Gramatica gramatica) {
        try {
            // Establecer el ResourceBundle en TabManager para internacionalización
            TabManager.setResourceBundle(tabPane, bundle);
            
            // Generar ID único para el simulador
            String simuladorId = "simulador_" + System.currentTimeMillis();
            
            // Crear el simulador descendente
            PanelSimuladorDesc simulador = new PanelSimuladorDesc(gramatica, tabPane, bundle, simuladorId);
            
            // Saltar directamente al paso 6 (índice 5)
            simulador.cambiarPaso(5);
            
            // Log para debug
            System.out.println("MAIN MENU: Created new SIMULATOR with ID: " + simuladorId);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Muestra los errores de validación de la gramática.
     */
    private void mostrarErroresValidacion(javafx.collections.ObservableList<String> errores) {
        StringBuilder mensaje = new StringBuilder(bundle.getString("editor.msg.validar.errores") + "\n\n");
        for (int i = 0; i < errores.size(); i++) {
            mensaje.append(i + 1).append(". ").append(errores.get(i)).append("\n\n");
        }
        
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error de Validación");
        alert.setHeaderText("La gramática seleccionada contiene errores");
        alert.setContentText(mensaje.toString());
        
        // Expandir el diálogo para mostrar todo el texto
        TextArea textArea = new TextArea(mensaje.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        javafx.scene.layout.GridPane.setVgrow(textArea, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.GridPane.setHgrow(textArea, javafx.scene.layout.Priority.ALWAYS);
        
        javafx.scene.layout.GridPane gridPane = new javafx.scene.layout.GridPane();
        gridPane.setMaxWidth(Double.MAX_VALUE);
        gridPane.add(textArea, 0, 0);
        
        alert.getDialogPane().setExpandableContent(gridPane);
        alert.showAndWait();
    }
    
    /**
     * Muestra un mensaje de error simple.
     */
    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    @FXML
    private void onMostrarErrorAction(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR, mensaje, ButtonType.OK);
        alerta.setTitle("Error");
        alerta.setHeaderText("Error al abrir el archivo");
        alerta.showAndWait();
    }

    @FXML
    private void onBtnSalirAction() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                bundle.getString("msg.confirmar.salir"),
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle(bundle.getString("title.menu"));
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                System.exit(0);
            }
        });
    }

    @FXML
    private void onBtnInfoAction() {
        Alert acercaDe = new Alert(Alert.AlertType.INFORMATION,
                "SimAS 3.0\nDesarrollado por Antonio.",
                ButtonType.OK);
        acercaDe.setTitle("Acerca de");
        acercaDe.showAndWait();
    }

    public void setBundle(ResourceBundle bundle) {
        this.bundle = bundle;
        actualizarTextos(bundle);
    }

    private void actualizarTextos(ResourceBundle bundle) {
        if (labelTitulo != null) labelTitulo.setText(bundle.getString("label.titulo"));
        if (labelSubtitulo != null) labelSubtitulo.setText(bundle.getString("label.subtitulo"));
        if (labelDesarrollado != null) labelDesarrollado.setText(bundle.getString("label.desarrollado"));
        if (btnEditor != null) btnEditor.setText(bundle.getString("btn.editor"));
        if (btnSimulador != null) btnSimulador.setText(bundle.getString("btn.simulador"));
        if (btnAyuda != null) btnAyuda.setText(bundle.getString("btn.ayuda"));
        if (btnTutorial != null) btnTutorial.setText(bundle.getString("btn.tutorial"));
        if (btnSalir != null) btnSalir.setText(bundle.getString("btn.salir"));
        if (btnCerrarTabs != null) btnCerrarTabs.setText(bundle.getString("btn.cerrar"));
    }

    public static void reasignarNumerosSimulaciones(TabPane tabPane) {
        if (tabPane == null) return;
        // ... (resto del código igual, pero usando el tabPane recibido)
    }
}
