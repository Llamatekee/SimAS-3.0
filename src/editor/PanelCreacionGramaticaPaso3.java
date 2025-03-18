package editor;

import bienvenida.MenuPrincipal;
import gramatica.Produccion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.io.IOException;

public class PanelCreacionGramaticaPaso3 extends VBox {

    @FXML private ListView<Produccion> listProducciones;

    public final PanelCreacionGramatica panelPadre;
    public final TabPane tabPane;
    public final MenuPrincipal menuPane;


    private final ObservableList<Produccion> producciones = FXCollections.observableArrayList();

    public PanelCreacionGramaticaPaso3(PanelCreacionGramatica panelPadre, TabPane tabPane, MenuPrincipal menuPane) {
        this.panelPadre = panelPadre;
        this.tabPane = tabPane;
        this.menuPane = menuPane;
        cargarFXML();
    }

    private void cargarFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/PanelCreacionGramaticaPaso3.fxml"));
            loader.setController(this);
            Parent root = loader.load();
            this.getChildren().add(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void initialize() {
        listProducciones.setItems(producciones);

        // Formatear correctamente la lista de producciones
        listProducciones.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Produccion item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(item.toString()); // Ahora mostrará "S → A B C" correctamente
                }
            }
        });
    }


    @FXML
    private void onBtnModificarProduccionesAction() {
        if (panelPadre.getGramatica().getNoTerminales().isEmpty()) {
            panelPadre.mostrarAlerta("Error", "El conjunto de No Terminales está vacío.");
            return;
        }

        if (panelPadre.getGramatica().getTerminales().isEmpty()) {
            panelPadre.mostrarAlerta("Error", "El conjunto de Terminales está vacío.");
            return;
        }

        PanelProducciones panel = new PanelProducciones(this, producciones, tabPane);
        Tab tab = new Tab("Modificar Producciones", panel);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    @FXML
    private void onBtnSiguienteAction() {
        panelPadre.getGramatica().setProducciones(producciones);
        panelPadre.cambiarPaso(4);
    }

    @FXML
    private void onBtnAnteriorAction() {
        panelPadre.cambiarPaso(2);
    }

    @FXML
    private void onBtnCancelarAction() {
        panelPadre.cancelarEdicion();
    }

    @FXML
    private void onBtnUltimoAction() {
        panelPadre.cambiarPaso(4);
    }

    @FXML
    private void onBtnPrimeroAction() {
        panelPadre.cambiarPaso(1);
    }

    public void asignarProducciones(ObservableList<Produccion> nuevasProducciones) {
        if (nuevasProducciones != null) {
            producciones.setAll(nuevasProducciones);
            listProducciones.refresh();  // Refrescar la ListView para mostrar los datos actualizados
        }
    }
}
