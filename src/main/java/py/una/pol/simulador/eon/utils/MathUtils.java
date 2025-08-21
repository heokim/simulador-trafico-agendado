/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package py.una.pol.simulador.eon.utils;

/**
 *
 * @author Néstor E. Reinoso Wood
 */
public class MathUtils {

    /**
     * Formula para la distribución de poisson
     *
     * @param lambda Cantidad de demandas promedio a insertar por unidad de
     * tiempo
     * @return Valor calculado
     */
    public static Integer poisson(Integer lambda) {
        Integer b, bFact;
        Double s, a;
        Double e = Math.E;
        a = (Math.random() * 1) + 0;
        b = 0;
        bFact = factorial(b);
        s = (Math.pow(e, (-lambda))) * ((Math.pow(lambda, b)) / (bFact));
        while (a > s) {
            b++;
            bFact = factorial(b);
            s = s + ((Math.pow(e, (-lambda))) * ((Math.pow(lambda, b)) / (bFact)));
        }
        return b;
    }

    /**
     * Calcula el tiempo de vida de la demanda
     *
     * @param ht Erlang/Lambda
     * @return Tiempo de vida calculado
     */
    public static Integer getLifetime(Integer ht) {
        Double b;
        Double s, a, aux, auxB, auxHT;
        Double e = Math.E;
        a = (Math.random() * 1) + 0;
        b = 1D;
        auxB = (double) b;
        auxHT = (double) ht;
        aux = (-1) * (auxB / auxHT);
        s = 1 - (Math.pow(e, (aux)));
        while (s < a) {
            b++;
            auxB = b;
            aux = (-1) * (auxB / auxHT);
            s = 1 - (Math.pow(e, (aux)));
        }
        return b.intValue();
    }

    /**
     * Calcula el factorial de un número
     *
     * @param n Número de entrada
     * @return Factorial de n
     */
    public static Integer factorial(Integer n) {
        Integer resultado = 1;
        for (Integer i = 1; i <= n; i++) {
            resultado *= i;
        }
        return resultado;
    }

    /**
     * Obtiene el tiempo de simulación en base a la cantidad de demandas total y
     * la cantidad de demandas promedio a insertar por unidad de tiempo
     *
     * @param demands Cantidad de demandas total
     * @param lambda Cantidad de demandas promedio a insertar
     * @return Tiempo de simulación
     */
    public static Integer getSimulationTime(Integer demands, Integer lambda) {
        Integer simulationTime = demands / lambda;
        return simulationTime;
    }
}
