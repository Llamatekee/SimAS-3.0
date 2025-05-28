package simulador;

import gramatica.Gramatica;
import gramatica.TablaPredictivaPaso5;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.FlowPane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import java.io.IOException;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.Stack;
import java.util.Arrays;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import java.util.ArrayList;

public class SimulacionFinal extends BorderPane {
    @FXML private TextField campoEntrada;
    @FXML private Button btnIniciar;
    @FXML private Button btnPaso;
    @FXML private Button btnFinal;
    @FXML private Button btnRetroceso;
    @FXML private Button btnInicio;
    @FXML private TextArea areaPila;
    @FXML private TextArea areaEntrada;
    @FXML private TextArea areaMensajes;
    @FXML private Button btnEditarCadena;
    @FXML private TableView<HistorialPaso> tablaHistorial;
    @FXML private TableColumn<HistorialPaso, String> colPaso;
    @FXML private TableColumn<HistorialPaso, String> colPila;
    @FXML private TableColumn<HistorialPaso, String> colEntrada;
    @FXML private TableColumn<HistorialPaso, String> colAccion;

    private Gramatica gramatica;
    private TablaPredictivaPaso5 tablaPredictiva;

    // Estado de la simulación
    private Stack<String> pilaSimulacion;
    private List<String> entradaSimulacion;
    private int pasoActual;
    private boolean simulacionEnCurso = false;
    private ObservableList<HistorialPaso> historialObservable = FXCollections.observableArrayList();
    // Lista para almacenar los estados anteriores
    private List<EstadoSimulacion> estadosAnteriores = new ArrayList<>();

    // Clase para almacenar el estado de la simulación
    private static class EstadoSimulacion {
        Stack<String> pila;
        List<String> entrada;
        String accion;
        
        public EstadoSimulacion(Stack<String> pila, List<String> entrada, String accion) {
            this.pila = new Stack<>();
            this.pila.addAll(pila);
            this.entrada = new ArrayList<>(entrada);
            this.accion = accion;
        }
    }

    public SimulacionFinal(Gramatica gramatica, TablaPredictivaPaso5 tablaPredictiva) {
        this.gramatica = gramatica;
        this.tablaPredictiva = tablaPredictiva;
        cargarFXML();
    }

    private void cargarFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/SimulacionFinal.fxml"));
            loader.setController(this);
            Parent root = loader.load();
            this.setCenter(root);
            initialize(); 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void initialize() {
        btnEditarCadena.setOnAction(e -> mostrarDialogoEditarCadena());
        btnIniciar.setOnAction(e -> iniciarSimulacionFinal());
        btnPaso.setOnAction(e -> avanzarPaso());
        btnFinal.setOnAction(e -> avanzarAlFinal());
        btnInicio.setOnAction(e -> retrocederAlInicio());
        btnRetroceso.setOnAction(e -> retrocederPaso());
        
        // Inicializar áreas de texto
        areaMensajes.setText("");
        areaPila.setText("");
        areaEntrada.setText("");
        
        // Inicializar tabla de historial
        colPaso.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPaso()));
        colPila.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPila()));
        colEntrada.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEntrada()));
        colAccion.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAccion()));
        tablaHistorial.setItems(historialObservable);
        tablaHistorial.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Deshabilitar botones inicialmente
        btnPaso.setDisable(true);
        btnFinal.setDisable(true);
        btnRetroceso.setDisable(true);
        btnInicio.setDisable(true);
    }

    private void mostrarDialogoEditarCadena() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Editar cadena de entrada");

        FlowPane terminalPane = new FlowPane();
        terminalPane.setHgap(10);
        terminalPane.setVgap(10);
        terminalPane.setPadding(new Insets(20));
        terminalPane.setAlignment(Pos.CENTER);

        StringBuilder cadenaActual = new StringBuilder(campoEntrada.getText());
        TextField campoCadena = new TextField(cadenaActual.toString());
        campoCadena.setEditable(false);
        campoCadena.setPrefWidth(300);

        // Botones de terminales
        for (var terminal : gramatica.getTerminales()) {
            Button btn = new Button(terminal.getNombre());
            btn.getStyleClass().add("button-grammar");
            btn.setOnAction(ev -> {
                if (cadenaActual.length() > 0) cadenaActual.append(" ");
                cadenaActual.append(terminal.getNombre());
                campoCadena.setText(cadenaActual.toString());
            });
            terminalPane.getChildren().add(btn);
        }

        // Botón para borrar último
        Button btnBorrar = new Button("Borrar último");
        btnBorrar.getStyleClass().add("button-cancel");
        btnBorrar.setOnAction(ev -> {
            String[] partes = campoCadena.getText().trim().split(" ");
            if (partes.length > 0 && !campoCadena.getText().trim().isEmpty()) {
                cadenaActual.setLength(0);
                for (int i = 0; i < partes.length - 1; i++) {
                    if (i > 0) cadenaActual.append(" ");
                    cadenaActual.append(partes[i]);
                }
                campoCadena.setText(cadenaActual.toString());
            }
        });

        // Botón aceptar
        Button btnAceptar = new Button("Aceptar");
        btnAceptar.getStyleClass().add("button-grammar");
        btnAceptar.setOnAction(ev -> {
            campoEntrada.setText(campoCadena.getText());
            dialog.close();
        });

        // Botón cancelar
        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("button-cancel");
        btnCancelar.setOnAction(ev -> dialog.close());

        HBox acciones = new HBox(10, btnBorrar, btnAceptar, btnCancelar);
        acciones.setAlignment(Pos.CENTER);
        acciones.setPadding(new Insets(10, 0, 0, 0));

        VBox layout = new VBox(15,
            new Label("Haz clic en los terminales para construir la cadena de entrada:"),
            terminalPane,
            campoCadena,
            acciones
        );
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));

        Scene scene = new Scene(layout);
        scene.getStylesheets().add(getClass().getResource("/vistas/styles.css").toExternalForm());
        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.showAndWait();
    }

    private void iniciarSimulacionFinal() {
        // Inicializar pila y entrada
        pilaSimulacion = new Stack<>();
        pilaSimulacion.push("$");
        pilaSimulacion.push(gramatica.getSimbInicial());

        String entradaUsuario = campoEntrada.getText().trim();
        if (entradaUsuario.isEmpty()) {
            mostrarAlertaCadenaVacia();
            return;
        }
        entradaSimulacion = new java.util.ArrayList<>(Arrays.asList(entradaUsuario.split(" ")));
        entradaSimulacion.add("$");
        
        areaMensajes.setText("");
        pasoActual = 0;
        simulacionEnCurso = true;
        btnPaso.setDisable(false);
        btnFinal.setDisable(false);
        btnRetroceso.setDisable(false);
        btnInicio.setDisable(false);
        actualizarVista();
        historialObservable.clear();
        estadosAnteriores.clear();
        // Guardar estado inicial
        estadosAnteriores.add(new EstadoSimulacion(pilaSimulacion, entradaSimulacion, "Inicio"));
    }

    private void mostrarAlertaCadenaVacia() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Cadena de entrada vacía");
        alert.setHeaderText(null);
        alert.setContentText("Introduce una cadena de entrada válida antes de iniciar la simulación.");
        alert.showAndWait();
    }

    private void avanzarPaso() {
        if (!simulacionEnCurso) return;
        if (pilaSimulacion.isEmpty() || entradaSimulacion.isEmpty()) return;

        // Guardar estado actual antes de modificarlo
        estadosAnteriores.add(new EstadoSimulacion(pilaSimulacion, entradaSimulacion, areaMensajes.getText()));

        String cimaPila = pilaSimulacion.peek();
        String simboloEntrada = entradaSimulacion.get(0);
        String accionRealizada = "";

        // Caso de aceptación
        if (cimaPila.equals("$") && simboloEntrada.equals("$")) {
            accionRealizada = "Aceptar";
            areaMensajes.setText(accionRealizada);
            simulacionEnCurso = false;
            btnPaso.setDisable(true);
            btnFinal.setDisable(true);
            pasoActual++;
            agregarPasoHistorial(accionRealizada);
            return;
        }

        // Si son iguales y terminales, consumir
        if (cimaPila.equals(simboloEntrada)) {
            pilaSimulacion.pop();
            entradaSimulacion.remove(0);
            accionRealizada = "Emparejar";
            areaMensajes.setText(accionRealizada);
        } else if (esTerminal(cimaPila)) {
            // Error: terminal en pila distinto de entrada
            accionRealizada = "Error";
            areaMensajes.setText(accionRealizada);
            simulacionEnCurso = false;
            btnPaso.setDisable(true);
            btnFinal.setDisable(true);
            pasoActual++;
            agregarPasoHistorial(accionRealizada);
            return;
        } else {
            // Buscar producción o función de error en la tabla predictiva
            String accion = buscarAccionTabla(cimaPila, simboloEntrada);
            if (accion == null || accion.isEmpty()) {
                accionRealizada = "Error";
                areaMensajes.setText(accionRealizada);
                simulacionEnCurso = false;
                btnPaso.setDisable(true);
                btnFinal.setDisable(true);
                pasoActual++;
                agregarPasoHistorial(accionRealizada);
                return;
            }
            if (accion.startsWith("E")) {
                accionRealizada = accion;
                areaMensajes.setText(accionRealizada);
            } else if (accion.equals("ε") || accion.equals("ε_")) {
                pilaSimulacion.pop();
                accionRealizada = cimaPila + " → ε";
                areaMensajes.setText(accionRealizada);
            } else {
                // Es una producción, ejemplo: "3. D → T L;"
                String produccion = accion;
                pilaSimulacion.pop();
                // Extraer la parte derecha de la producción
                String[] partes = produccion.split("→");
                if (partes.length == 2) {
                    String derecha = partes[1].trim();
                    if (!derecha.equals("ε")) {
                        String[] simbolos = derecha.split(" ");
                        // Apilar de derecha a izquierda
                        for (int i = simbolos.length - 1; i >= 0; i--) {
                            if (!simbolos[i].isEmpty()) pilaSimulacion.push(simbolos[i]);
                        }
                    }
                    accionRealizada = produccion.trim();
                    areaMensajes.setText(accionRealizada);
                } else {
                    accionRealizada = "Error";
                    areaMensajes.setText(accionRealizada);
                }
            }
        }
        pasoActual++;
        agregarPasoHistorial(accionRealizada);
        actualizarVista();
    }

    private void avanzarAlFinal() {
        while (simulacionEnCurso) {
            avanzarPaso();
        }
    }

    private void retrocederAlInicio() {
        if (estadosAnteriores.size() <= 1) {
            areaMensajes.setText("Ya estamos en el inicio.");
            return;
        }

        // Mantener solo el estado inicial
        EstadoSimulacion estadoInicial = estadosAnteriores.get(0);
        estadosAnteriores.clear();
        estadosAnteriores.add(estadoInicial);

        // Restaurar el estado inicial
        pilaSimulacion.clear();
        pilaSimulacion.addAll(estadoInicial.pila);
        
        entradaSimulacion.clear();
        entradaSimulacion.addAll(estadoInicial.entrada);
        
        areaMensajes.setText(estadoInicial.accion);
        
        // Actualizar la vista
        actualizarVista();
        
        // Limpiar el historial
        historialObservable.clear();
        pasoActual = 0;
        
        // Deshabilitar el botón de retroceso
        btnRetroceso.setDisable(true);
    }

    private void retrocederPaso() {
        if (estadosAnteriores.size() <= 1) {
            areaMensajes.setText("No hay pasos anteriores para retroceder.");
            return;
        }

        // Eliminar el estado actual
        estadosAnteriores.remove(estadosAnteriores.size() - 1);
        
        // Obtener el estado anterior
        EstadoSimulacion estadoAnterior = estadosAnteriores.get(estadosAnteriores.size() - 1);
        
        // Restaurar el estado
        pilaSimulacion.clear();
        pilaSimulacion.addAll(estadoAnterior.pila);
        
        entradaSimulacion.clear();
        entradaSimulacion.addAll(estadoAnterior.entrada);
        
        areaMensajes.setText(estadoAnterior.accion);
        
        // Actualizar la vista
        actualizarVista();
        
        // Actualizar el historial
        historialObservable.remove(historialObservable.size() - 1);
        pasoActual--;
        
        // Si volvemos al inicio, deshabilitar el botón de retroceso
        if (estadosAnteriores.size() == 1) {
            btnRetroceso.setDisable(true);
        }
    }

    private void actualizarVista() {
        areaPila.setText(String.join(" ", pilaSimulacion));
        areaEntrada.setText(String.join(" ", entradaSimulacion));
    }

    private boolean esTerminal(String simbolo) {
        return gramatica.getTerminales().stream().anyMatch(t -> t.getNombre().equals(simbolo));
    }

    private String buscarAccionTabla(String noTerminal, String terminal) {
        // Buscar la acción en la tabla predictiva extendida
        for (var fila : tablaPredictiva.getTablaPredictiva().getItems()) {
            if (fila.getSimbolo().equals(noTerminal)) {
                return fila.getValor(terminal).get();
            }
        }
        return null;
    }

    private void agregarPasoHistorial(String accion) {
        String pilaStr = String.join(" ", pilaSimulacion);
        String entradaStr = String.join(" ", entradaSimulacion);
        historialObservable.add(new HistorialPaso(String.valueOf(pasoActual), pilaStr, entradaStr, accion));
    }

    // Modelo para la tabla de historial
    public static class HistorialPaso {
        private final String paso;
        private final String pila;
        private final String entrada;
        private final String accion;
        
        public HistorialPaso(String paso, String pila, String entrada, String accion) {
            this.paso = paso;
            this.pila = pila;
            this.entrada = entrada;
            this.accion = accion;
        }
        
        public String getPaso() { return paso; }
        public String getPila() { return pila; }
        public String getEntrada() { return entrada; }
        public String getAccion() { return accion; }
    }
} 