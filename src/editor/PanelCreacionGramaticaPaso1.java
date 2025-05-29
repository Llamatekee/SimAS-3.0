package editor;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.beans.binding.BooleanBinding;

import java.io.IOException;
import java.util.ResourceBundle;
import editor.ActualizableTextos;

/**
 * Panel de creación de gramática - Paso 1
 * Permite al usuario ingresar el nombre y la descripción de la gramática
 */
public class PanelCreacionGramaticaPaso1 extends VBox implements ActualizableTextos {

    private static final int MAX_NOMBRE_LENGTH = 50;
    private static final int MAX_DESCRIPCION_LENGTH = 500;

    @FXML private TextField txtNombre;
    @FXML private TextArea txtDescripcion;
    @FXML private Button btnSiguiente;
    @FXML private Button btnAnterior;
    @FXML private Button btnCancelar;
    @FXML private Button btnUltimo;
    @FXML private Button btnPrimero;
    @FXML private Label lblNombreError;
    @FXML private Label lblDescripcionError;
    @FXML private Label labelHeader;
    @FXML private Label labelNombreSeccion;
    @FXML private Label labelDescripcionSeccion;

    private final PanelCreacionGramatica panelPadre;
    private ResourceBundle bundle;

    public PanelCreacionGramaticaPaso1(PanelCreacionGramatica panelPadre, ResourceBundle bundle) {
        this.panelPadre = panelPadre;
        this.bundle = bundle;
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
            mostrarError("Error al cargar la interfaz", e.getMessage());
        }
    }

    @FXML
    private void initialize() {
        actualizarTextos(bundle);
        // Configurar validaciones en tiempo real
        configurarValidaciones();
        
        // Cargar datos existentes si los hay
        String nombreActual = panelPadre.getGramatica().getNombre();
        String descripcionActual = panelPadre.getGramatica().getDescripcion();
        
        if (nombreActual != null && !nombreActual.isEmpty()) {
            txtNombre.setText(nombreActual);
        }
        if (descripcionActual != null && !descripcionActual.isEmpty()) {
            txtDescripcion.setText(descripcionActual);
        }

        // Deshabilitar botón anterior en el primer paso
        btnAnterior.setDisable(true);
    }

    private void configurarValidaciones() {
        // Validación del nombre
        txtNombre.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.trim().isEmpty()) {
                mostrarErrorCampo(lblNombreError, bundle.getString("creacion.error.nombre.vacio"));
            } else if (newValue.length() > MAX_NOMBRE_LENGTH) {
                mostrarErrorCampo(lblNombreError, bundle.getString("creacion.error.nombre.largo").replace("{MAX}", String.valueOf(MAX_NOMBRE_LENGTH)));
                txtNombre.setText(oldValue);
            } else {
                ocultarErrorCampo(lblNombreError);
            }
        });

        // Validación de la descripción
        txtDescripcion.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.trim().isEmpty()) {
                mostrarErrorCampo(lblDescripcionError, bundle.getString("creacion.error.descripcion.vacia"));
            } else if (newValue.length() > MAX_DESCRIPCION_LENGTH) {
                mostrarErrorCampo(lblDescripcionError, bundle.getString("creacion.error.descripcion.larga").replace("{MAX}", String.valueOf(MAX_DESCRIPCION_LENGTH)));
                txtDescripcion.setText(oldValue);
            } else {
                ocultarErrorCampo(lblDescripcionError);
            }
        });

        // Habilitar/deshabilitar botón siguiente basado en validaciones
        BooleanBinding camposValidos = txtNombre.textProperty().isEmpty()
                .or(txtDescripcion.textProperty().isEmpty())
                .or(lblNombreError.visibleProperty())
                .or(lblDescripcionError.visibleProperty());
        
        btnSiguiente.disableProperty().bind(camposValidos);
    }

    private void mostrarErrorCampo(Label label, String mensaje) {
        label.setText(mensaje);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void ocultarErrorCampo(Label label) {
        label.setVisible(false);
        label.setManaged(false);
    }

    @FXML
    private void onBtnSiguienteAction() {
        // Guardar los datos validados en la gramática temporal
        panelPadre.getGramatica().setNombre(txtNombre.getText().trim());
        panelPadre.getGramatica().setDescripcion(txtDescripcion.getText().trim());
        panelPadre.cambiarPaso(2);
    }

    @FXML
    private void onBtnCancelarAction() {
        if (hayDatosSinGuardar()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmar Cancelación");
            alert.setHeaderText(null);
            alert.setContentText("¿Está seguro que desea cancelar la edición? Los cambios no guardados se perderán.");
            
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    panelPadre.cancelarEdicion();
                }
            });
        } else {
            panelPadre.cancelarEdicion();
        }
    }

    @FXML
    private void onBtnUltimoAction() {
        if (datosValidos()) {
            // Guardar los datos antes de ir al último paso
            panelPadre.getGramatica().setNombre(txtNombre.getText().trim());
            panelPadre.getGramatica().setDescripcion(txtDescripcion.getText().trim());
            panelPadre.cambiarPaso(4);
        }
    }

    private boolean datosValidos() {
        return !txtNombre.getText().trim().isEmpty() && 
               !txtDescripcion.getText().trim().isEmpty() &&
               !lblNombreError.isVisible() &&
               !lblDescripcionError.isVisible();
    }

    private boolean hayDatosSinGuardar() {
        String nombreActual = panelPadre.getGramatica().getNombre();
        String descripcionActual = panelPadre.getGramatica().getDescripcion();
        
        return !txtNombre.getText().trim().equals(nombreActual != null ? nombreActual : "") ||
               !txtDescripcion.getText().trim().equals(descripcionActual != null ? descripcionActual : "");
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
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

    public void actualizarTextos(ResourceBundle bundle) {
        this.bundle = bundle;
        // Actualizar header y labels de sección
        if (labelHeader != null) labelHeader.setText(bundle.getString("creacion.header"));
        if (labelNombreSeccion != null) labelNombreSeccion.setText(bundle.getString("creacion.label.nombre.seccion"));
        if (labelDescripcionSeccion != null) labelDescripcionSeccion.setText(bundle.getString("creacion.label.descripcion.seccion"));
        // Actualizar botones de navegación
        if (btnCancelar != null) btnCancelar.setText(bundle.getString("button.cancelar"));
        if (btnAnterior != null) btnAnterior.setText(bundle.getString("button.anterior"));
        if (btnSiguiente != null) btnSiguiente.setText(bundle.getString("button.siguiente"));
        if (btnUltimo != null) btnUltimo.setText(bundle.getString("button.ultimo"));
        if (btnPrimero != null) btnPrimero.setText(bundle.getString("button.primero"));
        // Actualizar mensajes de error visibles
        if (lblNombreError != null && lblNombreError.isVisible()) {
            String texto = "";
            String nombre = txtNombre.getText();
            if (nombre == null || nombre.trim().isEmpty()) {
                texto = bundle.getString("creacion.error.nombre.vacio");
            } else if (nombre.length() > MAX_NOMBRE_LENGTH) {
                texto = bundle.getString("creacion.error.nombre.largo").replace("{MAX}", String.valueOf(MAX_NOMBRE_LENGTH));
            }
            lblNombreError.setText(texto);
        }
        if (lblDescripcionError != null && lblDescripcionError.isVisible()) {
            String texto = "";
            String desc = txtDescripcion.getText();
            if (desc == null || desc.trim().isEmpty()) {
                texto = bundle.getString("creacion.error.descripcion.vacia");
            } else if (desc.length() > MAX_DESCRIPCION_LENGTH) {
                texto = bundle.getString("creacion.error.descripcion.larga").replace("{MAX}", String.valueOf(MAX_DESCRIPCION_LENGTH));
            }
            lblDescripcionError.setText(texto);
        }
    }
}
