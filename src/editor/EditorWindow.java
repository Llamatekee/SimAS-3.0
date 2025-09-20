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
import gramatica.Gramatica;
import simulador.PanelSimuladorDesc;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import utils.TabPaneMonitor;
import utils.TabManager;
import utils.SecondaryWindow;

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

        // ===== ATAJOS PRINCIPALES PARA EDITORWINDOW =====

        // Nuevo editor (Cmd/Ctrl + N)
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN),
            () -> abrirEditorEnEditorWindow()
        );

        // Simulador directo (Cmd/Ctrl + S)
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN),
            () -> abrirSimuladorEnEditorWindow()
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
                if (tabPane != null && !tabPane.getTabs().isEmpty()) {
                    tabPane.getSelectionModel().selectFirst();
                }
            }
        );
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

    // ===== MÉTODOS PARA ATAJOS DE TECLADO EN EDITORWINDOW =====

    /**
     * Abre un nuevo editor en esta ventana
     */
    private void abrirEditorEnEditorWindow() {
        try {
            // Establecer el ResourceBundle en TabManager para internacionalización
            TabManager.setResourceBundle(tabPane, bundle);

            // Crear un nuevo editor usando TabManager para posicionamiento correcto
            Editor editor = new Editor(tabPane, null, bundle);

            // CREAR NUEVO GRUPO: Editor independiente desde EditorWindow
            Tab editorTab = TabManager.getOrCreateTab(tabPane, Editor.class,
                bundle.getString("editor.title"), editor, editor.getEditorId(), null);

            // Asegurar que el editorId esté configurado como userData
            editorTab.setUserData(editor.getEditorId());

            // Reasignar numeración para reflejar los cambios
            TabManager.reasignarNumerosGruposGramatica(tabPane);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error", "No se pudo abrir el editor: " + e.getMessage());
        }
    }

    /**
     * Abre el simulador directo en esta ventana
     */
    private void abrirSimuladorEnEditorWindow() {
        cargarGramaticaYSimularDirectamenteEnEditorWindow();
    }

    /**
     * Carga una gramática desde archivo y va directamente al paso 6 de la simulación en EditorWindow
     */
    private void cargarGramaticaYSimularDirectamenteEnEditorWindow() {
        try {
            // Crear una nueva gramática
            Gramatica nuevaGramatica = new Gramatica();

            // Cargar gramática desde archivo (esto abrirá el selector de archivos)
            Gramatica gramaticaCargada = nuevaGramatica.cargarGramatica(null);

            if (gramaticaCargada != null) {
                // Validar la gramática cargada
                javafx.collections.ObservableList<String> errores = gramaticaCargada.validarGramatica(bundle);

                if (gramaticaCargada.getEstado() == 1) {
                    // Gramática válida - proceder con la simulación
                    crearSimuladorDirectoAlPaso6EnEditorWindow(gramaticaCargada);
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
     * Crea un simulador y lo lleva directamente al paso 6 en EditorWindow
     */
    private void crearSimuladorDirectoAlPaso6EnEditorWindow(Gramatica gramatica) {
        try {
            // Establecer el ResourceBundle en TabManager para internacionalización
            TabManager.setResourceBundle(tabPane, bundle);

            // Generar ID único para el simulador
            String simuladorId = "simulador_" + System.currentTimeMillis();

            // ASIGNAR A NUEVO GRUPO ANTES de crear la pestaña: Simulador independiente desde EditorWindow
            TabManager.asignarElementoANuevoGrupo(tabPane, simuladorId);

            // Crear una copia de la gramática para el simulador (para no modificar la original)
            Gramatica gramaticaParaSimulador = new Gramatica(gramatica);

            // Crear el simulador descendente con la copia de la gramática
            PanelSimuladorDesc simulador = new PanelSimuladorDesc(gramaticaParaSimulador, tabPane, bundle, simuladorId);

            // Crear la pestaña del simulador con el título correcto
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

            // Saltar directamente al paso 6 (índice 5)
            simulador.cambiarPaso(5);

            // Reasignar numeración para reflejar los cambios
            TabManager.reasignarNumerosGruposGramatica(tabPane);
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
                    mostrarError("Error", "El escritorio no es compatible");
                }
            } catch (IOException e) {
                mostrarError("Error", "No se pudo abrir el manual: " + e.getMessage());
            }
        } else {
            mostrarError("Error", "El archivo de manual no existe");
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
                    mostrarError("Error", "El escritorio no es compatible");
                }
            } catch (IOException e) {
                mostrarError("Error", "No se pudo abrir el tutorial: " + e.getMessage());
            }
        } else {
            mostrarError("Error", "El archivo de tutorial no existe");
        }
    }

    /**
     * Sale de la aplicación
     */
    private void salirAplicacion() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "msg.confirmar.salir",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("title.menu");
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
        String resumen = bundle != null ? bundle.getString("editor.msg.validar.errores") + " (" + errores.size() + ")" : "Validation errors (" + errores.size() + ")";
        StringBuilder detalle = new StringBuilder();
        for (int i = 0; i < errores.size(); i++) {
            detalle.append(i + 1).append(". ").append(errores.get(i)).append("\n\n");
        }

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(bundle != null ? bundle.getString("editor.dialog.validar.error.titulo") : "Validation Error");
        alert.setHeaderText(bundle != null ? bundle.getString("editor.dialog.validar.error.header") : "Errors found:");
        alert.setContentText(resumen);

        // Expandir el diálogo para mostrar todo el texto
        TextArea textArea = new TextArea(detalle.toString());
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
        try {
            javafx.scene.control.DialogPane dp = alert.getDialogPane();
            dp.setExpanded(true);
            dp.setExpandableContent(gridPane);
            ((javafx.scene.control.Button) dp.lookupButton(ButtonType.OK)).setText(bundle != null ? bundle.getString("button.aceptar") : "Accept");
            javafx.scene.control.Button details = (javafx.scene.control.Button) dp.lookup(".details-button");
            if (details != null && bundle != null) {
                String txt = bundle.getString(dp.isExpanded() ? "dialog.ocultar.detalles" : "dialog.mostrar.detalles");
                details.setText(txt);
                javafx.application.Platform.runLater(() -> {
                    details.setText(bundle.getString(dp.isExpanded() ? "dialog.ocultar.detalles" : "dialog.mostrar.detalles"));
                });
                dp.expandedProperty().addListener((obs, was, isNow) -> {
                    String t = bundle.getString(isNow ? "dialog.ocultar.detalles" : "dialog.mostrar.detalles");
                    details.setText(t);
                    javafx.application.Platform.runLater(() -> details.setText(t));
                });
                details.textProperty().addListener((o, oldV, newV) -> {
                    String desired = bundle.getString(dp.isExpanded() ? "dialog.ocultar.detalles" : "dialog.mostrar.detalles");
                    if (!desired.equals(newV)) {
                        details.setText(desired);
                    }
                });
            }
        } catch (Exception ignored) {}
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