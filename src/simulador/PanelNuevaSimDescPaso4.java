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
import editor.TabManager;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class PanelNuevaSimDescPaso4 implements PanelNuevaSimDescPaso, ActualizableTextos {

    @FXML private Label labelTitulo;
    @FXML private Label labelSubtitulo;
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
    private java.util.ResourceBundle bundle;

    public PanelNuevaSimDescPaso4(PanelSimuladorDesc panelPadre) {
        this.panelPadre = panelPadre;
        this.gramatica = panelPadre.gramatica;
        this.bundle = panelPadre.getBundle();
        cargarFXML();
        initialize();
    }

    private void cargarFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/PanelNuevaSimDescPaso4.fxml"));
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

    @FXML
    private void initialize() {
        // Configurar la lista
        if (listViewFuncionesError != null) {
            listViewFuncionesError.setPlaceholder(new Label("No hay funciones de error definidas"));
        }
        
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
            string.append(bundle.getString(funError.get(i).getNombreAccion()));
            if (accion == 1 || accion == 3 || accion == 4 || accion == 6) {
                string.append(": ").append(funError.get(i).getSimbolo().getNombre());
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
            
            // Cerrar la pestaña de funciones de error si está abierta
            cerrarPestañaFuncionesError();
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
    
    /**
     * 🔹 Cierra la pestaña de funciones de error si está abierta.
     */
    private void cerrarPestañaFuncionesError() {
        panelPadre.cerrarPestañaFuncionesError();
    }

    @FXML
    private void handleNueva() {
        // Asegurarse de que las funciones predefinidas estén presentes
        inicializarFuncionesError();
        
        // Usar TabManager para obtener o crear la pestaña como hija del simulador
        String childId = "funciones_error_" + panelPadre.getSimuladorId();
        Tab tab = TabManager.getOrCreateTab(panelPadre.tabPane, NuevaFuncionError.class, 
            bundle.getString("simulador.paso4.btn.nueva.funcion"), null, panelPadre.getSimuladorId(), childId);
            
        // Si la pestaña es nueva, configurar su contenido
        if (tab.getContent() == null) {
            NuevaFuncionError nuevaFuncionError = new NuevaFuncionError(this.gramatica, this, bundle);
            tab.setContent(nuevaFuncionError.getRoot());
        }
        
        // Seleccionar la pestaña
        panelPadre.tabPane.getSelectionModel().select(tab);
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

    @Override
    public void actualizarTextos(java.util.ResourceBundle bundle) {
        if (labelTitulo != null) labelTitulo.setText(bundle.getString("simulador.paso4.titulo"));
        if (labelSubtitulo != null) labelSubtitulo.setText(bundle.getString("simulador.paso4.subtitulo"));
        if (checkBoxNoFuncionesError != null) checkBoxNoFuncionesError.setText(bundle.getString("simulador.paso4.checkbox"));
        if (buttonNueva != null) buttonNueva.setText(bundle.getString("simulador.paso4.btn.nueva.corta"));
        if (buttonEliminar != null) buttonEliminar.setText(bundle.getString("simulador.paso4.btn.eliminar.corta"));
        if (buttonFinalizar != null) buttonFinalizar.setText(bundle.getString("button.finalizar"));
        if (buttonCancelar != null) buttonCancelar.setText(bundle.getString("button.cancelar"));
        if (buttonVisualizarGramatica != null) buttonVisualizarGramatica.setText(bundle.getString("simulador.paso1.btn.gramatica"));
        if (buttonPrimero != null) buttonPrimero.setText(bundle.getString("button.primero"));
        if (buttonAnterior != null) buttonAnterior.setText(bundle.getString("button.anterior"));
        if (buttonSiguiente != null) buttonSiguiente.setText(bundle.getString("button.siguiente"));
        if (buttonUltimo != null) buttonUltimo.setText(bundle.getString("button.ultimo"));
        this.bundle = bundle;

        funcionError();
    }
}