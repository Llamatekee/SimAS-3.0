package editor;

import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import java.util.ResourceBundle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SecondaryWindow extends EditorWindow {
    
    public SecondaryWindow(ResourceBundle bundle, String title) {
        super(bundle);
        Stage stage = getStage();
        stage.setTitle(title);
        stage.initModality(Modality.NONE); // Permite interactuar con la ventana principal
        
        // Configurar el TabPane para gestión de grupos
        TabPane tabPane = getTabPane();
        
        // Inicializar mapas en TabManager para este TabPane
        TabManager.setResourceBundle(tabPane, bundle);
        
        // Configurar drag & drop de pestañas
        tabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        
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
                    EditorWindow newWindow = new EditorWindow(bundle);
                    
                    // Encontrar el grupo al que pertenece la pestaña
                    String grupoId = null;
                    String elementId = null;
                    
                    if (selectedTab.getUserData() != null) {
                        elementId = selectedTab.getUserData().toString();
                        
                        // Primero intentar obtener el grupo directamente si es un elemento principal
                        grupoId = TabManager.obtenerGrupoDeElemento(tabPane, elementId);
                        
                        // Si no tiene grupo directo, puede ser una pestaña hija
                        if (grupoId == null) {
                            // Buscar el padre de esta pestaña
                            for (Tab tab : tabPane.getTabs()) {
                                if (tab.getUserData() != null) {
                                    String potentialParentId = tab.getUserData().toString();
                                    String parentGrupoId = TabManager.obtenerGrupoDeElemento(tabPane, potentialParentId);
                                    
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
                        newWindow.moveGroupToWindow(tabPane, grupoId, selectedTab);
                    } else {
                        // Si no pertenece a un grupo, mover solo la pestaña
                        newWindow.addTab(selectedTab);
                        tabPane.getTabs().remove(selectedTab);
                    }
                    
                    // Show the new window at the cursor position
                    newWindow.show();
                    Stage newStage = (Stage) newWindow.getTabPane().getScene().getWindow();
                    newStage.setX(event.getScreenX() - 100);
                    newStage.setY(event.getScreenY() - 50);
                }
            }
        });
        
        // Listener para cierre de pestañas y actualización de grupos
        tabPane.getTabs().addListener((javafx.collections.ListChangeListener.Change<? extends Tab> change) -> {
            while (change.next()) {
                if (change.wasRemoved()) {
                    for (Tab tab : change.getRemoved()) {
                        // Si la pestaña es parte de un grupo, limpiar su grupo
                        if (tab.getUserData() != null) {
                            String elementId = tab.getUserData().toString();
                            String grupoId = TabManager.obtenerGrupoDeElemento(tabPane, elementId);
                            if (grupoId != null) {
                                TabManager.eliminarElementoDeGrupo(tabPane, elementId, grupoId);
                            }
                            // Si es una pestaña padre, cerrar sus hijas
                            TabManager.closeChildTabs(tabPane, elementId);
                        }
                    }
                    // Reasignar números de grupos
                    TabManager.reasignarNumerosGruposGramatica(tabPane);
                }
            }
        });
        
        // Configurar cierre de ventana
        stage.setOnCloseRequest(event -> {
            // Cerrar todas las pestañas y limpiar grupos
            if (tabPane != null) {
                // Primero cerrar las pestañas hijas para evitar problemas de dependencias
                for (Tab tab : new ArrayList<>(tabPane.getTabs())) {
                    if (tab.getUserData() != null) {
                        String elementId = tab.getUserData().toString();
                        TabManager.closeChildTabs(tabPane, elementId);
                    }
                }
                // Luego cerrar todas las pestañas y resetear grupos
                tabPane.getTabs().clear();
                TabManager.resetGrupos(tabPane);
            }
        });
    }
    
    @Override
    public void addTab(Tab tab) {
        TabPane tabPane = getTabPane();
        
        // Preserve the tab's properties
        String title = tab.getText();
        javafx.scene.Node content = tab.getContent();
        Object userData = tab.getUserData();
        boolean closable = tab.isClosable();
        
        // Create a new tab with the same properties
        Tab newTab = new Tab(title);
        newTab.setContent(content);
        newTab.setClosable(closable);
        newTab.setUserData(userData);
        
        // Configurar el listener de cierre para manejar pestañas hijas
        newTab.setOnClosed(event -> {
            // Si es una pestaña padre, cerrar también las hijas
            if (userData != null) {
                String elementId = userData.toString();
                
                // Cerrar las pestañas hijas
                TabManager.closeChildTabs(tabPane, elementId);
                
                // Si es parte de un grupo, limpiar el grupo
                String grupoId = TabManager.obtenerGrupoDeElemento(tabPane, elementId);
                if (grupoId != null) {
                    TabManager.eliminarElementoDeGrupo(tabPane, elementId, grupoId);
                }
                
                // Forzar renumeración de grupos
                TabManager.reasignarNumerosGruposGramatica(tabPane);
            }
        });
        
        // Actualizar referencias al TabPane en el contenido
        if (content instanceof Editor) {
            Editor editor = (Editor) content;
            editor.setTabPane(tabPane);
            editor.configurarRelacionesPadreHijo();
        } else if (TabManager.isSimuladorContent(content)) {
            try {
                // Actualizar el tabPane directamente en el simulador
                java.lang.reflect.Method setTabPaneMethod = content.getClass().getMethod("setTabPane", TabPane.class);
                setTabPaneMethod.invoke(content, tabPane);
                
                // Configurar relaciones padre-hijo si el método existe
                try {
                    java.lang.reflect.Method configureMethod = content.getClass().getMethod("configurarRelacionesPadreHijo");
                    configureMethod.invoke(content);
                } catch (NoSuchMethodException e) {
                    // El método no existe, ignorar
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // Si la pestaña tiene userData, es parte de un grupo
        if (userData != null) {
            String elementId = userData.toString();
            String grupoId = TabManager.obtenerGrupoDeElemento(tabPane, elementId);
            
            // Si es un editor o simulador independiente y no tiene grupo, crear uno nuevo
            if ((elementId.startsWith("editor_") || elementId.startsWith("simulador_")) && grupoId == null) {
                TabManager.asignarElementoANuevoGrupo(tabPane, elementId);
                grupoId = TabManager.obtenerGrupoDeElemento(tabPane, elementId);
            }
            
            // Si es una pestaña hija, registrar la relación padre-hijo
            if (elementId.contains("creacion_") || 
                elementId.contains("terminales_") || 
                elementId.contains("no_terminales_") || 
                elementId.contains("producciones_") ||
                elementId.startsWith("gramatica_") ||
                elementId.startsWith("funciones_error_")) {
                
                // Buscar el padre de esta pestaña
                for (Tab existingTab : tabPane.getTabs()) {
                    if (existingTab.getUserData() != null) {
                        String potentialParentId = existingTab.getUserData().toString();
                        if (TabManager.isPestañaHijaDeElemento(elementId, potentialParentId)) {
                            // Registrar la relación padre-hijo
                            Map<String, List<Tab>> relations = TabManager.getParentChildRelations(tabPane);
                            if (relations != null) {
                                relations.computeIfAbsent(potentialParentId, k -> new ArrayList<>()).add(newTab);
                            }
                            break;
                        }
                    }
                }
            }
            
            // Añadir la pestaña en la posición correcta según el grupo
            if (grupoId != null) {
                int posicion = TabManager.calcularPosicionSeguaDespuesDelMenu(tabPane);
                if (posicion >= 0 && posicion <= tabPane.getTabs().size()) {
                    tabPane.getTabs().add(posicion, newTab);
                } else {
                    tabPane.getTabs().add(newTab);
                }
            } else {
                tabPane.getTabs().add(newTab);
            }
        } else {
            tabPane.getTabs().add(newTab);
        }
        
        tabPane.getSelectionModel().select(newTab);
        TabManager.reasignarNumerosGruposGramatica(tabPane);
    }
    
    public Stage getStage() {
        return (Stage) getTabPane().getScene().getWindow();
    }
    
    public void setPosition(double x, double y) {
        Stage stage = getStage();
        stage.setX(x);
        stage.setY(y);
    }
    
    public void setSize(double width, double height) {
        Stage stage = getStage();
        stage.setWidth(width);
        stage.setHeight(height);
    }
} 