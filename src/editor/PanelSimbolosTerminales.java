package editor;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class PanelSimbolosTerminales extends VBox {

    @FXML private FlowPane symbolButtonsPane;
    @FXML private TextField txtSimboloTerminal;
    @FXML private ListView<String> listSimbolosTerminales;
    private ObservableList<String> simbolosTerminales;
    private ObservableList<String> simbolosTemporales; // Nueva lista temporal
    private Set<String> simbolosSet;
    private TabPane tabPane;
    private PanelCreacionGramaticaPaso2 panelPadre;

    // Lista de símbolos predefinidos
    private final String[] simbolosPredefinidos = {"+", "-", "*", "/", "(", ")", "{", "}", "=", ".", ",", ";", ":"};

    public PanelSimbolosTerminales(ObservableList<String> simbolosTerminales, TabPane tabPane, PanelCreacionGramaticaPaso2 panelPadre) {
        this.simbolosTerminales = simbolosTerminales;
        this.simbolosTemporales = FXCollections.observableArrayList(simbolosTerminales); // Copia temporal
        this.simbolosSet = new HashSet<>(simbolosTemporales);
        this.tabPane = tabPane;
        this.panelPadre = panelPadre;
        cargarFXML();
    }

    private void cargarFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/PanelSimbolosTerminales.fxml"));
            loader.setController(this);
            Parent root = loader.load();
            this.getChildren().add(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void initialize() {
        listSimbolosTerminales.setItems(simbolosTemporales); // Usamos la lista temporal
        generarBotonesPredefinidos();
    }

    private void generarBotonesPredefinidos() {
        for (String simbolo : simbolosPredefinidos) {
            Button btnSimbolo = new Button(simbolo);
            btnSimbolo.setStyle("-fx-font-size: 14px; -fx-padding: 5px; -fx-background-color: #3498db; -fx-text-fill: white; -fx-border-radius: 5px;");
            btnSimbolo.setOnAction(event -> agregarSimbolo(simbolo));
            symbolButtonsPane.getChildren().add(btnSimbolo);
        }
    }

    private void agregarSimbolo(String simbolo) {
        if (simbolosSet.add(simbolo)) {
            simbolosTemporales.add(simbolo); // Agregamos solo a la lista temporal
        } else {
            panelPadre.panelPadre.mostrarAlerta("Símbolo Duplicado", "El símbolo ya está en la lista.");
        }
    }

    @FXML
    private void onInsertarAction() {
        String nuevoSimbolo = txtSimboloTerminal.getText().trim();
        if (!nuevoSimbolo.isEmpty() && simbolosSet.add(nuevoSimbolo)) {
            simbolosTemporales.add(nuevoSimbolo); // Agregamos solo a la lista temporal
            txtSimboloTerminal.clear();
        } else {
            panelPadre.panelPadre.mostrarAlerta("Error", "El símbolo ya existe o es inválido.");
        }
    }

    @FXML
    private void onEliminarAction() {
        String seleccionado = listSimbolosTerminales.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            simbolosSet.remove(seleccionado);
            simbolosTemporales.remove(seleccionado); // Eliminamos solo de la lista temporal
        }
    }

    @FXML
    private void onAceptarAction() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Desea guardar los cambios y salir?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                simbolosTerminales.setAll(simbolosTemporales);
                panelPadre.panelPadre.getGramatica().setTerminalesModel(simbolosTerminales);// Guardamos cambios en la lista original
                cerrarPestanaActual();
            }
        });
    }


    @FXML
    private void onCancelarAction() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Desea borrar los cambios y salir?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                cerrarPestanaActual();
            }
        });
    }

    private String mostrarDialogoModificarSimbolo(String simboloActual) {
        TextInputDialog dialog = new TextInputDialog(simboloActual);
        dialog.setTitle("Modificar Símbolo");
        dialog.setHeaderText("Modificar Símbolo No Terminal");
        dialog.setContentText("Nuevo valor para el símbolo:");

        // Obtener el nuevo valor del símbolo
        return dialog.showAndWait().orElse(null);
    }

    // Método para modificar un símbolo no terminal existente
    @FXML
    private void onModificarAction() {
        String simboloSeleccionado = listSimbolosTerminales.getSelectionModel().getSelectedItem();

        if (simboloSeleccionado != null) {
            String nuevoSimbolo = mostrarDialogoModificarSimbolo(simboloSeleccionado).trim();

            if (!nuevoSimbolo.isEmpty() && !simbolosSet.contains(nuevoSimbolo)) {
                // Actualizar el conjunto y la lista temporal
                simbolosSet.remove(simboloSeleccionado);
                simbolosSet.add(nuevoSimbolo);
                simbolosTemporales.set(simbolosTemporales.indexOf(simboloSeleccionado), nuevoSimbolo);

                // Actualizar las producciones de la gramática
                panelPadre.panelPadre.getGramatica().modificarSimboloProduccion(simboloSeleccionado, nuevoSimbolo);
            } else {
                panelPadre.panelPadre.mostrarAlerta("Error", "El símbolo ya existe o es inválido.");
            }
        } else {
            panelPadre.panelPadre.mostrarAlerta("Error", "No se ha seleccionado ningún símbolo.");
        }
    }

    public void cerrarPestanaActual() {
        if (tabPane != null) {
            Tab tabActual = null;
            for (Tab tab : tabPane.getTabs()) {
                if (tab.getContent() == this) {  // Verifica si el contenido de la pestaña es el panel actual
                    tabActual = tab;
                    break;
                }
            }
            if (tabActual != null) {
                tabPane.getTabs().remove(tabActual); // Cierra la pestaña actual
            } else {
                System.err.println("No se encontró la pestaña actual.");
            }
        } else {
            System.err.println("TabPane es null, no se puede cerrar la pestaña.");
        }
    }

}
