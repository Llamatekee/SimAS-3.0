package gramatica;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Representa un símbolo en la gramática.
 */
public class Simbolo {

    private final StringProperty nombre;
    private final StringProperty valor;

    public Simbolo(String nombre, String valor) {
        this.nombre = new SimpleStringProperty(nombre);
        this.valor = new SimpleStringProperty(valor);
    }

    // Getter y setter para 'nombre'
    public String getNombre() {
        return nombre.get();
    }

    public void setNombre(String nombre) {
        this.nombre.set(nombre);
    }

    public StringProperty nombreProperty() {
        return nombre;
    }

    // Getter y setter para 'valor'
    public String getValor() {
        return valor.get();
    }

    public void setValor(String valor) {
        this.valor.set(valor);
    }

    public StringProperty valorProperty() {
        return valor;
    }
}
