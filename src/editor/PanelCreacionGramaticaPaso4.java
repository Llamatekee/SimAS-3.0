package editor;

import gramatica.NoTerminal;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.util.ResourceBundle;

/**
 * Panel para seleccionar el símbolo inicial de la gramática.
 */
public class PanelCreacionGramaticaPaso4 extends VBox implements ActualizableTextos {

    @FXML private ComboBox<NoTerminal> comboBoxSimboloInicial;
    @FXML private Button btnFinalizar;
    @FXML private Button btnCancelar;
    @FXML private Button btnAnterior;
    @FXML private Button btnPrimero;
    @FXML private Label labelHeader;
    @FXML private Label labelSimboloInicial;

    private final PanelCreacionGramatica panelPadre;
    private final TabPane tabPane;
    private final ObservableList<NoTerminal> noTerminales;
    private ResourceBundle bundle;

    public PanelCreacionGramaticaPaso4(PanelCreacionGramatica panelPadre, TabPane tabPane) {
        this.panelPadre = panelPadre;
        this.tabPane = tabPane;
        this.noTerminales = panelPadre.getGramatica().getNoTerminales();
        this.bundle = panelPadre.getBundle();
        cargarFXML();
    }

    private void cargarFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/PanelCreacionGramaticaPaso4.fxml"));
            loader.setController(this);
            Parent root = loader.load();
            this.getChildren().add(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void initialize() {
        // Cargar No Terminales en el ComboBox
        comboBoxSimboloInicial.setItems(noTerminales);

        // Seleccionar el símbolo inicial actual si ya está definido
        String simboloInicial = panelPadre.getGramatica().getSimbInicial();
        if (simboloInicial != null && !simboloInicial.isEmpty()) {
            for (NoTerminal nt : noTerminales) {
                if (nt.getNombre().equals(simboloInicial)) {
                    comboBoxSimboloInicial.setValue(nt);
                    break;
                }
            }
        } else if (!noTerminales.isEmpty()) {
            // Si no hay símbolo inicial definido pero hay no terminales, seleccionar el primero
            comboBoxSimboloInicial.setValue(noTerminales.get(0));
        }

        // Actualizar el símbolo inicial en la gramática con el valor inicial
        if (comboBoxSimboloInicial.getValue() != null) {
            panelPadre.getGramatica().setSimbInicial(comboBoxSimboloInicial.getValue().getNombre());
        }

        // Manejo de selección en ComboBox
        comboBoxSimboloInicial.setOnAction(event -> {
            NoTerminal seleccionado = comboBoxSimboloInicial.getValue();
            if (seleccionado != null) {
                panelPadre.getGramatica().setSimbInicial(seleccionado.getNombre());
            }
        });

        actualizarTextos(bundle);
    }

    @FXML
    private void onBtnFinalizarAction() {
        NoTerminal simboloInicial = comboBoxSimboloInicial.getValue();
        if (simboloInicial != null) {
            // Asignar el símbolo inicial a la gramática temporal
            panelPadre.getGramatica().setSimbInicial(simboloInicial.getNombre());
            panelPadre.getPanelPadre().setGramatica(panelPadre.getGramatica());
            panelPadre.getPanelPadre().actualizarVisualizacion();
            cerrarAsistente();
        } else {
            mostrarAlerta();
        }
    }

    @FXML
    private void onBtnCancelarAction() {
        ButtonType btnSi = new ButtonType(bundle.getString("button.si"), ButtonBar.ButtonData.YES);
        ButtonType btnNo = new ButtonType(bundle.getString("button.no"), ButtonBar.ButtonData.NO);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, bundle.getString("creacion.dialog.salir.mensaje"), btnSi, btnNo);
        confirm.setTitle(bundle.getString("creacion.dialog.salir.titulo"));
        confirm.setHeaderText(bundle.getString("creacion.dialog.salir.titulo"));
        confirm.showAndWait().ifPresent(response -> {
            if (response == btnSi) {
                cerrarAsistente();
            }
        });
    }

    @FXML
    private void onBtnAnteriorAction() {
        panelPadre.cambiarPaso(3);
    }

    @FXML
    private void onBtnPrimeroAction() {
        panelPadre.cambiarPaso(1);
    }

    private void cerrarAsistente() {
        // Buscar la pestaña que contiene el PanelCreacionGramatica
        tabPane.getTabs().stream()
                .filter(tab -> tab.getContent() instanceof PanelCreacionGramatica)
                .findFirst()
                .ifPresent(tabActual -> tabPane.getTabs().remove(tabActual));
    }

    private void mostrarAlerta() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(bundle.getString("editor.dialog.error.titulo"));
        alert.setHeaderText(null);
        alert.setContentText(bundle.getString("creacion4.error.simbolo.inicial"));
        alert.showAndWait();
    }

    @Override
    public void actualizarTextos(ResourceBundle bundle) {
        this.bundle = bundle;
        if (labelHeader != null) labelHeader.setText(bundle.getString("wizard.step4.header"));
        if (labelSimboloInicial != null) labelSimboloInicial.setText(bundle.getString("wizard.step4.select_initial"));
        if (btnFinalizar != null) btnFinalizar.setText(bundle.getString("wizard.finish"));
        if (btnCancelar != null) btnCancelar.setText(bundle.getString("wizard.cancel"));
        if (btnAnterior != null) btnAnterior.setText(bundle.getString("wizard.previous"));
        if (btnPrimero != null) btnPrimero.setText(bundle.getString("wizard.first"));
    }
}
