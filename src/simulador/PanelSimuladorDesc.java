package simulador;

import gramatica.Gramatica;
import gramatica.FuncionError;
import gramatica.TablaPredictiva;
import gramatica.TablaPredictivaPaso5;
import gramatica.Terminal;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * Controlador para la simulación descendente en JavaFX.
 */
public class PanelSimuladorDesc {

    @FXML
    public final TabPane tabPane;

    public Gramatica gramatica;
    private final Gramatica gramaticaOriginal;
    private Stage ventanaGramatica;
    private Tab pestañaSimulacion;
    private int pasoActual;
    private ArrayList<PanelNuevaSimDescPaso> pasos;
    private ResourceBundle bundle;
    
    // Referencia global a la tabla predictiva extendida (para pasos 5 y 6)
    private TablaPredictivaPaso5 tablaPredictivaExtendidaGlobal;

    public PanelSimuladorDesc(Gramatica gramatica, TabPane tabPane, ResourceBundle bundle) {
        this.gramatica = gramatica;
        this.gramaticaOriginal = gramatica;
        this.tabPane = tabPane;
        this.bundle = bundle;
        this.pasoActual = 0;
        
        // Inicializar funciones de error y tabla predictiva extendida
        inicializarTablaPredictivaYFuncionesError();
        
        // Inicializar pasos
        pasos = new ArrayList<>();
        try {
            pasos.add(new PanelNuevaSimDescPaso1(this));
            pasos.add(new PanelNuevaSimDescPaso2(this));
            pasos.add(new PanelNuevaSimDescPaso3(this));
            pasos.add(new PanelNuevaSimDescPaso4(this));
            pasos.add(new PanelNuevaSimDescPaso5(this));
            pasos.add(new PanelNuevaSimDescPaso6(this.gramatica, this));
            
            // Mostrar el primer paso
            mostrarPasoActual();
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
    
    private void mostrarPasoActual() {
        pestañaSimulacion = new Tab(bundle.getString("simulador.tab.paso1"));
        pestañaSimulacion.setClosable(false);
        pestañaSimulacion.setContent(pasos.get(pasoActual).getRoot());
        pestañaSimulacion.setUserData(this);  // Guardar la referencia al panel en userData

        tabPane.getTabs().add(pestañaSimulacion);
        tabPane.getSelectionModel().select(pestañaSimulacion);
    }

    /**
     * Ventana emergente para visualizar la gramática original.
     */
    public void mostrarGramaticaOriginal() {
        if (ventanaGramatica != null) {
            ventanaGramatica.toFront();
            return;
        }
        
        try {
            ventanaGramatica = new Stage();
            ventanaGramatica.initModality(Modality.NONE);
            ventanaGramatica.setTitle(bundle.getString("simulador.gramatica.original"));
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/VentanaGramaticaOriginal.fxml"));
            loader.setResources(bundle);
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/vistas/styles.css").toExternalForm());
            
            // Obtener los elementos del FXML
            ListView<String> listView = (ListView<String>) scene.lookup("#listViewProducciones");
            Button btnCerrar = (Button) scene.lookup(".button-cancel");
            
            // Configurar la lista de producciones
            listView.setItems(FXCollections.observableArrayList(gramaticaOriginal.getProduccionesModel()));
            
            // Configurar el botón de cerrar
            btnCerrar.setOnAction(e -> {
                ventanaGramatica.close();
                ventanaGramatica = null;
            });
            
            ventanaGramatica.setScene(scene);
            ventanaGramatica.setOnCloseRequest(e -> ventanaGramatica = null);
            ventanaGramatica.show();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cancelarSimulacion() {
        // Cerrar la pestaña de simulación si existe
        if (pestañaSimulacion != null) {
            tabPane.getTabs().remove(pestañaSimulacion);
            pestañaSimulacion = null;
        }
        // Seleccionar la primera pestaña
        tabPane.getSelectionModel().select(0);
    }

    public void cambiarPaso(int paso) {
        // Guardar explícitamente los datos de la tabla si venimos del paso 5
        if (this.pasoActual == 4 && pasos.get(4) instanceof PanelNuevaSimDescPaso5) {
            PanelNuevaSimDescPaso5 paso5 = (PanelNuevaSimDescPaso5) pasos.get(4);
            // Llamar al método público para guardar la tabla
            paso5.guardarDatosTabla();
        }
        
        this.pasoActual = paso;
        if (pestañaSimulacion == null) {
            pestañaSimulacion = new Tab(bundle.getString("simulador.tab.paso1"));
            pestañaSimulacion.setClosable(false);
            tabPane.getTabs().add(pestañaSimulacion);
        }
        if (paso == 5) {
            pestañaSimulacion.setText(bundle.getString("simulador.tab.paso6"));
        } else {
            pestañaSimulacion.setText(bundle.getString("simulador.tab.paso1").replace("1", String.valueOf(paso + 1)));
        }
        pestañaSimulacion.setContent(pasos.get(paso).getRoot());
        tabPane.getSelectionModel().select(pestañaSimulacion);

        // Refrescar la vista del paso al que vamos
        // Paso 5
        if (paso == 4 && pasos.get(4) instanceof PanelNuevaSimDescPaso5) {
            PanelNuevaSimDescPaso5 paso5View = (PanelNuevaSimDescPaso5) pasos.get(4);
            paso5View.refrescarVista();
        }
        // Paso 6
        else if (paso == 5 && pasos.get(5) instanceof PanelNuevaSimDescPaso6) {
            // Reconstruir el paso 6 con la tabla actualizada
            pasos.set(5, new PanelNuevaSimDescPaso6(this.gramatica, this));
            pestañaSimulacion.setContent(pasos.get(5).getRoot());
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
        
        // Update tab title and content if it exists
        if (pestañaSimulacion != null) {
            if (this.pasoActual == 5) {
                pestañaSimulacion.setText(bundle.getString("simulador.tab.paso6"));
            } else {
                pestañaSimulacion.setText(bundle.getString("simulador.tab.paso1").replace("1", String.valueOf(this.pasoActual + 1)));
            }
            // Refresh the content of the current step
            pestañaSimulacion.setContent(pasos.get(this.pasoActual).getRoot());
        }
    }
}
