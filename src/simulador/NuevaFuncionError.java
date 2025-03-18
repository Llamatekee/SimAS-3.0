package simulador;

import gramatica.FuncionError;
import gramatica.Gramatica;
import gramatica.Terminal;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NuevaFuncionError {

    @FXML private Label labelTitulo;
    @FXML private Label labelIdentificador;
    @FXML private TextField textFieldIdentificador;
    @FXML private Label labelAccion;
    @FXML private ComboBox<String> comboBoxAccion;
    @FXML private Label labelSimbolo;
    @FXML private ComboBox<String> comboBoxSimbolo;
    @FXML private Label labelMensaje;
    @FXML private TextField textFieldMensaje;
    @FXML private Button buttonAceptar;
    @FXML private Button buttonCancelar;

    private Parent root;
    private Gramatica gramatica;
    private PanelNuevaSimDescPaso4 paso4;

    public NuevaFuncionError(Gramatica gramatica, PanelNuevaSimDescPaso4 paso4) {
        this.gramatica = gramatica;
        this.paso4 = paso4;
        cargarFXML();
        initialize();
    }

    private void cargarFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/NuevaFuncionError.fxml"));
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
        // Inicializar comboBoxAccion con valores
        ObservableList<String> acciones = FXCollections.observableArrayList(
            "Insertar un Símbolo en la Entrada",
            "Borrar un Símbolo de la Entrada",
            "Modificar un Símbolo de la Entrada",
            "Insertar un Símbolo de la Pila",
            "Borrar un Símbolo de la Pila",
            "Modificar un Símbolo de la Pila",
            "Terminar el análisis"
        );
        comboBoxAccion.setItems(acciones);
    
        // Deshabilitar opciones si ya están definidas
        List<FuncionError> funcionesError = gramatica.getTPredictiva().getFuncionesError();
        for (FuncionError funcionError : funcionesError) {
            if (funcionError.getAccion() == 7 || funcionError.getAccion() == 2) {
                comboBoxAccion.getItems().set(funcionError.getAccion() - 1, acciones.get(funcionError.getAccion() - 1) + " (Definida)");
            }
        }
    
        // Aplicar estilos CSS
        comboBoxAccion.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item);
                if (item != null && (item.contains("Terminar el análisis (Definida)") || item.contains("Borrar un Símbolo de la Entrada (Definida)"))) {
                    setDisable(true);
                    setStyle("-fx-text-fill: gray; -fx-opacity: 0.5;");
                } else {
                    setDisable(false);
                    setStyle("");
                }
            }
        });
    
        // Inicializar comboBoxSimbolo con valores de la gramática
        List<String> simbolos = new ArrayList<>();
        for (String terminal : gramatica.getTerminalesModel()) {
            simbolos.add(terminal);
        }
        ObservableList<String> observableSimbolos = FXCollections.observableArrayList(simbolos);
        comboBoxSimbolo.setItems(observableSimbolos);
    
        // Inicializar el campo de texto del identificador
        textFieldIdentificador.setText(String.valueOf(obtenerNuevoIdentificador()));
    }

    @FXML
    private void handleAceptar() {
        try {
            int id = obtenerNuevoIdentificador();
            int accion = comboBoxAccion.getSelectionModel().getSelectedIndex() + 1;
            String mensaje = textFieldMensaje.getText();
            String simbolo = comboBoxSimbolo.getSelectionModel().getSelectedItem();
    
            if ((accion != 7 && accion != 2) && (simbolo == null || simbolo.isEmpty())) {
                throw new IllegalArgumentException("Debe seleccionar un símbolo.");
            }
    
            Terminal term = (accion == 7 || accion == 2) ? null : new Terminal(simbolo, simbolo);
            FuncionError nuevaFuncionError = new FuncionError(id, accion, mensaje);
            nuevaFuncionError.setSimbolo(term);
    
            // Obtener la lista más reciente de funciones de error
            List<FuncionError> funcionesError = gramatica.getTPredictiva().getFuncionesError();
    
            // Verificar si la función de error ya está definida
            for (FuncionError funcionError : funcionesError) {
                if (funcionError.getAccion() == nuevaFuncionError.getAccion() &&
                    (funcionError.getSimbolo() == null || funcionError.getSimbolo().getNombre().equals(nuevaFuncionError.getSimbolo().getNombre()))) {
                    throw new IllegalArgumentException("Esta función de error ya está definida.");
                }
            }
    
            // Agregar la nueva función de error
            gramatica.getTPredictiva().crearFunError(nuevaFuncionError);
            paso4.funcionError();
    
            // Cerrar solo la pestaña actual
            TabPane tabPane = (TabPane) root.getParent().getParent();
            Tab tab = tabPane.getSelectionModel().getSelectedItem();
            tabPane.getTabs().remove(tab);
        } catch (NumberFormatException e) {
            // Manejar error de formato de número
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Identificador inválido");
            alert.setContentText("Por favor, ingrese un número válido para el identificador.");
            alert.showAndWait();
        } catch (IllegalArgumentException e) {
            // Manejar otros errores de validación
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error de validación");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        } catch (Exception e) {
            // Manejar cualquier otra excepción
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error inesperado");
            alert.setContentText("Ocurrió un error inesperado. Por favor, inténtelo de nuevo.");
            alert.showAndWait();
        }
    }

    private int obtenerNuevoIdentificador() {
        List<FuncionError> funcionesError = gramatica.getTPredictiva().getFuncionesError();
        int maxId = 0;
        for (FuncionError funcion : funcionesError) {
            if (funcion.getIdentificador() > maxId) {
                maxId = funcion.getIdentificador();
            }
        }
        return maxId + 1;
    }

    @FXML
    private void handleCancelar() {
        TabPane tabPane = (TabPane) root.getParent().getParent();
        Tab tab = tabPane.getSelectionModel().getSelectedItem();
        tabPane.getTabs().remove(tab);
    }
}