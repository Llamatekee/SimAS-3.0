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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Controlador para el Paso 1 de la Simulación Descendente.
 * Muestra la gramática original.
 */
public class PanelNuevaSimDescPaso1 implements PanelNuevaSimDescPaso {

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
    private Parent root;

    public PanelNuevaSimDescPaso1(PanelSimuladorDesc panelPadre) {
        this.panelPadre = panelPadre;
        this.gramatica = panelPadre.gramatica;
        this.producciones = FXCollections.observableArrayList();
        cargarFXML();
        inicializarBotones();
        verificarEstadoGramatica();
    }

    private void cargarFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/PanelNuevaSimDescPaso1.fxml"));
            loader.setController(this);
            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            lblRecursividad.setText("La gramática original era recursiva por la izquierda.");
            lblRecursividad.setStyle("-fx-text-fill: red;");
        }
        if (necesitaFactorizacion) {
            lblFactorizacion.setText("La gramática original no estaba factorizada.");
            lblFactorizacion.setStyle("-fx-text-fill: red;");
        }
        if (!esRecursiva && !necesitaFactorizacion) {
            lblEstadoGramatica.setText("La gramática original es correcta.");
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
}