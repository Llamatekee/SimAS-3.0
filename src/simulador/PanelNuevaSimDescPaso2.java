package simulador;

import gramatica.Gramatica;
import gramatica.NoTerminal;
import gramatica.Terminal;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Label;
import editor.ActualizableTextos;

import java.io.IOException;
import java.util.ResourceBundle;

/**
 * Controlador para el Paso 2 de la Simulación Descendente.
 * Muestra los Conjuntos Primero y Siguiente de cada No Terminal.
 */
public class PanelNuevaSimDescPaso2 implements PanelNuevaSimDescPaso, ActualizableTextos {

    @FXML private TableView<NoTerminalData> tablaConjuntos;
    @FXML private TableColumn<NoTerminalData, String> colSimbolo;
    @FXML private TableColumn<NoTerminalData, String> colPrimero;
    @FXML private TableColumn<NoTerminalData, String> colSiguiente;
    @FXML private Button btnCancelar;
    @FXML private Button btnAnterior;
    @FXML private Button btnSiguiente;
    @FXML private Button btnPrimero;
    @FXML private Button btnUltimo;
    @FXML private Button btnVisualizarGramatica;
    @FXML private Label lblTitulo;
    @FXML private Label lblConjuntos;
    private Parent root;

    private final PanelSimuladorDesc panelPadre;
    private final Gramatica gramatica;
    private ObservableList<NoTerminalData> datosConjuntos;
    private ResourceBundle bundle;

    public PanelNuevaSimDescPaso2(PanelSimuladorDesc panelPadre) {
        this.panelPadre = panelPadre;
        this.gramatica = panelPadre.gramatica;
        this.bundle = panelPadre.getBundle();
        cargarFXML();
        construirConjuntos();
    }

    @FXML
    private void initialize() {
        // Este método se llama automáticamente después de cargar el FXML
        if (tablaConjuntos != null) {
            tablaConjuntos.setPlaceholder(new Label("No hay datos disponibles"));
        }
    }

    private void cargarFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/PanelNuevaSimDescPaso2.fxml"));
            loader.setController(this);
            loader.setResources(bundle);
            root = loader.load();
            
            // Inicializar los textos después de cargar el FXML
            if (lblTitulo != null) lblTitulo.setText(bundle.getString("simulador.paso2.titulo"));
            if (lblConjuntos != null) lblConjuntos.setText(bundle.getString("simulador.paso2.conjuntos"));
            if (colSimbolo != null) colSimbolo.setText(bundle.getString("simulador.paso2.columna.simbolo"));
            if (colPrimero != null) colPrimero.setText(bundle.getString("simulador.paso2.columna.primero"));
            if (colSiguiente != null) colSiguiente.setText(bundle.getString("simulador.paso2.columna.siguiente"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void construirConjuntos() {
        gramatica.generarConjPrim();  // Calcular Conjunto Primero
        gramatica.generarConjSig();   // Calcular Conjunto Siguiente

        datosConjuntos = FXCollections.observableArrayList();
        for (NoTerminal nt : gramatica.getNoTerminales()) {
            String primeros = nt.getPrimeros().isEmpty() ? "ε" : 
                String.join(" ", nt.getPrimeros().stream().map(Terminal::getNombre).toList());
            String siguientes = nt.getSiguientes().isEmpty() ? "ε" : 
                String.join(" ", nt.getSiguientes().stream().map(Terminal::getNombre).toList());
            datosConjuntos.add(new NoTerminalData(nt.getNombre(), primeros, siguientes));
        }
        
        // Configurar la tabla
        tablaConjuntos.setItems(datosConjuntos);
        tablaConjuntos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tablaConjuntos.setTableMenuButtonVisible(false);
        
        // Asegurar que las columnas tengan el ancho correcto
        if (colSimbolo != null) colSimbolo.setPrefWidth(120);
        if (colPrimero != null) colPrimero.setPrefWidth(250);
        if (colSiguiente != null) colSiguiente.setPrefWidth(250);
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
    private void retrocederPaso() {
        panelPadre.cambiarPaso(0);
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
        panelPadre.cambiarPaso(4);
    }

    public Parent getRoot() {
        return root;
    }

    @Override
    public void actualizarTextos(ResourceBundle bundle) {
        this.bundle = bundle;
        
        // Actualizar textos de la interfaz
        if (lblTitulo != null) lblTitulo.setText(bundle.getString("simulador.paso2.titulo"));
        if (lblConjuntos != null) lblConjuntos.setText(bundle.getString("simulador.paso2.conjuntos"));
        
        // Actualizar textos de las columnas
        colSimbolo.setText(bundle.getString("simulador.paso2.columna.simbolo"));
        colPrimero.setText(bundle.getString("simulador.paso2.columna.primero"));
        colSiguiente.setText(bundle.getString("simulador.paso2.columna.siguiente"));
        
        // Actualizar textos de los botones
        btnCancelar.setText(bundle.getString("button.cancelar"));
        btnAnterior.setText(bundle.getString("button.anterior"));
        btnSiguiente.setText(bundle.getString("button.siguiente"));
        btnPrimero.setText(bundle.getString("button.primero"));
        btnUltimo.setText(bundle.getString("button.ultimo"));
        btnVisualizarGramatica.setText(bundle.getString("simulador.paso1.btn.gramatica"));
    }

    /**
     * Clase auxiliar para mostrar los datos en la TableView.
     */
    public static class NoTerminalData {
        private final String simbolo;
        private final String conjuntoPrimero;
        private final String conjuntoSiguiente;

        public NoTerminalData(String simbolo, String conjuntoPrimero, String conjuntoSiguiente) {
            this.simbolo = simbolo;
            this.conjuntoPrimero = conjuntoPrimero;
            this.conjuntoSiguiente = conjuntoSiguiente;
        }

        public String getSimbolo() { return simbolo; }
        public String getConjuntoPrimero() { return conjuntoPrimero; }
        public String getConjuntoSiguiente() { return conjuntoSiguiente; }
    }
}
