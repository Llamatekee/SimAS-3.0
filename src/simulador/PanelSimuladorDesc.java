package simulador;

import gramatica.Gramatica;
import gramatica.NoTerminal;
import gramatica.Produccion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
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

    public PanelSimuladorDesc(Gramatica gramatica, TabPane tabPane) {
        this.gramatica = gramatica;
        this.gramaticaOriginal = new Gramatica();
        this.gramaticaOriginal.copiarDesde(gramatica);
        this.tabPane = tabPane;
        ArrayList<NoTerminal> noTerminales = new ArrayList<>();
        ArrayList<Produccion> pr = new ArrayList<>();
        cargarPaso1();
    }

    private void cargarPaso1() {
        // Crear instancia del controlador (esto ya carga su propio FXML internamente)
        PanelNuevaSimDescPaso1 paso1 = new PanelNuevaSimDescPaso1(this);

        // Crear una nueva pestaña y asignar el contenido del controlador
        Tab paso1tab = new Tab("Simulación: Paso 1", paso1.getRoot());

        // Agregar la pestaña al TabPane y seleccionarla
        tabPane.getTabs().add(paso1tab);
        tabPane.getSelectionModel().select(paso1tab);
    }

    public void cambiarPaso(int paso) {
        tabPane.getTabs().removeIf(tab -> tab.getText().startsWith("Simulación: Paso"));
        Tab nuevoPaso = new Tab("Simulación: Paso " + paso);

        switch (paso) {
            case 1:
                PanelNuevaSimDescPaso1 paso1 = new PanelNuevaSimDescPaso1(this);
                nuevoPaso.setContent(paso1.getRoot());
                break;
            case 2:
                PanelNuevaSimDescPaso2 paso2 = new PanelNuevaSimDescPaso2(this);
                nuevoPaso.setContent(paso2.getRoot());
                break;
            case 3:
                PanelNuevaSimDescPaso3 paso3 = new PanelNuevaSimDescPaso3(this);
                nuevoPaso.setContent(paso3.getRoot());
                break;
            case 4:
                PanelNuevaSimDescPaso4 paso4 = new PanelNuevaSimDescPaso4(this);
                nuevoPaso.setContent(paso4.getRoot());
                break;
            /*default:
                nuevoPaso.setContent(new AnchorPane()); // Paso no definido
                break;*/
        }

        tabPane.getTabs().add(nuevoPaso);
        tabPane.getSelectionModel().select(nuevoPaso);
    }


    /**
     * Ventana emergente para visualizar la gramática original.
     */
    public void mostrarGramaticaOriginal() {
        if (ventanaGramatica != null) { // Si ya está abierta, traerla al frente
            ventanaGramatica.toFront();
            return;
        }

        ventanaGramatica = new Stage(); // Guardamos la ventana
        ventanaGramatica.initModality(Modality.NONE);
        ventanaGramatica.setTitle("Gramática Original");

        Label titulo = new Label("Producciones Originales:");
        titulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        ObservableList<String> producciones = FXCollections.observableArrayList(gramaticaOriginal.getProduccionesModel());
        ListView<String> listView = new ListView<>(producciones);
        listView.setPrefSize(400, 300);

        Button cerrar = new Button("Cerrar");
        cerrar.setOnAction(e -> {
            ventanaGramatica.close();
            ventanaGramatica = null; // Liberar la referencia al cerrarse
        });
        cerrar.setStyle("-fx-background-color: #bf616a; -fx-text-fill: white; -fx-font-weight: bold;");

        VBox layout = new VBox(10, titulo, listView, cerrar);
        layout.setStyle("-fx-padding: 15px; -fx-background-color: #2e3440;");

        Scene escena = new Scene(layout);
        ventanaGramatica.setScene(escena);
        ventanaGramatica.setOnCloseRequest(e -> ventanaGramatica = null); // Liberar la referencia al cerrarse
        ventanaGramatica.show();
    }

    public void cancelarSimulacion() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Desea salir del asistente de simulación?",
                ButtonType.YES, ButtonType.NO);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                // 🔹 Cerrar la ventana de la gramática si está abierta
                if (ventanaGramatica != null) {
                    ventanaGramatica.close();
                    ventanaGramatica = null; // 🔹 Liberar referencia
                }

                // Cerrar todas las pestañas de la simulación
                tabPane.getTabs().removeIf(tab -> tab.getText().startsWith("Simulación: Paso"));
                tabPane.getTabs().removeIf(tab -> tab.getText().equals("Simulador"));
            }
        });
    }

}
