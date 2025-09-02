package py.una.pol.simulador.eon.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleWeightedGraph;

import py.una.pol.simulador.eon.models.AssignFsResponse;
import py.una.pol.simulador.eon.models.Core;
import py.una.pol.simulador.eon.models.Demand;
import py.una.pol.simulador.eon.models.EstablishedRoute;
import py.una.pol.simulador.eon.models.Link;
import py.una.pol.simulador.eon.models.enums.TopologiesEnum;

/**
 * Utilerías generales
 *
 * @author Néstor E. Reinoso Wood
 */
public class Utils {

    /**
     * Creates the graph that represents the optical network
     *
     * @param topology      Topology selected for the network
     * @param numberOfCores Quantity of cores in each link
     * @param fsWidth       Width of the frequency slots
     * @param capacity      Quantity of frequency slots in a core
     * @return Graph that represents the optical network
     * @throws IOException              Error de I/O
     * @throws IllegalArgumentException Parámetros no válidos
     */
    public static Graph<Integer, Link> createTopology(TopologiesEnum topology, int numberOfCores,
                                                      BigDecimal fsWidth, Integer capacity, double factor, String h)
            throws IOException, IllegalArgumentException {
        ObjectMapper objectMapper = new ObjectMapper();
        Graph<Integer, Link> g = new SimpleWeightedGraph<>(Link.class);
        InputStream is = ResourceReader.getFileFromResourceAsStream(topology.filePath());
        JsonNode object = objectMapper.readTree(is);
        for (int i = 0; i < object.get("network").size(); i++) {
            g.addVertex(i);
        }
        int vertex = 0;
        for (JsonNode node : object.get("network")) {
            for (int i = 0; i < node.get("connections").size(); i++) {
                int connection = node.get("connections").get(i).intValue();
                int distance = node.get("distance").get(i).intValue();
                // agrego el factor F que multiplique a las distancias de los enlaces
                //dependiendo del valor de h cambia para que aumente o disminuya
                if ("h3".equals(h)) {
                    distance = (int) Math.ceil(distance * factor);
                } else {
                    distance = (int) Math.ceil(distance / factor);
                }
                List<Core> cores = new ArrayList<>();
                for (int j = 0; j < numberOfCores; j++) {
                    Core core = new Core(fsWidth, capacity);
                    cores.add(core);
                }
                Link link = new Link(distance, cores, vertex, connection);
                g.addEdge(vertex, connection, link);
                g.setEdgeWeight(link, distance);
            }
            vertex++;
        }
        return g;
    }

    /**
     * Genera una lista de demandas en base a los argumentos de entrada
     *
     * @param lambda    Cantidad de demandas a insertar por unidad de tiempo
     * @param totalTime Tiempo total de simulación
     * @param fsMin     Cantidad mínima de ranuras que puede ocupar una demanda
     * @param fsMax     Cantidad máxima de ranuras que puede ocupar una demanda
     * @param cantNodos Cantidad de nodos de la red
     * @param HT        Erlang/Lambda
     * @param demandId  Identificador de la última demanda generada
     * @param ts        Tiempo inicial para instalar la demanda; Tiempo en el que se generan las demandas
     * @param tMin      Tiempo mínimo para instalar la demanda
     * @param tMax      Tiempo máximo para instalar la demanda
     * @return Lista de demandas generadas
     */
    public static List<Demand> generateDemands(Integer lambda, Integer totalTime,
                                               Integer fsMin, Integer fsMax, Integer cantNodos, Integer HT, Integer demandId, Integer ts, Integer tMin, Integer tMax) {
        List<Demand> demands = new ArrayList<>();
        Random rand;
        Integer demandasQuantity = MathUtils.poisson(lambda);
        for (Integer j = demandId; j < demandasQuantity + demandId; j++) {
            rand = new Random();
            Integer source = rand.nextInt(cantNodos);
            Integer destination = rand.nextInt(cantNodos);
            Integer fs = (int) (Math.random() * (fsMax - fsMin + 1)) + fsMin;
            while (source.equals(destination)) {
                destination = rand.nextInt(cantNodos);
            }
            Integer tLife = MathUtils.getLifetime(HT); // TODO: Mofificar a futuro
            Integer te = ts + (int) (Math.random() * (tMax - tMin + 1)) + tMin;
            demands.add(new Demand(j, source, destination, fs, tLife * 2, false, ts, te));
        }
        return demands;
    }

    /**
     * Calcula el valor de Crosstalk en un núcleo
     *
     * @param n Número de cores vecinos
     * @param h Crosstalk por Unidad de Longitud
     * @param L Longitud del enlace
     * @return Crosstalk
     */
    public static double XT(Integer n, double h, int L) {
        double XT = 0;
        /* le agrego el menor o igual , porque se tiene que ejecutar una vez
        y luego si en los vecinos hay crosstal , se le debe sumar la cantidad de vecinos
        donde se presenta crosstalk en las mismas ranuras*/
        for (int i = 0; i <= n; i++) {
            XT = XT + (h * (L * 1000));
        }
        return XT;
    }

    /**
     * Calcula la cantidad de nucleos adyacentes para un núcleo en una red de 7
     * núcleos
     *
     * @param core Núcleo a utilizar para encontrar la cantidad de vecinos
     * @return Cantidad de vecinos del núcleo
     */
    public static int getCantidadVecinos(int core) {
        if (core == 6) {
            return 6;
        }
        return 3;
    }

    /**
     * Conversión a decibelios
     *
     * @param value Valor de crosstalk
     * @return Valor de crosstalk en decibelios
     */
    public static BigDecimal toDB(double value) {
        try {
            //return new BigDecimal(10D*Math.log10(value));
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value);
    }

    /**
     * Función de asignación de conexiones a la red
     *
     * @param graph                  Red
     * @param establishedRoute       Ruta a establecer
     * @param crosstalkPerUnitLength Crosstalk por unidad de distancia de la
     *                               fibra
     * @return Respuesta de la operación
     */
    public static AssignFsResponse assignFs(Graph<Integer, Link> graph, EstablishedRoute establishedRoute, Double crosstalkPerUnitLength) {
        for (int j = 0; j < establishedRoute.getPath().size(); j++) {
            for (int i = establishedRoute.getFsIndexBegin(); i < establishedRoute.getFsIndexBegin() + establishedRoute.getFsWidth(); i++) {
                double crosstalk = 0;
                BigDecimal crosstalkDB = null;
                // se establece que el fs ya esta ocupado
                establishedRoute.getPath().get(j).getCores().get(establishedRoute.getPathCores().get(j)).getFrequencySlots().get(i).setFree(false);
                //marca el fs con el id de la ruta.
                establishedRoute.getPath().get(j).getCores().get(establishedRoute.getPathCores().get(j)).getFrequencySlots().get(i).setId_rutas(establishedRoute.getId());

                Integer core = establishedRoute.getPathCores().get(j);
                establishedRoute.getPath().get(j).getCores().get(core).getFrequencySlots().get(i).setLifetime(establishedRoute.getLifetime());
                List<Integer> coreVecinos = getCoreVecinos(core);

                //Se suma el crosstalk a los vecinos
                for (Integer coreIndex = 0; coreIndex < establishedRoute.getPath().get(j).getCores().size(); coreIndex++) {
                    if (!core.equals(coreIndex) && coreVecinos.contains(coreIndex)) {

                        //se asigna el crosstalk a las ranuras de los cores vecinos
                        crosstalk = XT(establishedRoute.getVecinos_crosstalk().get(j), crosstalkPerUnitLength, establishedRoute.getPath().get(j).getDistance());
                        crosstalkDB = toDB(crosstalk);
                        establishedRoute.getPath().get(j).getCores().get(coreIndex).getFrequencySlots().get(i).setCrosstalk(establishedRoute.getPath().get(j).getCores().get(coreIndex).getFrequencySlots().get(i).getCrosstalk().add(crosstalkDB));

                        // se suma doble el crosstalk...
                        // BigDecimal existingCrosstalk = graph.getEdge(establishedRoute.getPath().get(j).getTo(), establishedRoute.getPath().get(j).getFrom()).getCores().get(coreIndex).getFrequencySlots().get(i).getCrosstalk();
                        // graph.getEdge(establishedRoute.getPath().get(j).getTo(), establishedRoute.getPath().get(j).getFrom()).getCores().get(coreIndex).getFrequencySlots().get(i).setCrosstalk(existingCrosstalk.add(crosstalkDB));
                        // System.out.println("CT despues de suma" + graph.getEdge(establishedRoute.getPath().get(j).getTo(), establishedRoute.getPath().get(j).getFrom()).getCores().get(coreIndex).getFrequencySlots().get(i).getCrosstalk());
                    }
                }
                // tambien se agrega al core de la ruta, se le suma el crosstalk por si es que otras rutas tiene crosstalk en los cores vecinos.
                // cuenta el crosstalk que genera a si mismo.
                establishedRoute.getPath().get(j).getCores().get(core).getFrequencySlots().get(i).setCrosstalk(establishedRoute.getPath().get(j).getCores().get(core).getFrequencySlots().get(i).getCrosstalk().add(crosstalkDB));

            }
        }
        AssignFsResponse response = new AssignFsResponse(graph, establishedRoute);
        return response;
    }

    /**
     * Función de desasignación de conexiones a la red
     *
     * @param graph                  Red
     * @param establishedRoute       Ruta a establecer
     * @param crosstalkPerUnitLength Crosstalk por unidad de distancia de la
     *                               fibra
     */
    public static void deallocateFs(Graph<Integer, Link> graph, EstablishedRoute establishedRoute, Double crosstalkPerUnitLength) {
        for (int j = 0; j < establishedRoute.getPath().size(); j++) {
            boolean flag_delete = false;
            for (int i = establishedRoute.getFsIndexBegin(); i < establishedRoute.getFsIndexBegin() + establishedRoute.getFsWidth(); i++) {
                double crosstalk = 0;
                BigDecimal crosstalkDB = null;
                Integer core = establishedRoute.getPathCores().get(j);
                // establece nuevamente el fs como libre
                establishedRoute.getPath().get(j).getCores().get(core).getFrequencySlots().get(i).setFree(true);
                establishedRoute.getPath().get(j).getCores().get(core).getFrequencySlots().get(i).setLifetime(0);
                List<Integer> coreVecinos = getCoreVecinos(core);

                // Desasigna el crosstalk de los cores vecinos
                for (Integer coreIndex = 0; coreIndex < establishedRoute.getPath().get(j).getCores().size(); coreIndex++) {
                    if (!core.equals(coreIndex) && coreVecinos.contains(coreIndex)) {
                        crosstalk = XT(establishedRoute.getVecinos_crosstalk().get(j), crosstalkPerUnitLength, establishedRoute.getPath().get(j).getDistance());
                        crosstalkDB = toDB(crosstalk);
                        establishedRoute.getPath().get(j).getCores().get(coreIndex).getFrequencySlots().get(i).setCrosstalk(establishedRoute.getPath().get(j).getCores().get(coreIndex).getFrequencySlots().get(i).getCrosstalk().subtract(crosstalkDB));
                        //BigDecimal existingCrosstalk = graph.getEdge(establishedRoute.getPath().get(j).getTo(), establishedRoute.getPath().get(j).getFrom()).getCores().get(coreIndex).getFrequencySlots().get(i).getCrosstalk();
                        //graph.getEdge(establishedRoute.getPath().get(j).getTo(), establishedRoute.getPath().get(j).getFrom()).getCores().get(coreIndex).getFrequencySlots().get(i).setCrosstalk(existingCrosstalk.subtract(crosstalkDB));
                        //System.out.println("CT despues de suma" + graph.getEdge(establishedRoute.getPath().get(j).getTo(), establishedRoute.getPath().get(j).getFrom()).getCores().get(coreIndex).getFrequencySlots().get(i).getCrosstalk());
                    }
                }
                //se resta el crosstalk generado en el fs del core de la ruta.
                establishedRoute.getPath().get(j).getCores().get(core).getFrequencySlots().get(i).setCrosstalk(establishedRoute.getPath().get(j).getCores().get(core).getFrequencySlots().get(i).getCrosstalk().subtract(crosstalkDB));
                establishedRoute.getPath().get(j).getCores().get(core).getFrequencySlots().get(i).setId_rutas(null);
            }
            //eliminar el numero de id de la lista de rutas del core  
            Eliminar_idrutas(establishedRoute, establishedRoute.getPathCores().get(j), establishedRoute.getId(), j);
        }
    }

    /**
     * Obtiene los índices de los núcleos vecinos para un núcleo de la fibra
     *
     * @param coreActual Núcleo de la fibra
     * @return Núcleos adyacentes al núcleo actual
     */
    public static List<Integer> getCoreVecinos(Integer coreActual) {
        List<Integer> vecinos = new ArrayList<>();
        switch (coreActual) {
            case 0 -> {
                vecinos.add(1);
                vecinos.add(5);
                vecinos.add(6);
            }
            case 1 -> {
                vecinos.add(0);
                vecinos.add(2);
                vecinos.add(6);
            }
            case 2 -> {
                vecinos.add(1);
                vecinos.add(3);
                vecinos.add(6);
            }
            case 3 -> {
                vecinos.add(2);
                vecinos.add(4);
                vecinos.add(6);
            }
            case 4 -> {
                vecinos.add(3);
                vecinos.add(5);
                vecinos.add(6);
            }
            case 5 -> {
                vecinos.add(0);
                vecinos.add(4);
                vecinos.add(6);
            }
            case 6 -> {
                vecinos.add(0);
                vecinos.add(1);
                vecinos.add(2);
                vecinos.add(3);
                vecinos.add(4);
                vecinos.add(5);
            }
        }
        return vecinos;
    }

    /**
     * Elimina el id de la ruta eliminado en la lista de rutas el core
     *
     * @param establishedRoute es la ruta el cual se va a eliminar
     * @param core             es indice del core analizado
     * @param id               es el id de la ruta a eliminar
     */
    private static void Eliminar_idrutas(EstablishedRoute establishedRoute, Integer core, Integer id, int camino) {
        //auxiliar para el tamanho de la lista
        int size = establishedRoute.getPath().get(camino).getCores().get(core).getId_rutas().size();
        List<Integer> id_rutas = new ArrayList<>(); // variable auxiliar
        id_rutas = establishedRoute.getPath().get(camino).getCores().get(core).getId_rutas();
        for (int i = 0; i < size; i++) {
            if (id_rutas.get(i).equals(id)) {
                establishedRoute.getPath().get(camino).getCores().get(core).getId_rutas().remove(i);
                break;
            }
        }
    }
}
