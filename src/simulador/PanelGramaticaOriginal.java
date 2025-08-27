package simulador;

import editor.ActualizableTextos;
import gramatica.Gramatica;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.util.ResourceBundle;

/**
 * Panel para mostrar la gramática original en una pestaña separada.
 * Implementa ActualizableTextos para soporte de internacionalización.
 */
public class PanelGramaticaOriginal extends VBox implements ActualizableTextos {

    @FXML private Label titulo;
    @FXML private ListView<String> listView;
    
    private final Gramatica gramaticaOriginal;

    public PanelGramaticaOriginal(Gramatica gramaticaOriginal, ResourceBundle bundle) {
        this.gramaticaOriginal = gramaticaOriginal;
        
        // Configurar el panel
        setPadding(new Insets(40));
        getStyleClass().add("wizard-step");
        setSpacing(30);
        
        // Crear y configurar el título
        titulo = new Label(bundle.getString("simulador.gramatica.titulo"));
        titulo.getStyleClass().add("wizard-header");
        titulo.setMaxWidth(Double.MAX_VALUE);
        titulo.setAlignment(Pos.CENTER);
        
        // Crear y configurar la lista de producciones
        listView = new ListView<>();
        listView.setItems(FXCollections.observableArrayList(gramaticaOriginal.getProduccionesModel()));
        listView.setPrefHeight(350);
        listView.setMaxHeight(400);
        listView.setMaxWidth(Double.MAX_VALUE);
        listView.setPrefWidth(Double.MAX_VALUE);
        listView.getStyleClass().add("gramatica-original-list");
        
        // Configurar el estilo de las celdas de la lista
        listView.setCellFactory(param -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    getStyleClass().clear();
                } else {
                    setText(item);
                    getStyleClass().clear();
                    // Aplicar estilo personalizado para las celdas
                    setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10px 15px;");
                }
            }
        });
        
        // Añadir elementos al contenido
        getChildren().addAll(titulo, listView);
    }

    @Override
    public void actualizarTextos(ResourceBundle bundle) {
        
        // Actualizar el título
        if (titulo != null) {
            titulo.setText(bundle.getString("simulador.gramatica.titulo"));
        }
        
        // Actualizar la lista de producciones
        if (listView != null) {
            listView.setItems(FXCollections.observableArrayList(gramaticaOriginal.getProduccionesModel()));
        }
    }

    public Parent getRoot() {
        return this;
    }
}
