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
import javafx.scene.control.TablePosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import gramatica.FilaTablaPredictiva;
import gramatica.FuncionError;
import gramatica.Gramatica;
import gramatica.TablaPredictivaPaso5;

public class PanelNuevaSimDescPaso5 implements PanelNuevaSimDescPaso {

    @FXML private Label labelTitulo;
    @FXML private Button buttonAnterior;
    @FXML private Button buttonSiguiente;
    @FXML private Button buttonCancelar;
    @FXML private Button buttonEliminar;
    @FXML private Button buttonRellenar;
    @FXML private Button buttonGramatica;
    @FXML private Button buttonPrimero;
    @FXML private Button buttonUltimo;
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

    @FXML
    private void initialize() {
        // Deshabilitar los botones Siguiente y Último en el paso 5
        buttonSiguiente.setDisable(true);
        buttonUltimo.setDisable(true);

        // Verificar que tenemos una tabla predictiva válida
        if (gramatica.getTPredictiva() == null) {
            System.out.println("Error: No hay tabla predictiva inicializada");
            return;
        }

        // Verificar las funciones de error antes de construir
        List<FuncionError> funcionesError = gramatica.getTPredictiva().getFuncionesError();
        if (funcionesError == null || funcionesError.isEmpty()) {
            System.out.println("Error: No hay funciones de error disponibles antes de construir");
            return;
        }
        System.out.println("Funciones de error disponibles antes de construir: " + funcionesError.size());

        // Construir la tabla predictiva manteniendo las funciones de error existentes
        construirTablaPredictiva();

        // Verificar las funciones de error después de construir
        funcionesError = gramatica.getTPredictiva().getFuncionesError();
        if (funcionesError == null || funcionesError.isEmpty()) {
            System.out.println("Error: Se perdieron las funciones de error después de construir");
            return;
        }
        System.out.println("Funciones de error disponibles después de construir: " + funcionesError.size());

        // Llenar el ComboBox con las funciones de error
        actualizarComboBoxFuncionesError();

        // Configurar el manejador de clics en la tabla
        tablaPredictiva.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                TablePosition<?, ?> pos = tablaPredictiva.getFocusModel().getFocusedCell();
                if (pos != null && pos.getColumn() > 0) { // Ignorar la columna "No Terminal"
                    // Obtener la función de error seleccionada
                    String funcionSeleccionada = comboBoxFuncionesError.getSelectionModel().getSelectedItem();
                    if (funcionSeleccionada != null) {
                        // Extraer el número de la función de error (E0, E1, etc.)
                        String numeroFuncion = funcionSeleccionada.split(" ")[0];
                        
                        // Obtener la fila y columna seleccionadas
                        FilaTablaPredictiva fila = tablaPredictiva.getItems().get(pos.getRow());
                        String columna = tablaPredictiva.getColumns().get(pos.getColumn()).getText();
                        
                        // Actualizar el valor en la tabla
                        if (!fila.getEsTerminal()) {
                            fila.setValor(columna, numeroFuncion);
                            tablaPredictiva.refresh();
                        }
                    }
                }
            }
        });
    }

    private void construirTablaPredictiva() {
        if (gramatica.getProducciones().get(0).getNumero() == 0) {
            gramatica.numerarProducciones();
        }
        
        // Guardar las funciones de error existentes
        List<FuncionError> funcionesErrorExistentes = new ArrayList<>(gramatica.getTPredictiva().getFuncionesError());
        System.out.println("Número de funciones de error existentes: " + funcionesErrorExistentes.size());
        
        // Usar la versión específica para el paso 5
        TablaPredictivaPaso5 tpredictiva = new TablaPredictivaPaso5(tablaPredictiva);
        tpredictiva.setPanelPaso5(this);
        
        // Establecer las funciones de error ANTES de construir
        tpredictiva.setFuncionesError(funcionesErrorExistentes);
        
        // Construir la tabla
        tpredictiva.construir(gramatica);
        
        // Verificar que las funciones de error se mantuvieron
        System.out.println("Número de funciones de error después de construir: " + tpredictiva.getFuncionesError().size());
        
        // Guardar la instancia en la gramática
        gramatica.setTPredictiva(tpredictiva);
        
        // Configurar la tabla
        tablaPredictiva.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tablaPredictiva.setTableMenuButtonVisible(false);
        tablaPredictiva.refresh();
    }

    private void actualizarComboBoxFuncionesError() {
        // Limpiar el ComboBox antes de llenarlo
        comboBoxFuncionesError.getItems().clear();
        
        // Obtener las funciones de error
        List<FuncionError> funcionesError = gramatica.getTPredictiva().getFuncionesError();
        System.out.println("Actualizando ComboBox con " + funcionesError.size() + " funciones de error");
        
        // Llenar el ComboBox con las funciones de error
        for (FuncionError funcion : funcionesError) {
            StringBuilder descripcion = new StringBuilder();
            descripcion.append("E").append(funcion.getIdentificador()).append(" - ");
            switch (funcion.getAccion()) {
                case 1 -> descripcion.append("Insertar un Símbolo en la Entrada: ");
                case 2 -> descripcion.append("Borrar un Símbolo de la Entrada");
                case 3 -> descripcion.append("Modificar un Símbolo de la Entrada: ");
                case 4 -> descripcion.append("Insertar un Símbolo de la Pila: ");
                case 5 -> descripcion.append("Borrar un Símbolo de la Pila");
                case 6 -> descripcion.append("Modificar un Símbolo de la Pila: ");
                case 7 -> descripcion.append("Terminar el análisis");
            }
            if (funcion.getAccion() == 1 || funcion.getAccion() == 3 || funcion.getAccion() == 4 || funcion.getAccion() == 6) {
                if (funcion.getSimbolo() != null) {
                    descripcion.append(funcion.getSimbolo().getNombre());
                }
            }
            comboBoxFuncionesError.getItems().add(descripcion.toString());
        }
        
        // Seleccionar el primer elemento si hay elementos disponibles
        if (!comboBoxFuncionesError.getItems().isEmpty()) {
            comboBoxFuncionesError.getSelectionModel().selectFirst();
        } else {
            System.out.println("No se encontraron funciones de error para mostrar en el ComboBox");
        }
    }

    public String getFuncionErrorSeleccionada() {
        return comboBoxFuncionesError.getSelectionModel().getSelectedItem();
    }

    @FXML
    private void handleAnterior() {
        panelPadre.cambiarPaso(3);
    }

    @FXML
    private void handleSiguiente() { //Finalizar
        // Validar las funciones de error
        if (validarFuncionesError()) {
            panelPadre.cambiarPaso(4); // Cambiar al siguiente paso SIMULADOR
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
        // Obtener la celda seleccionada
        TableColumn<FilaTablaPredictiva, ?> column = tablaPredictiva.getFocusModel().getFocusedCell().getTableColumn();
        if (column != null && !column.getText().equals("No Terminal")) {
            FilaTablaPredictiva fila = tablaPredictiva.getSelectionModel().getSelectedItem();
            if (fila != null) {
                String valorCelda = fila.getValor(column.getText()).get();
                
                // Verificar si la celda está vacía
                if (valorCelda == null || valorCelda.isEmpty()) {
                    // Obtener la función de error seleccionada
                    String funcionErrorSeleccionada = getFuncionErrorSeleccionada();
                    if (funcionErrorSeleccionada != null) {
                        // Añadir la función de error a la celda
                        fila.setValor(column.getText(), funcionErrorSeleccionada);
                        tablaPredictiva.refresh();
                    } else {
                        // Mostrar alerta si no se ha seleccionado una función de error
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("No se ha seleccionado una función de error");
                        alert.setContentText("Por favor, seleccione una función de error antes de rellenar la celda.");
                        alert.showAndWait();
                    }
                } else {
                    // Mostrar alerta si la celda ya tiene un valor
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Celda no vacía");
                    alert.setContentText("Esta celda ya tiene un valor. Por favor, seleccione una celda vacía.");
                    alert.showAndWait();
                }
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

    @FXML
    private void handlePrimero() {
        panelPadre.cambiarPaso(0);
    }

    @FXML
    private void handleUltimo() {
        // Ya estamos en el último paso, no hacer nada
    }
}