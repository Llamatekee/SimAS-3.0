package simulador;

import com.itextpdf.text.DocumentException;
import gramatica.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.scene.layout.*;
import java.util.List;
import java.io.File;
import java.io.IOException;

/**
 * Controlador para la simulación descendente en JavaFX.
 */
public class Simulador extends BorderPane {

    @FXML private TableView<FilaTablaPredictiva> tablePredictiva;
    @FXML private ListView<String> listProducciones;
    @FXML private ListView<String> listFuncionesError;
    @FXML private Button btnSimular;
    @FXML private Button btnModificarErrores;
    @FXML private Button btnGenerarInforme;

    private Gramatica gramatica;
    private TablaPredictiva tablaPredictiva;
    private List<FuncionError> funcionesError;
    private ObservableList<String> producciones;
    private ObservableList<Terminal> cadenaEntrada;

    public Simulador(Gramatica gramatica) {
        this.gramatica = gramatica;
        this.tablaPredictiva = gramatica.getTPredictiva();
        this.funcionesError = tablaPredictiva.getFuncionesError();
        this.producciones = FXCollections.observableArrayList();
        this.cadenaEntrada = FXCollections.observableArrayList();
        cargarFXML();
        cargarDatos();
    }

    private void cargarFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/Simulador.fxml"));
            loader.setController(this);
            BorderPane root = loader.load();
            this.setTop(root.getTop());
            this.setCenter(root.getCenter());
            this.setBottom(root.getBottom());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cargarDatos() {
        // Cargar producciones
        producciones.setAll(gramatica.getProduccionesModel());
        listProducciones.setItems(producciones);

        // Cargar funciones de error
        ObservableList<String> errores = FXCollections.observableArrayList();
        if (funcionesError != null && !funcionesError.isEmpty()) {
            for (FuncionError fe : funcionesError) {
                String desc = fe.getMensaje();
                if (desc == null || desc.trim().isEmpty()) {
                    desc = "Función de error sin descripción";
                }
                errores.add(fe.getIdentificador() + " - " + desc);
            }
        } else {
            errores.add("No se están usando funciones de error");
        }
        listFuncionesError.setItems(errores);

        // Cargar tabla predictiva si tienes un modelo para ello
        if (tablaPredictiva instanceof TablaPredictivaPaso5) {
            TableView<FilaTablaPredictiva> tabla = ((TablaPredictivaPaso5) tablaPredictiva).getTablaPredictiva();
            tablePredictiva.getColumns().setAll(tabla.getColumns());
            tablePredictiva.setItems(tabla.getItems());
        }
    }

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
        // Implementa la lógica para iniciar la simulación descendente
    }

    @FXML
    private void modificarErrores() {
        // Implementa la lógica para modificar funciones de error
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

