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
import editor.ActualizableTextos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class NuevaFuncionError implements ActualizableTextos {

    @FXML private Label labelTitulo;
    @FXML private Label labelConfiguracion;
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
    private ResourceBundle bundle;

    public NuevaFuncionError(Gramatica gramatica, PanelNuevaSimDescPaso4 paso4, ResourceBundle bundle) {
        this.gramatica = gramatica;
        this.paso4 = paso4;
        this.bundle = bundle;
        cargarFXML();
        inicializarCampos();
        actualizarTextos(bundle);
    }

    @FXML
    private void initialize() {
        // Este método se llama automáticamente después de cargar el FXML
        // Configurar el campo de identificador como no editable
        if (textFieldIdentificador != null) {
            textFieldIdentificador.setEditable(false);
        }
    }

    private void cargarFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/NuevaFuncionError.fxml"));
            loader.setController(this);
            loader.setResources(bundle);
            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Parent getRoot() {
        return root;
    }

    private void inicializarCampos() {
        // Acciones internacionalizadas
        ObservableList<String> acciones = FXCollections.observableArrayList(
            bundle.getString("funcion.error.insertar.entrada"),
            bundle.getString("funcion.error.borrar.entrada"),
            bundle.getString("funcion.error.modificar.entrada"),
            bundle.getString("funcion.error.insertar.pila"),
            bundle.getString("funcion.error.borrar.pila"),
            bundle.getString("funcion.error.modificar.pila"),
            bundle.getString("funcion.error.terminar")
        );
        comboBoxAccion.setItems(acciones);
        // Deshabilitar opciones si ya están definidas
        List<FuncionError> funcionesError = gramatica.getTPredictiva().getFuncionesError();
        for (FuncionError funcionError : funcionesError) {
            if (funcionError.getAccion() == 7 || funcionError.getAccion() == 2) {
                comboBoxAccion.getItems().set(funcionError.getAccion() - 1,
                    acciones.get(funcionError.getAccion() - 1) + bundle.getString("funcion.error.definida"));
            }
        }
        // ComboBox de símbolos (terminales)
        List<String> simbolos = new ArrayList<>();
        for (String terminal : gramatica.getTerminalesModel()) {
            simbolos.add(terminal);
        }
        ObservableList<String> observableSimbolos = FXCollections.observableArrayList(simbolos);
        comboBoxSimbolo.setItems(observableSimbolos);
        // Identificador
        textFieldIdentificador.setText(String.valueOf(obtenerNuevoIdentificador()));
    }

    @FXML
    private void handleAceptar() {
        try {
            if (comboBoxAccion.getSelectionModel().isEmpty()) {
                throw new IllegalArgumentException(bundle.getString("nuevaFuncionError.alert.seleccionar.accion"));
            }
            int id = obtenerNuevoIdentificador();
            int accion = comboBoxAccion.getSelectionModel().getSelectedIndex() + 1;
            String mensaje = textFieldMensaje.getText();
            String simbolo = comboBoxSimbolo.getSelectionModel().getSelectedItem();
            if ((accion != 7 && accion != 2 && accion != 5) && (simbolo == null || simbolo.isEmpty())) {
                throw new IllegalArgumentException(bundle.getString("nuevaFuncionError.alert.seleccionar.simbolo"));
            }
            Terminal term = (accion == 7 || accion == 2 || accion == 5) ? null : new Terminal(simbolo, simbolo);
            FuncionError nuevaFuncionError = new FuncionError(id, accion, mensaje);
            nuevaFuncionError.setSimbolo(term);
            List<FuncionError> funcionesError = gramatica.getTPredictiva().getFuncionesError();
            for (FuncionError funcionError : funcionesError) {
                if (funcionError.getAccion() == nuevaFuncionError.getAccion() &&
                    (funcionError.getSimbolo() == null || 
                     (nuevaFuncionError.getSimbolo() != null && 
                      funcionError.getSimbolo().getNombre().equals(nuevaFuncionError.getSimbolo().getNombre())))) {
                    throw new IllegalArgumentException(bundle.getString("nuevaFuncionError.alert.ya.definida"));
                }
            }
            if (id == 0 || id == 1) {
                throw new IllegalArgumentException(bundle.getString("nuevaFuncionError.alert.no.sobrescribir.predefinida"));
            }
            gramatica.getTPredictiva().crearFunError(nuevaFuncionError);
            paso4.reordenarIndicesFuncionesError();
            paso4.funcionError();
            TabPane tabPane = (TabPane) root.getParent().getParent();
            Tab tab = tabPane.getSelectionModel().getSelectedItem();
            tabPane.getTabs().remove(tab);
        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(bundle.getString("nuevaFuncionError.alert.titulo"));
            alert.setHeaderText(bundle.getString("nuevaFuncionError.alert.header.identificador"));
            alert.setContentText(bundle.getString("nuevaFuncionError.alert.mensaje.identificador"));
            alert.showAndWait();
        } catch (IllegalArgumentException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(bundle.getString("nuevaFuncionError.alert.titulo"));
            alert.setHeaderText(bundle.getString("nuevaFuncionError.alert.header.validacion"));
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(bundle.getString("nuevaFuncionError.alert.titulo"));
            alert.setHeaderText(bundle.getString("nuevaFuncionError.alert.header.inesperado"));
            alert.setContentText(bundle.getString("nuevaFuncionError.alert.mensaje.inesperado"));
            alert.showAndWait();
        }
    }

    private int obtenerNuevoIdentificador() {
        List<FuncionError> funcionesError = gramatica.getTPredictiva().getFuncionesError();
        int maxId = 0;
        
        // Encontrar el identificador máximo actual
        for (FuncionError funcion : funcionesError) {
            if (funcion.getIdentificador() > maxId) {
                maxId = funcion.getIdentificador();
            }
        }
        
        // Asegurarse de que no sobrescribimos las funciones predefinidas (0 y 1)
        // Si no hay funciones o el máximo es menor que 1, empezamos desde 2
        if (maxId < 1) {
            return 2;
        }
        
        // Verificar si ya existe una función con el siguiente ID
        boolean idExiste = false;
        for (FuncionError funcion : funcionesError) {
            if (funcion.getIdentificador() == maxId + 1) {
                idExiste = true;
                break;
            }
        }
        
        // Si el ID ya existe, buscar el siguiente ID disponible
        if (idExiste) {
            int id = maxId + 1;
            while (idExiste) {
                id++;
                idExiste = false;
                for (FuncionError funcion : funcionesError) {
                    if (funcion.getIdentificador() == id) {
                        idExiste = true;
                        break;
                    }
                }
            }
            return id;
        }
        
        return maxId + 1;
    }

    @FXML
    private void handleCancelar() {
        TabPane tabPane = (TabPane) root.getParent().getParent();
        Tab tab = tabPane.getSelectionModel().getSelectedItem();
        tabPane.getTabs().remove(tab);
    }

    @Override
    public void actualizarTextos(ResourceBundle bundle) {
        this.bundle = bundle;
        if (labelTitulo != null) labelTitulo.setText(bundle.getString("nuevaFuncionError.titulo"));
        if (labelConfiguracion != null) labelConfiguracion.setText(bundle.getString("nuevaFuncionError.label.configuracion"));
        if (labelIdentificador != null) labelIdentificador.setText(bundle.getString("nuevaFuncionError.label.identificador"));
        if (labelAccion != null) labelAccion.setText(bundle.getString("nuevaFuncionError.label.accion"));
        if (labelSimbolo != null) labelSimbolo.setText(bundle.getString("nuevaFuncionError.label.simbolo"));
        if (labelMensaje != null) labelMensaje.setText(bundle.getString("nuevaFuncionError.label.mensaje"));
        if (buttonAceptar != null) buttonAceptar.setText(bundle.getString("button.aceptar"));
        if (buttonCancelar != null) buttonCancelar.setText(bundle.getString("button.cancelar"));
        inicializarCampos();
    }
}