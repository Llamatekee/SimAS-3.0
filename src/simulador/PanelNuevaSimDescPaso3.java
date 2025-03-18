package simulador;

import gramatica.FilaTablaPredictiva;
import gramatica.Gramatica;
import gramatica.TablaPredictiva;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.io.IOException;

/**
 * Controlador para el Paso 3 de la Simulación Descendente.
 * Genera y muestra la Tabla Predictiva.
 */
public class PanelNuevaSimDescPaso3 {

    @FXML private TableView<FilaTablaPredictiva> tablaPredictiva;
    private Parent root;

    private final PanelSimuladorDesc panelPadre;
    private final Gramatica gramatica;
    private ObservableList<FilaTablaPredictiva> datosTabla;

    public PanelNuevaSimDescPaso3(PanelSimuladorDesc panelPadre) {
        this.panelPadre = panelPadre;
        this.gramatica = panelPadre.gramatica;
        cargarFXML();
        construirTablaPredictiva();
    }

    private void cargarFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/PanelNuevaSimDescPaso3.fxml"));
            loader.setController(this);
            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void construirTablaPredictiva() {
        if (gramatica.getProducciones().get(0).getNumero() == 0) { // 🔥 Evitar numerar si ya están numeradas
            gramatica.numerarProducciones();
        }
        gramatica.generarTPredictiva(); // Generar tabla predictiva
        TablaPredictiva tpredictiva = new TablaPredictiva(tablaPredictiva); // 🔥 Ahora pasa la tabla del FXML
        tpredictiva.construir(gramatica);

        if (!tablaPredictiva.getColumns().contains("$")) {
            TableColumn<FilaTablaPredictiva, String> columnaFinCadena = new TableColumn<>("$");
            tablaPredictiva.getColumns().add(columnaFinCadena);
        }

        tablaPredictiva.refresh(); // Refrescar la UI
    }


    @FXML
    private void cancelarSimulacion() {
        panelPadre.cancelarSimulacion();
    }

    @FXML
    private void avanzarPaso() {
        panelPadre.cambiarPaso(4);
    }

    @FXML
    private void retrocederPaso() {
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