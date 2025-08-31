package utils;

import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

/**
 * Celda personalizada para mostrar idiomas con banderas en el ComboBox
 */
public class LanguageListCell extends ListCell<LanguageItem> {
    
    private final HBox content;
    private final Label textLabel;
    
    public LanguageListCell() {
        content = new HBox();
        content.setSpacing(8);
        
        textLabel = new Label();
        textLabel.setTextFill(Color.WHITE); // Establecer el color del texto en blanco
        textLabel.setStyle("-fx-text-fill: white;"); // CSS adicional para asegurar el color blanco
        HBox.setHgrow(textLabel, Priority.ALWAYS);
        
        content.getChildren().addAll(textLabel);
    }
    
    @Override
    protected void updateItem(LanguageItem item, boolean empty) {
        super.updateItem(item, empty);
        
        if (empty || item == null) {
            setGraphic(null);
            setText(null);
        } else {
            // Limpiar contenido anterior
            content.getChildren().clear();
            
            // Crear una nueva instancia del ImageView para evitar problemas de referencia
            ImageView flagView = new ImageView(item.getFlagImageView().getImage());
            flagView.setFitHeight(16);
            flagView.setFitWidth(24);
            flagView.setPreserveRatio(true);
            
            // Agregar la bandera
            content.getChildren().add(flagView);
            
            // Agregar el texto del idioma
            textLabel.setText(item.getName());
            content.getChildren().add(textLabel);
            
            // Asegurar que el contenido se mantenga visible
            setGraphic(content);
            setText(null);
            
            // Forzar la actualización del layout
            content.requestLayout();
        }
    }
}
