package py.una.pol.simulador.eon;

import org.jgrapht.Graph;
import py.una.pol.simulador.eon.models.*;
import py.una.pol.simulador.eon.models.enums.RSAEnum;
import py.una.pol.simulador.eon.models.enums.TopologiesEnum;
import py.una.pol.simulador.eon.models.enums.XTPerUnitLenght;
import py.una.pol.simulador.eon.rsa.Algorithms;
import py.una.pol.simulador.eon.utils.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author Néstor E. Reinoso Wood
 */
public class SimulatorTest {

    // Contadores globales de la simulacion. Algorithms incrementa algunos de estos valores
    // cuando no puede asignar una ruta por fragmentacion, crosstalk o fragmentacion de camino.
    public static int CONTADOR_CROSSTALK = 0;
    public static int CONTADOR_FRAG = 0;
    public static int CONTADOR_FRAG_RUTA = 0;
    public static int DEMANDAS_POSPUESTAS = 0;
    public static int CANTIDAD_POSPUESTAS = 0;
    public static int CANTIDAD_POSPUESTAS_MAX = 0;
    public static int RUTAS_ESTABLECIDAS = 0;
    public static int NUMERO_BLOQUEOS = 0;

    // Configuraciones del Trafico Agendado.
    // Si ambos valores son 0, las demandas se comportan como trafico dinamico.
    // Si tienen rango, representan la ventana [Ts, Te] para instalar demandas agendadas.
    public static int T_RANGE_MIN = 0;
    public static int T_RANGE_MAX = 0;

    public static String DESCRIPCION = "";

    // Configuraciones fijas del simulador
    private static int ERLANG = 0;
    public static int ERLANG_BAJO = 0;
    public static int ERLANG_MEDIO = 0;
    public static int ERLANG_ALTO = 0;
    private static TopologiesEnum TOPOLOGY = TopologiesEnum.NSFNET; // NSFNET, USNET, JPNNET
    private static String VALOR_H = "h2"; // h1, h2, h3
    private static double XT_Per_Unit_Length = XTPerUnitLenght.H2.getValue(); // H1, H2, H3

    private static final int DEMANDS = 250000;
    private static final BigDecimal FS_WIDTH = new BigDecimal("12.5");
    private static final int FS_RANGE_MIN = 2;
    private static final int FS_RANGE_MAX = 8;
    private static final int CAPACITY = 325;
    private static final int CORES = 7;
    private static final int LAMBDA = 5;
    private static final BigDecimal MAX_CROSSTALK = new BigDecimal("0.003162277660168379331998893544"); // XT = -25 dB

    public static Database databaseUtil = new Database();

    /**
     * Punto de entrada del simulador. Aqui se configuran los parametros del caso de estudio
     * y se elige el tipo de simulacion a ejecutar.
     *
     * @param args Argumentos de entrada (vacio)
     */
    public static void main(String[] args) throws SQLException, IOException {
        TOPOLOGY = TopologiesEnum.USNET;
        T_RANGE_MIN = 0;
        T_RANGE_MAX = 0;

//      Basico
//        ERLANG = 1400;
//        for (int i = 0; i < 10; i++) {
//            simular();
//        }

        DESCRIPCION = "Dinamico, erlang bajo, medio y alto";
        VALOR_H = "h1"; // h1, h2, h3
        XT_Per_Unit_Length = XTPerUnitLenght.H1.getValue(); // H1, H2, H3
        ERLANG_BAJO = 1000;
        ERLANG_MEDIO = 1400;
        ERLANG_ALTO = 1800;
        simulacionErlangVariable();

        DESCRIPCION = "Dinamico, erlang bajo, medio y alto";
        VALOR_H = "h2"; // h1, h2, h3
        XT_Per_Unit_Length = XTPerUnitLenght.H2.getValue(); // H1, H2, H3
        ERLANG_BAJO = 2450;
        ERLANG_MEDIO = 3400;
        ERLANG_ALTO = 4200;
        simulacionErlangVariable();

        DESCRIPCION = "Dinamico, erlang bajo, medio y alto";
        VALOR_H = "h3"; // h1, h2, h3
        XT_Per_Unit_Length = XTPerUnitLenght.H3.getValue(); // H1, H2, H3
        ERLANG_BAJO = 3250;
        ERLANG_MEDIO = 3550;
        ERLANG_ALTO = 4600;
        simulacionErlangVariable();

        generarSonidoNotificacion(2);
    }

    /**
     * Ejecuta la simulacion con Erlang fijo.
     * Todas las unidades de tiempo usan el valor configurado en ERLANG.
     */
    public static double simular() throws IOException, SQLException {
        return ejecutarSimulacion(null);
    }

    /**
     * Ejecuta la simulacion con Erlang variable en 5 franjas:
     * bajo, medio, alto, medio y bajo.
     */
    public static double simulacionErlangVariable() throws IOException, SQLException {
        DynamicErlangDistribution distribution = new DynamicErlangDistribution(ERLANG_BAJO, ERLANG_MEDIO, ERLANG_ALTO);
        return ejecutarSimulacion(distribution);
    }

    /**
     * Implementacion comun para ambos modos de simulacion.
     * Si distribution es null se usa ERLANG fijo; si no, cada unidad de tiempo consulta
     * la distribucion para obtener el Erlang correspondiente a esa franja.
     */
    private static double ejecutarSimulacion(IErlangDistribution distribution) throws IOException, SQLException {

        CONTADOR_CROSSTALK = 0;
        CONTADOR_FRAG = 0;
        CONTADOR_FRAG_RUTA = 0;
        DEMANDAS_POSPUESTAS = 0;
        RUTAS_ESTABLECIDAS = 0;
        NUMERO_BLOQUEOS = 0;
        CANTIDAD_POSPUESTAS = 0;
        CANTIDAD_POSPUESTAS_MAX = 0;

        System.out.println("Inicializando simulación para erlang: " + (ERLANG) + " para la topología " + TOPOLOGY.label() + " y Fibra = " + VALOR_H);
        System.out.println("Descripción: " + DESCRIPCION);

        databaseUtil.openConnection();
        long simulacionId = databaseUtil.obtenerIdSimulacion() + 1;
        long startTime = System.currentTimeMillis();
        Timestamp tiempoInicio = Timestamp.valueOf(LocalDateTime.now());

        // Datos de entrada comunes a toda la simulacion.
        Input input = new SimulatorTest().getTestingInput(ERLANG);

        // Se crea una primera topologia para generar demandas y graficar la red.
        Graph<Integer, Link> graph = Utils.createTopology(TOPOLOGY, input.getCores(), input.getFsWidth(), input.getCapacity(), input.getNumero_h());
        GraphUtils.createImage(graph, TOPOLOGY.label());

        // Longitud promedio disponible para resumenes antiguos o analisis manual.
        String longitud_promedio = calcularLongitudPromedioAristas(graph);

        // Contador incremental usado como id inicial al generar demandas.
        Integer demandsQ = 1;
        List<List<Demand>> listaDemandas = new ArrayList<>();
        System.out.println("Unidades de tiempo a simular: " + input.getSimulationTime());

        boolean erlangVariable = distribution != null;
        double[] xTime = erlangVariable ? new double[input.getSimulationTime()] : null;
        double[] yErlang = erlangVariable ? new double[input.getSimulationTime()] : null;
        double[] yErlangReal = erlangVariable ? new double[input.getSimulationTime()] : null;
        double[] yBloqueosAcum = new double[input.getSimulationTime()];

        // Generacion previa de demandas por unidad de tiempo.
        // En modo variable, currentErlang cambia segun la franja; en modo fijo, usa input.getErlang().
        for (int i = 0; i < input.getSimulationTime(); i++) {
            int currentErlang = erlangVariable
                    ? distribution.getErlang(i, input.getSimulationTime(), input.getErlang())
                    : input.getErlang();
            if (erlangVariable) {
                xTime[i] = i;
                yErlang[i] = currentErlang;
            }

            List<Demand> demands = Utils.generateDemands(
                    input.getLambda(),
                    input.getSimulationTime(),
                    input.getFsRangeMin(),
                    input.getFsRangeMax(),
                    graph.vertexSet().size(),
                    currentErlang / input.getLambda(),
                    demandsQ,
                    i,
                    T_RANGE_MIN,
                    T_RANGE_MAX
            );
            if (erlangVariable) {
                // Erlang real generado en este tiempo: suma de los tiempos de vida de las demandas creadas.
                // Puede diferir del Erlang objetivo porque la cantidad de demandas usa Poisson y el lifetime es aleatorio.
                yErlangReal[i] = demands.stream().mapToInt(Demand::getLifetime).sum();
            }
            demandsQ += demands.size();
            listaDemandas.add(demands);
        }

        // Se recrea la topologia para iniciar la simulacion con la red libre de asignaciones.
        graph = Utils.createTopology(TOPOLOGY, input.getCores(), input.getFsWidth(), input.getCapacity(), input.getNumero_h());

        // Rutas actualmente establecidas. En cada unidad de tiempo se reduce su vida y se liberan si expiran.
        List<EstablishedRoute> establishedRoutes = new ArrayList<>();
        int demandaNumero = 0;
        Integer camino = null;

        // Contadores para registrar que k-camino fue elegido por el algoritmo de ruteo.
        Integer k1 = 0, k2 = 0, k3 = 0, k4 = 0, k5 = 0;

        // Diametro maximo observado en las rutas establecidas durante la simulacion.
        Integer Diametro = 0;

        // Grado promedio de la topologia: suma de grados de vertices / cantidad de vertices.
        int prom_grado = 0;
        int grado_grafo = 0;
        for (int vertex = 0; vertex < graph.vertexSet().size(); vertex++) {
            grado_grafo = grado_grafo + graph.degreeOf(vertex);
        }
        prom_grado = (grado_grafo / graph.vertexSet().size());

        // Iteracion principal: procesa las demandas generadas para cada unidad de tiempo.
        for (int t = 0; t < input.getSimulationTime(); t++) {
            List<Demand> demands = listaDemandas.get(t);

            // Demandas pospuestas son las que se intentan instalar despues de su Ts original.
            final int tiempoActual = t;
            long pospuestas = demands.stream().filter(d -> tiempoActual > d.getTs()).count();
            CANTIDAD_POSPUESTAS += pospuestas;
            if (pospuestas > CANTIDAD_POSPUESTAS_MAX) {
                CANTIDAD_POSPUESTAS_MAX = (int) pospuestas;
            }

            for (Demand demand : demands) {
                demandaNumero++;

                // Intenta encontrar ruta, nucleo y slots para la demanda actual.
                EstablishedRoute establishedRoute = Algorithms.ruteoCoreMultipleAgendadoFixed(graph, demand, input.getCapacity(), input.getCores(), input.getMaxCrosstalk(), XT_Per_Unit_Length);
                if (establishedRoute == null || establishedRoute.getFsIndexBegin() == -1) {
                    if (demand.getTe() > t) {
                        if (listaDemandas.size() > t + 1) {
                            // Todavia hay margen hasta Te: se reintenta en el siguiente tiempo y se prioriza al inicio.
                            listaDemandas.get(t + 1).add(0, demand);
                            demandaNumero--;
                            demand.setCantPospuesto(demand.getCantPospuesto() + 1);
                        }

                    } else if (demand.getTe() == t) {
                        // Llego al limite Te sin poder instalarse: la demanda queda bloqueada definitivamente.
                        databaseUtil.insertarBloqueo(TOPOLOGY.label(), "" + t, "" + demand.getId(), "" + ERLANG, String.valueOf(XT_Per_Unit_Length));
                        NUMERO_BLOQUEOS++;

                        demand.setBlocked(true);
                        demand.setSimulacionId(simulacionId);
                        databaseUtil.insertDemand(demand);
                        DEMANDAS_POSPUESTAS++;
                    }
                } else {
                    if (demand.getCantPospuesto() > 0) DEMANDAS_POSPUESTAS++;
                    camino = establishedRoute.getK_elegido();

                    switch (camino) {
                        case 0 -> k1++;
                        case 1 -> k2++;
                        case 2 -> k3++;
                        case 3 -> k4++;
                        default -> k5++;
                    }

                    if (establishedRoute.getDiametro() > Diametro)
                        Diametro = establishedRoute.getDiametro();

                    RUTAS_ESTABLECIDAS++;

                    // Reserva los FS en la red y guarda la ruta para liberar recursos cuando expire.
                    AssignFsResponse response = Utils.assignFs(graph, establishedRoute, XT_Per_Unit_Length);
                    establishedRoute = response.getRoute();
                    graph = response.getGraph();
                    establishedRoutes.add(establishedRoute);

                    demand.setBlocked(false);
                    demand.setTiempoInstalacion(t);
                    demand.setSimulacionId(simulacionId);
                    databaseUtil.insertDemand(demand);
                }
            }

            // Avanza una unidad de vida en todas las rutas establecidas.
            for (EstablishedRoute route : establishedRoutes) {
                route.subLifeTime();
            }

            // Libera slots de las rutas cuyo lifetime llego a 0.
            for (int ri = 0; ri < establishedRoutes.size(); ri++) {
                EstablishedRoute route = establishedRoutes.get(ri);
                if (route.getLifetime().equals(0)) {
                    Utils.deallocateFs(graph, route, XT_Per_Unit_Length);
                    establishedRoutes.remove(ri);
                    ri--;
                }
            }

            // Guarda el porcentaje de bloqueo acumulado en este tiempo para el grafico de Erlang variable.
            double pocentajeT = 0.0;
            if (demandaNumero > 0) {
                pocentajeT = ((double) NUMERO_BLOQUEOS * 100.0) / demandaNumero;
            }
            yBloqueosAcum[t] = pocentajeT;
        }

        // Datos derivados para persistir el resumen final de la simulacion.
        String motivo_bloqueo = MotivoBloqueo(CONTADOR_FRAG, CONTADOR_CROSSTALK);
        String porcentaje_motivo = PorcentajeMotivo(NUMERO_BLOQUEOS, CONTADOR_FRAG, CONTADOR_CROSSTALK);
        String porcentaje = PorcentajeBloqueo(demandaNumero, NUMERO_BLOQUEOS);
        String tipo_erlang = TipoErlang(porcentaje);
        double promCantPospuetasEnUnTiempo = (double) CANTIDAD_POSPUESTAS / (double) input.getSimulationTime();

        System.out.println("---------------------------------");
        System.out.println("\nTopologia" + input.getTopologies() + "\n");
        System.out.println("TOTAL DE BLOQUEOS: " + NUMERO_BLOQUEOS);
        System.out.println("TOTAL DE RUTAS ESTABLECIDAS: " + RUTAS_ESTABLECIDAS);
        System.out.println("TOTAL DE DEMANDA POSPUESTA: " + DEMANDAS_POSPUESTAS);
        System.out.println("Cantidad de demandas: " + demandaNumero);
        System.out.println("Cantidad de pospuestas: " + CANTIDAD_POSPUESTAS);
        System.out.println("cant. pospuestas por unidad de tiempo MAX: " + CANTIDAD_POSPUESTAS_MAX);
        System.out.println("Promedio de cant. pospuestas por unidad de tiempo: " + promCantPospuetasEnUnTiempo);
        System.out.println("\nRESUMEN DE DATOS \n");
        System.out.printf("Resumen de caminos:\nk1:%d\nk2:%d\nk3:%d\nk4:%d\nk5:%d\n", k1, k2, k3, k4, k5);
        System.out.printf("Resumen de bloqueos:\n fragmentacion = %d \n crosstalk = %d\n fragmentacion de camino = %d\n", CONTADOR_FRAG, CONTADOR_CROSSTALK, CONTADOR_FRAG_RUTA);
        System.out.printf("\nEl diametro del grafo es:  %d kms\n", Diametro);
        System.out.printf("\nEl grado promedio: %d\n", prom_grado);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        System.out.println("Tiempo de ejecución: " + duration / 1000 + " segundos");
        long hours = duration / 3600000;
        long minutes = (duration % 3600000) / 60000;
        long seconds = (duration % 60000) / 1000;
        System.out.println("Tiempo de ejecución: " + hours + " horas, " + minutes + " minutos y " + seconds + " segundos");

        SimulacionResumen resumen = new SimulacionResumen(
                tiempoInicio, Timestamp.valueOf(LocalDateTime.now()), duration / 1000,
                (hours + " horas, " + minutes + " minutos y " + seconds + " segundos"),
                TOPOLOGY.label(),
                NUMERO_BLOQUEOS, RUTAS_ESTABLECIDAS, DEMANDAS_POSPUESTAS,
                demandaNumero,
                k1, k2, k3, k4, k5,
                CONTADOR_FRAG, CONTADOR_CROSSTALK, CONTADOR_FRAG_RUTA,
                Diametro, prom_grado,
                DEMANDS, VALOR_H, FS_WIDTH, FS_RANGE_MAX, FS_RANGE_MIN,
                CAPACITY, CORES, LAMBDA, input.getSimulationTime(),
                MAX_CROSSTALK, T_RANGE_MIN, T_RANGE_MAX, ERLANG,
                BigDecimal.valueOf(XT_Per_Unit_Length),
                motivo_bloqueo, porcentaje_motivo, porcentaje, tipo_erlang,
                CANTIDAD_POSPUESTAS_MAX, promCantPospuetasEnUnTiempo, DESCRIPCION
        );

        databaseUtil.insertSimulacionResumen(resumen);
        databaseUtil.closeConnection();

        // El grafico se genera solo para Erlang variable, porque muestra la curva de carga por franjas.
        if (erlangVariable) {
            try {
                String fileName = "erlang_vs_tiempo_" + simulacionId + ".png";
                GraphAnalyticsUtils.guardarGraficoErlang(
                    xTime, yErlang, yErlangReal, yBloqueosAcum,
                    input.getSimulationTime(),
                    fileName,
                    TOPOLOGY.label(),
                    VALOR_H
                );
                System.out.println("Gráfico guardado en: " + fileName);
            } catch(Exception e) {
                System.err.println("Error generando gráfico JFreeChart: " + e.getMessage());
            }
        }

        // Retorna el porcentaje de bloqueo como numero para comparaciones automaticas.
        porcentaje = porcentaje.replace(",", ".").replace("%", "").trim();
        Double valor = Double.parseDouble(porcentaje);
        System.out.println("Porcentaje de bloqueo: " + porcentaje);
        return valor;
    }

    /**
     * Configuracion inicial para el simulador.
     *
     * @param erlang Erlang base para la simulacion. En modo variable se usa como referencia,
     *               pero cada unidad de tiempo puede usar ERLANG_BAJO, ERLANG_MEDIO o ERLANG_ALTO.
     * @return Datos de entrada del simulador
     */
    private Input getTestingInput(Integer erlang) {
        Input input = new Input();
        input.setTopologies(List.of(TOPOLOGY));
        input.setNumero_h(VALOR_H);
        input.setDemands(DEMANDS);
        input.setFsWidth(FS_WIDTH);
        input.setFsRangeMax(FS_RANGE_MAX);
        input.setFsRangeMin(FS_RANGE_MIN);
        input.setCapacity(CAPACITY);
        input.setCores(CORES);
        input.setLambda(LAMBDA);
        input.setErlang(erlang);
        input.setAlgorithms(List.of(RSAEnum.MULTIPLES_CORES));
        input.setSimulationTime(MathUtils.getSimulationTime(DEMANDS, LAMBDA));
        input.setMaxCrosstalk(MAX_CROSSTALK); // XT = -25 dB
        return input;
    }

    /**
     * Retorna el motivo predominante de bloqueo segun los contadores del algoritmo RSA.
     *
     * @param contador1 cantidad de bloqueos por fragmentacion
     * @param contador2 cantidad de bloqueos por crosstalk
     * @return Motivo de bloqueo de la red
     */
    public static String MotivoBloqueo(int contador1, int contador2) {
        String motivo_bloqueo;
        if (contador1 > 0 && contador2 == 0) {
            motivo_bloqueo = "Fragmentacion";
        } else if (contador1 == 0 && contador2 > 0) {
            motivo_bloqueo = "Crosstalk";
        } else if (contador1 == 0 && contador2 == 0) {
            motivo_bloqueo = " ";
        } else {
            motivo_bloqueo = "Crosstalk y Fragmentacion";
        }
        return motivo_bloqueo;
    }

    /**
     * Calcula el porcentaje asociado al motivo de bloqueo.
     *
     * @param bloqueos  cantidad total de bloqueos
     * @param contador1 cantidad de bloqueos por fragmentacion
     * @param contador2 cantidad de bloqueos por crosstalk
     * @return Porcentaje del motivo de bloqueo
     */
    public static String PorcentajeMotivo(int bloqueos, int contador1, int contador2) {
        String porcentaje = "";
        float p_frag, p_crosstalk;
        if (bloqueos == 0) {
            return "Sin fragmentacion ni crosstalk";
        }

        if (contador1 == 0 || contador2 == 0) {
            porcentaje = "100";
        } else if (contador1 > 0 && contador2 > 0) {
            p_frag = (contador1 * 100) / bloqueos;
            p_crosstalk = (contador2 * 100) / bloqueos;
            porcentaje = "" + p_frag + " fragmentacion " + " y " + p_crosstalk + " crosstalk";
        }
        return porcentaje;
    }

    /**
     * Calcula el porcentaje de bloqueo total de la red.
     *
     * @param demandas cantidad de demandas procesadas
     * @param bloqueos cantidad de demandas bloqueadas
     * @return Porcentaje de bloqueo formateado
     */
    public static String PorcentajeBloqueo(int demandas, int bloqueos) {
        double porcentaje = (double) bloqueos * 100 / demandas;
        return String.format("%.2f%%", porcentaje);
    }

    /**
     * Clasifica el nivel de Erlang segun el porcentaje de bloqueo observado.
     */
    public static String TipoErlang(String porcentaje) {
        String tipo_erlang;
        porcentaje = porcentaje.replace(",", ".").replace("%", "").trim();
        Double valor = Double.parseDouble(porcentaje);
        if (valor <= 1.5) {
            tipo_erlang = "BAJO";
        } else if (valor <= 5.5) {
            tipo_erlang = "MEDIO";
        } else {
            tipo_erlang = "ALTO";
        }
        return tipo_erlang;
    }

    /**
     * Calcula la longitud promedio de las aristas de la topologia.
     *
     * @param grafo red utilizada en la simulacion
     * @return Longitud promedio formateada a dos decimales
     */
    public static String calcularLongitudPromedioAristas(Graph<Integer, Link> grafo) {
        if (grafo.edgeSet().isEmpty()) {
            return "0.00";
        }

        double sumaTotal = 0.0;

        for (Link arista : grafo.edgeSet()) {
            sumaTotal += grafo.getEdgeWeight(arista);
        }

        double promedio = sumaTotal / grafo.edgeSet().size();
        return String.format("%.2f", promedio);
    }

    /**
     * Emite una notificacion sonora al finalizar lotes largos de simulacion.
     */
    public static void generarSonidoNotificacion(int n) {
        for (int i = 0; i < n; i++) {
            java.awt.Toolkit.getDefaultToolkit().beep();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
