package simulador;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import gramatica.Terminal;
import java.util.List;

public class EditorCadenaEntradaController {
    @FXML private TextField inputField;
    @FXML private Button buttonBorrar;
    @FXML private ListView<String> listViewTerminales;
    @FXML private Button buttonCancelar;
    @FXML private Button buttonAceptar;
    
    private Stage stage;
    private String resultado;
    
    @FXML
    private void initialize() {
        // Configurar el evento de doble clic en la lista de terminales
        listViewTerminales.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String terminal = listViewTerminales.getSelectionModel().getSelectedItem();
                if (terminal != null) {
                    // Añadir el terminal en la posición del cursor
                    int pos = inputField.getCaretPosition();
                    String texto = inputField.getText();
                    String nuevoTexto = texto.substring(0, pos) + terminal + texto.substring(pos);
                    inputField.setText(nuevoTexto);
                    inputField.positionCaret(pos + terminal.length());
                }
            }
        });
        
        // Configurar los botones
        buttonBorrar.setOnAction(e -> inputField.clear());
        buttonCancelar.setOnAction(e -> stage.close());
        buttonAceptar.setOnAction(e -> {
            resultado = inputField.getText();
            stage.close();
        });
    }
    
    public void setStage(Stage stage) {
        this.stage = stage;
    }
    
    public void setTerminales(List<Terminal> terminales) {
        ObservableList<String> items = FXCollections.observableArrayList();
        for (Terminal t : terminales) {
            items.add(t.getNombre());
        }
        listViewTerminales.setItems(items);
    }
    
    public void setCadenaInicial(String cadena) {
        inputField.setText(cadena);
    }
    
    public String getResultado() {
        return resultado;
    }
} 