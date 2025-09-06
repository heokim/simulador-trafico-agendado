package py.una.pol.simulador.eon.models;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Ruta establecida por un algoritmo RSA
 *
 * @author Néstor E. Reinoso Wood
 */
@Data
@AllArgsConstructor
public class EstablishedRoute {

    private static int contador = 0;

    /**
     * Índice inicial del bloque de ranuras de frecuencias que ocupa la conexión
     */
    private Integer fsIndexBegin;
    /**
     * Cantidad de ranuras que ocupa la conexión
     */
    private Integer fsWidth;
    /**
     * Tiempo de vida restante de la conexión
     */
    private Integer lifetime;
    /**
     * Nodo origen
     */
    private Integer from;
    /**
     * Nodo destino
     */
    private Integer to;
    /**
     * Enlaces de la ruta
     */
    private List<Link> path;
    /**
     * Núcleos de los enlaces de la ruta
     */
    private List<Integer> pathCores;

    /**
     * Variable para guardar el camino con el que
     * se establece la ruta, el valor es de 0 - 4 (por los 5 caminos del KSP)
     **/
    private Integer K_elegido;

    /**
     * Variable para guardar la mayor longitud de los enlaces del camino K.
     */
    private Integer diametro;

    /**
     * Lista Auxiliar para guardar la cantidad de nucleos que interfieren con el crosstalk por enlace
     * esta lista se usa para el calculo del crosstalk de la ruta
     */
    List<Integer> vecinos_crosstalk;

    /**
     * ID de la ruta
     */
    Integer id;

    /**
     * Constructor vacío
     */
    public EstablishedRoute() {
    }

    /**
     * Constructor con parámetros
     *
     * @param path              Enlaces de la ruta establecida
     * @param fsIndexBegin      Indice inicial del bloque de frecuencias utilizado
     * @param fsWidth           Cantidad de ranuras de frecuencia a utilizar
     * @param lifetime          Tiempo de vida de la demanda en la ruta
     * @param from              Nodo origen
     * @param to                Nodo destino
     * @param pathCores         Núcleos a los que pertenecen los enlaces de la lista
     * @param K_elegido         Es el camino elegido entre los 5 caminos del alogirmto KSP.
     * @param diametro          es la longitud mayor encontrado entre todos los enlaces del camino K_elegido de la ruta.
     * @param vecinos_crosstalk lista donde se guardan los vecinos a considerar para el crosstalk por enlace de la ruta establecida
     */
    public EstablishedRoute(List<Link> path, Integer fsIndexBegin, Integer fsWidth, Integer lifetime, Integer from, Integer to, List<Integer> pathCores, Integer K_elegido, Integer diametro, List<Integer> vecinos_crosstalk) {
        this.path = path;
        this.fsIndexBegin = fsIndexBegin;
        this.fsWidth = fsWidth;
        this.lifetime = lifetime;
        this.from = from;
        this.to = to;
        this.pathCores = pathCores;
        this.K_elegido = K_elegido;
        this.diametro = diametro;
        this.vecinos_crosstalk = vecinos_crosstalk;
        this.id = ++contador;
    }

    /**
     * Resta una unidad de tiempo a la conexión
     */
    public void subLifeTime() {
        this.lifetime--;
    }

    @Override
    public String toString() {
        String asd = "EstablisedRoute{"
                + "path=" + path
                + ", fsIndexBegin=" + fsIndexBegin
                + ", fsWidth=" + fsWidth
                + ", lifetime=" + lifetime
                + ", from=" + from
                + ", to=" + to
                + ", pathCores=" + pathCores
                + ", K_elegido=" + K_elegido
                + ", diametro=" + diametro
                + ", vecinos_crosstalk=" + vecinos_crosstalk
                + ", id=" + id
                + "}";
        for (Link link : path) {
            asd = asd + link.toString();
        }
        return asd;
    }

}
