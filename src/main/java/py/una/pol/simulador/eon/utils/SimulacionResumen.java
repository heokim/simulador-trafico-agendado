package py.una.pol.simulador.eon.utils;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class SimulacionResumen {
    private Timestamp tiempoInicio;
    private Timestamp tiempoFin;
    private long tiempoEjecucion;
    private String duracion;
    private String topologia;
    private int numeroDeBloqueos;
    private int numeroDeRutasEstablecidas;
    private int numeroDeDemandasPospuestas;
    private int cantidadDeDemandas;
    private int k1, k2, k3, k4, k5;
    private int numeroDeBloqueosPorFragmentacion;
    private int numeroDeBloqueosPorCrosstalk;
    private int numeroDeBloqueosPorFragmentacionDeCamino;
    private int diametroGrafo;
    private int gradoPromedio;
    private int inputDemands;
    private String inputValorH;
    private BigDecimal inputDecimal;
    private BigDecimal inputFsWidth;
    private int inputFsRangeMax;
    private int inputFsRangeMin;
    private int inputCapacity;
    private int inputCores;
    private int inputLambda;
    private int inputSimulationTime;
    private BigDecimal inputMaxCrosstalk;
    private int inputTRangeMin;
    private int inputTRangeMax;
    private int inputErlang;
    private BigDecimal inputXtPerUnitLength;

    public SimulacionResumen(Timestamp tiempoInicio, Timestamp tiempoFin, long tiempoEjecucion, String duracion,
                             String topologia, int numeroDeBloqueos, int numeroDeRutasEstablecidas, int numeroDeDemandasPospuestas,
                             int cantidadDeDemandas, int k1, int k2, int k3, int k4, int k5, int numeroDeBloqueosPorFragmentacion,
                             int numeroDeBloqueosPorCrosstalk, int numeroDeBloqueosPorFragmentacionDeCamino, int diametroGrafo,
                             int gradoPromedio, int inputDemands, String inputValorH, BigDecimal inputDecimal, BigDecimal inputFsWidth,
                             int inputFsRangeMax, int inputFsRangeMin, int inputCapacity, int inputCores, int inputLambda,
                             int inputSimulationTime, BigDecimal inputMaxCrosstalk, int inputTRangeMin, int inputTRangeMax,
                             int inputErlang, BigDecimal inputXtPerUnitLength) {
        this.tiempoInicio = tiempoInicio;
        this.tiempoFin = tiempoFin;
        this.tiempoEjecucion = tiempoEjecucion;
        this.duracion = duracion;
        this.topologia = topologia;
        this.numeroDeBloqueos = numeroDeBloqueos;
        this.numeroDeRutasEstablecidas = numeroDeRutasEstablecidas;
        this.numeroDeDemandasPospuestas = numeroDeDemandasPospuestas;
        this.cantidadDeDemandas = cantidadDeDemandas;
        this.k1 = k1;
        this.k2 = k2;
        this.k3 = k3;
        this.k4 = k4;
        this.k5 = k5;
        this.numeroDeBloqueosPorFragmentacion = numeroDeBloqueosPorFragmentacion;
        this.numeroDeBloqueosPorCrosstalk = numeroDeBloqueosPorCrosstalk;
        this.numeroDeBloqueosPorFragmentacionDeCamino = numeroDeBloqueosPorFragmentacionDeCamino;
        this.diametroGrafo = diametroGrafo;
        this.gradoPromedio = gradoPromedio;
        this.inputDemands = inputDemands;
        this.inputValorH = inputValorH;
        this.inputDecimal = inputDecimal;
        this.inputFsWidth = inputFsWidth;
        this.inputFsRangeMax = inputFsRangeMax;
        this.inputFsRangeMin = inputFsRangeMin;
        this.inputCapacity = inputCapacity;
        this.inputCores = inputCores;
        this.inputLambda = inputLambda;
        this.inputSimulationTime = inputSimulationTime;
        this.inputMaxCrosstalk = inputMaxCrosstalk;
        this.inputTRangeMin = inputTRangeMin;
        this.inputTRangeMax = inputTRangeMax;
        this.inputErlang = inputErlang;
        this.inputXtPerUnitLength = inputXtPerUnitLength;
    }
}

