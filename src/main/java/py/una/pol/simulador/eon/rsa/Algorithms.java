package py.una.pol.simulador.eon.rsa;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.Data;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.shortestpath.KShortestSimplePaths;
import org.jgrapht.graph.SimpleWeightedGraph;
import py.una.pol.simulador.eon.SimulatorTest;
import py.una.pol.simulador.eon.models.*;
import py.una.pol.simulador.eon.models.enums.CoreSelectionEnum;
import py.una.pol.simulador.eon.utils.Utils;

import static py.una.pol.simulador.eon.models.enums.CoreSelectionEnum.HEURISTIC_V0;

/**
 *
 * @author Néstor E. Reinoso Wood
 */
public class Algorithms {

    /**
     * Ordena una lista de rutas candidatas (KSP) basándose en su uso actual, utilizando una política
     * de "Least Loaded" (Menos Cargada).
     * <p>
     * El "peso" de cada ruta se calcula como la suma de los pesos de sus enlaces individuales.
     * El peso de un enlace se define por el núcleo que tiene la <b>menor cantidad de ranuras de frecuencia (FS) ocupadas</b>.
     * Es decir, para cada enlace, se busca el núcleo más libre (con menos FS ocupados) y ese valor de ocupación
     * se suma al peso total de la ruta.
     * </p>
     * <p>
     * Las rutas se ordenan de forma ascendente: las rutas con menor peso (más libres) aparecerán primero.
     * Este ordenamiento modifica la lista original proporcionada.
     * </p>
     *
     * @param kspPaths Lista de rutas candidatas (GraphPath) que será ordenada in-place.
     */
    private static void ordenarKSPPorUso(List<GraphPath<Integer, Link>> kspPaths) {
        Collections.sort(kspPaths, (path1, path2) -> {
            int weight1 = 0;
            for (Link link : path1.getEdgeList()) {
                int minUsed = Integer.MAX_VALUE;
                for (Core core : link.getCores()) {
                    int used = 0;
                    for (FrequencySlot fs : core.getFrequencySlots()) {
                        if (!fs.isFree()) {
                            used++;
                        }
                    }
                    if (used < minUsed) {
                        minUsed = used;
                    }
                }
                if (minUsed == Integer.MAX_VALUE) {
                    minUsed = 0;
                }
                weight1 += minUsed;
            }

            int weight2 = 0;
            for (Link link : path2.getEdgeList()) {
                int minUsed = Integer.MAX_VALUE;
                for (Core core : link.getCores()) {
                    int used = 0;
                    for (FrequencySlot fs : core.getFrequencySlots()) {
                        if (!fs.isFree()) {
                            used++;
                        }
                    }
                    if (used < minUsed) {
                        minUsed = used;
                    }
                }
                if (minUsed == Integer.MAX_VALUE) {
                    minUsed = 0;
                }
                weight2 += minUsed;
            }
            return Integer.compare(weight1, weight2);
        });
    }

    /**
     * Funcion que verifica si el bloque de ranuras candidatas estan libres, si
     * no pertenecen a alguna ruta
     *
     * @param bloqueFS es una lista que representa el bloque de fs a analizar.
     * @return boolean true si se puede usar, false si alguna ranura ya esta
     * ocupada por otra ruta.
     */
    private static Boolean isFSBlockFree(List<FrequencySlot> bloqueFS) {
        for (FrequencySlot fs : bloqueFS) {
            if (!fs.isFree()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Verifica que no se supere el crosstalk maximo, al sumar el crosstalk de
     * la ruta con el del bloque de ranuras candidatas (esto se hace por cada
     * enlace), menos con el crosstalk final de la ruta (hasta el penultimo
     * bloque)
     *
     * @param link          es el enlace analizado
     * @param core          es el nucleos analizado
     * @param index         es el indice donde comienza el bloque de ranuras
     * @param fss           es el bloque de ranuras elegidas como candidatas para
     *                      establecer la demanda en el enlace
     * @param maxCrosstalk  es el umbral maximo tolerado de crosstalk
     * @param crosstalkRuta es una lista donde se contiene la sumatoria de los
     *                      crosstalk por enlace de la ruta.
     * @return booleano, true si no supera el umbral maximo, false caso
     * contrario.
     *
     */
    private static Boolean isFsBlockCrosstalkFree(Link link, int core, int index, List<FrequencySlot> fss, BigDecimal maxCrosstalk, List<BigDecimal> crosstalkRuta) {
        // verifica primero cuantos vecinos ya tienen crosstalk existen
        int v_crosstalk = CalculaVecinosConCrosstalk(link, core, index, fss.size());

        for (int j = index; j < fss.size(); j++) {
            BigDecimal crosstalkActual = crosstalkRuta.get(j).add(fss.get(j).getCrosstalk());
            if (crosstalkActual.compareTo(maxCrosstalk) > 0) {
                //si existe algun vecino con crosstalk, retorna false y hay bloqueo de crosstalk.
                if (v_crosstalk > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Funcion que teniendo el crosstalk final de la ruta (sumatoria de todos
     * los enlaces) compara con el bloque de ranuras de cada enlace para
     * verificar que no se supere el crosstalk
     *
     * @param bloques        es una lista que en cada posicion contiene los bloques de
     *                       ranuras candidatas de cada enlace de la ruta
     * @param index          es el indice donde inicia el bloque de ranuras
     * @param enlaces        es una lista de los enlaces de la ruta
     * @param Cores          es la lista de numero de core elegido por enlace
     * @param tamanhobloque, es el tamanho maximo de ranuras que ocupa la demanda
     * @param maxCrosstalk   es el umbral maximo tolerado de crosstalk
     * @param crosstalkRuta  es la lista auxiliar donde va guardando la sumatoria de crosstalk de los enlaces
     * @return booleano , true si los bloques en los enlaces superan la
     * sumatoria de crosstalk total.
     */
    private static Boolean BloqueFsToleraCrosstalkFinal(List<List<FrequencySlot>> bloques, int index, List<Link> enlaces, List<Integer> Cores, int tamanhobloque, BigDecimal maxCrosstalk, List<BigDecimal> crosstalkRuta) {
        int indice = 0; // para ir iterando las posiciones de las listas
        int v_crosstalk = 0; //cantidad de vecinos con crosstalk (se calcula por enlace)

        for (List<FrequencySlot> bloque : bloques) {
            Link enlace = enlaces.get(indice);
            Integer core = Cores.get(indice);
            v_crosstalk = CalculaVecinosConCrosstalk(enlace, core, index, bloque.size());
            for (int i = index; i < bloque.size(); i++) {
                //le suma el crosstalk total , al crosstalk del fs del bloque del enlace si es que hay.
                BigDecimal crosstalkActual = crosstalkRuta.get(i).add(bloque.get(i).getCrosstalk());
                if (crosstalkActual.compareTo(maxCrosstalk) > 0) {
                    if (v_crosstalk > 0) //si supera pero no tiene vecinos con crosstalk activo , igual debe instalar la ruta.
                    {
                        return false; // inmediatamente si alguno supera, se devuelve false
                    }
                }
            }
            indice++;
        }
        return true;
    }

    /**
     * Funcion que verifica si el crosstalk generado por el enlace sumando con
     * los crosstalks vecinos no supera el umbral maximo tolerado de crosstalk,
     * verifica las ranuras de los nucleos vecinos de enlace analizado.
     *
     * @param link                   enlace analizado
     * @param maxCrosstalk,          valor del umbral maximo tolerado de crosstalk
     * @param core                   nucleo utilizado en el enlace
     * @param fsIndexBegin           indice desde donde empieza el bloque de ranuras del enlace
     * @param fsWidth                indice final del bloque de ranuras del enlace
     * @param crosstalkPerUnitLength valor de h utilizado para calcular el crosstalk.
     * @return boolean true si no se supera el umbral maximo, false en caso contrario.
     */
    private static Boolean isNextToCrosstalkFreeCores(Link link, BigDecimal maxCrosstalk, Integer core, Integer fsIndexBegin, Integer fsWidth, Double crosstalkPerUnitLength) {
        List<Integer> vecinos = Utils.getCoreVecinos(core);
        //aca verifica cuantos vecinos debe sumarle para tener el crosstalk a sumar 
        int v_crosstalk = CalculaVecinosConCrosstalk(link, core, fsIndexBegin, fsWidth);
        for (Integer coreVecino : vecinos) {
            for (Integer i = fsIndexBegin; i < fsIndexBegin + fsWidth; i++) {
                FrequencySlot fsVecino = link.getCores().get(coreVecino).getFrequencySlots().get(i);
                if (!fsVecino.isFree()) {
                    BigDecimal crosstalkASumar = Utils.toDB(Utils.XT(v_crosstalk, crosstalkPerUnitLength, link.getDistance()));
                    BigDecimal crosstalk = fsVecino.getCrosstalk().add(crosstalkASumar);
                    //BigDecimal crosstalkDB = Utils.toDB(crosstalk.doubleValue());
                    if (crosstalk.compareTo(maxCrosstalk) >= 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Teniendo el crosstalk de la ruta (suma hasta el enlace final) Va
     * verificando nuevamente que no sobrepase el umbral maximo en los fs de los
     * vecinos
     *
     * @param cores         es una lista de nucleos, contiene los nucleos elegidos de todos los enlaces.
     * @param enlaces       una lista de todos los enlaces de las rutas
     * @param maxCrosstalk  umbral maximo tolerado para el crosstalk.
     * @param fsIndexBegin  indice donde comienza el bloque de ranuras.
     * @param fsWidth       indice final del bloque de ranuras.
     * @param crosstalkRuta es la variable que contiene la sumatoria de crosstalk de
     *                      los enlaces (crosstalk final en el último enlace)
     * @return boolean true si no se sobrepasa el crosstalk, false caso contrario.
     */
    private static boolean ToleraCrosstalkVecinos(List<Integer> cores, List<Link> enlaces, BigDecimal maxCrosstalk, int fsIndexBegin, int fsWidth, BigDecimal crosstalkRuta) {
        for (int j = 0; j < cores.size(); j++) {
            //j itera el core y el enlace
            List<Integer> vecinos = Utils.getCoreVecinos(j);
            for (Integer coreVecino : vecinos) {
                for (Integer i = fsIndexBegin; i < fsIndexBegin + fsWidth; i++) {
                    FrequencySlot fsVecino = enlaces.get(j).getCores().get(coreVecino).getFrequencySlots().get(i);
                    if (!fsVecino.isFree()) {
                        BigDecimal crosstalk = fsVecino.getCrosstalk().add(crosstalkRuta);
                        //BigDecimal crosstalkDB = Utils.toDB(crosstalk.doubleValue());
                        if (crosstalk.compareTo(maxCrosstalk) >= 0) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Funcion que retorna la cantidad de vecinos afectados por el crosstalk, y
     * que se deben tener en cuenta para el cálculo del crosstalk de la ruta
     *
     * @param link         enlace analizado
     * @param core         cantidad de nucleos de la fibra = 7 // SimulatorTest.contador_crosstalk ++;
     * @param fsIndexBegin indice de la ranura inicial del bloque de ranuras candidatas
     * @param fsWidth      cantidad de ranuras necesarias para la demanda
     * @return cantidad de vecinos a tener en cuenta en el calculo del crosstalk de la red.
     */
    private static int CalculaVecinosConCrosstalk(Link link, Integer core, Integer fsIndexBegin, Integer fsWidth) {
        //variable auxiliar donde se guarda la cantidad de vecinos que si son afectados por el crosstalk.
        Integer vecino_afectado = 0;
        List<Integer> vecinos = Utils.getCoreVecinos(core);
        for (Integer coreVecino : vecinos) {
            for (Integer i = fsIndexBegin; i < fsIndexBegin + fsWidth; i++) {
                FrequencySlot fsVecino = link.getCores().get(coreVecino).getFrequencySlots().get(i);
                if (!fsVecino.isFree()) {
                    vecino_afectado++;
                    // Salir del for interno y continuar con el siguiente coreVecino
                    break;
                }
            }
        }
        return vecino_afectado;
    }

    /**
     * Funcion que asigna el id de la ruta establecida como marcador a los cores
     * de los enlaces de las rutas
     *
     * @param establishedRoute , obtiene la ruta establecida actual
     */
    private static void Assigna_idruta(EstablishedRoute establishedRoute) {
        for (int i = 0; i < establishedRoute.getPath().size(); i++) {
            //una variable donde trae el core elegido para el enlace de la ruta
            int core_index = establishedRoute.getPathCores().get(i);
            //va estableciendo los id de rutas en los cores de los enlaces
            establishedRoute.getPath().get(i).getCores().get(core_index).getId_rutas().add(establishedRoute.getId());
        }
    }


    /**
     * Versión Paralela Random Fit del algoritmo ruteoCoreMultipleAgendadoFixed.
     */
    public static EstablishedRoute ruteoCoreMultipleAgendadoFixed(Graph<Integer, Link> graph, Demand demand, Integer capacity, Integer cores, BigDecimal maxCrosstalk, Double crosstalkPerUnitLength, py.una.pol.simulador.eon.models.enums.CoreSelectionEnum coreSelection) {
//        KShortestSimplePaths<Integer, Link> kspFinder = new KShortestSimplePaths<>(graph);
//        List<GraphPath<Integer, Link>> kspPaths = kspFinder.getPaths(demand.getSource(), demand.getDestination(), 5);
//
//        ordenarKSPPorUso(kspPaths);

        Graph<Integer, Link> grafoCongestion = getGrafoPonderadoPorUso(graph);
        KShortestSimplePaths<Integer, Link> kspFinder = new KShortestSimplePaths<>(grafoCongestion);
        List<GraphPath<Integer, Link>> kspPaths = kspFinder.getPaths(demand.getSource(), demand.getDestination(), 5);

        AtomicBoolean flag_crosstalk = new AtomicBoolean(false);
        AtomicBoolean flag_frag = new AtomicBoolean(false);
        AtomicBoolean flag_capacidad = new AtomicBoolean(false);

        // 1. Iterar sobre los K caminos más cortos candidatos (Path Selection)
        for (GraphPath<Integer, Link> path : kspPaths) {

            List<Integer> shuffledFSList = new ArrayList<>();
            for (int fsIndex = 0; fsIndex <= capacity - demand.getFs(); fsIndex++) {
                shuffledFSList.add(fsIndex);
            }
            // sin random FS
//            Collections.shuffle(shuffledFSList);

            // Parallel Search
            /*
            Optional<AllocationResult> resultOpt = shuffledFSList.parallelStream()
                    .map(fsIndex -> tryAllocatePath(path, fsIndex, demand, cores, maxCrosstalk, crosstalkPerUnitLength))
                    .peek(res -> {
                        if (!res.isSuccess()) {
                            if (res.isCrosstalkError()) flag_crosstalk.set(true);
                            if (res.isFragmentationError()) flag_frag.set(true);
                            if (res.isCapacityError()) flag_capacidad.set(true);
                        }
                    })
                    .filter(AllocationResult::isSuccess)
                    .findAny();
             */

            Optional<AllocationResult> resultOpt = Optional.empty();
            for (int fsIndex = 0; fsIndex <= capacity - demand.getFs(); fsIndex++) {
                AllocationResult res = tryAllocatePath(path, fsIndex, demand, cores, maxCrosstalk, crosstalkPerUnitLength, coreSelection);
                if (res.isSuccess()) {
                    resultOpt = Optional.of(res);
                    break;
                } else {
                    if (res.isCrosstalkError()) flag_crosstalk.set(true);
                    if (res.isFragmentationError()) flag_frag.set(true);
                    if (res.isCapacityError()) flag_capacidad.set(true);
                }
            }


            if (resultOpt.isPresent()) {
                AllocationResult result = resultOpt.get();
                // Ruta Encontrada: Construir y retornar objeto EstablishedRoute
                EstablishedRoute route = new EstablishedRoute(
                        path.getEdgeList(),
                        result.getFsIndex(),
                        demand.getFs(),
                        demand.getLifetime(),
                        demand.getSource(),
                        demand.getDestination(),
                        result.getAssignedCores(),
                        kspPaths.indexOf(path),
                        result.getMaxDistance(),
                        result.getCrosstalkNeighbors()
                );
                Assigna_idruta(route);
                return route;
            }
        }

        // Si no se encontró ruta, actualizar contadores globales de simulación
        if (flag_capacidad.get()) {
            SimulatorTest.CONTADOR_FRAG_RUTA++;
        }
        if (flag_crosstalk.get()) {
            SimulatorTest.CONTADOR_CROSSTALK++;
        }
        if (flag_frag.get() && !flag_crosstalk.get()) {
            SimulatorTest.CONTADOR_FRAG++;
        }

        return null; // Bloqueo
    }


    /**
     * Intenta asignar núcleos a todos los enlaces de una ruta candidata para un bloque de espectro específico.
     */
    private static AllocationResult tryAllocatePath(GraphPath<Integer, Link> path, int fsIndex, Demand demand, Integer totalCores, BigDecimal maxCrosstalk, Double crosstalkPerUnitLength, CoreSelectionEnum coreSelection) {
        AllocationResult result = new AllocationResult();
        result.setFsIndex(fsIndex);

        List<Link> links = path.getEdgeList();
        List<Integer> currentCores = new ArrayList<>();
        List<List<FrequencySlot>> currentBlocks = new ArrayList<>();
        List<Link> currentLinks = new ArrayList<>();
        List<Integer> neighborCounts = new ArrayList<>();

        // Inicializar monitoreo de crosstalk acumulado por slot
        List<BigDecimal> routeCrosstalkPerFS = new ArrayList<>();
        for(int i=0; i<demand.getFs(); i++) routeCrosstalkPerFS.add(BigDecimal.ZERO);

        int maxDist = 0;

        for (Link link : links) {
            boolean linkAllocated = false;
            List<Integer> coresList;

            switch (coreSelection) {
                case RANDOM:
                    coresList = Arrays.asList(0, 1, 2, 3, 4, 5, 6);
                    Collections.shuffle(coresList);
                    break;
                case SORTED_BY_FREE_FS:
                    coresList = getSortedCoresByFreeFS(link);
                    break;
                case SORTED_BY_ENTROPY:
                    coresList = getSortedCoresByEntropy(link);
                    break;
                case SORTED_BY_BFR:
                    coresList = ordernarCoresPorBFR(link);
                    break;
                case HEURISTIC_V0:
                    coresList = Arrays.asList(1, 2, 3, 4, 5, 6, 0);
                    break;
                case HEURISTIC_V1:
                    coresList = Arrays.asList(1, 3, 5, 2, 4, 6, 0);
                    break;
                case HEURISTIC_ORDER:
                    coresList = heuristicCoresOrder();
                    break;
                case HEURISTIC_ORDER_DUAL:
                    coresList = heuristicCoresOrderDual();
                    break;
                case SEQUENTIAL:
                default:
                    coresList = Arrays.asList(0, 1, 2, 3, 4, 5, 6);
                    break;
            }

            // variante para solo buscar en los primeros 3 núcleos mas libres
            for (int core : coresList) {
                // --- Validaciones Locales ---

                // 1. Bloque de Espectro Libre
                List<FrequencySlot> block = link.getCores().get(core).getFrequencySlots().subList(fsIndex, fsIndex + demand.getFs());
                if (!isFSBlockFree(block)) {
                    result.setFragmentationError(true);
                    continue;
                }

                // 2. Crosstalk Local y Acumulado (Parcial)
                if (!isFsBlockCrosstalkFree(link, core, fsIndex, block, maxCrosstalk, routeCrosstalkPerFS)) {
                    result.setCrosstalkError(true);
                    continue;
                }

                // 3. Crosstalk Vecinos del Enlace
                if (!isNextToCrosstalkFreeCores(link, maxCrosstalk, core, fsIndex, demand.getFs(), crosstalkPerUnitLength)) {
                    result.setCrosstalkError(true);
                    continue;
                }

                // --- Cálculo de Impacto de Crosstalk ---
                int activeNeighbors = CalculaVecinosConCrosstalk(link, core, fsIndex, demand.getFs());
                List<BigDecimal> tempCrosstalk = new ArrayList<>(routeCrosstalkPerFS);
                BigDecimal linkXT = Utils.toDB(Utils.XT(activeNeighbors, crosstalkPerUnitLength, link.getDistance()));

                boolean limitExceeded = false;
                for (int i = 0; i < demand.getFs(); i++) {
                    BigDecimal newVal = tempCrosstalk.get(i).add(linkXT);
                    tempCrosstalk.set(i, newVal);
                    if (newVal.compareTo(maxCrosstalk) > 0) limitExceeded = true;
                }

                // Regla especial de tolerancia: Solo permitir exceder si no hay vecinos activos (supuesto del modelo)
                if (limitExceeded && activeNeighbors > 0) {
                    result.setCrosstalkError(true);
                    continue;
                }

                // --- Validaciones Globales (Whole Path Consistency) ---
                /*
                List<List<FrequencySlot>> testBlocks = new ArrayList<>(currentBlocks);
                testBlocks.add(block);
                List<Link> testLinks = new ArrayList<>(currentLinks);
                testLinks.add(link);
                List<Integer> testCores = new ArrayList<>(currentCores);
                testCores.add(core);

                // 4. Re-validar bloques anteriores con el nuevo nivel de crosstalk total
                if (!BloqueFsToleraCrosstalkFinal(testBlocks, fsIndex, testLinks, testCores, demand.getFs(), maxCrosstalk, tempCrosstalk)) {
                    result.setCrosstalkError(true);
                    continue;
                }

                // 5. Re-validar vecinos anteriores con el nuevo nivel de crosstalk total
                // Nota: Usamos el crosstalk del último slot como proxy conservador del crosstalk total de la ruta
                BigDecimal lastSlotCrosstalk = tempCrosstalk.get(demand.getFs() - 1);
                if (!ToleraCrosstalkVecinos(testCores, testLinks, maxCrosstalk, fsIndex, demand.getFs(), lastSlotCrosstalk)) {
                    result.setCrosstalkError(true);
                    continue;
                }
                */

                // --- Asignación Exitosa para este Enlace ---
                currentCores.add(core);
                currentBlocks.add(block);
                currentLinks.add(link);
                neighborCounts.add(activeNeighbors);
                routeCrosstalkPerFS = tempCrosstalk;
                if(link.getDistance() > maxDist) maxDist = link.getDistance();

                linkAllocated = true;
                break; // Núcleo encontrado, pasar al siguiente enlace
            }

            if (!linkAllocated) {
                result.setCapacityError(true);
                return result; // Fallo del camino: no se encontró núcleo para un enlace intermedio
            }
        }

        // Todos los enlaces asignados correctamente
        result.setSuccess(true);
        result.setAssignedCores(currentCores);
        result.setCrosstalkNeighbors(neighborCounts);
        result.setMaxDistance(maxDist);
        return result;
    }

    private static List<Integer> getSortedCoresByFreeFS(Link link) {
        List<Integer> coresByFreeFS = new ArrayList<>();
        int numCores = link.getCores().size();
        int[] freeCounts = new int[numCores];

        for (int c = 0; c < numCores; c++) {
            int free = 0;
            for (FrequencySlot fs : link.getCores().get(c).getFrequencySlots()) {
                if (fs.isFree()) free++;
            }
            freeCounts[c] = free;
            coresByFreeFS.add(c);
        }

        coresByFreeFS.sort((a, b) -> Integer.compare(freeCounts[b], freeCounts[a]));

        if (coresByFreeFS.contains(0)) {
            int core0Free = freeCounts[0];
            // Encontrar la última posición del grupo de empate
            int lastTiedPos = -1;
            for (int i = 0; i < coresByFreeFS.size(); i++) {
                if (freeCounts[coresByFreeFS.get(i)] == core0Free) {
                    lastTiedPos = i;
                }
            }
            // Solo mover si core 0 no está ya en la última posición del grupo
            int currentPos = coresByFreeFS.indexOf(0);
            if (currentPos < lastTiedPos) {
                coresByFreeFS.remove(Integer.valueOf(0));
                coresByFreeFS.add(lastTiedPos, 0); // insertar al final del grupo, no de la lista
            }
        }
        return coresByFreeFS;
    }

    @Data
    private static class AllocationResult {
        private boolean success = false;
        private int fsIndex;
        private boolean crosstalkError = false;
        private boolean fragmentationError = false;
        private boolean capacityError = false;

        private List<Integer> assignedCores;
        private List<Integer> crosstalkNeighbors;
        private int maxDistance;
    }

    /**
     * Heurística que genera un orden aleatorio de los núcleos, pero siempre
     * colocando al núcleo 0 (más central) en la última posición, para favorecer su uso.
     */
    public static List<Integer> heuristicCoresOrder() {
        List<Integer> resultado = new ArrayList<>();
        List<Integer> impares = new ArrayList<>(Arrays.asList(1, 3, 5));
        List<Integer> pares = new ArrayList<>(Arrays.asList(0, 2, 4, 6));

        Collections.shuffle(impares);
        Collections.shuffle(pares);

        resultado.addAll(impares);
        resultado.addAll(pares);

        // System.out.println("heuristicCoresOrder: " + resultado);

        return resultado;
    }

    /**
     * Heurística que genera un orden aleatorio de los núcleos, pero con una
     * probabilidad del 50% de colocar al grupo de núcleos impares (1,3,5) antes que los pares (0,2,4,6), y viceversa.
     */
    public static List<Integer> heuristicCoresOrderDual() {
        List<Integer> resultado = new ArrayList<>();
        List<Integer> impares = new ArrayList<>(Arrays.asList(1, 3, 5));
        List<Integer> pares = new ArrayList<>(Arrays.asList(0, 2, 4, 6));

        Collections.shuffle(impares);
        Collections.shuffle(pares);

        if (ThreadLocalRandom.current().nextBoolean()) {
            resultado.addAll(impares);
            resultado.addAll(pares);
        } else {
            resultado.addAll(pares);
            resultado.addAll(impares);
        }

        // System.out.println("heuristicCoresOrderDual: " + resultado);

        return resultado;
    }

    // Crea un nuevo grafo ponderado no dirigido con todos los vértices del grafo
    // original, las aristas y asignamos sus pesos basado en el uso actual
    private static Graph<Integer, Link> getGrafoPonderadoPorUso(Graph<Integer, Link> grafoOriginal) {

        Graph<Integer, Link> grafoUso = new SimpleWeightedGraph<>(Link.class);
        Graphs.addAllVertices(grafoUso, grafoOriginal.vertexSet());

        for (Link link : grafoOriginal.edgeSet()) {
            Integer source = grafoOriginal.getEdgeSource(link);
            Integer target = grafoOriginal.getEdgeTarget(link);

            grafoUso.addEdge(source, target, link);

            double pesoUso = calcularPesoPorUsoDeEnlace(link);

            grafoUso.setEdgeWeight(link, pesoUso);
        }

        return grafoUso;
    }

    // Retorna slots ocupados de todos los cores
    private static double calcularPesoPorUsoDeEnlace(Link link) {
        double slotsOcupados = 0;
        for (Core core : link.getCores()) {
            for (FrequencySlot fs : core.getFrequencySlots()) {
                if (!fs.isFree()) {
                    slotsOcupados++;
                }
            }
        }
        return slotsOcupados + 0.0001;
    }

    private static List<Integer> getSortedCoresByEntropy(Link link) {
        List<Integer> coresByEntropy = new ArrayList<>();
        int numCores = link.getCores().size();
        int[] entropyValues = new int[numCores];

        for (int c = 0; c < numCores; c++) {
            entropyValues[c] = calcularEntropiaPorCore(link.getCores().get(c));
            coresByEntropy.add(c);
        }

        coresByEntropy.sort((a, b) -> {
            int cmp = Integer.compare(entropyValues[a], entropyValues[b]);
            if (cmp == 0) {
                if (a == 0)
                    return 1;
                if (b == 0)
                    return -1;
            }
            return cmp;
        });

        return coresByEntropy;
    }

    public static int calcularEntropiaPorCore(Core core) {
        if (core == null || core.getFrequencySlots() == null || core.getFrequencySlots().isEmpty()) {
            return 0;
        }

        List<FrequencySlot> slots = core.getFrequencySlots();
        int transitions = 0;

        for (int i = 0; i < slots.size() - 1; i++) {
            boolean currentFree = slots.get(i).isFree();
            boolean nextFree = slots.get(i + 1).isFree();

            if (currentFree != nextFree) {
                transitions++;
            }
        }
        return transitions;
    }

    private static List<Integer> ordernarCoresPorBFR(Link link) {
        List<Integer> coresByBFR = new ArrayList<>();
        int numCores = link.getCores().size();
        double[] bfrs = new double[numCores];

        for (int c = 0; c < numCores; c++) {
            bfrs[c] = calcularBFR(link.getCores().get(c));
            coresByBFR.add(c);
        }

        // Ordenar de menor a mayor BFR (menos fragmentado a más fragmentado)
        coresByBFR.sort((a, b) -> Double.compare(bfrs[a], bfrs[b]));

        return coresByBFR;
    }

    /**
     * Calcula el Blocking Fragmentation Ratio (BFR) para un núcleo dado, que es una métrica de fragmentación que
     * refleja la relación entre el bloque de ranuras más grande disponible y el total de ranuras libres en ese núcleo.
     * Un BFR cercano a 1 indica alta fragmentación (muchas ranuras libres pero ninguna lo suficientemente grande),
     * mientras que un BFR cercano a 0 indica baja fragmentación (un bloque grande de ranuras libres).
     *
     * @param core El núcleo para el cual se desea calcular el BFR.
     * @return El valor del BFR para el núcleo dado, entre 0 y 1.
     */
    private static double calcularBFR(Core core) {
        List<FrequencySlot> slots = core.getFrequencySlots();
        int totalFree = 0;
        int maxBlock = 0;
        int currentBlock = 0;

        for (FrequencySlot fs : slots) {
            if (fs.isFree()) {
                totalFree++;
                currentBlock++;
            } else {
                if (currentBlock > maxBlock) {
                    maxBlock = currentBlock;
                }
                currentBlock = 0;
            }
        }
        if (currentBlock > maxBlock) {
            maxBlock = currentBlock;
        }

        if (totalFree == 0) {
            return 1.0;
        }

        return 1.0 - ((double) maxBlock / totalFree);
    }

}
