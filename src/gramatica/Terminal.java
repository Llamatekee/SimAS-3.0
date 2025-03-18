package gramatica;

/**
 * Representa un símbolo terminal de la gramática.
 */
public class Terminal extends Simbolo {

    public Terminal(String nombre, String valor) {
        super(nombre, valor);
    }

    @Override
    public String toString() {
        return getNombre(); // Usa el método de la clase padre
    }
}
