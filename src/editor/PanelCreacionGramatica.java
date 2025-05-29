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

    public PanelCreacionGramatica(Editor ventanaPadre, TabPane tabPane, Gramatica gr, MenuPrincipal menuPane) {
        this.tabPane = tabPane;
        this.panelPadre = ventanaPadre;
        this.menuPane = menuPane;
        this.gramaticaTemporal = (gr != null) ? new Gramatica(gr) : new Gramatica();
        this.bundle = panelPadre.getBundle();

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

        // Agregar pestaña del asistente (el contenido es this)
        Tab tabAsistente = new Tab(bundle.getString("creacion.tab.paso1"), this);
        tabPane.getTabs().add(tabAsistente);
        tabPane.getSelectionModel().select(tabAsistente);
    }

    public Gramatica getGramatica() {
        return gramaticaTemporal;
    }

    public void setGramatica(Gramatica gramatica) {
        this.gramaticaTemporal = gramatica;
    }

    public void cambiarPaso(int paso) {
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
                tab.setText(bundle.getString("creacion.tab.paso" + paso));
            }
        }
        
        actualizarTextos(bundle);
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
                // Obtener la gramática actual desde el editor original
                Gramatica gramatica = getPanelPadre().getGramatica();

                // Eliminar la pestaña del asistente
                tabPane.getTabs().removeIf(tab -> tab.getText().startsWith("Edición: Paso"));

                // Buscar la pestaña del editor existente
                for (Tab tab : tabPane.getTabs()) {
                    if (tab.getText().equals("Editor")) {
                        tabPane.getSelectionModel().select(tab);
                        return;  // Salimos del método sin crear otro Editor
                    }
                }

                // Si no se encuentra el editor, lo creamos (caso raro)
                Editor nuevoEditor = new Editor(tabPane, gramatica, menuPane);
                Tab editorTab = new Tab("Editor", nuevoEditor.getRoot());
                editorTab.setClosable(true);
                tabPane.getTabs().add(editorTab);
                tabPane.getSelectionModel().select(editorTab);
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
                int pasoActual = 1;
                if (getCenter() == paso2) pasoActual = 2;
                else if (getCenter() == paso3) pasoActual = 3;
                else if (getCenter() == paso4) pasoActual = 4;
                
                tab.setText(bundle.getString("creacion.tab.paso" + pasoActual));
            }
        }
    }

    public ResourceBundle getBundle() {
        return this.bundle;
    }
}
