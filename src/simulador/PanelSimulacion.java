package simulador;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import gramatica.*;
import java.util.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.IOException;
import javafx.stage.Stage;
import utils.SecondaryWindow;
import java.util.ResourceBundle;

public class PanelSimulacion extends VBox {
    @FXML private VBox root;
    @FXML private TextField inputField;
    @FXML private Button buttonSimular;
    @FXML private TextArea outputArea;
    @FXML private TableView<String> pilaTableView;
    @FXML private TableView<String> entradaTableView;
    @FXML private Label estadoLabel;
    
    private Gramatica gramatica;
    private TablaPredictivaPaso5 tablaPredictiva;
    private List<FuncionError> funcionesError;
    private String entrada;
    
    // Componentes de la UI
    private TextArea areaEntrada;
    private TextArea areaPila;
    private TextArea areaSalida;
    private TreeView<String> arbolDerivacion;
    private TextField campoEntrada;  // Campo para ingresar la cadena a analizar
    private Button buttonIniciar;
    private Button buttonSiguiente;
    private Button buttonReiniciar;
    private Label labelEstado;
    
    // Estado de la simulación
    private Stack<String> pila;
    private List<String> entradaActual;
    private int posicionEntrada;
    private boolean simulacionEnCurso;
    

    private ObservableList<String> pilaList;
    private ObservableList<String> entradaList;
    
    private ResourceBundle bundle;
    
    public PanelSimulacion(Gramatica gramatica, ResourceBundle bundle) {
        this.gramatica = gramatica;
        this.bundle = bundle;
        this.tablaPredictiva = (TablaPredictivaPaso5) gramatica.getTPredictiva();
        this.funcionesError = tablaPredictiva.getFuncionesError();
        this.pilaList = FXCollections.observableArrayList();
        this.entradaList = FXCollections.observableArrayList();
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/PanelSimulacion.fxml"));
            loader.setController(this);
            root = loader.load();
            
            // Agregar el root al VBox
            getChildren().add(root);
            
            initialize();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void initialize() {
        // Configurar el layout principal
        setPadding(new Insets(20));
        setSpacing(10);

        // Área de entrada
        Label labelEntrada = new Label("Entrada:");
        labelEntrada.setFont(Font.font("System", FontWeight.BOLD, 14));
        areaEntrada = new TextArea();
        areaEntrada.setEditable(false);
        areaEntrada.setPrefRowCount(3);

        // Área de pila
        Label labelPila = new Label("Pila:");
        labelPila.setFont(Font.font("System", FontWeight.BOLD, 14));
        areaPila = new TextArea();
        areaPila.setEditable(false);
        areaPila.setPrefRowCount(3);

        // Área de salida
        Label labelSalida = new Label("Salida:");
        labelSalida.setFont(Font.font("System", FontWeight.BOLD, 14));
        areaSalida = new TextArea();
        areaSalida.setEditable(false);
        areaSalida.setPrefRowCount(5);

        // Campo de entrada y botones
        HBox controlBox = new HBox(10);
        campoEntrada = new TextField();
        buttonIniciar = new Button("Iniciar");
        buttonSiguiente = new Button("Siguiente");
        buttonReiniciar = new Button("Reiniciar");
        labelEstado = new Label("");
        
        buttonIniciar.setOnAction(e -> iniciarSimulacion());
        buttonSiguiente.setOnAction(e -> siguientePaso());
        buttonReiniciar.setOnAction(e -> reiniciarSimulacion());
        
        buttonSiguiente.setDisable(true);
        buttonReiniciar.setDisable(true);
        
        controlBox.getChildren().addAll(new Label("Cadena:"), campoEntrada, buttonIniciar, 
                                      buttonSiguiente, buttonReiniciar, labelEstado);

        // Árbol de derivación
        Label labelArbol = new Label("Árbol de Derivación:");
        labelArbol.setFont(Font.font("System", FontWeight.BOLD, 14));
        arbolDerivacion = new TreeView<>();
        arbolDerivacion.setRoot(new TreeItem<>(gramatica.getSimbInicial()));
        VBox.setVgrow(arbolDerivacion, Priority.ALWAYS);

        // Configurar la tabla de la pila
        TableColumn<String, String> pilaColumn = new TableColumn<>("Pila");
        pilaColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()));
        pilaTableView.getColumns().add(pilaColumn);
        pilaTableView.setItems(pilaList);
        
        // Configurar la tabla de entrada
        TableColumn<String, String> entradaColumn = new TableColumn<>("Entrada");
        entradaColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()));
        entradaTableView.getColumns().add(entradaColumn);
        entradaTableView.setItems(entradaList);

        // Agregar todos los componentes al layout principal
        root.getChildren().addAll(controlBox, labelEntrada, areaEntrada, labelPila, areaPila, 
                           labelSalida, areaSalida, labelArbol, arbolDerivacion, pilaTableView, entradaTableView);
    }
    
    private void iniciarSimulacion() {
        entrada = campoEntrada.getText().trim();
        if (entrada.isEmpty()) {
            mostrarError("Error", "La cadena de entrada está vacía");
            return;
        }

        // Inicializar estado de la simulación
        pila = new Stack<>();
        pila.push("$");
        pila.push(gramatica.getSimbInicial());
        
        entradaActual = new ArrayList<>(Arrays.asList(entrada.split("")));
        entradaActual.add("$");
        posicionEntrada = 0;
        
        simulacionEnCurso = true;

        // Actualizar UI
        actualizarAreas();
        buttonIniciar.setDisable(true);
        buttonSiguiente.setDisable(false);
        buttonReiniciar.setDisable(false);
        campoEntrada.setEditable(false);
    }
    
    private void siguientePaso() {
        if (!simulacionEnCurso) return;

        String simboloPila = pila.peek();
        String simboloEntrada = entradaActual.get(posicionEntrada);

        // Si ambos son $, la cadena es aceptada
        if (simboloPila.equals("$") && simboloEntrada.equals("$")) {
            finalizarSimulacion("Cadena aceptada");
            return;
        }

        // Si el símbolo de la pila es igual al de entrada, consumir ambos
        if (simboloPila.equals(simboloEntrada)) {
            pila.pop();
            posicionEntrada++;
            actualizarAreas();
            return;
        }

        // Buscar producción en la tabla predictiva
        String produccion = buscarProduccion(simboloPila, simboloEntrada);
        if (produccion == null || produccion.isEmpty()) {
            // Buscar función de error aplicable
            FuncionError funcionError = buscarFuncionError(simboloPila, simboloEntrada);
            if (funcionError != null) {
                aplicarFuncionError(funcionError);
            } else {
                finalizarSimulacion("Error: No hay producción ni función de error aplicable");
                return;
            }
        } else {
            // Aplicar la producción
            aplicarProduccion(produccion);
        }

        actualizarAreas();
    }
    
    private String buscarProduccion(String noTerminal, String terminal) {
        NoTerminal nt = new NoTerminal(noTerminal, noTerminal);
        Terminal t = new Terminal(terminal, terminal);
        List<String> producciones = gramatica.getProduccionesPorNoTerminalYTerminal(nt, t);
        return producciones.isEmpty() ? null : producciones.get(0);
    }
    
    private FuncionError buscarFuncionError(String simboloPila, String simboloEntrada) {
        for (FuncionError funcion : funcionesError) {
            if (funcion.getAccion() == FuncionError.TERMINAR_ANALISIS) {
                return funcion;
            }
            Terminal simboloFuncion = funcion.getSimbolo();
            if (simboloFuncion != null && simboloFuncion.getNombre().equals(simboloEntrada)) {
                return funcion;
            }
        }
        return null;
    }
    
    private void aplicarFuncionError(FuncionError funcion) {
        String mensaje = funcion.getMensaje();
        if (mensaje == null || mensaje.isEmpty()) {
            mensaje = "Aplicando función de error E" + funcion.getIdentificador();
        }
        
        switch (funcion.getAccion()) {
            case FuncionError.INSERTAR_ENTRADA:
                if (funcion.getSimbolo() != null) {
                    entradaActual.add(posicionEntrada, funcion.getSimbolo().getNombre());
                }
                break;
            case FuncionError.BORRAR_ENTRADA:
                if (posicionEntrada < entradaActual.size()) {
                    entradaActual.remove(posicionEntrada);
                }
                break;
            case FuncionError.TERMINAR_ANALISIS:
                finalizarSimulacion(mensaje);
                return;
        }
        
        areaSalida.appendText(mensaje + "\n");
    }
    
    private void aplicarProduccion(String produccion) {
        pila.pop(); // Remover el no terminal actual
        
        // Separar la producción en símbolos
        String[] simbolos = produccion.split(" ");
        
        // Apilar los símbolos en orden inverso
        for (int i = simbolos.length - 1; i >= 0; i--) {
            String simbolo = simbolos[i];
            if (!simbolo.equals("ε")) { // No apilar épsilon
                pila.push(simbolo);
            }
        }
        
        actualizarArbolDerivacion(produccion);
    }
    
    private void actualizarArbolDerivacion(String produccion) {
        // Implementar la actualización del árbol de derivación
        // Este es un placeholder para la implementación real
    }
    
    private void actualizarAreas() {
        // Actualizar área de entrada
        StringBuilder entradaStr = new StringBuilder();
        for (int i = 0; i < entradaActual.size(); i++) {
            if (i == posicionEntrada) {
                entradaStr.append("►");
            }
            entradaStr.append(entradaActual.get(i));
        }
        areaEntrada.setText(entradaStr.toString());

        // Actualizar área de pila
        areaPila.setText(String.join(" ", pila));
    }
    
    private void reiniciarSimulacion() {
        simulacionEnCurso = false;
        buttonIniciar.setDisable(false);
        buttonSiguiente.setDisable(true);
        buttonReiniciar.setDisable(true);
        campoEntrada.setEditable(true);
        
        areaEntrada.clear();
        areaPila.clear();
        areaSalida.clear();
        arbolDerivacion.setRoot(new TreeItem<>(gramatica.getSimbInicial()));
        
        labelEstado.setText("");
    }
    
    private void finalizarSimulacion(String mensaje) {
        simulacionEnCurso = false;
        buttonSiguiente.setDisable(true);
        labelEstado.setText(mensaje);
        areaSalida.appendText(mensaje + "\n");
    }
    
    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
    
    @FXML
    private void handleEditar() {
        try {
            // Cargar el FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/EditorCadenaEntrada.fxml"));
            
            // Crear el controlador explícitamente
            EditorCadenaEntradaController controller = new EditorCadenaEntradaController();
            loader.setController(controller);
            
            // Cargar el FXML
            VBox root = loader.load();
            
            // Crear y configurar la ventana secundaria
            SecondaryWindow secondaryWindow = new SecondaryWindow(bundle, "Editor de Cadena de Entrada");
            secondaryWindow.getTabPane().getTabs().add(new Tab("Editor", root));
            // Configurar el tamaño de la ventana
            secondaryWindow.getStage().setWidth(600);
            secondaryWindow.getStage().setHeight(400);

            // Centrar la ventana en la pantalla
            Stage parentStage = (Stage) getScene().getWindow();
            double centerX = parentStage.getX() + (parentStage.getWidth() - 600) / 2;
            double centerY = parentStage.getY() + (parentStage.getHeight() - 400) / 2;
            secondaryWindow.getStage().setX(centerX);
            secondaryWindow.getStage().setY(centerY);
            
            // Configurar el controlador
            controller.setStage(secondaryWindow.getStage());
            controller.setTerminales(gramatica.getTerminales());
            controller.setCadenaInicial(inputField.getText());
            
            // Mostrar la ventana y esperar
            secondaryWindow.getStage().showAndWait();
            
            // Actualizar el campo de entrada si se aceptó
            String resultado = controller.getResultado();
            if (resultado != null) {
                inputField.setText(resultado);
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public VBox getRoot() {
        return this;
    }
} 