package editor;

import gramatica.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.ResourceBundle;

/**
 * Panel para modificar las producciones de la gramática.
 */
public class PanelProducciones extends VBox implements ActualizableTextos {

    @FXML private ComboBox<NoTerminal> comboBoxAntecedente;
    @FXML private TextField txtConsecuente;
    @FXML private ListView<Produccion> listProducciones;
    @FXML private ListView<Simbolo> listTerminales;
    @FXML private ListView<Simbolo> listNoTerminales;
    @FXML private Button btnInsertar;
    @FXML private Button btnModificar;
    @FXML private Button btnEliminar;
    @FXML private Button btnBorrar;
    @FXML private Button btnEpsilon;
    @FXML private Button btnCancelar;
    @FXML private Button btnAceptar;
    @FXML private Label labelHeader;
    @FXML private Label labelAntecedente;
    @FXML private Label labelConsecuente;
    @FXML private Label labelAnadirSimbolos;
    @FXML private Label labelNoTerminales;
    @FXML private Label labelTerminales;
    @FXML private Label labelEpsilon;
    @FXML private Label labelLista;

    private final PanelCreacionGramaticaPaso3 panelPadre;
    private final TabPane tabPane;
    private final ObservableList<Produccion> producciones;
    private final ObservableList<NoTerminal> noTerminales;
    private final ObservableList<Terminal> terminales;
    private Produccion produccionSeleccionada = null;
    private ResourceBundle bundle;

    public PanelProducciones(PanelCreacionGramaticaPaso3 panelPadre, ObservableList<Produccion> producciones, TabPane tabPane) {
        this.panelPadre = panelPadre;
        this.tabPane = tabPane;
        this.producciones = FXCollections.observableArrayList(producciones);
        this.noTerminales = panelPadre.panelPadre.getGramatica().getNoTerminales();
        this.terminales = panelPadre.panelPadre.getGramatica().getTerminales();
        this.bundle = panelPadre.panelPadre.getBundle();
        cargarFXML();
    }

    private void cargarFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/PanelProducciones.fxml"));
            loader.setController(this);
            Parent root = loader.load();
            this.getChildren().add(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void initialize() {
        listProducciones.setItems(producciones);
        listNoTerminales.setItems(FXCollections.observableArrayList(noTerminales));
        listTerminales.setItems(FXCollections.observableArrayList(terminales));
        comboBoxAntecedente.setItems(noTerminales);
        
        // Agregar listener para debuggear la selección del ComboBox
        comboBoxAntecedente.setOnAction(event -> {
            NoTerminal selected = comboBoxAntecedente.getValue();
            if (selected != null) {
                System.out.println("ComboBox seleccionado: " + selected.getNombre());
            }
        });
        
        // Agregar listener para el valor del ComboBox
        comboBoxAntecedente.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                System.out.println("Valor del ComboBox cambiado a: " + newVal.getNombre());
            }
        });
        
        // Deshabilitar la edición manual del campo consecuente
        txtConsecuente.setEditable(false);
        txtConsecuente.setFocusTraversable(false);
        
        actualizarTextos(bundle);
        // Configurar visualización de la lista de producciones
        listProducciones.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Produccion item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(item.toString());
                }
            }
        });

        // Configurar visualización de terminales y no terminales
        listNoTerminales.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Simbolo item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getNombre());
            }
        });

        listTerminales.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Simbolo item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getNombre());
            }
        });

        // Manejo de selección en lista de producciones
        listProducciones.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                cargarProduccionParaModificar(newSelection);
            }
        });

        // Manejo de doble clic en listas de símbolos
        listNoTerminales.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                agregarSimboloAlConsecuente(listNoTerminales.getSelectionModel().getSelectedItem());
            }
        });

        listTerminales.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                agregarSimboloAlConsecuente(listTerminales.getSelectionModel().getSelectedItem());
            }
        });
    }

    private void cargarProduccionParaModificar(Produccion produccion) {
        produccionSeleccionada = produccion;
        comboBoxAntecedente.setValue(produccion.getAntec().getSimboloNT());
        txtConsecuente.setText(produccion.getConsec().stream().map(Simbolo::getNombre).collect(Collectors.joining(" ")));
        btnInsertar.setText("Guardar Cambios");
    }

    private void agregarSimboloAlConsecuente(Simbolo simbolo) {
        if (simbolo != null) {
            if (!txtConsecuente.getText().isEmpty()) {
                txtConsecuente.setText(txtConsecuente.getText() + " " + simbolo.getNombre());
            } else {
                txtConsecuente.setText(simbolo.getNombre());
            }
        }
    }

    @FXML
    private void onBtnInsertarAction() {
        if (comboBoxAntecedente.getValue() == null || txtConsecuente.getText().trim().isEmpty()) {
            mostrarAlerta(bundle.getString("producciones.error.antecedente.consecuente"));
            return;
        }

        // Convertir el texto del consecuente en una lista de símbolos
        ObservableList<Simbolo> consecuente = FXCollections.observableArrayList();
        for (String simbolo : txtConsecuente.getText().split(" ")) {
            if (!simbolo.isEmpty()) {
                consecuente.add(new Simbolo(simbolo, ""));
            }
        }

        if (btnInsertar.getText().equals("Guardar Cambios") && produccionSeleccionada != null) {
            // Modificar producción existente
            produccionSeleccionada.setAntec(new Antecedente());
            produccionSeleccionada.getAntec().setSimboloNT(comboBoxAntecedente.getValue());
            produccionSeleccionada.setConsec(consecuente);
            listProducciones.refresh();
        } else {
            // Insertar nueva producción
            Produccion nuevaProduccion = new Produccion();
            nuevaProduccion.setAntec(new Antecedente());
            nuevaProduccion.getAntec().setSimboloNT(comboBoxAntecedente.getValue());
            nuevaProduccion.setConsec(consecuente);
            producciones.add(nuevaProduccion);
        }

        // Limpiar campos
        txtConsecuente.clear();
        btnInsertar.setText("Insertar Producción");
        listProducciones.refresh();
    }

    @FXML
    private void onBtnEliminarAction() {
        Produccion seleccionada = listProducciones.getSelectionModel().getSelectedItem();
        if (seleccionada != null) {
            producciones.remove(seleccionada);
        }
    }

    @FXML
    private void onBtnBorrarAction() {
        txtConsecuente.clear();
    }

    @FXML
    private void onBtnAceptarAction() {
        panelPadre.panelPadre.getGramatica().setProducciones(producciones);
        panelPadre.asignarProducciones(producciones);// 🔹 Guardar en la gramática
        cerrarPestanaActual();
    }

    @FXML
    private void onBtnCancelarAction() {
        ButtonType btnSi = new ButtonType(bundle.getString("button.si"), ButtonBar.ButtonData.YES);
        ButtonType btnNo = new ButtonType(bundle.getString("button.no"), ButtonBar.ButtonData.NO);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, bundle.getString("producciones.dialog.cancelar.mensaje"), btnSi, btnNo);
        confirm.setTitle(bundle.getString("producciones.dialog.cancelar.titulo"));
        confirm.setHeaderText(bundle.getString("producciones.dialog.cancelar.titulo"));
        confirm.showAndWait().ifPresent(response -> {
            if (response == btnSi) {
                cerrarPestanaActual();
            }
        });
    }

    private void cerrarPestanaActual() {
        tabPane.getTabs().stream()
                .filter(tab -> tab.getContent() == this)
                .findFirst().ifPresent(tabActual -> tabPane.getTabs().remove(tabActual));

    }

    private void mostrarAlerta(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(bundle.getString("editor.dialog.error.titulo"));
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    @FXML
    private void onListNoTerminalesClicked() {
        NoTerminal seleccionado = (NoTerminal) listNoTerminales.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            agregarSimboloAlConsecuente(seleccionado);
        }
    }

    @FXML
    private void onListTerminalesClicked() {
        Terminal seleccionado = (Terminal) listTerminales.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            agregarSimboloAlConsecuente(seleccionado);
        }
    }

    @FXML
    private void onBtnEpsilonAction() {
        // Verifica que el campo txtConsecuente esté vacío antes de añadir ε
        if (txtConsecuente.getText().isEmpty()) {
            txtConsecuente.setText("ε");
        } else {
            mostrarAlerta(bundle.getString("producciones.error.epsilon"));
        }
    }

    @FXML
    private void onBtnModificarAction() {
        // Verificar si el conjunto de no terminales está vacío
        if (noTerminales.isEmpty()) {
            mostrarAlerta(bundle.getString("producciones.error.no.terminales.vacio"));
            return;
        }

        // Verificar si el conjunto de terminales está vacío
        if (terminales.isEmpty()) {
            mostrarAlerta(bundle.getString("producciones.error.terminales.vacio"));
            return;
        }

        // Obtener la producción seleccionada
        Produccion seleccionada = listProducciones.getSelectionModel().getSelectedItem();

        if (seleccionada != null) {
            produccionSeleccionada = seleccionada;

            // Buscar el índice del antecedente en el ComboBox y seleccionarlo
            comboBoxAntecedente.setValue(seleccionada.getAntec().getSimboloNT());

            // Convertir la lista de símbolos en un string separado por espacios
            String consecuenteTexto = seleccionada.getConsec().stream()
                    .map(Simbolo::getNombre)
                    .collect(Collectors.joining(" "));

            txtConsecuente.setText(consecuenteTexto);

            // Cambiar el botón de "Insertar" a "Guardar cambios"
            btnInsertar.setText(bundle.getString("producciones.btn.guardar.cambios"));
        } else {
            mostrarAlerta(bundle.getString("producciones.error.seleccion"));
        }
    }

    @Override
    public void actualizarTextos(ResourceBundle bundle) {
        this.bundle = bundle;
        if (labelHeader != null) labelHeader.setText(bundle.getString("producciones.header"));
        if (labelAntecedente != null) labelAntecedente.setText(bundle.getString("producciones.label.antecedente"));
        if (labelConsecuente != null) labelConsecuente.setText(bundle.getString("producciones.label.consecuente"));
        if (labelAnadirSimbolos != null) labelAnadirSimbolos.setText(bundle.getString("producciones.label.anadir.simbolos"));
        if (labelNoTerminales != null) labelNoTerminales.setText(bundle.getString("producciones.label.no.terminales"));
        if (labelTerminales != null) labelTerminales.setText(bundle.getString("producciones.label.terminales"));
        if (labelEpsilon != null) labelEpsilon.setText(bundle.getString("producciones.label.epsilon"));
        if (labelLista != null) labelLista.setText(bundle.getString("producciones.label.lista"));
        if (btnInsertar != null) btnInsertar.setText(bundle.getString("producciones.btn.insertar"));
        if (btnModificar != null) btnModificar.setText(bundle.getString("producciones.btn.modificar"));
        if (btnEliminar != null) btnEliminar.setText(bundle.getString("producciones.btn.eliminar"));
        if (btnBorrar != null) btnBorrar.setText(bundle.getString("producciones.btn.borrar"));
        if (btnEpsilon != null) btnEpsilon.setText(bundle.getString("producciones.btn.epsilon"));
        if (btnCancelar != null) btnCancelar.setText(bundle.getString("button.cancelar"));
        if (btnAceptar != null) btnAceptar.setText(bundle.getString("button.aceptar"));
        if (txtConsecuente != null) txtConsecuente.setPromptText(bundle.getString("producciones.prompt.consecuente"));
    }

}
