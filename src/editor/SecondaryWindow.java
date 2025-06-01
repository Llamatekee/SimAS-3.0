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
import java.util.concurrent.ConcurrentHashMap;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.application.Platform;

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
        localTabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        
        // Configurar el menú contextual para las pestañas
        TabManager.configurarMenuContextual(localTabPane, bundle);
        
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
        configureKeyboardShortcuts(stage, scene, bundle);
        
        // Configurar el manejo de arrastre
        configureDragAndDrop();
        
        printTabCount("Ventana creada");
        
        // Listener para monitorear cambios en las pestañas
        localTabPane.getTabs().addListener((javafx.collections.ListChangeListener<Tab>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    printTabCount("Pestaña(s) añadida(s)");
                    // Actualizar el TabPane en los contenidos añadidos
                    for (Tab tab : change.getAddedSubList()) {
                        updateTabPaneReferences(tab);
                    }
                }
                if (change.wasRemoved()) {
                    printTabCount("Pestaña(s) eliminada(s)");
                    if (localTabPane.getTabs().isEmpty()) {
                        System.err.println("\n¡VENTANA [" + windowId + "] VACÍA! Cerrando...\n");
                        Platform.runLater(() -> stage.close());
                    }
                }
            }
        });
        
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
    
    private void updateTabPaneReferences(Tab tab) {
        if (tab.getContent() != null) {
            // Si es un Editor, actualizar su referencia al TabPane
            if (tab.getContent() instanceof Editor) {
                Editor editor = (Editor) tab.getContent();
                editor.setTabPane(localTabPane);
                try {
                    editor.configurarRelacionesPadreHijo();
                } catch (Exception e) {
                    System.err.println("Error al configurar relaciones del editor: " + e.getMessage());
                }
            }
            // Si es un PanelSimuladorDesc, actualizar su referencia al TabPane
            else if (tab.getContent().getClass().getName().equals("simulador.PanelSimuladorDesc")) {
                try {
                    java.lang.reflect.Method setTabPaneMethod = tab.getContent().getClass().getMethod("setTabPane", TabPane.class);
                    setTabPaneMethod.invoke(tab.getContent(), localTabPane);
                    
                    // Intentar configurar relaciones padre-hijo si el método existe
                    try {
                        java.lang.reflect.Method configureMethod = tab.getContent().getClass().getMethod("configurarRelacionesPadreHijo");
                        configureMethod.invoke(tab.getContent());
                    } catch (Exception e) {
                        // El método puede no existir, ignorar
                    }
                } catch (Exception e) {
                    System.err.println("Error al actualizar TabPane en simulador: " + e.getMessage());
                }
            }
        }
    }
    
    @Override
    public void moveGroupToWindow(TabPane sourceTabPane, String grupoId, Tab selectedTab) {
        System.err.println("\n>>> Moviendo grupo a ventana secundaria [" + windowId + "] <<<");
        System.err.println("ID del grupo: " + grupoId);
        System.err.println("Pestaña seleccionada: " + (selectedTab != null ? selectedTab.getText() : "null"));
        
                        if (grupoId == null) {
            // Si no hay grupo, solo mover la pestaña seleccionada
            addTab(selectedTab);
            sourceTabPane.getTabs().remove(selectedTab);
            return;
        }

        // Recolectar todas las pestañas del grupo
        List<Tab> groupTabs = new ArrayList<>();
        List<String> elementIds = new ArrayList<>();
        
        // Primero encontrar los elementos principales del grupo
        for (Tab tab : new ArrayList<>(sourceTabPane.getTabs())) {
                                if (tab.getUserData() != null) {
                String elementId = tab.getUserData().toString();
                String elementGrupoId = TabManager.obtenerGrupoDeElemento(sourceTabPane, elementId);
                
                if (grupoId.equals(elementGrupoId)) {
                    groupTabs.add(tab);
                    elementIds.add(elementId);
                    
                    // Buscar pestañas hijas
                    if (elementId.startsWith("editor_")) {
                        // Buscar pestañas de creación y símbolos
                        for (Tab potentialChild : sourceTabPane.getTabs()) {
                            if (potentialChild.getUserData() != null) {
                                String childId = potentialChild.getUserData().toString();
                                if (TabManager.isPestañaHijaDeElemento(childId, elementId)) {
                                    groupTabs.add(potentialChild);
                                }
                            }
                        }
                    } else if (elementId.startsWith("simulador_")) {
                        // Buscar pestañas de gramática y funciones de error
                        for (Tab potentialChild : sourceTabPane.getTabs()) {
                            if (potentialChild.getUserData() != null) {
                                String childId = potentialChild.getUserData().toString();
                                if (childId.equals("gramatica_" + elementId) ||
                                    childId.equals("funciones_error_" + elementId)) {
                                    groupTabs.add(potentialChild);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Limpiar las relaciones en la ventana origen
        for (String elementId : elementIds) {
            TabManager.eliminarElementoDeGrupo(sourceTabPane, elementId, grupoId);
        }

        // Crear un nuevo grupo en la ventana destino
        String nuevoGrupoId = "grupo_secundario_" + windowId + "_" + System.currentTimeMillis();
        
        // Mover cada pestaña a la nueva ventana
        for (Tab tab : groupTabs) {
            // Crear una copia de la pestaña
            Tab newTab = new Tab(tab.getText(), tab.getContent());
            newTab.setClosable(true);
                        if (tab.getUserData() != null) {
                newTab.setUserData(tab.getUserData().toString());
                
                // Si es un elemento principal, asignarlo al nuevo grupo
                            String elementId = tab.getUserData().toString();
                if (elementId.startsWith("editor_") || elementId.startsWith("simulador_")) {
                    TabManager.asignarElementoAGrupo(localTabPane, elementId, nuevoGrupoId);
                }
            }
            
            // Añadir la pestaña a la ventana secundaria
            localTabPane.getTabs().add(newTab);
            
            // Actualizar la referencia al TabPane en el contenido
            updateTabPaneReferences(newTab);
            
            // Remover la pestaña original
            sourceTabPane.getTabs().remove(tab);
        }
        
        // Seleccionar la pestaña que se arrastró inicialmente
        if (selectedTab != null) {
            for (Tab tab : localTabPane.getTabs()) {
                if (tab.getUserData() != null && 
                    tab.getUserData().equals(selectedTab.getUserData())) {
                    localTabPane.getSelectionModel().select(tab);
                    break;
                }
            }
        }
        
        // Reasignar números de grupos en ambas ventanas
        Platform.runLater(() -> {
            TabManager.reasignarNumerosGruposGramatica(sourceTabPane);
            TabManager.reasignarNumerosGruposGramatica(localTabPane);
        });
        
        printTabCount("Grupo movido a ventana secundaria");
    }
    
    @Override
    public void show() {
        stage.show();
    }
    
    @Override
    public TabPane getTabPane() {
        return localTabPane;
    }
    
    @Override
    public void addTab(Tab tab) {
        System.err.println("\n>>> Añadiendo pestaña a ventana secundaria [" + windowId + "] <<<");
        System.err.println("Título de la pestaña: " + tab.getText());
        System.err.println("UserData: " + (tab.getUserData() != null ? tab.getUserData().toString() : "null"));
        
        // Crear una copia de la pestaña para esta ventana
        Tab newTab = new Tab(tab.getText(), tab.getContent());
        newTab.setClosable(true);
        if (tab.getUserData() != null) {
            newTab.setUserData(tab.getUserData().toString());
        }
        
        // Añadir la pestaña al TabPane local
        localTabPane.getTabs().add(newTab);
        localTabPane.getSelectionModel().select(newTab);
        
        // Actualizar la referencia al TabPane en el contenido
        updateTabPaneReferences(newTab);
        
        printTabCount("Pestaña añadida mediante addTab");
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
                
                // Crear una copia de la pestaña para esta ventana
                Tab newTab = new Tab(draggedTab.getText());
                newTab.setContent(draggedTab.getContent());
                newTab.setClosable(true);
                if (draggedTab.getUserData() != null) {
                    newTab.setUserData(draggedTab.getUserData().toString());
                }
                
                // Añadir la pestaña al TabPane local
                localTabPane.getTabs().add(newTab);
                localTabPane.getSelectionModel().select(newTab);
                
                event.setDropCompleted(true);
                event.consume();
                
                printTabCount("Pestaña añadida mediante arrastre");
            }
        });
    }
    
    private void configureKeyboardShortcuts(Stage stage, Scene scene, ResourceBundle bundle) {
        scene.setOnKeyPressed(event -> {
            // Solo procesar eventos si la ventana está enfocada
            if (!stage.isFocused()) {
                return;
            }

            // Cmd+W: Cerrar pestaña actual
            if (event.isShortcutDown() && !event.isShiftDown() && event.getCode() == KeyCode.W) {
                event.consume(); // Prevenir que el evento se propague
                Platform.runLater(() -> {
                    Tab selectedTab = localTabPane.getSelectionModel().getSelectedItem();
                    if (selectedTab != null) {
                        localTabPane.getTabs().remove(selectedTab);
                    }
                });
            }
            
            // Cmd+Shift+W: Cerrar ventana con confirmación
            else if (event.isShortcutDown() && event.isShiftDown() && event.getCode() == KeyCode.W) {
                event.consume(); // Prevenir que el evento se propague
                Platform.runLater(() -> {
                    // Verificar nuevamente que la ventana esté enfocada
                    if (stage.isFocused()) {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("Cerrar ventana");
                        alert.setHeaderText("¿Cerrar todas las pestañas?");
                        alert.setContentText("Se cerrarán todas las pestañas de esta ventana.");
                        alert.initOwner(stage);
                        
                        // Asegurarnos de que el diálogo se muestre en el thread de JavaFX
                        alert.showAndWait().ifPresent(response -> {
                            if (response == ButtonType.OK) {
                                stage.close();
                            }
                        });
                    }
                });
            }
        });
    }
    
    public Stage getStage() {
        return stage;
    }
    
    public static int getActiveWindowCount() {
        return activeWindows.size();
    }
} 