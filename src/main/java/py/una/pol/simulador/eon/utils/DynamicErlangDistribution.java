package py.una.pol.simulador.eon.utils;

/**
 * Distribucion usada por simulacionErlangVariable().
 *
 * Divide el tiempo total de simulacion en 5 franjas iguales:
 * bajo, medio, alto, medio y bajo.
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
        if (fraction < 0.2) return "BAJO";
        if (fraction < 0.4) return "MEDIO";
        if (fraction < 0.6) return "ALTO";
        if (fraction < 0.8) return "MEDIO";
        return "BAJO";
    }
}
