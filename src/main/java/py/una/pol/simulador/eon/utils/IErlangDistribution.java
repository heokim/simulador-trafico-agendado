package py.una.pol.simulador.eon.utils;

public interface IErlangDistribution {
    int getErlang(int time, int totalTime, int baseErlang);

    String getTrafficType(int time, int totalTime);
}
