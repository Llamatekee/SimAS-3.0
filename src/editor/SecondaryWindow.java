package editor;

import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import java.util.ResourceBundle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.application.Platform;
import javafx.scene.Node;

public class SecondaryWindow extends EditorWindow {
    
    private static final Map<String, SecondaryWindow> activeWindows = new ConcurrentHashMap<>();
    private final String windowId;
    private static int windowCounter = 0;
    private final TabPane localTabPane;
    private final Stage stage;
    private final ResourceBundle bundle;
    
    static {
        System.err.println("\n¡CLASE SecondaryWindow CARGADA!\n");
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
                System.err.println("\n>>> Eliminando ventana inactiva: " + entry.getKey() + " <<<\n");
                return true;
            }
            return false;
        });
        
        // Devolver una copia del mapa para evitar modificaciones concurrentes
        return new ConcurrentHashMap<>(activeWindows);
    }
    
    private void printTabCount(String action) {
        System.err.println("\n=== Ventana Secundaria [" + windowId + "] ===");
        System.err.println("Acción: " + action);
        System.err.println("Número de pestañas: " + localTabPane.getTabs().size());
        System.err.println("========================\n");
    }
    
    public SecondaryWindow(ResourceBundle bundle, String title) {
        super(null); // No inicializar la ventana en la clase padre
        
        this.bundle = bundle;
        System.err.println("\n>>> CREANDO NUEVA VENTANA SECUNDARIA <<<\n");
        
        windowId = "SecondaryWindow-" + (++windowCounter);
        activeWindows.put(windowId, this);
        
        // Crear un nuevo TabPane local para esta ventana
        localTabPane = new TabPane();
        configureTabPane();
        
        // Configurar la ventana
        stage = new Stage();
        Scene scene = new Scene(localTabPane);
        stage.setScene(scene);
        stage.setTitle(title);
        stage.initModality(Modality.NONE);
        
        // Configurar el tamaño de la ventana
        stage.setWidth(800);
        stage.setHeight(900);
        stage.setMinWidth(600);
        stage.setMinHeight(700);
        
        // Aplicar estilos CSS si existen
        try {
            scene.getStylesheets().add(getClass().getResource("/vistas/styles.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("No se pudieron cargar los estilos CSS");
        }
        
        // Configurar los atajos de teclado específicos para esta ventana
        configureKeyboardShortcuts(stage, scene);
        
        // Configurar el manejo de arrastre
        configureDragAndDrop();
        
        printTabCount("Ventana creada");
        
        stage.setOnCloseRequest(event -> {
            printTabCount("Ventana cerrándose");
            if (localTabPane != null) {
                // Cerrar pestañas localmente
                for (Tab tab : new ArrayList<>(localTabPane.getTabs())) {
                    localTabPane.getTabs().remove(tab);
                }
            }
            activeWindows.remove(windowId);
            System.err.println("\n>>> VENTANA [" + windowId + "] ELIMINADA DEL REGISTRO <<<\n");
        });
    }
    
    private void configureTabPane() {
        localTabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        TabManager.configurarMenuContextual(localTabPane, bundle);
        
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
                
                printTabCount("Pestaña(s) añadida(s) mediante arrastre");
            }
        });
    }
    
    @Override
    public void moveGroupToWindow(TabPane sourceTabPane, String grupoId, Tab selectedTab) {
        if (grupoId == null) return;

        System.err.println("\n=== INICIO MOVIMIENTO DE GRUPO ===");
        System.err.println("Moviendo desde: " + sourceTabPane);
        System.err.println("Hacia: " + localTabPane);
        System.err.println("ID del grupo: " + grupoId);

        List<Tab> groupTabs = new ArrayList<>();
        List<String> elementIds = new ArrayList<>();
        Map<String, List<Tab>> parentChildMap = new HashMap<>();
        
        // Analizar estado inicial de la ventana origen
        System.err.println("\n--- ESTADO INICIAL VENTANA ORIGEN ---");
        System.err.println("Total pestañas en origen: " + sourceTabPane.getTabs().size());
        Map<String, List<Tab>> sourceRelations = TabManager.getParentChildRelations(sourceTabPane);
        
        // Primero, recolectar todas las pestañas padre del grupo
        for (Tab tab : new ArrayList<>(sourceTabPane.getTabs())) {
            if (tab.getUserData() != null) {
                String elementId = tab.getUserData().toString();
                String elementGrupoId = TabManager.obtenerGrupoDeElemento(sourceTabPane, elementId);
                
                if (grupoId.equals(elementGrupoId)) {
                    // Es una pestaña padre del grupo
                    if (elementId.startsWith("editor_") || elementId.startsWith("simulador_")) {
                        groupTabs.add(tab);
                        elementIds.add(elementId);
                        System.err.println("\nEncontrada pestaña padre del grupo: " + elementId);
                    }
                }
            }
        }

        // Luego, para cada padre, recolectar sus hijos
        for (Tab parentTab : new ArrayList<>(groupTabs)) {
            String parentId = parentTab.getUserData().toString();
            List<Tab> childTabs = new ArrayList<>();
            
            // Buscar todas las pestañas hijas
            for (Tab tab : new ArrayList<>(sourceTabPane.getTabs())) {
                if (tab.getUserData() != null) {
                    String childId = tab.getUserData().toString();
                    if (TabManager.isPestañaHijaDeElemento(childId, parentId)) {
                        childTabs.add(tab);
                        System.err.println("  Hijo encontrado para " + parentId + ": " + childId);
                    }
                }
            }
            
            if (!childTabs.isEmpty()) {
                parentChildMap.put(parentId, childTabs);
                groupTabs.addAll(childTabs);
            }
        }

        System.err.println("\n--- PESTAÑAS A MOVER ---");
        System.err.println("Total pestañas a mover: " + groupTabs.size());
        for (Tab tab : groupTabs) {
            System.err.println("• " + (tab.getUserData() != null ? tab.getUserData().toString() : "sin ID") + " - " + tab.getText());
        }

        // Limpiar las referencias del grupo en la ventana origen
        for (String elementId : elementIds) {
            TabManager.eliminarElementoDeGrupo(sourceTabPane, elementId, grupoId);
        }

        // Mantener el mismo grupoId en la ventana destino
        for (String elementId : elementIds) {
            TabManager.asignarElementoAGrupo(localTabPane, elementId, grupoId);
        }

        // Mover las pestañas manteniendo sus referencias originales
        Map<String, List<Tab>> destRelations = TabManager.getParentChildRelations(localTabPane);
        
        // Primero limpiar las relaciones existentes en el destino para este grupo
        for (String parentId : parentChildMap.keySet()) {
            destRelations.remove(parentId);
        }

        // Ahora mover las pestañas y establecer las nuevas relaciones
        for (Tab tab : groupTabs) {
            sourceTabPane.getTabs().remove(tab);
            localTabPane.getTabs().add(tab);
            updateTabPaneReferences(tab);
            
            // Si es una pestaña padre, establecer sus relaciones
            if (tab.getUserData() != null) {
                String elementId = tab.getUserData().toString();
                if (parentChildMap.containsKey(elementId)) {
                    destRelations.put(elementId, parentChildMap.get(elementId));
                }
            }
        }

        // Verificar estado final
        System.err.println("\n--- ESTADO FINAL VENTANA DESTINO ---");
        System.err.println("Total pestañas en destino: " + localTabPane.getTabs().size());
        for (Map.Entry<String, List<Tab>> entry : destRelations.entrySet()) {
            System.err.println("Padre: " + entry.getKey());
            for (Tab childTab : entry.getValue()) {
                System.err.println("  └─ Hijo: " + (childTab.getUserData() != null ? childTab.getUserData().toString() : "sin ID"));
            }
        }

        // Seleccionar la pestaña arrastrada
        if (selectedTab != null && localTabPane.getTabs().contains(selectedTab)) {
            localTabPane.getSelectionModel().select(selectedTab);
        }

        // Forzar renumeración inmediata en ambas ventanas
        Platform.runLater(() -> {
            // Primero limpiar cualquier referencia residual
            for (String elementId : elementIds) {
                TabManager.eliminarElementoDeGrupo(sourceTabPane, elementId, grupoId);
            }
            
            // Luego renumerar
            TabManager.reasignarNumerosGruposGramatica(sourceTabPane);
            TabManager.reasignarNumerosGruposGramatica(localTabPane);
            
            // Programar una segunda renumeración para asegurar que todo se actualice
            Platform.runLater(() -> {
                TabManager.reasignarNumerosGruposGramatica(sourceTabPane);
                TabManager.reasignarNumerosGruposGramatica(localTabPane);
            });
        });

        System.err.println("\n=== FIN MOVIMIENTO DE GRUPO ===\n");
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
} 