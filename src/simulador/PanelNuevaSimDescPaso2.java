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

import java.io.IOException;

/**
 * Controlador para el Paso 2 de la Simulación Descendente.
 * Muestra los Conjuntos Primero y Siguiente de cada No Terminal.
 */
public class PanelNuevaSimDescPaso2 {

    @FXML private TableView<NoTerminalData> tablaConjuntos;
    @FXML private TableColumn<NoTerminalData, String> colSimbolo;
    @FXML private TableColumn<NoTerminalData, String> colPrimero;
    @FXML private TableColumn<NoTerminalData, String> colSiguiente;
    @FXML private Button btnCancelar;
    @FXML private Button btnAnterior;
    @FXML private Button btnSiguiente;
    private Parent root;

    private final PanelSimuladorDesc panelPadre;
    private final Gramatica gramatica;
    private ObservableList<NoTerminalData> datosConjuntos;

    public PanelNuevaSimDescPaso2(PanelSimuladorDesc panelPadre) {
        this.panelPadre = panelPadre;
        this.gramatica = panelPadre.gramatica;
        cargarFXML();
        construirConjuntos();
    }

    private void cargarFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/PanelNuevaSimDescPaso2.fxml"));
            loader.setController(this);
            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void construirConjuntos() {
        gramatica.generarConjPrim();  // Calcular Conjunto Primero
        gramatica.generarConjSig();   // Calcular Conjunto Siguiente

        datosConjuntos = FXCollections.observableArrayList();
        for (NoTerminal nt : gramatica.getNoTerminales()) {
            String primeros = String.join(" ", nt.getPrimeros().stream().map(Terminal::getNombre).toList());
            String siguientes = String.join(" ", nt.getSiguientes().stream().map(Terminal::getNombre).toList());
            datosConjuntos.add(new NoTerminalData(nt.getNombre(), primeros, siguientes));
        }
        tablaConjuntos.setItems(datosConjuntos);
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

    public Parent getRoot() {
        return root;
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
