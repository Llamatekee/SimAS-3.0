package simulador;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import gramatica.FuncionError;
import gramatica.Gramatica;
import gramatica.Terminal;

public class PanelNuevaSimDescPaso5 {

    @FXML private Label labelTitulo;
    @FXML private Button buttonAnterior;
    @FXML private Button buttonSiguiente;
    @FXML private Button buttonCancelar;
    @FXML private Button buttonEliminar;
    @FXML private Button buttonRellenar;
    @FXML private Button buttonGramatica;
    @FXML private ComboBox<String> comboBoxFuncionesError;
    @FXML private TableView<FuncionError> tableViewFuncionesError;
    @FXML private TableColumn<FuncionError, String> columnSimbolo;
    @FXML private TableColumn<FuncionError, String> columnAccion;

    private Parent root;
    private PanelSimuladorDesc panelPadre;
    private Gramatica gramatica;
    private List<Integer> filasInvalidas = new ArrayList<>();

    public PanelNuevaSimDescPaso5(PanelSimuladorDesc panelPadre) {
        this.panelPadre = panelPadre;
        this.gramatica = panelPadre.getGramatica();
        cargarFXML();
        initialize();
    }

    private void cargarFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/PanelNuevaSimDescPaso5.fxml"));
            loader.setController(this);
            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Parent getRoot() {
        return root;
    }

    private void initialize() {
        // Inicializar columnas de la tabla
        columnSimbolo.setCellValueFactory(new PropertyValueFactory<>("simbolo"));
        columnAccion.setCellValueFactory(new PropertyValueFactory<>("accion"));

        // Inicializar comboBoxFuncionesError con valores
        List<String> funcionesError = new ArrayList<>();
        for (FuncionError funcionError : gramatica.getTPredictiva().getFuncionesError()) {
            funcionesError.add(funcionError.toString());
        }
        comboBoxFuncionesError.getItems().addAll(funcionesError);

        // Inicializar tabla con funciones de error
        tableViewFuncionesError.getItems().addAll(gramatica.getTPredictiva().getFuncionesError());

        // Añadir terminales y símbolo $ como nuevas filas
        List<Terminal> terminales = new ArrayList<>(gramatica.getTerminales());
        terminales.add(new Terminal("$", "$"));
        for (Terminal terminal : terminales) {
            FuncionError fila = new FuncionError();
            fila.setSimbolo(terminal);
            tableViewFuncionesError.getItems().add(fila);
        }
    }

    @FXML
    private void handleAnterior() {
        panelPadre.cambiarPaso(4);
    }

    @FXML
    private void handleSiguiente() { //Finalizar
        // Validar las funciones de error
        if (validarFuncionesError()) {
            panelPadre.cambiarPaso(6); // Cambiar al siguiente paso SIMULADOR
        } else {
            // Mostrar un mensaje de error si la validación falla
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Validación fallida");
            alert.setContentText("Por favor, asegúrese de que todas las funciones de error estén correctamente definidas.");
            alert.showAndWait();
        }
    }

    private boolean validarFuncionesError() {
        // Implementar la lógica de validación de las funciones de error
        return true;
    }

    @FXML
    private void handleCancelar() {
        panelPadre.cancelarSimulacion();
    }

    @FXML
    private void handleEliminar() {
        FuncionError seleccionada = tableViewFuncionesError.getSelectionModel().getSelectedItem();
        if (seleccionada != null) {
            gramatica.getTPredictiva().getFuncionesError().remove(seleccionada);
            tableViewFuncionesError.getItems().remove(seleccionada);
            comboBoxFuncionesError.getItems().remove(seleccionada.toString());
        }
    }

    @FXML
    private void handleRellenar() {
        // Lógica para rellenar la tabla con producciones épsilon
        List<FuncionError> produccionesEpsilon = obtenerProduccionesEpsilon();
        for (FuncionError funcionError : produccionesEpsilon) {
            if (!tableViewFuncionesError.getItems().contains(funcionError)) {
                tableViewFuncionesError.getItems().add(funcionError);
                gramatica.getTPredictiva().getFuncionesError().add(funcionError);
                comboBoxFuncionesError.getItems().add(funcionError.toString());
            }
        }
    }
    
    private List<FuncionError> obtenerProduccionesEpsilon() {
        // Implementar la lógica para obtener las producciones épsilon
        return new ArrayList<>();
    }

    @FXML
    private void handleGramatica() {
        panelPadre.mostrarGramaticaOriginal();
    }
}