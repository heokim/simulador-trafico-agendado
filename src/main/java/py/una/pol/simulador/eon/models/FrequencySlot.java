package py.una.pol.simulador.eon.models;

import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

/**
 * Ranura de frecuencia en los núcleos de las fibras
 *
 * @author Néstor E. Reinoso Wood
 */
@Data
public class FrequencySlot implements Serializable {

    /**
     * Tiempo de vida de la conexión insertada
     */
    private int lifetime;
    /**
     * Indica si la ranura está o no ocupada
     */
    private boolean free;
    /**
     * Ancho de banda de la ranura
     */
    private BigDecimal fsWidh;
    /**
     * Crosstalk actual sobre la ranura
     */
    private BigDecimal crosstalk;

    /**
     * Etiqueta para identificar con que ruta esta relacionado el fs
     */
    private Integer id_rutas;

    /**
     * Constructor
     *
     * @param fsWidh Tamaño de la ranura de frecuencia en GHz.
     */
    public FrequencySlot(BigDecimal fsWidh) {
        this.fsWidh = fsWidh;
        this.lifetime = 0;
        this.free = true;
        this.crosstalk = BigDecimal.ZERO;
    }

    /**
     * Substrae una unidad de tiempo de la ranura de frecuencias, si es posible
     *
     * @return  <ul>
     * <li><code>true</code> Si la ranura está ocupada despues de la resta</li>
     * <li><code>false</code> Si la ranura está libre despues de la resta</li>
     * </ul>
     */
    public boolean subLifetime() {
        if (this.free) {
            return false;
        }
        this.lifetime--;
        if (this.lifetime == 0) {
            this.free = true;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "FrecuencySlot{"
                + "lifetime=" + lifetime
                + ", free=" + free
                + ", fsWidh=" + fsWidh
                + ", crosstalk=" + crosstalk.toPlainString()
                + "}";
    }

}
