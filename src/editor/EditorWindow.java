package editor;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import java.util.ResourceBundle;

public class EditorWindow {
    private Stage stage;
    private TabPane tabPane;
    private ResourceBundle bundle;

    public EditorWindow(ResourceBundle bundle) {
        this.bundle = bundle;
        initialize();
    }

    private void initialize() {
        stage = new Stage();
        tabPane = new TabPane();
        
        // Configurar la ventana
        stage.setTitle(bundle.getString("editor.title"));
        stage.setWidth(800);
        stage.setHeight(900);
        stage.setMinWidth(600);
        stage.setMinHeight(700);

        // Crear la escena
        Scene scene = new Scene(tabPane);
        stage.setScene(scene);
    }

    public void show() {
        stage.show();
    }

    public void addEditor(Editor editor) {
        Tab editorTab = new Tab(bundle.getString("editor.title"), editor);
        editorTab.setClosable(true);
        tabPane.getTabs().add(editorTab);
        tabPane.getSelectionModel().select(editorTab);
    }

    public TabPane getTabPane() {
        return tabPane;
    }
} 