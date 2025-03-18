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

public class MenuPrincipal {

    @FXML
    private TabPane tabPane;

    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/MenuPrincipal.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("SimAS 3.0");

        // 🔹 Establecer tamaño inicial
        primaryStage.setWidth(800);
        primaryStage.setHeight(800);

        // 🔹 Evitar que la ventana se reduzca más allá de estos valores
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(400);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @FXML
    private void initialize() {
        Tab closeTab = tabPane.getTabs().get(0);
        Tab mainMenuTab = tabPane.getTabs().get(1);

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == closeTab) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "¿Seguro que quieres cerrar todas las pestañas menos la principal?",
                        ButtonType.YES, ButtonType.NO);
                confirm.setTitle("Confirmación");
                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.YES) {
                        tabPane.getSelectionModel().select(mainMenuTab);
                        onBtnCerrarPestanasAction();
                    } else {
                        tabPane.getSelectionModel().select(oldTab);
                    }
                });
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
    private void onBtnCerrarPestanasAction() {
        tabPane.getTabs().removeIf(Tab::isClosable);
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
