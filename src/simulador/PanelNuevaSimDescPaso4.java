package simulador;

import gramatica.FilaTablaPredictiva;
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
import java.util.ArrayList;

public class PanelNuevaSimDescPaso4 implements PanelNuevaSimDescPaso {

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

    @FXML
    private void initialize() {
        // Inicializar funciones de error si no existen
        inicializarFuncionesError();
        // Mostrar las funciones de error existentes
        funcionError();
    }

    private void inicializarFuncionesError() {
        // Verificar si las funciones predefinidas existen
        List<FuncionError> funcionesError = this.gramatica.getTPredictiva().getFuncionesError();
        boolean existeFuncion0 = false;
        boolean existeFuncion1 = false;
        
        for (FuncionError funcion : funcionesError) {
            if (funcion.getIdentificador() == 0) {
                existeFuncion0 = true;
            }
            if (funcion.getIdentificador() == 1) {
                existeFuncion1 = true;
            }
        }
        
        // Si falta alguna función predefinida, añadirla
        if (!existeFuncion0) {
            FuncionError funErrorInicial = new FuncionError(0, 7, "");
            this.gramatica.getTPredictiva().crearFunError(funErrorInicial);
        }
        
        if (!existeFuncion1) {
            FuncionError funErrorInicial2 = new FuncionError(1, 2, "");
            this.gramatica.getTPredictiva().crearFunError(funErrorInicial2);
        }
        
        // Si no hay funciones de error para los terminales, añadirlas
        if (funcionesError.size() <= 2) {
            ObservableList<String> simbolosTerminales = this.gramatica.getTerminalesModel();
            int x = 2;
            for (int w = 0; w < simbolosTerminales.size(); w++) {
                boolean existeFuncionParaTerminal = false;
                for (FuncionError funcion : funcionesError) {
                    if (funcion.getAccion() == 1 && funcion.getSimbolo() != null && 
                        funcion.getSimbolo().getNombre().equals(simbolosTerminales.get(w).toString())) {
                        existeFuncionParaTerminal = true;
                        break;
                    }
                }
                
                if (!existeFuncionParaTerminal) {
                    FuncionError funError = new FuncionError(x, 1, "");
                    Terminal term = new Terminal(simbolosTerminales.get(w).toString(), simbolosTerminales.get(w).toString());
                    funError.setSimbolo(term);
                    this.gramatica.getTPredictiva().crearFunError(funError);
                    x++;
                }
            }
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
        panelPadre.cambiarPaso(4);
    }

    @FXML
    private void handleSiguiente() {
        panelPadre.cambiarPaso(4);
    }

    @FXML
    private void handleAnterior() {
        panelPadre.cambiarPaso(2);
    }

    @FXML
    private void handlePrimero() {
        panelPadre.cambiarPaso(0);
    }

    @FXML
    private void handleNoFuncionesError() {
        boolean noUsarFuncionesError = this.checkBoxNoFuncionesError.isSelected();
        
        if (noUsarFuncionesError) {
            this.buttonNueva.setDisable(true);
            this.buttonEliminar.setDisable(true);
            this.buttonFinalizar.setVisible(true);
            this.buttonUltimo.setDisable(true);
            this.buttonSiguiente.setDisable(true);
        } else {
            if (!this.listViewFuncionesError.getItems().isEmpty()) {
                this.buttonFinalizar.setVisible(false);
                this.buttonUltimo.setDisable(false);
                this.buttonSiguiente.setDisable(false);
                this.buttonNueva.setDisable(false);
                this.buttonEliminar.setDisable(false);
            } else {
                this.buttonNueva.setDisable(false);
                this.buttonEliminar.setDisable(true);
                this.buttonFinalizar.setVisible(true);
                this.buttonUltimo.setDisable(true);
                this.buttonSiguiente.setDisable(true);
            }
        }
    }

    @FXML
    private void handleNueva() {
        // Asegurarse de que las funciones predefinidas estén presentes
        inicializarFuncionesError();
        
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
            
            // No permitir eliminar las funciones predefinidas (0 y 1)
            if (num == 0 || num == 1) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("No se puede eliminar");
                alert.setContentText("No se pueden eliminar las funciones predefinidas (0 y 1).");
                alert.showAndWait();
                return;
            }
            
            // Eliminar la función seleccionada
            int i = 0;
            while (i < funError.size()) {
                if (funError.get(i).getIdentificador() == num) {
                    funError.remove(i);
                    break;
                }
                i++;
            }
            
            // Reordenar los índices de las funciones restantes (excepto las predefinidas)
            reordenarIndicesFuncionesError();
            
            // Actualizar la lista de funciones de error
            funcionError();
        }
    }
    
    public void reordenarIndicesFuncionesError() {
        List<FuncionError> funcionesError = this.gramatica.getTPredictiva().getFuncionesError();
        
        // Ordenar las funciones por identificador (excepto las predefinidas)
        List<FuncionError> funcionesOrdenadas = new ArrayList<>();
        List<FuncionError> funcionesPredefinidas = new ArrayList<>();
        
        // Separar las funciones predefinidas y las demás
        for (FuncionError funcion : funcionesError) {
            if (funcion.getIdentificador() == 0 || funcion.getIdentificador() == 1) {
                funcionesPredefinidas.add(funcion);
            } else {
                funcionesOrdenadas.add(funcion);
            }
        }
        
        // Ordenar las funciones no predefinidas por identificador
        funcionesOrdenadas.sort((f1, f2) -> Integer.compare(f1.getIdentificador(), f2.getIdentificador()));
        
        // Reasignar índices a las funciones no predefinidas, empezando desde 2
        int nuevoIndice = 2;
        for (FuncionError funcion : funcionesOrdenadas) {
            funcion.setIdentificador(nuevoIndice);
            nuevoIndice++;
        }
        
        // Reconstruir la lista de funciones de error
        funcionesError.clear();
        funcionesError.addAll(funcionesPredefinidas);
        funcionesError.addAll(funcionesOrdenadas);
    }

    @FXML
    private void handleFinalizar() {
        panelPadre.cambiarPaso(5);
    }

    @FXML
    private void handleVisualizarGramatica() {
        panelPadre.mostrarGramaticaOriginal();
    }
}