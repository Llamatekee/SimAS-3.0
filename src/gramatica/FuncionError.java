package gramatica;

import java.util.Objects;

/**
 * Representa una función de manejo de errores en la Tabla Predictiva.
 */
public class FuncionError {

    public static final int INSERTAR_ENTRADA = 1;
    public static final int BORRAR_ENTRADA = 2;
    public static final int MODIFICAR_ENTRADA = 3;
    public static final int INSERTAR_PILA = 4;
    public static final int BORRAR_PILA = 5;
    public static final int MODIFICAR_PILA = 6;
    public static final int TERMINAR_ANALISIS = 7;

    private int identificador;
    private int accion;
    private String mensaje;
    private Terminal simbolo;

    public FuncionError(int id, int acc, String mensaje) {
        this.identificador = id;
        this.accion = acc;
        this.mensaje = mensaje;
    }

    public FuncionError() {}

    public int getIdentificador() {
        return identificador;
    }

    public void setIdentificador(int identificador) {
        this.identificador = identificador;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public int getAccion() {
        return accion;
    }

    public void setAccion(int accion) {
        this.accion = accion;
    }

    public Terminal getSimbolo() {
        return simbolo;
    }

    public void setSimbolo(Terminal simbolo) {
        this.simbolo = simbolo;
    }

    private String getNombreAccion() {
        switch (accion) {
            case INSERTAR_ENTRADA:
                return "Insertar en entrada";
            case BORRAR_ENTRADA:
                return "Borrar de entrada";
            case MODIFICAR_ENTRADA:
                return "Modificar entrada";
            case INSERTAR_PILA:
                return "Insertar en pila";
            case BORRAR_PILA:
                return "Borrar de pila";
            case MODIFICAR_PILA:
                return "Modificar pila";
            case TERMINAR_ANALISIS:
                return "Terminar análisis";
            default:
                return "Acción desconocida";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FuncionError that = (FuncionError) o;
        return accion == that.accion &&
                Objects.equals(simbolo, that.simbolo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accion, simbolo);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(identificador).append(". ");
        sb.append(getNombreAccion());
        if (simbolo != null) {
            sb.append(" - Símbolo: ").append(simbolo.getNombre());
        }
        if (mensaje != null && !mensaje.isEmpty()) {
            sb.append(" (").append(mensaje).append(")");
        }
        return sb.toString();
    }
}
