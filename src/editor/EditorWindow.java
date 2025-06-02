package editor;

import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import javafx.scene.Node;
import java.util.ResourceBundle;
import java.util.List;
import java.util.ArrayList;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import java.util.Map;
import java.util.HashMap;

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
        
        // Aplicar estilos CSS
        scene.getStylesheets().add(getClass().getResource("/vistas/styles.css").toExternalForm());
        
        // Configurar atajos de teclado
        configurarAtajosTeclado(scene);
        
        stage.setScene(scene);
        
        // Establecer el ResourceBundle en TabManager para internacionalización
        TabManager.setResourceBundle(tabPane, bundle);
        
        // Enable tab dragging within this window
        tabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        
        // Configurar el menú contextual para las pestañas
        TabManager.configurarMenuContextual(tabPane, bundle);
        
        // Añadir listener para detectar cuando se cierran pestañas
        tabPane.getTabs().addListener((javafx.collections.ListChangeListener.Change<? extends Tab> change) -> {
            while (change.next()) {
                if (change.wasRemoved()) {
                    // Forzar renumeración de grupos cuando se cierra una pestaña
                    TabManager.reasignarNumerosGruposGramatica(tabPane);
                }
            }
        });
        
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
                    Stage stage = (Stage) newWindow.getTabPane().getScene().getWindow();
                    stage.setX(event.getScreenX() - 100);
                    stage.setY(event.getScreenY() - 50);
                }
            }
        });
    }

    private void configurarAtajosTeclado(Scene scene) {
        // Cerrar pestaña actual (Cmd/Ctrl + W)
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

        // Cerrar todas las pestañas (Cmd/Ctrl + Shift + W)
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
            () -> {
                if (tabPane != null) {
                    // Cerrar todas las pestañas
                    tabPane.getTabs().clear();
                    // Reiniciar la numeración de grupos
                    TabManager.resetGrupos(tabPane);
                }
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
                    if (tabPane != null) {
                        // Buscar la primera pestaña del grupo especificado
                        Tab firstGroupTab = findFirstTabInGroup(groupNumber);
                        if (firstGroupTab != null) {
                            tabPane.getSelectionModel().select(firstGroupTab);
                        }
                    }
                }
            );
        }
    }

    private Tab findFirstTabInGroup(int groupNumber) {
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() != null) {
                String elementId = tab.getUserData().toString();
                String grupoId = TabManager.obtenerGrupoDeElemento(tabPane, elementId);
                if (grupoId != null) {
                    int numeroGrupo = TabManager.obtenerNumeroGrupo(tabPane, elementId);
                    if (numeroGrupo == groupNumber) {
                        return tab;
                    }
                }
            }
        }
        return null;
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
        
        // Si el contenido es un Editor, actualizar su referencia al TabPane
        if (content instanceof Editor) {
            Editor editor = (Editor) content;
            editor.setTabPane(tabPane);
            // Asegurar que se configuren las relaciones padre-hijo
            editor.configurarRelacionesPadreHijo();
        }
        // Si el contenido es un PanelSimuladorDesc, actualizar su referencia al TabPane
        else if (content.getClass().getName().equals("simulador.PanelSimuladorDesc")) {
            try {
                java.lang.reflect.Method setTabPaneMethod = content.getClass().getMethod("setTabPane", TabPane.class);
                setTabPaneMethod.invoke(content, tabPane);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // Añadir la pestaña en la posición correcta según TabManager
        if (userData != null) {
            String elementId = userData.toString();
            String grupoId = TabManager.obtenerGrupoDeElemento(tabPane, elementId);
            
            if (grupoId != null) {
                // Es parte de un grupo, añadir en la posición correcta dentro del grupo
                int posicion = TabManager.calcularPosicionSeguaDespuesDelMenu(tabPane);
                if (posicion >= 0 && posicion <= tabPane.getTabs().size()) {
                    tabPane.getTabs().add(posicion, newTab);
                } else {
                    tabPane.getTabs().add(newTab);
                }
            } else {
                // No es parte de un grupo, añadir al final
                tabPane.getTabs().add(newTab);
            }
        } else {
            // No tiene userData, añadir al final
            tabPane.getTabs().add(newTab);
        }
        
        tabPane.getSelectionModel().select(newTab);
        
        // Reasignar numeración después de añadir la pestaña
        TabManager.reasignarNumerosGruposGramatica(tabPane);
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
        Map<String, List<Tab>> parentChildMap = new HashMap<>();
        
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
                        List<Tab> childTabs = new ArrayList<>();
                        
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
                                    childTabs.add(potentialChild);
                                }
                            }
                        }
                        
                        // Guardar la relación padre-hijo para reconstruirla después
                        if (!childTabs.isEmpty()) {
                            parentChildMap.put(elementId, childTabs);
                        }
                    }
                    // Si es un simulador, buscar sus pestañas relacionadas
                    else if (elementId.startsWith("simulador_")) {
                        List<Tab> childTabs = new ArrayList<>();
                        
                        // Buscar todas las pestañas relacionadas con este simulador
                        for (Tab potentialChild : new ArrayList<>(sourceTabPane.getTabs())) {
                            if (potentialChild.getUserData() != null) {
                                String childId = potentialChild.getUserData().toString();
                                
                                // Verificar pestañas hijas de simulador
                                if (childId.equals("gramatica_" + elementId) ||
                                    childId.equals("funciones_error_" + elementId)) {
                                    groupTabs.add(potentialChild);
                                    childTabs.add(potentialChild);
                                }
                            }
                        }
                        
                        // Guardar la relación padre-hijo para reconstruirla después
                        if (!childTabs.isEmpty()) {
                            parentChildMap.put(elementId, childTabs);
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
        String nuevoGrupoId = "grupo_" + System.currentTimeMillis() + "_" + (++TabManager.contadorGrupos);
        
        // Mover todas las pestañas del grupo a la nueva ventana
        for (Tab tab : groupTabs) {
            // Remover la pestaña del TabPane original
            sourceTabPane.getTabs().remove(tab);
            
            // Actualizar referencias al TabPane en el contenido
            if (tab.getContent() instanceof Editor) {
                Editor editor = (Editor) tab.getContent();
                editor.setTabPane(tabPane);
                editor.configurarRelacionesPadreHijo();
                
                // Asignar el editor al nuevo grupo
                if (tab.getUserData() != null) {
                    String elementId = tab.getUserData().toString();
                    if (elementId.startsWith("editor_")) {
                        TabManager.asignarElementoAGrupo(tabPane, elementId, nuevoGrupoId);
                    }
                }
            } else if (TabManager.isSimuladorContent(tab.getContent())) {
                // Actualizar el tabPane directamente en el simulador
                try {
                    java.lang.reflect.Method setTabPaneMethod = tab.getContent().getClass().getMethod("setTabPane", TabPane.class);
                    setTabPaneMethod.invoke(tab.getContent(), tabPane);
                    
                    // Configurar relaciones padre-hijo si el método existe
                    try {
                        java.lang.reflect.Method configureMethod = tab.getContent().getClass().getMethod("configurarRelacionesPadreHijo");
                        configureMethod.invoke(tab.getContent());
                    } catch (Exception e) {
                        // El método no existe, ignorar
                    }
                    
                    // Asignar el simulador al nuevo grupo
                    String elementId = tab.getUserData() != null ? tab.getUserData().toString() : null;
                    if (elementId != null && elementId.startsWith("simulador_")) {
                        TabManager.asignarElementoAGrupo(tabPane, elementId, nuevoGrupoId);
                        
                        // Actualizar todas las simulaciones asociadas a este simulador
                        for (Tab simTab : tabPane.getTabs()) {
                            if (simTab.getContent() instanceof simulador.SimulacionFinal) {
                                simulador.SimulacionFinal sim = (simulador.SimulacionFinal) simTab.getContent();
                                if (sim != null && sim.getSimuladorPadreId() != null && 
                                    sim.getSimuladorPadreId().equals(elementId)) {
                                    sim.setGrupoId(nuevoGrupoId);
                                    // Forzar actualización inmediata del número de grupo
                                    sim.actualizarGrupoYTitulo();
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            // Añadir la pestaña directamente a la nueva ventana
            tabPane.getTabs().add(tab);
        }
        
        // Reconstruir las relaciones padre-hijo en la nueva ventana
        for (Map.Entry<String, List<Tab>> entry : parentChildMap.entrySet()) {
            String parentId = entry.getKey();
            List<Tab> childTabs = entry.getValue();
            
            Map<String, List<Tab>> relations = TabManager.getParentChildRelations(tabPane);
            relations.put(parentId, new ArrayList<>(childTabs));
            
            // Configurar el listener de cierre para cada pestaña hija
            for (Tab childTab : childTabs) {
                childTab.setOnClosed(event -> {
                    if (childTab.getUserData() != null) {
                        // Eliminar la pestaña de la lista de hijos
                        relations.get(parentId).remove(childTab);
                        if (relations.get(parentId).isEmpty()) {
                            relations.remove(parentId);
                        }
                    }
                });
            }
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