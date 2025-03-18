package editor;

import bienvenida.MenuPrincipal;
import gramatica.Gramatica;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class PanelCreacionGramaticaPaso2 extends VBox {

    public final PanelCreacionGramatica panelPadre;
    public final MenuPrincipal menuPane;
    public final TabPane tabPane;

    @FXML private ListView<String> listNoTerminales;
    @FXML private ListView<String> listTerminales;

    private final ObservableList<String> simbolosNoTerminales = FXCollections.observableArrayList();
    private final ObservableList<String> simbolosTerminales = FXCollections.observableArrayList();

    public PanelCreacionGramaticaPaso2(PanelCreacionGramatica panelPadre, MenuPrincipal menuPane, TabPane tabPane) {
        this.panelPadre = panelPadre;
        this.menuPane = menuPane;
        this.tabPane = tabPane;
        cargarFXML();
    }

    private void cargarFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/PanelCreacionGramaticaPaso2.fxml"));
            loader.setController(this);
            Parent root = loader.load();
            this.getChildren().setAll(root); // 🔹 Sustituye todo el contenido actual por el del FXML sin tocar el TabPane
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void initialize() {
        listNoTerminales.setItems(simbolosNoTerminales);
        listTerminales.setItems(simbolosTerminales);

        // Configurar visualización de los elementos correctamente
        listNoTerminales.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? "" : item);
            }
        });

        listTerminales.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? "" : item);
            }
        });

        cargarDatosDesdeGramatica();
    }

    /**
     * 🔹 Carga los datos desde la gramática activa.
     */
    private void cargarDatosDesdeGramatica() {
        Gramatica gramatica = panelPadre.getGramatica();
        if (gramatica != null) {
            simbolosNoTerminales.setAll(gramatica.getNoTerminalesModel());
            simbolosTerminales.setAll(gramatica.getTerminalesModel());
        }
    }

    @FXML
    private void onBtnSiguienteAction() {
        panelPadre.cambiarPaso(3);
    }

    @FXML
    private void onBtnAnteriorAction() {
        panelPadre.cambiarPaso(1);
    }

    @FXML
    private void onBtnCancelarAction() {
        panelPadre.cancelarEdicion();
    }

    @FXML
    private void onBtnModificarNoTerminalesAction() {
        PanelSimbolosNoTerminales panel = new PanelSimbolosNoTerminales(simbolosNoTerminales, tabPane, this);
        Tab tab = new Tab("Modificar No Terminales", panel);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    @FXML
    private void onBtnModificarTerminalesAction() {
        PanelSimbolosTerminales panel = new PanelSimbolosTerminales(simbolosTerminales, tabPane, this);
        Tab tab = new Tab("Modificar Terminales", panel);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    @FXML
    private void onBtnUltimoAction() {
        panelPadre.cambiarPaso(4);
    }

    @FXML
    private void onBtnPrimeroAction() {
        panelPadre.cambiarPaso(1);
    }

    public void asignarListaSimbolosNoTerminales(ObservableList<String> lista) {
        simbolosNoTerminales.setAll(lista);
    }

    public void asignarListaSimbolosTerminales(ObservableList<String> lista) {
        simbolosTerminales.setAll(lista);
    }
}
