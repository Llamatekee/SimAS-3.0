package utils;

import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.TransferMode;
import java.util.ResourceBundle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javafx.application.Platform;
import java.util.Set;
import java.util.HashSet;
import java.util.Comparator;
import gramatica.Gramatica;
import simulador.PanelSimuladorDesc;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import editor.EditorWindow;
import editor.Editor;

public class SecondaryWindow extends EditorWindow {
    
    private static final Map<String, SecondaryWindow> activeWindows = new ConcurrentHashMap<>();
    private final String windowId;
    private final TabPane localTabPane;
    private final Stage stage;
    private ResourceBundle bundle;
    private int windowNumber;
    
    static {
    }
    
    /**
     * Obtiene una copia del mapa de ventanas secundarias activas.
     * @return Un mapa con las ventanas secundarias activas, donde la clave es el ID de la ventana
     */
    public static Map<String, SecondaryWindow> getActiveWindows() {
        // Limpiar ventanas que ya no están visibles
        activeWindows.entrySet().removeIf(entry -> {
            SecondaryWindow window = entry.getValue();
            if (window == null || window.getStage() == null || !window.getStage().isShowing()) {
                return true;
            }
            return false;
        });
        
        // Devolver una copia del mapa para evitar modificaciones concurrentes
        return new ConcurrentHashMap<>(activeWindows);
    }
    
    public SecondaryWindow(ResourceBundle bundle, String baseTitle) {
        super(null); // No inicializar la ventana en la clase padre
        
        this.bundle = bundle;
        
        windowNumber = getNextAvailableNumber();
        windowId = "SecondaryWindow-" + windowNumber;
        activeWindows.put(windowId, this);
        
        // Crear un nuevo TabPane local para esta ventana
        localTabPane = new TabPane();
        configureTabPane();
        
        // Crear un contenedor raíz para aplicar el fondo
        BorderPane rootContainer = new BorderPane();
        rootContainer.setCenter(localTabPane);
        
        // Configurar la ventana
        stage = new Stage();
        Scene scene = new Scene(rootContainer);
        stage.setScene(scene);
        
        // Configurar el título con el número de ventana
        updateWindowTitle(baseTitle);
        
        // Configurar el tamaño de la ventana
        stage.setWidth(800);
        stage.setHeight(900);
        stage.setMinWidth(600);
        stage.setMinHeight(700);
        
        // Aplicar estilos CSS si existen
        try {
            scene.getStylesheets().add(getClass().getResource("/vistas/styles2.css").toExternalForm());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Configurar los atajos de teclado específicos para esta ventana
        configureKeyboardShortcuts(stage, scene);
        
        // Configurar el manejo de arrastre
        configureDragAndDrop();
        
        stage.setOnCloseRequest(event -> {
            if (localTabPane != null) {
                // Desregistrar del monitor antes de cerrar
                TabPaneMonitor.getInstance().desregistrarTabPane(localTabPane);
                
                // Cerrar pestañas localmente
                for (Tab tab : new ArrayList<>(localTabPane.getTabs())) {
                    localTabPane.getTabs().remove(tab);
                }
            }
            activeWindows.remove(windowId);
            reorderWindowNumbers();
        });
    }
    
    private void configureTabPane() {
        localTabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        
        // ESTABLECER RESOURCEBUNDLE EN TABMANAGER PARA ESTA VENTANA
        TabManager.setResourceBundle(localTabPane, bundle);
        
        TabManager.configurarMenuContextual(localTabPane, bundle);
        
        // Registrar este TabPane en el monitor para supervisión continua
        TabPaneMonitor.getInstance().registrarTabPane(localTabPane, "VentanaSecundaria-" + windowNumber);
        
        localTabPane.getTabs().addListener((javafx.collections.ListChangeListener<Tab>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Tab tab : change.getAddedSubList()) {
                        updateTabPaneReferences(tab);
                    }
                }
                if (change.wasRemoved()) {
                    if (localTabPane.getTabs().isEmpty()) {
                        Platform.runLater(() -> stage.close());
                    }
                    TabManager.reasignarNumerosGruposGramatica(localTabPane);
                }
            }
        });
    }
    
    private void configureKeyboardShortcuts(Stage stage, Scene scene) {
        // Cerrar pestaña actual (Cmd/Ctrl + W)
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN),
            () -> {
                Tab selectedTab = localTabPane.getSelectionModel().getSelectedItem();
                if (selectedTab != null && selectedTab.isClosable()) {
                    String elementId = selectedTab.getUserData() != null ? selectedTab.getUserData().toString() : null;
                    if (elementId != null) {
                        TabManager.closeChildTabs(localTabPane, elementId);
                        String grupoId = TabManager.obtenerGrupoDeElemento(localTabPane, elementId);
                        localTabPane.getTabs().remove(selectedTab);
                        TabManager.eliminarElementoDeGrupo(localTabPane, elementId, grupoId);
                        TabManager.reasignarNumerosGruposGramatica(localTabPane);
                    } else {
                        localTabPane.getTabs().remove(selectedTab);
                    }
                }
            }
        );

        // Cerrar todas las pestañas (Cmd/Ctrl + Shift + W)
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
            () -> {
                localTabPane.getTabs().clear();
                TabManager.resetGrupos(localTabPane);
            }
        );

        // Atajos para grupos (Cmd/Ctrl + 1-9)
        KeyCode[] numberKeys = {
            KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3, KeyCode.DIGIT4, KeyCode.DIGIT5,
            KeyCode.DIGIT6, KeyCode.DIGIT7, KeyCode.DIGIT8, KeyCode.DIGIT9
        };
        
        for (int i = 0; i < numberKeys.length; i++) {
            final int groupNumber = i + 1;
            scene.getAccelerators().put(
                new KeyCodeCombination(numberKeys[i], KeyCombination.SHORTCUT_DOWN),
                () -> {
                    Tab firstGroupTab = findFirstTabInGroup(groupNumber);
                    if (firstGroupTab != null) {
                        localTabPane.getSelectionModel().select(firstGroupTab);
                    }
                }
            );
        }

        // ===== ATAJOS PRINCIPALES PARA VENTANAS SECUNDARIAS =====

        // Nuevo editor (Cmd/Ctrl + N)
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN),
            () -> abrirEditorEnVentanaSecundaria()
        );

        // Simulador directo (Cmd/Ctrl + S)
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN),
            () -> abrirSimuladorEnVentanaSecundaria()
        );

        // Ayuda (Cmd/Ctrl + H)
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.H, KeyCombination.SHORTCUT_DOWN),
            () -> abrirAyuda()
        );

        // Tutorial (Cmd/Ctrl + T)
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.T, KeyCombination.SHORTCUT_DOWN),
            () -> abrirTutorial()
        );

        // Salir (Cmd/Ctrl + Q)
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN),
            () -> salirAplicacion()
        );

        // Atajo para ir al menú principal (Cmd/Ctrl + 0)
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.SHORTCUT_DOWN),
            () -> {
                // Seleccionar la primera pestaña (menú principal) si existe
                if (!localTabPane.getTabs().isEmpty()) {
                    localTabPane.getSelectionModel().selectFirst();
                }
            }
        );
    }
    
    private Tab findFirstTabInGroup(int groupNumber) {
        for (Tab tab : localTabPane.getTabs()) {
            if (tab.getUserData() != null) {
                String elementId = tab.getUserData().toString();
                String grupoId = TabManager.obtenerGrupoDeElemento(localTabPane, elementId);
                if (grupoId != null) {
                    int numeroGrupo = TabManager.obtenerNumeroGrupo(localTabPane, elementId);
                    if (numeroGrupo == groupNumber) {
                        return tab;
                    }
                }
            }
        }
        return null;
    }
    
    private void configureDragAndDrop() {
        // Permitir que el TabPane acepte pestañas arrastradas
        localTabPane.setOnDragOver(event -> {
            if (event.getGestureSource() instanceof Tab) {
                event.acceptTransferModes(TransferMode.MOVE);
                event.consume();
            }
        });
        
        // Manejar el drop de pestañas
        localTabPane.setOnDragDropped(event -> {
            if (event.getGestureSource() instanceof Tab) {
                Tab draggedTab = (Tab) event.getGestureSource();
                TabPane sourceTabPane = draggedTab.getTabPane();
                
                // Obtener el grupo de la pestaña arrastrada
                String grupoId = null;
                if (draggedTab.getUserData() != null) {
                    String elementId = draggedTab.getUserData().toString();
                    grupoId = TabManager.obtenerGrupoDeElemento(sourceTabPane, elementId);
                    
                    // Si no tiene grupo directo, puede ser una pestaña hija
                    if (grupoId == null) {
                        // Buscar el padre de esta pestaña
                        for (Tab tab : sourceTabPane.getTabs()) {
                            if (tab.getUserData() != null) {
                                String potentialParentId = tab.getUserData().toString();
                                String parentGrupoId = TabManager.obtenerGrupoDeElemento(sourceTabPane, potentialParentId);
                                
                                if (parentGrupoId != null) {
                                    // Verificar si esta pestaña es hija del elemento principal
                                    if (TabManager.isPestañaHijaDeElemento(elementId, potentialParentId)) {
                                        grupoId = parentGrupoId;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (grupoId != null) {
                    // Si pertenece a un grupo, mover todo el grupo
                    moveGroupToWindow(sourceTabPane, grupoId, draggedTab);
                } else {
                    // Si no pertenece a un grupo, mover solo la pestaña
                    addTab(draggedTab);
                    sourceTabPane.getTabs().remove(draggedTab);
                }
                
                event.setDropCompleted(true);
                event.consume();
            }
        });
    }
    
    @Override
    public void moveGroupToWindow(TabPane sourceTabPane, String grupoId, Tab selectedTab) {
        if (grupoId == null) return;

        // Usar el método mejorado que maneja correctamente la numeración local
        boolean exito = TabManager.moverGrupoEntreVentanasMejorado(sourceTabPane, localTabPane, grupoId, selectedTab);
        
        if (!exito) {
            System.err.println("[ERROR] Falló el movimiento del grupo: " + grupoId);
            // Fallback: mover solo la pestaña seleccionada
            if (selectedTab != null) {
                addTab(selectedTab);
                sourceTabPane.getTabs().remove(selectedTab);
            }
        }
    }
    
    @Override
    public void show() {
        if (!stage.isShowing()) {
            stage.show();
            // Asegurarse de que la ventana está registrada
            activeWindows.put(windowId, this);
        }
    }
    
    @Override
    public TabPane getTabPane() {
        return localTabPane;
    }
    
    @Override
    public void addTab(Tab tab) {
        Tab newTab = new Tab(tab.getText(), tab.getContent());
        newTab.setClosable(true);
        
        if (tab.getUserData() != null) {
            String userData = tab.getUserData().toString();
            newTab.setUserData(userData);
            
            // Mantener grupo si existe
            String grupoId = TabManager.obtenerGrupoDeElemento(tab.getTabPane(), userData);
            if (grupoId != null) {
                TabManager.asignarElementoAGrupo(localTabPane, userData, grupoId);
                
                // Calcular posición correcta dentro del grupo
                int posicion = TabManager.calcularPosicionSeguaDespuesDelMenu(localTabPane);
                if (posicion >= 0 && posicion < localTabPane.getTabs().size()) {
                    localTabPane.getTabs().add(posicion, newTab);
                } else {
                    localTabPane.getTabs().add(newTab);
                }
            } else {
                localTabPane.getTabs().add(newTab);
            }
        } else {
            localTabPane.getTabs().add(newTab);
        }
        
        localTabPane.getSelectionModel().select(newTab);
        updateTabPaneReferences(newTab);
        TabManager.reasignarNumerosGruposGramatica(localTabPane);
    }
    
    public Stage getStage() {
        return stage;
    }
    
    public static int getActiveWindowCount() {
        return activeWindows.size();
    }
    
    private void updateTabPaneReferences(Tab tab) {
        if (tab.getContent() instanceof Editor) {
            Editor editor = (Editor) tab.getContent();
            editor.setTabPane(localTabPane);
            editor.configurarRelacionesPadreHijo();
        } else if (tab.getContent() != null && tab.getContent().getClass().getName().equals("simulador.PanelSimuladorDesc")) {
            try {
                java.lang.reflect.Method setTabPaneMethod = tab.getContent().getClass().getMethod("setTabPane", TabPane.class);
                setTabPaneMethod.invoke(tab.getContent(), localTabPane);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private static int getNextAvailableNumber() {
        // Encontrar el primer número disponible
        Set<Integer> usedNumbers = new HashSet<>();
        for (SecondaryWindow window : activeWindows.values()) {
            usedNumbers.add(window.getWindowNumber());
        }
        
        int number = 1;
        while (usedNumbers.contains(number)) {
            number++;
        }
        
        return number;
    }
    
    private static void reorderWindowNumbers() {
        // Ordenar las ventanas por su número actual
        List<SecondaryWindow> windows = new ArrayList<>(activeWindows.values());
        windows.sort(Comparator.comparingInt(SecondaryWindow::getWindowNumber));
        
        // Reasignar números secuencialmente
        int newNumber = 1;
        for (SecondaryWindow window : windows) {
            // Solo actualizar si el número ha cambiado
            if (window.windowNumber != newNumber) {
                // Eliminar la entrada antigua
                activeWindows.remove("SecondaryWindow-" + window.windowNumber);
                
                // Actualizar el número
                window.windowNumber = newNumber;
                window.updateWindowTitle(window.stage.getTitle().replaceAll(" \\[\\d+\\]$", ""));
                
                // Añadir la nueva entrada
                activeWindows.put("SecondaryWindow-" + newNumber, window);
            }
            newNumber++;
        }
    }
    
    private void updateWindowTitle(String baseTitle) {
        stage.setTitle(baseTitle + " [" + windowNumber + "]");
    }
    
    public int getWindowNumber() {
        return windowNumber;
    }
    
    /**
     * Actualiza el idioma de esta ventana secundaria y todas sus pestañas
     */
    public void actualizarIdioma(ResourceBundle nuevoBundle) {
        // Actualizar el ResourceBundle de esta ventana
        this.bundle = nuevoBundle;
        
        // Actualizar el ResourceBundle en TabManager para esta ventana
        TabManager.setResourceBundle(localTabPane, nuevoBundle);
        
        // Actualizar el menú contextual con el nuevo idioma
        TabManager.configurarMenuContextual(localTabPane, nuevoBundle);
        
        // Actualizar todos los textos de las pestañas
        for (Tab tab : localTabPane.getTabs()) {
                    if (tab.getContent() instanceof ActualizableTextos) {
            ((ActualizableTextos) tab.getContent()).actualizarTextos(nuevoBundle);
            }
        }
        
        // Forzar la actualización de títulos de pestañas
        // Esto es crucial para las pestañas que ya estaban abiertas
        TabManager.actualizarTitulosPestañas(localTabPane);
        
        // Actualizar manualmente los títulos de pestañas que podrían no estar en el sistema de grupos
        for (Tab tab : localTabPane.getTabs()) {
            Object content = tab.getContent();
            if (content instanceof editor.Editor) {
                tab.setText(nuevoBundle.getString("editor.title"));
            } else if (content != null && content.getClass().getName().equals("simulador.PanelSimuladorDesc")) {
                try {
                    // Usar reflexión para acceder al método setBundle
                    java.lang.reflect.Method setBundleMethod = content.getClass().getMethod("setBundle", ResourceBundle.class);
                    setBundleMethod.invoke(content, nuevoBundle);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (content instanceof simulador.SimulacionFinal) {
                simulador.SimulacionFinal sim = (simulador.SimulacionFinal) content;
                sim.actualizarTextos(nuevoBundle);
            }
            
            // Actualizar títulos de pestañas hijas específicas por su userData
            if (tab.getUserData() != null) {
                String userData = tab.getUserData().toString();
                if (userData.startsWith("terminales_")) {
                    tab.setText(nuevoBundle.getString("creacion2.tab.modificar.terminales"));
                } else if (userData.startsWith("no_terminales_")) {
                    tab.setText(nuevoBundle.getString("creacion2.tab.modificar.no.terminales"));
                } else if (userData.startsWith("producciones_")) {
                    tab.setText(nuevoBundle.getString("creacion3.tab.modificar.producciones"));
                } else if (userData.startsWith("creacion_")) {
                    tab.setText(nuevoBundle.getString("editor.asistente"));
                }
            }
        }

        // Actualizar el título de la ventana si es necesario
        updateWindowTitle("SimAS 3.0");
    }

    // ===== MÉTODOS PARA ATAJOS DE TECLADO EN VENTANAS SECUNDARIAS =====

    /**
     * Abre un nuevo editor en esta ventana secundaria
     */
    private void abrirEditorEnVentanaSecundaria() {
        try {
            // Establecer el ResourceBundle en TabManager para internacionalización
            TabManager.setResourceBundle(localTabPane, bundle);

            // Crear un nuevo editor usando TabManager para posicionamiento correcto
            Editor editor = new Editor(localTabPane, null, bundle);

            // CREAR NUEVO GRUPO: Editor independiente desde ventana secundaria
            Tab editorTab = TabManager.getOrCreateTab(localTabPane, Editor.class,
                bundle.getString("editor.title"), editor, editor.getEditorId(), null);

            // Asegurar que el editorId esté configurado como userData
            editorTab.setUserData(editor.getEditorId());

            // Reasignar numeración para reflejar los cambios
            TabManager.reasignarNumerosGruposGramatica(localTabPane);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error", "No se pudo abrir el editor: " + e.getMessage());
        }
    }

    /**
     * Abre el simulador directo en esta ventana secundaria
     */
    private void abrirSimuladorEnVentanaSecundaria() {
        cargarGramaticaYSimularDirectamenteEnSecundaria();
    }

    /**
     * Carga una gramática desde archivo y va directamente al paso 6 de la simulación en ventana secundaria
     */
    private void cargarGramaticaYSimularDirectamenteEnSecundaria() {
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
                    crearSimuladorDirectoAlPaso6EnSecundaria(gramaticaCargada);
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
     * Crea un simulador y lo lleva directamente al paso 6 en ventana secundaria
     */
    private void crearSimuladorDirectoAlPaso6EnSecundaria(Gramatica gramatica) {
        try {
            // Establecer el ResourceBundle en TabManager para internacionalización
            TabManager.setResourceBundle(localTabPane, bundle);

            // Generar ID único para el simulador
            String simuladorId = "simulador_" + System.currentTimeMillis();

            // ASIGNAR A NUEVO GRUPO ANTES de crear la pestaña: Simulador independiente desde ventana secundaria
            TabManager.asignarElementoANuevoGrupo(localTabPane, simuladorId);

            // Crear una copia de la gramática para el simulador (para no modificar la original)
            Gramatica gramaticaParaSimulador = new Gramatica(gramatica);

            // Crear el simulador descendente con la copia de la gramática
            PanelSimuladorDesc simulador = new PanelSimuladorDesc(gramaticaParaSimulador, localTabPane, bundle, simuladorId);

            // Crear la pestaña del simulador con el título correcto
            String tituloBase = bundle.getString("simulador.tab.paso6");
            int numeroGrupo = TabManager.obtenerNumeroGrupo(localTabPane, simuladorId);
            String tituloFinal = numeroGrupo > 0 ? numeroGrupo + "-" + tituloBase : tituloBase;

            // Crear la pestaña usando TabManager
            Tab pestañaSimulador = TabManager.getOrCreateTab(
                localTabPane,
                PanelSimuladorDesc.class,
                tituloFinal,
                simulador,
                simuladorId,
                null
            );

            // Establecer la pestaña en el simulador
            simulador.setPestañaSimulacion(pestañaSimulador);

            // Asegurarse de que la pestaña esté seleccionada
            localTabPane.getSelectionModel().select(pestañaSimulador);

            // Saltar directamente al paso 6 (índice 5)
            simulador.cambiarPaso(5);

            // Reasignar numeración para reflejar los cambios
            TabManager.reasignarNumerosGruposGramatica(localTabPane);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error", "No se pudo crear el simulador: " + e.getMessage());
        }
    }

    /**
     * Abre el manual de ayuda
     */
    private void abrirAyuda() {
        File manual = new File("ManualDeUsuario.pdf");
        if (manual.exists()) {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(manual);
                } else {
                    mostrarError("Error", bundle.getString("msg.error.escritorio"));
                }
            } catch (IOException e) {
                mostrarError("Error", bundle.getString("msg.error.manual") + ": " + e.getMessage());
            }
        } else {
            mostrarError("Error", bundle.getString("msg.error.archivo"));
        }
    }

    /**
     * Abre el tutorial
     */
    private void abrirTutorial() {
        File tutorial = new File("src/centroayuda/SimAS.html");
        if (tutorial.exists()) {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(tutorial.toURI());
                } else {
                    mostrarError("Error", bundle.getString("msg.error.escritorio"));
                }
            } catch (IOException e) {
                mostrarError("Error", bundle.getString("msg.error.tutorial") + ": " + e.getMessage());
            }
        } else {
            mostrarError("Error", bundle.getString("msg.error.archivo"));
        }
    }

    /**
     * Sale de la aplicación
     */
    private void salirAplicacion() {
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

    /**
     * Muestra errores de validación de la gramática
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
     * Muestra un mensaje de error simple
     */
    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
} 