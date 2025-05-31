package editor;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import java.util.*;

public class TabManager {
    private static final Map<TabPane, Map<Class<?>, Tab>> tabInstances = new HashMap<>();
    private static final Map<TabPane, Map<String, List<Tab>>> parentChildRelations = new HashMap<>();
    private static final Map<TabPane, Map<String, Integer>> gruposGramatica = new HashMap<>(); // Mapea editorId -> numeroGrupo
    private static final Map<TabPane, java.util.ResourceBundle> resourceBundles = new HashMap<>();

    public static Tab getOrCreateTab(TabPane tabPane, Class<?> tabType, String title, Object content) {
        return getOrCreateTab(tabPane, tabType, title, content, null, null);
    }

    public static Tab getOrCreateTab(TabPane tabPane, Class<?> tabType, String title, Object content, String parentId, String childId) {
        // Inicializar el mapa para este TabPane si no existe
        tabInstances.computeIfAbsent(tabPane, k -> new HashMap<>());
        parentChildRelations.computeIfAbsent(tabPane, k -> new HashMap<>());
        gruposGramatica.computeIfAbsent(tabPane, k -> new HashMap<>());

        // Obtener el mapa de pestañas para este TabPane
        Map<Class<?>, Tab> paneTabs = tabInstances.get(tabPane);

        // Para editores y pestañas hijas de editores, permitir múltiples instancias (no usar caché)
        boolean isChildOfEditor = (parentId != null && parentId.startsWith("editor_")) || 
                                 (childId != null && (childId.contains("editor_") || 
                                                     childId.contains("creacion_") ||
                                                     childId.startsWith("terminales_") ||
                                                     childId.startsWith("no_terminales_") ||
                                                     childId.startsWith("producciones_")));
        
        if (!isEditorType(tabType) && !isChildOfEditor) {
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
        Tab newTab = new Tab(title, (javafx.scene.Node) content);
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
            if (parentId != null) {
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
            
            // Limpiar grupos de gramática si es necesario
            if (isEditorType(tabType) && parentId != null) {
                Map<String, Integer> grupos = gruposGramatica.get(tabPane);
                if (grupos != null) {
                    grupos.remove(parentId);
                }
            }
            
            // Reasignar numeración de grupos después de cerrar
            if (isEditorType(tabType) || isChildOfEditor) {
                javafx.application.Platform.runLater(() -> {
                    reasignarNumerosGruposGramatica(tabPane);
                });
            }
        });

        // Guardar la nueva pestaña en el mapa (solo para no-editores y no-hijas-de-editores)
        if (!isEditorType(tabType) && !isChildOfEditor) {
            paneTabs.put(tabType, newTab);
        }
        
        // Encontrar la posición donde insertar la pestaña
        int insertPosition = calcularPosicionInsercion(tabPane, tabType, parentId, childId);
        
        // Si es una pestaña hija, registrar la relación padre-hijo
        if (parentId != null && childId != null) {
            Map<String, List<Tab>> relations = parentChildRelations.get(tabPane);
            relations.computeIfAbsent(parentId, k -> new ArrayList<>()).add(newTab);
            
            // Debug: mostrar las relaciones que se están creando
            System.out.println("DEBUG: Registering child relationship - parentId: " + parentId + ", childId: " + childId + ", title: " + title);
        }
        
        // Si es un editor, asignar número de grupo
        if (isEditorType(tabType) && parentId != null) {
            asignarNumeroGrupoEditor(tabPane, parentId);
        }
        
        // Añadir la pestaña al TabPane en la posición correcta
        if (insertPosition >= tabPane.getTabs().size()) {
            tabPane.getTabs().add(newTab);
        } else {
            tabPane.getTabs().add(insertPosition, newTab);
        }
        
        tabPane.getSelectionModel().select(newTab);
        
        // Reasignar numeración de grupos después de añadir
        javafx.application.Platform.runLater(() -> {
            reasignarNumerosGruposGramatica(tabPane);
        });
        
        return newTab;
    }
    
    /**
     * Asigna un número de grupo a un editor basado en su posición.
     */
    private static void asignarNumeroGrupoEditor(TabPane tabPane, String editorId) {
        Map<String, Integer> grupos = gruposGramatica.get(tabPane);
        if (grupos != null && !grupos.containsKey(editorId)) {
            // Contar editores existentes para asignar el siguiente número
            int numeroGrupo = contarEditoresActivos(tabPane);
            grupos.put(editorId, numeroGrupo);
            System.out.println("DEBUG: Assigned group number " + numeroGrupo + " to editor " + editorId);
        }
    }
    
    /**
     * Cuenta el número de editores activos en el TabPane.
     */
    private static int contarEditoresActivos(TabPane tabPane) {
        int contador = 0;
        for (Tab tab : tabPane.getTabs()) {
            if (isEditorContent(tab.getContent())) {
                contador++;
            }
        }
        return contador;
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
        
        // Para otros tipos, al final
        return tabPane.getTabs().size();
    }
    
    /**
     * Calcula la posición correcta para un Editor (después del menú principal y todos los editores existentes).
     */
    private static int calcularPosicionEditor(TabPane tabPane) {
        int menuPosition = -1;
        
        System.out.println("DEBUG: calcularPosicionEditor - Looking for menu position...");
        
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
            // Buscar la última pestaña relacionada con cualquier editor
            int lastEditorRelatedPosition = menuPosition;
            
            System.out.println("DEBUG: Searching for editor-related tabs after menu position " + menuPosition + "...");
            
            for (int i = menuPosition + 1; i < tabPane.getTabs().size(); i++) {
                Tab tab = tabPane.getTabs().get(i);
                if (isTabRelatedToEditor(tab)) {
                    lastEditorRelatedPosition = i;
                    System.out.println("DEBUG: Found editor-related tab at position " + i + ": " + tab.getText() + " (userData: " + tab.getUserData() + ")");
                }
            }
            
            int newPosition = lastEditorRelatedPosition + 1;
            System.out.println("DEBUG: New editor will be positioned at: " + newPosition);
            return newPosition;
        }
        
        // Si no encontramos el menú principal, colocar al principio
        System.out.println("DEBUG: Menu not found, positioning at beginning");
        return 0;
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
    private static boolean isEditorContent(Object content) {
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
        
        // Buscar en las relaciones padre-hijo registradas
        Map<String, List<Tab>> relations = parentChildRelations.get(tabPane);
        if (relations != null && relations.containsKey(parentId)) {
            List<Tab> childTabs = relations.get(parentId);
            System.out.println("DEBUG: Found " + childTabs.size() + " registered child tabs for parent: " + parentId);
            for (Tab childTab : childTabs) {
                if (childTab.getUserData() != null && 
                    childTab.getUserData().toString().equals(childId) &&
                    tabPane.getTabs().contains(childTab)) {
                    System.out.println("DEBUG: Found existing child tab in registered relations: " + childId + " for parent: " + parentId);
                    return childTab;
                }
            }
        }
        
        // Si no se encuentra en las relaciones registradas, buscar por patrón de ID
        // pero solo si el childId realmente pertenece a este grupo
        if (isChildIdBelongsToParent(childId, parentId)) {
            System.out.println("DEBUG: Child belongs to parent by pattern, searching in all tabs...");
            for (Tab tab : tabPane.getTabs()) {
                if (tab.getUserData() != null && 
                    tab.getUserData().toString().equals(childId)) {
                    System.out.println("DEBUG: Found child tab by pattern matching: " + childId + " for parent: " + parentId);
                    return tab;
                }
            }
        } else {
            System.out.println("DEBUG: Child ID " + childId + " does NOT belong to parent " + parentId + " by pattern");
        }
        
        System.out.println("DEBUG: No existing child tab found for: " + childId + " in group: " + parentId);
        return null;
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
     * Reasigna los números de los grupos de gramática según su orden en el TabPane.
     * Cada grupo está encabezado por un Editor y contiene todas sus pestañas relacionadas.
     */
    public static void reasignarNumerosGruposGramatica(TabPane tabPane) {
        if (tabPane == null) return;
        
        System.out.println("DEBUG: reasignarNumerosGruposGramatica called");
        
        // Recopilar todos los editores en orden de aparición
        List<Tab> editoresOrdenados = new ArrayList<>();
        List<String> editorIdsOrdenados = new ArrayList<>();
        
        for (Tab tab : tabPane.getTabs()) {
            if (isEditorContent(tab.getContent())) {
                editoresOrdenados.add(tab);
                String editorId = (String) tab.getUserData();
                if (editorId != null) {
                    editorIdsOrdenados.add(editorId);
                }
                System.out.println("DEBUG: Found editor tab: " + tab.getText() + ", userData: " + tab.getUserData());
            }
        }
        
        System.out.println("DEBUG: Total editors found: " + editoresOrdenados.size());
        
        // Actualizar el mapa de grupos con la nueva numeración
        Map<String, Integer> grupos = gruposGramatica.get(tabPane);
        if (grupos != null) {
            grupos.clear();
            for (int i = 0; i < editorIdsOrdenados.size(); i++) {
                String editorId = editorIdsOrdenados.get(i);
                grupos.put(editorId, i + 1);
                System.out.println("DEBUG: Assigned group number " + (i + 1) + " to editor " + editorId);
            }
        }
        
        // Actualizar títulos de todas las pestañas según su grupo
        if (editoresOrdenados.size() > 1) {
            // Solo mostrar numeración si hay más de un grupo
            for (int i = 0; i < editoresOrdenados.size(); i++) {
                Tab editorTab = editoresOrdenados.get(i);
                String editorId = editorIdsOrdenados.get(i);
                int numeroGrupo = i + 1;
                
                // Actualizar título del editor
                actualizarTituloEditor(editorTab, numeroGrupo);
                
                // Actualizar todas las pestañas que pertenecen a este grupo
                actualizarPestañasDelGrupo(tabPane, editorId, numeroGrupo);
            }
        } else if (editoresOrdenados.size() == 1) {
            // Si solo hay un grupo, remover numeración
            Tab editorTab = editoresOrdenados.get(0);
            String editorId = editorIdsOrdenados.get(0);
            
            actualizarTituloEditor(editorTab, -1); // -1 indica sin numeración
            actualizarPestañasDelGrupo(tabPane, editorId, -1);
        }
    }
    
    /**
     * Actualiza todas las pestañas que pertenecen a un grupo de gramática específico.
     */
    private static void actualizarPestañasDelGrupo(TabPane tabPane, String editorId, int numeroGrupo) {
        System.out.println("DEBUG: Updating group " + numeroGrupo + " for editor " + editorId);
        
        // Obtener el ID de creación esperado para este editor
        String expectedCreacionId = "creacion_" + editorId.replace("editor_", "");
        
        // Actualizar todas las pestañas del TabPane que pertenezcan a este grupo
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() != null) {
                String userData = tab.getUserData().toString();
                System.out.println("DEBUG: Checking tab userData: " + userData + " for group " + numeroGrupo);
                
                // Pestañas de creación (hijas directas del editor)
                if (userData.equals(expectedCreacionId)) {
                    String tituloBase = obtenerTituloBaseCreacion(tab, tabPane);
                    if (numeroGrupo > 0) {
                        tab.setText("Creación " + numeroGrupo + ": " + tituloBase);
                        System.out.println("DEBUG: Updated creation tab to: Creación " + numeroGrupo + ": " + tituloBase);
                    } else {
                        tab.setText(tituloBase);
                    }
                }
                
                // Pestañas de símbolos y producciones (nietas del editor)
                else if (userData.startsWith("terminales_" + expectedCreacionId) || 
                         userData.startsWith("no_terminales_" + expectedCreacionId) || 
                         userData.startsWith("producciones_" + expectedCreacionId)) {
                    
                    String tituloBase = obtenerTituloBaseSimbolos(userData, tabPane);
                    if (numeroGrupo > 0) {
                        tab.setText("Edición " + numeroGrupo + ": " + tituloBase);
                        System.out.println("DEBUG: Updated symbol tab to: Edición " + numeroGrupo + ": " + tituloBase);
                    } else {
                        tab.setText(tituloBase);
                    }
                }
            }
        }
    }
    
    /**
     * Actualiza el título de un editor con numeración de grupo.
     */
    private static void actualizarTituloEditor(Tab editorTab, int numeroGrupo) {
        String tituloBase = obtenerTituloBaseEditor(editorTab.getTabPane());
        if (numeroGrupo > 0) {
            editorTab.setText(tituloBase + " " + numeroGrupo);
        } else {
            editorTab.setText(tituloBase);
        }
    }
    
    /**
     * Obtiene el título base para editores.
     */
    private static String obtenerTituloBaseEditor(TabPane tabPane) {
        try {
            java.util.ResourceBundle bundle = resourceBundles.get(tabPane);
            if (bundle != null) {
                return bundle.getString("editor.title");
            }
        } catch (Exception e) {
            // Si no se puede obtener del bundle, usar valor por defecto
        }
        return "Editor de Gramáticas";
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
     * Obtiene el número de grupo asignado a un editor específico.
     */
    public static int obtenerNumeroGrupo(TabPane tabPane, String editorId) {
        Map<String, Integer> grupos = gruposGramatica.get(tabPane);
        if (grupos != null && grupos.containsKey(editorId)) {
            return grupos.get(editorId);
        }
        return -1; // Sin numeración
    }
} 