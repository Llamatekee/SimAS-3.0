package simulador;

import gramatica.FuncionError;
import gramatica.Gramatica;
import gramatica.TablaPredictiva;
import gramatica.Terminal;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class PanelNuevaSimDescPaso4 {

    @FXML private Label labelTitulo;
    @FXML private Button buttonCancelar;
    @FXML private Button buttonUltimo;
    @FXML private Button buttonSiguiente;
    @FXML private Button buttonAnterior;
    @FXML private Button buttonPrimero;
    @FXML private ListView<String> listViewFuncionesError;
    @FXML private CheckBox checkBoxNoFuncionesError;
    @FXML private Button buttonNueva;
    @FXML private Button buttonEliminar;
    @FXML private Button buttonFinalizar;
    @FXML private Button buttonVisualizarGramatica;

    private Parent root;
    private PanelSimuladorDesc panelPadre;
    private Gramatica gramatica;
    private int funError = 0;

    public PanelNuevaSimDescPaso4(PanelSimuladorDesc panelPadre) {
        this.panelPadre = panelPadre;
        this.gramatica = panelPadre.gramatica;
        cargarFXML();
        initialize();
    }

    private void cargarFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/PanelNuevaSimDescPaso4.fxml"));
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
        if (this.gramatica.getTPredictiva().getFuncionesError().isEmpty()) {
            ObservableList<String> simbolosTerminales = this.gramatica.getTerminalesModel();
            TablaPredictiva tPredictiva = this.gramatica.getTPredictiva();
            FuncionError funErrorInicial = new FuncionError(0, 7, "");
            tPredictiva.crearFunError(funErrorInicial);
            FuncionError funErrorInicial2 = new FuncionError(1, 2, "");
            tPredictiva.crearFunError(funErrorInicial2);
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

        if (!this.gramatica.getTPredictiva().getFuncionesError().isEmpty()) {
            funcionError();
            this.funError = 1;
        }
        if (funError == 0) {
            this.buttonFinalizar.setVisible(true);
            this.buttonUltimo.setVisible(false);
            this.buttonSiguiente.setDisable(true);
            this.buttonEliminar.setDisable(true);
        }
    }

    public void funcionError() {
        ObservableList<String> lista = FXCollections.observableArrayList();
        List<FuncionError> funError = this.gramatica.getTPredictiva().getFuncionesError();
        StringBuilder string;
        int i = 0;
    
        while (i < funError.size()) {
            string = new StringBuilder();
            int accion;
            string.append(funError.get(i).getIdentificador()).append(" - ");
            accion = funError.get(i).getAccion();
            switch (accion) {
                case 1 -> string.append("Insertar un Símbolo en la Entrada: ");
                case 2 -> string.append("Borrar un Símbolo de la Entrada");
                case 3 -> string.append("Modificar un Símbolo de la Entrada: ");
                case 4 -> string.append("Insertar un Símbolo de la Pila: ");
                case 5 -> string.append("Borrar un Símbolo de la Pila");
                case 6 -> string.append("Modificar un Símbolo de la Pila: ");
                case 7 -> string.append("Terminar el análisis");
            }
            if (accion == 1 || accion == 3 || accion == 4 || accion == 6) {
                string.append(funError.get(i).getSimbolo().getNombre());
            }
            lista.add(string.toString());
            i++;
        }
        this.listViewFuncionesError.setItems(lista);
    
        if (!this.listViewFuncionesError.getItems().isEmpty()) {
            this.buttonFinalizar.setVisible(false);
            this.buttonSiguiente.setDisable(false);
            this.buttonNueva.setDisable(false);
            this.buttonEliminar.setDisable(false);
        } else {
            this.buttonNueva.setDisable(false);
            this.buttonEliminar.setDisable(true);
            this.buttonFinalizar.setVisible(true);
            this.buttonSiguiente.setDisable(true);
        }
    }

    @FXML
    private void handleCancelar() {
        panelPadre.cancelarSimulacion();
    }

    @FXML
    private void handleUltimo() {
        panelPadre.cambiarPaso(5);
    }

    @FXML
    private void handleSiguiente() {
        panelPadre.cambiarPaso(5);
    }

    @FXML
    private void handleAnterior() {
        panelPadre.cambiarPaso(3);
    }

    @FXML
    private void handlePrimero() {
        panelPadre.cambiarPaso(1);
    }

    @FXML
    private void handleNoFuncionesError() {
        if (this.checkBoxNoFuncionesError.isSelected()) {
            this.buttonNueva.setDisable(true);
            this.buttonEliminar.setDisable(true);
            this.buttonFinalizar.setVisible(true);
            this.buttonUltimo.setVisible(false);
            this.buttonSiguiente.setDisable(true);
        } else {
            if (!this.listViewFuncionesError.getItems().isEmpty()) {
                this.buttonFinalizar.setVisible(false);
                this.buttonUltimo.setVisible(true);
                this.buttonSiguiente.setDisable(false);
                this.buttonNueva.setDisable(false);
                this.buttonEliminar.setDisable(false);
            } else {
                this.buttonNueva.setDisable(false);
                this.buttonEliminar.setDisable(true);
                this.buttonFinalizar.setVisible(true);
                this.buttonUltimo.setVisible(false);
                this.buttonSiguiente.setDisable(true);
            }
        }
    }

    @FXML
    private void handleNueva() {
        Tab nuevaFunc = new Tab("Nueva Función de Error");
        NuevaFuncionError nuevaFuncionError = new NuevaFuncionError(this.gramatica, this);
        nuevaFunc.setContent(nuevaFuncionError.getRoot());
        panelPadre.tabPane.getTabs().add(nuevaFunc);
        panelPadre.tabPane.getSelectionModel().select(nuevaFunc);
    }

    @FXML
    private void handleEliminar() {
        int seleccion = this.listViewFuncionesError.getSelectionModel().getSelectedIndex();
        if (seleccion != -1) {
            String funcion = this.listViewFuncionesError.getItems().get(seleccion);
            List<FuncionError> funError = this.gramatica.getTPredictiva().getFuncionesError();
            String id = funcion.substring(0, funcion.indexOf(" - "));
            int num = Integer.parseInt(id);
            int i = 0;
    
            while (i < funError.size()) {
                if (funError.get(i).getIdentificador() == num) {
                    funError.remove(i);
                } else {
                    i++;
                }
            }
    
            // Actualizar identificadores
            for (int j = 0; j < funError.size(); j++) {
                funError.get(j).setIdentificador(j + 1);
            }
    
            // Actualizar la lista de funciones de error en la vista
            funcionError();
    
            // Actualizar el estado del ComboBox
            new NuevaFuncionError(this.gramatica, this);
            //nuevaFuncionError.initialize();
        }
    }

    @FXML
    private void handleFinalizar() {
        panelPadre.cancelarSimulacion();
    }

    @FXML
    private void handleVisualizarGramatica() {
        panelPadre.mostrarGramaticaOriginal();
    }
}