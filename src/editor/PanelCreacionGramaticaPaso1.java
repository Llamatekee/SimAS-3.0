package editor;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.IOException;

/**
 * Panel de creación de gramática - Paso 1
 */
public class PanelCreacionGramaticaPaso1 extends VBox {

    @FXML private TextField txtNombre;
    @FXML private TextArea txtDescripcion;
    @FXML private Button btnSiguiente;
    @FXML private Button btnAnterior;
    @FXML private Button btnCancelar;
    @FXML private Button btnUltimo;

    private final PanelCreacionGramatica panelPadre;

    public PanelCreacionGramaticaPaso1(PanelCreacionGramatica panelPadre) {
        this.panelPadre = panelPadre;
        cargarFXML();
    }

    private void cargarFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/PanelCreacionGramaticaPaso1.fxml"));
            loader.setController(this);
            Parent root = loader.load();
            this.getChildren().add(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void initialize() {
        // Cargar los datos actuales de la gramática
        txtNombre.setText(panelPadre.getGramatica().getNombre());
        txtDescripcion.setText(panelPadre.getGramatica().getDescripcion());

        // Deshabilitar el botón "Anterior" en el primer paso
        btnAnterior.setDisable(true);
    }

    @FXML
    private void onBtnSiguienteAction() {
        if (txtNombre.getText().trim().isEmpty()) {
            mostrarAlerta("El nombre de la gramática no puede estar vacío.");
            return;
        }
        if (txtDescripcion.getText().trim().isEmpty()) {
            mostrarAlerta("La descripción de la gramática no puede estar vacía.");
            return;
        }

        // Guardar los datos en la gramática temporal
        panelPadre.getGramatica().setNombre(txtNombre.getText().trim());
        panelPadre.getGramatica().setDescripcion(txtDescripcion.getText().trim());

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

    private void mostrarAlerta(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public void setNombre(String nombre) {
        txtNombre.setText(nombre);
    }

    public void setDescripcion(String descripcion) {
        txtDescripcion.setText(descripcion);
    }
}
