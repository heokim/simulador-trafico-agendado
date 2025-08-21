package py.una.pol.simulador.eon.models;

import lombok.Data;
import org.jgrapht.Graph;

/**
 * Respuesta a la operación de asignación de conexiones a ranuras de frecuencias
 *
 * @author Néstor E. Reinoso Wood
 */
@Data
public class AssignFsResponse {

    /**
     * Ruta establecida
     */
    private EstablishedRoute route;
    /**
     * Estado de la red durante la conexión
     */
    private Graph<Integer, Link> graph;

    /**
     * Constructor vacío
     */
    public AssignFsResponse() {
    }

    /**
     * Constructor con parámetros
     *
     * @param graph Topología de la red
     * @param route Ruta establecida por el RSA
     */
    public AssignFsResponse(Graph<Integer, Link> graph, EstablishedRoute route) {
        this.graph = graph;
        this.route = route;
    }

}
