package py.una.pol.simulador.eon.utils;

/**
 * Define como cambia el Erlang a lo largo de una simulacion.
 */
public interface IErlangDistribution {

    /**
     * Devuelve el Erlang que debe usarse en una unidad de tiempo.
     *
     * @param time Tiempo actual
     * @param totalTime Tiempo total de la simulacion
     * @param baseErlang Erlang base, util para distribuciones que lo toman como referencia
     * @return Erlang calculado para el tiempo actual
     */
    int getErlang(int time, int totalTime, int baseErlang);

    /**
     * Devuelve la etiqueta de carga de la franja evaluada.
     *
     * @param time Tiempo actual
     * @param totalTime Tiempo total de la simulacion
     * @return Etiqueta de carga: BAJO, MEDIO o ALTO
     */
    String getTrafficType(int time, int totalTime);
}
