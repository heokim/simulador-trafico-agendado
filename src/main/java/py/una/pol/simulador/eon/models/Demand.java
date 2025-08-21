package py.una.pol.simulador.eon.models;

import lombok.Data;

/**
 * Demanda generada para la conexión
 *
 * @author Néstor E. Reinoso Wood
 */
@Data
public class Demand {

    /**
     * Identificador de la demanda
     */
    private Integer id;
    /**
     * Nodo origen
     */
    private Integer source;
    /**
     * Nodo destino
     */
    private Integer destination;
    /**
     * Cantidad de ranuras de frecuencia a utilizar
     */
    private Integer fs;
    /**
     * Tiempo de vida
     */
    private Integer lifetime;
    /**
     * Indica si la demanda fue bloqueada
     */
    private Boolean blocked;
    /**
     * Tiempo de simulación en el que se inserta la demanda
     */
    private final Integer insertionTime;

    /**
     * Constructor con todos los parámetros
     *
     * @param id ID de la demanda
     * @param source Origen de la demanda
     * @param destination Destino de la demanda
     * @param fs Ranuras de frecuencia que requiere la demanda
     * @param lifetime Tiempo de vida de la conexión
     * @param blocked Indica si la demanda fue bloqueada o no
     * @param insertionTime Tiempo en el que se inserta la demanda
     */
    public Demand(Integer id, Integer source, Integer destination, Integer fs,
            Integer lifetime, Boolean blocked, Integer insertionTime) {
        this.id = id;
        this.source = source;
        this.destination = destination;
        this.fs = fs;
        this.lifetime = lifetime;
        this.blocked = blocked;
        this.insertionTime = insertionTime;
    }

    @Override
    public String toString() {
        return "Demand{"
                + "Origen=" + source
                + ", Destino=" + destination
                + ", FS=" + fs
                + ", Tiempo de vida=" + lifetime
                + ", Bloqueado=" + (blocked ? "Si" : "No")
                + '}';
    }
}
