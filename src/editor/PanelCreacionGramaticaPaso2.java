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

public class PanelCreacionGramaticaPaso2 extends VBox implements ActualizableTextos {

    public final PanelCreacionGramatica panelPadre;
    public final MenuPrincipal menuPane;
    public final TabPane tabPane;

    @FXML private ListView<String> listNoTerminales;
    @FXML private ListView<String> listTerminales;
    @FXML private Button btnModificarNoTerminales;
    @FXML private Button btnModificarTerminales;
    @FXML private Button btnCancelar;
    @FXML private Button btnPrimero;
    @FXML private Button btnAnterior;
    @FXML private Button btnSiguiente;
    @FXML private Button btnUltimo;
    @FXML private Label labelHeader;
    @FXML private Label labelNoTerminalesSeccion;
    @FXML private Label labelTerminalesSeccion;

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
        java.util.ResourceBundle bundle = panelPadre.getBundle();
        if (TabManager.hasTab(tabPane, PanelSimbolosNoTerminales.class)) {
            // Si ya existe una pestaña de no terminales, seleccionarla
            TabManager.getOrCreateTab(tabPane, PanelSimbolosNoTerminales.class, bundle.getString("creacion2.tab.modificar.no.terminales"), null);
        } else {
            // Si no existe, crear una nueva
            PanelSimbolosNoTerminales panel = new PanelSimbolosNoTerminales(simbolosNoTerminales, tabPane, this);
            TabManager.getOrCreateTab(tabPane, PanelSimbolosNoTerminales.class, bundle.getString("creacion2.tab.modificar.no.terminales"), panel);
        }
    }

    @FXML
    private void onBtnModificarTerminalesAction() {
        java.util.ResourceBundle bundle = panelPadre.getBundle();
        if (TabManager.hasTab(tabPane, PanelSimbolosTerminales.class)) {
            // Si ya existe una pestaña de terminales, seleccionarla
            TabManager.getOrCreateTab(tabPane, PanelSimbolosTerminales.class, bundle.getString("creacion2.tab.modificar.terminales"), null);
        } else {
            // Si no existe, crear una nueva
            PanelSimbolosTerminales panel = new PanelSimbolosTerminales(simbolosTerminales, tabPane, this);
            TabManager.getOrCreateTab(tabPane, PanelSimbolosTerminales.class, bundle.getString("creacion2.tab.modificar.terminales"), panel);
        }
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

    public void actualizarTextos(java.util.ResourceBundle bundle) {
        if (labelHeader != null) labelHeader.setText(bundle.getString("creacion2.header"));
        if (labelNoTerminalesSeccion != null) labelNoTerminalesSeccion.setText(bundle.getString("creacion2.label.no.terminales.seccion"));
        if (labelTerminalesSeccion != null) labelTerminalesSeccion.setText(bundle.getString("creacion2.label.terminales.seccion"));
        if (btnModificarNoTerminales != null) btnModificarNoTerminales.setText(bundle.getString("creacion2.btn.modificar.no.terminales"));
        if (btnModificarTerminales != null) btnModificarTerminales.setText(bundle.getString("creacion2.btn.modificar.terminales"));
        if (btnCancelar != null) btnCancelar.setText(bundle.getString("button.cancelar"));
        if (btnPrimero != null) btnPrimero.setText(bundle.getString("button.primero"));
        if (btnAnterior != null) btnAnterior.setText(bundle.getString("button.anterior"));
        if (btnSiguiente != null) btnSiguiente.setText(bundle.getString("button.siguiente"));
        if (btnUltimo != null) btnUltimo.setText(bundle.getString("button.ultimo"));
    }
}
