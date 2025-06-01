package editor;

import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import javafx.scene.Node;
import java.util.ResourceBundle;
import simulador.PanelSimuladorDesc;
import java.util.List;
import java.util.ArrayList;

public class EditorWindow {
    private Stage stage;
    private TabPane tabPane;
    private ResourceBundle bundle;

    public EditorWindow(ResourceBundle bundle) {
        this.bundle = bundle;
        initialize();
    }

    private void initialize() {
        stage = new Stage();
        tabPane = new TabPane();
        
        // Configurar la ventana
        stage.setTitle("SimAS 3.0");
        stage.setWidth(800);
        stage.setHeight(900);
        stage.setMinWidth(600);
        stage.setMinHeight(700);

        // Crear la escena
        Scene scene = new Scene(tabPane);
        stage.setScene(scene);
        
        // Enable tab dragging within this window
        tabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
    }

    public void show() {
        stage.show();
    }

    public void addTab(Tab tab) {
        // Preserve the tab's properties
        String title = tab.getText();
        Node content = tab.getContent();
        Object userData = tab.getUserData();
        boolean closable = tab.isClosable();
        
        // Create a new tab with the same properties
        Tab newTab = new Tab(title);
        newTab.setContent(content);
        newTab.setClosable(closable);
        newTab.setUserData(userData);
        
        tabPane.getTabs().add(newTab);
        tabPane.getSelectionModel().select(newTab);
    }

    /**
     * Mueve un grupo completo de pestañas a esta ventana.
     * @param sourceTabPane El TabPane original
     * @param grupoId El ID del grupo a mover
     * @param selectedTab La pestaña que se arrastró inicialmente
     */
    public void moveGroupToWindow(TabPane sourceTabPane, String grupoId, Tab selectedTab) {
        if (grupoId == null) return;

        // Recolectar todas las pestañas del grupo y sus IDs
        List<Tab> groupTabs = new ArrayList<>();
        List<String> elementIds = new ArrayList<>();
        
        // Primero encontrar los elementos principales del grupo (editores/simuladores)
        for (Tab tab : new ArrayList<>(sourceTabPane.getTabs())) {  // Crear copia para evitar ConcurrentModification
            if (tab.getUserData() != null) {
                String elementId = tab.getUserData().toString();
                String elementGrupoId = TabManager.obtenerGrupoDeElemento(sourceTabPane, elementId);
                
                if (grupoId.equals(elementGrupoId)) {
                    // Es un elemento principal del grupo (editor o simulador)
                    groupTabs.add(tab);
                    elementIds.add(elementId);
                    
                    // Si es un editor, buscar sus pestañas relacionadas
                    if (elementId.startsWith("editor_")) {
                        String editorBaseId = elementId.replace("editor_", "");
                        String creacionId = "creacion_" + editorBaseId;
                        
                        // Buscar todas las pestañas relacionadas con este editor
                        for (Tab potentialChild : new ArrayList<>(sourceTabPane.getTabs())) {
                            if (potentialChild.getUserData() != null) {
                                String childId = potentialChild.getUserData().toString();
                                
                                // Verificar todos los posibles tipos de pestañas hijas de editor
                                if (childId.equals(creacionId) ||  // Pestaña de creación
                                    childId.startsWith("terminales_" + creacionId) ||  // Pestaña de terminales
                                    childId.startsWith("no_terminales_" + creacionId) ||  // Pestaña de no terminales
                                    childId.startsWith("producciones_" + creacionId)) {  // Pestaña de producciones
                                    groupTabs.add(potentialChild);
                                }
                            }
                        }
                    }
                    // Si es un simulador, buscar sus pestañas relacionadas
                    else if (elementId.startsWith("simulador_")) {
                        for (Tab potentialChild : new ArrayList<>(sourceTabPane.getTabs())) {
                            if (potentialChild.getUserData() != null) {
                                String childId = potentialChild.getUserData().toString();
                                
                                // Verificar pestañas hijas de simulador
                                if (childId.equals("gramatica_" + elementId) ||  // Pestaña de gramática
                                    childId.equals("funciones_error_" + elementId)) {  // Pestaña de funciones de error
                                    groupTabs.add(potentialChild);
                                }
                            }
                            
                            // Verificar si es una pestaña de simulación (derivación o árbol)
                            if (potentialChild.getContent() instanceof simulador.SimulacionFinal) {
                                simulador.SimulacionFinal sim = (simulador.SimulacionFinal) potentialChild.getContent();
                                if (sim.perteneceASimulador(elementId)) {
                                    groupTabs.add(potentialChild);
                                    
                                    // Buscar pestañas hijas de la simulación
                                    for (Tab simChild : new ArrayList<>(sourceTabPane.getTabs())) {
                                        if (simChild.getUserData() != null && sim.esHijaDeLaSimulacion(simChild)) {
                                            groupTabs.add(simChild);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Eliminar el grupo y sus elementos de la ventana original
        for (String elementId : elementIds) {
            TabManager.eliminarElementoDeGrupo(sourceTabPane, elementId, grupoId);
        }

        // Mover todas las pestañas del grupo a la nueva ventana
        for (Tab tab : groupTabs) {
            // Remover la pestaña del TabPane original
            sourceTabPane.getTabs().remove(tab);
            
            // Añadir la pestaña directamente a la nueva ventana
            tabPane.getTabs().add(tab);
        }
        
        // Seleccionar la pestaña que se arrastró inicialmente
        if (selectedTab != null && tabPane.getTabs().contains(selectedTab)) {
            tabPane.getSelectionModel().select(selectedTab);
        }
        
        // Reasignar números de grupos en ambas ventanas
        TabManager.reasignarNumerosGruposGramatica(sourceTabPane);
        TabManager.reasignarNumerosGruposGramatica(tabPane);
    }

    public void addEditor(Editor editor) {
        Tab editorTab = new Tab(bundle.getString("editor.title"), editor);
        editorTab.setClosable(true);
        editorTab.setUserData(editor.getEditorId());
        tabPane.getTabs().add(editorTab);
        tabPane.getSelectionModel().select(editorTab);
    }

    public TabPane getTabPane() {
        return tabPane;
    }
} 