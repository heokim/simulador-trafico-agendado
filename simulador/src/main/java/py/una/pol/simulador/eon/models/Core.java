package py.una.pol.simulador.eon.models;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Núcleo de un enlace de la red
 *
 * @author Néstor E. Reinoso Wood
 */
@Data
public class Core implements Serializable {

    /**
     * Ancho de banda del núcleo
     */
    private BigDecimal bandwidth;
    /**
     * Ranuras de frecuencia del núcleo
     */
    private List<FrequencySlot> frequencySlots;

    /*
     * Una lista para tener todas las rutas que ocupan fs sobre un core
     */

     private List<Integer> id_rutas;

    /**
     * Constructor con cantidad de Ranuras de Frecuencia
     *
     * @param bandwidth Ancho de banda del nucleo
     * @param frequencySlotQuantity Cantidad de ranuras disponibles
     */
    public Core(BigDecimal bandwidth, Integer frequencySlotQuantity) {
        this.bandwidth = bandwidth;
        this.frequencySlots = new ArrayList<>();
        for (int i = 0; i < frequencySlotQuantity; i++) {
            this.frequencySlots.add(new FrequencySlot(bandwidth.divide(new BigDecimal(frequencySlotQuantity), RoundingMode.HALF_UP)));
        }
        //se incializa la lista de los id de las rutas
        id_rutas = new ArrayList<>();
    }

    /**
     * Constructor con una lista de Ranuras de Frecuencia
     *
     * @param bandwidth Ancho de banda del núcleo
     * @param frequencySlots Ranuras de frecuencias disponibles
     */
    public Core(BigDecimal bandwidth, List<FrequencySlot> frequencySlots) {
        this.bandwidth = bandwidth;
        this.frequencySlots = frequencySlots;
    }

    @Override
    public String toString() {
        return "Core{"
                + "bandwidth=" + bandwidth
                + ", fs=" + frequencySlots
                + "}";
    }

}
