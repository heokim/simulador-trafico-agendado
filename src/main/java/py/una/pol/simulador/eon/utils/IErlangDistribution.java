package py.una.pol.simulador.eon.utils;

/**
 * Define como cambia el Erlang a lo largo de una simulacion.
 */
public interface IErlangDistribution {

    int getErlang(int time, int totalTime, int baseErlang);

    String getTrafficType(int time, int totalTime);
}
