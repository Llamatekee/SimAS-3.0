package simulador;

import gramatica.Gramatica;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Button;
import editor.ActualizableTextos;

import java.io.IOException;
import java.util.ResourceBundle;

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
    @FXML private Button btnCancelar;
    @FXML private Button btnVisualizarGramatica;
    @FXML private Button btnPrimero;
    @FXML private Button btnAnterior;
    @FXML private Button btnSiguiente;
    @FXML private Button btnUltimo;

    private final PanelSimuladorDesc panelPadre;
    private final Gramatica gramatica;
    private ResourceBundle bundle;
    private Parent root;

    public PanelNuevaSimDescPaso1(PanelSimuladorDesc panelPadre) {
        this.panelPadre = panelPadre;
        this.gramatica = panelPadre.gramatica;
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
        inicializarBotones();
        verificarEstadoGramatica();
    }

    private void inicializarBotones() {
        // En el paso 1, los botones Primero y Anterior están deshabilitados
        if (btnPrimero != null) btnPrimero.setDisable(true);
        if (btnAnterior != null) btnAnterior.setDisable(true);
    }

    private void verificarEstadoGramatica() {
        boolean esRecursiva = gramatica.eliminarRecursividad();
        boolean necesitaFactorizacion = gramatica.factorizar();

        // Limpiar estilos previos
        lblRecursividad.getStyleClass().removeAll("error-label", "wizard-label");
        lblFactorizacion.getStyleClass().removeAll("error-label", "wizard-label");
        lblEstadoGramatica.getStyleClass().removeAll("error-label", "wizard-label");

        if (esRecursiva) {
            lblRecursividad.setText(bundle.getString("simulador.gramatica.recursiva"));
            lblRecursividad.getStyleClass().add("error-label");
        } else {
            lblRecursividad.setText("");
        }
        
        if (necesitaFactorizacion) {
            lblFactorizacion.setText(bundle.getString("simulador.gramatica.no.factorizada"));
            lblFactorizacion.getStyleClass().add("error-label");
        } else {
            lblFactorizacion.setText("");
        }
        
        if (!esRecursiva && !necesitaFactorizacion) {
            lblEstadoGramatica.setText(bundle.getString("simulador.gramatica.correcta"));
            lblEstadoGramatica.getStyleClass().add("wizard-label");
        } else {
            lblEstadoGramatica.setText("");
        }
        
        listProducciones.setItems(gramatica.getProduccionesModel());
    }

    @FXML
    private void cancelarSimulacion() {
        if (panelPadre != null) {
            panelPadre.cancelarSimulacion();
        }
    }

    @FXML
    private void handleSiguiente() {
        if (panelPadre != null) {
            panelPadre.cambiarPaso(1);
        }
    }

    @FXML
    private void handleUltimo() {
        if (panelPadre != null) {
            panelPadre.cambiarPaso(5);
        }
    }

    @FXML
    private void visualizarGramatica() {
        if (panelPadre != null) {
            panelPadre.mostrarGramaticaOriginal();
        }
    }

    @FXML
    private void handlePrimero() {
        if (panelPadre != null) {
            panelPadre.cambiarPaso(0);
        }
    }

    @FXML
    private void handleAnterior() {
        // No hay paso anterior, este es el primer paso
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