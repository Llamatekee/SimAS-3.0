package simulador;

import gramatica.Gramatica;
import gramatica.FuncionError;
import gramatica.TablaPredictiva;
import gramatica.Terminal;
import gramatica.FilaTablaPredictiva;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import editor.ActualizableTextos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Controlador para el Paso 1 de la Simulación Descendente.
 * Muestra la gramática original.
 */
public class PanelNuevaSimDescPaso1 implements PanelNuevaSimDescPaso, ActualizableTextos {

    @FXML private Label lblTitulo;
    @FXML private Label lblEstadoTitulo;
    @FXML private Label lblProduccionesTitulo;
    @FXML private Label lblEstadoGramatica;
    @FXML private Label lblRecursividad;
    @FXML private Label lblFactorizacion;
    @FXML private ListView<String> listProducciones;
    @FXML private Button btnPrimero;
    @FXML private Button btnAnterior;
    @FXML private Button btnUltimo;

    private final PanelSimuladorDesc panelPadre;
    private final Gramatica gramatica;
    private final ObservableList<String> producciones;
    private ResourceBundle bundle;
    private Parent root;

    public PanelNuevaSimDescPaso1(PanelSimuladorDesc panelPadre) {
        this.panelPadre = panelPadre;
        this.gramatica = panelPadre.gramatica;
        this.producciones = FXCollections.observableArrayList();
        this.bundle = panelPadre.getBundle();
        cargarFXML();
        inicializarBotones();
        verificarEstadoGramatica();
    }

    private void cargarFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/PanelNuevaSimDescPaso1.fxml"));
            loader.setController(this);
            loader.setResources(bundle);
            root = loader.load();
            
            // Inicializar los textos después de cargar el FXML
            if (lblTitulo != null) lblTitulo.setText(bundle.getString("simulador.window.paso1"));
            if (lblEstadoTitulo != null) lblEstadoTitulo.setText(bundle.getString("simulador.paso1.estado.titulo"));
            if (lblProduccionesTitulo != null) lblProduccionesTitulo.setText(bundle.getString("simulador.paso1.producciones.titulo"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void initialize() {
        // Este método se llama automáticamente después de cargar el FXML
        // No necesitamos hacer nada aquí ya que la inicialización se hace en el constructor
    }

    private void inicializarBotones() {
        // En el paso 1, los botones Primero y Anterior están deshabilitados
        btnPrimero.setDisable(true);
        btnAnterior.setDisable(true);
    }

    private void verificarEstadoGramatica() {
        boolean esRecursiva = gramatica.eliminarRecursividad();
        boolean necesitaFactorizacion = gramatica.factorizar();

        if (esRecursiva) {
            lblRecursividad.setText(bundle.getString("simulador.gramatica.recursiva"));
            lblRecursividad.setStyle("-fx-text-fill: red;");
        }
        if (necesitaFactorizacion) {
            lblFactorizacion.setText(bundle.getString("simulador.gramatica.no.factorizada"));
            lblFactorizacion.setStyle("-fx-text-fill: red;");
        }
        if (!esRecursiva && !necesitaFactorizacion) {
            lblEstadoGramatica.setText(bundle.getString("simulador.gramatica.correcta"));
            lblEstadoGramatica.setStyle("-fx-text-fill: green;");
        }
        listProducciones.setItems(gramatica.getProduccionesModel());
    }

    @FXML
    private void cancelarSimulacion() {
        panelPadre.cancelarSimulacion();
    }

    @FXML
    private void avanzarPaso() {
        panelPadre.cambiarPaso(1);
    }

    @FXML
    private void retrocederPaso() {
        // No hay paso anterior
    }

    @FXML
    private void visualizarGramatica() {
        panelPadre.mostrarGramaticaOriginal();
    }

    @FXML
    private void handlePrimero() {
        panelPadre.cambiarPaso(0);
    }

    @FXML
    private void handleAnterior() {
        // No hay paso anterior, este es el primer paso
    }

    @FXML
    private void handleUltimo() {
        panelPadre.cambiarPaso(3);
    }

    @Override
    public Parent getRoot() {
        return root;
    }

    @Override
    public void actualizarTextos(ResourceBundle bundle) {
        this.bundle = bundle;
        try {
            // Recargar el FXML con el nuevo bundle
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/PanelNuevaSimDescPaso1.fxml"));
            loader.setController(this);
            loader.setResources(bundle);
            root = loader.load();
            
            // Actualizar los textos
            if (lblTitulo != null) lblTitulo.setText(bundle.getString("simulador.window.paso1"));
            if (lblEstadoTitulo != null) lblEstadoTitulo.setText(bundle.getString("simulador.paso1.estado.titulo"));
            if (lblProduccionesTitulo != null) lblProduccionesTitulo.setText(bundle.getString("simulador.paso1.producciones.titulo"));
            
            // Actualizar el estado de la gramática
            verificarEstadoGramatica();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}