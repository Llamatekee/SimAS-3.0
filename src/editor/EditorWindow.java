package editor;

import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.scene.Node;
import java.util.ResourceBundle;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

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
        
        // Crear un contenedor raíz para aplicar el fondo
        BorderPane rootContainer = new BorderPane();
        rootContainer.setCenter(tabPane);
        
        // Configurar la ventana
        stage.setTitle("SimAS 3.0");
        stage.setWidth(800);
        stage.setHeight(900);
        stage.setMinWidth(600);
        stage.setMinHeight(700);

        // Crear la escena
        Scene scene = new Scene(rootContainer);
        
        // Aplicar estilos CSS
        scene.getStylesheets().add(getClass().getResource("/vistas/styles2.css").toExternalForm());
        
        // Configurar atajos de teclado
        configurarAtajosTeclado(scene);
        
        stage.setScene(scene);
        
        // Establecer el ResourceBundle en TabManager para internacionalización
        TabManager.setResourceBundle(tabPane, bundle);
        
        // Enable tab dragging within this window
        tabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        
        // Configurar el menú contextual para las pestañas
        TabManager.configurarMenuContextual(tabPane, bundle);
        
        // Registrar este TabPane en el monitor para supervisión continua
        TabPaneMonitor.getInstance().registrarTabPane(tabPane, "VentanaPrincipal");
        
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

        // Usar el método mejorado que maneja correctamente la numeración local
        boolean exito = TabManager.moverGrupoEntreVentanasMejorado(sourceTabPane, tabPane, grupoId, selectedTab);
        
        if (!exito) {
            System.err.println("[ERROR] Falló el movimiento del grupo: " + grupoId);
            // Fallback: mover solo la pestaña seleccionada
            if (selectedTab != null) {
                addTab(selectedTab);
                sourceTabPane.getTabs().remove(selectedTab);
            }
        }
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