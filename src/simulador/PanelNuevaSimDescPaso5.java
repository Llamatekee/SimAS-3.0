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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import gramatica.FilaTablaPredictiva;
import gramatica.FuncionError;
import gramatica.Gramatica;
import gramatica.TablaPredictivaPaso5;

public class PanelNuevaSimDescPaso5 {

    @FXML private Label labelTitulo;
    @FXML private Button buttonAnterior;
    @FXML private Button buttonSiguiente;
    @FXML private Button buttonCancelar;
    @FXML private Button buttonEliminar;
    @FXML private Button buttonRellenar;
    @FXML private Button buttonGramatica;
    @FXML private ComboBox<String> comboBoxFuncionesError;
    @FXML private TableView<FilaTablaPredictiva> tablaPredictiva;
    @FXML private TableColumn<String[], String> columnSimbolo;
    @FXML private TableColumn<String[], String> columnAccion;

    private Parent root;
    private PanelSimuladorDesc panelPadre;
    private Gramatica gramatica;

    public PanelNuevaSimDescPaso5(PanelSimuladorDesc panelPadre) {
        this.panelPadre = panelPadre;
        this.gramatica = panelPadre.gramatica;
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
        // Inicializar comboBoxFuncionesError con valores
        List<String> funcionesError = new ArrayList<>();
        for (FuncionError funcionError : gramatica.getTPredictiva().getFuncionesError()) {
            funcionesError.add(funcionError.toString());
        }
        comboBoxFuncionesError.getItems().addAll(funcionesError);

        // Mostrar tabla predictiva
        construirTablaPredictiva();
    }

    private void construirTablaPredictiva() {
        if (gramatica.getProducciones().get(0).getNumero() == 0) {
            gramatica.numerarProducciones();
        }
        
        // Usar la versión específica para el paso 5
        TablaPredictivaPaso5 tpredictiva = new TablaPredictivaPaso5(tablaPredictiva);
        tpredictiva.setPanelPaso5(this);
        tpredictiva.construir(gramatica);
        gramatica.setTPredictiva(tpredictiva); // Guardar la instancia en la gramática
        
        // Configurar la tabla
        tablaPredictiva.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tablaPredictiva.setTableMenuButtonVisible(false);
        tablaPredictiva.refresh();
    }

    public String getFuncionErrorSeleccionada() {
        return comboBoxFuncionesError.getSelectionModel().getSelectedItem();
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
        // Obtener la celda seleccionada
        TableColumn<FilaTablaPredictiva, ?> column = tablaPredictiva.getFocusModel().getFocusedCell().getTableColumn();
        if (column != null && !column.getText().equals("No Terminal")) {
            FilaTablaPredictiva fila = tablaPredictiva.getSelectionModel().getSelectedItem();
            if (fila != null) {
                String valorCelda = fila.getValor(column.getText()).get();
                
                // Verificar si la celda tiene una función de error o una producción épsilon añadida
                if (valorCelda != null && (valorCelda.startsWith("E") || valorCelda.startsWith("ε_"))) {
                    // Eliminar la función de error o la producción épsilon (dejar la celda vacía)
                    fila.setValor(column.getText(), "");
                    tablaPredictiva.refresh();
                } else {
                    // Mostrar alerta si la celda no tiene una función de error o una producción épsilon añadida
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Celda no válida");
                    alert.setContentText("Esta celda no contiene una función de error o una producción épsilon añadida para eliminar.");
                    alert.showAndWait();
                }
            }
        }
    }

    @FXML
    private void handleRellenar() {
        // Usar la instancia existente de TablaPredictivaPaso5
        TablaPredictivaPaso5 tpredictiva = (TablaPredictivaPaso5) gramatica.getTPredictiva();
        if (tpredictiva != null) {
            tpredictiva.rellenarProduccionesEpsilon();
        } else {
            // Si no hay tabla, crear una nueva
            construirTablaPredictiva();
            tpredictiva = (TablaPredictivaPaso5) gramatica.getTPredictiva();
            tpredictiva.rellenarProduccionesEpsilon();
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