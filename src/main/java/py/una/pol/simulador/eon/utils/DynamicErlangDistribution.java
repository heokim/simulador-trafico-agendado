package py.una.pol.simulador.eon.utils;

/**
 * Distribucion usada por simulacionErlangVariable().
 *
 * Divide el tiempo total de simulacion en 3 franjas iguales:
 * bajo, medio y alto.
 */
public class DynamicErlangDistribution implements IErlangDistribution {

    private int erlangBajo;
    private int erlangMedio;
    private int erlangAlto;

    public DynamicErlangDistribution(int erlangBajo, int erlangMedio, int erlangAlto) {
        this.erlangBajo = erlangBajo;
        this.erlangMedio = erlangMedio;
        this.erlangAlto = erlangAlto;
    }

    @Override
    public int getErlang(int time, int totalTime, int baseErlang) {
        String trafficType = getTrafficType(time, totalTime);
        switch (trafficType) {
            case "BAJO":
                return erlangBajo;
            case "MEDIO":
                return erlangMedio;
            case "ALTO":
                return erlangAlto;
            default:
                return baseErlang;
        }
    }

    @Override
    public String getTrafficType(int time, int totalTime) {
        double fraction = (double) time / totalTime;
        if (fraction < (1.0 / 3.0)) return "BAJO";
        if (fraction < (2.0 / 3.0)) return "MEDIO";
        return "ALTO";
    }
}
