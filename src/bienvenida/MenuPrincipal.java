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
        // Verificar si ya existe un editor en la ventana principal
        boolean editorExists = false;
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getContent() instanceof Editor) {
                editorExists = true;
                break;
            }
        }

        if (editorExists) {
            // Si ya existe un editor, abrir en una nueva ventana
            EditorWindow newWindow = new EditorWindow(bundle);
            Editor newEditor = new Editor(newWindow.getTabPane(), null, bundle);
            newWindow.addEditor(newEditor);
            newWindow.show();
            // Asegurar que la nueva ventana tenga el título correcto
            Stage stage = (Stage) newWindow.getTabPane().getScene().getWindow();
            stage.setTitle("SimAS 3.0");
        } else {
            // Si no existe un editor, abrir en la ventana actual
            Editor editor = new Editor(tabPane, this, bundle);
            Tab editorTab = new Tab(bundle.getString("editor.title"), editor);
            editorTab.setClosable(true);
            tabPane.getTabs().add(editorTab);
            tabPane.getSelectionModel().select(editorTab);
        }
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
}
