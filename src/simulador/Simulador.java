package simulador;

import com.itextpdf.text.DocumentException;
import gramatica.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;

/**
 * Controlador para la simulación descendente en JavaFX.
 */
public class Simulador {

    @FXML private Label lblSimulador;
    @FXML private Label lblTabla;
    @FXML private ListView<String> listProducciones;
    @FXML private ListView<String> listFuncionesError;
    @FXML private TableView<FilaTablaPredictiva> tablePredictiva;
    @FXML private Button btnGenerarInforme;
    @FXML private Button btnModificarErrores;
    @FXML private Button btnSimular;

    private Gramatica gramatica;
    private ObservableList<String> producciones;
    private ObservableList<String> funcionesError;
    private TablaPredictiva tablaPredictiva;
    private ObservableList<Terminal> cadenaEntrada;

    public void inicializar(Gramatica gramatica) {
        this.gramatica = gramatica;
        this.producciones = FXCollections.observableArrayList();
        this.funcionesError = FXCollections.observableArrayList();
        this.tablaPredictiva = gramatica.getTPredictiva();
        this.cadenaEntrada = FXCollections.observableArrayList();
        //cargarDatos();
    }

    /*private void cargarDatos() {
        producciones.clear();
        for (String prod : gramatica.getProduccionesModel()) {
            producciones.add(prod);
        }
        listProducciones.setItems(producciones);

        funcionesError.clear();
        for (FuncionError error : tablaPredictiva.getFuncionesError()) {
            funcionesError.add(error.getIdentificador() + " - " + error.getMensaje());
        }
        listFuncionesError.setItems(funcionesError);

        tablePredictiva.setItems(tablaPredictiva.getTablaPredictiva().getItems());
    }*/

    @FXML
    private void generarInforme() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Informes de simulación (.pdf)", "*.pdf"));
        fileChooser.setTitle("Guardar Informe");
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try {
                boolean resultado = gramatica.generarInforme(file.getAbsolutePath());
                if (!resultado) {
                    mostrarAlerta("Error", "No se pudo generar el informe de simulación.");
                }
            } catch (DocumentException e) {
                mostrarAlerta("Error", "Error al generar el informe: " + e.getMessage());
            }
        }
    }

    @FXML
    private void iniciarSimulacion() {
        // Método que debería iniciar la simulación descendente
        // Dependerá de la implementación de NuevaSimulacionDesc
        // NuevaSimulacionDesc simDesc = new NuevaSimulacionDesc(this.gramatica, this);
        // simDesc.ejecutar();
    }

    @FXML
    private void modificarErrores() {
        // Método para modificar funciones de error
        // Dependerá de la implementación de PanelSimuladorDesc
        // PanelSimuladorDesc panelErrores = new PanelSimuladorDesc(this.gramatica);
        // panelErrores.mostrar();
    }

    public String actualizarVisualizacion() {
        StringBuilder cadena = new StringBuilder();
        for (Terminal terminal : cadenaEntrada) {
            cadena.append(terminal.getNombre()).append(" ");
        }
        return cadena.toString();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

}
