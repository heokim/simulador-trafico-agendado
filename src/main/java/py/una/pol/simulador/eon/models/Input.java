package py.una.pol.simulador.eon.models;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;
import py.una.pol.simulador.eon.models.enums.RSAEnum;
import py.una.pol.simulador.eon.models.enums.TopologiesEnum;

/**
 * Parámetros de entrada del simulador
 *
 * @author Néstor E. Reinoso Wood
 */
@Data
public class Input {

    /**
     * Tiempo total de simulación
     */
    private Integer simulationTime;
    /**
     * Cantidad de demandas a insertar
     */
    private Integer demands;
    /**
     * Listado de topologías a simular
     */
    private List<TopologiesEnum> topologies;
    /**
     * Listado de algoritmos a utilizar
     */
    private List<RSAEnum> algorithms;
    /**
     * Ancho de banda de las ranuras de frecuencias
     */
    private BigDecimal fsWidth;
    /**
     * Capacidad total de cada enlace
     */
    private Integer capacity;
    /**
     * Volumen de tráfico
     */
    private Integer erlang;
    /**
     * Cantidad de demandas promedio a insertar por unidad de tiempo
     */
    private Integer lambda;
    /**
     * Cantidad mínima de ranuras de frecuencia que puede ocupar cada demanda
     */
    private Integer fsRangeMin;
    /**
     * Cantidad máxima de ranuras de frecuencia que puede ocupar cada demanda
     */
    private Integer fsRangeMax;
    /**
     * Cantidad de núcleos de la red
     */
    private Integer cores;
    /**
     * Nivel máximo de crosstalk tolerable
     */
    private BigDecimal maxCrosstalk;
    /**
     * Listado de Crosstalk por unidad de longitud (h) de las distintas fibras a
     * simular
     */
    private List<Double> crosstalkPerUnitLenghtList;

    /**
     * Variable para indicar que tipo de h se usa, del 1 al 3
     */
    private String numero_h;

    /**
     * Variable para indicar el factor por el cual va a multiplicarse o dividirse las distancias
     */
    private double F;

}
