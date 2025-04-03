package gramatica;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Representa una fila de la Tabla Predictiva, asociando un símbolo (terminal o no terminal) con sus valores.
 */
public class FilaTablaPredictiva {
    private final StringProperty simbolo;
    private final StringProperty prediccion;
    private final Map<String, StringProperty> valoresColumnas; // 🔥 Mapa dinámico para las columnas
    private final BooleanProperty esTerminal; // Para distinguir entre filas de terminales y no terminales

    public FilaTablaPredictiva(String simbolo, String prediccion, boolean esTerminal) {
        this.simbolo = new SimpleStringProperty(simbolo);
        this.prediccion = new SimpleStringProperty(prediccion);
        this.valoresColumnas = new HashMap<>();
        this.esTerminal = new SimpleBooleanProperty(esTerminal);
    }

    // Constructor original para mantener compatibilidad
    public FilaTablaPredictiva(String simbolo, String prediccion) {
        this(simbolo, prediccion, false); // Por defecto, es no terminal
    }

    // Métodos existentes (sin cambios)
    public String getSimbolo() { return simbolo.get(); }
    public void setSimbolo(String simbolo) { this.simbolo.set(simbolo); }
    public String getPrediccion() { return prediccion.get(); }
    public void setPrediccion(String prediccion) { this.prediccion.set(prediccion); }

    // 🔥 Métodos para manejar las columnas dinámicas
    public void setValor(String columna, String valor) {
        valoresColumnas.put(columna, new SimpleStringProperty(valor));
    }

    public StringProperty getValor(String columna) {
        return valoresColumnas.getOrDefault(columna, new SimpleStringProperty(""));
    }

    public ObservableValue<String> simboloProperty() {
        return simbolo;
    }

    // Métodos para el manejo del tipo de fila
    public boolean getEsTerminal() {
        return esTerminal.get();
    }

    public void setEsTerminal(boolean esTerminal) {
        this.esTerminal.set(esTerminal);
    }

    public BooleanProperty esTerminalProperty() {
        return esTerminal;
    }

    // Métodos de utilidad
    public String getNoTerminal() {
        return simbolo.getName();
    }

    public String getAccion(int j) {
        return valoresColumnas.getOrDefault(String.valueOf(j), new SimpleStringProperty("")).get();
    }
}
