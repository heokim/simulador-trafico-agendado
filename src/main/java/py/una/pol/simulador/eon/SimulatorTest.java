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
import java.util.Date;
import java.util.List;

/**
 *
 * @author Néstor E. Reinoso Wood
 */
public class SimulatorTest {

    // Contadores
    public static int CONTADOR_CROSSTALK = 0;
    public static int CONTADOR_FRAG = 0;
    public static int CONTADOR_FRAG_RUTA = 0;
    public static int DEMANDAS_POSPUESTAS = 0;
    public static int RUTAS_ESTABLECIDAS = 0;
    public static int NUMERO_BLOQUEOS = 0;

    // Configuraciones del Trafico Agendado
    public static int T_RANGE_MIN = 3;
    public static int T_RANGE_MAX = 8;

    // Configuraciones fijas del simulador
    private static int ERLANG = 10000;
    private static TopologiesEnum TOPOLOGY = TopologiesEnum.NSFNET; // NSFNET, USNET, JPNNET
    private static final String VALOR_H = "h1"; // h1, h2, h3
    private static final double XT_Per_Unit_Length = XTPerUnitLenght.H1.getValue(); // H1, H2, H3
    private static final double DECIMAL = 100; // factor f de distancia, para el grafo

    private static final int DEMANDS = 100000;
    private static final BigDecimal FS_WIDTH = new BigDecimal("12.5");
    private static final int FS_RANGE_MAX = 8;
    private static final int FS_RANGE_MIN = 2;
    private static final int CAPACITY = 325;
    private static final int CORES = 7;
    private static final int LAMBDA = 5;
    private static final BigDecimal MAX_CROSSTALK = new BigDecimal("0.003162277660168379331998893544"); // XT = -25 dB

    public static Database databaseUtil = new Database();

    /**
     * Simulador
     *
     * @param args Argumentos de entrada (Vacío)
     */
    public static void main(String[] args) throws SQLException, IOException {
        int[] erlags = new int[]{100, 500, 1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000, 15000, 20000};
        TopologiesEnum[] topologias = new TopologiesEnum[]{TopologiesEnum.NSFNET, TopologiesEnum.USNET, TopologiesEnum.JPNNET};

        for (TopologiesEnum topologia : topologias) {
            TOPOLOGY = topologia;
            for (int m = 0; m < erlags.length; m++) {
                ERLANG = erlags[m];

                // 10 simulaciones de cada tipo
                for (int n = 0; n < 10; n++) {
                    CONTADOR_CROSSTALK = 0;
                    CONTADOR_FRAG = 0;
                    CONTADOR_FRAG_RUTA = 0;
                    DEMANDAS_POSPUESTAS = 0;
                    RUTAS_ESTABLECIDAS = 0;
                    NUMERO_BLOQUEOS = 0;
                    simular();
                }
            }
        }

    }

    public static void simular() throws IOException, SQLException {
        databaseUtil.openConnection();
        // cuando tiempo tarda en ejecutar el programa completo
        long startTime = System.currentTimeMillis();
        Timestamp tiempoInicio = Timestamp.valueOf(LocalDateTime.now());

        // Volumen de Tráfico promedio (V T): representa el volumen del tráfico promedio
        // en cada instante de tiempo T dentro de la red, medido en erlangs
        // Se obtienen los datos de entrada
        Input input = new SimulatorTest().getTestingInput(ERLANG);
        // Se genera la red de acuerdo a los datos de entrada
        Graph<Integer, Link> graph = Utils.createTopology(TOPOLOGY, input.getCores(), input.getFsWidth(), input.getCapacity(), input.getF(), input.getNumero_h());
        GraphUtils.createImage(graph, TOPOLOGY.label());
        // obtengo la longitud promedio del grafo
        String longitud_promedio = calcularLongitudPromedioAristas(graph);
        // Contador de demandas utilizado para identificación
        Integer demandsQ = 1;
        List<List<Demand>> listaDemandas = new ArrayList<>();
        for (int i = 0; i < input.getSimulationTime(); i++) {
            List<Demand> demands = Utils.generateDemands(
                    input.getLambda(),
                    input.getSimulationTime(),
                    input.getFsRangeMin(),
                    input.getFsRangeMax(),
                    graph.vertexSet().size(),
                    input.getErlang() / input.getLambda(),
                    demandsQ,
                    i,
                    T_RANGE_MIN,
                    T_RANGE_MAX
            );
            demandsQ += demands.size();
            listaDemandas.add(demands);
        }

        graph = Utils.createTopology(TOPOLOGY, input.getCores(), input.getFsWidth(), input.getCapacity(), input.getF(), input.getNumero_h());
        // Lista de rutas establecidas durante la simulación
        List<EstablishedRoute> establishedRoutes = new ArrayList<>();
        System.out.println("Inicializando simulación para erlang: " + (ERLANG) + " para la topología " + TOPOLOGY.label() + " y H = " + XT_Per_Unit_Length);
        int demandaNumero = 0;
        Integer camino = null;
        //Declaro las variables auxiliares para verificar el camino tomado
        Integer k1 = 0, k2 = 0, k3 = 0, k4 = 0, k5 = 0;

        // Diametro del grafo
        Integer Diametro = 0;
        // Variables para calcular el promedio del grado del grafo
        int prom_grado = 0; //valor promedio del grado del grafo
        int grado_grafo = 0; //grado del grafo
        for (int vertex = 0; vertex < graph.vertexSet().size(); vertex++) {
            grado_grafo = grado_grafo + graph.degreeOf(vertex);
        }
        prom_grado = (grado_grafo / graph.vertexSet().size());

        // Iteración de unidades de tiempo
        for (int t = 0; t < input.getSimulationTime(); t++) {
            // System.out.println("Tiempo: " + t);
            // Generación de demandas para la unidad de tiempo
            List<Demand> demands = listaDemandas.get(t);
            for (Demand demand : demands) {
                demandaNumero++;
                // k caminos más cortos entre source y destination de la demanda actual
                EstablishedRoute establishedRoute = Algorithms.ruteoCoreMultipleAgendado(graph, demand, input.getCapacity(), input.getCores(), input.getMaxCrosstalk(), XT_Per_Unit_Length);
                if (establishedRoute == null || establishedRoute.getFsIndexBegin() == -1) {
                    // TODO: se puede seguir probando instalar en el siguiente tiempo?
                    if (demand.getTe() > t) {
                        if (listaDemandas.size() > t + 1) {
                            // Priorizamos las demandas que no se pududieron instalar en su Ts
                            listaDemandas.get(t + 1).add(0, demand);
                            // como la demanda no se instalo, no se cuenta
                            demandaNumero--;
                        }
                        DEMANDAS_POSPUESTAS++;
                    } else if (demand.getTe() == t) {
                        // nunca se puedo instalar entre el Ts y Te de la demanda
                        // Bloqueo
                        databaseUtil.insertarBloqueo(TOPOLOGY.label(), "" + t, "" + demand.getId(), "" + ERLANG, String.valueOf(XT_Per_Unit_Length));
                        NUMERO_BLOQUEOS++;
                    }
                } else {
                    camino = establishedRoute.getK_elegido();
                    switch (camino) {
                        case 0 -> k1++;
                        case 1 -> k2++;
                        case 2 -> k3++;
                        case 3 -> k4++;
                        default -> k5++;
                    }

                    // va buscando y guardando el diametro mayor entre las rutas.
                    if (establishedRoute.getDiametro() > Diametro)
                        Diametro = establishedRoute.getDiametro();

                    RUTAS_ESTABLECIDAS++;
                    AssignFsResponse response = Utils.assignFs(graph, establishedRoute, XT_Per_Unit_Length);
                    establishedRoute = response.getRoute();
                    graph = response.getGraph();
                    establishedRoutes.add(establishedRoute);
                }
            }
            for (EstablishedRoute route : establishedRoutes) {
                route.subLifeTime();
            }
            // Verifica las rutas establecidas y elimina las que ya expiraron
            for (int ri = 0; ri < establishedRoutes.size(); ri++) {
                EstablishedRoute route = establishedRoutes.get(ri);
                if (route.getLifetime().equals(0)) {
                    Utils.deallocateFs(graph, route, XT_Per_Unit_Length);
                    establishedRoutes.remove(ri);
                    ri--;
                }
            }
        }
        //Determina los datos para ingresar a la base de datos
        // los motivos de bloqueos
        String motivo_bloqueo = MotivoBloqueo(CONTADOR_FRAG, CONTADOR_CROSSTALK);
        String porcentaje_motivo = PorcentajeMotivo(NUMERO_BLOQUEOS, CONTADOR_FRAG, CONTADOR_CROSSTALK);
        String porcentaje = PorcentajeBloqueo(demandaNumero, NUMERO_BLOQUEOS);
        String tipo_erlang = TipoErlang(porcentaje);

        System.out.println("---------------------------------");
        System.out.println("\nTopologia" + input.getTopologies() + "\n");
        System.out.println("TOTAL DE BLOQUEOS: " + NUMERO_BLOQUEOS);
        System.out.println("TOTAL DE RUTAS ESTABLESIDAS: " + RUTAS_ESTABLECIDAS);
        System.out.println("TOTAL DE DEMANDA POSPUESTA: " + DEMANDAS_POSPUESTAS);
        System.out.println("Cantidad de demandas: " + demandaNumero);
        System.out.println("\nRESUMEN DE DATOS \n");
        System.out.printf("Resumen de caminos:\nk1:%d\nk2:%d\nk3:%d\nk4:%d\nk5:%d\n", k1, k2, k3, k4, k5);
        System.out.printf("Resumen de bloqueos:\n fragmentacion = %d \n crosstalk = %d\n fragmentacion de camino = %d\n", CONTADOR_FRAG, CONTADOR_CROSSTALK, CONTADOR_FRAG_RUTA);
        System.out.printf("\nEl diametro del grafo es:  %d kms\n", Diametro);
        System.out.printf("\nEl grado promedio: %d", prom_grado);

        // fin programa
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        System.out.println("Tiempo de ejecución: " + duration / 1000 + " segundos");
        long hours = duration / 3600000;
        long minutes = (duration % 3600000) / 60000;
        long seconds = (duration % 60000) / 1000;
        System.out.println("Tiempo de ejecución: " + hours + " horas, " + minutes + " minutos y " + seconds + " segundos");

        SimulacionResumen resumen = new SimulacionResumen(
                tiempoInicio, Timestamp.valueOf(LocalDateTime.now()), duration,
                (hours + " horas, " + minutes + " minutos y " + seconds + " segundos"),
                TOPOLOGY.label(),
                NUMERO_BLOQUEOS, RUTAS_ESTABLECIDAS, DEMANDAS_POSPUESTAS,
                demandaNumero,
                k1, k2, k3, k4, k5,
                CONTADOR_FRAG, CONTADOR_CROSSTALK, CONTADOR_FRAG_RUTA,
                Diametro, prom_grado,
                DEMANDS, VALOR_H, BigDecimal.valueOf(DECIMAL), FS_WIDTH, FS_RANGE_MAX, FS_RANGE_MIN,
                CAPACITY, CORES, LAMBDA, input.getSimulationTime(),
                MAX_CROSSTALK, T_RANGE_MIN, T_RANGE_MAX, ERLANG,
                BigDecimal.valueOf(XT_Per_Unit_Length)
        );

        databaseUtil.insertSimulacionResumen(resumen);
        databaseUtil.closeConnection();
    }

    /**
     * Configuración inicial para el simulador
     *
     * @param erlang Erlang para la simulación
     * @return Datos de entrada del simulador
     */
    private Input getTestingInput(Integer erlang) {
        Input input = new Input();
        input.setTopologies(List.of(TOPOLOGY));
        input.setNumero_h(VALOR_H);
        input.setF(DECIMAL);
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
        input.setMaxCrosstalk(MAX_CROSSTALK);// XT = -25 dB
        return input;
    }

    /**
     * Funcion que retorna el motivo de fragmentacion de la red
     *
     * @param contador1 es el contador de cantidades de bloqueos por fragmentacion
     * @param contador2 es el contador de cantidades de bloqueos por crosstalk
     * @return Motivo de fragmentacion de la red
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
     * Funcion que devuelve el motivo de bloqueo
     *
     * @param bloqueos  Cantidad de bloqueos de la red
     * @param contador1 Cantidad de bloqueos por fragmentacion en la red
     * @param contador2 Cantidad de bloqueos por crosstalk en la red
     * @return el porcentaje de bloqueo.
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
     * Funcion que devuelve el porcentaje de bloqueo de la red
     *
     * @param demandas cantidad de demandas de la red
     * @param bloqueos porcentaje de bloqueos de la red
     * @return el porcentaje de bloqueo.
     */
    public static String PorcentajeBloqueo(int demandas, int bloqueos) {
        double porcentaje = (double) bloqueos * 100 / demandas;
        return String.format("%.2f%%", porcentaje);
    }

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
     * Funcion que devuelve la longitud media del grafo
     *
     * @param grafo es el grafo utilizado como red
     * @return longitud media del grafo
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
        // Formatear a 2 decimales como String
        return String.format("%.2f", promedio);
    }

}

