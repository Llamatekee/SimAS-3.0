package gramatica;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Representa una fila de la Tabla Predictiva, asociando un No Terminal con sus valores predictivos.
 */
public class FilaTablaPredictiva {
    private final StringProperty simbolo;
    private final StringProperty prediccion;
    private final Map<String, StringProperty> valoresColumnas; // 🔥 Mapa dinámico para las columnas

    public FilaTablaPredictiva(String simbolo, String prediccion) {
        this.simbolo = new SimpleStringProperty(simbolo);
        this.prediccion = new SimpleStringProperty(prediccion);
        this.valoresColumnas = new HashMap<>();
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

    public String getNoTerminal() {
        return simbolo.getName();
    }

    public String getAccion(int j) {
        return valoresColumnas.getOrDefault(j, new SimpleStringProperty("")).get();
    }
}
