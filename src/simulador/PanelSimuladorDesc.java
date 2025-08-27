package simulador;

import gramatica.Gramatica;
import gramatica.FuncionError;
import gramatica.TablaPredictiva;
import gramatica.TablaPredictivaPaso5;
import gramatica.Terminal;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.Parent;
import editor.TabManager;

import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * Controlador para la simulación descendente en JavaFX.
 */
public class PanelSimuladorDesc {

    @FXML
    public TabPane tabPane;

    public Gramatica gramatica;
    private final Gramatica gramaticaOriginal;
    private Tab pestañaSimulacion;
    private int pasoActual;
    private ArrayList<PanelNuevaSimDescPaso> pasos;
    private ResourceBundle bundle;
    private bienvenida.MenuPrincipal menuPane;
    
    // Referencia global a la tabla predictiva extendida (para pasos 5 y 6)
    private TablaPredictivaPaso5 tablaPredictivaExtendidaGlobal;

    // Sistema de identificación para relaciones padre-hijo
    private String simuladorId;
    private static int contadorSimuladores = 0;

    // *** NUEVO: Registro estático de simuladores activos ***
    private static final java.util.Map<String, PanelSimuladorDesc> simuladoresActivos = new java.util.HashMap<>();

    public PanelSimuladorDesc(Gramatica gramatica, TabPane tabPane, ResourceBundle bundle) {
        this(gramatica, tabPane, bundle, null);
    }
    
    public PanelSimuladorDesc(Gramatica gramatica, TabPane tabPane, ResourceBundle bundle, String simuladorIdPersonalizado) {
        this.gramatica = gramatica;
        this.gramaticaOriginal = gramatica;
        this.tabPane = tabPane;
        this.bundle = bundle;
        this.pasoActual = 0;
        
        // Usar ID personalizado si se proporciona, sino generar uno nuevo
        if (simuladorIdPersonalizado != null && !simuladorIdPersonalizado.isEmpty()) {
            this.simuladorId = simuladorIdPersonalizado;
        } else {
            this.simuladorId = "simulador_" + System.currentTimeMillis() + "_" + (++contadorSimuladores);
        }
        
        // *** NUEVO: Registrar este simulador en el registro estático ***
        simuladoresActivos.put(this.simuladorId, this);

        
        // Inicializar funciones de error y tabla predictiva extendida
        inicializarTablaPredictivaYFuncionesError();
        
        // Configurar relaciones padre-hijo
        configurarRelacionesPadreHijo();
        
        // Inicializar pasos
        pasos = new ArrayList<>();
        try {
            pasos.add(new PanelNuevaSimDescPaso1(this));
            pasos.add(new PanelNuevaSimDescPaso2(this));
            pasos.add(new PanelNuevaSimDescPaso3(this));
            pasos.add(new PanelNuevaSimDescPaso4(this));
            pasos.add(new PanelNuevaSimDescPaso5(this));
            pasos.add(new PanelNuevaSimDescPaso6(this.gramatica, this));
            
            // NO llamar a mostrarPasoActual() aquí automáticamente
            // Será llamado explícitamente cuando sea necesario
        } catch (Exception e) {
            e.printStackTrace();
            // Si hay un error, cerrar la pestaña
            cancelarSimulacion();
        }
    }

    public PanelSimuladorDesc(Gramatica gramatica, TabPane tabPane, bienvenida.MenuPrincipal menuPane, String simuladorId, ResourceBundle bundle) {
        this.gramatica = gramatica;
        this.gramaticaOriginal = gramatica;
        this.tabPane = tabPane;
        this.menuPane = menuPane;
        this.bundle = bundle;
        this.pasoActual = 0;
        this.simuladorId = simuladorId;
        
        // *** NUEVO: Registrar este simulador en el registro estático ***
        simuladoresActivos.put(this.simuladorId, this);
        
        // Inicializar funciones de error y tabla predictiva extendida
        inicializarTablaPredictivaYFuncionesError();
        
        // Configurar relaciones padre-hijo
        configurarRelacionesPadreHijo();
        
        // Inicializar pasos
        pasos = new ArrayList<>();
        try {
            pasos.add(new PanelNuevaSimDescPaso1(this));
            pasos.add(new PanelNuevaSimDescPaso2(this));
            pasos.add(new PanelNuevaSimDescPaso3(this));
            pasos.add(new PanelNuevaSimDescPaso4(this));
            pasos.add(new PanelNuevaSimDescPaso5(this));
            pasos.add(new PanelNuevaSimDescPaso6(this.gramatica, this));
            
            // Inicializar el contenido del primer paso sin crear una nueva pestaña
            if (pasos.size() > 0) {
                PanelNuevaSimDescPaso primerPaso = pasos.get(0);
                if (primerPaso instanceof editor.ActualizableTextos) {
                    ((editor.ActualizableTextos) primerPaso).actualizarTextos(bundle);
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            // Si hay un error, cerrar la pestaña
            cancelarSimulacion();
        }
    }

    /**
     * Inicializa la tabla predictiva extendida global y las funciones de error.
     * Se crea una única instancia de TablaPredictivaPaso5 que será compartida entre los pasos 5 y 6.
     */
    private void inicializarTablaPredictivaYFuncionesError() {
        // Verificar si la tabla predictiva existe
        if (this.gramatica.getTPredictiva() == null) {
            // Si no existe, crear una nueva tabla predictiva básica
            this.gramatica.generarTPredictiva();
        }
        
        // Inicializar funciones de error si no existen
        inicializarFuncionesError();
        
        // Crear la tabla predictiva extendida global si no existe
        if (this.tablaPredictivaExtendidaGlobal == null) {
            this.tablaPredictivaExtendidaGlobal = new TablaPredictivaPaso5();
            // Copiar las funciones de error de la tabla básica
            this.tablaPredictivaExtendidaGlobal.setFuncionesError(
                this.gramatica.getTPredictiva().getFuncionesError());
        } 
    }

    /**
     * Inicializa las funciones de error básicas si no existen.
     */
    private void inicializarFuncionesError() {
        // Verificar si ya hay funciones de error
        if (this.gramatica.getTPredictiva().getFuncionesError().isEmpty()) {
            ObservableList<String> simbolosTerminales = this.gramatica.getTerminalesModel();
            TablaPredictiva tPredictiva = this.gramatica.getTPredictiva();
            
            // Función de error inicial
            FuncionError funErrorInicial = new FuncionError(0, 7, "");
            tPredictiva.crearFunError(funErrorInicial);
            
            // Función de error inicial 2
            FuncionError funErrorInicial2 = new FuncionError(1, 2, "");
            tPredictiva.crearFunError(funErrorInicial2);
            
            // Funciones de error para cada terminal
            int x = 2;
            for (int w = 0; w < simbolosTerminales.size(); w++) {
                FuncionError funError = new FuncionError(x, 1, "");
                Terminal term = new Terminal(simbolosTerminales.get(w).toString(), simbolosTerminales.get(w).toString());
                funError.setSimbolo(term);
                tPredictiva.crearFunError(funError);
                x++;
            }
        } 
    }
    
    /**
     * Obtiene la tabla predictiva extendida global para los pasos 5 y 6.
     * @return La tabla predictiva extendida global.
     */
    public TablaPredictivaPaso5 getTablaPredictivaExtendidaGlobal() {
        return this.tablaPredictivaExtendidaGlobal;
    }
    
    /**
     * Establece la tabla predictiva extendida global.
     * @param tabla La tabla predictiva extendida a establecer como global.
     */
    public void setTablaPredictivaExtendidaGlobal(TablaPredictivaPaso5 tabla) {
        this.tablaPredictivaExtendidaGlobal = tabla;
    }
    
    /**
     * Obtiene la gramática original del simulador.
     */
    public Gramatica getGramaticaOriginal() {
        return gramaticaOriginal;
    }

    /**
     * Muestra la gramática original en una nueva pestaña.
     */
    public void mostrarGramaticaOriginal() {
        try {
            // Crear el panel de gramática original con soporte de internacionalización
            PanelGramaticaOriginal panelGramatica = new PanelGramaticaOriginal(gramaticaOriginal, bundle);
            
            // Usar TabManager para obtener o crear la pestaña como hija del simulador
            String childId = "gramatica_" + simuladorId;
            Tab tab = TabManager.getOrCreateTab(tabPane, PanelGramaticaOriginal.class, 
                bundle.getString("simulador.gramatica.titulo"), panelGramatica, simuladorId, childId);
            tab.setUserData(childId);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cancelarSimulacion() {
        // Desregistrar este simulador del registro estático
        desregistrarSimulador(simuladorId);
        
        // Cerrar todas las pestañas hijas usando el TabManager
        TabManager.closeChildTabs(tabPane, simuladorId);
        
        // Cerrar la pestaña principal del simulador
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() != null && tab.getUserData().toString().equals(simuladorId)) {
                tabPane.getTabs().remove(tab);
                break;
            }
        }
        
        // *** NUEVO: Forzar actualización inmediata de numeración ***
        javafx.application.Platform.runLater(() -> {
            TabManager.reasignarNumerosGruposGramatica(tabPane);
        });
    }

    public void cambiarPaso(int paso) {
        // Cerrar pestañas específicas del paso anterior
        cerrarPestañasEspecificasPaso(paso);
        
        // Guardar explícitamente los datos de la tabla si venimos del paso 5
        if (this.pasoActual == 4 && pasos.get(4) instanceof PanelNuevaSimDescPaso5) {
            PanelNuevaSimDescPaso5 paso5 = (PanelNuevaSimDescPaso5) pasos.get(4);
            // Llamar al método público para guardar la tabla
            paso5.guardarDatosTabla();
        }
        
        this.pasoActual = paso;
        
        // Determinar el título base según el paso
        String tituloBase;
        if (paso == 5) {
            // Paso 6: "Simulador"
            tituloBase = bundle.getString("simulador.tab.paso6");
        } else {
            // Pasos 1-5: "Asistente Simulador"
            tituloBase = bundle.getString("simulador.asistente");
        }
        
        // Construir el título final - siempre aplicamos la numeración si está disponible
        String tituloPestaña = construirTituloConNumeracion(tituloBase);
        
        // Verificar que el paso esté dentro de los límites
        if (paso < 0 || paso >= pasos.size()) {
            return;
        }
        
        // Actualizar el paso actual con el bundle actual
        PanelNuevaSimDescPaso pasoActual = pasos.get(paso);
        if (pasoActual instanceof editor.ActualizableTextos) {
            ((editor.ActualizableTextos) pasoActual).actualizarTextos(bundle);
        }
        
        // Buscar la pestaña existente por el simuladorId
        if (pestañaSimulacion == null) {
            for (Tab tab : tabPane.getTabs()) {
                if (tab.getUserData() != null && tab.getUserData().toString().equals(simuladorId)) {
                    pestañaSimulacion = tab;
                    break;
                }
            }
        }
        
        // Si aún no encontramos la pestaña, algo está mal porque debería haber sido creada por el Editor
        if (pestañaSimulacion == null) {
            return;
        }
        
        // Actualizar el contenido y título de la pestaña existente
        pestañaSimulacion.setText(tituloPestaña);
        pestañaSimulacion.setContent(pasoActual.getRoot());
        
        // Seleccionar la pestaña
        tabPane.getSelectionModel().select(pestañaSimulacion);
    }
    
    /**
     * Construye el título con numeración de grupo si es necesario.
     */
    private String construirTituloConNumeracion(String tituloBase) {
        // Obtener el número de grupo para la numeración
        int numeroGrupo = TabManager.obtenerNumeroGrupo(tabPane, simuladorId);
        
        // Construir el título final con numeración si corresponde
        if (numeroGrupo > 0 && numeroGrupo != -1) {
            return numeroGrupo + "-" + tituloBase;
        } else {
            return tituloBase;
        }
    }
    
    /**
     * 🔹 Cierra las pestañas específicas del paso actual antes de cambiar de paso.
     */
    private void cerrarPestañasEspecificasPaso(int nuevoPaso) {
        if (tabPane == null) return;
        
        // Si estamos saliendo del paso 4, cerrar pestañas de funciones de error
        if (pasoActual == 3 && nuevoPaso != 3) { // pasoActual es 0-indexado, paso 4 = índice 3
            java.util.List<Tab> tabsToRemove = new java.util.ArrayList<>();
            
            for (Tab tab : tabPane.getTabs()) {
                if (tab.getUserData() != null) {
                    String userData = tab.getUserData().toString();
                    if (userData.startsWith("funciones_error_" + simuladorId)) {
                        tabsToRemove.add(tab);
                    }
                }
            }
            
            // Cerrar las pestañas encontradas
            for (Tab tab : tabsToRemove) {
                tabPane.getTabs().remove(tab);
            }
        }
    }

    /**
     * 🔹 Cierra la pestaña de funciones de error si está abierta.
     */
    public void cerrarPestañaFuncionesError() {
        String childId = "funciones_error_" + simuladorId;
        
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() != null && tab.getUserData().toString().equals(childId)) {
                tabPane.getTabs().remove(tab);
                break;
            }
        }
    }

    public ResourceBundle getBundle() {
        return bundle;
    }

    public void setBundle(ResourceBundle bundle) {
        this.bundle = bundle;
        
        // Recargar el FXML del paso actual con el nuevo bundle
        if (pasoActual >= 0 && pasoActual < pasos.size()) {
            PanelNuevaSimDescPaso pasoActual = pasos.get(this.pasoActual);
            if (pasoActual instanceof editor.ActualizableTextos) {
                ((editor.ActualizableTextos) pasoActual).actualizarTextos(bundle);
            }
        }
        
        // Actualizar título de la pestaña principal del simulador con el nuevo idioma
        if (pestañaSimulacion != null) {
            // Determinar el título base según el paso actual
            String tituloBase;
            if (this.pasoActual == 5) {
                // Paso 6: "Simulador"
                tituloBase = bundle.getString("simulador.tab.paso6");
            } else {
                // Pasos 1-5 (índices 0-4) son el asistente
                tituloBase = bundle.getString("simulador.asistente");
            }
            
            // Aplicar numeración si corresponde
            String tituloFinal = construirTituloConNumeracion(tituloBase);
            pestañaSimulacion.setText(tituloFinal);
            
            // Refresh the content of the current step
            pestañaSimulacion.setContent(pasos.get(this.pasoActual).getRoot());
        }
        
        // Actualizar las pestañas hijas según sus identificadores
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() != null) {
                String userData = tab.getUserData().toString();
                
                // Actualizar pestaña de gramática
                if (userData.equals("gramatica_" + simuladorId)) {
                    String tituloBase = bundle.getString("simulador.gramatica.original");
                    // Aplicar numeración de grupo si corresponde
                    int numeroGrupo = TabManager.obtenerNumeroGrupo(tabPane, simuladorId);
                    if (numeroGrupo > 0 && numeroGrupo != -1) {
                        tab.setText(numeroGrupo + "-" + tituloBase);
                    } else {
                        tab.setText(tituloBase);
                    }
                    
                    // Actualizar contenido de la pestaña de gramática
                    if (tab.getContent() instanceof editor.ActualizableTextos) {
                        ((editor.ActualizableTextos) tab.getContent()).actualizarTextos(bundle);
                    }
                }
                
                // Actualizar pestaña de funciones de error
                if (userData.equals("funciones_error_" + simuladorId)) {
                    String tituloBase = bundle.getString("simulador.paso4.btn.nueva");
                    // Aplicar numeración de grupo si corresponde
                    int numeroGrupo = TabManager.obtenerNumeroGrupo(tabPane, simuladorId);
                    if (numeroGrupo > 0 && numeroGrupo != -1) {
                        tab.setText(numeroGrupo + "-" + tituloBase);
                    } else {
                        tab.setText(tituloBase);
                    }
                    
                    // Actualizar contenido de la pestaña de funciones de error
                    // Buscar la instancia de NuevaFuncionError a través del paso 4
                    if (pasos.size() > 3 && pasos.get(3) instanceof PanelNuevaSimDescPaso4) {
                        PanelNuevaSimDescPaso4 paso4 = (PanelNuevaSimDescPaso4) pasos.get(3);
                        NuevaFuncionError nuevaFuncionError = paso4.getNuevaFuncionErrorInstance();
                        if (nuevaFuncionError != null) {
                            nuevaFuncionError.actualizarTextos(bundle);
                        }
                    }
                }
            }
        }
    }

    public Parent getRoot() {
        if (pestañaSimulacion == null) {
            // Si no hay pestaña de simulación, devolver el contenido del paso actual
            if (pasoActual >= 0 && pasoActual < pasos.size()) {
                return pasos.get(pasoActual).getRoot();
            }
            // Si no hay paso actual, devolver un contenedor vacío
            return new javafx.scene.layout.VBox();
        }
        return (Parent) pestañaSimulacion.getContent();
    }

    /**
     * Obtiene el paso actual del simulador.
     * @return El índice del paso actual (0-5).
     */
    public int getPasoActual() {
        return pasoActual;
    }

    /**
     * Configura las relaciones padre-hijo para cerrar pestañas hijas cuando se cierre el simulador.
     */
    public void configurarRelacionesPadreHijo() {
        if (tabPane != null) {
            // Añadir listener para cerrar pestañas hijas cuando se cierre la pestaña de simulador
            tabPane.getTabs().addListener((javafx.collections.ListChangeListener.Change<? extends Tab> change) -> {
                while (change.next()) {
                    if (change.wasRemoved()) {
                        for (Tab tab : change.getRemoved()) {
                            if (tab.getUserData() != null && 
                                tab.getUserData().toString().equals(simuladorId)) {
                                // Cerrar las pestañas hijas
                                javafx.application.Platform.runLater(() -> {
                                    TabManager.closeChildTabs(tabPane, simuladorId);
                                });
                            }
                        }
                    }
                }
            });
        }
    }
    
    /**
     * 🔹 Obtiene el ID único de este simulador.
     */
    public String getSimuladorId() {
        return simuladorId;
    }
    
    /**
     * *** NUEVO: Método estático para actualizar todos los simuladores activos ***
     */
    public static void actualizarTodosLosSimuladores(ResourceBundle bundle) {
        for (PanelSimuladorDesc simulador : simuladoresActivos.values()) {
            try {
                simulador.setBundle(bundle);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * *** NUEVO: Método para desregistrar un simulador cuando se cierra ***
     */
    public static void desregistrarSimulador(String simuladorId) {
        simuladoresActivos.remove(simuladorId);
    }
    
    /**
     * *** NUEVO: Obtener simulador activo por ID ***
     */
    public static PanelSimuladorDesc obtenerSimulador(String simuladorId) {
        return simuladoresActivos.get(simuladorId);
    }

    public bienvenida.MenuPrincipal getMenuPane() {
        return menuPane;
    }

    public void setTabPane(TabPane tabPane) {
        this.tabPane = tabPane;
        // Reconfigurar relaciones padre-hijo con el nuevo TabPane
        configurarRelacionesPadreHijo();
    }

    /**
     * Establece la pestaña de simulación asociada a este simulador.
     * @param tab La pestaña que contiene este simulador.
     */
    public void setPestañaSimulacion(Tab tab) {
        this.pestañaSimulacion = tab;
    }
}
