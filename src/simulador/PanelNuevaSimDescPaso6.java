package simulador;

import com.itextpdf.text.DocumentException;
import gramatica.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.scene.layout.*;
import java.util.List;
import java.io.File;
import java.io.IOException;
import javafx.scene.Parent;

/**
 * Controlador para la simulación descendente en JavaFX.
 */
public class PanelNuevaSimDescPaso6 extends BorderPane implements PanelNuevaSimDescPaso {

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
    private PanelSimuladorDesc panelSimuladorDesc;

    public PanelNuevaSimDescPaso6(Gramatica gramatica, PanelSimuladorDesc panelSimuladorDesc) {
        this.gramatica = gramatica;
        if (!(gramatica.getTPredictiva() instanceof TablaPredictivaPaso5)) {
            TablaPredictivaPaso5 nuevaTabla = new TablaPredictivaPaso5();
            nuevaTabla.setFuncionesError(gramatica.getTPredictiva().getFuncionesError());
            nuevaTabla.construir(gramatica);
            gramatica.setTPredictiva(nuevaTabla);
        }
        this.tablaPredictiva = gramatica.getTPredictiva();
        this.funcionesError = tablaPredictiva.getFuncionesError();
        this.producciones = FXCollections.observableArrayList();
        this.cadenaEntrada = FXCollections.observableArrayList();
        this.panelSimuladorDesc = panelSimuladorDesc;
        cargarFXML();
        cargarDatos();
    }

    // Constructor anterior para compatibilidad
    public PanelNuevaSimDescPaso6(Gramatica gramatica) {
        this(gramatica, null);
    }

    private void cargarFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/PanelNuevaSimDescPaso6.fxml"));
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

        // Cargar tabla predictiva - Ahora usamos directamente la instancia del paso 5
        if (tablaPredictiva instanceof TablaPredictivaPaso5) {
            TablaPredictivaPaso5 tablaPaso5 = (TablaPredictivaPaso5) tablaPredictiva;
            
            // Nos aseguramos de que la tabla de UI tenga todas las columnas necesarias
            if (tablePredictiva.getColumns().isEmpty() || 
                tablePredictiva.getColumns().size() != tablaPaso5.getTablaPredictiva().getColumns().size()) {
                
                tablePredictiva.getColumns().setAll(tablaPaso5.getTablaPredictiva().getColumns());
            }
            
            // Nos aseguramos de usar los mismos items (filas) que en el paso 5
            tablePredictiva.setItems(tablaPaso5.getTablaPredictiva().getItems());
            
            // Configurar la tabla para mantener el estilo y comportamiento
            tablePredictiva.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            tablePredictiva.setTableMenuButtonVisible(false);
            
            // Hacemos un refresh para asegurar la visualización actualizada
            tablePredictiva.refresh();
            
            System.out.println("Tabla cargada correctamente. Número de filas: " + tablePredictiva.getItems().size());
            System.out.println("Número de columnas: " + tablePredictiva.getColumns().size());
        } else {
            System.out.println("Error: La tabla predictiva no es del tipo TablaPredictivaPaso5");
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
        // Volver a abrir el paso 5
        if (panelSimuladorDesc != null) {
            panelSimuladorDesc.cambiarPaso(4);
        }
        else{
            System.out.println("No se pudo acceder al panelSimuladorDesc");
        }
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

    @Override
    public Parent getRoot() {
        return this;
    }
}

