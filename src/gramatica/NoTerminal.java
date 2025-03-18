package gramatica;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Representa un símbolo no terminal de la gramática.
 */
public class NoTerminal extends Simbolo {

    private boolean simboloInicial;
    private ObservableList<Terminal> primeros;
    private ObservableList<Terminal> siguientes;

    public NoTerminal(String nombre, String valor) {
        super(nombre, valor);
        // Inicializamos las listas como ObservableList
        this.primeros = FXCollections.observableArrayList();
        this.siguientes = FXCollections.observableArrayList();
    }

    public boolean getSimboloInicial() {
        return simboloInicial;
    }

    public void setSimboloInicial(boolean simboloInicial) {
        this.simboloInicial = simboloInicial;
    }

    public ObservableList<Terminal> getPrimeros() {
        return primeros;
    }

    public void setPrimeros(ObservableList<Terminal> primeros) {
        this.primeros = primeros;
    }

    public ObservableList<Terminal> getSiguientes() {
        return siguientes;
    }

    public void setSiguientes(ObservableList<Terminal> siguientes) {
        this.siguientes = siguientes;
    }
    @Override
    public String toString() {
        return getNombre();
    }
}
