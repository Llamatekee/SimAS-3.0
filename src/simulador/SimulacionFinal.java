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
import java.io.File;
import editor.ActualizableTextos;
import editor.TabManager;
import java.util.ResourceBundle;
import javafx.application.Platform;
import java.awt.image.BufferedImage;
import javafx.embed.swing.SwingFXUtils;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import javafx.scene.control.TreeView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TreeCell;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.control.Slider;

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

    private String simuladorPadreId;
    private int numeroSimulacion;
    private String grupoId;
    private int numeroGrupo;
    private int numeroInstancia = 1;
    public String simulacionId;

    // Referencias a pestañas hijas activas
    private Tab derivacionTab;
    private Tab arbolTab;

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

    // Nodo para el árbol sintáctico
    private static class NodoArbol {
        String valor;
        List<NodoArbol> hijos = new ArrayList<>();
        NodoArbol(String valor) { this.valor = valor; }
    }

    public SimulacionFinal(Gramatica gramatica, TablaPredictivaPaso5 tablaPredictiva, TabPane tabPane, ResourceBundle bundle) {
        this.gramatica = gramatica;
        this.tablaPredictiva = tablaPredictiva;
        this.tabPane = tabPane;
        this.bundle = bundle;
        // El simulacionId se asignará desde fuera (PanelNuevaSimDescPaso6)
        this.simulacionId = null; // Se asignará después
        
        // Obtener el simulador padre de la pestaña actualmente seleccionada
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null && selectedTab.getUserData() != null && 
            selectedTab.getUserData().toString().startsWith("simulador_")) {
            this.simuladorPadreId = selectedTab.getUserData().toString();
        }
        
        // Contar simulaciones específicamente para este simulador y encontrar la última
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
        
        // Añadir listener para cambios en las pestañas
        tabPane.getTabs().addListener((javafx.collections.ListChangeListener.Change<? extends Tab> c) -> {
            while (c.next()) {
                if (c.wasRemoved() || c.wasAdded()) {
                    // Actualizar el grupo y título cuando hay cambios en las pestañas
                    actualizarGrupoYTitulo();
                }
            }
        });
        
        cargarFXML();
        actualizarTitulosPestañasInterno(TabManager.contarGruposActivos(tabPane) > 1, simulacionesEnGrupo > 1);
        
        // Añadir listener para cerrar pestañas hijas
        if (tabPane != null) {
            tabPane.getTabs().addListener((javafx.collections.ListChangeListener.Change<? extends Tab> change) -> {
                while (change.next()) {
                    if (change.wasRemoved()) {
                        for (Tab tab : change.getRemoved()) {
                            if (tab.getContent() == this) {
                                // Cerrar las pestañas hijas usando TabManager
                                Platform.runLater(() -> {
                                    // Usar TabManager para cerrar las pestañas hijas
                                    if (simulacionId != null) {
                                        TabManager.closeChildTabs(tabPane, simulacionId);
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
     * Establece el ID de simulación desde fuera (usado por TabManager)
     */
    public void setSimulacionId(String simulacionId) {
        this.simulacionId = simulacionId;
    }

    /**
     * Actualiza el grupo y título de la simulación basado en el estado actual del TabPane
     */
    public void actualizarGrupoYTitulo() {
        if (simuladorPadreId == null || tabPane == null) return;
        
        // Obtener el nuevo grupo y número del simulador padre
        String nuevoGrupoId = TabManager.obtenerGrupoDeElemento(tabPane, simuladorPadreId);
        int nuevoNumeroGrupo = TabManager.obtenerNumeroGrupo(tabPane, simuladorPadreId);
        boolean hayMultiplesGrupos = TabManager.contarGruposActivos(tabPane) > 1;
        
        // Actualizar los valores internos
        this.grupoId = nuevoGrupoId;
        this.numeroGrupo = nuevoNumeroGrupo;
        
        // Contar simulaciones en el mismo grupo para determinar si mostrar el número de instancia
        int simulacionesEnGrupo = contarSimulacionesEnGrupo();
        boolean mostrarInstancia = simulacionesEnGrupo > 1;
        
        // Forzar actualización de títulos
        actualizarTitulosPestañas(nuevoNumeroGrupo, hayMultiplesGrupos, numeroInstancia, mostrarInstancia);
    }

    /**
     * Actualiza los títulos de las pestañas con el número de grupo especificado.
     */
    public void actualizarTitulosPestañas(int numeroGrupo, boolean mostrarGrupo, int numeroInstancia, boolean mostrarInstancia) {
        System.out.println("[DEBUG] SimulacionFinal.actualizarTitulosPestañas: recibido numeroGrupo=" + numeroGrupo + 
                         ", mostrarGrupo=" + mostrarGrupo + ", numeroInstancia=" + numeroInstancia + 
                         ", mostrarInstancia=" + mostrarInstancia);
        System.out.println("[DEBUG] SimulacionFinal.actualizarTitulosPestañas: numeroGrupo antes=" + this.numeroGrupo);
        
        this.numeroGrupo = numeroGrupo;
        this.numeroInstancia = numeroInstancia;
        
        System.out.println("[DEBUG] SimulacionFinal.actualizarTitulosPestañas: numeroGrupo después=" + this.numeroGrupo);
        
        actualizarTitulosPestañasInterno(mostrarGrupo, mostrarInstancia);
    }

    /**
     * Actualiza los títulos de las pestañas usando el número de grupo actual.
     */
    public void actualizarTitulosPestañas() {
        if (tabPane == null || simuladorPadreId == null) return;
        
        boolean hayMultiplesGrupos = TabManager.contarGruposActivos(tabPane) > 1;
        int simulacionesEnGrupo = contarSimulacionesEnGrupo();
        boolean mostrarInstancia = simulacionesEnGrupo > 1;
        
        actualizarTitulosPestañasInterno(hayMultiplesGrupos, mostrarInstancia);
    }

    /**
     * Actualiza los títulos de las pestañas relacionadas con esta simulación.
     */
    private void actualizarTitulosPestañasInterno(boolean mostrarGrupo, boolean mostrarInstancia) {
        System.out.println("[DEBUG] SimulacionFinal.actualizarTitulosPestañasInterno: buscando pestaña para simulación...");
        System.out.println("[DEBUG] SimulacionFinal.actualizarTitulosPestañasInterno: simulacionId=" + simulacionId);
        
        // Buscar la pestaña de simulación y actualizarla
        for (Tab tab : tabPane.getTabs()) {
            System.out.println("[DEBUG] SimulacionFinal.actualizarTitulosPestañasInterno: revisando pestaña userData=" + 
                             (tab.getUserData() != null ? tab.getUserData().toString() : "null") + 
                             ", contenido es this=" + (tab.getContent() == this));
            
            // Buscar por userData primero, luego por contenido como respaldo
            if ((simulacionId != null && tab.getUserData() != null && 
                 tab.getUserData().toString().equals(simulacionId)) || 
                tab.getContent() == this) {
                
                String tituloBase = bundle.getString("simulador.paso6.simulacion");
                String nuevoTitulo = construirTitulo(tituloBase, mostrarGrupo, mostrarInstancia);
                String tituloAnterior = tab.getText();
                tab.setText(nuevoTitulo);
                System.out.println("[DEBUG] SimulacionFinal.actualizarTitulosPestañasInterno: pestaña encontrada y actualizada: " + 
                                 tituloAnterior + " -> " + nuevoTitulo);
                break;
            }
        }
        
        // Las pestañas de derivación y árbol ahora se gestionan automáticamente por TabManager
        // No necesitamos actualizarlas manualmente aquí
    }

    /**
     * Construye el título de una pestaña basado en el estado actual.
     */
    private String construirTitulo(String tituloBase, boolean mostrarGrupo, boolean mostrarInstancia) {
        StringBuilder titulo = new StringBuilder();
        
        if (mostrarGrupo) {
            titulo.append(numeroGrupo).append("-");
        }
        
        titulo.append(tituloBase);
        
        if (mostrarInstancia) {
            titulo.append(" (").append(numeroInstancia).append(")");
        }
        
        String resultado = titulo.toString();
        System.out.println("[DEBUG] SimulacionFinal.construirTitulo: mostrarGrupo=" + mostrarGrupo + 
                         ", numeroGrupo=" + numeroGrupo + ", mostrarInstancia=" + mostrarInstancia + 
                         ", numeroInstancia=" + numeroInstancia + " -> resultado: " + resultado);
        
        return resultado;
    }

    /**
     * Reasigna los números de las simulaciones en orden secuencial.
     */
    private void reasignarNumerosSimulaciones(TabPane tabPane) {
        if (tabPane == null || simuladorPadreId == null) return;
        
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
                SimulacionFinal sim = simulaciones.get(i);
                sim.setNumeroInstancia(i + 1);
                sim.actualizarTitulosPestañas();
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
        // Limpiar el historial y estados anteriores
        historialObservable.clear();
        estadosAnteriores.clear();
        pasoActual = 0;

        // Limpiar el área de mensajes/acción
        areaMensajes.clear();

        // Inicializar la pila con el símbolo inicial y el marcador de fin
        pilaSimulacion = new Stack<>();
        pilaSimulacion.push("$");
        pilaSimulacion.push(gramatica.getSimbInicial());

        // Preparar la entrada
        String entrada = campoEntrada.getText().trim();
        if (entrada.isEmpty()) {
            mostrarAlertaCadenaVacia();
            return;
        }

        // Convertir la entrada en una lista de símbolos y añadir el marcador de fin
        entradaSimulacion = Arrays.asList(entrada.split("\\s+"));
        List<String> entradaConFin = new ArrayList<>(entradaSimulacion);
        entradaConFin.add("$");
        entradaSimulacion = entradaConFin;

        // Iniciar la simulación
        simulacionEnCurso = true;
        // El botón Iniciar siempre debe estar activo
        btnIniciar.setDisable(false);
        btnPaso.setDisable(false);
        btnFinal.setDisable(false);
        btnRetroceso.setDisable(true);
        btnInicio.setDisable(true);
        campoEntrada.setDisable(true);
        btnEditarCadena.setDisable(false);

        // Actualizar la vista
        actualizarVista();

        // Resetear las pestañas hijas al estado inicial si están activas
        if (derivacionTab != null && tabPane.getTabs().contains(derivacionTab)) {
            TextArea areaDerivacion = (TextArea) derivacionTab.getContent();
            areaDerivacion.setText(gramatica.getSimbInicial()); // Solo mostrar el símbolo inicial
        }

        if (arbolTab != null && tabPane.getTabs().contains(arbolTab)) {
            // Crear árbol con solo el nodo inicial
            NodoArbol raiz = new NodoArbol(gramatica.getSimbInicial());
            
            // Generar el código DOT para Graphviz
            String dotCode = generarDotDesdeArbol(raiz);
            
            try {
                // Crear un archivo temporal para el código DOT
                java.nio.file.Path dotFile = java.nio.file.Files.createTempFile("arbol_", ".dot");
                java.nio.file.Files.write(dotFile, dotCode.getBytes());
                
                // Crear un archivo temporal para la imagen
                java.nio.file.Path imgFile = java.nio.file.Files.createTempFile("arbol_", ".png");
                
                // Ejecutar Graphviz para generar la imagen
                ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng", dotFile.toString(), "-o", imgFile.toString());
                Process process = pb.start();
                process.waitFor();
                
                // Cargar la imagen en un ImageView
                javafx.scene.image.Image imagen = new javafx.scene.image.Image(imgFile.toUri().toString());
                javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(imagen);
                imageView.setPreserveRatio(true);
                imageView.setFitWidth(800);
                
                // Obtener el contenedor VBox existente y el ScrollPane
                VBox contenedor = (VBox) arbolTab.getContent();
                ScrollPane scrollPane = (ScrollPane) contenedor.getChildren().get(0);
                
                // Actualizar el contenido del ScrollPane
                scrollPane.setContent(imageView);
                
                // Limpiar archivos temporales
                java.nio.file.Files.deleteIfExists(dotFile);
                java.nio.file.Files.deleteIfExists(imgFile);
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
            actualizarVista();
            actualizarPestañasHijas();
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
            actualizarVista();
            actualizarPestañasHijas();
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
                actualizarVista();
                actualizarPestañasHijas();
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
        actualizarPestañasHijas();
    }

    private void avanzarAlFinal() {
        while (simulacionEnCurso) {
            avanzarPaso();
        }
        actualizarPestañasHijas();
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
        actualizarPestañasHijas();
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
        actualizarPestañasHijas();
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

    /**
     * Crea una pestaña de derivación usando TabManager para gestión correcta de grupos.
     */
    private Tab crearPestanaDerivacionConTabManager() {
        if (simulacionId == null) {
            System.err.println("Error: simulacionId es null en crearPestanaDerivacionConTabManager");
            return null;
        }
        
        String childId = "derivacion_" + simulacionId;
        String tituloBase = bundle.getString("simulacionfinal.tab.derivacion");
        
        // Crear el contenido
        TextArea areaDerivacion = new TextArea();
        areaDerivacion.setEditable(false);
        areaDerivacion.setWrapText(true);
        
        // Generar la derivación
        StringBuilder derivacion = new StringBuilder();
        for (HistorialPaso paso : historialObservable) {
            derivacion.append(paso.getAccion()).append("\n");
        }
        areaDerivacion.setText(derivacion.toString());
        
        // Usar TabManager para crear la pestaña como hija de la simulación
        Tab nuevaPestana = TabManager.getOrCreateTab(
            tabPane,
            TextArea.class, // Usar TextArea como tipo de contenido
            tituloBase,
            areaDerivacion,
            simulacionId, // parentId es el ID de la simulación
            childId
        );
        
        return nuevaPestana;
    }

    /**
     * Crea una pestaña de árbol sintáctico usando TabManager para gestión correcta de grupos.
     */
    private Tab crearPestanaArbolConTabManager() {
        if (simulacionId == null) {
            System.err.println("Error: simulacionId es null en crearPestanaArbolConTabManager");
            return null;
        }
        
        String childId = "arbol_" + simulacionId;
        String tituloBase = bundle.getString("simulacionfinal.tab.arbol");
        
        // Crear el árbol con al menos el nodo inicial
        NodoArbol raiz;
        if (historialObservable.isEmpty()) {
            raiz = new NodoArbol(gramatica.getSimbInicial());
        } else {
            raiz = construirArbolDesdeHistorial();
        }
        
        // Generar el código DOT para Graphviz
        String dotCode = generarDotDesdeArbol(raiz);
        
        try {
            // Crear un archivo temporal para el código DOT
            java.nio.file.Path dotFile = java.nio.file.Files.createTempFile("arbol_", ".dot");
            java.nio.file.Files.write(dotFile, dotCode.getBytes());
            
            // Crear un archivo temporal para la imagen
            java.nio.file.Path imgFile = java.nio.file.Files.createTempFile("arbol_", ".png");
            
            // Ejecutar Graphviz para generar la imagen
            ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng", dotFile.toString(), "-o", imgFile.toString());
            Process process = pb.start();
            process.waitFor();
            
            // Cargar la imagen en un ImageView
            javafx.scene.image.Image imagen = new javafx.scene.image.Image(imgFile.toUri().toString());
            javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(imagen);
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(800);
            
            // Crear un ScrollPane para permitir zoom y scroll
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setContent(imageView);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            
            // Añadir controles de zoom
            Slider zoomSlider = new Slider(0.5, 2, 1);
            zoomSlider.setShowTickLabels(true);
            zoomSlider.setShowTickMarks(true);
            
            // Vincular el zoom del slider con la escala de la imagen
            zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                imageView.setScaleX(newVal.doubleValue());
                imageView.setScaleY(newVal.doubleValue());
            });
            
            // Crear contenedor para la imagen y el slider
            VBox contenedor = new VBox(10);
            contenedor.setPadding(new Insets(10));
            contenedor.setAlignment(Pos.CENTER);
            contenedor.getChildren().addAll(scrollPane, zoomSlider);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);
            
            // Usar TabManager para crear la pestaña como hija de la simulación
            Tab nuevaPestana = TabManager.getOrCreateTab(
                tabPane,
                VBox.class, // Usar VBox como tipo de contenido
                tituloBase,
                contenedor,
                simulacionId, // parentId es el ID de la simulación
                childId
            );
            
            // Limpiar archivos temporales
            java.nio.file.Files.deleteIfExists(dotFile);
            java.nio.file.Files.deleteIfExists(imgFile);
            
            return nuevaPestana;
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Muestra la derivación de la simulación actual.
     */
    private void mostrarDerivacion() {
        if (simulacionId == null) {
            System.err.println("Error: simulacionId es null en mostrarDerivacion");
            return;
        }
        
        // Buscar si ya existe una pestaña de derivación para esta simulación
        derivacionTab = null;
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() != null && 
                tab.getUserData().toString().equals("derivacion_" + simulacionId)) {
                derivacionTab = tab;
                break;
            }
        }
        
        // Si no existe la pestaña, crearla usando TabManager
        if (derivacionTab == null) {
            derivacionTab = crearPestanaDerivacionConTabManager();
            
            // Añadir listener para cuando se cierre la pestaña
            if (derivacionTab != null) {
                derivacionTab.setOnClosed(e -> derivacionTab = null);
            }
        } else {
            // Si ya existe, actualizar su contenido
            TextArea areaDerivacion = (TextArea) derivacionTab.getContent();
            StringBuilder derivacion = new StringBuilder();
            for (HistorialPaso paso : historialObservable) {
                derivacion.append(paso.getAccion()).append("\n");
            }
            areaDerivacion.setText(derivacion.toString());
        }
        
        // Seleccionar la pestaña
        if (derivacionTab != null) {
            tabPane.getSelectionModel().select(derivacionTab);
        }
    }

    /**
     * Muestra el contenido del árbol en una pestaña existente.
     */
    private void mostrarArbol(NodoArbol raiz) {
        System.out.println("Iniciando mostrarArbol()");
        if (raiz == null) {
            System.out.println("Error: raiz es null");
            return;
        }
        
        // Crear un TreeView para mostrar el árbol
        TreeView<String> treeView = new TreeView<>();
        TreeItem<String> rootItem = convertirNodoArbolATreeItem(raiz);
        treeView.setRoot(rootItem);
        treeView.setShowRoot(true);
        
        System.out.println("TreeView creado con raíz: " + rootItem.getValue());
        
        // Configurar el estilo del TreeView
        treeView.setStyle("-fx-font-size: 14px; -fx-font-family: 'Consolas';");
        
        // Personalizar la apariencia de los nodos
        treeView.setCellFactory(tv -> new TreeCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    // Añadir estilo para nodos terminales y no terminales
                    if (esTerminal(item)) {
                        setStyle("-fx-text-fill: #2196F3;"); // Azul para terminales
                    } else {
                        setStyle("-fx-text-fill: #4CAF50;"); // Verde para no terminales
                    }
                }
            }
        });
        
        // Expandir todo el árbol por defecto
        expandirArbol(rootItem);
        
        // Crear un ScrollPane para permitir scroll si el árbol es grande
        ScrollPane scrollPane = new ScrollPane(treeView);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        
        // Buscar la pestaña del árbol y actualizar su contenido
        boolean pestañaEncontrada = false;
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() != null && 
                tab.getUserData().toString().equals("arbol_" + simulacionId)) {
                
                // Crear un contenedor para el árbol
                BorderPane contenedor = new BorderPane();
                contenedor.setCenter(scrollPane);
                
                // Establecer el contenido de la pestaña
                tab.setContent(contenedor);
                pestañaEncontrada = true;
                System.out.println("Contenido establecido en la pestaña del árbol");
                break;
            }
        }
        
        if (!pestañaEncontrada) {
            System.out.println("Error: No se encontró la pestaña para mostrar el árbol");
        }
    }

    private TreeItem<String> convertirNodoArbolATreeItem(NodoArbol nodo) {
        System.out.println("Convirtiendo nodo a TreeItem: " + nodo.valor);
        TreeItem<String> item = new TreeItem<>(nodo.valor);
        
        for (NodoArbol hijo : nodo.hijos) {
            item.getChildren().add(convertirNodoArbolATreeItem(hijo));
        }
        
        return item;
    }

    private void expandirArbol(TreeItem<?> item) {
        if (item != null) {
            item.setExpanded(true);
            for (TreeItem<?> child : item.getChildren()) {
                expandirArbol(child);
            }
        }
    }

    /**
     * Muestra el árbol sintáctico de la simulación actual.
     */
    private void mostrarArbolSintactico() {
        if (simulacionId == null) {
            System.err.println("Error: simulacionId es null en mostrarArbolSintactico");
            return;
        }
        
        // Buscar si ya existe una pestaña de árbol para esta simulación
        arbolTab = null;
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() != null && 
                tab.getUserData().toString().equals("arbol_" + simulacionId)) {
                arbolTab = tab;
                break;
            }
        }
        
        // Si no existe la pestaña, crearla usando TabManager
        if (arbolTab == null) {
            arbolTab = crearPestanaArbolConTabManager();
            
            // Añadir listener para cuando se cierre la pestaña
            if (arbolTab != null) {
                arbolTab.setOnClosed(e -> arbolTab = null);
            }
        } else {
            // Si ya existe, actualizar su contenido
            actualizarContenidoArbol(arbolTab);
        }
        
        // Seleccionar la pestaña
        if (arbolTab != null) {
            tabPane.getSelectionModel().select(arbolTab);
        }
    }

    /**
     * Actualiza el contenido de una pestaña de árbol existente.
     */
    private void actualizarContenidoArbol(Tab arbolTab) {
        // Construir el nuevo árbol
        NodoArbol raiz;
        if (historialObservable.isEmpty()) {
            raiz = new NodoArbol(gramatica.getSimbInicial());
        } else {
            raiz = construirArbolDesdeHistorial();
        }

        // Generar el código DOT para Graphviz
        String dotCode = generarDotDesdeArbol(raiz);
        
        try {
            // Crear un archivo temporal para el código DOT
            java.nio.file.Path dotFile = java.nio.file.Files.createTempFile("arbol_", ".dot");
            java.nio.file.Files.write(dotFile, dotCode.getBytes());
            
            // Crear un archivo temporal para la imagen
            java.nio.file.Path imgFile = java.nio.file.Files.createTempFile("arbol_", ".png");
            
            // Ejecutar Graphviz para generar la imagen
            ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng", dotFile.toString(), "-o", imgFile.toString());
            Process process = pb.start();
            process.waitFor();
            
            // Cargar la imagen en un ImageView
            javafx.scene.image.Image imagen = new javafx.scene.image.Image(imgFile.toUri().toString());
            javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(imagen);
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(800);
            
            // Obtener el contenedor VBox existente y el ScrollPane
            VBox contenedor = (VBox) arbolTab.getContent();
            ScrollPane scrollPane = (ScrollPane) contenedor.getChildren().get(0);
            
            // Actualizar el contenido del ScrollPane
            scrollPane.setContent(imageView);
            
            // Limpiar archivos temporales
            java.nio.file.Files.deleteIfExists(dotFile);
            java.nio.file.Files.deleteIfExists(imgFile);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String generarDotDesdeArbol(NodoArbol raiz) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph G {\n");
        sb.append("  node [shape=box, style=rounded, fontname=\"Arial\"];\n");
        sb.append("  edge [arrowhead=none];\n");
        
        // Usar un contador para generar IDs únicos de nodos
        int[] idCounter = {0};
        generarDotRec(raiz, sb, idCounter, null);
        
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

    /**
     * Verifica si esta simulación pertenece a un simulador específico.
     * @param simuladorId El ID del simulador a verificar
     * @return true si la simulación pertenece al simulador especificado
     */
    public boolean perteneceASimulador(String simuladorId) {
        return this.simuladorPadreId != null && this.simuladorPadreId.equals(simuladorId);
    }
    
    /**
     * Verifica si una pestaña es hija de esta simulación.
     * @param tab La pestaña a verificar
     * @return true si la pestaña es una derivación o árbol sintáctico de esta simulación
     */
    public boolean esHijaDeLaSimulacion(Tab tab) {
        if (tab == null || tab.getUserData() == null) return false;
        
        String userData = tab.getUserData().toString();
        return (userData.startsWith("derivacion_") || userData.startsWith("arbol_")) && 
               userData.endsWith(this.simulacionId);
    }

    /**
     * Obtiene el ID del simulador padre de esta simulación.
     * @return El ID del simulador padre
     */
    public String getSimuladorPadreId() {
        return simuladorPadreId;
    }

    /**
     * Cuenta el número de simulaciones que pertenecen al mismo grupo que esta simulación.
     */
    private int contarSimulacionesEnGrupo() {
        if (tabPane == null) return 1;
        
        int simulacionesEnGrupo = 0;
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getContent() instanceof SimulacionFinal) {
                SimulacionFinal sim = (SimulacionFinal) tab.getContent();
                if (sim.simuladorPadreId != null && sim.simuladorPadreId.equals(this.simuladorPadreId)) {
                    simulacionesEnGrupo++;
                }
            }
        }
        return simulacionesEnGrupo;
    }

    public void setGrupoId(String grupoId) {
        this.grupoId = grupoId;
    }

    public void setNumeroGrupo(int numeroGrupo) {
        this.numeroGrupo = numeroGrupo;
    }

    public String getGrupoId() {
        return grupoId;
    }

    public int getNumeroGrupo() {
        return numeroGrupo;
    }

    public void setNumeroInstancia(int numeroInstancia) {
        this.numeroInstancia = numeroInstancia;
    }

    public int getNumeroInstancia() {
        return numeroInstancia;
    }

    // Construir el árbol sintáctico a partir del historial de pasos
    private NodoArbol construirArbolDesdeHistorial() {
        // Creamos la raíz con el símbolo inicial
        NodoArbol raiz = new NodoArbol(gramatica.getSimbInicial());
        
        // Crear una copia del historial para no modificar el original
        List<HistorialPaso> historialCopia = new ArrayList<>(historialObservable);
        construirRecursivo(raiz, historialCopia);
        
        return raiz;
    }

    // Recursivo: expande el primer no terminal de cada producción
    private void construirRecursivo(NodoArbol nodo, List<HistorialPaso> pasos) {
        if (pasos.isEmpty()) {
            return;
        }
        
        // Buscar la producción que expande este nodo
        for (int i = 0; i < pasos.size(); i++) {
            HistorialPaso paso = pasos.get(i);
            String accion = paso.getAccion();
            
            // Si es una producción (contiene una flecha)
            if (accion.contains("→")) {
                // Eliminar el número de producción si existe
                String accionLimpia = accion.replaceAll("^\\d+\\.\\s*", "").trim();
                String[] partes = accionLimpia.split("→");
                String izquierda = partes[0].trim();
                String derecha = partes[1].trim();
                
                // Si esta producción corresponde al nodo actual
                if (izquierda.equals(nodo.valor)) {
                    // Dividir la parte derecha en símbolos
                    String[] simbolos = derecha.split("\\s+");
                    
                    // Crear nodos hijos para cada símbolo
                    for (String simbolo : simbolos) {
                        if (!simbolo.isEmpty()) {
                            NodoArbol hijo = new NodoArbol(simbolo);
                            nodo.hijos.add(hijo);
                            
                            // Si el hijo es no terminal, procesarlo recursivamente
                            if (!esTerminal(simbolo)) {
                                // Crear una nueva lista con los pasos restantes
                                List<HistorialPaso> pasosRestantes = new ArrayList<>(pasos.subList(i + 1, pasos.size()));
                                construirRecursivo(hijo, pasosRestantes);
                            }
                        }
                    }
                    
                    // Remover esta producción para no reutilizarla
                    pasos.remove(i);
                    return;
                }
            }
        }
    }

    /**
     * Actualiza el contenido de las pestañas hijas activas (derivación y árbol).
     */
    private void actualizarPestañasHijas() {
        if (simulacionId == null) {
            return; // No hay simulacionId, no se pueden actualizar las pestañas hijas
        }
        
        // Buscar las pestañas hijas usando TabManager
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() != null) {
                String userData = tab.getUserData().toString();
                
                // Actualizar pestaña de derivación si está activa
                if (userData.equals("derivacion_" + simulacionId)) {
                    TextArea areaDerivacion = (TextArea) tab.getContent();
                    StringBuilder derivacion = new StringBuilder();
                    for (HistorialPaso paso : historialObservable) {
                        derivacion.append(paso.getAccion()).append("\n");
                    }
                    areaDerivacion.setText(derivacion.toString());
                }
                
                // Actualizar pestaña de árbol si está activa
                else if (userData.equals("arbol_" + simulacionId)) {
                    actualizarContenidoArbol(tab);
                }
            }
        }
    }
} 