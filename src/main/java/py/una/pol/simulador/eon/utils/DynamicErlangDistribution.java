package py.una.pol.simulador.eon.utils;

public class DynamicErlangDistribution implements IErlangDistribution {

    private final int erlangBajo;
    private final int erlangMedio;
    private final int erlangAlto;

    public DynamicErlangDistribution(int erlangBajo, int erlangMedio, int erlangAlto) {
        this.erlangBajo = erlangBajo;
        this.erlangMedio = erlangMedio;
        this.erlangAlto = erlangAlto;
    }

    @Override
    public int getErlang(int time, int totalTime, int baseErlang) {
        String trafficType = getTrafficType(time, totalTime);
        if ("BAJO".equals(trafficType)) {
            return erlangBajo;
        }
        if ("MEDIO".equals(trafficType)) {
            return erlangMedio;
        }
        return erlangAlto;
    }

    @Override
    public String getTrafficType(int time, int totalTime) {
        double fraction = (double) time / totalTime;
        if (fraction < 0.2) {
            return "BAJO";
        }
        if (fraction < 0.4) {
            return "MEDIO";
        }
        if (fraction < 0.6) {
            return "ALTO";
        }
        if (fraction < 0.8) {
            return "MEDIO";
        }
        return "BAJO";
    }
}
