package gramatica;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Representa la tabla predictiva de una gramática.
 */
public class TablaPredictiva {

    private final TableView<FilaTablaPredictiva> tablaPredictiva;
    private Gramatica gramatica;
    private final Map<String, Integer> indiceColumnas;
    private List<FuncionError> funcionesError;

    public TablaPredictiva() {
        this.tablaPredictiva = new TableView<>();
        this.indiceColumnas = new HashMap<>();
        this.funcionesError = new ArrayList<>();
    }
    public TablaPredictiva(TableView<FilaTablaPredictiva> tabla) {
        this.tablaPredictiva = tabla;
        this.indiceColumnas = new HashMap<>();
        this.funcionesError = new ArrayList<>();
    }

    public void construir(Gramatica gramatica) {
        this.gramatica = gramatica;
        tablaPredictiva.getColumns().clear();
        tablaPredictiva.getItems().clear();
        indiceColumnas.clear();

        // Primera columna con los No Terminales
        TableColumn<FilaTablaPredictiva, String> colNT = new TableColumn<>("No Terminal");
        colNT.setCellValueFactory(cellData -> cellData.getValue().simboloProperty());
        tablaPredictiva.getColumns().add(colNT);

        // Crear columnas dinámicas para los terminales
        for (Terminal t : gramatica.getTerminales()) {
            TableColumn<FilaTablaPredictiva, String> colT = new TableColumn<>(t.getNombre());

            // Usamos un Callback para acceder a los valores en el Map de `FilaTablaPredictiva`
            colT.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<FilaTablaPredictiva, String>, ObservableValue<String>>() {
                @Override
                public ObservableValue<String> call(TableColumn.CellDataFeatures<FilaTablaPredictiva, String> param) {
                    return param.getValue().getValor(t.getNombre());
                }
            });

            tablaPredictiva.getColumns().add(colT);
        }

        cargarDatos();
    }

    private void cargarDatos() {
        ObservableList<FilaTablaPredictiva> filas = FXCollections.observableArrayList();

        for (NoTerminal nt : gramatica.getNoTerminales()) {
            FilaTablaPredictiva fila = new FilaTablaPredictiva(nt.getNombre(), "");

            for (Terminal t : gramatica.getTerminales()) {
                List<String> producciones = gramatica.getProduccionesPorNoTerminalYTerminal(nt, t);

                if (!producciones.isEmpty()) {
                    String primeraProduccion = producciones.get(0);
                    fila.setValor(t.getNombre(), primeraProduccion);
                }
            }

            filas.add(fila); // ✅ Solo esta línea es correcta
        }

        tablaPredictiva.setItems(filas);
        tablaPredictiva.refresh();
    }

    private String obtenerPredicciones(NoTerminal nt) {
        // Suponiendo que hay un mapa con la relación NoTerminal -> Predicciones
        List<String> reglas = new ArrayList<>();

        for (String terminal : indiceColumnas.keySet()) {
            // Obtener la producción asociada a (NoTerminal, Terminal)
            String produccion = gramatica.getProduccion(nt, terminal);
            if (produccion != null) {
                reglas.add(terminal + " → " + produccion);
            }
        }

        return String.join(", ", reglas);
    }

    public void crearFunError(FuncionError funcionError) {
        funcionesError.add(funcionError);
    }

    public List<FuncionError> getFuncionesError() {
        return funcionesError;
    }

    public void setFuncionesError(List<FuncionError> funcionesError) {
        this.funcionesError = funcionesError;
    }

    public TableView<FilaTablaPredictiva> getTablaPredictiva() {
        return tablaPredictiva;
    }

    public List<FilaTablaPredictiva> getFilas() {
        return tablaPredictiva.getItems();
    }
}