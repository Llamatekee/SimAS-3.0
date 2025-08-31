package editor;

import bienvenida.MenuPrincipal;
import gramatica.Gramatica;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.beans.binding.BooleanBinding;

import java.io.IOException;
import utils.TabManager;
import utils.ActualizableTextos;

public class PanelCreacionGramaticaPaso2 extends VBox implements ActualizableTextos {

    public final PanelCreacionGramatica panelPadre;
    public final MenuPrincipal menuPane;
    public final TabPane tabPane;

    @FXML private ListView<String> listNoTerminales;
    @FXML private ListView<String> listTerminales;
    @FXML private Button btnModificarNoTerminales;
    @FXML private Button btnModificarTerminales;
    @FXML private Button btnCancelar;
    @FXML private Button btnPrimero;
    @FXML private Button btnAnterior;
    @FXML private Button btnSiguiente;
    @FXML private Button btnUltimo;
    @FXML private Label labelHeader;
    @FXML private Label labelNoTerminalesSeccion;
    @FXML private Label labelTerminalesSeccion;
    @FXML private Label labelErrorSimbolos;

    private final ObservableList<String> simbolosNoTerminales = FXCollections.observableArrayList();
    private final ObservableList<String> simbolosTerminales = FXCollections.observableArrayList();

    public PanelCreacionGramaticaPaso2(PanelCreacionGramatica panelPadre, MenuPrincipal menuPane, TabPane tabPane) {
        this.panelPadre = panelPadre;
        this.menuPane = menuPane;
        this.tabPane = tabPane;
        cargarFXML();
    }

    private void cargarFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/PanelCreacionGramaticaPaso2.fxml"));
            loader.setController(this);
            Parent root = loader.load();
            this.getChildren().setAll(root); // 🔹 Sustituye todo el contenido actual por el del FXML sin tocar el TabPane
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void initialize() {
        listNoTerminales.setItems(simbolosNoTerminales);
        listTerminales.setItems(simbolosTerminales);

        // Configurar visualización de los elementos correctamente
        listNoTerminales.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? "" : item);
            }
        });

        listTerminales.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? "" : item);
            }
        });

        cargarDatosDesdeGramatica();
        
        // Configurar validación de símbolos
        configurarValidacionSimbolos();
        
        // Aplicar estilos responsivos basados en el tamaño de la pantalla
        aplicarEstilosResponsivos();
    }

    /**
     * Aplica estilos responsivos basados en el tamaño de la pantalla
     */
    private void aplicarEstilosResponsivos() {
        // Obtener el tamaño de la pantalla principal
        Screen primaryScreen = Screen.getPrimary();
        double screenWidth = primaryScreen.getVisualBounds().getWidth();
        double screenHeight = primaryScreen.getVisualBounds().getHeight();
        
        // Determinar si es una pantalla pequeña
        boolean isSmallScreen = screenWidth < 1200 || screenHeight < 800;
        
        if (isSmallScreen) {
            aplicarEstilosCompactos();
        }
    }
    
    /**
     * Aplica estilos compactos para pantallas pequeñas
     */
    private void aplicarEstilosCompactos() {
        // Aplicar estilos compactos a los elementos principales
        if (labelHeader != null) {
            labelHeader.getStyleClass().add("wizard-header-compact");
        }
        
        if (labelNoTerminalesSeccion != null) {
            labelNoTerminalesSeccion.getStyleClass().add("wizard-section-header-compact");
        }
        
        if (labelTerminalesSeccion != null) {
            labelTerminalesSeccion.getStyleClass().add("wizard-section-header-compact");
        }
        
        // Aplicar estilos compactos a las listas
        if (listNoTerminales != null) {
            listNoTerminales.getStyleClass().add("list-view-compact");
            // Ajustar tamaños mínimos para pantallas pequeñas
            listNoTerminales.setMinHeight(60);
            listNoTerminales.setPrefHeight(80);
            listNoTerminales.setMaxHeight(120);
        }
        
        if (listTerminales != null) {
            listTerminales.getStyleClass().add("list-view-compact");
            // Ajustar tamaños mínimos para pantallas pequeñas
            listTerminales.setMinHeight(60);
            listTerminales.setPrefHeight(80);
            listTerminales.setMaxHeight(120);
        }
        
        // Aplicar estilos compactos a los botones
        if (btnModificarNoTerminales != null) {
            btnModificarNoTerminales.getStyleClass().add("wizard-action-button-main");
        }
        
        if (btnModificarTerminales != null) {
            btnModificarTerminales.getStyleClass().add("wizard-action-button-main");
        }
        
        if (btnCancelar != null) {
            btnCancelar.getStyleClass().add("button-compact");
        }
        
        if (btnPrimero != null) {
            btnPrimero.getStyleClass().add("button-next");
        }
        
        if (btnAnterior != null) {
            btnAnterior.getStyleClass().add("button-next");
        }
        
        if (btnSiguiente != null) {
            btnSiguiente.getStyleClass().add("button-next");
        }
        
        if (btnUltimo != null) {
            btnUltimo.getStyleClass().add("button-next");
        }
    }

    /**
     * 🔹 Carga los datos desde la gramática activa.
     */
    private void cargarDatosDesdeGramatica() {
        Gramatica gramatica = panelPadre.getGramatica();
        if (gramatica != null) {
            simbolosNoTerminales.setAll(gramatica.getNoTerminalesModel());
            simbolosTerminales.setAll(gramatica.getTerminalesModel());
        }
    }

    /**
     * Configura la validación de símbolos para habilitar/deshabilitar botones y mostrar mensajes de error.
     */
    private void configurarValidacionSimbolos() {
        // Crear binding para verificar si ambos conjuntos tienen al menos un elemento
        BooleanBinding simbolosValidos = new BooleanBinding() {
            {
                bind(simbolosNoTerminales, simbolosTerminales);
            }
            
            @Override
            protected boolean computeValue() {
                return !simbolosNoTerminales.isEmpty() && !simbolosTerminales.isEmpty();
            }
        };
        
        // Deshabilitar botones siguiente y último si no hay símbolos válidos
        btnSiguiente.disableProperty().bind(simbolosValidos.not());
        btnUltimo.disableProperty().bind(simbolosValidos.not());
        
        // Mostrar/ocultar mensaje de error
        labelErrorSimbolos.visibleProperty().bind(simbolosValidos.not());
        labelErrorSimbolos.managedProperty().bind(simbolosValidos.not());
        
        // Configurar el texto del mensaje de error
        actualizarMensajeError();
    }
    
    /**
     * Actualiza el mensaje de error con el texto del bundle actual.
     */
    private void actualizarMensajeError() {
        if (labelErrorSimbolos != null && panelPadre.getBundle() != null) {
            try {
                labelErrorSimbolos.setText(panelPadre.getBundle().getString("creacion2.error.simbolos.vacios"));
            } catch (Exception e) {
                // Si no se encuentra la clave, usar un mensaje por defecto
                labelErrorSimbolos.setText("Se necesita al menos un símbolo en cada conjunto para poder avanzar");
            }
        }
    }

    @FXML
    private void onBtnSiguienteAction() {
        panelPadre.cambiarPaso(3);
    }

    @FXML
    private void onBtnAnteriorAction() {
        panelPadre.cambiarPaso(1);
    }

    @FXML
    private void onBtnCancelarAction() {
        panelPadre.cancelarEdicion();
    }

    @FXML
    private void onBtnModificarNoTerminalesAction() {
        java.util.ResourceBundle bundle = panelPadre.getBundle();
        String childId = "no_terminales_" + panelPadre.getCreacionId();
        
        // Verificar si ya existe una pestaña para esta creación específica
        Tab existingTab = findTabByChildId(childId);
        if (existingTab != null) {
            tabPane.getSelectionModel().select(existingTab);
            return;
        }
        
        // Crear una nueva pestaña de no terminales como hija de la creación
        PanelSimbolosNoTerminales panel = new PanelSimbolosNoTerminales(simbolosNoTerminales, tabPane, this);
        TabManager.getOrCreateTab(tabPane, PanelSimbolosNoTerminales.class, 
            bundle.getString("creacion2.tab.modificar.no.terminales"), panel, panelPadre.getCreacionId(), childId);
    }

    @FXML
    private void onBtnModificarTerminalesAction() {
        java.util.ResourceBundle bundle = panelPadre.getBundle();
        String childId = "terminales_" + panelPadre.getCreacionId();
        
        // Verificar si ya existe una pestaña para esta creación específica
        Tab existingTab = findTabByChildId(childId);
        if (existingTab != null) {
            tabPane.getSelectionModel().select(existingTab);
            return;
        }
        
        // Crear una nueva pestaña de terminales como hija de la creación
        PanelSimbolosTerminales panel = new PanelSimbolosTerminales(simbolosTerminales, tabPane, this);
        TabManager.getOrCreateTab(tabPane, PanelSimbolosTerminales.class, 
            bundle.getString("creacion2.tab.modificar.terminales"), panel, panelPadre.getCreacionId(), childId);
    }
    
    /**
     * Busca una pestaña por su childId específico.
     */
    private Tab findTabByChildId(String childId) {
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() != null && tab.getUserData().toString().equals(childId)) {
                return tab;
            }
        }
        return null;
    }

    @FXML
    private void onBtnUltimoAction() {
        // Determinar el paso más lejano posible
        int pasoDestino = determinarPasoUltimo();
        panelPadre.cambiarPaso(pasoDestino);
    }
    
    /**
     * Determina el paso más lejano al que se puede ir desde el paso 2.
     * Verifica si los pasos siguientes tienen datos válidos.
     */
    private int determinarPasoUltimo() {
        // Verificar paso 3 (producciones)
        if (panelPadre.getGramatica().getProducciones().isEmpty()) {
            return 3; // Solo puede ir al paso 3
        }
        
        // Verificar paso 4 (símbolo inicial)
        if (panelPadre.getGramatica().getSimbInicial() == null || 
            panelPadre.getGramatica().getSimbInicial().isEmpty()) {
            return 4; // Puede ir al paso 4
        }
        
        return 4; // Puede ir al paso final
    }

    @FXML
    private void onBtnPrimeroAction() {
        panelPadre.cambiarPaso(1);
    }

    public void asignarListaSimbolosNoTerminales(ObservableList<String> lista) {
        simbolosNoTerminales.setAll(lista);
    }

    public void asignarListaSimbolosTerminales(ObservableList<String> lista) {
        simbolosTerminales.setAll(lista);
    }

    public void actualizarTextos(java.util.ResourceBundle bundle) {
        if (labelHeader != null) labelHeader.setText(bundle.getString("creacion2.header"));
        if (labelNoTerminalesSeccion != null) labelNoTerminalesSeccion.setText(bundle.getString("creacion2.label.no.terminales.seccion"));
        if (labelTerminalesSeccion != null) labelTerminalesSeccion.setText(bundle.getString("creacion2.label.terminales.seccion"));
        if (btnModificarNoTerminales != null) btnModificarNoTerminales.setText(bundle.getString("creacion2.btn.modificar.no.terminales"));
        if (btnModificarTerminales != null) btnModificarTerminales.setText(bundle.getString("creacion2.btn.modificar.terminales"));
        if (btnCancelar != null) btnCancelar.setText(bundle.getString("button.cancelar"));
        if (btnPrimero != null) btnPrimero.setText(bundle.getString("button.primero"));
        if (btnAnterior != null) btnAnterior.setText(bundle.getString("button.anterior"));
        if (btnSiguiente != null) btnSiguiente.setText(bundle.getString("button.siguiente"));
        if (btnUltimo != null) btnUltimo.setText(bundle.getString("button.ultimo"));
        
        // Actualizar el mensaje de error
        actualizarMensajeError();
    }
}
