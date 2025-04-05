package editor;

import gramatica.NoTerminal;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.io.IOException;

/**
 * Panel para seleccionar el símbolo inicial de la gramática.
 */
public class PanelCreacionGramaticaPaso4 extends VBox {

    @FXML private ComboBox<NoTerminal> comboBoxSimboloInicial;
    @FXML private Button btnFinalizar;

    private final PanelCreacionGramatica panelPadre;
    private final TabPane tabPane;
    private final ObservableList<NoTerminal> noTerminales;

    public PanelCreacionGramaticaPaso4(PanelCreacionGramatica panelPadre, TabPane tabPane) {
        this.panelPadre = panelPadre;
        this.tabPane = tabPane;
        this.noTerminales = panelPadre.getGramatica().getNoTerminales();
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
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Desea salir de la edición de la gramática?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
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
        tabPane.getTabs().stream()
                .filter(tab -> tab.getContent() == this)
                .findFirst().ifPresent(tabActual -> tabPane.getTabs().remove(tabActual));
    }

    private void mostrarAlerta() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText("Debe seleccionar un símbolo inicial en el Paso 4.");
        alert.showAndWait();
    }
}
