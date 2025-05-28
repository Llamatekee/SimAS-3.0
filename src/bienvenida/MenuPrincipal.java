package bienvenida;

import editor.Editor;
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

public class MenuPrincipal {

    @FXML private TabPane tabPane;
    @FXML private Button btnCerrarTabs;
    @FXML private ComboBox<String> comboIdioma;
    @FXML private Button btnEditor;
    @FXML private Button btnSalir;
    private Tab lastSelectedTab;
    private ResourceBundle bundle;
    private Locale currentLocale = new Locale("es");

    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/MenuPrincipal.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("SimAS 3.0");

        // Configurar el tamaño de la ventana
        primaryStage.setWidth(800);
        primaryStage.setHeight(900);
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(700);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
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
            
            // Actualizar textos de las etiquetas
            if (comboIdioma != null && comboIdioma.getParent() != null) {
                Label labelIdioma = (Label) comboIdioma.getParent().getChildrenUnmodifiable().get(0);
                labelIdioma.setText(bundle.getString("label.idioma"));
            }
            
            // Actualizar título de la ventana
            if (btnEditor != null && btnEditor.getScene() != null) {
                Stage stage = (Stage) btnEditor.getScene().getWindow();
                stage.setTitle(bundle.getString("title.menu"));
            }
            
            // Actualizar textos de las pestañas
            if (tabPane != null) {
                for (Tab tab : tabPane.getTabs()) {
                    if (tab.getText().equals("Menú Principal")) {
                        tab.setText(bundle.getString("title.menu"));
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
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Seguro que quieres cerrar todas las pestañas menos la principal?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmación");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                // Seleccionar la pestaña del menú principal
                tabPane.getSelectionModel().selectFirst();
                // Cerrar todas las pestañas excepto la principal
                tabPane.getTabs().removeIf(Tab::isClosable);
            } else {
                // Volver a la pestaña anterior
                if (lastSelectedTab != null) {
                    tabPane.getSelectionModel().select(lastSelectedTab);
                }
            }
        });
    }

    @FXML
    private void onBtnEditorAction() {
        Editor editor = new Editor(tabPane, this);
        Tab editorTab = new Tab("Editor", editor.getRoot());
        editorTab.setClosable(true);
        tabPane.getTabs().add(editorTab);
        tabPane.getSelectionModel().select(editorTab);
    }

    @FXML
    private void onBtnAyudaAction() {
        File manual = new File("ManualDeUsuario.pdf");
        if (manual.exists()) {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(manual);
                } else {
                    onMostrarErrorAction("La funcionalidad de escritorio no está soportada en este sistema.");
                }
            } catch (IOException e) {
                onMostrarErrorAction("No se pudo abrir el manual de usuario: " + e.getMessage());
            }
        } else {
            onMostrarErrorAction("El archivo 'ManualDeUsuario.pdf' no se encuentra.");
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
                    onMostrarErrorAction("La funcionalidad de escritorio no está soportada en este sistema.");
                }
            } catch (IOException e) {
                onMostrarErrorAction("No se pudo abrir el tutorial: " + e.getMessage());
            }
        } else {
            onMostrarErrorAction("El archivo 'SimAS.html' no se encuentra.");
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
                "¿Estás seguro de que quieres salir?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Salir");
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
