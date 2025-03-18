package editor;

import bienvenida.MenuPrincipal;
import gramatica.*;
import gramatica.NoTerminal;
import gramatica.Terminal;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class PanelCreacionGramatica extends BorderPane {

    private final TabPane tabPane;
    private final PanelCreacionGramaticaPaso1 paso1;
    private final PanelCreacionGramaticaPaso2 paso2;
    private final PanelCreacionGramaticaPaso3 paso3;
    private final PanelCreacionGramaticaPaso4 paso4;
    private final Editor panelPadre;
    private final MenuPrincipal menuPane;
    private Gramatica gramaticaTemporal;

    public PanelCreacionGramatica(Editor ventanaPadre, TabPane tabPane, Gramatica gr, MenuPrincipal menuPane) {
        this.tabPane = tabPane;
        this.panelPadre = ventanaPadre;
        this.menuPane = menuPane;
        this.gramaticaTemporal = (gr != null) ? new Gramatica(gr) : new Gramatica();

        // Inicializar paneles del asistente
        this.paso1 = new PanelCreacionGramaticaPaso1(this);
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

        // Agregar pestaña del Paso 1
        Tab tabPaso1 = new Tab("Edición: Paso 1", this.paso1);
        tabPane.getTabs().add(tabPaso1);
        tabPane.getSelectionModel().select(tabPaso1);
    }

    public Gramatica getGramatica() {
        return gramaticaTemporal;
    }

    public void setGramatica(Gramatica gramatica) {
        this.gramaticaTemporal = gramatica;
    }

    public void cambiarPaso(int paso) {

        tabPane.getTabs().removeIf(tab -> tab.getText().startsWith("Edición: Paso"));
        switch (paso) {
            case 1:
                tabPane.getTabs().add(new Tab("Edición: Paso 1", this.paso1));
                break;
            case 2:
                tabPane.getTabs().add(new Tab("Edición: Paso 2", this.paso2));
                break;
            case 3:
                tabPane.getTabs().add(new Tab("Edición: Paso 3", this.paso3));
                break;
            case 4:
                tabPane.getTabs().add(new Tab("Edición: Paso 4", this.paso4));
                break;
        }

        tabPane.getSelectionModel().selectLast();
    }



    public MenuPrincipal getMenuPane() {
        return menuPane;
    }

    public Editor getPanelPadre() {
        return panelPadre;
    }

    public void cancelarEdicion() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Desea salir de la edición de la gramática?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Salir");
        Stage stage = (Stage) confirm.getDialogPane().getScene().getWindow();
        stage.toFront(); // Asegura que la alerta esté al frente
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.YES) {
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
}
