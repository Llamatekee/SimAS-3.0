package editor;

import bienvenida.MenuPrincipal;
import gramatica.*;
import gramatica.NoTerminal;
import gramatica.Terminal;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import java.util.ResourceBundle;

public class PanelCreacionGramatica extends BorderPane implements ActualizableTextos {

    private final TabPane tabPane;
    private final PanelCreacionGramaticaPaso1 paso1;
    private final PanelCreacionGramaticaPaso2 paso2;
    private final PanelCreacionGramaticaPaso3 paso3;
    private final PanelCreacionGramaticaPaso4 paso4;
    private final Editor panelPadre;
    private final MenuPrincipal menuPane;
    private Gramatica gramaticaTemporal;
    private ResourceBundle bundle;
    
    // Sistema de identificación para relaciones padre-hijo
    private String creacionId;
    private static int contadorCreaciones = 0;

    public PanelCreacionGramatica(Editor ventanaPadre, TabPane tabPane, Gramatica gr, MenuPrincipal menuPane) {
        this(ventanaPadre, tabPane, gr, menuPane, null);
    }
    
    public PanelCreacionGramatica(Editor ventanaPadre, TabPane tabPane, Gramatica gr, MenuPrincipal menuPane, String creacionId) {
        this.tabPane = tabPane;
        this.panelPadre = ventanaPadre;
        this.menuPane = menuPane;
        this.gramaticaTemporal = (gr != null) ? new Gramatica(gr) : new Gramatica();
        this.bundle = panelPadre.getBundle();
        
        // Usar el creacionId proporcionado o generar uno nuevo
        if (creacionId != null) {
            this.creacionId = creacionId;
        } else {
            this.creacionId = "creacion_" + System.currentTimeMillis() + "_" + (++contadorCreaciones);
        }

        // Inicializar paneles del asistente
        this.paso1 = new PanelCreacionGramaticaPaso1(this, this.bundle);
        this.paso2 = new PanelCreacionGramaticaPaso2(this, menuPane, tabPane);
        this.paso3 = new PanelCreacionGramaticaPaso3(this, tabPane, menuPane);
        this.paso4 = new PanelCreacionGramaticaPaso4(this, tabPane);

        // Rellenar datos
        this.paso1.setNombre(gramaticaTemporal.getNombre());
        this.paso1.setDescripcion(gramaticaTemporal.getDescripcion());

        // Convertir ObservableList<Terminal> a ObservableList<String>
        ObservableList<String> terminales = FXCollections.observableArrayList();
        for (Terminal t : gramaticaTemporal.getTerminales()) {
            terminales.add(t.toString());
        }
        this.paso2.asignarListaSimbolosTerminales(terminales);

        // Convertir ObservableList<NoTerminal> a ObservableList<String>
        ObservableList<String> noTerminales = FXCollections.observableArrayList();
        for (NoTerminal nt : gramaticaTemporal.getNoTerminales()) {
            noTerminales.add(nt.toString());
        }
        this.paso2.asignarListaSimbolosNoTerminales(noTerminales);

        this.paso3.asignarProducciones(gramaticaTemporal.getProducciones());
        //this.paso4.setSimbInicial(gr.getSimbInicial());

        // Mostrar el Paso 1 en el centro del asistente
        this.setCenter(this.paso1);
        
        // Configurar relaciones padre-hijo
        configurarRelacionesPadreHijo();
    }
    
    /**
     * 🔹 Configura las relaciones padre-hijo para cerrar pestañas hijas cuando se cierre la creación.
     */
    private void configurarRelacionesPadreHijo() {
        if (tabPane != null) {
            // Añadir listener para cerrar pestañas hijas cuando se cierre la pestaña de creación
            tabPane.getTabs().addListener((javafx.collections.ListChangeListener.Change<? extends Tab> change) -> {
                while (change.next()) {
                    if (change.wasRemoved()) {
                        for (Tab tab : change.getRemoved()) {
                            if (tab.getContent() == this && tab.getUserData() != null && 
                                tab.getUserData().toString().contains(creacionId)) {
                                // Cerrar las pestañas hijas
                                javafx.application.Platform.runLater(() -> {
                                    TabManager.closeChildTabs(tabPane, creacionId);
                                });
                            }
                        }
                    }
                }
            });
        }
    }
    
    /**
     * 🔹 Obtiene el ID único de esta creación.
     */
    public String getCreacionId() {
        return creacionId;
    }

    public Gramatica getGramatica() {
        return gramaticaTemporal;
    }

    public void setGramatica(Gramatica gramatica) {
        this.gramaticaTemporal = gramatica;
    }

    public void cambiarPaso(int paso) {
        // Cerrar pestañas específicas del paso anterior
        cerrarPestañasEspecificasPaso();
        
        switch (paso) {
            case 1:
                this.setCenter(this.paso1);
                break;
            case 2:
                this.setCenter(this.paso2);
                break;
            case 3:
                this.setCenter(this.paso3);
                break;
            case 4:
                this.setCenter(this.paso4);
                break;
        }
        
        // Actualizar el título de la pestaña
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getContent() == this) {
                // Usar la nueva clave consistente para asistente de editor
                tab.setText(bundle.getString("editor.asistente"));
            }
        }
        
        actualizarTextos(bundle);
    }
    
    /**
     * 🔹 Cierra las pestañas específicas del paso actual antes de cambiar de paso.
     */
    private void cerrarPestañasEspecificasPaso() {
        if (tabPane == null) return;
        
        // Determinar qué paso estamos dejando
        int pasoActual = 1;
        if (getCenter() == paso2) pasoActual = 2;
        else if (getCenter() == paso3) pasoActual = 3;
        else if (getCenter() == paso4) pasoActual = 4;
        
        // Cerrar pestañas específicas según el paso que se está dejando
        java.util.List<Tab> tabsToRemove = new java.util.ArrayList<>();
        
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() != null) {
                String userData = tab.getUserData().toString();
                
                // Si estamos saliendo del paso 2, cerrar pestañas de símbolos
                if (pasoActual == 2 && 
                    (userData.startsWith("no_terminales_" + creacionId) || 
                     userData.startsWith("terminales_" + creacionId))) {
                    tabsToRemove.add(tab);
                }
                
                // Si estamos saliendo del paso 3, cerrar pestañas de producciones
                if (pasoActual == 3 && userData.startsWith("producciones_" + creacionId)) {
                    tabsToRemove.add(tab);
                }
            }
        }
        
        // Cerrar las pestañas encontradas
        for (Tab tab : tabsToRemove) {
            tabPane.getTabs().remove(tab);
        }
    }

    public MenuPrincipal getMenuPane() {
        return menuPane;
    }

    public Editor getPanelPadre() {
        return panelPadre;
    }

    public void cancelarEdicion() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(bundle.getString("editor.header.salir"));
        confirm.setHeaderText(bundle.getString("editor.header.salir"));
        confirm.setContentText(bundle.getString("creacion.dialog.salir.mensaje"));
        ButtonType btnSi = new ButtonType(bundle.getString("button.si"), ButtonBar.ButtonData.YES);
        ButtonType btnNo = new ButtonType(bundle.getString("button.no"), ButtonBar.ButtonData.NO);
        confirm.getButtonTypes().setAll(btnNo, btnSi);
        Stage stage = (Stage) confirm.getDialogPane().getScene().getWindow();
        stage.toFront(); // Asegura que la alerta esté al frente
        confirm.showAndWait().ifPresent(result -> {
            if (result == btnSi) {
                // Cerrar todas las pestañas hijas antes de cerrar la principal
                TabManager.closeChildTabs(tabPane, creacionId);
                
                // Encontrar la pestaña actual
                Tab currentTab = null;
                for (Tab tab : tabPane.getTabs()) {
                    if (tab.getContent() == this) {
                        currentTab = tab;
                        break;
                    }
                }

                if (currentTab != null) {
                    // Obtener el índice de la pestaña actual
                    int currentIndex = tabPane.getTabs().indexOf(currentTab);
                    
                    // Eliminar la pestaña actual
                    tabPane.getTabs().remove(currentTab);
                    
                    // Seleccionar la pestaña adyacente o el menú principal
                    if (!tabPane.getTabs().isEmpty()) {
                        // Si hay pestañas, seleccionar la anterior o la siguiente
                        int newIndex = Math.min(currentIndex, tabPane.getTabs().size() - 1);
                        tabPane.getSelectionModel().select(newIndex);
                    }
                }
            }
        });
    }

    public void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public void actualizarTextos(ResourceBundle bundle) {
        this.bundle = bundle;
        if (paso1 != null) paso1.actualizarTextos(bundle);
        if (paso2 != null) paso2.actualizarTextos(bundle);
        if (paso3 != null) paso3.actualizarTextos(bundle);
        if (paso4 != null) paso4.actualizarTextos(bundle);

        // Actualizar el título de la pestaña
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getContent() == this) {
                // Usar la nueva clave consistente para asistente de editor
                tab.setText(bundle.getString("editor.asistente"));
            }
        }
    }

    public ResourceBundle getBundle() {
        return this.bundle;
    }
}
