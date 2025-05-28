package simulador;

import gramatica.Gramatica;
import gramatica.TablaPredictivaPaso5;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import java.io.IOException;

public class SimulacionFinal extends BorderPane {
    @FXML private TextField campoEntrada;
    @FXML private Button btnIniciar;
    @FXML private Button btnPaso;
    @FXML private Button btnFinal;
    @FXML private Button btnRetroceso;
    @FXML private Button btnInicio;
    @FXML private TableView<String> tablaPila;
    @FXML private TableView<String> tablaEntrada;
    @FXML private TextArea areaMensajes;

    private Gramatica gramatica;
    private TablaPredictivaPaso5 tablaPredictiva;

    public SimulacionFinal(Gramatica gramatica, TablaPredictivaPaso5 tablaPredictiva) {
        this.gramatica = gramatica;
        this.tablaPredictiva = tablaPredictiva;
        cargarFXML();
    }

    private void cargarFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/SimulacionFinal.fxml"));
            loader.setController(this);
            Parent root = loader.load();
            this.setCenter(root);
        } catch (IOException e) {
            // Manejo de error de carga de FXML
        }
    }

    // Métodos para manejar los botones y la simulación se implementarán aquí
} 