package gramatica;

/**
 * Representa el antecedente de una producción en la gramática.
 */
public class Antecedente {

    private NoTerminal simboloNT;

    public Antecedente() {
        // Inicializamos el símbolo no terminal con valores nulos.
        this.simboloNT = new NoTerminal("", ""); // Evita valores nulos
    }
    public Antecedente(NoTerminal simboloNT) {
        this.simboloNT = simboloNT;
    }

    public NoTerminal getSimboloNT() {
        return simboloNT;
    }

    public void setSimboloNT(NoTerminal simboloNT) {
        this.simboloNT = simboloNT;
    }

    @Override
    public String toString() {
        return simboloNT != null ? simboloNT.toString() : "∅"; // Representación de vacío si es null
    }
}
