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
            // Es una pestaña hija, colocarla después del padre
            Tab parentTab = findTabByUserData(tabPane, parentId);
            if (parentTab != null) {
                int parentIndex = tabPane.getTabs().indexOf(parentTab);
                
                // Buscar la última pestaña hija de este padre
                Map<String, List<Tab>> relations = parentChildRelations.get(tabPane);
                if (relations.containsKey(parentId)) {
                    List<Tab> siblings = relations.get(parentId);
                    int lastChildIndex = parentIndex;
                    for (Tab sibling : siblings) {
                        int siblingIndex = tabPane.getTabs().indexOf(sibling);
                        if (siblingIndex > lastChildIndex) {
                            lastChildIndex = siblingIndex;
                        }
                    }
                    insertPosition = lastChildIndex + 1;
                } else {
                    insertPosition = parentIndex + 1;
                }
                
                // Registrar la relación padre-hijo
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