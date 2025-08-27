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
        // Los botones y el estado se inicializan automáticamente en el método initialize()
    }

    private void cargarFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/PanelNuevaSimDescPaso1.fxml"));
            loader.setController(this);
            loader.setResources(bundle);
            root = loader.load();
            
            // Los textos se inicializan automáticamente desde el FXML
            // El método initialize() se llamará automáticamente después
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void initialize() {
        // Este método se llama automáticamente después de cargar el FXML
        // Inicializar los textos con el bundle actual
        actualizarTextos(bundle);
        inicializarBotones();
        verificarEstadoGramatica();
    }

    private void inicializarBotones() {
        // En el paso 1, los botones Primero y Anterior están deshabilitados
        if (btnPrimero != null) btnPrimero.setDisable(true);
        if (btnAnterior != null) btnAnterior.setDisable(true);
        
        // Habilitar los botones Siguiente y Último
        if (btnSiguiente != null) btnSiguiente.setDisable(false);
        if (btnUltimo != null) btnUltimo.setDisable(false);
    }

    private void verificarEstadoGramatica() {
        boolean esRecursiva = gramatica.eliminarRecursividad();
        boolean necesitaFactorizacion = gramatica.factorizar();

        // Verificar que los labels existan antes de usarlos
        if (lblRecursividad != null) {
            // Limpiar estilos previos
            lblRecursividad.getStyleClass().removeAll("error-label", "wizard-label");
            
            if (esRecursiva) {
                lblRecursividad.setText(bundle.getString("simulador.gramatica.recursiva"));
                lblRecursividad.getStyleClass().add("error-label");
            } else {
                lblRecursividad.setText("");
            }
        }
        
        if (lblFactorizacion != null) {
            // Limpiar estilos previos
            lblFactorizacion.getStyleClass().removeAll("error-label", "wizard-label");
            
            if (necesitaFactorizacion) {
                lblFactorizacion.setText(bundle.getString("simulador.gramatica.no.factorizada"));
                lblFactorizacion.getStyleClass().add("error-label");
            } else {
                lblFactorizacion.setText("");
            }
        }
        
        if (lblEstadoGramatica != null) {
            // Limpiar estilos previos
            lblEstadoGramatica.getStyleClass().removeAll("error-label", "wizard-label");
            
            if (!esRecursiva && !necesitaFactorizacion) {
                lblEstadoGramatica.setText(bundle.getString("simulador.gramatica.correcta"));
                lblEstadoGramatica.getStyleClass().add("wizard-label");
            } else {
                lblEstadoGramatica.setText("");
            }
        }
        
        if (listProducciones != null) {
            listProducciones.setItems(gramatica.getProduccionesModel());
        }
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
            panelPadre.cambiarPaso(4);
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
        
        // Actualizar los textos directamente como en el editor
        if (lblTitulo != null) lblTitulo.setText(bundle.getString("simulador.window.paso1"));
        if (lblEstadoTitulo != null) lblEstadoTitulo.setText(bundle.getString("simulador.paso1.estado.titulo"));
        if (lblProduccionesTitulo != null) lblProduccionesTitulo.setText(bundle.getString("simulador.paso1.producciones.titulo"));
        
        // Actualizar textos de los botones de navegación
        if (btnCancelar != null) btnCancelar.setText(bundle.getString("button.cancelar"));
        if (btnAnterior != null) btnAnterior.setText(bundle.getString("button.anterior"));
        if (btnSiguiente != null) btnSiguiente.setText(bundle.getString("button.siguiente"));
        if (btnPrimero != null) btnPrimero.setText(bundle.getString("button.primero"));
        if (btnUltimo != null) btnUltimo.setText(bundle.getString("button.ultimo"));
        if (btnVisualizarGramatica != null) btnVisualizarGramatica.setText(bundle.getString("simulador.paso1.btn.gramatica"));
        
        // Actualizar el estado de la gramática
        verificarEstadoGramatica();
    }
}