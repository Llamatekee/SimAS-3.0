package simulador;

import javafx.scene.Parent;

/**
 * Interfaz que deben implementar todos los pasos de la simulación descendente.
 */
public interface PanelNuevaSimDescPaso {
    /**
     * Retorna el nodo raíz del paso.
     * @return El nodo raíz del paso.
     */
    Parent getRoot();
} 