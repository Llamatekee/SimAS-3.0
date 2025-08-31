package utils;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Clase que representa un idioma con su bandera y nombre
 * para ser usado en el ComboBox de selección de idioma
 */
public class LanguageItem {
    private final String name;
    private final String locale;
    private final String flagPath;
    private final ImageView flagImageView;
    
    public LanguageItem(String name, String locale, String flagPath) {
        this.name = name;
        this.locale = locale;
        this.flagPath = flagPath;
        
        // Crear el ImageView para la bandera
        ImageView tempImageView;
        try {
            Image flagImage = new Image(getClass().getResourceAsStream("/resources/" + flagPath));
            tempImageView = new ImageView(flagImage);
            tempImageView.setFitHeight(16);
            tempImageView.setFitWidth(24);
            tempImageView.setPreserveRatio(true);
        } catch (Exception e) {
            // Si no se puede cargar la imagen, crear un ImageView vacío
            tempImageView = new ImageView();
            System.err.println("No se pudo cargar la bandera para " + name + ": " + e.getMessage());
        }
        this.flagImageView = tempImageView;
    }
    
    public String getName() {
        return name;
    }
    
    public String getLocale() {
        return locale;
    }
    
    public String getFlagPath() {
        return flagPath;
    }
    
    public ImageView getFlagImageView() {
        return flagImageView;
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LanguageItem that = (LanguageItem) obj;
        return locale.equals(that.locale);
    }
    
    @Override
    public int hashCode() {
        return locale.hashCode();
    }
}
