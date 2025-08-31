package bienvenida;

import editor.Editor;
import editor.EditorWindow;
import utils.SecondaryWindow;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;
import utils.TabManager;
import utils.LanguageItem;
import utils.LanguageListCell;
import utils.ActualizableTextos;
import gramatica.Gramatica;
import simulador.PanelSimuladorDesc;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import java.util.Map;

public class MenuPrincipal extends Application {

    @FXML private TabPane tabPane;
    @FXML private Tab mainTab;
    @FXML private Button btnCerrarTabs;
    @FXML private ComboBox<LanguageItem> comboIdioma;
    @FXML private Button btnEditor;
    @FXML private Button btnSalir;
    @FXML private Button btnSimulador;
    @FXML private Button btnAyuda;
    @FXML private Button btnTutorial;
    @FXML private Label labelTitulo;
    @FXML private Label labelSubtitulo;
    @FXML private Label labelDesarrollado;
    private Tab lastSelectedTab;
    private ResourceBundle bundle;
    private Locale currentLocale = new Locale("es");

    @Override
    public void start(Stage primaryStage) {
        try {
            // Cargar el FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/MenuPrincipal.fxml"));
            loader.setController(this);
            Parent root = loader.load();
            
            // Configurar la escena
            Scene scene = new Scene(root);
            primaryStage.setTitle("SimAS 3.0");
            primaryStage.setScene(scene);
            
            // Configurar el tamaño de la ventana
            primaryStage.setWidth(800);
            primaryStage.setHeight(900);
            primaryStage.setMinWidth(600);
            primaryStage.setMinHeight(700);
            
            // Configurar atajos de teclado
            scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN),
                () -> onBtnEditorAction()
            );
            
            scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN),
                () -> onBtnSimuladorAction()
            );
            
            scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.H, KeyCombination.SHORTCUT_DOWN),
                () -> onBtnAyudaAction()
            );
            
            scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.T, KeyCombination.SHORTCUT_DOWN),
                () -> onBtnTutorialAction()
            );
            
            scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN),
                () -> onBtnSalirAction()
            );
            
            scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN),
                () -> {
                    if (tabPane != null) {
                        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
                        if (selectedTab != null && selectedTab.isClosable()) {
                            
                            // Get the tab's userData (which contains the editor/simulator ID)
                            String elementId = selectedTab.getUserData() != null ? selectedTab.getUserData().toString() : null;
                            
                            // Close child tabs first if this is a parent tab
                            if (elementId != null) {
                                TabManager.closeChildTabs(tabPane, elementId);
                                
                                // Get the group ID before removing the tab
                                String grupoId = TabManager.obtenerGrupoDeElemento(tabPane, elementId);
                                
                                // Remove the tab
                                tabPane.getTabs().remove(selectedTab);
                                
                                // Clean up the element from group management
                                TabManager.eliminarElementoDeGrupo(tabPane, elementId, grupoId);
                                
                                // Force immediate renumbering
                                TabManager.reasignarNumerosGruposGramatica(tabPane);
                            } else {
                                // For non-group tabs, just remove them
                                tabPane.getTabs().remove(selectedTab);
                            }
                        }
                    }
                }
            );
            
            // Add Ctrl/Cmd+Shift+W shortcut to close all tabs
            scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
                () -> onBtnCerrarTabsAction()
            );
            
            // Add shortcuts for Cmd/Ctrl + number (1-9)
            KeyCode[] numberKeys = {
                KeyCode.DIGIT0, // Add 0 for main menu
                KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3, KeyCode.DIGIT4, KeyCode.DIGIT5,
                KeyCode.DIGIT6, KeyCode.DIGIT7, KeyCode.DIGIT8, KeyCode.DIGIT9
            };
            
            for (int i = 0; i < numberKeys.length; i++) {
                final int groupNumber = i;  // Now starts from 0
                scene.getAccelerators().put(
                    new KeyCodeCombination(numberKeys[i], KeyCombination.SHORTCUT_DOWN),
                    () -> {
                        if (tabPane != null) {
                            if (groupNumber == 0) {
                                // For 0, select the main menu (first tab)
                                tabPane.getSelectionModel().selectFirst();
                            } else {
                                // For 1-9, find the first tab of the specified group
                                Tab firstGroupTab = findFirstTabInGroup(tabPane, groupNumber);
                                if (firstGroupTab != null) {
                                    tabPane.getSelectionModel().select(firstGroupTab);
                                }
                            }
                        }
                    }
                );
            }
            
            // Enable tab dragging and detaching
            tabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
            
            // Configurar el menú contextual para las pestañas
            TabManager.configurarMenuContextual(tabPane, bundle);
            
            // Setup drag and drop handling for tabs
            tabPane.setOnDragDetected(event -> {
                if (event.isShortcutDown()) {  // Ctrl/Cmd is pressed
                    Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
                    if (selectedTab != null && selectedTab.isClosable()) {
                        // Start drag operation
                        javafx.scene.input.Dragboard db = tabPane.startDragAndDrop(javafx.scene.input.TransferMode.MOVE);
                        
                        // Put a string on dragboard (needed for the drag operation)
                        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                        content.putString("tab-transfer");
                        db.setContent(content);
                        
                        // Store the tab temporarily
                        event.consume();
                        
                        // Create new window
                        EditorWindow newWindow = new SecondaryWindow(bundle, "SimAS 3.0");
                        
                        // Encontrar el grupo al que pertenece la pestaña
                        String grupoId = null;
                        if (selectedTab.getUserData() != null) {
                            String elementId = selectedTab.getUserData().toString();
                            
                            // Si es una pestaña de simulación, buscar su simulador padre
                            if (selectedTab.getContent() instanceof simulador.SimulacionFinal) {
                                simulador.SimulacionFinal sim = (simulador.SimulacionFinal) selectedTab.getContent();
                                if (sim.perteneceASimulador(sim.getSimuladorPadreId())) {
                                    grupoId = TabManager.obtenerGrupoDeElemento(tabPane, sim.getSimuladorPadreId());
                                }
                            } else {
                                // Para otros tipos de pestañas
                                String potentialParentId = elementId; // Inicializar con el ID actual
                                
                                // Si es una pestaña hija, buscar el ID del padre
                                if (elementId.contains("_")) {
                                    if (elementId.startsWith("gramatica_simulador_") || 
                                        elementId.startsWith("funciones_error_simulador_")) {
                                        // Para pestañas hijas de simulador
                                        potentialParentId = elementId.substring(elementId.indexOf("simulador_"));
                                    } else if (elementId.startsWith("creacion_")) {
                                        // Para pestañas de creación
                                        potentialParentId = "editor_" + elementId.substring("creacion_".length());
                                    } else if (elementId.startsWith("terminales_") || 
                                             elementId.startsWith("no_terminales_") || 
                                             elementId.startsWith("producciones_")) {
                                        // Para pestañas de símbolos
                                        String creacionId = elementId.substring(elementId.indexOf("creacion_"));
                                        potentialParentId = "editor_" + creacionId.substring("creacion_".length());
                                    } else if (elementId.startsWith("derivacion_") || elementId.startsWith("arbol_")) {
                                        // Buscar la simulación padre
                                        for (Tab tab : tabPane.getTabs()) {
                                            if (tab.getContent() instanceof simulador.SimulacionFinal) {
                                                simulador.SimulacionFinal sim = (simulador.SimulacionFinal) tab.getContent();
                                                if (sim.esHijaDeLaSimulacion(selectedTab)) {
                                                    // Encontramos la simulación padre, ahora buscar su simulador padre
                                                    if (sim.perteneceASimulador(sim.getSimuladorPadreId())) {
                                                        potentialParentId = sim.getSimuladorPadreId();
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                // Obtener el grupo usando el ID del padre
                                grupoId = TabManager.obtenerGrupoDeElemento(tabPane, potentialParentId);
                            }
                        }
                        
                        // Si la pestaña pertenece a un grupo, mover todo el grupo
                        if (grupoId != null) {
                            newWindow.moveGroupToWindow(tabPane, grupoId, selectedTab);
                        } else {
                            // Si no pertenece a un grupo, mover solo la pestaña
                            newWindow.addTab(selectedTab);
                            tabPane.getTabs().remove(selectedTab);
                        }
                        
                        // Show the new window at the cursor position
                        newWindow.show();
                        Stage stage = (Stage) newWindow.getTabPane().getScene().getWindow();
                        stage.setX(event.getScreenX() - 100);
                        stage.setY(event.getScreenY() - 50);
                    }
                }
            });
            
            primaryStage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void initialize() {
        // Inicializar idiomas con banderas
        comboIdioma.getItems().addAll(
            new LanguageItem("Español", "es", "espana.png"),
            new LanguageItem("English", "en", "england.png"),
            new LanguageItem("Français", "fr", "francia.png"),
            new LanguageItem("Português", "pt", "portugal.png"),
            new LanguageItem("Deutsch", "de", "alemania.png"),
            new LanguageItem("日本語", "ja", "japon.png")
        );
        
        // Configurar la celda personalizada para mostrar banderas
        comboIdioma.setCellFactory(param -> new LanguageListCell());
        comboIdioma.setButtonCell(new LanguageListCell());
        
        // Establecer el idioma por defecto (Español)
        comboIdioma.setValue(comboIdioma.getItems().get(0));
        comboIdioma.setOnAction(e -> cambiarIdioma());
        cargarBundle(currentLocale);
        
        // Enable tab dragging and detaching
        tabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        
        // Configurar el menú contextual para las pestañas
        TabManager.configurarMenuContextual(tabPane, bundle);
        
        // Setup drag and drop handling for tabs
        tabPane.setOnDragDetected(event -> {
            if (event.isShortcutDown()) {  // Ctrl/Cmd is pressed
                Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
                if (selectedTab != null && selectedTab.isClosable()) {
                    // Start drag operation
                    javafx.scene.input.Dragboard db = tabPane.startDragAndDrop(javafx.scene.input.TransferMode.MOVE);
                    
                    // Put a string on dragboard (needed for the drag operation)
                    javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                    content.putString("tab-transfer");
                    db.setContent(content);
                    
                    // Store the tab temporarily
                    event.consume();
                    
                    // Create new window
                    EditorWindow newWindow = new SecondaryWindow(bundle, "SimAS 3.0");
                    
                    // Encontrar el grupo al que pertenece la pestaña
                    String grupoId = null;
                    if (selectedTab.getUserData() != null) {
                        String elementId = selectedTab.getUserData().toString();
                        
                        // Si es una pestaña de simulación, buscar su simulador padre
                        if (selectedTab.getContent() instanceof simulador.SimulacionFinal) {
                            simulador.SimulacionFinal sim = (simulador.SimulacionFinal) selectedTab.getContent();
                            if (sim.perteneceASimulador(sim.getSimuladorPadreId())) {
                                grupoId = TabManager.obtenerGrupoDeElemento(tabPane, sim.getSimuladorPadreId());
                            }
                        } else {
                            // Para otros tipos de pestañas
                            String potentialParentId = elementId; // Inicializar con el ID actual
                            
                            // Si es una pestaña hija, buscar el ID del padre
                            if (elementId.contains("_")) {
                                if (elementId.startsWith("gramatica_simulador_") || 
                                    elementId.startsWith("funciones_error_simulador_")) {
                                    // Para pestañas hijas de simulador
                                    potentialParentId = elementId.substring(elementId.indexOf("simulador_"));
                                } else if (elementId.startsWith("creacion_")) {
                                    // Para pestañas de creación
                                    potentialParentId = "editor_" + elementId.substring("creacion_".length());
                                } else if (elementId.startsWith("terminales_") || 
                                         elementId.startsWith("no_terminales_") || 
                                         elementId.startsWith("producciones_")) {
                                    // Para pestañas de símbolos
                                    String creacionId = elementId.substring(elementId.indexOf("creacion_"));
                                    potentialParentId = "editor_" + creacionId.substring("creacion_".length());
                                } else if (elementId.startsWith("derivacion_") || elementId.startsWith("arbol_")) {
                                    // Buscar la simulación padre
                                    for (Tab tab : tabPane.getTabs()) {
                                        if (tab.getContent() instanceof simulador.SimulacionFinal) {
                                            simulador.SimulacionFinal sim = (simulador.SimulacionFinal) tab.getContent();
                                            if (sim.esHijaDeLaSimulacion(selectedTab)) {
                                                // Encontramos la simulación padre, ahora buscar su simulador padre
                                                if (sim.perteneceASimulador(sim.getSimuladorPadreId())) {
                                                    potentialParentId = sim.getSimuladorPadreId();
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Obtener el grupo usando el ID del padre
                            grupoId = TabManager.obtenerGrupoDeElemento(tabPane, potentialParentId);
                        }
                    }
                    
                    // Si la pestaña pertenece a un grupo, mover todo el grupo
                    if (grupoId != null) {
                        newWindow.moveGroupToWindow(tabPane, grupoId, selectedTab);
                    } else {
                        // Si no pertenece a un grupo, mover solo la pestaña
                        newWindow.addTab(selectedTab);
                        tabPane.getTabs().remove(selectedTab);
                    }
                    
                    // Show the new window at the cursor position
                    newWindow.show();
                    Stage stage = (Stage) newWindow.getTabPane().getScene().getWindow();
                    stage.setX(event.getScreenX() - 100);
                    stage.setY(event.getScreenY() - 50);
                }
            }
        });
        
        // Actualizar el título inicial de la pestaña principal
        if (mainTab != null) {
            mainTab.setText(bundle.getString("title.menu"));
        }
        
        // Guardar la última pestaña seleccionada
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                lastSelectedTab = newTab;
            }
        });

        // Añadir listener para detectar cuando se cierran pestañas
        tabPane.getTabs().addListener((javafx.collections.ListChangeListener.Change<? extends Tab> change) -> {
            while (change.next()) {
                if (change.wasRemoved()) {
                    // Forzar renumeración de grupos cuando se cierra una pestaña
                    TabManager.reasignarNumerosGruposGramatica(tabPane);
                }
            }
        });
    }

    private void cambiarIdioma() {
        LanguageItem selectedLanguage = comboIdioma.getValue();
        if (selectedLanguage != null) {
            currentLocale = new Locale(selectedLanguage.getLocale());
            cargarBundle(currentLocale);
            actualizarTextos();
        }
    }

    private void cargarBundle(Locale locale) {
        bundle = ResourceBundle.getBundle("utils.messages", locale);
    }

    private void actualizarTextos() {
        try {
            // Actualizar ResourceBundle en TabManager
            TabManager.setResourceBundle(tabPane, bundle);
            
            // Actualizar textos de los botones principales
            if (btnEditor != null) btnEditor.setText(bundle.getString("btn.editor"));
            if (btnSalir != null) btnSalir.setText(bundle.getString("btn.salir"));
            if (btnSimulador != null) btnSimulador.setText(bundle.getString("btn.simulador"));
            if (btnAyuda != null) btnAyuda.setText(bundle.getString("btn.ayuda"));
            if (btnTutorial != null) btnTutorial.setText(bundle.getString("btn.tutorial"));
            if (btnCerrarTabs != null) {
                btnCerrarTabs.setText("✖");
                btnCerrarTabs.getTooltip().setText(bundle.getString("tooltip.cerrar"));
            }
            
            // Actualizar textos de las etiquetas
            if (comboIdioma != null && comboIdioma.getParent() != null) {
                Label labelIdioma = (Label) comboIdioma.getParent().getChildrenUnmodifiable().get(0);
                labelIdioma.setText(bundle.getString("label.idioma"));
            }
            if (labelTitulo != null) labelTitulo.setText(bundle.getString("label.titulo"));
            if (labelSubtitulo != null) labelSubtitulo.setText(bundle.getString("label.subtitulo"));
            if (labelDesarrollado != null) labelDesarrollado.setText(bundle.getString("label.desarrollado"));
            
            // Actualizar el título de la pestaña principal
            if (mainTab != null) {
                mainTab.setText(bundle.getString("title.menu"));
            }
            
            // Actualizar textos de las pestañas y contenido
            for (Tab tab : tabPane.getTabs()) {
                            if (tab.getContent() instanceof ActualizableTextos) {
                ((ActualizableTextos) tab.getContent()).actualizarTextos(bundle);
                }
            }
            
            // Actualizar títulos de pestañas
            TabManager.actualizarTitulosPestañas(tabPane);
            
            // SINCRONIZAR IDIOMA CON TODAS LAS VENTANAS SECUNDARIAS
            sincronizarIdiomaConVentanasSecundarias();
            
            // ACTUALIZAR TODOS LOS SIMULADORES ACTIVOS
            actualizarTodosLosSimuladores();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Sincroniza el idioma actual con todas las ventanas secundarias activas
     */
    private void sincronizarIdiomaConVentanasSecundarias() {
        Map<String, SecondaryWindow> activeWindows = SecondaryWindow.getActiveWindows();
        
        for (SecondaryWindow window : activeWindows.values()) {
            if (window != null && window.getStage() != null && window.getStage().isShowing()) {
                // Actualizar el ResourceBundle de la ventana secundaria
                window.actualizarIdioma(bundle);
            }
        }
    }

    /**
     * Actualiza el bundle de todos los simuladores activos
     */
    private void actualizarTodosLosSimuladores() {
        // Usar el nuevo método estático para actualizar todos los simuladores
        simulador.PanelSimuladorDesc.actualizarTodosLosSimuladores(bundle);
    }

    @FXML
    private void onBtnCerrarTabsAction() {
        ButtonType btnCerrar = new ButtonType(bundle.getString("btn.cerrar"), ButtonBar.ButtonData.YES);
        ButtonType btnCancelar = new ButtonType(bundle.getString("button.cancelar"), ButtonBar.ButtonData.NO);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                bundle.getString("msg.confirmar.cerrar"),
                btnCerrar, btnCancelar);
        confirm.setTitle(bundle.getString("title.cerrar.pestanas"));
        confirm.setHeaderText(bundle.getString("title.cerrar.pestanas"));
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == btnCerrar) {
                tabPane.getSelectionModel().selectFirst();
                tabPane.getTabs().removeIf(Tab::isClosable);
                // Reiniciar la numeración de grupos
                TabManager.resetGrupos(tabPane);
            } else {
                if (lastSelectedTab != null) {
                    tabPane.getSelectionModel().select(lastSelectedTab);
                }
            }
        });
    }

    @FXML
    private void onBtnEditorAction() {
        // Establecer el ResourceBundle en TabManager para internacionalización
        TabManager.setResourceBundle(tabPane, bundle);
        
        // Crear un nuevo editor usando TabManager para posicionamiento correcto
        Editor editor = new Editor(tabPane, this, bundle);
        
        // CREAR NUEVO GRUPO: Editor independiente desde menú principal
        // parentId = editorId, childId = null → Esto creará un NUEVO GRUPO automáticamente
        Tab editorTab = TabManager.getOrCreateTab(tabPane, Editor.class, 
            bundle.getString("editor.title"), editor, editor.getEditorId(), null);
        
        // Asegurar que el editorId esté configurado como userData
        editorTab.setUserData(editor.getEditorId());
        
        // Reasignar numeración para reflejar los cambios
        TabManager.reasignarNumerosGruposGramatica(tabPane);
    }

    @FXML
    private void onBtnAyudaAction() {
        File manual = new File("ManualDeUsuario.pdf");
        if (manual.exists()) {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(manual);
                } else {
                    onMostrarErrorAction(bundle.getString("msg.error.escritorio"));
                }
            } catch (IOException e) {
                onMostrarErrorAction(bundle.getString("msg.error.manual") + ": " + e.getMessage());
            }
        } else {
            onMostrarErrorAction(bundle.getString("msg.error.archivo"));
        }
    }

    @FXML
    private void onBtnTutorialAction() {
        File tutorial = new File("src/centroayuda/SimAS.html");
        if (tutorial.exists()) {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(tutorial.toURI());
                } else {
                    onMostrarErrorAction(bundle.getString("msg.error.escritorio"));
                }
            } catch (IOException e) {
                onMostrarErrorAction(bundle.getString("msg.error.tutorial") + ": " + e.getMessage());
            }
        } else {
            onMostrarErrorAction(bundle.getString("msg.error.archivo"));
        }
    }

    @FXML
    private void onBtnSimuladorAction() {
        // Implementar funcionalidad del simulador descendente directo
        cargarGramaticaYSimularDirectamente();
    }
    
    /**
     * Carga una gramática desde archivo y va directamente al paso 6 de la simulación.
     */
    private void cargarGramaticaYSimularDirectamente() {
        try {
            // Crear una nueva gramática
            Gramatica nuevaGramatica = new Gramatica();
            
            // Cargar gramática desde archivo (esto abrirá el selector de archivos)
            Gramatica gramaticaCargada = nuevaGramatica.cargarGramatica(null);
            
            if (gramaticaCargada != null) {
                // Validar la gramática cargada
                javafx.collections.ObservableList<String> errores = gramaticaCargada.validarGramatica();
                
                if (gramaticaCargada.getEstado() == 1) {
                    // Gramática válida - proceder con la simulación
                    crearSimuladorDirectoAlPaso6(gramaticaCargada);
                } else {
                    // Gramática inválida - mostrar errores
                    mostrarErroresValidacion(errores);
                }
            }
            // Si gramaticaCargada es null, significa que el usuario canceló la selección
            
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error", "No se pudo cargar la gramática: " + e.getMessage());
        }
    }
    
    /**
     * Crea un simulador y lo lleva directamente al paso 6.
     */
    private void crearSimuladorDirectoAlPaso6(Gramatica gramatica) {
        try {
            // Establecer el ResourceBundle en TabManager para internacionalización
            TabManager.setResourceBundle(tabPane, bundle);
            
            // Generar ID único para el simulador
            String simuladorId = "simulador_" + System.currentTimeMillis();
            
            // ASIGNAR A NUEVO GRUPO ANTES de crear la pestaña: Simulador independiente desde menú principal
            // Esto asegura que la numeración y posicionamiento sean correctos
            TabManager.asignarElementoANuevoGrupo(tabPane, simuladorId);
            
            // Crear una copia de la gramática para el simulador (para no modificar la original)
            Gramatica gramaticaParaSimulador = new Gramatica(gramatica);

            // Crear el simulador descendente con la copia de la gramática
            PanelSimuladorDesc simulador = new PanelSimuladorDesc(gramaticaParaSimulador, tabPane, bundle, simuladorId);
            
            // Crear la pestaña del simulador con el título correcto
            // Como vamos directamente al paso 6, usamos el título de simulador
            String tituloBase = bundle.getString("simulador.tab.paso6");
            int numeroGrupo = TabManager.obtenerNumeroGrupo(tabPane, simuladorId);
            String tituloFinal = numeroGrupo > 0 ? numeroGrupo + "-" + tituloBase : tituloBase;
            
            // Crear la pestaña usando TabManager
            Tab pestañaSimulador = TabManager.getOrCreateTab(
                tabPane,
                PanelSimuladorDesc.class,
                tituloFinal,
                simulador,
                simuladorId,
                null
            );
            
            // Establecer la pestaña en el simulador
            simulador.setPestañaSimulacion(pestañaSimulador);
            
            // Asegurarse de que la pestaña esté seleccionada
            tabPane.getSelectionModel().select(pestañaSimulador);
            
            // Ahora sí, saltar directamente al paso 6 (índice 5)
            simulador.cambiarPaso(5);
            
            // Reasignar numeración para reflejar los cambios
            TabManager.reasignarNumerosGruposGramatica(tabPane);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Muestra los errores de validación de la gramática.
     */
    private void mostrarErroresValidacion(javafx.collections.ObservableList<String> errores) {
        StringBuilder mensaje = new StringBuilder(bundle.getString("editor.msg.validar.errores") + "\n\n");
        for (int i = 0; i < errores.size(); i++) {
            mensaje.append(i + 1).append(". ").append(errores.get(i)).append("\n\n");
        }
        
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error de Validación");
        alert.setHeaderText("La gramática seleccionada contiene errores");
        alert.setContentText(mensaje.toString());
        
        // Expandir el diálogo para mostrar todo el texto
        TextArea textArea = new TextArea(mensaje.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        javafx.scene.layout.GridPane.setVgrow(textArea, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.GridPane.setHgrow(textArea, javafx.scene.layout.Priority.ALWAYS);
        
        javafx.scene.layout.GridPane gridPane = new javafx.scene.layout.GridPane();
        gridPane.setMaxWidth(Double.MAX_VALUE);
        gridPane.add(textArea, 0, 0);
        
        alert.getDialogPane().setExpandableContent(gridPane);
        alert.showAndWait();
    }
    
    /**
     * Muestra un mensaje de error simple.
     */
    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    @FXML
    private void onMostrarErrorAction(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR, mensaje, ButtonType.OK);
        alerta.setTitle("Error");
        alerta.setHeaderText("Error al abrir el archivo");
        alerta.showAndWait();
    }

    @FXML
    private void onBtnSalirAction() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                bundle.getString("msg.confirmar.salir"),
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle(bundle.getString("title.menu"));
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                System.exit(0);
            }
        });
    }

    @FXML
    private void onBtnInfoAction() {
        Alert acercaDe = new Alert(Alert.AlertType.INFORMATION,
                "SimAS 3.0\nDesarrollado por Antonio.",
                ButtonType.OK);
        acercaDe.setTitle("Acerca de");
        acercaDe.showAndWait();
    }

    public void setBundle(ResourceBundle bundle) {
        this.bundle = bundle;
        actualizarTextos(bundle);
    }

    private void actualizarTextos(ResourceBundle bundle) {
        if (labelTitulo != null) labelTitulo.setText(bundle.getString("label.titulo"));
        if (labelSubtitulo != null) labelSubtitulo.setText(bundle.getString("label.subtitulo"));
        if (labelDesarrollado != null) labelDesarrollado.setText(bundle.getString("label.desarrollado"));
        if (btnEditor != null) btnEditor.setText(bundle.getString("btn.editor"));
        if (btnSimulador != null) btnSimulador.setText(bundle.getString("btn.simulador"));
        if (btnAyuda != null) btnAyuda.setText(bundle.getString("btn.ayuda"));
        if (btnTutorial != null) btnTutorial.setText(bundle.getString("btn.tutorial"));
        if (btnSalir != null) btnSalir.setText(bundle.getString("btn.salir"));
        if (btnCerrarTabs != null) btnCerrarTabs.setText(bundle.getString("btn.cerrar"));
    }

    public static void reasignarNumerosSimulaciones(TabPane tabPane) {
        if (tabPane == null) return;
        // ... (resto del código igual, pero usando el tabPane recibido)
    }

    /**
     * Finds the first tab belonging to the specified group number.
     */
    private Tab findFirstTabInGroup(TabPane tabPane, int groupNumber) {
        // First, find all tabs in the specified group
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() != null) {
                String elementId = tab.getUserData().toString();
                // Get the group number for this tab
                int tabGroupNumber = TabManager.obtenerNumeroGrupo(tabPane, elementId);
                if (tabGroupNumber == groupNumber) {
                    return tab;
                }
            }
        }
        return null;
    }
}
