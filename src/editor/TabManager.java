package editor;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import java.util.*;

public class TabManager {
    private static final Map<TabPane, Map<Class<?>, Tab>> tabInstances = new HashMap<>();
    private static final Map<TabPane, Map<String, List<Tab>>> parentChildRelations = new HashMap<>();

    public static Tab getOrCreateTab(TabPane tabPane, Class<?> tabType, String title, Object content) {
        return getOrCreateTab(tabPane, tabType, title, content, null, null);
    }

    public static Tab getOrCreateTab(TabPane tabPane, Class<?> tabType, String title, Object content, String parentId, String childId) {
        // Inicializar el mapa para este TabPane si no existe
        tabInstances.computeIfAbsent(tabPane, k -> new HashMap<>());
        parentChildRelations.computeIfAbsent(tabPane, k -> new HashMap<>());

        // Obtener el mapa de pestañas para este TabPane
        Map<Class<?>, Tab> paneTabs = tabInstances.get(tabPane);

        // Si ya existe una pestaña de este tipo, seleccionarla
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
        });

        // Guardar la nueva pestaña en el mapa
        paneTabs.put(tabType, newTab);
        
        // Encontrar la posición donde insertar la pestaña
        int insertPosition = tabPane.getTabs().size();
        
        if (parentId != null && childId != null) {
            // Es una pestaña hija, colocarla después del padre con prioridades
            Tab parentTab = findTabByUserData(tabPane, parentId);
            if (parentTab != null) {
                insertPosition = calcularPosicionHija(tabPane, parentTab, childId, parentId);
                
                // Registrar la relación padre-hijo
                Map<String, List<Tab>> relations = parentChildRelations.get(tabPane);
                relations.computeIfAbsent(parentId, k -> new ArrayList<>()).add(newTab);
            }
        }
        
        // Añadir la pestaña al TabPane en la posición correcta
        if (insertPosition >= tabPane.getTabs().size()) {
            tabPane.getTabs().add(newTab);
        } else {
            tabPane.getTabs().add(insertPosition, newTab);
        }
        
        tabPane.getSelectionModel().select(newTab);
        return newTab;
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
} 