package py.una.pol.simulador.eon.models;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Enlace entre dos nodos
 *
 * @author Néstor E. Reinoso Wood
 */
@Data
@AllArgsConstructor
public class Link implements Serializable {

    /**
     * Distancia entre nodos
     */
    private int distance;
    /**
     * Núcleos de la fibra
     */
    private List<Core> cores;
    /**
     * Nodo origen
     */
    private int from;
    /**
     * Nodo destino
     */
    private int to;

    @Override
    public String toString() {
        return String.format("%s - %s\n%s", to, from, distance);
    }

    /**
     * Imprime el estado de un enlace en un nucleo en un rango de frecuencias
     *
     * @param core Núcleo
     * @param fsIndexBegin Indice inicial del bloque de frecuencias
     * @param fsWidth Cantidad de ranuras de frecuencia
     * @return Representación en texto del enlace
     */
    public String toString(int core, int fsIndexBegin, int fsWidth) {
        String asd = "Link {"
                + "distance=" + distance
                + ", core=" + core
                + ", from=" + from
                + ", to=" + to
                + "}";
        for (FrequencySlot fs : cores.get(core).getFrequencySlots()) {
            asd = asd + fs.toString();
        }
        return asd;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }

        if (!(object instanceof Link)) {
            return false;
        }

        Link c = (Link) object;
        return this.hashCode() == c.hashCode();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + this.from;
        hash = 67 * hash + this.to;
        return hash;
    }

}
