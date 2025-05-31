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
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Priority;
import java.io.File;
import editor.ActualizableTextos;
import editor.TabManager;
import java.util.ResourceBundle;
import javafx.application.Platform;
import java.util.Collections;
import java.awt.image.BufferedImage;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import java.util.Map;
import java.util.HashMap;

public class SimulacionFinal extends BorderPane implements ActualizableTextos {
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
    @FXML private Button btnDerivacion;
    @FXML private Button btnArbol;
    @FXML private TableView<HistorialPaso> tablaHistorial;
    @FXML private TableColumn<HistorialPaso, String> colPaso;
    @FXML private TableColumn<HistorialPaso, String> colPila;
    @FXML private TableColumn<HistorialPaso, String> colEntrada;
    @FXML private TableColumn<HistorialPaso, String> colAccion;
    // Labels para internacionalización
    @FXML private Label labelEntrada;
    @FXML private Label labelPila;
    @FXML private Label labelEntradaStack;
    @FXML private Label labelAccion;
    @FXML private Label labelHistorial;

    private Gramatica gramatica;
    private TablaPredictivaPaso5 tablaPredictiva;
    private TabPane tabPane;
    private ResourceBundle bundle;

    // Estado de la simulación
    private Stack<String> pilaSimulacion;
    private List<String> entradaSimulacion;
    private int pasoActual;
    private boolean simulacionEnCurso = false;
    private ObservableList<HistorialPaso> historialObservable = FXCollections.observableArrayList();
    // Lista para almacenar los estados anteriores
    private List<EstadoSimulacion> estadosAnteriores = new ArrayList<>();

    private String simulacionId; // Identificador único para esta simulación
    private static int contadorSimulaciones = 0; // Contador global para numerar simulaciones
    private static Map<Integer, List<Integer>> numerosDisponiblesPorGrupo = new HashMap<>(); // Números disponibles por grupo
    private static Map<Integer, Integer> contadoresPorGrupo = new HashMap<>(); // Contadores por grupo
    private int numeroSimulacion; // Número de esta simulación específica
    private String simuladorPadreId; // Nuevo campo para almacenar el ID del simulador padre

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

    public SimulacionFinal(Gramatica gramatica, TablaPredictivaPaso5 tablaPredictiva, TabPane tabPane, ResourceBundle bundle) {
        this.gramatica = gramatica;
        this.tablaPredictiva = tablaPredictiva;
        this.tabPane = tabPane;
        this.bundle = bundle;
        this.simulacionId = "sim_" + System.currentTimeMillis(); // Generar ID único
        
        // Obtener el simulador padre de la pestaña actualmente seleccionada
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null && selectedTab.getUserData() != null && 
            selectedTab.getUserData().toString().startsWith("simulador_")) {
            this.simuladorPadreId = selectedTab.getUserData().toString();
        }
        
        System.out.println("DEBUG: SimulacionFinal constructor - Selected simulador: " + this.simuladorPadreId);
        
        // Contar simulaciones específicamente para este simulador
        int simulacionesEnGrupo = 0;
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getContent() instanceof SimulacionFinal) {
                SimulacionFinal otraSim = (SimulacionFinal) tab.getContent();
                if (otraSim.simuladorPadreId != null && 
                    otraSim.simuladorPadreId.equals(this.simuladorPadreId)) {
                    simulacionesEnGrupo++;
                }
            }
        }
        
        // Asignar número de simulación
        this.numeroSimulacion = simulacionesEnGrupo + 1;
        System.out.println("DEBUG: SimulacionFinal constructor - Número asignado: " + this.numeroSimulacion);
        
        cargarFXML();
        actualizarTitulosPestañas();
        
        // Añadir listener para cerrar pestañas hijas
        if (tabPane != null) {
            tabPane.getTabs().addListener((javafx.collections.ListChangeListener.Change<? extends Tab> change) -> {
                while (change.next()) {
                    if (change.wasRemoved()) {
                        for (Tab tab : change.getRemoved()) {
                            if (tab.getContent() == this) {
                                // Cerrar las pestañas hijas
                                Platform.runLater(() -> {
                                    List<Tab> tabsToRemove = new ArrayList<>();
                                    for (Tab t : tabPane.getTabs()) {
                                        if (t.getUserData() != null) {
                                            String userData = t.getUserData().toString();
                                            if ((userData.startsWith("derivacion_") || userData.startsWith("arbol_")) 
                                                && userData.endsWith(simulacionId)) {
                                                tabsToRemove.add(t);
                                            }
                                        }
                                    }
                                    for (Tab t : tabsToRemove) {
                                        tabPane.getTabs().remove(t);
                                    }
                                    
                                    // Reasignar números después de cerrar
                                    reasignarNumerosSimulaciones(tabPane);
                                });
                            }
                        }
                    }
                }
            });
        }
    }

    /**
     * Actualiza los títulos de todas las pestañas relacionadas con esta simulación.
     */
    public void actualizarTitulosPestañas() {
        if (tabPane == null || simuladorPadreId == null) return;
        
        // Obtener el número de grupo del simulador padre
        int numeroGrupo = TabManager.obtenerNumeroGrupo(tabPane, simuladorPadreId);
        System.out.println("DEBUG: actualizarTitulosPestañas - Grupo del simulador padre " + simuladorPadreId + ": " + numeroGrupo);
        
        // Determinar si hay más de un grupo
        boolean mostrarGrupo = TabManager.contarGruposActivos(tabPane) > 1;
        System.out.println("DEBUG: actualizarTitulosPestañas - Mostrar grupo: " + mostrarGrupo);
        
        // Actualizar títulos
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getContent() == this) {
                String tituloBase = bundle.getString("simulador.paso6.simulacion");
                String nuevoTitulo = mostrarGrupo ? 
                    numeroGrupo + "-" + tituloBase + " " + numeroSimulacion :
                    tituloBase + " " + numeroSimulacion;
                System.out.println("DEBUG: actualizarTitulosPestañas - Nuevo título: " + nuevoTitulo);
                tab.setText(nuevoTitulo);
            } else if (tab.getUserData() != null) {
                String userData = tab.getUserData().toString();
                if (userData.startsWith("derivacion_") && userData.endsWith(simulacionId)) {
                    String tituloBase = bundle.getString("simulacionfinal.tab.derivacion");
                    tab.setText(mostrarGrupo ? 
                        numeroGrupo + "-" + tituloBase + " " + numeroSimulacion :
                        tituloBase + " " + numeroSimulacion);
                } else if (userData.startsWith("arbol_") && userData.endsWith(simulacionId)) {
                    String tituloBase = bundle.getString("simulacionfinal.tab.arbol");
                    tab.setText(mostrarGrupo ? 
                        numeroGrupo + "-" + tituloBase + " " + numeroSimulacion :
                        tituloBase + " " + numeroSimulacion);
                }
            }
        }
    }

    public static void reasignarNumerosSimulaciones(TabPane tabPane) {
        if (tabPane == null) return;
        
        // Agrupar simulaciones por simulador padre
        Map<String, List<SimulacionFinal>> simulacionesPorSimulador = new HashMap<>();
        
        // Recolectar todas las simulaciones y agruparlas por simulador padre
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getContent() instanceof SimulacionFinal) {
                SimulacionFinal sim = (SimulacionFinal) tab.getContent();
                if (sim.simuladorPadreId != null) {
                    simulacionesPorSimulador
                        .computeIfAbsent(sim.simuladorPadreId, k -> new ArrayList<>())
                        .add(sim);
                }
            }
        }
        
        // Reasignar números para cada simulador
        for (List<SimulacionFinal> simulaciones : simulacionesPorSimulador.values()) {
            // Ordenar por posición en el TabPane para mantener el orden visual
            simulaciones.sort((s1, s2) -> {
                int pos1 = tabPane.getTabs().indexOf(findTabForSimulacion(tabPane, s1));
                int pos2 = tabPane.getTabs().indexOf(findTabForSimulacion(tabPane, s2));
                return Integer.compare(pos1, pos2);
            });
            
            // Reasignar números
            for (int i = 0; i < simulaciones.size(); i++) {
                simulaciones.get(i).numeroSimulacion = i + 1;
                simulaciones.get(i).actualizarTitulosPestañas();
            }
        }
    }

    private static Tab findTabForSimulacion(TabPane tabPane, SimulacionFinal sim) {
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getContent() == sim) {
                return tab;
            }
        }
        return null;
    }

    private void cargarFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/SimulacionFinal.fxml"));
            loader.setController(this);
            // Si tienes un bundle global, pásalo aquí:
            if (bundle != null) loader.setResources(bundle);
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
        btnDerivacion.setOnAction(e -> mostrarDerivacion());
        btnArbol.setOnAction(e -> mostrarArbolSintactico());
        
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
        // No mostrar mensaje de tabla vacía
        tablaHistorial.setPlaceholder(new Label(""));
        
        // Deshabilitar botones inicialmente
        btnPaso.setDisable(true);
        btnFinal.setDisable(true);
        btnRetroceso.setDisable(true);
        btnInicio.setDisable(true);
    }

    private void mostrarDialogoEditarCadena() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(bundle != null ? bundle.getString("simulacionfinal.dialog.editar.titulo") : "Editar cadena de entrada");

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
        Button btnBorrar = new Button(bundle != null ? bundle.getString("simulacionfinal.btn.borrar.ultimo") : "Borrar último");
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
        Button btnAceptar = new Button(bundle != null ? bundle.getString("button.aceptar") : "Aceptar");
        btnAceptar.getStyleClass().add("button-grammar");
        btnAceptar.setOnAction(ev -> {
            campoEntrada.setText(campoCadena.getText());
            dialog.close();
        });

        // Botón cancelar
        Button btnCancelar = new Button(bundle != null ? bundle.getString("button.cancelar") : "Cancelar");
        btnCancelar.getStyleClass().add("button-cancel");
        btnCancelar.setOnAction(ev -> dialog.close());

        HBox acciones = new HBox(10, btnBorrar, btnAceptar, btnCancelar);
        acciones.setAlignment(Pos.CENTER);
        acciones.setPadding(new Insets(10, 0, 0, 0));

        VBox layout = new VBox(15,
            new Label(bundle != null ? bundle.getString("simulacionfinal.dialog.editar.instruccion") : "Haz clic en los terminales para construir la cadena de entrada:"),
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
        alert.setTitle(bundle != null ? bundle.getString("simulacionfinal.alert.cadena.vacia.titulo") : "Cadena de entrada vacía");
        alert.setHeaderText(null);
        alert.setContentText(bundle != null ? bundle.getString("simulacionfinal.alert.cadena.vacia.mensaje") : "Introduce una cadena de entrada válida antes de iniciar la simulación.");
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

    private void mostrarDerivacion() {
        try {
            // Obtener el simuladorId del padre
            String simuladorId = null;
            for (Tab tab : tabPane.getTabs()) {
                if (tab.getUserData() != null && tab.getUserData().toString().startsWith("simulador_")) {
                    simuladorId = tab.getUserData().toString();
                    break;
                }
            }
            
            if (simuladorId != null) {
                // Obtener el número de grupo
                int numeroGrupo = TabManager.obtenerNumeroGrupo(tabPane, simuladorId);
                String tituloBase = bundle.getString("simulacionfinal.tab.derivacion");
                String tituloDerivacion = numeroGrupo > 0 ? 
                    numeroGrupo + "-" + tituloBase + " " + numeroSimulacion :
                    tituloBase + " " + numeroSimulacion;
                
                // Buscar si ya existe una pestaña de derivación
                Tab tabDerivacion = null;
                for (Tab tab : tabPane.getTabs()) {
                    if (tab.getUserData() != null && 
                        tab.getUserData().toString().equals("derivacion_" + simulacionId)) {
                        tabDerivacion = tab;
                        break;
                    }
                }
                
                // Si no existe, crear una nueva
                if (tabDerivacion == null) {
                    final Tab newTab = new Tab(tituloDerivacion);
                    newTab.setUserData("derivacion_" + simulacionId);
                    newTab.setClosable(true);
                    newTab.setOnCloseRequest(e -> {
                        e.consume();
                        tabPane.getTabs().remove(newTab);
                    });
                    tabPane.getTabs().add(newTab);
                    tabDerivacion = newTab;
                }
                
                // Crear el contenido de la derivación
                VBox layout = new VBox(10);
                layout.setPadding(new javafx.geometry.Insets(20));
                layout.setAlignment(javafx.geometry.Pos.TOP_LEFT);
                layout.setStyle("-fx-background-color: white;");
                
                Label titulo = new Label(bundle.getString("simulacionfinal.tab.derivacion"));
                titulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #3498DB;");
                
                TextArea areaDerivacion = new TextArea();
                areaDerivacion.setEditable(false);
                areaDerivacion.setWrapText(true);
                areaDerivacion.setStyle("-fx-font-family: monospace; -fx-font-size: 14px;");
                areaDerivacion.setPrefRowCount(20);
                
                // Generar la derivación
                StringBuilder derivacion = new StringBuilder();
                for (int i = 0; i < historialObservable.size(); i++) {
                    HistorialPaso paso = historialObservable.get(i);
                    derivacion.append("Paso ").append(i + 1).append(":\n");
                    derivacion.append("Pila: ").append(paso.getPila()).append("\n");
                    derivacion.append("Entrada: ").append(paso.getEntrada()).append("\n");
                    derivacion.append("Acción: ").append(paso.getAccion()).append("\n\n");
                }
                
                areaDerivacion.setText(derivacion.toString());
                
                layout.getChildren().addAll(titulo, areaDerivacion);
                tabDerivacion.setContent(layout);
                tabDerivacion.setText(tituloDerivacion);
                tabPane.getSelectionModel().select(tabDerivacion);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mostrarArbolSintactico() {
        try {
            // 1. Construir el árbol sintáctico como estructura de nodos
            NodoArbol raiz = construirArbolDesdeHistorial();
            // 2. Generar el código DOT
            String dot = generarDotDesdeArbol(raiz);
            // 3. Guardar DOT en archivo temporal
            File dotFile = File.createTempFile("arbol_sintactico", ".dot");
            File imgFile = File.createTempFile("arbol_sintactico", ".png");
            try (java.io.FileWriter fw = new java.io.FileWriter(dotFile)) {
                fw.write(dot);
            }
            // 4. Ejecutar Graphviz para generar la imagen
            ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng", dotFile.getAbsolutePath(), "-o", imgFile.getAbsolutePath());
            pb.start().waitFor();
            // 5. Mostrar la imagen en una pestaña
            javafx.scene.image.Image img = new javafx.scene.image.Image(imgFile.toURI().toString());
            javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(img);
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(700);
            imageView.setFitHeight(700);
            VBox layout = new VBox(15);
            layout.setPadding(new Insets(40, 0, 0, 0));
            layout.setAlignment(Pos.TOP_CENTER);
            String tituloArbol = bundle.getString("simulacionfinal.tab.arbol") + " " + numeroSimulacion;
            Label labelTitulo = new Label(tituloArbol);
            labelTitulo.setStyle(
                "-fx-font-size: 30px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #3498db;" +
                "-fx-padding: 0 0 32 0;"
            );
            layout.getChildren().addAll(labelTitulo, imageView);

            mostrarArbol(raiz);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Nodo para el árbol sintáctico
    private static class NodoArbol {
        String valor;
        List<NodoArbol> hijos = new ArrayList<>();
        NodoArbol(String valor) { this.valor = valor; }
    }

    // Construir el árbol sintáctico a partir del historial de pasos
    private NodoArbol construirArbolDesdeHistorial() {
        // Creamos la raíz con el símbolo inicial
        NodoArbol raiz = new NodoArbol(gramatica.getSimbInicial());
        construirRecursivo(raiz, new ArrayList<>(historialObservable));
        return raiz;
    }

    // Recursivo: expande el primer no terminal de cada producción
    private void construirRecursivo(NodoArbol nodo, List<HistorialPaso> pasos) {
        if (pasos.isEmpty()) return;
        HistorialPaso paso = pasos.remove(0);
        String accion = paso.getAccion();
        if (accion.contains("→")) {
            String[] partes = accion.split("→");
            if (partes.length == 2) {
                String derecha = partes[1].trim();
                if (!derecha.equals("ε")) {
                    String[] simbolos = derecha.split(" ");
                    for (String s : simbolos) {
                        if (!s.isEmpty()) {
                            NodoArbol hijo = new NodoArbol(s);
                            nodo.hijos.add(hijo);
                        }
                    }
                    // Expandir recursivamente los hijos no terminales
                    for (NodoArbol hijo : nodo.hijos) {
                        if (!esTerminal(hijo.valor) && !hijo.valor.equals("ε")) {
                            construirRecursivo(hijo, new ArrayList<>(pasos));
                        }
                    }
                } else {
                    nodo.hijos.add(new NodoArbol("ε"));
                }
            }
        }
    }

    // Generar el código DOT para Graphviz
    private String generarDotDesdeArbol(NodoArbol raiz) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph G {\n");
        sb.append("node [shape=ellipse, fontsize=18, fontname=Consolas, style=filled, fillcolor=white, color=black];\n");
        int[] id = {0};
        generarDotRec(raiz, sb, id, null);
        sb.append("}\n");
        return sb.toString();
    }

    private void generarDotRec(NodoArbol nodo, StringBuilder sb, int[] id, String parentId) {
        String myId = "n" + id[0]++;
        sb.append(myId + " [label=\"" + nodo.valor.replace("\"", "\\\"") + "\"];\n");
        if (parentId != null) {
            sb.append(parentId + " -> " + myId + ";\n");
        }
        for (NodoArbol hijo : nodo.hijos) {
            generarDotRec(hijo, sb, id, myId);
        }
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

    @Override
    public void actualizarTextos(ResourceBundle bundle) {
        this.bundle = bundle;
        
        // Actualizar textos de los controles
        labelEntrada.setText(bundle.getString("simulacionfinal.label.entrada"));
        labelPila.setText(bundle.getString("simulacionfinal.label.pila"));
        labelEntradaStack.setText(bundle.getString("simulacionfinal.label.entrada"));
        labelAccion.setText(bundle.getString("simulacionfinal.label.accion"));
        labelHistorial.setText(bundle.getString("simulacionfinal.label.historial"));
        
        btnIniciar.setText(bundle.getString("simulacionfinal.btn.iniciar"));
        btnPaso.setText(bundle.getString("simulacionfinal.btn.paso"));
        btnFinal.setText(bundle.getString("simulacionfinal.btn.final"));
        btnRetroceso.setText(bundle.getString("simulacionfinal.btn.retroceso"));
        btnInicio.setText(bundle.getString("simulacionfinal.btn.inicio"));
        btnEditarCadena.setText(bundle.getString("simulacionfinal.btn.editar.cadena"));
        btnDerivacion.setText(bundle.getString("simulacionfinal.btn.derivacion"));
        btnArbol.setText(bundle.getString("simulacionfinal.btn.arbol"));
        
        // Actualizar títulos de las pestañas
        actualizarTitulosPestañas();
    }

    private void mostrarArbol(NodoArbol raiz) {
        try {
            // Obtener el simuladorId del padre
            String simuladorId = null;
            for (Tab tab : tabPane.getTabs()) {
                if (tab.getUserData() != null && tab.getUserData().toString().startsWith("simulador_")) {
                    simuladorId = tab.getUserData().toString();
                    break;
                }
            }
            
            if (simuladorId != null) {
                // Obtener el número de grupo
                int numeroGrupo = TabManager.obtenerNumeroGrupo(tabPane, simuladorId);
                String tituloBase = bundle.getString("simulacionfinal.tab.arbol");
                String tituloArbol = numeroGrupo > 0 ? 
                    numeroGrupo + "-" + tituloBase + " " + numeroSimulacion :
                    tituloBase + " " + numeroSimulacion;
                
                // Buscar si ya existe una pestaña de árbol
                Tab tabArbol = null;
                for (Tab tab : tabPane.getTabs()) {
                    if (tab.getUserData() != null && 
                        tab.getUserData().toString().equals("arbol_" + simulacionId)) {
                        tabArbol = tab;
                        break;
                    }
                }
                
                // Si no existe, crear una nueva
                if (tabArbol == null) {
                    final Tab newTab = new Tab(tituloArbol);
                    newTab.setUserData("arbol_" + simulacionId);
                    newTab.setClosable(true);
                    newTab.setOnCloseRequest(e -> {
                        e.consume();
                        tabPane.getTabs().remove(newTab);
                    });
                    tabPane.getTabs().add(newTab);
                    tabArbol = newTab;
                }
                
                // Crear el contenido del árbol
                VBox layout = new VBox(10);
                layout.setPadding(new javafx.geometry.Insets(20));
                layout.setAlignment(javafx.geometry.Pos.TOP_LEFT);
                layout.setStyle("-fx-background-color: white;");
                
                Label titulo = new Label(bundle.getString("simulacionfinal.tab.arbol"));
                titulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #3498DB;");
                
                // Crear la imagen del árbol
                javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView();
                imageView.setFitWidth(800);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);
                
                // Generar la imagen del árbol y convertirla a Image de JavaFX
                BufferedImage bufferedImage = generarImagenArbol(raiz);
                javafx.scene.image.Image fxImage = SwingFXUtils.toFXImage(bufferedImage, null);
                imageView.setImage(fxImage);
                
                layout.getChildren().addAll(titulo, imageView);
                tabArbol.setContent(layout);
                tabArbol.setText(tituloArbol);
                tabPane.getSelectionModel().select(tabArbol);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private BufferedImage generarImagenArbol(NodoArbol raiz) {
        // Implementa la lógica para generar una imagen del árbol a partir del nodo raíz
        // Puedes usar bibliotecas como JavaFX o Swing para crear la imagen
        // Aquí se usa un ejemplo simple con JavaFX
        javafx.scene.image.WritableImage writableImage = new javafx.scene.image.WritableImage(800, 800);
        javafx.scene.image.PixelWriter pixelWriter = writableImage.getPixelWriter();
        generarImagenRec(raiz, 0, 0, 800, 800, pixelWriter);
        return SwingFXUtils.fromFXImage(writableImage, null);
    }

    private void generarImagenRec(NodoArbol nodo, int x, int y, int width, int height, javafx.scene.image.PixelWriter pixelWriter) {
        if (nodo == null) return;
        
        // Dibujar el nodo actual
        int color = 0xFF000000; // Negro
        pixelWriter.setArgb(x, y, color);
        
        // Dibujar los hijos
        int childWidth = width / (nodo.hijos.size() + 1);
        for (int i = 0; i < nodo.hijos.size(); i++) {
            int childX = x + (i + 1) * childWidth;
            int childY = y + 50;
            generarImagenRec(nodo.hijos.get(i), childX, childY, childWidth, height - 50, pixelWriter);
        }
    }
} 