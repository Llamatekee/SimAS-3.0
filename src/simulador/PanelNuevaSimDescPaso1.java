package simulador;

import gramatica.Gramatica;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.io.IOException;

/**
 * Primer paso en la simulación descendente.
 * Verifica si la gramática tiene recursividad por la izquierda o necesita factorización.
 */
public class PanelNuevaSimDescPaso1 {

    @FXML private Label lblEstadoGramatica;
    @FXML private Label lblRecursividad;
    @FXML private Label lblFactorizacion;
    @FXML private ListView<String> listProducciones;

    private final PanelSimuladorDesc panelPadre;
    private final Gramatica gramatica;
    private final ObservableList<String> producciones;
    private Parent root;

    public PanelNuevaSimDescPaso1(PanelSimuladorDesc panelPadre) {
        this.panelPadre = panelPadre;
        this.gramatica = panelPadre.gramatica;
        this.producciones = FXCollections.observableArrayList();
        cargarFXML();
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
        panelPadre.cambiarPaso(2);
    }

    @FXML
    private void visualizarGramatica() {
        panelPadre.mostrarGramaticaOriginal();
    }

    public Parent getRoot() {
        return root;
    }

}