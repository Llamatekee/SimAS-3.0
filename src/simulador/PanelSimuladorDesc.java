package simulador;

import gramatica.Gramatica;
import gramatica.FuncionError;
import gramatica.TablaPredictiva;
import gramatica.Terminal;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;

/**
 * Controlador para la simulación descendente en JavaFX.
 */
public class PanelSimuladorDesc {

    @FXML
    public final TabPane tabPane;

    public Gramatica gramatica;
    private final Gramatica gramaticaOriginal;
    private Stage ventanaGramatica;
    private Tab pestañaSimulacion;
    private int pasoActual;
    private ArrayList<PanelNuevaSimDescPaso> pasos;

    public PanelSimuladorDesc(Gramatica gramatica, TabPane tabPane) {
        this.gramatica = gramatica;
        this.gramaticaOriginal = gramatica;
        this.tabPane = tabPane;
        
        // Inicializar funciones de error al principio
        inicializarFuncionesError();
        
        // Inicializar pasos
        pasos = new ArrayList<>();
        pasos.add(new PanelNuevaSimDescPaso1(this));
        pasos.add(new PanelNuevaSimDescPaso2(this));
        pasos.add(new PanelNuevaSimDescPaso3(this));
        pasos.add(new PanelNuevaSimDescPaso4(this));
        pasos.add(new PanelNuevaSimDescPaso5(this));
        
        // Mostrar el primer paso
        mostrarPasoActual();
    }

    /**
     * Inicializa las funciones de error al principio de la simulación
     */
    private void inicializarFuncionesError() {
        if (this.gramatica.getTPredictiva().getFuncionesError().isEmpty()) {
            ObservableList<String> simbolosTerminales = this.gramatica.getTerminalesModel();
            TablaPredictiva tPredictiva = this.gramatica.getTPredictiva();
            
            // Función de error inicial
            FuncionError funErrorInicial = new FuncionError(0, 7, "");
            tPredictiva.crearFunError(funErrorInicial);
            
            // Función de error inicial 2
            FuncionError funErrorInicial2 = new FuncionError(1, 2, "");
            tPredictiva.crearFunError(funErrorInicial2);
            
            // Funciones de error para cada terminal
            int x = 2;
            for (int w = 0; w < simbolosTerminales.size(); w++) {
                FuncionError funError = new FuncionError(x, 1, "");
                Terminal term = new Terminal(simbolosTerminales.get(w).toString(), simbolosTerminales.get(w).toString());
                funError.setSimbolo(term);
                tPredictiva.crearFunError(funError);
                x++;
            }
            
            this.gramatica.setTPredictiva(tPredictiva);
        }
    }

    private void mostrarPasoActual() {
        pestañaSimulacion = new Tab("Simulación: Paso " + (pasoActual + 1));
        pestañaSimulacion.setClosable(false);
        pestañaSimulacion.setContent(pasos.get(pasoActual).getRoot());

        tabPane.getTabs().add(pestañaSimulacion);
        tabPane.getSelectionModel().select(pestañaSimulacion);
    }

    /**
     * Ventana emergente para visualizar la gramática original.
     */
    public void mostrarGramaticaOriginal() {
        if (ventanaGramatica != null) {
            ventanaGramatica.toFront();
            return;
        }
        
        try {
            ventanaGramatica = new Stage();
            ventanaGramatica.initModality(Modality.NONE);
            ventanaGramatica.setTitle("Gramática Original");
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/VentanaGramaticaOriginal.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/vistas/styles.css").toExternalForm());
            
            // Obtener los elementos del FXML
            ListView<String> listView = (ListView<String>) scene.lookup("#listViewProducciones");
            Button btnCerrar = (Button) scene.lookup(".button-cancel");
            
            // Configurar la lista de producciones
            listView.setItems(FXCollections.observableArrayList(gramaticaOriginal.getProduccionesModel()));
            
            // Configurar el botón de cerrar
            btnCerrar.setOnAction(e -> {
                ventanaGramatica.close();
                ventanaGramatica = null;
            });
            
            ventanaGramatica.setScene(scene);
            ventanaGramatica.setOnCloseRequest(e -> ventanaGramatica = null);
            ventanaGramatica.show();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cancelarSimulacion() {
        // Cerrar la pestaña de simulación si existe
        if (pestañaSimulacion != null) {
            tabPane.getTabs().remove(pestañaSimulacion);
            pestañaSimulacion = null;
        }
        // Seleccionar la primera pestaña
        tabPane.getSelectionModel().select(0);
    }

    public void cambiarPaso(int paso) {
        // Actualizar el título de la pestaña
        pestañaSimulacion.setText("Simulación: Paso " + (paso + 1));

        // Actualizar el contenido según el paso
        pestañaSimulacion.setContent(pasos.get(paso).getRoot());

        // Asegurarse de que la pestaña esté seleccionada
        tabPane.getSelectionModel().select(pestañaSimulacion);
    }
}
