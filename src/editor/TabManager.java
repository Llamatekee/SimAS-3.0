package editor;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import java.util.HashMap;
import java.util.Map;

public class TabManager {
    private static final Map<TabPane, Map<Class<?>, Tab>> tabInstances = new HashMap<>();

    public static Tab getOrCreateTab(TabPane tabPane, Class<?> tabType, String title, Object content) {
        // Inicializar el mapa para este TabPane si no existe
        tabInstances.computeIfAbsent(tabPane, k -> new HashMap<>());

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
        
        // Añadir listener para cuando se cierre la pestaña
        newTab.setOnClosed(event -> {
            Map<Class<?>, Tab> tabs = tabInstances.get(tabPane);
            if (tabs != null) {
                tabs.remove(tabType);
            }
        });

        // Guardar la nueva pestaña en el mapa
        paneTabs.put(tabType, newTab);
        
        // Añadir la pestaña al TabPane
        tabPane.getTabs().add(newTab);
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
} 