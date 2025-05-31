package editor;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import java.util.*;

public class TabManager {
    private static final Map<TabPane, Map<Class<?>, Tab>> tabInstances = new HashMap<>();
    private static final Map<TabPane, Map<String, List<Tab>>> parentChildRelations = new HashMap<>();
    private static final Map<TabPane, Map<String, String>> elementoToGrupo = new HashMap<>(); // Mapea editorId/simuladorId -> grupoId
    private static final Map<TabPane, Map<String, Integer>> gruposGramatica = new HashMap<>(); // Mapea grupoId -> numeroGrupo
    private static final Map<TabPane, java.util.ResourceBundle> resourceBundles = new HashMap<>();
    
    // Contador global para generar IDs únicos de grupo
    private static int contadorGrupos = 0;

    public static Tab getOrCreateTab(TabPane tabPane, Class<?> tabType, String title, Object content) {
        return getOrCreateTab(tabPane, tabType, title, content, null, null);
    }

    public static Tab getOrCreateTab(TabPane tabPane, Class<?> tabType, String title, Object content, String parentId, String childId) {
        // Inicializar el mapa para este TabPane si no existe
        tabInstances.computeIfAbsent(tabPane, k -> new HashMap<>());
        parentChildRelations.computeIfAbsent(tabPane, k -> new HashMap<>());
        elementoToGrupo.computeIfAbsent(tabPane, k -> new HashMap<>());
        gruposGramatica.computeIfAbsent(tabPane, k -> new HashMap<>());

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
                                                        childId.startsWith("funciones_error_simulador_")));
        
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
                    System.out.println("DEBUG CLOSE: Removing element " + parentId + " from group " + grupoId);
                    elementos.remove(parentId); // Solo quitar este elemento, no todo el grupo
                    necesitaRenumeracion = true;
                    
                    // Verificar si el grupo queda vacío DESPUÉS de quitar este elemento
                    boolean grupoVacio = elementos.values().stream().noneMatch(g -> g.equals(grupoId));
                    if (grupoVacio) {
                        Map<String, Integer> grupos = gruposGramatica.get(tabPane);
                        if (grupos != null) {
                            grupos.remove(grupoId);
                            System.out.println("DEBUG CLOSE: Group " + grupoId + " is now empty, removing it");
                        }
                    } else {
                        System.out.println("DEBUG CLOSE: Group " + grupoId + " still has other elements");
                    }
                }
            }
            
            // Si se eliminó un elemento del grupo o es una pestaña hija relacionada, forzar renumeración
            if (necesitaRenumeracion || (childId != null && (isChildOfEditor || isSimuladorChild(childId)))) {
                // *** MEJORADO: Usar doble llamada para asegurar la actualización ***
                System.out.println("DEBUG CLOSE: Triggering IMMEDIATE group renumbering after closing " + 
                                 (parentId != null ? parentId : childId));
                
                // Llamada inmediata
                reasignarNumerosGruposGramatica(tabPane);
                
                // Llamada asíncrona como respaldo para asegurar que se ejecute
                javafx.application.Platform.runLater(() -> {
                    reasignarNumerosGruposGramatica(tabPane);
                    System.out.println("DEBUG CLOSE: Secondary renumbering completed for " + 
                                     (parentId != null ? parentId : childId));
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
            
            System.out.println("DEBUG AUTO-ASSIGN: parentId=" + parentId + ", childId=" + childId);
            System.out.println("DEBUG AUTO-ASSIGN: tabType=" + tabType.getSimpleName());
            System.out.println("DEBUG AUTO-ASSIGN: isEditorType=" + isEditorType(tabType));
            System.out.println("DEBUG AUTO-ASSIGN: isSimuladorType=" + isSimuladorType(tabType));
            
            // Verificar si ya está asignado a un grupo (ej: simulador desde editor o asignación previa desde MenuPrincipal)
            Map<String, String> elementos = elementoToGrupo.get(tabPane);
            boolean yaAsignado = (elementos != null && elementos.containsKey(parentId));
            
            System.out.println("DEBUG AUTO-ASSIGN: yaAsignado=" + yaAsignado);
            
            if (!yaAsignado) {
                if (isEditorType(tabType)) {
                    // EDITOR INDEPENDIENTE desde menú principal → NUEVO GRUPO
                    System.out.println("DEBUG AUTO-ASSIGN: Creating NEW GROUP for EDITOR");
                    asignarElementoANuevoGrupo(tabPane, parentId);
                    System.out.println("DEBUG: Created NEW GROUP for EDITOR: " + parentId);
                    
                } else if (isSimuladorType(tabType)) {
                    // SIMULADOR INDEPENDIENTE desde menú principal → NUEVO GRUPO
                    // Solo crear grupo si no está ya asignado (ej: desde MenuPrincipal)
                    System.out.println("DEBUG AUTO-ASSIGN: Creating NEW GROUP for SIMULATOR");
                    asignarElementoANuevoGrupo(tabPane, parentId);
                    System.out.println("DEBUG: Created NEW GROUP for SIMULATOR: " + parentId);
                } else {
                    System.out.println("DEBUG AUTO-ASSIGN: Unknown tabType, no group assignment");
                }
            } else {
                System.out.println("DEBUG: Element " + parentId + " already assigned to group, skipping auto-assignment");
            }
        } else {
            System.out.println("DEBUG AUTO-ASSIGN: Skipping auto-assignment - parentId=" + parentId + ", childId=" + childId);
        }
        
        // AHORA calcular la posición donde insertar la pestaña (después de asignar grupos)
        int insertPosition = calcularPosicionInsercion(tabPane, tabType, parentId, childId);
        
        // Si es una pestaña hija, registrar la relación padre-hijo
        if (parentId != null && childId != null) {
            Map<String, List<Tab>> relations = parentChildRelations.get(tabPane);
            relations.computeIfAbsent(parentId, k -> new ArrayList<>()).add(newTab);
            
            // Debug: mostrar las relaciones que se están creando
            System.out.println("DEBUG: Registering child relationship - parentId: " + parentId + ", childId: " + childId + ", title: " + title);
        }
        
        // Añadir la pestaña al TabPane en la posición correcta
        if (insertPosition >= tabPane.getTabs().size()) {
            tabPane.getTabs().add(newTab);
        } else {
            tabPane.getTabs().add(insertPosition, newTab);
        }
        
        tabPane.getSelectionModel().select(newTab);
        
        // Reasignar numeración inmediatamente si se creó un nuevo grupo
        boolean seCreoNuevoGrupo = (parentId != null && childId == null && 
                                   (isEditorType(tabType) || isSimuladorType(tabType)));
        if (seCreoNuevoGrupo) {
            System.out.println("DEBUG: New group element added, triggering immediate renumbering");
            reasignarNumerosGruposGramatica(tabPane);
        }
        
        // Reasignar numeración de grupos después de añadir (asíncrono como respaldo)
        javafx.application.Platform.runLater(() -> {
            reasignarNumerosGruposGramatica(tabPane);
        });
        
        return newTab;
    }
    
    /**
     * Asigna un elemento (editor o simulador) a un grupo de gramática.
     */
    private static void asignarElementoAGrupo(TabPane tabPane, String elementoId, String grupoIdPadre) {
        Map<String, String> elementos = elementoToGrupo.get(tabPane);
        Map<String, Integer> grupos = gruposGramatica.get(tabPane);
        
        if (elementos != null && grupos != null && !elementos.containsKey(elementoId)) {
            String grupoId;
            
            if (grupoIdPadre != null) {
                // Si se especifica un grupo padre, usar ese grupo
                grupoId = grupoIdPadre;
            } else {
                // Crear un nuevo grupo
                grupoId = "grupo_" + System.currentTimeMillis() + "_" + (++contadorGrupos);
                
                // Asignar número de grupo basado en el orden actual
                int numeroGrupo = contarElementosActivosEnGrupos(tabPane) + 1;
                grupos.put(grupoId, numeroGrupo);
            }
            
            elementos.put(elementoId, grupoId);
            System.out.println("DEBUG: Assigned element " + elementoId + " to group " + grupoId);
        }
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
                
                System.out.println("DEBUG: Created NEW group " + grupoId + " with number " + numeroGrupo + " for element " + elementoId);
            } else {
                System.out.println("DEBUG: Element " + elementoId + " already has a group assigned");
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
        
        System.out.println("DEBUG contarGruposActivos: Total active groups: " + totalGrupos);
        return totalGrupos;
    }
    
    /**
     * Cuenta el número de elementos activos (editores y simuladores) en todos los grupos.
     */
    private static int contarElementosActivosEnGrupos(TabPane tabPane) {
        int contador = 0;
        System.out.println("DEBUG contarElementosActivosEnGrupos: Starting count...");
        for (Tab tab : tabPane.getTabs()) {
            System.out.println("DEBUG contarElementosActivosEnGrupos: Checking tab: " + tab.getText() + 
                             ", content type: " + (tab.getContent() != null ? tab.getContent().getClass().getSimpleName() : "null") +
                             ", userData: " + tab.getUserData());
            if (isEditorContent(tab.getContent()) || isSimuladorTab(tab)) {
                contador++;
                System.out.println("DEBUG contarElementosActivosEnGrupos: Found element! Count now: " + contador);
            }
        }
        System.out.println("DEBUG contarElementosActivosEnGrupos: Final count: " + contador);
        return contador;
    }
    
    /**
     * Verifica si el tipo de pestaña es un Simulador.
     */
    private static boolean isSimuladorType(Class<?> tabType) {
        boolean result = tabType.getSimpleName().contains("Simulador") || 
               tabType.getName().contains("simulador.PanelSimuladorDesc") ||
               tabType == simulador.PanelSimuladorDesc.class;
               
        System.out.println("DEBUG isSimuladorType: tabType=" + tabType.getName() + 
                         ", simpleName=" + tabType.getSimpleName() + 
                         ", result=" + result);
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
        System.out.println("DEBUG: asignarSimuladorAGrupoDeEditor - editorId: " + editorId + 
                         ", grupoEditor: " + grupoEditor + ", simuladorId: " + simuladorId);
        
        if (grupoEditor != null) {
            Map<String, String> elementos = elementoToGrupo.get(tabPane);
            if (elementos != null) {
                elementos.put(simuladorId, grupoEditor);
                System.out.println("DEBUG: Successfully assigned simulator " + simuladorId + " to editor's group " + grupoEditor);
            }
        } else {
            System.out.println("DEBUG: WARNING - Editor " + editorId + " has no group assigned, cannot assign simulator to group");
        }
    }
    
    /**
     * Calcula la posición de inserción para diferentes tipos de pestañas.
     */
    private static int calcularPosicionInsercion(TabPane tabPane, Class<?> tabType, String parentId, String childId) {
        // Si es una pestaña hija, usar la lógica existente
        if (parentId != null && childId != null) {
            Tab parentTab = findTabByUserData(tabPane, parentId);
            if (parentTab != null) {
                return calcularPosicionHija(tabPane, parentTab, childId, parentId);
            }
        }
        
        // Si es un Editor, colocarlo después del menú principal
        if (isEditorType(tabType)) {
            return calcularPosicionEditor(tabPane);
        }
        
        // Si es un Simulador independiente (no hijo), posicionarlo después del menú principal
        if (isSimuladorType(tabType) && parentId != null && childId == null) {
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
        
        System.out.println("DEBUG: calcularPosicionSimuladorInteligente - simuladorId: " + simuladorId + 
                         ", grupoDelSimulador: " + grupoDelSimulador);
        
        if (grupoDelSimulador != null) {
            // Verificar si es un simulador DE EDITOR (debe tener un editor en el mismo grupo)
            boolean esSimuladorDeEditor = false;
            if (elementos != null) {
                for (Map.Entry<String, String> entry : elementos.entrySet()) {
                    String elementoId = entry.getKey();
                    String grupoId = entry.getValue();
                    
                    if (grupoId.equals(grupoDelSimulador) && elementoId.startsWith("editor_")) {
                        esSimuladorDeEditor = true;
                        System.out.println("DEBUG: Found editor " + elementoId + " in same group - this is an EDITOR SIMULATOR");
                        break;
                    }
                }
            }
            
            if (esSimuladorDeEditor) {
                // Simulador DE EDITOR: posicionar después del último elemento del grupo
                System.out.println("DEBUG: Positioning EDITOR SIMULATOR within group");
                return calcularPosicionDentroDeGrupo(tabPane, grupoDelSimulador);
            } else {
                // Simulador INDEPENDIENTE: aunque tenga grupo, va al final
                System.out.println("DEBUG: Positioning INDEPENDENT SIMULATOR at end (even though it has a group)");
                return calcularPosicionSimuladorIndependiente(tabPane);
            }
        } else {
            // Simulador sin grupo: independiente, va al final
            System.out.println("DEBUG: Positioning SIMULATOR without group at end");
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
        
        System.out.println("DEBUG: calcularPosicionSimuladorIndependiente - Menu at: " + menuPosition + 
                         ", TabPane size: " + tabPane.getTabs().size() + 
                         ", positioning independent simulator at: " + posicionFinal);
        
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
        
        // Encontrar todas las pestañas que pertenecen a este grupo
        int ultimaPosicionDelGrupo = -1;
        
        System.out.println("DEBUG: calcularPosicionDentroDeGrupo - Looking for group: " + grupoId);
        
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
                    System.out.println("DEBUG: Found root element of group at position " + i + ": " + userData);
                }
                
                // Verificar si es una pestaña hija de algún elemento del grupo
                if (!perteneceAlGrupo) {
                    for (Map.Entry<String, String> entry : elementos.entrySet()) {
                        if (grupoId.equals(entry.getValue())) {
                            String elementoDelGrupo = entry.getKey();
                            if (isPestañaHijaDeElemento(userData, elementoDelGrupo)) {
                                perteneceAlGrupo = true;
                                System.out.println("DEBUG: Found child element of group at position " + i + ": " + userData + 
                                                 " (child of " + elementoDelGrupo + ")");
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
            System.out.println("DEBUG: calcularPosicionDentroDeGrupo - No elements found for group " + grupoId + 
                             ", using safe position after menu");
            return calcularPosicionSeguaDespuesDelMenu(tabPane);
        }
        
        int nuevaPosicion = ultimaPosicionDelGrupo + 1;
        System.out.println("DEBUG: calcularPosicionDentroDeGrupo - Last position of group " + grupoId + 
                         " was " + ultimaPosicionDelGrupo + ", new position: " + nuevaPosicion);
        
        return nuevaPosicion;
    }
    
    /**
     * Calcula una posición segura después del menú principal.
     */
    private static int calcularPosicionSeguaDespuesDelMenu(TabPane tabPane) {
        // Buscar la posición del menú principal
        for (int i = 0; i < tabPane.getTabs().size(); i++) {
            Tab tab = tabPane.getTabs().get(i);
            String tabText = tab.getText();
            
            if (tabText != null && (tabText.contains("Menú") || tabText.contains("Menu") || 
                                   tabText.contains("Principal") || tabText.contains("Main"))) {
                int posicionSegura = i + 1;
                System.out.println("DEBUG: calcularPosicionSeguaDespuesDelMenu - Menu at " + i + 
                                 ", safe position: " + posicionSegura);
                return posicionSegura;
            }
        }
        
        // Si no se encuentra el menú, usar posición 1
        System.out.println("DEBUG: calcularPosicionSeguaDespuesDelMenu - Menu not found, using position 1");
        return 1;
    }
    
    /**
     * Verifica si una pestaña es hija de un elemento específico.
     */
    private static boolean isPestañaHijaDeElemento(String pestañaUserData, String elementoId) {
        if (pestañaUserData == null || elementoId == null) {
            return false;
        }
        
        // Para pestañas de creación directas (ej: "creacion_1234" es hija de "editor_1234")
        if (elementoId.startsWith("editor_")) {
            String baseId = elementoId.replace("editor_", "");
            String expectedCreacionId = "creacion_" + baseId;
            
            if (pestañaUserData.equals(expectedCreacionId) || 
                pestañaUserData.startsWith("terminales_" + expectedCreacionId) ||
                pestañaUserData.startsWith("no_terminales_" + expectedCreacionId) ||
                pestañaUserData.startsWith("producciones_" + expectedCreacionId)) {
                return true;
            }
        }
        
        // Para pestañas de simulador (ej: "gramatica_simulador_123" es hija de "simulador_123")
        if (elementoId.startsWith("simulador_")) {
            if (pestañaUserData.equals("gramatica_" + elementoId) ||
                pestañaUserData.equals("funciones_error_" + elementoId)) {
                return true;
            }
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
        
        System.out.println("DEBUG: calcularPosicionEditor - Menu at: " + menuPosition + 
                         ", TabPane size: " + tabPane.getTabs().size() + 
                         ", positioning new editor at: " + posicionFinal);
        
        return posicionFinal;
    }
    
    /**
     * Verifica si una pestaña está relacionada con un editor (es un editor o es hija de un editor).
     */
    private static boolean isTabRelatedToEditor(Tab tab) {
        // Si el contenido es un editor
        if (isEditorContent(tab.getContent())) {
            return true;
        }
        
        // Si es una pestaña hija de un editor
        if (tab.getUserData() != null) {
            String userData = tab.getUserData().toString();
            // Las pestañas hijas de editores tienen IDs que contienen "editor_", "creacion_", 
            // o son pestañas auxiliares (terminales, no terminales, producciones)
            return userData.contains("editor_") || 
                   userData.startsWith("creacion_") ||
                   userData.startsWith("terminales_") ||
                   userData.startsWith("no_terminales_") ||
                   userData.startsWith("producciones_");
        }
        
        return false;
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
    
    public static void closeChildTabs(TabPane tabPane, String parentId) {
        Map<String, List<Tab>> relations = parentChildRelations.get(tabPane);
        Map<Class<?>, Tab> paneTabs = tabInstances.get(tabPane);
        
        if (relations != null && relations.containsKey(parentId)) {
            List<Tab> childTabs = new ArrayList<>(relations.get(parentId));
            for (Tab childTab : childTabs) {
                if (tabPane.getTabs().contains(childTab)) {
                    tabPane.getTabs().remove(childTab);
                    
                    // También eliminar de tabInstances si es necesario
                    if (paneTabs != null) {
                        // Buscar y eliminar la entrada correspondiente en tabInstances
                        paneTabs.entrySet().removeIf(entry -> entry.getValue() == childTab);
                    }
                }
            }
            relations.remove(parentId);
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
        System.out.println("DEBUG: findChildTabInGroup - Looking for childId: " + childId + " in parent: " + parentId);
        
        // Obtener el grupo del padre
        String grupoDelPadre = obtenerGrupoDeElemento(tabPane, parentId);
        System.out.println("DEBUG: Parent " + parentId + " belongs to group: " + grupoDelPadre);
        
        // Buscar en las relaciones padre-hijo registradas DENTRO DEL MISMO GRUPO
        Map<String, List<Tab>> relations = parentChildRelations.get(tabPane);
        if (relations != null && relations.containsKey(parentId)) {
            List<Tab> childTabs = relations.get(parentId);
            System.out.println("DEBUG: Found " + childTabs.size() + " registered child tabs for parent: " + parentId);
            for (Tab childTab : childTabs) {
                if (childTab.getUserData() != null && 
                    childTab.getUserData().toString().equals(childId) &&
                    tabPane.getTabs().contains(childTab)) {
                    
                    // Verificar que la pestaña hija realmente pertenezca al grupo correcto
                    if (verificarPestañaPerteneceAGrupo(childTab, grupoDelPadre, parentId)) {
                        System.out.println("DEBUG: Found existing child tab in registered relations: " + childId + " for parent: " + parentId);
                        return childTab;
                    }
                }
            }
        }
        
        // Si no se encuentra en las relaciones registradas, NO buscar más
        // Esto evita la detección cruzada entre grupos
        System.out.println("DEBUG: No existing child tab found for: " + childId + " in group: " + grupoDelPadre);
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
            
            System.out.println("DEBUG: verificarPestañaPerteneceAGrupo - childId: " + childId + " -> simuladorId: " + simuladorIdFromChild);
            
            // Verificar que el simulador del childId pertenezca al mismo grupo
            String grupoDelSimulador = obtenerGrupoDeElemento(childTab.getTabPane(), simuladorIdFromChild);
            boolean pertenece = grupoDelPadre.equals(grupoDelSimulador);
            System.out.println("DEBUG: Child " + childId + " simulator group: " + grupoDelSimulador + 
                             ", parent group: " + grupoDelPadre + ", belongs: " + pertenece);
            return pertenece;
        }
        
        // Para pestañas de creación y símbolos de editores
        return isChildIdBelongsToParent(childId, parentId);
    }
    
    /**
     * Verifica si un childId realmente pertenece a un parentId específico basado en los patrones de ID.
     */
    private static boolean isChildIdBelongsToParent(String childId, String parentId) {
        if (childId == null || parentId == null) {
            return false;
        }
        
        System.out.println("DEBUG: Checking if childId '" + childId + "' belongs to parentId '" + parentId + "'");
        
        // Extraer el identificador base del parentId (ej: "editor_1234" -> "1234")
        String parentBaseId = extractBaseId(parentId);
        System.out.println("DEBUG: Parent base ID: " + parentBaseId);
        
        // Para pestañas de creación directas (ej: "creacion_1234")
        if (childId.startsWith("creacion_") && childId.contains(parentBaseId)) {
            System.out.println("DEBUG: Child is a direct creation tab of this parent");
            return true;
        }
        
        // Para pestañas de símbolos (ej: "terminales_creacion_1234")
        if ((childId.startsWith("terminales_") || childId.startsWith("no_terminales_") || childId.startsWith("producciones_")) &&
            childId.contains("creacion_" + parentBaseId)) {
            System.out.println("DEBUG: Child is a symbol tab of this parent's creation");
            return true;
        }
        
        System.out.println("DEBUG: Child does NOT belong to this parent");
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
     */
    public static void reasignarNumerosGruposGramatica(TabPane tabPane) {
        Map<String, Integer> gruposNumerados = new HashMap<>();
        List<String> gruposOrdenados = new ArrayList<>();
        
        // Primero, recolectar todos los grupos y sus timestamps
        for (Map.Entry<String, Integer> entry : gruposGramatica.get(tabPane).entrySet()) {
            String grupoId = entry.getKey();
            gruposOrdenados.add(grupoId);
        }
        
        // Ordenar los grupos por timestamp
        Collections.sort(gruposOrdenados, (g1, g2) -> {
            long t1 = extraerTimestampDeGrupoId(g1);
            long t2 = extraerTimestampDeGrupoId(g2);
            return Long.compare(t1, t2);
        });
        
        // Asignar números a los grupos
        for (int i = 0; i < gruposOrdenados.size(); i++) {
            String grupoId = gruposOrdenados.get(i);
            gruposNumerados.put(grupoId, i + 1);
        }
        
        // Actualizar los números en el mapa de grupos
        gruposGramatica.get(tabPane).clear();
        gruposGramatica.get(tabPane).putAll(gruposNumerados);
        
        // Determinar si debemos mostrar numeración (solo si hay más de un grupo)
        boolean mostrarNumeracion = gruposOrdenados.size() > 1;
        
        // Actualizar los títulos de las pestañas
        for (Tab tab : tabPane.getTabs()) {
            String userData = (String) tab.getUserData();
            if (userData != null) {
                String grupoId = elementoToGrupo.get(tabPane).get(userData);
                if (grupoId != null) {
                    int numeroGrupo = mostrarNumeracion ? gruposNumerados.get(grupoId) : -1;
                    actualizarPestañasDelGrupo(tabPane, userData, numeroGrupo);
                }
            }
            
            // Actualizar simulaciones si el contenido es una SimulacionFinal
            if (tab.getContent() instanceof simulador.SimulacionFinal) {
                simulador.SimulacionFinal sim = (simulador.SimulacionFinal) tab.getContent();
                sim.actualizarTitulosPestañas();
            }
        }
        
        // Reasignar números de simulaciones dentro de cada grupo
        simulador.SimulacionFinal.reasignarNumerosSimulaciones(tabPane);
    }
    
    /**
     * Extrae el timestamp de un grupoId (formato: grupo_TIMESTAMP_CONTADOR).
     */
    private static long extraerTimestampDeGrupoId(String grupoId) {
        try {
            // Formato esperado: "grupo_1748704312294_1"
            String[] parts = grupoId.split("_");
            if (parts.length >= 2) {
                return Long.parseLong(parts[1]);
            }
        } catch (NumberFormatException e) {
            System.out.println("DEBUG: Could not extract timestamp from grupoId: " + grupoId);
        }
        return 0; // Fallback timestamp
    }
    
    /**
     * Actualiza todas las pestañas que pertenecen a un grupo de gramática específico.
     */
    private static void actualizarPestañasDelGrupo(TabPane tabPane, String elementoId, int numeroGrupo) {
        // Si el número de grupo es inválido, intentar obtenerlo
        if (numeroGrupo < 0) {
            numeroGrupo = obtenerNumeroGrupo(tabPane, elementoId);
            System.out.println("DEBUG actualizarPestañasDelGrupo: Recalculated group number for " + elementoId + ": " + numeroGrupo);
        }

        // Determinar si hay más de un grupo
        boolean mostrarGrupo = contarGruposActivos(tabPane) > 1;

        System.out.println("DEBUG: Updating group " + numeroGrupo + " for element " + elementoId + 
                         ", showing group numbers: " + mostrarGrupo);
        
        // Actualizar todas las pestañas del TabPane que pertenezcan a este grupo
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() != null) {
                String userData = tab.getUserData().toString();
                System.out.println("DEBUG: Checking tab userData: " + userData + " for group " + numeroGrupo);
                
                // 1. ACTUALIZAR ELEMENTO PRINCIPAL (editor o simulador)
                if (userData.equals(elementoId)) {
                    if (elementoId.startsWith("editor_")) {
                        // Actualizar editores automáticamente
                        String tituloBase = obtenerTituloBaseEditor(tabPane);
                        if (mostrarGrupo && numeroGrupo > 0) {
                            tab.setText(numeroGrupo + "-" + tituloBase);
                            System.out.println("DEBUG: Updated editor title to: " + numeroGrupo + "-" + tituloBase);
                        } else {
                            tab.setText(tituloBase);
                            System.out.println("DEBUG: Updated editor title to: " + tituloBase);
                        }
                    } else if (elementoId.startsWith("simulador_")) {
                        // Lógica de actualización para simuladores
                        String tituloActual = tab.getText();
                        
                        if (tituloActual.contains("Simulador de gramáticas") || 
                            tituloActual.contains("Grammar Simulator") ||
                            tituloActual.contains("Simulateur de grammaires") ||
                            tituloActual.contains("Simulador") ||
                            tituloActual.contains("Simulator") ||
                            tituloActual.contains("Simulateur")) {
                            
                            // Es un simulador independiente (paso 6) - SIEMPRE actualizar
                            String tituloBase = obtenerTituloBaseSimulador(tabPane);
                            if (mostrarGrupo && numeroGrupo > 0) {
                                tab.setText(numeroGrupo + "-" + tituloBase);
                                System.out.println("DEBUG: Updated independent simulator title to: " + numeroGrupo + "-" + tituloBase);
                            } else {
                                tab.setText(tituloBase);
                                System.out.println("DEBUG: Updated independent simulator title to: " + tituloBase);
                            }
                            
                        } else if (tituloActual.contains("Asistente") ||
                                   tituloActual.contains("Assistant") ||
                                   tituloActual.contains("Simulation") ||
                                   tituloActual.contains("Simulación") ||
                                   tituloActual.contains("Wizard")) {
                            
                            // Es un simulador de editor (Asistente) - SIEMPRE actualizar para asegurar sincronización
                            String tituloBase = extraerTituloBaseAsistente(tabPane);
                            if (mostrarGrupo && numeroGrupo > 0) {
                                tab.setText(numeroGrupo + "-" + tituloBase);
                                System.out.println("DEBUG: Updated assistant simulator title to: " + numeroGrupo + "-" + tituloBase);
                            } else {
                                tab.setText(tituloBase);
                                System.out.println("DEBUG: Updated assistant simulator title to: " + tituloBase);
                            }
                        }
                    }
                }
                
                // 2. ACTUALIZAR PESTAÑAS HIJAS DE EDITORES
                if (elementoId.startsWith("editor_")) {
                    String expectedCreacionId = "creacion_" + elementoId.replace("editor_", "");
                    
                    // Pestañas de creación (hijas directas del editor)
                    if (userData.equals(expectedCreacionId)) {
                        // Usar el título correcto del ResourceBundle basado en el paso actual
                        String tituloCorrectoConPaso = obtenerTituloCreacionActual(tabPane, tab);
                        if (mostrarGrupo && numeroGrupo > 0) {
                            // Extraer solo la base del título (sin "Edición:" parte)
                            String tituloBase = extraerTituloBaseCreacion(tituloCorrectoConPaso);
                            tab.setText(numeroGrupo + "-" + tituloBase);
                            System.out.println("DEBUG: Updated creation tab to: " + numeroGrupo + "-" + tituloBase);
                        } else {
                            tab.setText(tituloCorrectoConPaso);
                            System.out.println("DEBUG: Updated creation tab to: " + tituloCorrectoConPaso);
                        }
                    }
                    
                    // Pestañas de símbolos y producciones (nietas del editor)
                    else if (userData.startsWith("terminales_" + expectedCreacionId) || 
                             userData.startsWith("no_terminales_" + expectedCreacionId) || 
                             userData.startsWith("producciones_" + expectedCreacionId)) {
                        
                        // Usar nombres simplificados
                        String nombreSimple;
                        if (userData.startsWith("terminales_")) {
                            nombreSimple = "Terminales";
                        } else if (userData.startsWith("no_terminales_")) {
                            nombreSimple = "No Terminales";
                        } else {
                            nombreSimple = "Producciones";
                        }
                        
                        if (mostrarGrupo && numeroGrupo > 0) {
                            tab.setText(numeroGrupo + "-" + nombreSimple);
                            System.out.println("DEBUG: Updated symbol tab to: " + numeroGrupo + "-" + nombreSimple);
                        } else {
                            tab.setText(nombreSimple);
                        }
                    }
                }
                
                // 3. ACTUALIZAR PESTAÑAS HIJAS DE SIMULADORES
                if (elementoId.startsWith("simulador_")) {
                    // Pestañas de gramática original
                    if (userData.equals("gramatica_" + elementoId)) {
                        String tituloBase = obtenerTituloBaseGramaticaOriginal(tabPane);
                        if (mostrarGrupo && numeroGrupo > 0) {
                            tab.setText(numeroGrupo + "-" + tituloBase);
                            System.out.println("DEBUG: Updated grammar tab to: " + numeroGrupo + "-" + tituloBase);
                        } else {
                            tab.setText(tituloBase);
                        }
                    }
                    
                    // Pestañas de funciones de error
                    else if (userData.equals("funciones_error_" + elementoId)) {
                        String tituloBase = obtenerTituloBaseFuncionesError(tabPane);
                        if (mostrarGrupo && numeroGrupo > 0) {
                            tab.setText(numeroGrupo + "-" + tituloBase);
                            System.out.println("DEBUG: Updated error functions tab to: " + numeroGrupo + "-" + tituloBase);
                        } else {
                            tab.setText(tituloBase);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Actualiza el título de un elemento con numeración de grupo.
     */
    private static void actualizarTituloElemento(Tab elementoTab, int numeroGrupo) {
        // Obtener el número de grupo actual si no se proporciona uno válido
        if (numeroGrupo < 0 && elementoTab.getUserData() != null) {
            String elementoId = elementoTab.getUserData().toString();
            numeroGrupo = obtenerNumeroGrupo(elementoTab.getTabPane(), elementoId);
            System.out.println("DEBUG actualizarTituloElemento: Recalculated group number for " + elementoId + ": " + numeroGrupo);
        }

        String tituloBase = obtenerTituloBaseEditor(elementoTab.getTabPane());
        if (numeroGrupo > 0) {
            elementoTab.setText(numeroGrupo + "-" + tituloBase);
            System.out.println("DEBUG actualizarTituloElemento: Updated title with group: " + numeroGrupo + "-" + tituloBase);
        } else {
            elementoTab.setText(tituloBase);
            System.out.println("DEBUG actualizarTituloElemento: Updated title without group: " + tituloBase);
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
     * Obtiene el título base para simuladores.
     */
    private static String obtenerTituloBaseSimulador(TabPane tabPane) {
        // Intentar usar el ResourceBundle si está disponible
        try {
            java.util.ResourceBundle bundle = resourceBundles.get(tabPane);
            if (bundle != null) {
                return bundle.getString("simulador.tab.paso6");
            }
        } catch (Exception e) {
            // Si no se puede obtener del bundle, usar valor por defecto
        }
        // Usar directamente el nombre corto como fallback
        return "Simulador";
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
     * Extrae el título base de un asistente de simulación, removiendo la numeración si existe.
     */
    private static String extraerTituloBaseAsistente(TabPane tabPane) {
        // Intentar usar el ResourceBundle si está disponible
        try {
            java.util.ResourceBundle bundle = resourceBundles.get(tabPane);
            if (bundle != null) {
                return bundle.getString("simulador.asistente");
            }
        } catch (Exception e) {
            // Si no se puede obtener del bundle, usar valor por defecto
        }
        // Usar directamente el nombre corto como fallback
        return "Asistente Simulación";
    }
    
    /**
     * Obtiene el título base para pestañas de creación.
     */
    private static String obtenerTituloBaseCreacion(Tab tab, TabPane tabPane) {
        String titulo = tab.getText();
        
        // Remover numeración existente si la hay (múltiples idiomas)
        if (titulo.contains("Creación ") && titulo.contains(": ")) {
            return titulo.substring(titulo.indexOf(": ") + 2);
        } else if (titulo.contains("Creation ") && titulo.contains(": ")) {
            return titulo.substring(titulo.indexOf(": ") + 2);
        } else if (titulo.contains("Création ") && titulo.contains(": ")) {
            return titulo.substring(titulo.indexOf(": ") + 2);
        }
        
        // Si no tiene numeración, intentar obtener desde ResourceBundle
        try {
            java.util.ResourceBundle bundle = resourceBundles.get(tabPane);
            if (bundle != null) {
                // Intentar identificar el paso por el contenido o userData
                if (titulo.contains("Paso 1") || titulo.contains("Step 1") || titulo.contains("Étape 1")) {
                    return bundle.getString("creacion.tab.paso1");
                } else if (titulo.contains("Paso 2") || titulo.contains("Step 2") || titulo.contains("Étape 2")) {
                    return bundle.getString("creacion.tab.paso2");
                } else if (titulo.contains("Paso 3") || titulo.contains("Step 3") || titulo.contains("Étape 3")) {
                    return bundle.getString("creacion.tab.paso3");
                } else if (titulo.contains("Paso 4") || titulo.contains("Step 4") || titulo.contains("Étape 4")) {
                    return bundle.getString("creacion.tab.paso4");
                }
            }
        } catch (Exception e) {
            // Si no se puede obtener del bundle, usar el título actual
        }
        
        return titulo;
    }
    
    /**
     * Obtiene el título base para pestañas de símbolos.
     */
    private static String obtenerTituloBaseSimbolos(String userData, TabPane tabPane) {
        try {
            java.util.ResourceBundle bundle = resourceBundles.get(tabPane);
            if (bundle != null) {
                if (userData.startsWith("terminales_")) {
                    return bundle.getString("creacion2.tab.modificar.terminales");
                } else if (userData.startsWith("no_terminales_")) {
                    return bundle.getString("creacion2.tab.modificar.no.terminales");
                } else if (userData.startsWith("producciones_")) {
                    return bundle.getString("creacion3.tab.modificar.producciones");
                }
            }
        } catch (Exception e) {
            // Si no se puede obtener del bundle, usar valores por defecto
        }
        
        // Valores por defecto en español
        if (userData.startsWith("terminales_")) {
            return "Símbolos Terminales";
        } else if (userData.startsWith("no_terminales_")) {
            return "Símbolos No Terminales";
        } else if (userData.startsWith("producciones_")) {
            return "Producciones";
        }
        return "Modificar";
    }

    /**
     * Establece el ResourceBundle para un TabPane específico.
     */
    public static void setResourceBundle(TabPane tabPane, java.util.ResourceBundle bundle) {
        resourceBundles.put(tabPane, bundle);
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
     * Calcula la posición correcta para un Simulador independiente (después del menú principal y todos los elementos existentes).
     */
    private static int calcularPosicionSimulador(TabPane tabPane) {
        int menuPosition = -1;
        
        System.out.println("DEBUG: calcularPosicionSimulador - Looking for menu position...");
        
        // Buscar la pestaña del menú principal
        for (int i = 0; i < tabPane.getTabs().size(); i++) {
            Tab tab = tabPane.getTabs().get(i);
            String tabText = tab.getText();
            
            // Identificar el menú principal por su título
            if (tabText != null && (tabText.contains("Menú") || tabText.contains("Menu") || 
                                   tabText.contains("Principal") || tabText.contains("Main"))) {
                menuPosition = i;
                System.out.println("DEBUG: Found menu at position: " + i + " with title: " + tabText);
                break;
            }
        }
        
        if (menuPosition != -1) {
            // Buscar la última pestaña relacionada con cualquier elemento raíz (editor o simulador)
            int lastElementRelatedPosition = menuPosition;
            
            System.out.println("DEBUG: Searching for element-related tabs after menu position " + menuPosition + "...");
            
            for (int i = menuPosition + 1; i < tabPane.getTabs().size(); i++) {
                Tab tab = tabPane.getTabs().get(i);
                if (isTabRelatedToElementoRaiz(tab)) {
                    lastElementRelatedPosition = i;
                    System.out.println("DEBUG: Found element-related tab at position " + i + ": " + tab.getText() + " (userData: " + tab.getUserData() + ")");
                }
            }
            
            int newPosition = lastElementRelatedPosition + 1;
            System.out.println("DEBUG: New simulator will be positioned at: " + newPosition);
            return newPosition;
        }
        
        // Si no encontramos el menú principal, colocar al principio
        System.out.println("DEBUG: Menu not found, positioning at beginning");
        return 0;
    }
    
    /**
     * Verifica si una pestaña está relacionada con un elemento raíz (editor o simulador) y sus hijos.
     */
    private static boolean isTabRelatedToElementoRaiz(Tab tab) {
        // Si el contenido es un editor o simulador
        if (isEditorContent(tab.getContent()) || isSimuladorTab(tab)) {
            return true;
        }
        
        // Si es una pestaña hija de un editor
        if (tab.getUserData() != null) {
            String userData = tab.getUserData().toString();
            // Las pestañas hijas de editores tienen IDs que contienen "editor_", "creacion_", 
            // o son pestañas auxiliares (terminales, no terminales, producciones)
            if (userData.contains("editor_") || 
                userData.startsWith("creacion_") ||
                userData.startsWith("terminales_") ||
                userData.startsWith("no_terminales_") ||
                userData.startsWith("producciones_")) {
                return true;
            }
            
            // Las pestañas hijas de simuladores tienen IDs específicos
            if (userData.startsWith("gramatica_simulador_") ||
                userData.startsWith("funciones_error_simulador_")) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Obtiene el título correcto de una pestaña de creación basado en su paso actual.
     */
    private static String obtenerTituloCreacionActual(TabPane tabPane, Tab tab) {
        try {
            java.util.ResourceBundle bundle = resourceBundles.get(tabPane);
            if (bundle != null) {
                // Usar la nueva clave específica para asistente de editor
                return bundle.getString("editor.asistente");
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Error obtaining creation title from ResourceBundle: " + e.getMessage());
        }
        
        // Fallback: usar título genérico en español
        return "Asistente Editor";
    }
    
    /**
     * Extrae el título base de una pestaña de creación, removiendo la numeración de grupo si existe.
     */
    private static String extraerTituloBaseCreacion(String tituloCompleto) {
        if (tituloCompleto == null) return "";
        
        // Si el título ya tiene formato "Titulo Numero", remover el número del final
        String[] partes = tituloCompleto.trim().split("\\s+");
        if (partes.length >= 2) {
            String ultimaParte = partes[partes.length - 1];
            // Si la última parte es un número, removerla
            try {
                Integer.parseInt(ultimaParte);
                // Es un número, reconstruir título sin el número
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < partes.length - 1; i++) {
                    if (i > 0) sb.append(" ");
                    sb.append(partes[i]);
                }
                return sb.toString();
            } catch (NumberFormatException e) {
                // No es un número, usar título completo
            }
        }
        
        return tituloCompleto;
    }
} 