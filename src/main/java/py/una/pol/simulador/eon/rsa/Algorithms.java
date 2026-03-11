package py.una.pol.simulador.eon.rsa;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.Data;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.KShortestSimplePaths;
import py.una.pol.simulador.eon.SimulatorTest;
import py.una.pol.simulador.eon.models.*;
import py.una.pol.simulador.eon.models.enums.MinFunction;
import py.una.pol.simulador.eon.utils.Utils;

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
    public static EstablishedRoute ruteoCoreMultipleAgendadoFixed(Graph<Integer, Link> graph, Demand demand, Integer capacity, Integer cores, BigDecimal maxCrosstalk, Double crosstalkPerUnitLength, MinFunction minFunction, py.una.pol.simulador.eon.models.enums.CoreSelectionStrategy coreSelectionStrategy) {
        KShortestSimplePaths<Integer, Link> kspFinder = new KShortestSimplePaths<>(graph);
        List<GraphPath<Integer, Link>> kspPaths = kspFinder.getPaths(demand.getSource(), demand.getDestination(), 5);

        ordenarKSPPorUso(kspPaths);

        AtomicBoolean flag_crosstalk = new AtomicBoolean(false);
        AtomicBoolean flag_frag = new AtomicBoolean(false);
        AtomicBoolean flag_capacidad = new AtomicBoolean(false);

        // 1. Iterar sobre los K caminos más cortos candidatos (Path Selection)
        for (GraphPath<Integer, Link> path : kspPaths) {

            List<Integer> shuffledFSList = new ArrayList<>();
            for (int fsIndex = 0; fsIndex <= capacity - demand.getFs(); fsIndex++) {
                shuffledFSList.add(fsIndex);
            }
            // Parallel Search
            /* Búsqueda paralela minimizando XT
            Optional<AllocationResult> resultOpt = shuffledFSList.parallelStream()
                    .map(fsIndex -> tryAllocatePath(path, fsIndex, demand, cores, maxCrosstalk, crosstalkPerUnitLength, fragmentationMetric))
                    .peek(res -> {
                        if (!res.isSuccess()) {
                            if (res.isCrosstalkError()) flag_crosstalk.set(true);
                            if (res.isFragmentationError()) flag_frag.set(true);
                            if (res.isCapacityError()) flag_capacidad.set(true);
                        }
                    })
                    .filter(AllocationResult::isSuccess)
                    .min(Comparator.comparing(AllocationResult::getMaxCrosstalkValue));
            */

            Comparator<AllocationResult> comparator;
            if (minFunction == MinFunction.FRAG_BFR) {
                comparator = Comparator.comparingDouble(AllocationResult::getBfrScore);
            } else if (minFunction == MinFunction.FRAG_ENTROPY) {
                comparator = Comparator.comparingDouble(AllocationResult::getEntropyScore);
            } else {
                comparator = Comparator.comparing(AllocationResult::getMaxCrosstalkValue);
            }

            Optional<AllocationResult> resultOpt = shuffledFSList.parallelStream()
                    .map(fsIndex -> tryAllocatePath(path, fsIndex, demand, cores, maxCrosstalk, crosstalkPerUnitLength, minFunction, coreSelectionStrategy))
                    .peek(res -> {
                        if (!res.isSuccess()) {
                            if (res.isCrosstalkError()) flag_crosstalk.set(true);
                            if (res.isFragmentationError()) flag_frag.set(true);
                            if (res.isCapacityError()) flag_capacidad.set(true);
                        }
                    })
                    .filter(AllocationResult::isSuccess)
                    .min(comparator);

            /*
            Optional<AllocationResult> resultOpt = Optional.empty();
            for (int fsIndex = 0; fsIndex <= capacity - demand.getFs(); fsIndex++) {
                AllocationResult res = tryAllocatePath(path, fsIndex, demand, cores, maxCrosstalk, crosstalkPerUnitLength);
                if (res.isSuccess()) {
                    resultOpt = Optional.of(res);
                    break;
                } else {
                    if (res.isCrosstalkError()) flag_crosstalk.set(true);
                    if (res.isFragmentationError()) flag_frag.set(true);
                    if (res.isCapacityError()) flag_capacidad.set(true);
                }
            }
            */


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
    private static AllocationResult tryAllocatePath(GraphPath<Integer, Link> path, int fsIndex, Demand demand, Integer totalCores, BigDecimal maxCrosstalk, Double crosstalkPerUnitLength, MinFunction minFunction, py.una.pol.simulador.eon.models.enums.CoreSelectionStrategy coreSelectionStrategy) {
        AllocationResult result = new AllocationResult();
        result.setFsIndex(fsIndex);

        List<Link> links = path.getEdgeList();
        List<Integer> currentCores = new ArrayList<>();
        List<List<FrequencySlot>> currentBlocks = new ArrayList<>();
        List<Link> currentLinks = new ArrayList<>();
        List<Integer> neighborCounts = new ArrayList<>();

        // Inicializar monitoreo de crosstalk acumulado por slot
        List<BigDecimal> routeCrosstalkPerFS = new ArrayList<>();
        for (int i = 0; i < demand.getFs(); i++) routeCrosstalkPerFS.add(BigDecimal.ZERO);

        int maxDist = 0;

        for (Link link : links) {
            boolean linkAllocated = false;
            // cores core criterios
            List<Integer> coresList;
            switch(coreSelectionStrategy) {
                case NORMAL:
                    coresList = Arrays.asList(0, 1, 2, 3, 4, 5, 6);
                    break;
                case LEAST_LOADED:
                    coresList = getSortedCoresByFreeFS(link);
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
                default:
                    coresList = Arrays.asList(0, 1, 2, 3, 4, 5, 6);
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
                if (link.getDistance() > maxDist) maxDist = link.getDistance();

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

        if (minFunction == MinFunction.XT) {
            // Crosstalk máximo de la ruta (para desempate)
            BigDecimal maxXT = routeCrosstalkPerFS.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            result.setMaxCrosstalkValue(maxXT);
        }

        if (minFunction == MinFunction.FRAG_BFR) {
            // Score de fragmentación residual según métricas
            double bfrScore = calcularFragmentacionRuta(
                    currentLinks, currentCores, fsIndex, demand.getFs(), MinFunction.FRAG_BFR);
            result.setBfrScore(bfrScore);
        }

        if (minFunction == MinFunction.FRAG_BFR) {
            double entropyScore = calcularFragmentacionRuta(
                    currentLinks, currentCores, fsIndex, demand.getFs(), MinFunction.FRAG_ENTROPY);
            result.setEntropyScore(entropyScore);
        }

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
        private BigDecimal maxCrosstalkValue = BigDecimal.ZERO;
        /**
         * Score de fragmentación residual según BFR (menor = mejor).
         */
        private double bfrScore = 0.0;
        /**
         * Score de fragmentación residual según entropía (menor = mejor).
         */
        private double entropyScore = 0.0;
    }

    // =========================================================================
    // Métodos de cálculo de fragmentación del espectro
    // =========================================================================

    /**
     * Calcula el score de fragmentación promedio de la ruta, simulando que el
     * bloque [fsIndex, fsIndex + fsWidth) está asignado en los cores indicados.
     * El grafo real NO se modifica.
     *
     * @param enlaces Lista de enlaces de la ruta
     * @param cores   Core elegido por enlace
     * @param fsIndex Índice de inicio del bloque a simular
     * @param fsWidth Cantidad de slots del bloque
     * @param metrica Métrica a utilizar (NONE=0, ENTROPY, BFR)
     * @return Promedio del score de fragmentación sobre todos los enlaces
     */
    private static double calcularFragmentacionRuta(
            List<Link> enlaces, List<Integer> cores,
            int fsIndex, int fsWidth, MinFunction metrica) {
        if (metrica == MinFunction.XT || enlaces.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        for (int i = 0; i < enlaces.size(); i++) {
            Core core = enlaces.get(i).getCores().get(cores.get(i));
            if (metrica == MinFunction.FRAG_ENTROPY) {
                total += calcularEntropiaCoreSiAsignado(core, fsIndex, fsWidth);
            } else { // BFR
                total += calcularBFRCoreSiAsignado(core, fsIndex, fsWidth);
            }
        }
        return total / enlaces.size();
    }

    /**
     * Calcula la entropía de Shannon del core simulando que el bloque
     * [fsStart, fsStart + fsWidth) pasa a estar ocupado.
     *
     * <p>La entropía se calcula sobre los bloques contiguos LIBRES residuales:
     * {@code H = -Σ (p_i * log2(p_i))}, donde {@code p_i = tamaño_bloque_i / total_slots_libres_residual}.
     *
     * <p>Un espectro sin fragmentación (un solo bloque libre) tiene entropía mínima.
     * Un espectro muy fragmentado (muchos bloques pequeños) tiene entropía máxima.
     *
     * @return Entropía residual (0 = sin fragmentación, mayor = más fragmentado)
     */
    private static double calcularEntropiaCoreSiAsignado(Core core, int fsStart, int fsWidth) {
        List<FrequencySlot> slots = core.getFrequencySlots();
        int total = slots.size();

        // Construir máscara de slots libres simulando la asignación del bloque
        boolean[] libre = new boolean[total];
        for (int i = 0; i < total; i++) {
            boolean enBloque = (i >= fsStart && i < fsStart + fsWidth);
            libre[i] = slots.get(i).isFree() && !enBloque;
        }

        // Identificar bloques contiguos libres y sus tamaños
        List<Integer> tamanhosBloques = new ArrayList<>();
        int totalLibre = 0;
        int cont = 0;
        for (int i = 0; i < total; i++) {
            if (libre[i]) {
                cont++;
            } else {
                if (cont > 0) {
                    tamanhosBloques.add(cont);
                    totalLibre += cont;
                    cont = 0;
                }
            }
        }
        if (cont > 0) {
            tamanhosBloques.add(cont);
            totalLibre += cont;
        }

        if (totalLibre == 0 || tamanhosBloques.isEmpty()) {
            return 0.0; // sin slots libres → sin fragmentación adicional posible
        }

        double entropia = 0.0;
        for (int tam : tamanhosBloques) {
            double p = (double) tam / totalLibre;
            entropia -= p * (Math.log(p) / Math.log(2.0));
        }
        return entropia;
    }

    /**
     * Calcula el BFR (Band Fragmentation Ratio) del core simulando que el bloque
     * [fsStart, fsStart + fsWidth) pasa a estar ocupado.
     *
     * <p>{@code BFR = número_de_bloques_contiguos_libres / total_slots_libres}.
     *
     * <p>BFR = 0 → sin fragmentación (o sin slots libres). BFR mayor → más fragmentado.
     *
     * @return BFR residual
     */
    private static double calcularBFRCoreSiAsignado(Core core, int fsStart, int fsWidth) {
        List<FrequencySlot> slots = core.getFrequencySlots();
        int total = slots.size();

        // Construir máscara de slots libres simulando la asignación del bloque
        boolean[] libre = new boolean[total];
        for (int i = 0; i < total; i++) {
            boolean enBloque = (i >= fsStart && i < fsStart + fsWidth);
            libre[i] = slots.get(i).isFree() && !enBloque;
        }

        int numBloques = 0;
        int totalLibre = 0;
        boolean enBloqueLibre = false;
        for (int i = 0; i < total; i++) {
            if (libre[i]) {
                totalLibre++;
                if (!enBloqueLibre) {
                    numBloques++;
                    enBloqueLibre = true;
                }
            } else {
                enBloqueLibre = false;
            }
        }

        if (totalLibre == 0) {
            return 0.0; // sin slots libres, puntaje neutro
        }
        return (double) numBloques / totalLibre;
    }


    /**
     * Genera todos los órdenes posibles de núcleos siguiendo la heurística:
     * [ núcleos impares | núcleos pares | core 0 ]
     */
    public static List<Integer> heuristicCoresOrder() {

        int[] oddCores = {1, 3, 5};
        int[] evenCores = {2, 4, 6, 0};

        List<int[]> oddPerms = new ArrayList<>();
        List<int[]> evenPerms = new ArrayList<>();

        permute(oddCores, 0, oddPerms);
        permute(evenCores, 0, evenPerms);

        LinkedHashSet<Integer> ordered = new LinkedHashSet<>();

        for (int[] odd : oddPerms) {
            for (int[] even : evenPerms) {

                for (int v : odd) ordered.add(v);
                for (int v : even) ordered.add(v);
            }
        }

        return new ArrayList<>(ordered);
    }

    public static List<Integer> heuristicCoresOrderDual() {

        int[] oddCores = {1, 3, 5};
        int[] evenCores = {2, 4, 6};

        List<int[]> oddPerms = new ArrayList<>();
        List<int[]> evenPerms = new ArrayList<>();

        permute(oddCores, 0, oddPerms);
        permute(evenCores, 0, evenPerms);

        LinkedHashSet<Integer> ordered = new LinkedHashSet<>();

        // Caso A: impares → pares → 0
        for (int[] odd : oddPerms) {
            for (int[] even : evenPerms) {
                for (int v : odd) ordered.add(v);
                for (int v : even) ordered.add(v);
                ordered.add(0);
            }
        }

        // Caso B: pares → impares → 0
        for (int[] even : evenPerms) {
            for (int[] odd : oddPerms) {
                for (int v : even) ordered.add(v);
                for (int v : odd) ordered.add(v);
                ordered.add(0);
            }
        }

        return new ArrayList<>(ordered);
    }

    // Permutador genérico
    private static void permute(int[] arr, int index, List<int[]> result) {
        if (index == arr.length) {
            result.add(arr.clone());
            return;
        }

        for (int i = index; i < arr.length; i++) {
            swap(arr, index, i);
            permute(arr, index + 1, result);
            swap(arr, index, i);
        }
    }

    private static void swap(int[] arr, int i, int j) {
        int t = arr[i];
        arr[i] = arr[j];
        arr[j] = t;
    }

}
