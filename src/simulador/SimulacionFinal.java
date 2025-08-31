package simulador;

import gramatica.Gramatica;
import gramatica.TablaPredictivaPaso5;
import gramatica.FuncionError;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.scene.Scene;
import javafx.scene.layout.FlowPane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import java.io.IOException;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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
import utils.ActualizableTextos;
import utils.TabManager;
import java.util.ResourceBundle;
import javafx.application.Platform;
import java.util.Map;
import java.util.HashMap;
import javafx.scene.control.Label;

public class SimulacionFinal extends BorderPane implements ActualizableTextos {
    @FXML private TextField campoEntrada;
    @FXML private Button btnIniciar;
    @FXML private Button btnPaso;
    @FXML private Button btnFinal;
    @FXML private Button btnRetroceso;
    @FXML private Button btnInicio;
    // Las áreas de texto individuales se han eliminado, ahora solo usamos la tabla de historial
    @FXML private Button btnEditarCadena;
    @FXML private Button btnDerivacion;
    @FXML private Button btnArbol;
    @FXML private Button btnGenerarInforme;
    @FXML private Label labelTitulo;
    @FXML private TableView<HistorialPaso> tablaHistorial;
    @FXML private TableColumn<HistorialPaso, String> colPaso;
    @FXML private TableColumn<HistorialPaso, String> colPila;
    @FXML private TableColumn<HistorialPaso, String> colEntrada;
    @FXML private TableColumn<HistorialPaso, String> colAccion;
    // Labels para internacionalización
    @FXML private Label labelEntrada;
    @FXML private Label labelHistorial;

    private Gramatica gramatica;
    private TablaPredictivaPaso5 tablaPredictiva;
    private TabPane tabPane;
    private ResourceBundle bundle;
    
    // Variables para el informe PDF (copiadas del paso 6)
    private List<FuncionError> funcionesError;

    // Estado de la simulación
    private Stack<String> pilaSimulacion;
    private List<String> entradaSimulacion;
    private int pasoActual;
    private boolean simulacionEnCurso = false;
    private ObservableList<HistorialPaso> historialObservable = FXCollections.observableArrayList();
    // Lista para almacenar los estados anteriores
    private List<EstadoSimulacion> estadosAnteriores = new ArrayList<>();
    // Flag para saber si ya se ha realizado al menos un paso
    private boolean seHaRealizadoAlMenosUnPaso = false;
    // Flag para saber si estamos en un estado final (aceptación o error)
    private boolean estadoFinalAlcanzado = false;

    private String simuladorPadreId;
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
        
        public EstadoSimulacion(Stack<String> pila, List<String> entrada, String accion) {
            this.pila = new Stack<>();
            this.pila.addAll(pila);
            this.entrada = new ArrayList<>(entrada);
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
        
        // Inicializar funciones de error para el informe PDF
        if (tablaPredictiva != null) {
            this.funcionesError = tablaPredictiva.getFuncionesError();
        }
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
        this.numeroGrupo = numeroGrupo;
        this.numeroInstancia = numeroInstancia;
        
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
        // Buscar la pestaña de simulación y actualizarla
        for (Tab tab : tabPane.getTabs()) {
            // Buscar por userData primero, luego por contenido como respaldo
            if ((simulacionId != null && tab.getUserData() != null && 
                 tab.getUserData().toString().equals(simulacionId)) || 
                tab.getContent() == this) {
                
                String tituloBase = bundle != null ? bundle.getString("simulacionfinal.titulo") : "Simulación";
                String nuevoTitulo = construirTitulo(tituloBase, mostrarGrupo, mostrarInstancia);
                tab.setText(nuevoTitulo);
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
            
            // Aplicar estilos CSS específicos para la simulación
            this.getStylesheets().add(getClass().getResource("/vistas/styles2.css").toExternalForm());
            
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
        btnGenerarInforme.setOnAction(e -> generarInforme());
        
        // Las áreas de texto individuales se han eliminado, ahora solo usamos la tabla de historial
        
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
        btnGenerarInforme.setDisable(true);

        // Configurar lógica de habilitación de botones de navegación
        configurarLogicaBotones();
    }

    /**
     * Configura la lógica de habilitación/deshabilitación de los botones según el estado de la simulación
     */
    private void configurarLogicaBotones() {
        // Los botones de retroceso deben estar deshabilitados inicialmente
        // Se habilitarán cuando haya estados anteriores para retroceder
        actualizarEstadoBotonesNavegacion();
        actualizarEstadoBotonInforme();
    }

    /**
     * Actualiza el estado de habilitación de los botones de navegación según el estado actual
     */
    private void actualizarEstadoBotonesNavegacion() {
        // Los botones de retroceso deben estar habilitados solo si se ha realizado al menos un paso
        btnRetroceso.setDisable(!seHaRealizadoAlMenosUnPaso);
        btnInicio.setDisable(!seHaRealizadoAlMenosUnPaso);
    }

    /**
     * Actualiza el estado del botón de generar informe según si estamos en un estado final
     */
    private void actualizarEstadoBotonInforme() {
        // El botón de informe solo debe estar habilitado si estamos en un estado final
        btnGenerarInforme.setDisable(!estadoFinalAlcanzado);
    }

    private void mostrarDialogoEditarCadena() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(bundle != null ? bundle.getString("simulacionfinal.dialog.editar.titulo") : "Editar cadena de entrada");

        // Crear contenedor principal con estilo moderno y tamaño más pequeño
        VBox mainContainer = new VBox(15);
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setPadding(new Insets(20));
        mainContainer.getStyleClass().add("dialog-container");

        // Header del diálogo
        Label headerLabel = new Label(bundle != null ? bundle.getString("simulacionfinal.dialog.editar.instruccion") : 
                                    "Haz clic en los terminales para construir la cadena de entrada:");
        headerLabel.getStyleClass().add("dialog-header");
        headerLabel.setWrapText(true);
        headerLabel.setAlignment(Pos.CENTER);

        // Panel de terminales con estilo moderno
        FlowPane terminalPane = new FlowPane();
        terminalPane.setHgap(8);
        terminalPane.setVgap(8);
        terminalPane.setPadding(new Insets(15));
        terminalPane.setAlignment(Pos.CENTER);
        terminalPane.getStyleClass().add("dialog-content");

        StringBuilder cadenaActual = new StringBuilder(campoEntrada.getText());
        
        // Campo de texto con estilo moderno
        TextField campoCadena = new TextField(cadenaActual.toString());
        campoCadena.setEditable(false);
        campoCadena.setPrefWidth(280);
        campoCadena.getStyleClass().add("dialog-field");

        // Botones de terminales más pequeños
        for (var terminal : gramatica.getTerminales()) {
            Button btn = new Button(terminal.getNombre());
            btn.getStyleClass().add("dialog-button");
            btn.setMinWidth(60);
            btn.setPrefWidth(60);
            btn.setMinHeight(35);
            btn.setPrefHeight(35);
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
        btnBorrar.setMinWidth(120);
        btnBorrar.setPrefWidth(120);
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
        btnAceptar.getStyleClass().add("button-finish");
        btnAceptar.setMinWidth(120);
        btnAceptar.setPrefWidth(120);
        btnAceptar.setOnAction(ev -> {
            campoEntrada.setText(campoCadena.getText());
            dialog.close();
        });

        // Botón cancelar
        Button btnCancelar = new Button(bundle != null ? bundle.getString("button.cancelar") : "Cancelar");
        btnCancelar.getStyleClass().add("button-cancel");
        btnCancelar.setMinWidth(120);
        btnCancelar.setPrefWidth(120);
        btnCancelar.setOnAction(ev -> dialog.close());

        // Contenedor de acciones con estilo moderno
        HBox acciones = new HBox(15);
        acciones.setAlignment(Pos.CENTER);
        acciones.setPadding(new Insets(15, 0, 0, 0));
        acciones.getStyleClass().add("dialog-actions");
        acciones.getChildren().addAll(btnBorrar, btnAceptar, btnCancelar);

        // Agregar elementos al contenedor principal
        mainContainer.getChildren().addAll(headerLabel, terminalPane, campoCadena, acciones);

        Scene scene = new Scene(mainContainer);
        scene.getStylesheets().add(getClass().getResource("/vistas/styles2.css").toExternalForm());
        dialog.setScene(scene);
        dialog.setResizable(false);
        
        // Obtener la ventana padre (la ventana principal de la aplicación)
        Stage parentStage = (Stage) this.getScene().getWindow();
        
        // Crear listeners reutilizables
        javafx.beans.value.ChangeListener<Number> xListener = (obs, oldVal, newVal) -> {
            centrarDialogoEnVentanaPadre(dialog, parentStage);
        };
        
        javafx.beans.value.ChangeListener<Number> yListener = (obs, oldVal, newVal) -> {
            centrarDialogoEnVentanaPadre(dialog, parentStage);
        };
        
        // Agregar listeners para mantener el diálogo centrado cuando se mueva la ventana padre
        parentStage.xProperty().addListener(xListener);
        parentStage.yProperty().addListener(yListener);
        
        // Mostrar el diálogo y luego centrarlo una vez que tenga dimensiones
        dialog.show();
        
        // Centrar el diálogo después de que se haya mostrado y tenga dimensiones
        Platform.runLater(() -> {
            centrarDialogoEnVentanaPadre(dialog, parentStage);
        });
        
        // Limpiar los listeners cuando se cierre el diálogo
        dialog.setOnHidden(e -> {
            parentStage.xProperty().removeListener(xListener);
            parentStage.yProperty().removeListener(yListener);
        });
    }

    private void iniciarSimulacionFinal() {
        // Limpiar el historial y estados anteriores
        historialObservable.clear();
        estadosAnteriores.clear();
        pasoActual = 0;
        seHaRealizadoAlMenosUnPaso = false;
        estadoFinalAlcanzado = false;

        // Limpiar el historial (las áreas de texto individuales se han eliminado)

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

        // Guardar el estado inicial antes de comenzar la simulación
        estadosAnteriores.add(new EstadoSimulacion(pilaSimulacion, entradaSimulacion, bundle.getString("simulacionfinal.accion.inicio")));

        // Iniciar la simulación
        simulacionEnCurso = true;
        // El botón Iniciar siempre debe estar activo
        btnIniciar.setDisable(false);
        btnPaso.setDisable(false);
        btnFinal.setDisable(false);
        campoEntrada.setDisable(true);
        btnEditarCadena.setDisable(false);
        btnGenerarInforme.setDisable(true); // Deshabilitar botón al iniciar nueva simulación

        // Actualizar estado de botones de navegación e informe (deben estar deshabilitados al inicio)
        actualizarEstadoBotonesNavegacion();
        actualizarEstadoBotonInforme();

        // Actualizar la vista
        actualizarVista();

        // Resetear las pestañas hijas al estado inicial si están activas
        if (derivacionTab != null && tabPane.getTabs().contains(derivacionTab)) {
            VBox mainContainer = (VBox) derivacionTab.getContent();
            VBox contentContainer = (VBox) mainContainer.getChildren().get(1); // El segundo elemento es el contentContainer
            ListView<String> listaDerivacion = null;
            if (contentContainer.getChildren().get(1) instanceof ListView) {
                @SuppressWarnings("unchecked")
                ListView<String> tempList = (ListView<String>) contentContainer.getChildren().get(1);
                listaDerivacion = tempList;
            }

            if (listaDerivacion != null) {
                // Resetear la lista con solo el símbolo inicial
                ObservableList<String> itemsDerivacion = FXCollections.observableArrayList();
                itemsDerivacion.add(bundle.getString("simulacionfinal.derivacion.iniciar"));
                listaDerivacion.setItems(itemsDerivacion);
            }
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
                
                // Obtener el contenedor VBox existente y navegar a través de la jerarquía
                VBox mainContainer = (VBox) arbolTab.getContent();

                // La estructura es: mainContainer -> [headerLabel, contentContainer]
                // contentContainer -> [descriptionLabel, scrollPane, controlsContainer]
                if (mainContainer.getChildren().size() > 1) {
                    VBox contentContainer = (VBox) mainContainer.getChildren().get(1); // contentContainer

                    if (contentContainer.getChildren().size() > 1) {
                        ScrollPane scrollPane = (ScrollPane) contentContainer.getChildren().get(1); // scrollPane

                        // Actualizar el contenido del ScrollPane
                        scrollPane.setContent(imageView);

                        // Limpiar archivos temporales
                        java.nio.file.Files.deleteIfExists(dotFile);
                        java.nio.file.Files.deleteIfExists(imgFile);
                    } else {
                        System.err.println("Error: No se encontró el ScrollPane en la estructura esperada");
                    }
                } else {
                    System.err.println("Error: Estructura del contenedor no es la esperada");
                }
                
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
        
        // Aplicar estilos modernos al diálogo
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/vistas/styles2.css").toExternalForm());
        dialogPane.getStyleClass().add("wizard-step");
        
        alert.showAndWait();
    }

    private void avanzarPaso() {
        if (!simulacionEnCurso) return;
        if (pilaSimulacion.isEmpty() || entradaSimulacion.isEmpty()) return;

        // Guardar estado actual antes de modificarlo
        estadosAnteriores.add(new EstadoSimulacion(pilaSimulacion, entradaSimulacion, ""));

        // Marcar que se ha realizado al menos un paso
        seHaRealizadoAlMenosUnPaso = true;

        // Actualizar estado de botones de navegación después de guardar el estado
        actualizarEstadoBotonesNavegacion();

        String cimaPila = pilaSimulacion.peek();
        String simboloEntrada = entradaSimulacion.get(0);
        String accionRealizada = "";

        // Caso de aceptación
        if (cimaPila.equals("$") && simboloEntrada.equals("$")) {
            accionRealizada = bundle.getString("simulacionfinal.accion.aceptar");
            simulacionEnCurso = false;
            estadoFinalAlcanzado = true;
            btnPaso.setDisable(true);
            btnFinal.setDisable(true);
            actualizarEstadoBotonInforme(); // Actualizar botón de informe
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
            accionRealizada = bundle.getString("simulacionfinal.accion.emparejar");
        } else if (esTerminal(cimaPila)) {
            // Error: terminal en pila distinto de entrada
            accionRealizada = bundle.getString("simulacionfinal.accion.error");
            simulacionEnCurso = false;
            estadoFinalAlcanzado = true;
            btnPaso.setDisable(true);
            btnFinal.setDisable(true);
            actualizarEstadoBotonInforme(); // Actualizar botón de informe
            pasoActual++;
            agregarPasoHistorial(accionRealizada);
            actualizarVista();
            actualizarPestañasHijas();
            return;
        } else {
            // Buscar producción o función de error en la tabla predictiva
            String accion = buscarAccionTabla(cimaPila, simboloEntrada);
            if (accion == null || accion.isEmpty()) {
                accionRealizada = bundle.getString("simulacionfinal.accion.error");
                simulacionEnCurso = false;
                estadoFinalAlcanzado = true;
                btnPaso.setDisable(true);
                btnFinal.setDisable(true);
                actualizarEstadoBotonInforme(); // Actualizar botón de informe
                pasoActual++;
                agregarPasoHistorial(accionRealizada);
                actualizarVista();
                actualizarPestañasHijas();
                return;
            }
            if (accion.startsWith("E")) {
                accionRealizada = accion;
            } else if (accion.equals("ε") || accion.equals("ε_")) {
                pilaSimulacion.pop();
                accionRealizada = cimaPila + " → ε";
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
                } else {
                    accionRealizada = bundle.getString("simulacionfinal.accion.error");
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
        if (!seHaRealizadoAlMenosUnPaso || estadosAnteriores.size() < 1) {
            // Ya estamos en el inicio
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

        // Actualizar la vista (solo la tabla de historial)
        actualizarVista();

        // Limpiar el historial
        historialObservable.clear();
        pasoActual = 0;

        // Marcar que ya no se ha realizado ningún paso
        seHaRealizadoAlMenosUnPaso = false;

        // Al retroceder al inicio, ya no estamos en un estado final
        estadoFinalAlcanzado = false;

        // Reactivar completamente la simulación
        simulacionEnCurso = true;

        // Rehabilitar TODOS los botones de navegación de avance
        btnPaso.setDisable(false);
        btnFinal.setDisable(false);

        // Actualizar estado de botones de navegación y de informe
        actualizarEstadoBotonesNavegacion();
        actualizarEstadoBotonInforme();
        actualizarPestañasHijas();
    }

    private void retrocederPaso() {
        if (!seHaRealizadoAlMenosUnPaso || estadosAnteriores.size() < 1) {
            // No hay pasos anteriores para retroceder
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
        
        // Actualizar la vista (solo la tabla de historial)
        actualizarVista();
        
        // Actualizar el historial
        historialObservable.remove(historialObservable.size() - 1);
        pasoActual--;

        // Al retroceder, ya no estamos en un estado final
        estadoFinalAlcanzado = false;

        // Reactivar la simulación ya que estamos retrocediendo
        simulacionEnCurso = true;

        // Rehabilitar los botones de navegación AVANCE (paso y final)
        btnPaso.setDisable(false);
        btnFinal.setDisable(false);

        // Si después de retroceder solo queda el estado inicial, marcar que no se ha realizado ningún paso
        if (estadosAnteriores.size() == 1) {
            seHaRealizadoAlMenosUnPaso = false;
        }

        // Actualizar estado de botones de navegación y de informe
        actualizarEstadoBotonesNavegacion();
        actualizarEstadoBotonInforme();
        actualizarPestañasHijas();
    }

    private void actualizarVista() {
        // Ya no necesitamos actualizar áreas de texto individuales
        // Solo la tabla de historial se actualiza automáticamente
    }

    /**
     * Centra el diálogo respecto a la ventana padre
     */
    private void centrarDialogoEnVentanaPadre(Stage dialog, Stage parentStage) {
        if (dialog != null && parentStage != null) {
            // Obtener las dimensiones de la pantalla
            javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
            javafx.geometry.Rectangle2D screenBounds = screen.getVisualBounds();
            
            // Calcular la posición centrada
            double centerX = parentStage.getX() + (parentStage.getWidth() / 2) - (dialog.getWidth() / 2);
            double centerY = parentStage.getY() + (parentStage.getHeight() / 2) - (dialog.getHeight() / 2);
            
            // Asegurar que el diálogo no se salga de la pantalla
            centerX = Math.max(screenBounds.getMinX(), Math.min(centerX, screenBounds.getMaxX() - dialog.getWidth()));
            centerY = Math.max(screenBounds.getMinY(), Math.min(centerY, screenBounds.getMaxY() - dialog.getHeight()));
            
            // Aplicar la posición
            dialog.setX(centerX);
            dialog.setY(centerY);
        }
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
        
        // Crear contenedor principal con estilo moderno
        VBox mainContainer = new VBox(15);
        mainContainer.setAlignment(Pos.TOP_CENTER);
        mainContainer.setPadding(new Insets(20, 40, 20, 40));
        mainContainer.getStyleClass().addAll("wizard-step", "derivacion-tab");
        
        // Header del título
        Label headerLabel = new Label(bundle.getString("simulacionfinal.derivacion.titulo"));
        headerLabel.getStyleClass().add("wizard-header");
        headerLabel.setAlignment(Pos.CENTER);
        
        // Contenedor del contenido
        VBox contentContainer = new VBox(10);
        contentContainer.getStyleClass().add("wizard-content");
        contentContainer.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(contentContainer, Priority.ALWAYS);
        
        // Label descriptivo
        Label descriptionLabel = new Label(bundle.getString("simulacionfinal.derivacion.descripcion"));
        descriptionLabel.getStyleClass().add("wizard-section-header");
        descriptionLabel.setAlignment(Pos.CENTER);
        
        // Lista de derivación con estilo moderno
        ListView<String> listaDerivacion = new ListView<>();
        listaDerivacion.getStyleClass().add("derivacion-list");
        listaDerivacion.setEditable(false);
        VBox.setVgrow(listaDerivacion, Priority.ALWAYS);
        
        // Generar la derivación como lista
        ObservableList<String> itemsDerivacion = FXCollections.observableArrayList();
        if (historialObservable.isEmpty()) {
            itemsDerivacion.add(bundle.getString("simulacionfinal.derivacion.iniciar"));
        } else {
            for (int i = 0; i < historialObservable.size(); i++) {
                HistorialPaso paso = historialObservable.get(i);
                itemsDerivacion.add(bundle.getString("simulacionfinal.derivacion.paso") + " " + (i + 1) + ": " + paso.getAccion());
            }
        }
        listaDerivacion.setItems(itemsDerivacion);
        
        // Agregar elementos al contenedor de contenido
        contentContainer.getChildren().addAll(descriptionLabel, listaDerivacion);
        
        // Agregar elementos al contenedor principal
        mainContainer.getChildren().addAll(headerLabel, contentContainer);
        
        // Usar TabManager para crear la pestaña como hija de la simulación
        Tab nuevaPestana = TabManager.getOrCreateTab(
            tabPane,
            VBox.class, // Usar VBox como tipo de contenido
            tituloBase,
            mainContainer,
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
        
        // Crear contenedor principal con estilo moderno
        VBox mainContainer = new VBox(15);
        mainContainer.setAlignment(Pos.TOP_CENTER);
        mainContainer.setPadding(new Insets(20, 40, 20, 40));
        mainContainer.getStyleClass().addAll("wizard-step", "arbol-tab");
        
        // Header del título
        Label headerLabel = new Label(bundle.getString("simulacionfinal.arbol.titulo"));
        headerLabel.getStyleClass().add("wizard-header");
        headerLabel.setAlignment(Pos.CENTER);
        
        // Contenedor del contenido
        VBox contentContainer = new VBox(15);
        contentContainer.getStyleClass().add("wizard-content");
        contentContainer.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(contentContainer, Priority.ALWAYS);
        
        // Label descriptivo
        Label descriptionLabel = new Label(bundle.getString("simulacionfinal.arbol.descripcion"));
        descriptionLabel.getStyleClass().add("wizard-section-header");
        descriptionLabel.setAlignment(Pos.CENTER);
        
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
            scrollPane.getStyleClass().add("arbol-scroll-pane");
            VBox.setVgrow(scrollPane, Priority.ALWAYS);
            
            // Contenedor para controles de zoom
            VBox controlsContainer = new VBox(10);
            controlsContainer.getStyleClass().add("arbol-controls");
            controlsContainer.setAlignment(Pos.CENTER);
            
            // Label para el slider
            Label zoomLabel = new Label(bundle.getString("simulacionfinal.arbol.zoom"));
            zoomLabel.getStyleClass().add("wizard-field-label");
            zoomLabel.setAlignment(Pos.CENTER);
            
            // Añadir controles de zoom
            Slider zoomSlider = new Slider(0.5, 2, 1);
            zoomSlider.setShowTickLabels(true);
            zoomSlider.setShowTickMarks(true);
            zoomSlider.getStyleClass().add("arbol-zoom-slider");
            
            // Vincular el zoom del slider con la escala de la imagen
            zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                imageView.setScaleX(newVal.doubleValue());
                imageView.setScaleY(newVal.doubleValue());
            });
            
            // Agregar controles al contenedor
            controlsContainer.getChildren().addAll(zoomLabel, zoomSlider);
            
            // Agregar elementos al contenedor de contenido
            contentContainer.getChildren().addAll(descriptionLabel, scrollPane, controlsContainer);
            
            // Agregar elementos al contenedor principal
            mainContainer.getChildren().addAll(headerLabel, contentContainer);
            
            // Usar TabManager para crear la pestaña como hija de la simulación
            Tab nuevaPestana = TabManager.getOrCreateTab(
                tabPane,
                VBox.class, // Usar VBox como tipo de contenido
                tituloBase,
                mainContainer,
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
            if (tab.getUserData() != null) {
                String userData = tab.getUserData().toString();
                if (userData.equals("derivacion_" + simulacionId)) {
                    derivacionTab = tab;
                    break;
                }
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
            VBox mainContainer = (VBox) derivacionTab.getContent();
            VBox contentContainer = (VBox) mainContainer.getChildren().get(1); // El segundo elemento es el contentContainer
            ListView<String> listaDerivacion = null;
            if (contentContainer.getChildren().get(1) instanceof ListView) {
                @SuppressWarnings("unchecked")
                ListView<String> tempList = (ListView<String>) contentContainer.getChildren().get(1);
                listaDerivacion = tempList;
            }

            if (listaDerivacion != null) {
                // Generar la derivación como lista
                ObservableList<String> itemsDerivacion = FXCollections.observableArrayList();
                                        if (historialObservable.isEmpty()) {
                            itemsDerivacion.add(bundle.getString("simulacionfinal.derivacion.iniciar"));
                        } else {
                                                for (int i = 0; i < historialObservable.size(); i++) {
                                HistorialPaso paso = historialObservable.get(i);
                                itemsDerivacion.add(bundle.getString("simulacionfinal.derivacion.paso") + " " + (i + 1) + ": " + paso.getAccion());
                            }
                }
                listaDerivacion.setItems(itemsDerivacion);
            }
        }
        
        // Seleccionar la pestaña
        if (derivacionTab != null) {
            tabPane.getSelectionModel().select(derivacionTab);
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
        
        arbolTab = null;
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() != null) {
                String userData = tab.getUserData().toString();
                if (userData.equals("arbol_" + simulacionId)) {
                    arbolTab = tab;
                    break;
                }
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
            
            // Obtener el contenedor VBox existente y navegar a través de la jerarquía
            VBox mainContainer = (VBox) arbolTab.getContent();

            // La estructura es: mainContainer -> [headerLabel, contentContainer]
            // contentContainer -> [descriptionLabel, scrollPane, controlsContainer]
            if (mainContainer.getChildren().size() > 1) {
                VBox contentContainer = (VBox) mainContainer.getChildren().get(1); // contentContainer

                if (contentContainer.getChildren().size() > 1) {
                    ScrollPane scrollPane = (ScrollPane) contentContainer.getChildren().get(1); // scrollPane

                    // Actualizar el contenido del ScrollPane
                    scrollPane.setContent(imageView);

                    // Limpiar archivos temporales
                    java.nio.file.Files.deleteIfExists(dotFile);
                    java.nio.file.Files.deleteIfExists(imgFile);
                } else {
                    System.err.println("Error: No se encontró el ScrollPane en la estructura esperada");
                }
            } else {
                System.err.println("Error: Estructura del contenedor no es la esperada");
            }
            
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
        
        // Actualizar textos de los controles que existen en el FXML
        if (labelTitulo != null) {
            labelTitulo.setText(bundle.getString("simulacionfinal.titulo"));
        }
        if (labelEntrada != null) {
            labelEntrada.setText(bundle.getString("simulacionfinal.label.entrada"));
        }
        if (labelHistorial != null) {
            labelHistorial.setText(bundle.getString("simulacionfinal.label.historial"));
        }
        
        // Actualizar textos de los botones
        if (btnIniciar != null) {
            btnIniciar.setText(bundle.getString("simulacionfinal.btn.iniciar.solo"));
        }
        if (btnPaso != null) {
            btnPaso.setText(bundle.getString("simulacionfinal.btn.paso.solo"));
        }
        if (btnFinal != null) {
            btnFinal.setText(bundle.getString("simulacionfinal.btn.final.solo"));
        }
        if (btnRetroceso != null) {
            btnRetroceso.setText(bundle.getString("simulacionfinal.btn.retroceso.solo"));
        }
        if (btnInicio != null) {
            btnInicio.setText(bundle.getString("simulacionfinal.btn.inicio.solo"));
        }
        if (btnEditarCadena != null) {
            btnEditarCadena.setText(bundle.getString("simulacionfinal.btn.editar.solo"));
        }
        if (btnDerivacion != null) {
            btnDerivacion.setText(bundle.getString("simulacionfinal.btn.derivacion.solo"));
        }
        if (btnArbol != null) {
            btnArbol.setText(bundle.getString("simulacionfinal.btn.informe.solo"));
        }
        if (btnGenerarInforme != null) {
            btnGenerarInforme.setText(bundle.getString("simulacionfinal.btn.informe.pdf"));
        }
        
        // Actualizar encabezados de las columnas de la tabla
        if (colPaso != null) {
            colPaso.setText(bundle.getString("simulacionfinal.col.paso"));
        }
        if (colPila != null) {
            colPila.setText(bundle.getString("simulacionfinal.col.pila"));
        }
        if (colEntrada != null) {
            colEntrada.setText(bundle.getString("simulacionfinal.col.entrada"));
        }
        if (colAccion != null) {
            colAccion.setText(bundle.getString("simulacionfinal.col.accion"));
        }
        
        // Actualizar títulos de las pestañas
        actualizarTitulosPestañas();
        
        // Actualizar contenido de las pestañas de derivación y árbol sintáctico
        actualizarContenidoPestañasHijas();
        
        // Actualizar la tabla de historial principal
        actualizarTablaHistorial();
    }

    /**
     * Actualiza el contenido de las pestañas de derivación y árbol sintáctico.
     */
    private void actualizarContenidoPestañasHijas() {
        if (tabPane == null || bundle == null) return;
        
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() != null) {
                String userData = tab.getUserData().toString();
                
                // Actualizar pestaña de derivación
                if (userData.equals("derivacion_" + simulacionId)) {
                    actualizarContenidoPestañaDerivacion(tab);
                }
                // Actualizar pestaña de árbol sintáctico
                else if (userData.equals("arbol_" + simulacionId)) {
                    actualizarContenidoPestañaArbol(tab);
                }
            }
        }
    }
    
    /**
     * Actualiza la tabla de historial principal con los textos del idioma actual.
     */
    private void actualizarTablaHistorial() {
        if (tablaHistorial == null || bundle == null) return;
        
        // Guardar una copia de los datos originales
        List<HistorialPaso> datosOriginales = new ArrayList<>(historialObservable);
        
        // Limpiar la lista observable
        historialObservable.clear();
        
        // Recrear los pasos con las acciones traducidas
        for (HistorialPaso pasoOriginal : datosOriginales) {
            HistorialPaso pasoActualizado = new HistorialPaso(
                pasoOriginal.getPaso(),
                pasoOriginal.getPila(),
                pasoOriginal.getEntrada(),
                traducirAccion(pasoOriginal.getAccion())
            );
            historialObservable.add(pasoActualizado);
        }
    }
    
    /**
     * Traduce una acción del historial al idioma actual.
     */
    private String traducirAccion(String accion) {
        if (bundle == null) return accion;
        
        // Traducir las acciones conocidas - verificar en todos los idiomas posibles
        String[] accionesEspanol = {"Aceptar", "Emparejar", "Error", "Inicio"};
        String[] accionesIngles = {"Accept", "Match", "Error", "Start"};
        String[] accionesFrances = {"Accepter", "Associer", "Erreur", "Début"};
        String[] accionesAleman = {"Akzeptieren", "Abgleichen", "Fehler", "Start"};
        String[] accionesPortugues = {"Aceitar", "Emparelhar", "Erro", "Início"};
        String[] accionesJapones = {"受理", "一致", "エラー", "開始"};
        
        String[][] todasLasAcciones = {
            accionesEspanol, accionesIngles, accionesFrances, 
            accionesAleman, accionesPortugues, accionesJapones
        };
        
        // Buscar la acción en todos los idiomas
        for (String[] acciones : todasLasAcciones) {
            for (int i = 0; i < acciones.length; i++) {
                if (accion.equals(acciones[i])) {
                    // Encontrar la clave correspondiente
                    switch (i) {
                        case 0: return bundle.getString("simulacionfinal.accion.aceptar");
                        case 1: return bundle.getString("simulacionfinal.accion.emparejar");
                        case 2: return bundle.getString("simulacionfinal.accion.error");
                        case 3: return bundle.getString("simulacionfinal.accion.inicio");
                    }
                }
            }
        }
        
        // Si no es una acción conocida, devolver la original
        return accion;
    }

    /**
     * Actualiza el contenido de la pestaña de derivación.
     */
    private void actualizarContenidoPestañaDerivacion(Tab tab) {
        if (tab.getContent() instanceof VBox) {
            VBox mainContainer = (VBox) tab.getContent();
            
            // Actualizar título del header
            if (mainContainer.getChildren().size() > 0 && mainContainer.getChildren().get(0) instanceof Label) {
                Label headerLabel = (Label) mainContainer.getChildren().get(0);
                headerLabel.setText(bundle.getString("simulacionfinal.derivacion.titulo"));
            }
            
            // Actualizar descripción
            if (mainContainer.getChildren().size() > 1 && mainContainer.getChildren().get(1) instanceof VBox) {
                VBox contentContainer = (VBox) mainContainer.getChildren().get(1);
                if (contentContainer.getChildren().size() > 0 && contentContainer.getChildren().get(0) instanceof Label) {
                    Label descriptionLabel = (Label) contentContainer.getChildren().get(0);
                    descriptionLabel.setText(bundle.getString("simulacionfinal.derivacion.descripcion"));
                }
                
                // Actualizar lista de derivación
                if (contentContainer.getChildren().size() > 1 && contentContainer.getChildren().get(1) instanceof ListView) {
                    @SuppressWarnings("unchecked")
                    ListView<String> listaDerivacion = (ListView<String>) contentContainer.getChildren().get(1);
                    actualizarListaDerivacion(listaDerivacion);
                }
            }
        }
    }
    
    /**
     * Actualiza el contenido de la pestaña de árbol sintáctico.
     */
    private void actualizarContenidoPestañaArbol(Tab tab) {
        if (tab.getContent() instanceof VBox) {
            VBox mainContainer = (VBox) tab.getContent();
            
            // Actualizar título del header
            if (mainContainer.getChildren().size() > 0 && mainContainer.getChildren().get(0) instanceof Label) {
                Label headerLabel = (Label) mainContainer.getChildren().get(0);
                headerLabel.setText(bundle.getString("simulacionfinal.arbol.titulo"));
            }
            
            // Actualizar descripción
            if (mainContainer.getChildren().size() > 1 && mainContainer.getChildren().get(1) instanceof VBox) {
                VBox contentContainer = (VBox) mainContainer.getChildren().get(1);
                if (contentContainer.getChildren().size() > 0 && contentContainer.getChildren().get(0) instanceof Label) {
                    Label descriptionLabel = (Label) contentContainer.getChildren().get(0);
                    descriptionLabel.setText(bundle.getString("simulacionfinal.arbol.descripcion"));
                }
                
                // Buscar y actualizar el label de zoom
                actualizarLabelZoom(contentContainer);
            }
        }
    }
    
    /**
     * Busca y actualiza el label de zoom en el contenedor del árbol.
     */
    private void actualizarLabelZoom(VBox contentContainer) {
        // Buscar recursivamente en todos los nodos del contenedor
        buscarYActualizarLabelZoom(contentContainer);
    }
    
    /**
     * Busca recursivamente el label de zoom y lo actualiza.
     */
    private void buscarYActualizarLabelZoom(javafx.scene.Node node) {
        if (node instanceof Label) {
            Label label = (Label) node;
            // Verificar si es el label de zoom (contiene "Zoom" o "Tree Zoom" o "Zoom de l'arbre")
            if (label.getText() != null && 
                (label.getText().contains("Zoom") || 
                 label.getText().contains("Tree Zoom") || 
                 label.getText().contains("Zoom de l'arbre"))) {
                label.setText(bundle.getString("simulacionfinal.arbol.zoom"));
                return;
            }
        } else if (node instanceof javafx.scene.Parent) {
            javafx.scene.Parent parent = (javafx.scene.Parent) node;
            for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
                buscarYActualizarLabelZoom(child);
            }
        }
    }
    
    /**
     * Actualiza la lista de derivación con textos internacionalizados.
     */
    private void actualizarListaDerivacion(ListView<String> listaDerivacion) {
        ObservableList<String> itemsDerivacion = FXCollections.observableArrayList();
        
        if (historialObservable.isEmpty()) {
            itemsDerivacion.add(bundle.getString("simulacionfinal.derivacion.iniciar"));
        } else {
            for (int i = 0; i < historialObservable.size(); i++) {
                HistorialPaso paso = historialObservable.get(i);
                String pasoText = bundle.getString("simulacionfinal.derivacion.paso") + " " + (i + 1) + ": " + paso.getAccion();
                itemsDerivacion.add(pasoText);
            }
        }
        
        listaDerivacion.setItems(itemsDerivacion);
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
                    VBox mainContainer = (VBox) tab.getContent();
                    VBox contentContainer = (VBox) mainContainer.getChildren().get(1); // El segundo elemento es el contentContainer
                    ListView<String> listaDerivacion = null;
                    if (contentContainer.getChildren().get(1) instanceof ListView) {
                        @SuppressWarnings("unchecked")
                        ListView<String> tempList = (ListView<String>) contentContainer.getChildren().get(1);
                        listaDerivacion = tempList;
                    }

                    if (listaDerivacion != null) {
                        // Generar la derivación como lista
                        ObservableList<String> itemsDerivacion = FXCollections.observableArrayList();
                        if (historialObservable.isEmpty()) {
                            itemsDerivacion.add(bundle.getString("simulacionfinal.derivacion.iniciar"));
                        } else {
                            for (int i = 0; i < historialObservable.size(); i++) {
                                HistorialPaso paso = historialObservable.get(i);
                                itemsDerivacion.add(bundle.getString("simulacionfinal.derivacion.paso") + " " + (i + 1) + ": " + paso.getAccion());
                            }
                        }
                        listaDerivacion.setItems(itemsDerivacion);
                    }
                }
                
                // Actualizar pestaña de árbol si está activa
                else if (userData.equals("arbol_" + simulacionId)) {
                    actualizarContenidoArbol(tab);
                }
            }
        }
    }

    @FXML
    private void generarInforme() {
        if (this.gramatica == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(bundle.getString("editor.informe.error.titulo"));
            alert.setHeaderText(null);
            alert.setContentText(bundle.getString("editor.informe.error.sin.gramatica"));
            alert.showAndWait();
            return;
        }

        // Crear y configurar el FileChooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(bundle.getString("editor.informe.guardar.titulo"));
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Documentos PDF", "*.pdf")
        );
        
        // Sugerir nombre de archivo basado en el nombre del archivo fuente
        String nombreArchivo = this.gramatica.generarNombreArchivoPDF("simulacion", this.bundle);
        fileChooser.setInitialFileName(nombreArchivo);

        // Mostrar diálogo de guardado
        File archivo = fileChooser.showSaveDialog(this.getScene().getWindow());
        if (archivo == null) {
            return; // Usuario canceló
        }

        try {
            // Usar la gramática actual para generar el informe
            Gramatica gramaticaOriginal = this.gramatica;
            
            // Determinar el estado de la simulación
            String estadoSimulacion = bundle.getString("informe.simulador.no.especificado");
            if (historialObservable.size() > 0) {
                HistorialPaso ultimoPaso = historialObservable.get(historialObservable.size() - 1);
                if (ultimoPaso.getAccion().equals(bundle.getString("simulacionfinal.accion.aceptar"))) {
                    estadoSimulacion = bundle.getString("informe.simulador.estado.aceptada");
                } else if (ultimoPaso.getAccion().equals(bundle.getString("simulacionfinal.accion.error"))) {
                    estadoSimulacion = bundle.getString("informe.simulador.estado.rechazada");
                }
            }
            
            // Generar el informe del simulador con formato profesional
            boolean exito = this.gramatica.generarInformeSimulacionFinalProfesional(
                archivo.getAbsolutePath(),
                gramaticaOriginal,
                this.tablaPredictiva,
                this.funcionesError,
                bundle,
                campoEntrada.getText(),
                estadoSimulacion,
                new ArrayList<>(historialObservable)
            );
            
            if (exito) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(bundle.getString("editor.informe.exito.titulo"));
                alert.setHeaderText(null);
                alert.setContentText(bundle.getString("editor.informe.exito.mensaje") + "\n" + archivo.getAbsolutePath());
                alert.showAndWait();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(bundle.getString("editor.informe.error.titulo"));
                alert.setHeaderText(null);
                alert.setContentText(bundle.getString("editor.informe.error.generacion"));
                alert.showAndWait();
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(bundle.getString("editor.informe.error.titulo"));
            alert.setHeaderText(null);
            alert.setContentText(bundle.getString("editor.informe.error.generacion") + "\n" + e.getMessage());
            alert.showAndWait();
            e.printStackTrace();
        }
    }
} 