package utils;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Monitor singleton que supervisa continuamente todos los TabPanes activos,
 * validando la consistencia de grupos y relaciones padre-hijo, y reparando
 * automáticamente las inconsistencias detectadas.
 */
public class TabPaneMonitor {
    private static TabPaneMonitor instance;
    private final Map<TabPane, TabPaneInfo> monitoredTabPanes = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "TabPaneMonitor");
        t.setDaemon(true);
        return t;
    });
    
    private boolean debugMode = true;
    private boolean movimientoEnProgreso = false;
    private final Map<TabPane, Integer> contadorReparaciones = new ConcurrentHashMap<>();
    
    private TabPaneMonitor() {
        // Iniciar monitoreo periódico cada 5 segundos (reducido de 2)
        scheduler.scheduleAtFixedRate(this::validarConsistenciaGlobal, 3, 5, TimeUnit.SECONDS);
    }
    
    public static TabPaneMonitor getInstance() {
        if (instance == null) {
            synchronized (TabPaneMonitor.class) {
                if (instance == null) {
                    instance = new TabPaneMonitor();
                }
            }
        }
        return instance;
    }
    
    /**
     * Información de estado de un TabPane monitoreado
     */
    private static class TabPaneInfo {
        final TabPane tabPane;
        final ListChangeListener<Tab> tabChangeListener;
        final String identifier;
        long lastValidation = 0;
        Map<String, String> lastKnownGroups = new HashMap<>();
        Map<String, List<String>> lastKnownRelations = new HashMap<>();
        
        TabPaneInfo(TabPane tabPane, String identifier) {
            this.tabPane = tabPane;
            this.identifier = identifier;
            this.tabChangeListener = this::onTabsChanged;
            
            // Registrar el listener
            tabPane.getTabs().addListener(tabChangeListener);
            
            // Tomar snapshot inicial
            takeSnapshot();
        }
        
        void takeSnapshot() {
            lastKnownGroups.clear();
            lastKnownRelations.clear();
            
            // Snapshot de grupos
            Map<String, String> elementos = TabManager.getElementoToGrupo(tabPane);
            if (elementos != null) {
                lastKnownGroups.putAll(elementos);
            }
            
            // Snapshot de relaciones padre-hijo
            Map<String, List<Tab>> relations = TabManager.getParentChildRelations(tabPane);
            if (relations != null) {
                for (Map.Entry<String, List<Tab>> entry : relations.entrySet()) {
                    String parentId = entry.getKey();
                    List<String> childIds = new ArrayList<>();
                    for (Tab childTab : entry.getValue()) {
                        if (childTab.getUserData() != null) {
                            childIds.add(childTab.getUserData().toString());
                        }
                    }
                    lastKnownRelations.put(parentId, childIds);
                }
            }
            
            lastValidation = System.currentTimeMillis();
        }
        
        void onTabsChanged(ListChangeListener.Change<? extends Tab> change) {
            Platform.runLater(() -> {
                
                // Validar inmediatamente después del cambio
                TabPaneMonitor.getInstance().validarConsistenciaTabPane(tabPane, true);
                
                // Tomar nuevo snapshot
                takeSnapshot();
            });
        }
        
        void cleanup() {
            if (tabPane != null && tabChangeListener != null) {
                tabPane.getTabs().removeListener(tabChangeListener);
            }
        }
    }
    
    /**
     * Registra un TabPane para monitoreo continuo
     */
    public void registrarTabPane(TabPane tabPane, String identifier) {
        if (tabPane == null) return;
        
        // Si ya está registrado, no hacer nada
        if (monitoredTabPanes.containsKey(tabPane)) {
            debug("TabPane ya registrado: " + identifier);
            return;
        }
        
        debug("Registrando TabPane para monitoreo: " + identifier);
        
        TabPaneInfo info = new TabPaneInfo(tabPane, identifier);
        monitoredTabPanes.put(tabPane, info);
        
        // Validación inicial
        Platform.runLater(() -> validarConsistenciaTabPane(tabPane, true));
    }
    
    /**
     * Desregistra un TabPane del monitoreo
     */
    public void desregistrarTabPane(TabPane tabPane) {
        TabPaneInfo info = monitoredTabPanes.remove(tabPane);
        if (info != null) {
            debug("Desregistrando TabPane: " + info.identifier);
            info.cleanup();
        }
    }
    
    /**
     * Valida la consistencia global de todos los TabPanes monitoreados
     */
    private void validarConsistenciaGlobal() {
        if (monitoredTabPanes.isEmpty() || movimientoEnProgreso) return;
        
        Platform.runLater(() -> {
            for (Map.Entry<TabPane, TabPaneInfo> entry : monitoredTabPanes.entrySet()) {
                TabPane tabPane = entry.getKey();
                TabPaneInfo info = entry.getValue();
                
                // Validar cada TabPane
                validarConsistenciaTabPane(tabPane, false);
                
                // Actualizar snapshot si ha pasado suficiente tiempo
                if (System.currentTimeMillis() - info.lastValidation > 10000) {
                    info.takeSnapshot();
                }
            }
        });
    }
    
    /**
     * Valida la consistencia de un TabPane específico
     */
    public void validarConsistenciaTabPane(TabPane tabPane, boolean verbose) {
        if (tabPane == null) return;
        
        try {
            TabPaneInfo info = monitoredTabPanes.get(tabPane);
            String identifier = info != null ? info.identifier : "Desconocido";
            
            debug("=== Validando consistencia de TabPane: " + identifier + " ===");
            
            List<String> problemas = new ArrayList<>();
            
            // 1. VALIDAR RELACIONES PADRE-HIJO
            problemas.addAll(validarRelacionesPadreHijo(tabPane));
            
            // 2. VALIDAR CONSISTENCIA DE GRUPOS
            problemas.addAll(validarConsistenciaGrupos(tabPane));
            
            // 3. VALIDAR PESTAÑAS HUÉRFANAS
            problemas.addAll(validarPestañasHuerfanas(tabPane));
            
            // 4. VALIDAR NUMERACIÓN DE GRUPOS
            problemas.addAll(validarNumeracionGrupos(tabPane));
            
            if (!problemas.isEmpty()) {
                // INTENTAR REPARAR AUTOMÁTICAMENTE
                repararInconsistencias(tabPane, problemas);
            } else {
                // Si no hay problemas, reiniciar contador de reparaciones
                contadorReparaciones.remove(tabPane);
                
                if (verbose) {
                    debug("TabPane " + identifier + " está consistente ✓");
                }
            }
            
        } catch (Exception e) {
            System.err.println("[MONITOR] Error durante validación: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Valida que las relaciones padre-hijo sean consistentes
     */
    private List<String> validarRelacionesPadreHijo(TabPane tabPane) {
        List<String> problemas = new ArrayList<>();
        
        Map<String, List<Tab>> relations = TabManager.getParentChildRelations(tabPane);
        if (relations == null) return problemas;
        
        for (Map.Entry<String, List<Tab>> entry : relations.entrySet()) {
            String parentId = entry.getKey();
            List<Tab> children = entry.getValue();
            
            // Verificar que el padre existe en el TabPane
            boolean parentExists = false;
            for (Tab tab : tabPane.getTabs()) {
                if (tab.getUserData() != null && tab.getUserData().toString().equals(parentId)) {
                    parentExists = true;
                    break;
                }
            }
            
            if (!parentExists) {
                problemas.add("Padre inexistente: " + parentId + " tiene hijos pero no está en el TabPane");
                continue;
            }
            
            // Verificar que todos los hijos existen y están en el TabPane
            Iterator<Tab> iterator = children.iterator();
            while (iterator.hasNext()) {
                Tab child = iterator.next();
                if (!tabPane.getTabs().contains(child)) {
                    problemas.add("Hijo huérfano: " + (child.getUserData() != null ? child.getUserData().toString() : "sin ID") + 
                                " está en relaciones pero no en TabPane");
                }
            }
            
            // Verificar que los hijos realmente pertenecen al padre
            for (Tab child : children) {
                if (child.getUserData() != null) {
                    String childId = child.getUserData().toString();
                    if (!TabManager.isPestañaHijaDeElemento(childId, parentId)) {
                        problemas.add("Relación incorrecta: " + childId + " no es hijo válido de " + parentId);
                    }
                }
            }
        }
        
        return problemas;
    }
    
    /**
     * Valida la consistencia de los grupos
     */
    private List<String> validarConsistenciaGrupos(TabPane tabPane) {
        List<String> problemas = new ArrayList<>();
        
        Map<String, String> elementos = TabManager.getElementoToGrupo(tabPane);
        Map<String, Integer> grupos = TabManager.getGruposGramatica(tabPane);
        
        if (elementos == null || grupos == null) return problemas;
        
        // Verificar que todos los elementos en grupos existen como pestañas
        for (Map.Entry<String, String> entry : elementos.entrySet()) {
            String elementId = entry.getKey();
            String grupoId = entry.getValue();
            
            boolean elementExists = false;
            for (Tab tab : tabPane.getTabs()) {
                if (tab.getUserData() != null && tab.getUserData().toString().equals(elementId)) {
                    elementExists = true;
                    break;
                }
            }
            
            if (!elementExists) {
                problemas.add("Elemento fantasma en grupo: " + elementId + " está en grupo " + grupoId + " pero no existe como pestaña");
            }
            
            // Verificar que el grupo tiene número asignado
            if (!grupos.containsKey(grupoId)) {
                problemas.add("Grupo sin número: " + grupoId + " no tiene número asignado");
            }
        }
        
        // Verificar que no hay grupos vacíos con números asignados
        for (Map.Entry<String, Integer> entry : grupos.entrySet()) {
            String grupoId = entry.getKey();
            boolean grupoTieneElementos = elementos.containsValue(grupoId);
            
            if (!grupoTieneElementos) {
                problemas.add("Grupo vacío: " + grupoId + " tiene número " + entry.getValue() + " pero no tiene elementos");
            }
        }
        
        return problemas;
    }
    
    /**
     * Valida que no hay pestañas huérfanas sin relaciones apropiadas
     */
    private List<String> validarPestañasHuerfanas(TabPane tabPane) {
        List<String> problemas = new ArrayList<>();
        
        Map<String, String> elementos = TabManager.getElementoToGrupo(tabPane);
        Map<String, List<Tab>> relations = TabManager.getParentChildRelations(tabPane);
        
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() == null) continue;
            
            String tabId = tab.getUserData().toString();
            
            // Verificar si es un elemento principal
            boolean esElementoPrincipal = elementos != null && elementos.containsKey(tabId);
            
            // Verificar si es hijo de algún elemento
            boolean esHijo = false;
            if (relations != null) {
                for (List<Tab> children : relations.values()) {
                    if (children.contains(tab)) {
                        esHijo = true;
                        break;
                    }
                }
            }
            
            // Si no es elemento principal ni hijo, pero parece ser una pestaña hija
            if (!esElementoPrincipal && !esHijo && esClararmentePestañaHija(tabId)) {
                problemas.add("Pestaña hija huérfana: " + tabId + " parece ser hija pero no tiene relación padre registrada");
            }
        }
        
        return problemas;
    }
    
    /**
     * Determina si un ID claramente pertenece a una pestaña hija
     */
    private boolean esClararmentePestañaHija(String tabId) {
        return tabId.startsWith("creacion_") ||
               tabId.startsWith("terminales_") ||
               tabId.startsWith("no_terminales_") ||
               tabId.startsWith("producciones_") ||
               tabId.startsWith("gramatica_") ||
               tabId.startsWith("funciones_error_") ||
               tabId.startsWith("simulacion_") ||
               tabId.startsWith("derivacion_") ||
               tabId.startsWith("arbol_");
    }
    
    /**
     * Valida que la numeración de grupos sea secuencial
     */
    private List<String> validarNumeracionGrupos(TabPane tabPane) {
        List<String> problemas = new ArrayList<>();
        
        Map<String, Integer> grupos = TabManager.getGruposGramatica(tabPane);
        if (grupos == null || grupos.isEmpty()) return problemas;
        
        // Obtener números ordenados
        List<Integer> numeros = new ArrayList<>(grupos.values());
        Collections.sort(numeros);
        
        // Verificar que la numeración sea secuencial desde 1
        for (int i = 0; i < numeros.size(); i++) {
            int expectedNumber = i + 1;
            int actualNumber = numeros.get(i);
            
            if (actualNumber != expectedNumber) {
                problemas.add("Numeración no secuencial: se esperaba " + expectedNumber + " pero se encontró " + actualNumber);
                break;
            }
        }
        
        return problemas;
    }
    
    /**
     * Intenta reparar automáticamente las inconsistencias detectadas
     */
    private void repararInconsistencias(TabPane tabPane, List<String> problemas) {
        // No reparar si hay un movimiento en progreso
        if (movimientoEnProgreso) {
            debug("Saltando reparación - movimiento en progreso");
            return;
        }
        
        // Limitar reparaciones para evitar bucles infinitos
        int reparaciones = contadorReparaciones.getOrDefault(tabPane, 0);
        if (reparaciones >= 3) {
            debug("Límite de reparaciones alcanzado para este TabPane, saltando...");
            return;
        }
        
        debug("Intentando reparar " + problemas.size() + " inconsistencias (intento " + (reparaciones + 1) + "/3)...");
        contadorReparaciones.put(tabPane, reparaciones + 1);
        
        boolean reparacionExitosa = false;
        
        // 1. Limpiar relaciones huérfanas
        if (limpiarRelacionesHuerfanas(tabPane)) {
            reparacionExitosa = true;
        }
        
        // 2. Registrar pestañas hijas huérfanas
        if (registrarPestañasHijasHuerfanas(tabPane)) {
            reparacionExitosa = true;
        }
        
        // 3. Limpiar elementos fantasma de grupos
        if (limpiarElementosFantasma(tabPane)) {
            reparacionExitosa = true;
        }
        
        // 4. Reasignar numeración de grupos
        if (reparacionExitosa) {
            TabManager.reasignarNumerosGruposGramatica(tabPane);
            debug("Reparación completada - forzando renumeración");
        }
        
        // Reiniciar contador si todo está bien
        if (!reparacionExitosa) {
            contadorReparaciones.remove(tabPane);
        }
    }
    
    /**
     * Limpia relaciones padre-hijo donde el padre o hijos ya no existen
     */
    private boolean limpiarRelacionesHuerfanas(TabPane tabPane) {
        Map<String, List<Tab>> relations = TabManager.getParentChildRelations(tabPane);
        if (relations == null) return false;
        
        boolean cambioRealizado = false;
        Iterator<Map.Entry<String, List<Tab>>> iterator = relations.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<String, List<Tab>> entry = iterator.next();
            String parentId = entry.getKey();
            List<Tab> children = entry.getValue();
            
            // Verificar si el padre existe
            boolean parentExists = false;
            for (Tab tab : tabPane.getTabs()) {
                if (tab.getUserData() != null && tab.getUserData().toString().equals(parentId)) {
                    parentExists = true;
                    break;
                }
            }
            
            if (!parentExists) {
                debug("Removiendo relación huérfana para padre inexistente: " + parentId);
                iterator.remove();
                cambioRealizado = true;
                continue;
            }
            
            // Limpiar hijos que ya no existen
            children.removeIf(child -> !tabPane.getTabs().contains(child));
            
            // Si no quedan hijos, remover la entrada completa
            if (children.isEmpty()) {
                debug("Removiendo relación vacía para: " + parentId);
                iterator.remove();
                cambioRealizado = true;
            }
        }
        
        return cambioRealizado;
    }
    
    /**
     * Registra pestañas hijas que no tienen relación padre registrada
     */
    private boolean registrarPestañasHijasHuerfanas(TabPane tabPane) {
        boolean cambioRealizado = false;
        
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() == null) continue;
            
            String tabId = tab.getUserData().toString();
            
            // Si es claramente una pestaña hija pero no está registrada
            if (esClararmentePestañaHija(tabId)) {
                String posiblePadreId = encontrarPadrePotencial(tabPane, tabId);
                
                if (posiblePadreId != null) {
                    // Verificar que no esté ya registrada
                    Map<String, List<Tab>> relations = TabManager.getParentChildRelations(tabPane);
                    List<Tab> hermanos = relations.get(posiblePadreId);
                    
                    if (hermanos == null || !hermanos.contains(tab)) {
                        debug("Registrando pestaña hija huérfana: " + tabId + " como hija de " + posiblePadreId);
                        relations.computeIfAbsent(posiblePadreId, k -> new ArrayList<>()).add(tab);
                        cambioRealizado = true;
                    }
                }
            }
        }
        
        return cambioRealizado;
    }
    
    /**
     * Encuentra el padre potencial de una pestaña hija basándose en su ID
     */
    private String encontrarPadrePotencial(TabPane tabPane, String childId) {
        // Buscar padres potenciales en el TabPane
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() == null) continue;
            
            String potentialParentId = tab.getUserData().toString();
            
            if (TabManager.isPestañaHijaDeElemento(childId, potentialParentId)) {
                return potentialParentId;
            }
        }
        
        return null;
    }
    
    /**
     * Limpia elementos fantasma de los grupos (elementos que están en el grupo pero no existen como pestañas)
     */
    private boolean limpiarElementosFantasma(TabPane tabPane) {
        Map<String, String> elementos = TabManager.getElementoToGrupo(tabPane);
        if (elementos == null) return false;
        
        boolean cambioRealizado = false;
        Iterator<Map.Entry<String, String>> iterator = elementos.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            String elementId = entry.getKey();
            
            // Verificar si el elemento existe como pestaña
            boolean elementExists = false;
            for (Tab tab : tabPane.getTabs()) {
                if (tab.getUserData() != null && tab.getUserData().toString().equals(elementId)) {
                    elementExists = true;
                    break;
                }
            }
            
            if (!elementExists) {
                debug("Removiendo elemento fantasma del grupo: " + elementId);
                iterator.remove();
                cambioRealizado = true;
            }
        }
        
        return cambioRealizado;
    }
    
    /**
     * Fuerza una validación inmediata de un TabPane específico
     */
    public void forzarValidacion(TabPane tabPane) {
        Platform.runLater(() -> validarConsistenciaTabPane(tabPane, true));
    }
    
    /**
     * Obtiene estadísticas de monitoreo
     */
    public String obtenerEstadisticas() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== Estadísticas de TabPaneMonitor ===\n");
        stats.append("TabPanes monitoreados: ").append(monitoredTabPanes.size()).append("\n");
        
        for (Map.Entry<TabPane, TabPaneInfo> entry : monitoredTabPanes.entrySet()) {
            TabPaneInfo info = entry.getValue();
            stats.append("- ").append(info.identifier).append(" (")
                 .append(info.tabPane.getTabs().size()).append(" pestañas)\n");
        }
        
        return stats.toString();
    }
    
    /**
     * Limpia recursos cuando la aplicación se cierra
     */
    public void shutdown() {
        for (TabPaneInfo info : monitoredTabPanes.values()) {
            info.cleanup();
        }
        monitoredTabPanes.clear();
        scheduler.shutdown();
    }
    
    /**
     * Indica que hay un movimiento en progreso, pausando reparaciones automáticas
     */
    public void setMovimientoEnProgreso(boolean enProgreso) {
        this.movimientoEnProgreso = enProgreso;
        debug("Movimiento en progreso: " + enProgreso);
    }
    
    /**
     * Reinicia los contadores de reparación para un TabPane específico
     */
    public void reiniciarContadorReparaciones(TabPane tabPane) {
        contadorReparaciones.remove(tabPane);
        debug("Contador de reparaciones reiniciado para TabPane");
    }
    
    /**
     * Reinicia todos los contadores de reparación
     */
    public void reiniciarTodosLosContadores() {
        contadorReparaciones.clear();
    }
    
    /**
     * Método de debug que imprime mensajes si el modo debug está activado
     */
    private void debug(String message) {
        if (debugMode) {
            //System.out.println("[MONITOR] " + message);
        }
    }
} 