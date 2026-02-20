package py.una.pol.simulador.eon.models.enums;

/**
 * Métrica de fragmentación del espectro utilizada para seleccionar
 * el bloque de ranuras de frecuencia (FS) que minimice la fragmentación
 * residual después de la asignación.
 *
 * <ul>
 *   <li>{@link #NONE}    – No se minimiza fragmentación; se elige el FS con menor crosstalk (comportamiento original).</li>
 *   <li>{@link #ENTROPY} – Se elige el FS cuya asignación genera menor entropía de Shannon residual en la ruta.</li>
 *   <li>{@link #BFR}     – Se elige el FS cuya asignación genera menor BFR (Band Fragmentation Ratio) en la ruta.</li>
 * </ul>
 */
public enum FragmentationMetric {

    /**
     * Sin minimización de fragmentación.
     * Comportamiento original: desempata solo por crosstalk mínimo.
     */
    NONE,

    /**
     * Minimiza la entropía de Shannon del espectro residual.
     * Mayor entropía = más fragmentado = peor opción.
     * H = -Σ p_i * log2(p_i), donde p_i = tamaño del i-ésimo bloque libre / total libre.
     */
    ENTROPY,

    /**
     * Minimiza el BFR (Band Fragmentation Ratio) del espectro residual.
     * BFR = número de bloques contiguos libres / total de slots libres.
     * Cuanto mayor es el BFR, más fragmentado está el espectro.
     */
    BFR
}
