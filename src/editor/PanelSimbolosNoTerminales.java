package editor;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;


public class PanelSimbolosNoTerminales extends VBox implements ActualizableTextos {

    @FXML private FlowPane symbolButtonsPane;
    @FXML private TextField txtSimboloNoTerminal;
    @FXML private ListView<String> listSimbolosNoTerminales;
    @FXML private Label labelHeader;
    @FXML private Label labelPredefinidos;
    @FXML private Label labelEditar;
    @FXML private Label labelLista;
    @FXML private Button btnInsertar;
    @FXML private Button btnModificar;
    @FXML private Button btnEliminar;
    @FXML private Button btnCancelar;
    @FXML private Button btnAceptar;

    private final ObservableList<String> simbolosNoTerminales;
    private final ObservableList<String> simbolosTemporales; // Lista temporal para cambios
    private final Set<String> simbolosSet;
    private final TabPane tabPane;
    private final PanelCreacionGramaticaPaso2 panelPadre;
    private ResourceBundle bundle;

    // Lista de símbolos predefinidos para No Terminales
    private final String[] simbolosPredefinidos = {"S", "A", "B", "C", "D", "X", "Y", "Z"};

    public PanelSimbolosNoTerminales(ObservableList<String> simbolosNoTerminales, TabPane tabPane, PanelCreacionGramaticaPaso2 panelPadre) {
        this(simbolosNoTerminales, tabPane, panelPadre, panelPadre.panelPadre.getBundle());
    }

    public PanelSimbolosNoTerminales(ObservableList<String> simbolosNoTerminales, TabPane tabPane, PanelCreacionGramaticaPaso2 panelPadre, ResourceBundle bundle) {
        this.simbolosNoTerminales = simbolosNoTerminales;
        this.simbolosTemporales = FXCollections.observableArrayList(simbolosNoTerminales); // Copia temporal
        this.simbolosSet = new HashSet<>(simbolosTemporales);
        this.tabPane = tabPane;
        this.panelPadre = panelPadre;
        this.bundle = bundle;
        cargarFXML();
    }

    private void cargarFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/PanelSimbolosNoTerminales.fxml"));
            loader.setController(this);
            Parent root = loader.load();
            this.getChildren().add(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void initialize() {
        listSimbolosNoTerminales.setItems(simbolosTemporales); // Usamos la lista temporal
        generarBotonesPredefinidos();
        actualizarTextos(bundle);
    }

    private void generarBotonesPredefinidos() {
        for (String simbolo : simbolosPredefinidos) {
            Button btnSimbolo = new Button(simbolo);
            btnSimbolo.setStyle("-fx-font-size: 14px; -fx-padding: 5px; -fx-background-color: #3498db; -fx-text-fill: white; -fx-border-radius: 5px;");
            btnSimbolo.setOnAction(event -> agregarSimbolo(simbolo));
            symbolButtonsPane.getChildren().add(btnSimbolo);
        }
    }

    private void agregarSimbolo(String simbolo) {
        if (simbolosSet.add(simbolo)) {
            simbolosTemporales.add(simbolo); // Agregamos solo a la lista temporal
        } else {
            panelPadre.panelPadre.mostrarAlerta(bundle.getString("simbolos.error.duplicado.titulo"), bundle.getString("simbolos.error.duplicado.mensaje"));
        }
    }

    @FXML
    private void onInsertarAction() {
        String nuevoSimbolo = txtSimboloNoTerminal.getText().trim();
        if (!nuevoSimbolo.isEmpty() && simbolosSet.add(nuevoSimbolo)) {
            simbolosTemporales.add(nuevoSimbolo); // Agregamos solo a la lista temporal
            txtSimboloNoTerminal.clear();
        } else {
            panelPadre.panelPadre.mostrarAlerta(bundle.getString("simbolos.error.insertar.titulo"), bundle.getString("simbolos.error.insertar.mensaje"));
        }
    }

    @FXML
    private void onEliminarAction() {
        String seleccionado = listSimbolosNoTerminales.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            simbolosSet.remove(seleccionado);
            simbolosTemporales.remove(seleccionado); // Eliminamos solo de la lista temporal
        }
    }

    @FXML
    private void onAceptarAction() {
        ButtonType btnSi = new ButtonType(bundle.getString("button.si"), ButtonBar.ButtonData.YES);
        ButtonType btnNo = new ButtonType(bundle.getString("button.no"), ButtonBar.ButtonData.NO);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, bundle.getString("simbolos.dialog.guardar.mensaje"), btnSi, btnNo);
        confirm.setTitle(bundle.getString("simbolos.dialog.guardar.titulo"));
        confirm.setHeaderText(bundle.getString("simbolos.dialog.guardar.titulo"));
        confirm.showAndWait().ifPresent(response -> {
            if (response == btnSi) {
                simbolosNoTerminales.setAll(simbolosTemporales); // Guardamos cambios en la lista original
                panelPadre.panelPadre.getGramatica().setNoTerminalesModel(simbolosNoTerminales);
                cerrarPestanaActual();
            }
        });
    }

    @FXML
    private void onCancelarAction() {
        ButtonType btnSi = new ButtonType(bundle.getString("button.si"), ButtonBar.ButtonData.YES);
        ButtonType btnNo = new ButtonType(bundle.getString("button.no"), ButtonBar.ButtonData.NO);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, bundle.getString("simbolos.dialog.cancelar.mensaje"), btnSi, btnNo);
        confirm.setTitle(bundle.getString("simbolos.dialog.cancelar.titulo"));
        confirm.setHeaderText(bundle.getString("simbolos.dialog.cancelar.titulo"));
        confirm.showAndWait().ifPresent(response -> {
            if (response == btnSi) {
                cerrarPestanaActual();
            }
        });
    }

    private String mostrarDialogoModificarSimbolo(String simboloActual) {
        TextInputDialog dialog = new TextInputDialog(simboloActual);
        dialog.setTitle(bundle.getString("simbolos.dialog.modificar.titulo"));
        dialog.setHeaderText(bundle.getString("simbolos.dialog.modificar.header"));
        dialog.setContentText(bundle.getString("simbolos.dialog.modificar.content"));
        return dialog.showAndWait().orElse(null);
    }

    // Método para modificar un símbolo no terminal existente
    @FXML
    private void onModificarAction() {
        String simboloSeleccionado = listSimbolosNoTerminales.getSelectionModel().getSelectedItem();
        if (simboloSeleccionado != null) {
            String nuevoSimbolo = mostrarDialogoModificarSimbolo(simboloSeleccionado);
            if (nuevoSimbolo != null) nuevoSimbolo = nuevoSimbolo.trim();
            if (nuevoSimbolo != null && !nuevoSimbolo.isEmpty() && !simbolosSet.contains(nuevoSimbolo)) {
                simbolosSet.remove(simboloSeleccionado);
                simbolosSet.add(nuevoSimbolo);
                simbolosTemporales.set(simbolosTemporales.indexOf(simboloSeleccionado), nuevoSimbolo);
                panelPadre.panelPadre.getGramatica().modificarSimboloProduccion(simboloSeleccionado, nuevoSimbolo);
            } else {
                panelPadre.panelPadre.mostrarAlerta(bundle.getString("simbolos.error.modificar.titulo"), bundle.getString("simbolos.error.modificar.mensaje"));
            }
        } else {
            panelPadre.panelPadre.mostrarAlerta(bundle.getString("simbolos.error.seleccion.titulo"), bundle.getString("simbolos.error.seleccion.mensaje"));
        }
    }

    public void cerrarPestanaActual() {
        if (tabPane != null) {
            Tab tabActual = null;
            for (Tab tab : tabPane.getTabs()) {
                if (tab.getContent() == this) {  // Verifica si el contenido de la pestaña es el panel actual
                    tabActual = tab;
                    break;
                }
            }
            if (tabActual != null) {
                tabPane.getTabs().remove(tabActual); // Cierra la pestaña actual
            } else {
                System.err.println("No se encontró la pestaña actual.");
            }
        } else {
            System.err.println("TabPane es null, no se puede cerrar la pestaña.");
        }
    }

    public void actualizarTextos(ResourceBundle bundle) {
        this.bundle = bundle;
        if (labelHeader != null) labelHeader.setText(bundle.getString("simbolos.no.terminales.header"));
        if (labelPredefinidos != null) labelPredefinidos.setText(bundle.getString("simbolos.predefinidos"));
        if (labelEditar != null) labelEditar.setText(bundle.getString("simbolos.editar"));
        if (labelLista != null) labelLista.setText(bundle.getString("simbolos.lista"));
        if (btnInsertar != null) btnInsertar.setText(bundle.getString("simbolos.btn.insertar"));
        if (btnModificar != null) btnModificar.setText(bundle.getString("simbolos.btn.modificar"));
        if (btnEliminar != null) btnEliminar.setText(bundle.getString("simbolos.btn.eliminar"));
        if (btnCancelar != null) btnCancelar.setText(bundle.getString("button.cancelar"));
        if (btnAceptar != null) btnAceptar.setText(bundle.getString("button.aceptar"));
        if (txtSimboloNoTerminal != null) txtSimboloNoTerminal.setPromptText(bundle.getString("simbolos.prompt.simbolo"));
    }

}
