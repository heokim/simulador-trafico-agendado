package py.una.pol.simulador.eon.utils;

/**
 * Define una distribución de Erlang sobre el tiempo de la simulación.
 */
public interface IErlangDistribution {
    /**
     * Devuelve el Erlang para la unidad de tiempo especificada.
     * 
     * @param time Tiempo actual
     * @param totalTime Tiempo total de la simulación
     * @param baseErlang Erlang base (opcional dependiendo de la implementación)
     * @return El erlang calculado
     */
    int getErlang(int time, int totalTime, int baseErlang);
    
    /**
     * Devuelve el entorno de la partición de tiempo evaluada.
     * Ej: "BAJO", "MEDIO", "ALTO".
     *
     * @param time Tiempo actual
     * @param totalTime Tiempo total de la simulación
     * @return una etiqueta del tipo de tráfico
     */
    String getTrafficType(int time, int totalTime);
}
