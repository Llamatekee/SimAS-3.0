package editor;

import bienvenida.MenuPrincipal;
import simulador.PanelSimuladorDesc;

import com.itextpdf.text.DocumentException;
import gramatica.Gramatica;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Editor extends VBox {

    // Modelo
    private Gramatica gramatica = crearGramatica();

    // Dependencias del sistema (por ejemplo, la pestaña y el menú principal)
    public TabPane tabPane;
    public MenuPrincipal menuPane;

    // Componentes inyectados desde el FXML
    @FXML
    private BorderPane rootPane;
    @FXML
    private Button btnGuardar;
    @FXML
    private Button btnEditar;
    @FXML
    private Button btnEliminar;
    @FXML
    private Button btnValidar;
    @FXML
    private Button btnPdf;
    @FXML
    private Button btnSimDesc;
    @FXML
    private TextArea txtNombre;
    @FXML
    private TextArea txtAreaDesc;
    @FXML
    private TextArea txtSimInicial;
    @FXML
    private ListView<String> listNoTerminales;
    @FXML
    private ListView<String> listTerminales;
    @FXML
    private ListView<String> listProducciones;

    // ==========================
    // CONSTRUCTORES
    // ==========================

    // ==========================
    // CONSTRUCTORES
    // ==========================

    /**
     * 🔹 Constructor vacío (requerido por JavaFX para cargar FXML).
     * ⚠ Se deben asignar `tabPane` y `menuPane` después de la carga.
     */
    public Editor() {
        this.gramatica = new Gramatica();
        cargarFXML();
    }

    /**
     * 🔹 Constructor con TabPane y MenuPrincipal para uso manual.
     */
    public Editor(TabPane tabPane, MenuPrincipal menuPane) {
        this.tabPane = tabPane;
        this.menuPane = menuPane;
        this.gramatica = new Gramatica();
        cargarFXML();
    }

    /**
     * 🔹 Constructor con gramática preexistente.
     */
    public Editor(TabPane tabPane, Gramatica gramatica, MenuPrincipal menuPane) {
        this.tabPane = tabPane;
        this.menuPane = menuPane;
        this.gramatica = gramatica;
        cargarFXML();
    }

    // ==========================
    // MÉTODOS DE INICIALIZACIÓN
    // ==========================

    /**
     * 🔹 Carga la interfaz desde el archivo FXML.
     */
    private void cargarFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/Editor.fxml"));
            loader.setController(this);
            rootPane = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public Gramatica getGramatica() {
        return this.gramatica;
    }

    public void setGramatica(Gramatica gramatica) {
        this.gramatica = gramatica;
    }

    // Método de inicialización; se invoca automáticamente tras cargar el FXML.
    @FXML
    private void initialize() {
        // Configurar la vista según sea necesario
        btnEditar.setDisable(true);
        btnEliminar.setDisable(true);
        btnGuardar.setDisable(true);
        btnValidar.setDisable(true);
        btnPdf.setDisable(true);
        btnSimDesc.setDisable(true);
    }

    // Métodos para inyectar dependencias (si se crean desde MenuPrincipal)
    public void setTabPane(TabPane tabPane) {
        this.tabPane = tabPane;
    }

    public void setMenuPane(MenuPrincipal menuPane) {
        this.menuPane = menuPane;
    }

    // Método para obtener el nodo raíz del editor (para incluirlo en una pestaña)
    public Parent getRoot() {
        return rootPane;
    }

    // Método auxiliar para crear una gramática vacía
    private Gramatica crearGramatica() {
        return new Gramatica();
    }

    // ===============================
    // Métodos de manejo de eventos
    // ===============================

    @FXML
    private void onBtnAnadirAction() {
        // Se abre el panel de creación de gramática sin una gramática existente (nueva gramática)
        new PanelCreacionGramatica(this, tabPane, null, menuPane);
    }

    @FXML
    private void onBtnEditarAction() {
        if (this.tabPane == null || this.menuPane == null) {
            System.err.println("Error: tabPane o menuPane no están inicializados.");
            return;
        }

        // Se abre el panel de creación de gramática con la gramática actual (edición)
        new PanelCreacionGramatica(this, tabPane, this.gramatica, menuPane);
    }


    @FXML
    private void onBtnSimDescAction() {
        // Ejecutar la simulación descendente.
        new PanelSimuladorDesc(gramatica, tabPane);
    }

    @FXML
    private void onBtnSalirAction() {
        Alert confirm = new Alert(AlertType.CONFIRMATION, "¿Desea salir de SimAS?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Salir");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            System.exit(0);
        }
    }

    @FXML
    private void onBtnGuardarAction() {
        grabarGramatica();
    }

    @FXML
    private void onBtnAbrirAction() {
        cargarGramatica();
    }

    @FXML
    private void onBtnEliminarAction() {
        if (gramatica == null || gramatica.getNombre() == null || gramatica.getNombre().isEmpty()) {
            Alert alert = new Alert(AlertType.WARNING, "No hay ninguna gramática abierta para cerrar.", ButtonType.OK);
            alert.setTitle("Aviso");
            alert.showAndWait();
            return;
        }

        Alert confirm = new Alert(AlertType.CONFIRMATION, "¿Desea cerrar la gramática '" + gramatica.getNombre() + "'?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Cerrar Gramática");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.YES) {
            Alert info = new Alert(AlertType.INFORMATION, "La gramática '" + gramatica.getNombre() + "' ha sido cerrada.", ButtonType.OK);
            info.setTitle("Gramática Cerrada");
            info.showAndWait();

            // Resetear el modelo de gramática sin cerrar la pestaña
            gramatica = null; // 🔹 Asignamos null en lugar de crear una nueva gramática
            actualizarVisualizacion(); // 🔹 Actualizar la vista y desactivar los botones
        }
    }

    @FXML
    private void onBtnValidarAction() {
        if (gramatica != null) {
            validarGramatica(gramatica);
        }
    }

    @FXML
    private void onBtnPdfAction() {
        // Generar informe PDF usando FileChooser de JavaFX
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Informes de gramática (.pdf)", "*.pdf"));
        fileChooser.setTitle("Guardar Informe Gramática");
        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try {
                boolean resultado = gramatica.generarInforme(file.getAbsolutePath());
                if (!resultado) {
                    Alert alert = new Alert(AlertType.ERROR, "No se pudo generar el informe de la gramática.", ButtonType.OK);
                    alert.showAndWait();
                }
            } catch (DocumentException ex) {
                Logger.getLogger(Editor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    // ===============================
    // Métodos auxiliares (adaptados o comentados)
    // ===============================

    public void cargarGramatica() {
        if (this.gramatica == null) {
            this.gramatica = new Gramatica();  // 🔹 Crear una nueva instancia si era null
        }

        Gramatica gr = this.gramatica.cargarGramatica(null);

        if (gr != null) {
            this.gramatica = gr;
            actualizarVisualizacion();
            validarGramatica(gramatica);
        } else {
            this.gramatica = null; // Si no se carga una nueva gramática, aseguramos que quede en null
            actualizarVisualizacion();
        }
    }


    public void grabarGramatica() {
        int i = gramatica.guardarGramatica(null); // Pasar null como ownerWindow
        if (i == 1) {
            Alert alert = new Alert(AlertType.INFORMATION, "Gramática guardada correctamente", ButtonType.OK);
            alert.setTitle("Guardar Gramática");
            alert.showAndWait();
        }
    }

    private void actualizarEstadoBotones() {
        boolean hayGramatica = (gramatica != null && gramatica.getNombre() != null && !gramatica.getNombre().isEmpty());

        btnEditar.setDisable(!hayGramatica);
        btnEliminar.setDisable(!hayGramatica);
        btnGuardar.setDisable(!hayGramatica);
        btnValidar.setDisable(!hayGramatica);
        btnPdf.setDisable(!hayGramatica);
        btnSimDesc.setDisable(!hayGramatica);
    }


    public void actualizarVisualizacion() {
        if (this.gramatica != null) {
            listNoTerminales.setItems(gramatica.getNoTerminalesModel());
            listTerminales.setItems(gramatica.getTerminalesModel());
            listProducciones.setItems(gramatica.getProduccionesModel());
            txtNombre.setText(gramatica.getNombre());
            txtAreaDesc.setText(gramatica.getDescripcion());
            txtSimInicial.setText(gramatica.getSimbInicial());
        } else {
            // Limpiar campos si no hay gramática
            listNoTerminales.setItems(FXCollections.observableArrayList());
            listTerminales.setItems(FXCollections.observableArrayList());
            listProducciones.setItems(FXCollections.observableArrayList());
            txtNombre.clear();
            txtAreaDesc.clear();
            txtSimInicial.clear();
        }

        actualizarEstadoBotones(); // 🔹 Llamamos para activar/desactivar los botones según corresponda
    }

    public void validarGramatica(Gramatica gramatica) {
        ObservableList<String> mensajesError = gramatica.validarGramatica();
        int estadoValidacion = gramatica.getEstado();

        if (estadoValidacion == 1) {
            gramatica.setEstado(1);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Gramática Validada");
            alert.setHeaderText(null);
            alert.setContentText("La gramática está validada correctamente.");
            alert.showAndWait();
        } else {
            gramatica.setEstado(-1);

            // Construcción del mensaje sin etiquetas HTML
            StringBuilder mensajeError = new StringBuilder("Se han detectado los siguientes errores:\n\n");
            for (int i = 0; i < mensajesError.size(); i++) {
                mensajeError.append(i + 1).append(". ").append(mensajesError.get(i)).append("\n\n");
            }

            // Crear un alert de error con el formato correcto
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error. Gramática No Validada");
            alert.setHeaderText("Errores encontrados:");
            alert.setContentText(mensajeError.toString());

            // Hacer que la alerta muestre todo el texto correctamente si es muy largo
            TextArea textArea = new TextArea(mensajeError.toString());
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);

            GridPane gridPane = new GridPane();
            gridPane.setMaxWidth(Double.MAX_VALUE);
            gridPane.add(textArea, 0, 0);

            alert.getDialogPane().setExpandableContent(gridPane);
            alert.showAndWait();
        }
    }
}