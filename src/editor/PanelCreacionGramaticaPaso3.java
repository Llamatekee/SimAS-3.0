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
import java.util.ResourceBundle;

public class PanelCreacionGramaticaPaso3 extends VBox implements ActualizableTextos {

    @FXML private ListView<Produccion> listProducciones;
    @FXML private Button btnModificarProducciones;
    @FXML private Button btnCancelar;
    @FXML private Button btnPrimero;
    @FXML private Button btnAnterior;
    @FXML private Button btnSiguiente;
    @FXML private Button btnUltimo;
    @FXML private Label labelHeader;
    @FXML private Label labelLista;

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
            this.getChildren().setAll(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void initialize() {
        listProducciones.setItems(producciones);
        actualizarTextos(panelPadre.getBundle());

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

    public void asignarProducciones(ObservableList<Produccion> nuevasProducciones) {
        if (nuevasProducciones != null) {
            producciones.setAll(nuevasProducciones);
            listProducciones.refresh();  // Refrescar la ListView para mostrar los datos actualizados
        }
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

        ResourceBundle bundle = panelPadre.getBundle();
        String childId = "producciones_" + panelPadre.getCreacionId();
        
        // Verificar si ya existe una pestaña para esta creación específica
        Tab existingTab = findTabByChildId(childId);
        if (existingTab != null) {
            tabPane.getSelectionModel().select(existingTab);
            return;
        }
        
        // Crear una nueva pestaña de producciones como hija de la creación
        PanelProducciones panel = new PanelProducciones(this, producciones, tabPane);
        TabManager.getOrCreateTab(tabPane, PanelProducciones.class, 
            bundle.getString("creacion3.tab.modificar.producciones"), panel, panelPadre.getCreacionId(), childId);
    }
    
    /**
     * Busca una pestaña por su childId específico.
     */
    private Tab findTabByChildId(String childId) {
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() != null && tab.getUserData().toString().equals(childId)) {
                return tab;
            }
        }
        return null;
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

    @Override
    public void actualizarTextos(ResourceBundle bundle) {
        if (labelHeader != null) labelHeader.setText(bundle.getString("creacion3.header"));
        if (labelLista != null) labelLista.setText(bundle.getString("creacion3.label.lista"));
        if (btnModificarProducciones != null) btnModificarProducciones.setText(bundle.getString("creacion3.btn.modificar.producciones"));
        if (btnCancelar != null) btnCancelar.setText(bundle.getString("button.cancelar"));
        if (btnPrimero != null) btnPrimero.setText(bundle.getString("button.primero"));
        if (btnAnterior != null) btnAnterior.setText(bundle.getString("button.anterior"));
        if (btnSiguiente != null) btnSiguiente.setText(bundle.getString("button.siguiente"));
        if (btnUltimo != null) btnUltimo.setText(bundle.getString("button.ultimo"));
    }
}
