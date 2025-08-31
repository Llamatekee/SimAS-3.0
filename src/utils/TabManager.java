package utils;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import java.util.*;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.stage.Window;
import javafx.scene.Scene;

public class TabManager {
    private static final Map<TabPane, Map<Class<?>, Tab>> tabInstances = new HashMap<>();
    private static final Map<TabPane, Map<String, List<Tab>>> parentChildRelations = new HashMap<>();
    private static final Map<TabPane, Map<String, String>> elementoToGrupo = new HashMap<>(); // Mapea editorId/simuladorId -> grupoId
    private static final Map<TabPane, Map<String, Integer>> gruposGramatica = new HashMap<>(); // Mapea grupoId -> numeroGrupo
    private static final Map<TabPane, java.util.ResourceBundle> resourceBundles = new HashMap<>();
    
    // Contador global para generar IDs únicos de grupo
    public static int contadorGrupos = 0;

    public static Tab getOrCreateTab(TabPane tabPane, Class<?> tabType, String title, Object content) {
        return getOrCreateTab(tabPane, tabType, title, content, null, null);
    }

    public static Tab getOrCreateTab(TabPane tabPane, Class<?> tabType, String title, Object content, String parentId, String childId) {
        
        // Inicializar el mapa para este TabPane si no existe
        tabInstances.computeIfAbsent(tabPane, k -> new HashMap<>());
        parentChildRelations.computeIfAbsent(tabPane, k -> new HashMap<>());
        elementoToGrupo.computeIfAbsent(tabPane, k -> new HashMap<>());
        gruposGramatica.computeIfAbsent(tabPane, k -> new HashMap<>());

        // Si es una pestaña hija, verificar que estamos en la ventana correcta del padre
        if (parentId != null) {
            // Buscar la ventana que contiene el padre
            TabPane correctTabPane = null;
            Window currentWindow = tabPane.getScene().getWindow();
            
            // Primero verificar la ventana actual
            for (Tab tab : tabPane.getTabs()) {
                if (tab.getUserData() != null && tab.getUserData().toString().equals(parentId)) {
                    correctTabPane = tabPane;
                    break;
                }
            }
            
            // Si no está en la ventana actual, buscar en otras ventanas
            if (correctTabPane == null) {
                for (Window window : Window.getWindows()) {
                    if (window instanceof Stage && window != currentWindow) {
                        Scene scene = ((Stage) window).getScene();
                        if (scene != null) {
                            for (Node node : scene.getRoot().lookupAll(".tab-pane")) {
                                if (node instanceof TabPane) {
                                    TabPane otherTabPane = (TabPane) node;
                                    for (Tab tab : otherTabPane.getTabs()) {
                                        if (tab.getUserData() != null && 
                                            tab.getUserData().toString().equals(parentId)) {
                                            // Encontramos el padre en otra ventana
                                            return getOrCreateTab(otherTabPane, tabType, title, content, parentId, childId);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Obtener el mapa de pestañas para este TabPane
        Map<Class<?>, Tab> paneTabs = tabInstances.get(tabPane);

        // Para editores, simuladores independientes y pestañas hijas de editores/simuladores, permitir múltiples instancias (no usar caché)
        boolean isChildOfEditor = (parentId != null && parentId.startsWith("editor_")) || 
                                 (childId != null && (childId.contains("editor_") || 
                                                     childId.contains("creacion_") ||
                                                     childId.startsWith("terminales_") ||
                                                     childId.startsWith("no_terminales_") ||
                                                     childId.startsWith("producciones_")));
        
        boolean isChildOfSimulator = (parentId != null && parentId.startsWith("simulador_")) ||
                                    (childId != null && (childId.startsWith("gramatica_simulador_") ||
                                                        childId.startsWith("funciones_error_simulador_") ||
                                                        childId.startsWith("derivacion_") ||
                                                        childId.startsWith("arbol_")));
        
        boolean isSimuladorIndependiente = isSimuladorType(tabType) && parentId != null && childId == null;
        
        if (!isEditorType(tabType) && !isChildOfEditor && !isChildOfSimulator && !isSimuladorIndependiente) {
            // Solo usar caché para pestañas que realmente deben ser únicas globalmente
            if (paneTabs.containsKey(tabType)) {
                Tab existingTab = paneTabs.get(tabType);
                if (tabPane.getTabs().contains(existingTab)) {
                    tabPane.getSelectionModel().select(existingTab);
                    return existingTab;
                } else {
                    // Si la pestaña existe en el mapa pero no en el TabPane, eliminarla del mapa
                    paneTabs.remove(tabType);
                }
            }
        } else if (parentId != null && childId != null) {
            // Para pestañas hijas, verificar si ya existe una pestaña hija de este padre específico
            // Buscar solo entre las pestañas que realmente pertenecen a este grupo/padre
            Tab existingChildTab = findChildTabInGroup(tabPane, parentId, childId);
            if (existingChildTab != null) {
                tabPane.getSelectionModel().select(existingChildTab);
                return existingChildTab;
            } else {
            }
        }

        // Crear una nueva pestaña
        javafx.scene.Node nodeContent;
        if (content instanceof javafx.scene.Node) {
            nodeContent = (javafx.scene.Node) content;
        } else if (content instanceof simulador.PanelSimuladorDesc) {
            nodeContent = ((simulador.PanelSimuladorDesc) content).getRoot();
        } else if (content instanceof editor.Editor) {
            nodeContent = ((editor.Editor) content).getRoot();
        } else {
            // Fallback: asumir que es un Node o que tiene un método getRoot()
            try {
                java.lang.reflect.Method getRootMethod = content.getClass().getMethod("getRoot");
                nodeContent = (javafx.scene.Node) getRootMethod.invoke(content);
            } catch (Exception e) {
                // Si todo falla, intentar cast directo
                nodeContent = (javafx.scene.Node) content;
            }
        }
        
        Tab newTab = new Tab(title, nodeContent);
        newTab.setClosable(true);
        
        // Establecer userData para identificar relaciones padre-hijo
        if (childId != null) {
            newTab.setUserData(childId);
        } else if (parentId != null) {
            newTab.setUserData(parentId);
        }
        
        // Añadir listener para cuando se cierre la pestaña
        newTab.setOnClosed(event -> {
            Map<Class<?>, Tab> tabs = tabInstances.get(tabPane);
            if (tabs != null) {
                tabs.remove(tabType);
            }
            
            // Si es una pestaña padre, cerrar también las hijas
            if (parentId != null && childId == null) {
                Map<String, List<Tab>> relations = parentChildRelations.get(tabPane);
                if (relations != null && relations.containsKey(parentId)) {
                    List<Tab> childTabs = new ArrayList<>(relations.get(parentId));
                    for (Tab childTab : childTabs) {
                        if (tabPane.getTabs().contains(childTab)) {
                            javafx.application.Platform.runLater(() -> {
                                tabPane.getTabs().remove(childTab);
                            });
                        }
                    }
                    relations.remove(parentId);
                }
            }
            
            // Si es una pestaña hija, eliminarla de la lista de hijos
            if (childId != null && parentId != null) {
                Map<String, List<Tab>> relations = parentChildRelations.get(tabPane);
                if (relations != null && relations.containsKey(parentId)) {
                    relations.get(parentId).remove(newTab);
                }
            }
            
            // LIMPIAR SOLO EL ELEMENTO INDIVIDUAL DEL GRUPO (no eliminar todo el grupo)
            boolean necesitaRenumeracion = false;
            if (parentId != null && (isEditorType(tabType) || isSimuladorType(tabType))) {
                Map<String, String> elementos = elementoToGrupo.get(tabPane);
                if (elementos != null) {
                    String grupoId = elementos.get(parentId);
                    elementos.remove(parentId); // Solo quitar este elemento, no todo el grupo
                    necesitaRenumeracion = true;
                    
                    // Verificar si el grupo queda vacío DESPUÉS de quitar este elemento
                    boolean grupoVacio = elementos.values().stream().noneMatch(g -> g.equals(grupoId));
                    if (grupoVacio) {
                        Map<String, Integer> grupos = gruposGramatica.get(tabPane);
                        if (grupos != null) {
                            grupos.remove(grupoId);
                        }
                    } 
                }
            }
            
            // Si se eliminó un elemento del grupo o es una pestaña hija relacionada, forzar renumeración
            if (necesitaRenumeracion || (childId != null && (isChildOfEditor || isSimuladorChild(childId)))) {
                // Llamada inmediata
                reasignarNumerosGruposGramatica(tabPane);
                
                // Llamada asíncrona como respaldo para asegurar que se ejecute
                javafx.application.Platform.runLater(() -> {
                    reasignarNumerosGruposGramatica(tabPane);
                });
            }
        });

        // Guardar la nueva pestaña en el mapa (solo para no-editores, no-hijas-de-editores y no-simuladores-independientes)
        if (!isEditorType(tabType) && !isChildOfEditor && !isChildOfSimulator && !isSimuladorIndependiente) {
            paneTabs.put(tabType, newTab);
        }
        
        // ASIGNACIÓN AUTOMÁTICA A GRUPOS - DEBE SER ANTES DEL POSICIONAMIENTO
        if (parentId != null && childId == null) {
            // Esto significa que es un elemento raíz (editor o simulador independiente)
           
            // Verificar si ya está asignado a un grupo (ej: simulador desde editor o asignación previa desde MenuPrincipal)
            Map<String, String> elementos = elementoToGrupo.get(tabPane);
            boolean yaAsignado = (elementos != null && elementos.containsKey(parentId));
            
            if (!yaAsignado) {
                if (isEditorType(tabType)) {
                    // EDITOR INDEPENDIENTE desde menú principal → NUEVO GRUPO
                    asignarElementoANuevoGrupo(tabPane, parentId);
                    
                } else if (isSimuladorType(tabType)) {
                    // SIMULADOR INDEPENDIENTE desde menú principal → NUEVO GRUPO
                    // Solo crear grupo si no está ya asignado (ej: desde MenuPrincipal)
                    asignarElementoANuevoGrupo(tabPane, parentId);
                } else {
                }
            } else {
            }
        } else if (parentId != null && childId != null) {
            // Esto significa que es una pestaña hija - debe heredar el grupo del padre
            Map<String, String> elementos = elementoToGrupo.get(tabPane);
            if (elementos != null) {
                String grupoDelPadre = elementos.get(parentId);
                if (grupoDelPadre != null) {
                    // Asignar la pestaña hija al mismo grupo que el padre
                    elementos.put(childId, grupoDelPadre);
                }
            }
        }
        
        // AHORA calcular la posición donde insertar la pestaña (después de asignar grupos)
        int insertPosition = calcularPosicionInsercion(tabPane, tabType, parentId, childId);
        
        // Si es una pestaña hija, registrar la relación padre-hijo
        if (parentId != null && childId != null) {
            Map<String, List<Tab>> relations = parentChildRelations.get(tabPane);
            relations.computeIfAbsent(parentId, k -> new ArrayList<>()).add(newTab);
        }
        
        // Añadir la pestaña al TabPane en la posición correcta
        if (insertPosition >= tabPane.getTabs().size()) {
            tabPane.getTabs().add(newTab);
        } else {
            tabPane.getTabs().add(insertPosition, newTab);
        }
        
        tabPane.getSelectionModel().select(newTab);
        
        // Debug: mostrar estado después de crear una nueva pestaña
        debugTabPaneState(tabPane);
        
        // Reasignar numeración inmediatamente si se creó un nuevo grupo
        boolean seCreoNuevoGrupo = (parentId != null && childId == null && 
                                   (isEditorType(tabType) || isSimuladorType(tabType)));
        if (seCreoNuevoGrupo) {
            reasignarNumerosGruposGramatica(tabPane);
        }
        
        // Reasignar numeración de grupos después de añadir (asíncrono como respaldo)
        javafx.application.Platform.runLater(() -> {
            reasignarNumerosGruposGramatica(tabPane);
        });
        
        return newTab;
    }
    
    /**
     * Asigna un elemento (editor o simulador independiente) a un NUEVO grupo automáticamente.
     */
    public static void asignarElementoANuevoGrupo(TabPane tabPane, String elementoId) {
        Map<String, String> elementos = elementoToGrupo.get(tabPane);
        Map<String, Integer> grupos = gruposGramatica.get(tabPane);
        
        if (elementos != null && grupos != null) {
            // Solo crear grupo si el elemento no está ya asignado
            if (!elementos.containsKey(elementoId)) {
                // Crear un nuevo grupo único
                String grupoId = "grupo_" + System.currentTimeMillis() + "_" + (++contadorGrupos);
                
                // Asignar número de grupo basado en el número de GRUPOS existentes, no elementos
                int numeroGrupo = contarGruposActivos(tabPane) + 1;
                grupos.put(grupoId, numeroGrupo);
                
                // Asignar el elemento al nuevo grupo
                elementos.put(elementoId, grupoId);
                
            } else {
            }
        }
    }
    
    /**
     * Cuenta el número de grupos activos (grupos que tienen al menos un elemento asignado).
     */
    public static int contarGruposActivos(TabPane tabPane) {
        Map<String, String> elementos = elementoToGrupo.get(tabPane);
        if (elementos == null) {
            return 0;
        }
        
        // Contar grupos únicos que tienen elementos asignados
        Set<String> gruposActivos = new HashSet<>(elementos.values());
        int totalGrupos = gruposActivos.size();
        
        return totalGrupos;
    }
    
    /**
     * Verifica si el tipo de pestaña es un Simulador.
     */
    private static boolean isSimuladorType(Class<?> tabType) {
        boolean result = tabType.getSimpleName().contains("Simulador") || 
               tabType.getName().contains("simulador.PanelSimuladorDesc") ||
               tabType == simulador.PanelSimuladorDesc.class;
               
        return result;
    }
    
    /**
     * Verifica si el contenido de una pestaña es un Simulador.
     */
    public static boolean isSimuladorContent(Object content) {
        return content instanceof simulador.PanelSimuladorDesc ||
               content instanceof simulador.PanelNuevaSimDescPaso ||
               content instanceof simulador.PanelNuevaSimDescPaso1 ||
               content instanceof simulador.PanelNuevaSimDescPaso2 ||
               content instanceof simulador.PanelNuevaSimDescPaso3 ||
               content instanceof simulador.PanelNuevaSimDescPaso4 ||
               content instanceof simulador.PanelNuevaSimDescPaso5 ||
               content instanceof simulador.PanelNuevaSimDescPaso6 ||
               (content != null && content.getClass().getSimpleName().contains("PanelNuevaSimDescPaso"));
    }
    
    /**
     * Verifica si una pestaña es un simulador basándose tanto en contenido como en userData.
     */
    public static boolean isSimuladorTab(Tab tab) {
        // Verificar por tipo de contenido
        if (isSimuladorContent(tab.getContent())) {
            return true;
        }
        
        // Verificar por userData (para simuladores de editores que tienen contenido GridPane)
        if (tab.getUserData() != null) {
            String userData = tab.getUserData().toString();
            if (userData.startsWith("simulador_")) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Verifica si un childId pertenece a un simulador.
     */
    private static boolean isSimuladorChild(String childId) {
        if (childId == null) {
            return false;
        }
        
        // Pestañas hijas típicas de simuladores
        return childId.startsWith("gramatica_simulador_") ||
               childId.startsWith("funciones_error_simulador_") ||
               (childId.startsWith("gramatica_") && childId.contains("simulador_")) ||
               (childId.startsWith("funciones_error_") && childId.contains("simulador_"));
    }
    
    /**
     * Obtiene el ID de grupo para un elemento específico.
     */
    public static String obtenerGrupoDeElemento(TabPane tabPane, String elementoId) {
        Map<String, String> elementos = elementoToGrupo.get(tabPane);
        if (elementos != null) {
            return elementos.get(elementoId);
        }
        return null;
    }
    
    /**
     * Asigna un simulador al mismo grupo que un editor.
     */
    public static void asignarSimuladorAGrupoDeEditor(TabPane tabPane, String simuladorId, String editorId) {
        // Inicializar mapas si no existen
        elementoToGrupo.computeIfAbsent(tabPane, k -> new HashMap<>());
        gruposGramatica.computeIfAbsent(tabPane, k -> new HashMap<>());
        
        String grupoEditor = obtenerGrupoDeElemento(tabPane, editorId);
        
        if (grupoEditor != null) {
            Map<String, String> elementos = elementoToGrupo.get(tabPane);
            if (elementos != null) {
                elementos.put(simuladorId, grupoEditor);
                
                // Debug: mostrar estado después de asignar simulador al grupo del editor
                debugTabPaneState(tabPane);
            }
        }
    }
    
    /**
     * Calcula la posición correcta para una nueva simulación.
     */
    private static int calcularPosicionSimulacion(TabPane tabPane, String simuladorId) {
        // Buscar el simulador padre
        Tab simuladorTab = null;
        Tab ultimaSimulacionTab = null;
        
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() != null) {
                String userData = tab.getUserData().toString();
                // Encontrar el simulador padre
                if (userData.equals(simuladorId)) {
                    simuladorTab = tab;
                }
                // Encontrar la última simulación de este simulador
                if (tab.getContent() instanceof simulador.SimulacionFinal) {
                    simulador.SimulacionFinal sim = (simulador.SimulacionFinal) tab.getContent();
                    if (sim.getSimuladorPadreId() != null && sim.getSimuladorPadreId().equals(simuladorId)) {
                        ultimaSimulacionTab = tab;
                    }
                }
            }
        }
        
        if (simuladorTab == null) return tabPane.getTabs().size();
        
        // Si no hay simulaciones previas, insertar después del simulador
        if (ultimaSimulacionTab == null) {
            return tabPane.getTabs().indexOf(simuladorTab) + 1;
        }
        
        // Si hay simulaciones existentes, insertar después de la última y sus auxiliares
        int insertPos = tabPane.getTabs().indexOf(ultimaSimulacionTab) + 1;
        
        // Buscar pestañas auxiliares de la última simulación
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() != null) {
                String userData = tab.getUserData().toString();
                if ((userData.startsWith("derivacion_") || userData.startsWith("arbol_")) &&
                    userData.endsWith(((simulador.SimulacionFinal)ultimaSimulacionTab.getContent()).simulacionId)) {
                    insertPos = tabPane.getTabs().indexOf(tab) + 1;
                }
            }
        }
        
        return insertPos;
    }

    /**
     * Calcula la posición de inserción para diferentes tipos de pestañas.
     */
    public static int calcularPosicionInsercion(TabPane tabPane, Class<?> tabType, String parentId, String childId) {
        // Si es una pestaña hija, usar la lógica existente
        if (parentId != null && childId != null) {
            // IMPORTANTE: Si es un SIMULADOR, NO usar la lógica de hija normal
            if (childId.startsWith("simulador_")) {
                // Para simuladores hijos de editor, usar la lógica especial
                return calcularPosicionSimuladorInteligente(tabPane, parentId);
            }

            // Si es una simulación, usar la lógica específica
            if (childId.startsWith("simulacion_")) {
                return calcularPosicionSimulacion(tabPane, parentId);
            }

            Tab parentTab = findTabByUserData(tabPane, parentId);
            if (parentTab != null) {
                return calcularPosicionHija(tabPane, parentTab, childId, parentId);
            }
        }

        // Si es un Editor, colocarlo después del menú principal
        if (isEditorType(tabType)) {
            return calcularPosicionEditor(tabPane);
        }

        // Si es un Simulador (independiente o hijo de editor), posicionarlo correctamente
        if (isSimuladorType(tabType) && parentId != null) {
            return calcularPosicionSimuladorInteligente(tabPane, parentId);
        }

        // Para otros tipos, al final
        return tabPane.getTabs().size();
    }
    
    /**
     * Calcula la posición correcta para un Simulador, considerando si pertenece a un grupo existente.
     */
    private static int calcularPosicionSimuladorInteligente(TabPane tabPane, String simuladorId) {
        // Verificar si este simulador debe ir en un grupo existente
        Map<String, String> elementos = elementoToGrupo.get(tabPane);
        String grupoDelSimulador = (elementos != null) ? elementos.get(simuladorId) : null;
        
        if (grupoDelSimulador != null) {
            // Verificar si es un simulador DE EDITOR (debe tener un editor en el mismo grupo)
            boolean esSimuladorDeEditor = false;
            if (elementos != null) {
                for (Map.Entry<String, String> entry : elementos.entrySet()) {
                    String elementoId = entry.getKey();
                    String grupoId = entry.getValue();
                    
                    if (grupoId.equals(grupoDelSimulador) && elementoId.startsWith("editor_")) {
                        esSimuladorDeEditor = true;
                        break;
                    }
                }
            }
            
            if (esSimuladorDeEditor) {
                // Simulador DE EDITOR: posicionar después del último elemento del grupo
                return calcularPosicionDentroDeGrupo(tabPane, grupoDelSimulador);
            } else {
                // Simulador INDEPENDIENTE: aunque tenga grupo, va al final
                return calcularPosicionSimuladorIndependiente(tabPane);
            }
        } else {
            // Simulador sin grupo: independiente, va al final
            return calcularPosicionSimuladorIndependiente(tabPane);
        }
    }
    
    /**
     * Calcula la posición para un simulador independiente: SIEMPRE al final para mantener grupos unidos.
     */
    private static int calcularPosicionSimuladorIndependiente(TabPane tabPane) {
        // Buscar la posición del menú principal para asegurar que el simulador vaya después
        int menuPosition = -1;
        
        for (int i = 0; i < tabPane.getTabs().size(); i++) {
            Tab tab = tabPane.getTabs().get(i);
            String tabText = tab.getText();
            
            // Identificar el menú principal por su título
            if (tabText != null && (tabText.contains("Menú") || tabText.contains("Menu") || 
                                   tabText.contains("Principal") || tabText.contains("Main"))) {
                menuPosition = i;
                break;
            }
        }
        
        // Los simuladores independientes van SIEMPRE al final
        int posicionFinal;
        if (menuPosition != -1) {
            // Menú encontrado: ir al final del TabPane
            posicionFinal = tabPane.getTabs().size();
        } else {
            // Menú no encontrado: usar posición 1 como mínimo de seguridad
            posicionFinal = Math.max(1, tabPane.getTabs().size());
        }
        
        return posicionFinal;
    }
    
    /**
     * Calcula la posición dentro de un grupo específico (después del último elemento del grupo).
     */
    private static int calcularPosicionDentroDeGrupo(TabPane tabPane, String grupoId) {
        Map<String, String> elementos = elementoToGrupo.get(tabPane);
        if (elementos == null) {
            return calcularPosicionSeguaDespuesDelMenu(tabPane);
        }

        // Buscar si hay un asistente de editor en este grupo
        int posicionEditor = -1;
        String editorId = null;

        for (int i = 0; i < tabPane.getTabs().size(); i++) {
            Tab tab = tabPane.getTabs().get(i);
            String userData = (tab.getUserData() != null) ? tab.getUserData().toString() : null;

            if (userData != null) {
                // Verificar si es un elemento raíz del grupo
                String grupoDeUsuario = elementos.get(userData);
                if (grupoId.equals(grupoDeUsuario) && userData.startsWith("editor_")) {
                    posicionEditor = i;
                    editorId = userData;
                    break;
                }
            }
        }

        // Si hay un asistente de editor en el grupo, colocar nuevas pestañas AL FINAL del grupo
        if (posicionEditor != -1 && editorId != null) {
            // Encontrar la última posición de todas las pestañas relacionadas con el editor
            int ultimaPosicionEditorGroup = posicionEditor;

            for (int i = posicionEditor + 1; i < tabPane.getTabs().size(); i++) {
                Tab tab = tabPane.getTabs().get(i);
                String userData = (tab.getUserData() != null) ? tab.getUserData().toString() : null;

                if (userData != null) {
                    String grupoDeUsuario = elementos.get(userData);
                    boolean perteneceAlGrupo = grupoId.equals(grupoDeUsuario);
                    boolean esHijaDelEditor = isPestañaHijaDeElemento(userData, editorId);

                    if (perteneceAlGrupo || esHijaDelEditor) {
                        ultimaPosicionEditorGroup = i;
                    }
                }
            }

            return ultimaPosicionEditorGroup + 1;
        }

        // Si no hay editor en el grupo, usar la lógica normal (al final del grupo)
        int ultimaPosicionDelGrupo = -1;

        for (int i = 0; i < tabPane.getTabs().size(); i++) {
            Tab tab = tabPane.getTabs().get(i);
            String userData = (tab.getUserData() != null) ? tab.getUserData().toString() : null;

            if (userData != null) {
                // Verificar si esta pestaña pertenece al grupo
                boolean perteneceAlGrupo = false;

                // Verificar si es un elemento raíz del grupo
                String grupoDeUsuario = elementos.get(userData);
                if (grupoId.equals(grupoDeUsuario)) {
                    perteneceAlGrupo = true;
                }

                // Verificar si es una pestaña hija de algún elemento del grupo
                if (!perteneceAlGrupo) {
                    for (Map.Entry<String, String> entry : elementos.entrySet()) {
                        if (grupoId.equals(entry.getValue())) {
                            String elementoDelGrupo = entry.getKey();
                            if (isPestañaHijaDeElemento(userData, elementoDelGrupo)) {
                                perteneceAlGrupo = true;
                                break;
                            }
                        }
                    }
                }

                if (perteneceAlGrupo) {
                    ultimaPosicionDelGrupo = i;
                }
            }
        }

        if (ultimaPosicionDelGrupo == -1) {
            // No se encontraron elementos del grupo en el TabPane
            // Esto puede pasar cuando es el primer elemento del grupo
            // Usar una posición segura después del menú
            return calcularPosicionSeguaDespuesDelMenu(tabPane);
        }

        int nuevaPosicion = ultimaPosicionDelGrupo + 1;
        return nuevaPosicion;
    }
    
    /**
     * Calcula una posición segura después del menú principal.
     */
    public static int calcularPosicionSeguaDespuesDelMenu(TabPane tabPane) {
        // Buscar la posición del menú principal
        for (int i = 0; i < tabPane.getTabs().size(); i++) {
            Tab tab = tabPane.getTabs().get(i);
            String tabText = tab.getText();
            
            if (tabText != null && (tabText.contains("Menú") || tabText.contains("Menu") || 
                                   tabText.contains("Principal") || tabText.contains("Main"))) {
                int posicionSegura = i + 1;
                return posicionSegura;
            }
        }
        
        // Si no se encuentra el menú, usar posición 1
        return 1;
    }
    
    /**
     * Verifica si un childId realmente pertenece a un parentId específico basado en los patrones de ID.
     */
    public static boolean isPestañaHijaDeElemento(String childId, String parentId) {
        if (childId == null || parentId == null) {
            return false;
        }
        
        // Para simuladores
        if (parentId.startsWith("simulador_")) {
            String simuladorBaseId = extractBaseId(parentId);
            
            // Pestañas hijas directas del simulador (gramática y funciones de error)
            if (childId.equals("gramatica_" + parentId) || 
                childId.equals("funciones_error_" + parentId)) {
                return true;
            }
            
            // Simulaciones del simulador
            if (childId.startsWith("simulacion_") && childId.contains(simuladorBaseId)) {
                return true;
            }
            
            return false;
        }
        
        // Para simulaciones (derivaciones y árboles)
        if (parentId.startsWith("simulacion_")) {
            // Extraer el ID base de la simulación (que incluye el ID del simulador padre)
            String simulacionBaseId = parentId;
            
            // Verificar si es una derivación o árbol de esta simulación específica
            return (childId.startsWith("derivacion_") || childId.startsWith("arbol_")) &&
                   childId.endsWith(simulacionBaseId);
        }
        
        // Para editores (mantener la lógica existente)
        String parentBaseId = extractBaseId(parentId);
        
        // Para pestañas de creación directas
        if (childId.startsWith("creacion_") && childId.contains(parentBaseId)) {
            return true;
        }
        
        // Para pestañas de símbolos
        if ((childId.startsWith("terminales_") || childId.startsWith("no_terminales_") || childId.startsWith("producciones_")) &&
            childId.contains("creacion_" + parentBaseId)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Calcula la posición correcta para un Editor: SIEMPRE al final de todos los grupos existentes.
     */
    private static int calcularPosicionEditor(TabPane tabPane) {
        // Buscar la posición del menú principal para asegurar que el editor vaya después
        int menuPosition = -1;
        
        for (int i = 0; i < tabPane.getTabs().size(); i++) {
            Tab tab = tabPane.getTabs().get(i);
            String tabText = tab.getText();
            
            // Identificar el menú principal por su título
            if (tabText != null && (tabText.contains("Menú") || tabText.contains("Menu") || 
                                   tabText.contains("Principal") || tabText.contains("Main"))) {
                menuPosition = i;
                break;
            }
        }
        
        // Los nuevos editores van SIEMPRE al final
        int posicionFinal;
        if (menuPosition != -1) {
            // Menú encontrado: ir al final del TabPane
            posicionFinal = tabPane.getTabs().size();
        } else {
            // Menú no encontrado: usar posición 1 como mínimo de seguridad
            posicionFinal = Math.max(1, tabPane.getTabs().size());
        }
        
        return posicionFinal;
    }
    
    /**
     * Verifica si el tipo de pestaña es un Editor.
     */
    private static boolean isEditorType(Class<?> tabType) {
        return tabType.getSimpleName().contains("Editor") || 
               tabType.getName().contains("editor.Editor") ||
               tabType == editor.Editor.class;
    }
    
    /**
     * Verifica si el contenido de una pestaña es un Editor.
     */
    public static boolean isEditorContent(Object content) {
        return content instanceof editor.Editor;
    }

    public static void closeTab(TabPane tabPane, Class<?> tabType) {
        Map<Class<?>, Tab> paneTabs = tabInstances.get(tabPane);
        if (paneTabs != null) {
            Tab tab = paneTabs.get(tabType);
            if (tab != null && tabPane.getTabs().contains(tab)) {
                tabPane.getTabs().remove(tab);
                paneTabs.remove(tabType);
            }
        }
    }

    public static boolean hasTab(TabPane tabPane, Class<?> tabType) {
        Map<Class<?>, Tab> paneTabs = tabInstances.get(tabPane);
        if (paneTabs != null) {
            Tab tab = paneTabs.get(tabType);
            return tab != null && tabPane.getTabs().contains(tab);
        }
        return false;
    }
    
    /**
     * Cierra todas las pestañas hijas asociadas a un elemento padre.
     * Solo cierra las pestañas en el TabPane especificado.
     */
    public static void closeChildTabs(TabPane tabPane, String parentId) {
        if (tabPane == null || parentId == null) return;

        // Obtener las relaciones padre-hijo para este TabPane específico
        Map<String, List<Tab>> relations = getParentChildRelations(tabPane);

        // Si hay pestañas hijas para este padre en este TabPane específico
        if (relations.containsKey(parentId)) {
            // Crear una copia de la lista para evitar ConcurrentModificationException
            List<Tab> childTabs = new ArrayList<>(relations.get(parentId));

            // Usar Platform.runLater para modificar la UI thread de manera segura
            Platform.runLater(() -> {
                // Cerrar cada pestaña hija
                for (Tab childTab : childTabs) {
                    if (tabPane.getTabs().contains(childTab)) {
                        // Si es una simulación, cerrar también sus derivaciones y árboles
                        String childId = childTab.getUserData() != null ? childTab.getUserData().toString() : null;
                        if (childId != null && childId.startsWith("simulacion_")) {
                            closeChildTabs(tabPane, childId); // Cerrar derivaciones y árboles
                        }
                        tabPane.getTabs().remove(childTab);
                    }
                }
                // Limpiar la relación después de cerrar todas las pestañas
                relations.remove(parentId);
            });
        }
    }

    /**
     * Cierra TODAS las pestañas descendientes (hijas, nietas, etc.) de un elemento.
     * Útil cuando se cierra un abuelo y se quieren cerrar todos sus descendientes.
     */
    public static void closeAllDescendantTabs(TabPane tabPane, String ancestorId) {
        if (tabPane == null || ancestorId == null) return;

        // Primero cerrar las pestañas hijas directas del ancestro
        closeChildTabs(tabPane, ancestorId);

        // Si el ancestro es un simulador, necesitamos cerrar también las simulaciones y sus derivaciones/árboles
        if (ancestorId.startsWith("simulador_")) {
            // Buscar todas las simulaciones que pertenecen a este simulador
            for (Tab tab : new ArrayList<>(tabPane.getTabs())) {
                if (tab.getUserData() != null) {
                    String tabId = tab.getUserData().toString();
                    if (tabId.startsWith("simulacion_") && tabId.contains(ancestorId)) {
                        // Esta es una simulación de este simulador, cerrar también sus derivaciones y árboles
                        closeChildTabs(tabPane, tabId);
                        // Cerrar la simulación misma
                        Platform.runLater(() -> {
                            if (tabPane.getTabs().contains(tab)) {
                                tabPane.getTabs().remove(tab);
                            }
                        });
                    }
                }
            }
        }
    }
    
    private static Tab findTabByUserData(TabPane tabPane, String userData) {
        for (Tab tab : tabPane.getTabs()) {
            if (userData.equals(tab.getUserData())) {
                return tab;
            }
        }
        return null;
    }

    /**
     * Busca una pestaña hija existente para un padre específico y tipo de contenido.
     * Solo busca entre las pestañas que realmente pertenecen al grupo/padre especificado.
     */
    private static Tab findChildTabInGroup(TabPane tabPane, String parentId, String childId) {
        
        // Obtener el grupo del padre
        String grupoDelPadre = obtenerGrupoDeElemento(tabPane, parentId);
        
        // Buscar en las relaciones padre-hijo registradas DENTRO DEL MISMO GRUPO
        Map<String, List<Tab>> relations = parentChildRelations.get(tabPane);
        if (relations != null && relations.containsKey(parentId)) {
            List<Tab> childTabs = relations.get(parentId);
            for (Tab childTab : childTabs) {
                if (childTab.getUserData() != null && 
                    childTab.getUserData().toString().equals(childId) &&
                    tabPane.getTabs().contains(childTab)) {
                    
                    // Verificar que la pestaña hija realmente pertenezca al grupo correcto
                    if (verificarPestañaPerteneceAGrupo(childTab, grupoDelPadre, parentId)) {
                        return childTab;
                    } else {
                    }
                }
            }
        } else {
        }
        
        // Si no se encuentra en las relaciones registradas, NO buscar más
        // Esto evita la detección cruzada entre grupos
        return null;
    }
    
    /**
     * Verifica que una pestaña hija realmente pertenezca al grupo del padre especificado.
     */
    private static boolean verificarPestañaPerteneceAGrupo(Tab childTab, String grupoDelPadre, String parentId) {
        
        if (childTab.getUserData() == null || grupoDelPadre == null) {
            return false;
        }
        
        String childId = childTab.getUserData().toString();
        
        // Para pestañas de gramática y funciones de error de simuladores
        if (childId.startsWith("gramatica_") || childId.startsWith("funciones_error_")) {
            // Extraer el simuladorId del childId correctamente
            String simuladorIdFromChild;
            if (childId.startsWith("gramatica_simulador_")) {
                // De "gramatica_simulador_1234" extraer "simulador_1234"
                simuladorIdFromChild = childId.replace("gramatica_", "");
            } else if (childId.startsWith("funciones_error_simulador_")) {
                // De "funciones_error_simulador_1234" extraer "simulador_1234"
                simuladorIdFromChild = childId.replace("funciones_error_", "");
            } else {
                // Fallback: usar el método original para casos edge
                simuladorIdFromChild = childId.substring(childId.lastIndexOf("_") + 1);
            }
            
            
            // Verificar que el simulador del childId pertenezca al mismo grupo
            String grupoDelSimulador = obtenerGrupoDeElemento(childTab.getTabPane(), simuladorIdFromChild);
            boolean pertenece = grupoDelPadre.equals(grupoDelSimulador);
            return pertenece;
        }
        
        // Para pestañas de creación y símbolos de editores
        boolean resultado = isChildIdBelongsToParent(childId, parentId);
        return resultado;
    }
    
    /**
     * Verifica si un childId realmente pertenece a un parentId específico basado en los patrones de ID.
     */
    private static boolean isChildIdBelongsToParent(String childId, String parentId) {
        
        if (childId == null || parentId == null) {
            return false;
        }
        
        
        // Extraer el identificador base del parentId (ej: "editor_1234" -> "1234")
        String parentBaseId = extractBaseId(parentId);
        
        // Para pestañas de creación directas (ej: "creacion_1234")
        if (childId.startsWith("creacion_") && childId.contains(parentBaseId)) {
            return true;
        }
        
        // Para pestañas de símbolos (ej: "terminales_creacion_1234")
        if ((childId.startsWith("terminales_") || childId.startsWith("no_terminales_") || childId.startsWith("producciones_")) &&
            childId.contains("creacion_" + parentBaseId)) {
            return true;
        }
        
        // Para pestañas de derivación y árbol de simulaciones
        if ((childId.startsWith("derivacion_") || childId.startsWith("arbol_")) && 
            childId.endsWith(parentId)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Extrae el identificador base de un ID (la parte numérica/temporal).
     */
    private static String extractBaseId(String id) {
        if (id == null) return "";
        
        // Para IDs como "editor_1234_5" o "creacion_1234", extraer la parte numérica
        String[] parts = id.split("_");
        if (parts.length >= 2) {
            return parts[1]; // Retornar la parte numérica (ej: "1234")
        }
        return id;
    }

    /**
     * Calcula la posición correcta para una pestaña hija basada en prioridades.
     */
    private static int calcularPosicionHija(TabPane tabPane, Tab parentTab, String childId, String parentId) {
        int parentIndex = tabPane.getTabs().indexOf(parentTab);
        int insertPosition = parentIndex + 1;
        
        // Definir prioridades para diferentes tipos de pestañas hijas
        int prioridad = obtenerPrioridadPestaña(childId);
        
        // Buscar la posición correcta entre las pestañas hijas existentes
        Map<String, List<Tab>> relations = parentChildRelations.get(tabPane);
        if (relations.containsKey(parentId)) {
            List<Tab> siblings = relations.get(parentId);
            
            for (Tab sibling : siblings) {
                int siblingIndex = tabPane.getTabs().indexOf(sibling);
                if (siblingIndex > parentIndex) {
                    String siblingId = sibling.getUserData().toString();
                    int siblingPrioridad = obtenerPrioridadPestaña(siblingId);
                    
                    if (prioridad <= siblingPrioridad) {
                        // Esta pestaña tiene mayor o igual prioridad, insertarla aquí
                        break;
                    } else {
                        // La pestaña hermana tiene mayor prioridad, continuar buscando
                        insertPosition = siblingIndex + 1;
                    }
                }
            }
        }
        
        return insertPosition;
    }
    
    /**
     * Obtiene la prioridad de una pestaña basada en su identificador.
     * Menor número = mayor prioridad (más cerca del padre).
     */
    private static int obtenerPrioridadPestaña(String childId) {
        if (childId.startsWith("funciones_error_")) {
            return 1; // Alta prioridad - va justo después del simulador
        } else if (childId.startsWith("gramatica_")) {
            return 2; // Prioridad media - va después de funciones de error
        } else if (childId.startsWith("creacion_")) {
            return 1; // Alta prioridad para pestañas de creación
        } else if (childId.startsWith("no_terminales_") || childId.startsWith("terminales_") || childId.startsWith("producciones_")) {
            return 2; // Prioridad media para pestañas de modificación
        }
        return 999; // Prioridad baja por defecto
    }

    /**
     * Reasigna los números de los grupos de gramática según su orden de creación, no su posición en el TabPane.
     * Cada grupo puede contener editores, simuladores y sus pestañas relacionadas.
     * La numeración es independiente para cada ventana.
     */
    public static void reasignarNumerosGruposGramatica(TabPane tabPane) {
        if (tabPane == null) return;
        
        // Obtener mapas del TabPane
        Map<String, String> elementos = elementoToGrupo.get(tabPane);
        Map<String, Integer> grupos = gruposGramatica.get(tabPane);
        
        if (elementos == null || grupos == null) {
            return;
        }

        // 1. RECOLECTAR GRUPOS ACTIVOS EN ORDEN DE APARICIÓN EN LAS PESTAÑAS
        Set<String> gruposActivos = new LinkedHashSet<>();
        
        // Recorrer pestañas en orden para mantener la secuencia visual
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() != null) {
                String elementId = tab.getUserData().toString();
                String grupoId = elementos.get(elementId);
                if (grupoId != null && !gruposActivos.contains(grupoId)) {
                    gruposActivos.add(grupoId);
                }
            }
        }

        // 2. LIMPIAR Y REASIGNAR NÚMEROS SECUENCIALMENTE DESDE 1
        Map<String, Integer> nuevosNumeros = new HashMap<>();
        int numeroSecuencial = 1;
        
        for (String grupoId : gruposActivos) {
            nuevosNumeros.put(grupoId, numeroSecuencial);
            numeroSecuencial++;
        }
        
        // Actualizar el mapa de grupos con los nuevos números
        grupos.clear();
        grupos.putAll(nuevosNumeros);

        // 3. ELIMINAR ELEMENTOS HUÉRFANOS (que ya no tienen pestañas)
        Set<String> elementosHuerfanos = new HashSet<>();
        for (String elementId : elementos.keySet()) {
            boolean existePestaña = false;
            for (Tab tab : tabPane.getTabs()) {
                if (tab.getUserData() != null && tab.getUserData().toString().equals(elementId)) {
                    existePestaña = true;
                    break;
                }
            }
            if (!existePestaña) {
                elementosHuerfanos.add(elementId);
            }
        }
        
        for (String elementoHuerfano : elementosHuerfanos) {
            String grupoHuerfano = elementos.remove(elementoHuerfano);
            
            // Si el grupo se queda sin elementos, eliminarlo también
            if (grupoHuerfano != null) {
                boolean grupoTieneOtrosElementos = elementos.values().stream()
                    .anyMatch(g -> g.equals(grupoHuerfano));
                if (!grupoTieneOtrosElementos) {
                    grupos.remove(grupoHuerfano);
                }
            }
        }

        // 4. RECALCULAR NÚMEROS DESPUÉS DE LIMPIAR HUÉRFANOS
        if (!elementosHuerfanos.isEmpty()) {
            gruposActivos.clear();
            nuevosNumeros.clear();
            numeroSecuencial = 1;
            
            // Recolectar grupos activos después de limpiar huérfanos
            for (Tab tab : tabPane.getTabs()) {
                if (tab.getUserData() != null) {
                    String elementId = tab.getUserData().toString();
                    String grupoId = elementos.get(elementId);
                    if (grupoId != null && !gruposActivos.contains(grupoId)) {
                        gruposActivos.add(grupoId);
                    }
                }
            }
            
            // Reasignar números secuenciales
            for (String grupoId : gruposActivos) {
                nuevosNumeros.put(grupoId, numeroSecuencial);
                numeroSecuencial++;
            }
            
            // Actualizar nuevamente el mapa
            grupos.clear();
            grupos.putAll(nuevosNumeros);
        }

        // 5. ACTUALIZAR SIMULACIONES CON LOS NUEVOS NÚMEROS
        actualizarNumerosGrupoSimulaciones(tabPane);
        
        // 6. ACTUALIZAR TÍTULOS DE TODAS LAS PESTAÑAS
        actualizarTitulosPestañas(tabPane);
    }
    
    /**
     * Actualiza los números de grupo de todas las simulaciones en un TabPane
     */
    private static void actualizarNumerosGrupoSimulaciones(TabPane tabPane) {
        if (tabPane == null) return;
        
        Map<String, String> elementos = elementoToGrupo.get(tabPane);
        Map<String, Integer> grupos = gruposGramatica.get(tabPane);
        
        if (elementos == null || grupos == null) return;
        
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getContent() instanceof simulador.SimulacionFinal) {
                simulador.SimulacionFinal sim = (simulador.SimulacionFinal) tab.getContent();
                String simuladorId = sim.getSimuladorPadreId();
                if (simuladorId != null) {
                    String grupoId = elementos.get(simuladorId);
                    if (grupoId != null) {
                        Integer nuevoNumero = grupos.get(grupoId);
                        if (nuevoNumero != null) {
                            sim.setGrupoId(grupoId);
                            sim.setNumeroGrupo(nuevoNumero);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Actualiza el título de un editor con su número de grupo.
     */
    private static void actualizarTituloEditor(Tab tab, Integer numeroGrupo, boolean mostrarGrupo) {
        if (tab == null || numeroGrupo == null) return;
        String tituloBase = obtenerTituloBaseEditor(tab.getTabPane());
        tab.setText(mostrarGrupo ? numeroGrupo + "-" + tituloBase : tituloBase);
    }
    
    /**
     * Actualiza el título de un simulador con su nútulo de grupo.
     */
    private static void actualizarTituloSimulador(Tab tab, Integer numeroGrupo, boolean mostrarGrupo) {
        if (tab == null || numeroGrupo == null) return;
        
        // Obtener el título base específico para este simulador
        String tituloBase;
        try {
            java.util.ResourceBundle bundle = resourceBundles.get(tab.getTabPane());
            if (bundle != null && tab.getUserData() != null) {
                String userData = tab.getUserData().toString();
                // Si es un simulador, verificar si está en el paso 6 por el título actual
                if (userData.startsWith("simulador_")) {
                    String currentTitle = tab.getText();
                    // Si el título actual contiene "Simulador" sin "Asistente", mantenerlo como paso 6
                    if (currentTitle != null && 
                        currentTitle.contains(bundle.getString("simulador.tab.paso6")) && 
                        !currentTitle.contains(bundle.getString("simulador.asistente"))) {
                        tituloBase = bundle.getString("simulador.tab.paso6");
                    } else {
                        tituloBase = bundle.getString("simulador.asistente");
                    }
                } else {
                    tituloBase = bundle.getString("simulador.asistente");
                }
            } else {
                tituloBase = "Simulador"; // Fallback
            }
        } catch (Exception e) {
            tituloBase = "Simulador"; // Fallback en caso de error
        }
        
        tab.setText(mostrarGrupo ? numeroGrupo + "-" + tituloBase : tituloBase);
    }
    
    /**
     * Actualiza los títulos de las pestañas hijas de un elemento.
     */
    private static void actualizarTitulosPestañasHijas(TabPane tabPane, String elementId, Integer numeroGrupo, boolean mostrarGrupo) {
        
        // Obtener las relaciones padre-hijo para este TabPane
        Map<String, List<Tab>> relations = parentChildRelations.get(tabPane);
        if (relations == null || !relations.containsKey(elementId)) {
            return;
        }
        
        List<Tab> childTabs = relations.get(elementId);
        
        for (Tab tab : childTabs) {
            if (tab.getUserData() == null) {
                continue;
            }
            
            String childId = tab.getUserData().toString();
            
            // Pestañas hijas de editor (terminales, no terminales, producciones)
            if (elementId.startsWith("editor_")) {
                if (childId.startsWith("terminales_")) {
                    String tituloBase = obtenerTituloBaseTerminales(tabPane);
                    String nuevoTitulo = mostrarGrupo ? numeroGrupo + "-" + tituloBase : tituloBase;
                    tab.setText(nuevoTitulo);
                } else if (childId.startsWith("no_terminales_")) {
                    String tituloBase = obtenerTituloBaseNoTerminales(tabPane);
                    String nuevoTitulo = mostrarGrupo ? numeroGrupo + "-" + tituloBase : tituloBase;
                    tab.setText(nuevoTitulo);
                } else if (childId.startsWith("producciones_")) {
                    String tituloBase = obtenerTituloBaseProducciones(tabPane);
                    String nuevoTitulo = mostrarGrupo ? numeroGrupo + "-" + tituloBase : tituloBase;
                    tab.setText(nuevoTitulo);
                } else if (childId.startsWith("creacion_")) {
                    String tituloBase = obtenerTituloBaseParaHija(tabPane, childId);
                    String nuevoTitulo = mostrarGrupo ? numeroGrupo + "-" + tituloBase : tituloBase;
                    tab.setText(nuevoTitulo);
                }
            }
            
            // Pestañas hijas de simulador (gramática original y funciones de error)
            else if (elementId.startsWith("simulador_")) {
                if (childId.equals("gramatica_original_" + elementId)) {
                    String tituloBase = obtenerTituloBaseGramaticaOriginal(tabPane);
                    String nuevoTitulo = mostrarGrupo ? numeroGrupo + "-" + tituloBase : tituloBase;
                    tab.setText(nuevoTitulo);
                } else if (childId.equals("funciones_error_" + elementId)) {
                    String tituloBase = obtenerTituloBaseFuncionesError(tabPane);
                    String nuevoTitulo = mostrarGrupo ? numeroGrupo + "-" + tituloBase : tituloBase;
                    tab.setText(nuevoTitulo);
                }
            }
            
            // Pestañas hijas de simulación (derivación y árbol)
            else if (elementId.startsWith("simulacion_")) {
                if (childId.startsWith("derivacion_")) {
                    String tituloBase = obtenerTituloBaseParaHija(tabPane, childId);
                    // Para pestañas hijas de simulación, combinar grupo e instancia
                    int numeroInstancia = obtenerNumeroInstanciaSimulacion(tabPane, elementId);
                    boolean hayMultiplesSimulaciones = contarSimulacionesDelSimulador(tabPane, elementId) > 1;

                    String nuevoTitulo;
                    if (mostrarGrupo && hayMultiplesSimulaciones) {
                        nuevoTitulo = numeroGrupo + "-" + tituloBase + " (" + numeroInstancia + ")";
                    } else if (mostrarGrupo) {
                        nuevoTitulo = numeroGrupo + "-" + tituloBase;
                    } else if (hayMultiplesSimulaciones) {
                        nuevoTitulo = tituloBase + " (" + numeroInstancia + ")";
                    } else {
                        nuevoTitulo = tituloBase;
                    }
                    tab.setText(nuevoTitulo);
                } else if (childId.startsWith("arbol_")) {
                    String tituloBase = obtenerTituloBaseParaHija(tabPane, childId);
                    // Para pestañas hijas de simulación, combinar grupo e instancia
                    int numeroInstancia = obtenerNumeroInstanciaSimulacion(tabPane, elementId);
                    boolean hayMultiplesSimulaciones = contarSimulacionesDelSimulador(tabPane, elementId) > 1;

                    String nuevoTitulo;
                    if (mostrarGrupo && hayMultiplesSimulaciones) {
                        nuevoTitulo = numeroGrupo + "-" + tituloBase + " (" + numeroInstancia + ")";
                    } else if (mostrarGrupo) {
                        nuevoTitulo = numeroGrupo + "-" + tituloBase;
                    } else if (hayMultiplesSimulaciones) {
                        nuevoTitulo = tituloBase + " (" + numeroInstancia + ")";
                    } else {
                        nuevoTitulo = tituloBase;
                    }
                    tab.setText(nuevoTitulo);
                }
            }
        }
    }
    
    /**
     * Obtiene el título base para editores.
     */
    private static String obtenerTituloBaseEditor(TabPane tabPane) {
        // Intentar usar el ResourceBundle si está disponible
        try {
            java.util.ResourceBundle bundle = resourceBundles.get(tabPane);
            if (bundle != null) {
                return bundle.getString("editor.title");
            }
        } catch (Exception e) {
            // Si no se puede obtener del bundle, usar valor por defecto
        }
        // Usar directamente el nombre corto como fallback
        return "Editor";
    }
    
    /**
     * Obtiene el título base para pestañas de gramática original.
     */
    private static String obtenerTituloBaseGramaticaOriginal(TabPane tabPane) {
        // Intentar usar el ResourceBundle si está disponible
        try {
            java.util.ResourceBundle bundle = resourceBundles.get(tabPane);
            if (bundle != null) {
                return bundle.getString("simulador.gramatica.original");
            }
        } catch (Exception e) {
            // Si no se puede obtener del bundle, usar valor por defecto
        }
        // Usar directamente el nombre corto como fallback
        return "Gramática Original";
    }
    
    /**
     * Obtiene el título base para pestañas de funciones de error.
     */
    private static String obtenerTituloBaseFuncionesError(TabPane tabPane) {
        // Intentar usar el ResourceBundle si está disponible
        try {
            java.util.ResourceBundle bundle = resourceBundles.get(tabPane);
            if (bundle != null) {
                return bundle.getString("simulador.paso4.btn.nueva");
            }
        } catch (Exception e) {
            // Si no se puede obtener del bundle, usar valor por defecto
        }
        // Usar directamente el nombre corto como fallback
        return "Nueva Función Error";
    }
    
    /**
     * Establece el ResourceBundle para un TabPane específico.
     */
    public static void setResourceBundle(TabPane tabPane, java.util.ResourceBundle bundle) {
        resourceBundles.put(tabPane, bundle);

        // Actualizar el menú contextual con el nuevo idioma
        actualizarMenuContextual(tabPane, bundle);
    }
    
    /**
     * Obtiene el número de grupo asignado a un elemento específico.
     * Devuelve 0 si no hay grupo o si el elemento no está asignado a ningún grupo.
     */
    public static int obtenerNumeroGrupo(TabPane tabPane, String elementoId) {
        Map<String, String> elementos = elementoToGrupo.get(tabPane);
        Map<String, Integer> grupos = gruposGramatica.get(tabPane);
        
        if (elementos != null && grupos != null) {
            String grupoId = elementos.get(elementoId);
            if (grupoId != null) {
                // Solo retornar el número de grupo si hay más de un grupo activo
                if (contarGruposActivos(tabPane) > 1) {
                    Integer numeroGrupo = grupos.get(grupoId);
                    if (numeroGrupo != null) {
                        return numeroGrupo;
                    }
                }
            }
        }
        
        return 0;
    }
    
    /**
     * Elimina un elemento de un grupo.
     * Solo afecta al TabPane especificado.
     */
    public static void eliminarElementoDeGrupo(TabPane tabPane, String elementId, String grupoId) {
        if (tabPane == null || elementId == null) return;
        
        Map<String, String> elementos = elementoToGrupo.get(tabPane);
        if (elementos != null) {
            elementos.remove(elementId);
        }
        
        // Si el grupo se queda vacío, eliminarlo
        if (grupoId != null) {
            boolean grupoVacio = true;
            if (elementos != null) {
                for (String grupo : elementos.values()) {
                    if (grupoId.equals(grupo)) {
                        grupoVacio = false;
                        break;
                    }
                }
            }
            if (grupoVacio) {
                // Eliminar el grupo solo para este TabPane
                Map<String, Integer> grupos = gruposGramatica.get(tabPane);
                if (grupos != null) {
                    grupos.remove(grupoId);
                }
            }
        }
        
        // Debug: mostrar estado después de eliminar elemento de grupo
        debugTabPaneState(tabPane);
    }

    /**
     * Reinicia la numeración de grupos, útil cuando se cierran todas las pestañas.
     */
    public static void resetGrupos(TabPane tabPane) {
        if (tabPane == null) return;
        
        // Limpiar los mapas de este TabPane
        elementoToGrupo.computeIfPresent(tabPane, (key, elementos) -> {
            elementos.clear();
            return elementos;
        });
        
        gruposGramatica.computeIfPresent(tabPane, (key, grupos) -> {
            grupos.clear();
            return grupos;
        });
        
        // Reiniciar el contador de grupos
        contadorGrupos = 0;
    }

    /**
     * Asigna un elemento a un grupo específico.
     * @param tabPane El TabPane donde está el elemento
     * @param elementoId El ID del elemento a asignar
     * @param grupoId El ID del grupo al que asignar
     */
    public static void asignarElementoAGrupo(TabPane tabPane, String elementoId, String grupoId) {
        elementoToGrupo.computeIfAbsent(tabPane, k -> new HashMap<>());
        gruposGramatica.computeIfAbsent(tabPane, k -> new HashMap<>());
        
        Map<String, String> elementos = elementoToGrupo.get(tabPane);
        Map<String, Integer> grupos = gruposGramatica.get(tabPane);
        
        if (elementos != null && grupos != null) {
            // Asignar el elemento al grupo
            elementos.put(elementoId, grupoId);
            
            // Si el grupo no tiene número asignado, asignarle uno
            if (!grupos.containsKey(grupoId)) {
                int numeroGrupo = contarGruposActivos(tabPane) + 1;
                grupos.put(grupoId, numeroGrupo);
            }
        }
        
        // Debug: mostrar estado después de asignar elemento a grupo
        debugTabPaneState(tabPane);
    }

    /**
     * Obtiene el mapa de relaciones padre-hijo para un TabPane específico.
     */
    public static Map<String, List<Tab>> getParentChildRelations(TabPane tabPane) {
        return parentChildRelations.computeIfAbsent(tabPane, k -> new HashMap<>());
    }

    /**
     * Obtiene el título base para pestañas de terminales.
     */
    private static String obtenerTituloBaseTerminales(TabPane tabPane) {
        try {
            java.util.ResourceBundle bundle = resourceBundles.get(tabPane);
            if (bundle != null) {
                return bundle.getString("creacion2.tab.modificar.terminales");
            }
        } catch (Exception e) {
            // Si no se puede obtener del bundle, usar valor por defecto
        }
        return "Terminales";
    }

    /**
     * Obtiene el título base para pestañas de no terminales.
     */
    private static String obtenerTituloBaseNoTerminales(TabPane tabPane) {
        try {
            java.util.ResourceBundle bundle = resourceBundles.get(tabPane);
            if (bundle != null) {
                return bundle.getString("creacion2.tab.modificar.no.terminales");
            }
        } catch (Exception e) {
            // Si no se puede obtener del bundle, usar valor por defecto
        }
        return "No Terminales";
    }

    /**
     * Obtiene el título base para pestañas de producciones.
     */
    private static String obtenerTituloBaseProducciones(TabPane tabPane) {
        try {
            java.util.ResourceBundle bundle = resourceBundles.get(tabPane);
            if (bundle != null) {
                return bundle.getString("creacion3.tab.modificar.producciones");
            }
        } catch (Exception e) {
            // Si no se puede obtener del bundle, usar valor por defecto
        }
        return "Producciones";
    }

    /**
     * Configura el menú contextual para las pestañas de un TabPane.
     */
    public static void configurarMenuContextual(TabPane tabPane, ResourceBundle initialBundle) {
        // Determinar el bundle a usar
        final ResourceBundle bundle;
        if (initialBundle == null) {
            // Si no hay bundle inicial, usar el último bundle guardado
            ResourceBundle savedBundle = resourceBundles.get(tabPane);
            if (savedBundle == null) {
                // Si aún no hay bundle, usar uno por defecto en español
                bundle = ResourceBundle.getBundle("utils.messages", new Locale("es"));
            } else {
                bundle = savedBundle;
            }
        } else {
            bundle = initialBundle;
        }
        
        // Crear un ContextMenu que se mostrará al hacer clic derecho
        javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();
        contextMenu.getStyleClass().add("context-menu");
        
        // Crear el ítem de menú para abrir en nueva ventana
        javafx.scene.control.MenuItem openInNewWindowMenuItem = new javafx.scene.control.MenuItem(
            bundle.getString("tab.context.open.new.window")
        );
        openInNewWindowMenuItem.getStyleClass().addAll("menu-item", "open-new-window-item");

        // Crear el menú para abrir en ventanas existentes
        javafx.scene.control.Menu openInExistingWindowMenu = new javafx.scene.control.Menu(
            bundle.getString("tab.context.open.existing.window")
        );
        openInExistingWindowMenu.getStyleClass().addAll("menu", "open-existing-window-menu");

        // Crear el ítem de menú para cerrar la pestaña actual
        javafx.scene.control.MenuItem closeMenuItem = new javafx.scene.control.MenuItem(
            bundle.getString("tab.context.close")
        );
        closeMenuItem.getStyleClass().addAll("menu-item", "close-tab-item");

        // Crear el ítem de menú para cerrar todas las pestañas
        javafx.scene.control.MenuItem closeAllMenuItem = new javafx.scene.control.MenuItem(
            bundle.getString("tab.context.close.all")
        );
        closeAllMenuItem.getStyleClass().addAll("menu-item", "close-all-tabs-item");

        // Añadir los items al menú contextual
        contextMenu.getItems().addAll(
            openInNewWindowMenuItem,
            openInExistingWindowMenu,
            closeMenuItem,
            closeAllMenuItem
        );

        // Configurar las acciones de los items del menú
        openInNewWindowMenuItem.setOnAction(event -> {
            Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
            if (selectedTab != null && selectedTab.isClosable()) {
                // Crear una nueva ventana secundaria
                SecondaryWindow newWindow = new SecondaryWindow(bundle, "SimAS 3.0");
                
                // Obtener el grupo de la pestaña seleccionada
                String grupoId = null;
                if (selectedTab.getUserData() != null) {
                    String elementId = selectedTab.getUserData().toString();
                    grupoId = obtenerGrupoDeElemento(tabPane, elementId);
                    
                    // Si no tiene grupo directo, puede ser una pestaña hija
                    if (grupoId == null) {
                        // Buscar el padre de esta pestaña
                        for (Tab tab : tabPane.getTabs()) {
                            if (tab.getUserData() != null) {
                                String potentialParentId = tab.getUserData().toString();
                                String parentGrupoId = obtenerGrupoDeElemento(tabPane, potentialParentId);
                                
                                if (parentGrupoId != null) {
                                    // Verificar si esta pestaña es hija del elemento principal
                                    if (isPestañaHijaDeElemento(elementId, potentialParentId)) {
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
                    newWindow.moveGroupToWindow(tabPane, grupoId, selectedTab);
                } else {
                    // Si no pertenece a un grupo, mover solo la pestaña
                    newWindow.addTab(selectedTab);
                    tabPane.getTabs().remove(selectedTab);
                }
                
                // Mostrar la nueva ventana en la posición del cursor
                newWindow.show();
                Stage stage = (Stage) newWindow.getTabPane().getScene().getWindow();
                java.awt.Point mouseLocation = java.awt.MouseInfo.getPointerInfo().getLocation();
                stage.setX(mouseLocation.getX() - 100);
                stage.setY(mouseLocation.getY() - 50);
            }
        });

        closeMenuItem.setOnAction(event -> {
            Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
            if (selectedTab != null && selectedTab.isClosable()) {
                String elementId = selectedTab.getUserData() != null ? selectedTab.getUserData().toString() : null;
                
                if (elementId != null) {
                    // Si es un simulador, cerrar TODOS sus descendientes (simulaciones, derivaciones, árboles)
                    if (elementId.startsWith("simulador_")) {
                        closeAllDescendantTabs(tabPane, elementId);
                    } else {
                        closeChildTabs(tabPane, elementId);
                    }
                    String grupoId = obtenerGrupoDeElemento(tabPane, elementId);
                    tabPane.getTabs().remove(selectedTab);
                    eliminarElementoDeGrupo(tabPane, elementId, grupoId);
                    reasignarNumerosGruposGramatica(tabPane);
                } else {
                    tabPane.getTabs().remove(selectedTab);
                }
            }
        });

        closeAllMenuItem.setOnAction(event -> {
            List<Tab> tabs = new ArrayList<>(tabPane.getTabs());
            for (Tab tab : tabs) {
                if (tab.isClosable()) {
                    String elementId = tab.getUserData() != null ? tab.getUserData().toString() : null;
                    if (elementId != null) {
                        // Si es un simulador, cerrar TODOS sus descendientes (simulaciones, derivaciones, árboles)
                        if (elementId.startsWith("simulador_")) {
                            closeAllDescendantTabs(tabPane, elementId);
                        } else {
                            closeChildTabs(tabPane, elementId);
                        }
                        String grupoId = obtenerGrupoDeElemento(tabPane, elementId);
                        eliminarElementoDeGrupo(tabPane, elementId, grupoId);
                    }
                    tabPane.getTabs().remove(tab);
                }
            }
            resetGrupos(tabPane);
        });

        // Añadir el listener para mostrar el menú contextual
        tabPane.setOnContextMenuRequested(event -> {
            // Obtener la pestaña en la posición del clic
            Node clickedNode = event.getPickResult().getIntersectedNode();
            Tab clickedTab = findTabFromNode(clickedNode);
            
            if (clickedTab != null && clickedTab.isClosable()) {
                // Actualizar el submenú de ventanas existentes antes de mostrar el menú contextual
                openInExistingWindowMenu.getItems().clear();
                
                // Obtener las ventanas secundarias activas
                Map<String, SecondaryWindow> activeWindows = SecondaryWindow.getActiveWindows();
                
                // Obtener la ventana actual
                Window currentWindow = tabPane.getScene().getWindow();
                Stage currentStage = (Stage) currentWindow;
                
                // Contador de ventanas disponibles
                int availableWindows = 0;
                
                // Buscar la ventana principal (será la que tenga el título exacto "SimAS 3.0")
                final Stage mainStage;
                Stage tempMainStage = null;
                for (Window window : Window.getWindows()) {
                    if (window instanceof Stage) {
                        Stage stage = (Stage) window;
                        // La ventana principal es la que tiene exactamente el título "SimAS 3.0"
                        if (stage.getTitle().equals("SimAS 3.0")) {
                            tempMainStage = stage;
                            break;
                        }
                    }
                }
                mainStage = tempMainStage;
                
                // Si estamos en una ventana secundaria y existe la ventana principal, añadirla como opción
                if (mainStage != null && currentStage != mainStage) {
                    availableWindows++;
                    String mainWindowTitle;
                    try {
                        mainWindowTitle = bundle.getString("window.main");
                    } catch (MissingResourceException e) {
                        // Fallback a texto en español
                        mainWindowTitle = "Ventana Principal";
                    }
                    mainWindowTitle += " (Principal)";
                    
                    javafx.scene.control.MenuItem mainWindowItem = new javafx.scene.control.MenuItem(mainWindowTitle);
                    mainWindowItem.getStyleClass().add("menu-item");
                    
                    // Obtener el TabPane de la ventana principal
                    TabPane mainTabPane = null;
                    for (Node node : mainStage.getScene().getRoot().lookupAll(".tab-pane")) {
                        if (node instanceof TabPane) {
                            mainTabPane = (TabPane)node;
                            break;
                        }
                    }
                    
                    final TabPane targetTabPane = mainTabPane;
                    mainWindowItem.setOnAction(e -> {
                        if (targetTabPane != null) {
                            Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
                            if (selectedTab != null && selectedTab.isClosable()) {
                                // Obtener el grupo de la pestaña seleccionada
                                String grupoId = obtenerGrupoDePestaña(tabPane, selectedTab);
                                
                                if (grupoId != null) {
                                    // CORRECCIÓN: Usar el método mejorado igual que para nueva ventana
                                    // Esto asegura IDs únicos y mantiene consistencia
                                    boolean exito = moverGrupoEntreVentanasMejorado(tabPane, targetTabPane, grupoId, selectedTab);
                                    
                                    if (!exito) {
                                        System.err.println("[ERROR] Falló el movimiento del grupo: " + grupoId);
                                        // Fallback: mover solo la pestaña seleccionada
                                        Tab newTab = new Tab(selectedTab.getText(), selectedTab.getContent());
                                        newTab.setUserData(selectedTab.getUserData());
                                        targetTabPane.getTabs().add(newTab);
                                        tabPane.getTabs().remove(selectedTab);
                                    }
                                } else {
                                    // Si no pertenece a un grupo, mover solo la pestaña
                                    Tab newTab = new Tab(selectedTab.getText(), selectedTab.getContent());
                                    newTab.setUserData(selectedTab.getUserData());
                                    targetTabPane.getTabs().add(newTab);
                                    tabPane.getTabs().remove(selectedTab);
                                    
                                    // Reasignar numeración en ambas ventanas
                                    reasignarNumerosGruposGramatica(tabPane);
                                    reasignarNumerosGruposGramatica(targetTabPane);
                                }
                                
                                // Traer la ventana principal al frente
                                mainStage.toFront();
                            }
                        }
                    });
                    
                    openInExistingWindowMenu.getItems().add(mainWindowItem);
                }
                
                // Añadir las ventanas secundarias que no sean la actual
                for (Map.Entry<String, SecondaryWindow> entry : activeWindows.entrySet()) {
                    SecondaryWindow window = entry.getValue();
                    String windowId = entry.getKey();
                    
                    // Saltar esta ventana si es la actual
                    if (window.getStage() == currentWindow) {
                        continue;
                    }
                    
                    availableWindows++;
                    // Obtener el título de la primera pestaña como identificador de la ventana
                    String windowTitle = bundle.getString("window.title") + " " + 
                        windowId.replace("SecondaryWindow-", "");
                    if (!window.getTabPane().getTabs().isEmpty()) {
                        Tab firstTab = window.getTabPane().getTabs().get(0);
                        windowTitle += " (" + firstTab.getText() + ")";
                    }
                    
                    javafx.scene.control.MenuItem windowItem = new javafx.scene.control.MenuItem(windowTitle);
                    windowItem.getStyleClass().add("menu-item");
                    
                    // Configurar la acción para mover la pestaña a la ventana seleccionada
                    windowItem.setOnAction(e -> {
                        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
                        if (selectedTab != null && selectedTab.isClosable()) {
                            // Obtener el grupo de la pestaña seleccionada
                            String grupoId = obtenerGrupoDePestaña(tabPane, selectedTab);
                            
                            if (grupoId != null) {
                                // CORRECCIÓN: Usar el método mejorado igual que para nueva ventana
                                // Esto asegura IDs únicos y mantiene consistencia
                                boolean exito = moverGrupoEntreVentanasMejorado(tabPane, window.getTabPane(), grupoId, selectedTab);
                                
                                if (!exito) {
                                    System.err.println("[ERROR] Falló el movimiento del grupo: " + grupoId);
                                    // Fallback: mover solo la pestaña seleccionada
                                    Tab newTab = new Tab(selectedTab.getText(), selectedTab.getContent());
                                    newTab.setUserData(selectedTab.getUserData());
                                    window.getTabPane().getTabs().add(newTab);
                                    tabPane.getTabs().remove(selectedTab);
                                }
                            } else {
                                // Si no pertenece a un grupo, mover solo la pestaña
                                Tab newTab = new Tab(selectedTab.getText(), selectedTab.getContent());
                                newTab.setUserData(selectedTab.getUserData());
                                window.getTabPane().getTabs().add(newTab);
                                tabPane.getTabs().remove(selectedTab);
                                
                                // Reasignar numeración en ambas ventanas
                                reasignarNumerosGruposGramatica(tabPane);
                                reasignarNumerosGruposGramatica(window.getTabPane());
                            }
                            
                            // Traer la ventana destino al frente
                            window.getStage().toFront();
                        }
                    });
                    
                    openInExistingWindowMenu.getItems().add(windowItem);
                }
                
                // Si no hay ventanas disponibles, mostrar mensaje
                if (availableWindows == 0) {
                    javafx.scene.control.MenuItem noWindowsItem = new javafx.scene.control.MenuItem(
                        bundle.getString("tab.context.no.windows")
                    );
                    noWindowsItem.getStyleClass().add("menu-item");
                    noWindowsItem.setDisable(true);
                    openInExistingWindowMenu.getItems().add(noWindowsItem);
                }
                
                // Seleccionar la pestaña clicada
                tabPane.getSelectionModel().select(clickedTab);
                // Mostrar el menú contextual
                contextMenu.show(clickedNode, event.getScreenX(), event.getScreenY());
            }
            event.consume();
        });

        // Guardar el menú contextual en el TabPane
        tabPane.setContextMenu(contextMenu);
    }

    /**
     * Actualiza el menú contextual de un TabPane con el nuevo ResourceBundle.
     * @param tabPane El TabPane cuyo menú contextual se actualizará
     * @param bundle El nuevo ResourceBundle con las traducciones
     */
    /**
     * Actualiza el menú contextual de un TabPane con el nuevo ResourceBundle.
     * Recrear completamente el menú asegura que todas las acciones funcionen correctamente.
     *
     * @param tabPane El TabPane cuyo menú contextual se actualizará
     * @param bundle El nuevo ResourceBundle con las traducciones
     */
    public static void actualizarMenuContextual(TabPane tabPane, ResourceBundle bundle) {
        // La forma más segura es recrear completamente el menú contextual
        // Esto asegura que todas las acciones funcionen correctamente con el nuevo idioma
        configurarMenuContextual(tabPane, bundle);
    }

    /**
     * Encuentra la pestaña asociada a un nodo del TabPane.
     */
    private static Tab findTabFromNode(Node node) {
        // Si el nodo es el texto, obtener su texto y buscar la pestaña correspondiente
        if (node.getClass().getName().contains("LabeledText")) {
            String clickedText = ((javafx.scene.text.Text) node).getText();
            
            // Buscar el TabPane padre
            Node parent = node;
            while (parent != null && !(parent instanceof TabPane)) {
                parent = parent.getParent();
            }
            
            if (parent instanceof TabPane) {
                TabPane tabPane = (TabPane) parent;
                // Buscar la pestaña que tenga este texto
                for (Tab tab : tabPane.getTabs()) {
                    if (tab.getText().equals(clickedText)) {
                        return tab;
                    }
                }
            }
        }
        
        // Si el nodo es una etiqueta, obtener su texto
        if (node instanceof javafx.scene.control.Label) {
            javafx.scene.control.Label label = (javafx.scene.control.Label) node;
            String labelText = label.getText();
            
            // Buscar el TabPane padre
            Node parent = node;
            while (parent != null && !(parent instanceof TabPane)) {
                parent = parent.getParent();
            }
            
            if (parent instanceof TabPane) {
                TabPane tabPane = (TabPane) parent;
                // Buscar la pestaña que tenga este texto
                for (Tab tab : tabPane.getTabs()) {
                    if (tab.getText().equals(labelText)) {
                        return tab;
                    }
                }
            }
        }
        
        // Si el nodo es parte del header de una pestaña
        if (node.getStyleClass().contains("tab")) {
            // Buscar el TabPane padre
            Node parent = node;
            while (parent != null && !(parent instanceof TabPane)) {
                parent = parent.getParent();
            }
            
            if (parent instanceof TabPane) {
                TabPane tabPane = (TabPane) parent;
                
                // Buscar el texto dentro de este nodo tab
                javafx.scene.control.Label label = (javafx.scene.control.Label) node.lookup(".tab-label");
                if (label != null) {
                    String tabText = label.getText();
                    
                    // Buscar la pestaña con este texto
                    for (Tab tab : tabPane.getTabs()) {
                        if (tab.getText().equals(tabText)) {
                            return tab;
                        }
                    }
                }
            }
        }
        
        // Si no encontramos la pestaña y el nodo tiene padre, intentar con el padre
        if (node.getParent() != null) {
            return findTabFromNode(node.getParent());
        }
        
        return null;
    }

    /**
     * Obtiene el ID del grupo al que pertenece una pestaña, ya sea principal o hija.
     */
    private static String obtenerGrupoDePestaña(TabPane tabPane, Tab tab) {
        if (tab == null || tab.getUserData() == null) return null;
        
        String tabId = tab.getUserData().toString();
        
        // Primero verificar si es un elemento principal
        String grupoDirecto = obtenerGrupoDeElemento(tabPane, tabId);
        if (grupoDirecto != null) {
            return grupoDirecto;
        }
        
        // Si no es principal, buscar si es hija de algún elemento con grupo
        Map<String, String> elementos = elementoToGrupo.get(tabPane);
        if (elementos != null) {
            for (Map.Entry<String, String> entry : elementos.entrySet()) {
                String elementId = entry.getKey();
                String grupoId = entry.getValue();
                
                // Si esta pestaña es hija del elemento, pertenece a su grupo
                if (isPestañaHijaDeElemento(tabId, elementId)) {
                    return grupoId;
                }
            }
        }
        
        return null;
    }

    /**
     * Actualiza los IDs de las pestañas relacionadas cuando se mueve un simulador a un nuevo grupo.
     */
    public static void actualizarIdsRelacionados(TabPane tabPane, String simuladorId, String nuevoGrupoId) {
        // Obtener el número del nuevo grupo
        Map<String, Integer> grupos = gruposGramatica.get(tabPane);
        if (grupos == null || !grupos.containsKey(nuevoGrupoId)) return;
        
        int nuevoNumeroGrupo = grupos.get(nuevoGrupoId);
        boolean hayMultiplesGrupos = contarGruposActivos(tabPane) > 1;
        
        // Actualizar pestañas hijas directas del simulador
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() != null) {
                String userData = tab.getUserData().toString();
                
                // Actualizar gramática y funciones de error
                if (userData.equals("gramatica_" + simuladorId) || 
                    userData.equals("funciones_error_" + simuladorId)) {
                    String tituloBase = tab.getText().replaceAll("^\\d+-", "");
                    tab.setText(hayMultiplesGrupos ? nuevoNumeroGrupo + "-" + tituloBase : tituloBase);
                }
                
                // Actualizar simulaciones y sus hijas
                if (userData.startsWith("simulacion_") && isPestañaHijaDeElemento(userData, simuladorId)) {
                    if (tab.getContent() instanceof simulador.SimulacionFinal) {
                        simulador.SimulacionFinal sim = (simulador.SimulacionFinal) tab.getContent();
                        sim.setGrupoId(nuevoGrupoId);
                        sim.setNumeroGrupo(nuevoNumeroGrupo);
                        
                        // Actualizar derivaciones y árboles de esta simulación
                        for (Tab childTab : tabPane.getTabs()) {
                            if (childTab.getUserData() != null) {
                                String childId = childTab.getUserData().toString();
                                if (isPestañaHijaDeElemento(childId, userData)) {
                                    String tituloBase = childTab.getText().replaceAll("^\\d+-", "");
                                    childTab.setText(hayMultiplesGrupos ? nuevoNumeroGrupo + "-" + tituloBase : tituloBase);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Actualiza los títulos de todas las pestañas en un TabPane específico.
     */
    public static void actualizarTitulosPestañas(TabPane tabPane) {
        if (tabPane == null) return;
        
        Map<String, String> elementos = elementoToGrupo.get(tabPane);
        Map<String, Integer> grupos = gruposGramatica.get(tabPane);
        
        if (elementos == null || grupos == null) return;
        
        // Determinar si hay múltiples grupos para mostrar/ocultar números
        boolean hayMultiplesGrupos = grupos.size() > 1;
        
        // Primero actualizar simulaciones para que tengan los nuevos números
        Map<String, Integer> contadorSimulacionesPorGrupo = new HashMap<>();
        
        // Primera pasada: contar simulaciones por grupo
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getContent() instanceof simulador.SimulacionFinal) {
                simulador.SimulacionFinal sim = (simulador.SimulacionFinal) tab.getContent();
                String simuladorId = sim.getSimuladorPadreId();
                if (simuladorId != null) {
                    String grupoId = elementos.get(simuladorId);
                    if (grupoId != null) {
                        contadorSimulacionesPorGrupo.merge(grupoId, 1, Integer::sum);
                    }
                }
            }
        }
        
        // Segunda pasada: actualizar títulos con números de instancia
        Map<String, Integer> contadorActualPorGrupo = new HashMap<>();
        
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getContent() instanceof simulador.SimulacionFinal) {
                simulador.SimulacionFinal sim = (simulador.SimulacionFinal) tab.getContent();
                String simuladorId = sim.getSimuladorPadreId();
                if (simuladorId != null) {
                    String grupoId = elementos.get(simuladorId);
                    if (grupoId != null) {
                        Integer numero = grupos.get(grupoId);
                        if (numero != null) {
                            // Incrementar contador de instancia para este grupo
                            int instancia = contadorActualPorGrupo.merge(grupoId, 1, Integer::sum);
                            boolean mostrarInstancia = contadorSimulacionesPorGrupo.get(grupoId) > 1;
                            
                            // Actualizar simulación con ambos números
                            sim.setGrupoId(grupoId);
                            sim.setNumeroGrupo(numero);
                            sim.setNumeroInstancia(instancia);
                            sim.actualizarTitulosPestañas(numero, hayMultiplesGrupos, instancia, mostrarInstancia);
                        }
                    }
                }
            }
        }
        
        // Luego actualizar el resto de pestañas (editores, simuladores, y sus hijas)
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() != null) {
                String elementId = tab.getUserData().toString();
                String grupoId = elementos.get(elementId);

                if (grupoId != null) {
                    Integer numero = grupos.get(grupoId);
                    if (numero != null) {

                        // Es un elemento principal (editor o simulador)
                        if (elementId.startsWith("editor_")) {
                            actualizarTituloEditor(tab, numero, hayMultiplesGrupos);
                        } else if (elementId.startsWith("simulador_")) {
                            actualizarTituloSimulador(tab, numero, hayMultiplesGrupos);
                        } else if (elementId.startsWith("simulacion_")) {
                            // Las simulaciones se actualizan desde SimulacionFinal.actualizarTitulosPestañas()
                            // pero necesitamos actualizar sus pestañas hijas (derivación y árbol)
                        }

                        // Actualizar sus pestañas hijas
                        actualizarTitulosPestañasHijas(tabPane, elementId, numero, hayMultiplesGrupos);
                    }
                } else {
                    // Verificar si es una pestaña hija de algún elemento con grupo
                    for (Map.Entry<String, String> entry : elementos.entrySet()) {
                        String elementoPadre = entry.getKey();
                        String grupoDelPadre = entry.getValue();

                        if (isPestañaHijaDeElemento(elementId, elementoPadre)) {
                            Integer numero = grupos.get(grupoDelPadre);
                            if (numero != null) {
                                // IMPORTANTE: Solo actualizar si NO es una pestaña hija de simulación
                                // (las hijas de simulación se manejan en actualizarTitulosPestañasHijas)
                                if (!elementoPadre.startsWith("simulacion_")) {
                                    actualizarTituloHija(tab, elementId, numero, hayMultiplesGrupos);
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
        
        // ACTUALIZACIÓN ADICIONAL: Buscar pestañas hijas que podrían no estar en las relaciones registradas
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() != null) {
                String userData = tab.getUserData().toString();
                
                // Buscar pestañas hijas de editores que podrían no estar en las relaciones
                if (userData.startsWith("terminales_") || userData.startsWith("no_terminales_") || 
                    userData.startsWith("producciones_") || userData.startsWith("creacion_")) {
                    
                    // Buscar el editor padre
                    for (Map.Entry<String, String> entry : elementos.entrySet()) {
                        String elementoPadre = entry.getKey();
                        String grupoDelPadre = entry.getValue();
                        
                        if (elementoPadre.startsWith("editor_") && isPestañaHijaDeElemento(userData, elementoPadre)) {
                            Integer numero = grupos.get(grupoDelPadre);
                            if (numero != null) {
                                actualizarTituloHija(tab, userData, numero, hayMultiplesGrupos);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Actualiza el título de una pestaña hija específica
     */
    private static void actualizarTituloHija(Tab tab, String childId, Integer numeroGrupo, boolean mostrarGrupo) {
        if (tab == null || numeroGrupo == null) return;
        
        String tituloBase = obtenerTituloBaseParaHija(tab.getTabPane(), childId);
        String nuevoTitulo = mostrarGrupo ? numeroGrupo + "-" + tituloBase : tituloBase;
        tab.setText(nuevoTitulo);
    }
    
    /**
     * Obtiene el título base apropiado para una pestaña hija
     */
    private static String obtenerTituloBaseParaHija(TabPane tabPane, String childId) {
        try {
            java.util.ResourceBundle bundle = resourceBundles.get(tabPane);
            if (bundle != null) {
                if (childId.startsWith("creacion_")) {
                    return bundle.getString("editor.asistente");
                } else if (childId.startsWith("terminales_")) {
                    return bundle.getString("creacion2.tab.modificar.terminales");
                } else if (childId.startsWith("no_terminales_")) {
                    return bundle.getString("creacion2.tab.modificar.no.terminales");
                } else if (childId.startsWith("producciones_")) {
                    return bundle.getString("creacion3.tab.modificar.producciones");
                } else if (childId.startsWith("gramatica_")) {
                    return bundle.getString("simulador.gramatica.original");
                } else if (childId.startsWith("funciones_error_")) {
                    return bundle.getString("simulador.paso4.btn.nueva");
                } else if (childId.startsWith("simulacion_")) {
                    return bundle.getString("simulacionfinal.titulo");
                } else if (childId.startsWith("derivacion_")) {
                    return bundle.getString("simulacionfinal.tab.derivacion");
                } else if (childId.startsWith("arbol_")) {
                    return bundle.getString("simulacionfinal.tab.arbol");
                }
            }
        } catch (Exception e) {
            // Fallback silencioso
        }
        
        // Fallback basado en el tipo de pestaña
        if (childId.startsWith("creacion_")) {
            return "Asistente Editor";
        } else if (childId.startsWith("terminales_")) {
            return "Terminales";
        } else if (childId.startsWith("no_terminales_")) {
            return "No Terminales";
        } else if (childId.startsWith("producciones_")) {
            return "Producciones";
        } else if (childId.startsWith("gramatica_")) {
            return "Gramática Original";
        } else if (childId.startsWith("funciones_error_")) {
            return "Nueva Función Error";
        } else if (childId.startsWith("simulacion_")) {
            return "Simulación";
        } else if (childId.startsWith("derivacion_")) {
            return "Derivación";
        } else if (childId.startsWith("arbol_")) {
            return "Árbol Sintáctico";
        }
        
        return "Pestaña"; // Fallback final
    }

    /**
     * Función de debug para mostrar el estado de los grupos y pestañas en un TabPane
     */
    public static void debugTabPaneState(TabPane tabPane) {
        if (tabPane == null) {
            return;
        }

        Map<String, String> elementos = elementoToGrupo.get(tabPane);
        Map<String, Integer> grupos = gruposGramatica.get(tabPane);

        if (elementos == null || grupos == null) {
            return;
        }

        // Agrupar elementos por grupo
        Map<String, List<String>> elementosPorGrupo = new HashMap<>();
        for (Map.Entry<String, String> entry : elementos.entrySet()) {
            String elementId = entry.getKey();
            String grupoId = entry.getValue();
            elementosPorGrupo.computeIfAbsent(grupoId, k -> new ArrayList<>()).add(elementId);
        }

        // Mostrar información de cada grupo
        for (Map.Entry<String, List<String>> entry : elementosPorGrupo.entrySet()) {
            List<String> elementosDelGrupo = entry.getValue();
            // Mostrar elementos principales del grupo
            for (String elementId : elementosDelGrupo) {
                
                // Buscar y mostrar pestañas hijas
                for (Tab tab : tabPane.getTabs()) {
                    if (tab.getUserData() != null) {
                        String childId = tab.getUserData().toString();
                        if (isPestañaHijaDeElemento(childId, elementId)) {
                            
                            // Si es una simulación, mostrar sus hijas también
                            if (childId.startsWith("simulacion_")) {
                                for (Tab grandChildTab : tabPane.getTabs()) {
                                    if (grandChildTab.getUserData() != null) {
                                        String grandChildId = grandChildTab.getUserData().toString();
                                        if (isPestañaHijaDeElemento(grandChildId, childId)) {
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() != null) {
                String tabId = tab.getUserData().toString();
                
                // Verificar si la pestaña pertenece a algún grupo
                for (String elementId : elementos.keySet()) {
                    if (tabId.equals(elementId) || isPestañaHijaDeElemento(tabId, elementId)) {
                        break;
                    }
                }
            }
        }
    }

    /**
     * MÉTODO MEJORADO: Mueve un grupo completo entre ventanas manteniendo consistencia
     * Maneja correctamente la numeración local por TabPane y preserva relaciones padre-hijo
     */
    public static boolean moverGrupoEntreVentanasMejorado(TabPane sourceTabPane, TabPane targetTabPane, String grupoId, Tab selectedTab) {
        if (sourceTabPane == null || targetTabPane == null || grupoId == null) {
            return false;
        }

        // PAUSAR MONITOR DURANTE EL MOVIMIENTO
        TabPaneMonitor monitor = TabPaneMonitor.getInstance();
        monitor.setMovimientoEnProgreso(true);
        
        try {
            // 1. RECOLECTAR TODAS LAS PESTAÑAS DEL GRUPO (incluyendo jerarquía completa)
            GrupoCompleto grupoCompleto = recolectarGrupoCompleto(sourceTabPane, grupoId);
            if (grupoCompleto.isEmpty()) {
                return false;
            }

            grupoCompleto.debug();

            // 2. OBTENER INFORMACIÓN DEL GRUPO ANTES DE MOVERLO
            Map<String, Integer> gruposOrigen = gruposGramatica.get(sourceTabPane);
            
            // 3. CALCULAR NUEVO NÚMERO DE GRUPO EN VENTANA DESTINO (CORRECTAMENTE)
            int gruposActivosDestino = contarGruposActivos(targetTabPane);
            int nuevoNumeroGrupo = gruposActivosDestino + 1;
            String nuevoGrupoId = "grupo_local_" + nuevoNumeroGrupo + "_" + System.currentTimeMillis();
            
            // 4. PRESERVAR INFORMACIÓN DE LISTENERS Y RELACIONES ANTES DEL MOVIMIENTO
            // (Se recrearán los listeners en el paso 10, no necesitamos preservarlos)

            // 5. REMOVER PESTAÑAS DE VENTANA ORIGEN LIMPIAMENTE
            List<Tab> pestañasMovidas = new ArrayList<>();
            for (Tab tab : grupoCompleto.todasLasPestañas) {
                if (sourceTabPane.getTabs().contains(tab)) {
                    sourceTabPane.getTabs().remove(tab);
                    pestañasMovidas.add(tab);
                    // Actualizar referencia al TabPane en el contenido
                    actualizarReferenciasTabPane(tab, targetTabPane);
                }
            }

            // 6. LIMPIAR COMPLETAMENTE EL GRUPO DE VENTANA ORIGEN
            Map<String, String> elementosOrigen = elementoToGrupo.get(sourceTabPane);
            if (elementosOrigen != null) {
                for (String elementoPadre : grupoCompleto.elementosPadre) {
                    elementosOrigen.remove(elementoPadre);
                }
            }
            
            // Limpiar grupo de la ventana origen
            if (gruposOrigen != null) {
                gruposOrigen.remove(grupoId);
            }
            
            // Limpiar relaciones padre-hijo de ventana origen
            Map<String, List<Tab>> relacionesOrigen = getParentChildRelations(sourceTabPane);
            for (String elementoPadre : grupoCompleto.elementosPadre) {
                relacionesOrigen.remove(elementoPadre);
            }

            // 7. AÑADIR PESTAÑAS A VENTANA DESTINO EN ORDEN CORRECTO
            for (Tab tab : pestañasMovidas) {
                targetTabPane.getTabs().add(tab);
            }

            // 8. ESTABLECER GRUPO EN VENTANA DESTINO
            Map<String, String> elementosDestino = elementoToGrupo.computeIfAbsent(targetTabPane, k -> new HashMap<>());
            Map<String, Integer> gruposDestino = gruposGramatica.computeIfAbsent(targetTabPane, k -> new HashMap<>());
            
            // Asignar elementos al nuevo grupo
            for (String elementoPadre : grupoCompleto.elementosPadre) {
                elementosDestino.put(elementoPadre, nuevoGrupoId);
            }
            gruposDestino.put(nuevoGrupoId, nuevoNumeroGrupo);

            // 8.5. COPIAR RESOURCEBUNDLE DE VENTANA ORIGEN A DESTINO
            java.util.ResourceBundle bundleOrigen = resourceBundles.get(sourceTabPane);
            if (bundleOrigen != null) {
                resourceBundles.put(targetTabPane, bundleOrigen);
            }

            // 9. RESTAURAR RELACIONES PADRE-HIJO EN VENTANA DESTINO
            Map<String, List<Tab>> relacionesDestino = parentChildRelations.computeIfAbsent(targetTabPane, k -> new HashMap<>());
            for (Map.Entry<String, List<Tab>> entry : grupoCompleto.relacionesPadreHijo.entrySet()) {
                String parentId = entry.getKey();
                List<Tab> hijosOriginales = entry.getValue();
                
                // Encontrar las pestañas movidas que corresponden a estos hijos
                List<Tab> hijosEnDestino = new ArrayList<>();
                for (Tab hijoOriginal : hijosOriginales) {
                    // Buscar la pestaña correspondiente en las pestañas movidas
                    for (Tab tabMovida : pestañasMovidas) {
                        if (tabMovida.getUserData() != null && hijoOriginal.getUserData() != null &&
                            tabMovida.getUserData().toString().equals(hijoOriginal.getUserData().toString())) {
                            hijosEnDestino.add(tabMovida);
                            break;
                        }
                    }
                }
                
                if (!hijosEnDestino.isEmpty()) {
                    relacionesDestino.put(parentId, hijosEnDestino);
                } else {
                }
            }

            // 10. RESTAURAR LISTENERS onClosed PARA MANTENER FUNCIONALIDAD PADRE-HIJO
            for (Tab tab : pestañasMovidas) {
                restaurarListenerOnClosed(tab, targetTabPane);
            }

            // 11. REASIGNAR NUMERACIÓN EN AMBAS VENTANAS
            Platform.runLater(() -> {
                reasignarNumerosGruposGramatica(sourceTabPane);
                reasignarNumerosGruposGramatica(targetTabPane);
                
                // ACTUALIZAR TÍTULOS DE PESTAÑAS EN LA VENTANA DESTINO
                actualizarTitulosPestañas(targetTabPane);
            });

            // 12. SELECCIONAR PESTAÑA ORIGINAL
            if (selectedTab != null && targetTabPane.getTabs().contains(selectedTab)) {
                Platform.runLater(() -> {
                    targetTabPane.getSelectionModel().select(selectedTab);
                });
            }

            return true;

        } catch (Exception e) {
            System.err.println("[ERROR] Error durante el movimiento del grupo: " + e.getMessage());
            e.printStackTrace();
            return false;
            
        } finally {
            // REANUDAR MONITOR DESPUÉS DEL MOVIMIENTO
            Platform.runLater(() -> {
                monitor.setMovimientoEnProgreso(false);
                
                // Validación final después de un retraso mayor
                Platform.runLater(() -> {
                    Platform.runLater(() -> {
                        monitor.forzarValidacion(sourceTabPane);
                        monitor.forzarValidacion(targetTabPane);
                    });
                });
            });
        }
    }

    /**
     * Restaura el listener onClosed para una pestaña movida, manteniendo la funcionalidad padre-hijo
     */
    private static void restaurarListenerOnClosed(Tab tab, TabPane targetTabPane) {
        if (tab.getUserData() == null) return;
        
        String tabId = tab.getUserData().toString();
        Class<?> tabType = determinarTipoDeTab(tab);
        
        // Recrear el listener onClosed que maneja el cierre de pestañas hijas
        tab.setOnClosed(event -> {
            
            // Limpiar del caché de instancias
            Map<Class<?>, Tab> tabs = tabInstances.get(targetTabPane);
            if (tabs != null) {
                tabs.remove(tabType);
            }
            
            // Cerrar pestañas hijas
            List<Tab> hijos = parentChildRelations.getOrDefault(targetTabPane, new HashMap<>()).get(tabId);
            if (hijos != null) {
                for (Tab hijo : hijos) {
                    hijo.getTabPane().getTabs().remove(hijo);
                }
            }
            
            // Eliminar relación padre-hijo
            parentChildRelations.getOrDefault(targetTabPane, new HashMap<>()).remove(tabId);
        });
    }

    /**
     * Determina el tipo de una pestaña basándose en su contenido
     */
    private static Class<?> determinarTipoDeTab(Tab tab) {
        if (tab.getContent() != null) {
            return tab.getContent().getClass();
        }
        // Fallback basado en userData
        if (tab.getUserData() != null) {
            String userData = tab.getUserData().toString();
            if (userData.startsWith("editor_")) {
                return editor.Editor.class;
            } else if (userData.startsWith("simulador_")) {
                return simulador.PanelSimuladorDesc.class;
            }
        }
        return Tab.class; // Fallback genérico
    }

    /**
     * Clase auxiliar para mantener la información completa de un grupo
     */
    private static class GrupoCompleto {
        List<Tab> pestañasPadre = new ArrayList<>();
        List<String> elementosPadre = new ArrayList<>();
        Map<String, List<Tab>> relacionesPadreHijo = new HashMap<>();
        List<Tab> todasLasPestañas = new ArrayList<>();
        Map<Tab, Integer> ordenOriginal = new HashMap<>();

        boolean isEmpty() {
            return pestañasPadre.isEmpty();
        }
        
        void debug() {
            // Método vacío para compatibilidad
        }
    }

    /**
     * Recolecta todas las pestañas de un grupo manteniendo las relaciones padre-hijo
     */
    private static GrupoCompleto recolectarGrupoCompleto(TabPane tabPane, String grupoId) {
        GrupoCompleto grupo = new GrupoCompleto();
        Map<String, String> elementos = elementoToGrupo.get(tabPane);
        if (elementos == null) return grupo;

        // 1. Encontrar elementos padre del grupo
        for (Map.Entry<String, String> entry : elementos.entrySet()) {
            if (grupoId.equals(entry.getValue())) {
                String elementoPadre = entry.getKey();
                
                // Buscar la pestaña padre
                Tab pestañaPadre = findTabByUserData(tabPane, elementoPadre);
                if (pestañaPadre != null) {
                    grupo.pestañasPadre.add(pestañaPadre);
                    grupo.elementosPadre.add(elementoPadre);
                    grupo.todasLasPestañas.add(pestañaPadre);
                    grupo.ordenOriginal.put(pestañaPadre, tabPane.getTabs().indexOf(pestañaPadre));
                }
            }
        }

        // 2. Para cada elemento padre, recolectar sus hijos
        for (String elementoPadre : grupo.elementosPadre) {
            List<Tab> hijosDelPadre = new ArrayList<>();

            // Recolectar hijos directos
            for (Tab tab : tabPane.getTabs()) {
                if (tab.getUserData() != null) {
                    String childId = tab.getUserData().toString();
                    if (isPestañaHijaDeElemento(childId, elementoPadre)) {
                        hijosDelPadre.add(tab);
                        grupo.todasLasPestañas.add(tab);
                        grupo.ordenOriginal.put(tab, tabPane.getTabs().indexOf(tab));

                        // Si es una simulación, recolectar también sus nietos (derivación, árbol)
                        if (tab.getContent() instanceof simulador.SimulacionFinal) {
                            recolectarNietosDeSimulacion(tabPane, tab, grupo);
                        }
                    }
                }
            }

            if (!hijosDelPadre.isEmpty()) {
                grupo.relacionesPadreHijo.put(elementoPadre, hijosDelPadre);
            }
        }

        // 3. Ordenar todas las pestañas según su orden original
        grupo.todasLasPestañas.sort((a, b) -> 
            grupo.ordenOriginal.get(a).compareTo(grupo.ordenOriginal.get(b)));

        return grupo;
    }

    /**
     * Recolecta pestañas nietas (derivación y árbol) de una simulación
     */
    private static void recolectarNietosDeSimulacion(TabPane tabPane, Tab simulacionTab, GrupoCompleto grupo) {
        if (!(simulacionTab.getContent() instanceof simulador.SimulacionFinal)) return;
        
        simulador.SimulacionFinal sim = (simulador.SimulacionFinal) simulacionTab.getContent();
        String simulacionId = sim.simulacionId;
        String simulacionTabId = simulacionTab.getUserData() != null ? simulacionTab.getUserData().toString() : null;
        
        // Lista para almacenar las pestañas nietas encontradas
        List<Tab> nietosDeSimulacion = new ArrayList<>();

        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() != null) {
                String userData = tab.getUserData().toString();
                
                // Buscar pestañas de derivación y árbol que pertenecen a esta simulación
                boolean esNietoDeSimulacion = false;
                
                if (userData.startsWith("derivacion_") && userData.contains(simulacionId)) {
                    esNietoDeSimulacion = true;
                } else if (userData.startsWith("arbol_") && userData.contains(simulacionId)) {
                    esNietoDeSimulacion = true;
                }
                
                if (esNietoDeSimulacion) {
                    nietosDeSimulacion.add(tab);
                    grupo.todasLasPestañas.add(tab);
                    grupo.ordenOriginal.put(tab, tabPane.getTabs().indexOf(tab));
                }
            }
        }
        
        // Si se encontraron nietos, añadirlos a la relación padre-hijo de la simulación
        if (!nietosDeSimulacion.isEmpty() && simulacionTabId != null) {
            // Buscar si ya existe una lista de hijos para esta simulación en el grupo
            for (Map.Entry<String, List<Tab>> entry : grupo.relacionesPadreHijo.entrySet()) {
                if (entry.getValue().contains(simulacionTab)) {
                    // Encontramos la entrada donde esta simulación es hija
                    // Necesitamos añadir los nietos a la relación de la simulación
                    List<Tab> nietosParaSimulacion = grupo.relacionesPadreHijo.get(simulacionTabId);
                    if (nietosParaSimulacion == null) {
                        nietosParaSimulacion = new ArrayList<>();
                        grupo.relacionesPadreHijo.put(simulacionTabId, nietosParaSimulacion);
                    }
                    nietosParaSimulacion.addAll(nietosDeSimulacion);
                    break;
                }
            }
        }
    }

    /**
     * Obtiene el mapa de elementos a grupos para un TabPane específico.
     * @param tabPane El TabPane del cual obtener el mapa
     * @return El mapa de elementos a grupos, o null si no existe
     */
    public static Map<String, String> getElementoToGrupo(TabPane tabPane) {
        return elementoToGrupo.get(tabPane);
    }
    
    /**
     * Obtiene el mapa de grupos a números para un TabPane específico.
     * @param tabPane El TabPane del cual obtener el mapa
     * @return El mapa de grupos a números, o null si no existe
     */
    public static Map<String, Integer> getGruposGramatica(TabPane tabPane) {
        return gruposGramatica.get(tabPane);
    }
    
    /**
     * Actualiza las referencias al TabPane en el contenido de la pestaña
     */
    private static void actualizarReferenciasTabPane(Tab tab, TabPane nuevoTabPane) {
        if (tab.getContent() instanceof editor.Editor) {
            editor.Editor editor = (editor.Editor) tab.getContent();
            editor.setTabPane(nuevoTabPane);
        } else if (tab.getContent() instanceof simulador.SimulacionFinal) {
            simulador.SimulacionFinal sim = (simulador.SimulacionFinal) tab.getContent();
            // SimulacionFinal no tiene setTabPane, pero podemos actualizar la referencia interna
            // usando reflexión para acceder al campo tabPane privado
            try {
                java.lang.reflect.Field tabPaneField = simulador.SimulacionFinal.class.getDeclaredField("tabPane");
                tabPaneField.setAccessible(true);
                tabPaneField.set(sim, nuevoTabPane);
            } catch (Exception e) {
                System.err.println("[ERROR] No se pudo actualizar la referencia TabPane en SimulacionFinal: " + e.getMessage());
            }
        } else {
            // Para otros tipos de contenido, intentar actualizar referencias si es necesario
            try {
                Object content = tab.getContent();
                if (content != null) {
                    java.lang.reflect.Method setTabPaneMethod = content.getClass().getMethod("setTabPane", TabPane.class);
                    setTabPaneMethod.invoke(content, nuevoTabPane);
                }
            } catch (Exception e) {
                // Método no existe o no es accesible, continuar silenciosamente
                // Esto es normal para muchos tipos de contenido
            }
        }
    }

    /**
     * Obtiene el número de instancia de una simulación específica.
     */
    private static int obtenerNumeroInstanciaSimulacion(TabPane tabPane, String simulacionId) {
        
        if (tabPane == null || simulacionId == null) return 1;
        
        int numeroInstancia = 0;
        String simuladorId = null;
        
        // Extraer el simuladorId del simulacionId
        if (simulacionId.startsWith("simulacion_simulador_")) {
            // Extraer solo la parte del simulador base: simulacion_simulador_123_456 -> simulador_123
            String parteDespuesDeSimulacion = simulacionId.substring("simulacion_".length());
            // Buscar el primer underscore después de "simulador_"
            int primerUnderscore = parteDespuesDeSimulacion.indexOf("_", "simulador_".length());
            if (primerUnderscore != -1) {
                simuladorId = parteDespuesDeSimulacion.substring(0, primerUnderscore);
            } else {
                simuladorId = parteDespuesDeSimulacion;
            }
        }
        
        if (simuladorId == null) return 1;
        
        // Contar cuántas simulaciones de este simulador existen hasta encontrar la nuestra
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() != null && tab.getUserData().toString().startsWith("simulacion_" + simuladorId + "_")) {
                numeroInstancia++;
                if (tab.getUserData().toString().equals(simulacionId)) {
                    // Encontramos nuestra simulación, retornar el número actual
                    return numeroInstancia;
                }
            }
        }
        
        return 1;
    }

    /**
     * Cuenta cuántas simulaciones existen para un simulador específico.
     */
    private static int contarSimulacionesDelSimulador(TabPane tabPane, String simulacionId) {
        
        if (tabPane == null || simulacionId == null) return 1;
        
        String simuladorId = null;
        
        // Extraer el simuladorId del simulacionId
        if (simulacionId.startsWith("simulacion_simulador_")) {
            // Extraer solo la parte del simulador base: simulacion_simulador_123_456 -> simulador_123
            String parteDespuesDeSimulacion = simulacionId.substring("simulacion_".length());
            // Buscar el primer underscore después de "simulador_"
            int primerUnderscore = parteDespuesDeSimulacion.indexOf("_", "simulador_".length());
            if (primerUnderscore != -1) {
                simuladorId = parteDespuesDeSimulacion.substring(0, primerUnderscore);
            } else {
                simuladorId = parteDespuesDeSimulacion;
            }
        }
        
        
        if (simuladorId == null) return 1;
        
        // Contar cuántas simulaciones de este simulador existen
        int contador = 0;
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() != null && tab.getUserData().toString().startsWith("simulacion_" + simuladorId + "_")) {
                contador++;
            }
        }
        
        return contador;
    }
} 
