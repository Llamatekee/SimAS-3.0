package simulador;

import gramatica.FilaTablaPredictiva;
import gramatica.Gramatica;
import gramatica.TablaPredictiva;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.Label;
import editor.ActualizableTextos;

import java.io.IOException;
import java.util.ResourceBundle;

/**
 * Controlador para el Paso 3 de la Simulación Descendente.
 * Genera y muestra la Tabla Predictiva.
 */
public class PanelNuevaSimDescPaso3 implements PanelNuevaSimDescPaso, ActualizableTextos {

    @FXML private Label lblTitulo;
    @FXML private Label lblTabla;
    @FXML private TableView<FilaTablaPredictiva> tablaPredictiva;
    @FXML private Button btnPrimero;
    @FXML private Button btnAnterior;
    @FXML private Button btnSiguiente;
    @FXML private Button btnUltimo;
    @FXML private Button btnCancelar;
    @FXML private Button btnVisualizarGramatica;
    
    private Parent root;
    private final PanelSimuladorDesc panelPadre;
    private final Gramatica gramatica;
    private ResourceBundle bundle;

    public PanelNuevaSimDescPaso3(PanelSimuladorDesc panelPadre) {
        this.panelPadre = panelPadre;
        this.gramatica = panelPadre.gramatica;
        this.bundle = panelPadre.getBundle();
        cargarFXML();
        construirTablaPredictiva();
    }

    @FXML
    private void initialize() {
        // Este método se llama automáticamente después de cargar el FXML
        if (tablaPredictiva != null) {
            tablaPredictiva.setPlaceholder(new Label("No hay datos disponibles"));
        }
    }

    private void cargarFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/PanelNuevaSimDescPaso3.fxml"));
            loader.setController(this);
            loader.setResources(bundle);
            root = loader.load();
            
            // Inicializar los textos
            if (lblTitulo != null) lblTitulo.setText(bundle.getString("simulador.paso3.titulo"));
            if (lblTabla != null) lblTabla.setText(bundle.getString("simulador.paso3.tabla"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void construirTablaPredictiva() {
        if (gramatica != null) {
            // Asegurarse de que las producciones estén numeradas
            if (gramatica.getProducciones().get(0).getNumero() == 0) {
                gramatica.numerarProducciones();
            }
            
            // Limpiar la tabla actual
            tablaPredictiva.getColumns().clear();
            tablaPredictiva.getItems().clear();
            
            // Generar una nueva tabla predictiva
            gramatica.generarTPredictiva();
            TablaPredictiva tpredictiva = new TablaPredictiva(tablaPredictiva, bundle);
            tpredictiva.construir(gramatica);
            
            // Configurar la tabla
            tablaPredictiva.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            tablaPredictiva.setTableMenuButtonVisible(false);
            
            // Forzar un refresh completo
            tablaPredictiva.refresh();
        }
    }

    @FXML
    private void cancelarSimulacion() {
        panelPadre.cancelarSimulacion();
    }

    @FXML
    private void avanzarPaso() {
        panelPadre.cambiarPaso(3);
    }

    @FXML
    private void retrocederPaso() {
        panelPadre.cambiarPaso(1);
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
        if (lblTitulo != null) lblTitulo.setText(bundle.getString("simulador.paso3.titulo"));
        if (lblTabla != null) lblTabla.setText(bundle.getString("simulador.paso3.tabla"));
        if (btnPrimero != null) btnPrimero.setText(bundle.getString("button.primero"));
        if (btnAnterior != null) btnAnterior.setText(bundle.getString("button.anterior"));
        if (btnSiguiente != null) btnSiguiente.setText(bundle.getString("button.siguiente"));
        if (btnUltimo != null) btnUltimo.setText(bundle.getString("button.ultimo"));
        if (btnCancelar != null) btnCancelar.setText(bundle.getString("button.cancelar"));
        if (btnVisualizarGramatica != null) btnVisualizarGramatica.setText("Gramática");
    }
}