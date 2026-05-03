package py.una.pol.simulador.eon.utils;

/**
 * Define cómo cambia el Erlang a lo largo de una simulación.
 */
public interface IErlangDistribution {
    /**
     * Devuelve el Erlang que debe usarse en una unidad de tiempo.
     * 
     * @param time Tiempo actual
     * @param totalTime Tiempo total de la simulación
     * @param baseErlang Erlang base, útil para distribuciones que lo toman como referencia
     * @return Erlang calculado para el tiempo actual
     */
    int getErlang(int time, int totalTime, int baseErlang);
    
    /**
     * Devuelve una etiqueta descriptiva de la franja de tráfico evaluada.
     *
     * @param time Tiempo actual
     * @param totalTime Tiempo total de la simulación
     * @return Etiqueta del tipo de tráfico, por ejemplo "BAJO", "MEDIO" o "ALTO"
     */
    String getTrafficType(int time, int totalTime);
}
