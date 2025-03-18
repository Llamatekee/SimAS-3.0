package gramatica;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Representa el consecuente de una producción en la gramática.
 */
public class Consecuente {

    private ObservableList<Simbolo> conjSimbolos;

    public Consecuente() {
        // Inicializamos la lista como ObservableList para facilitar el binding en JavaFX.
        this.conjSimbolos = FXCollections.observableArrayList();
    }

    public ObservableList<Simbolo> getConjSimbolos() {
        return conjSimbolos;
    }

    public void setConjSimbolos(ObservableList<Simbolo> conjSimbolos) {
        this.conjSimbolos = conjSimbolos;
    }
}
