package gramatica;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.stream.Collectors;

/**
 * Representa una producción de la gramática.
 */
public class Produccion {

    private Antecedente antec;
    private ObservableList<Simbolo> consec;
    private int numero;

    public Produccion() {
        this.antec = new Antecedente();
        this.consec = FXCollections.observableArrayList();
    }

    public Produccion(Antecedente antecedente, ObservableList<Simbolo> simbolos) {
        this.antec = antecedente;
        this.consec = simbolos;
    }

    public Antecedente getAntec() {
        return antec;
    }

    public void setAntec(Antecedente antec) {
        this.antec = antec;
    }

    public ObservableList<Simbolo> getConsec() {
        return consec;
    }

    public void setConsec(ObservableList<Simbolo> consec) {
        this.consec = consec;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public int getNumero() {
        return numero;
    }

    @Override
    public String toString() {
        // Convertir los símbolos del consecuente en nombres de texto
        String consecuenteStr = consec.stream()
                .map(Simbolo::getNombre)  // Obtener solo el nombre de cada símbolo
                .collect(Collectors.joining(" ")); // Unir con espacio

        // Devolver la producción con su número
        return numero + ". " + antec.toString() + " → " + (consecuenteStr.isEmpty() ? "∅" : consecuenteStr);
    }


    public void modificarSimbolo(String simboloAntiguo, String nuevoSimbolo) {
        // Modificar el antecedente si coincide con el símbolo antiguo
        if (antec.toString().equals(simboloAntiguo)) {
            NoTerminal nuevoSimboloNT = new NoTerminal(nuevoSimbolo, "");
            antec.setSimboloNT(nuevoSimboloNT);
        }

        // Modificar los símbolos en el consecuente
        for (int i = 0; i < consec.size(); i++) {
            if (consec.get(i).getNombre().equals(simboloAntiguo)) {
                consec.get(i).setNombre(nuevoSimbolo);
            }
        }
    }
}
