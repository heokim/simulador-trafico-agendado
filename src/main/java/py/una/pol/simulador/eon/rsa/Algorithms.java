package py.una.pol.simulador.eon.rsa;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.KShortestSimplePaths;
import py.una.pol.simulador.eon.SimulatorTest;
import py.una.pol.simulador.eon.models.*;
import py.una.pol.simulador.eon.models.enums.RouteSelectionStrategy;
import py.una.pol.simulador.eon.utils.Utils;

/**
 * Algorithms for RSA (Routing and Spectrum Assignment) in EON.
 * 
 * @author Néstor E. Reinoso Wood
 */
public class Algorithms {

    /**
     * Versión Paralela Random Fit del algoritmo ruteoCoreMultipleAgendado.
     * Main entry point for the RSA algorithm.
     */
    public static EstablishedRoute ruteoCoreMultipleAgendado(Graph<Integer, Link> graph, Demand demand, Integer capacity, Integer cores, BigDecimal maxCrosstalk, Double crosstalkPerUnitLength, RouteSelectionStrategy strategy) {
        KShortestSimplePaths<Integer, Link> kspFinder = new KShortestSimplePaths<>(graph);
        List<GraphPath<Integer, Link>> kspPaths = kspFinder.getPaths(demand.getSource(), demand.getDestination(), 5);

        ordenarKShortestPaths(kspPaths);

        AtomicBoolean flag_crosstalk = new AtomicBoolean(false);
        AtomicBoolean flag_frag = new AtomicBoolean(false);
        AtomicBoolean flag_capacidad = new AtomicBoolean(false);

        // 1. Iterar sobre los K caminos más cortos candidatos (Path Selection)
        for (GraphPath<Integer, Link> path : kspPaths) {

            // Crear bloques de 24 FS
            int blockSize = SimulatorTest.blockSize;
            int maxFsIndex = capacity - demand.getFs();
            List<List<Integer>> fsBlocks = new ArrayList<>();

            for (int i = 0; i <= maxFsIndex; i += blockSize) {
                List<Integer> block = new ArrayList<>();
                int end = Math.min(i + blockSize, maxFsIndex + 1);
                for (int j = i; j < end; j++) {
                    block.add(j);
                }
                fsBlocks.add(block);
            }

            // Aleatorizar el orden de los bloques
            Collections.shuffle(fsBlocks);

            // Iterar sobre los bloques aleatorizados
            for (List<Integer> blockIndices : fsBlocks) {
                // Parallel Search dentro del bloque actual
                // Usamos min() con un Comparator para encontrar el "mejor" candidato dentro del bloque
                Optional<AllocationResult> resultOpt = blockIndices.parallelStream()
                        .map(fsIndex -> tryAllocatePath(path, fsIndex, demand, cores, maxCrosstalk, crosstalkPerUnitLength))
                        .peek(res -> {
                            if (!res.isSuccess()) {
                                if (res.isCrosstalkError()) flag_crosstalk.set(true);
                                if (res.isFragmentationError()) flag_frag.set(true);
                                if (res.isCapacityError()) flag_capacidad.set(true);
                            }
                        })
                        .filter(AllocationResult::isSuccess)
                        .min(getComparator(strategy));

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
    private static AllocationResult tryAllocatePath(GraphPath<Integer, Link> path, int fsIndex, Demand demand, Integer totalCores, BigDecimal maxCrosstalk, Double crosstalkPerUnitLength) {
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
            // Obtener núcleos ordenados (Estrategia: Least Loaded / Prioritize non-core-0)
            List<Integer> sortedCores = getSortedCoresByFreeFS(link);

            // variante para solo buscar en los primeros 3 núcleos mas libres
            for (int core : sortedCores.subList(0, 3)) {
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
                for(int i=0; i<demand.getFs(); i++) {
                    BigDecimal newVal = tempCrosstalk.get(i).add(linkXT);
                    tempCrosstalk.set(i, newVal);
                    if (newVal.compareTo(maxCrosstalk) > 0) limitExceeded = true;
                }

                // Regla especial de tolerancia: Solo permitir exceder si no hay vecinos activos (supuesto del modelo)
                if (limitExceeded && activeNeighbors > 0) {
                    result.setCrosstalkError(true);
                    continue;
                }

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
        
        // Calcular Métricas Finales
        int coreSwitches = 0;
        BigDecimal totalXT = BigDecimal.ZERO;
        
        // Calcular Core Switches
        for (int i = 0; i < currentCores.size() - 1; i++) {
             if (!currentCores.get(i).equals(currentCores.get(i+1))) {
                 coreSwitches++;
             }
        }
        
        // Calcular Total XT y Avg XT
        // Nota: routeCrosstalkPerFS contiene el XT acumulado por FS.
        // Para tener una metrica de la ruta, podemos sumar los maximos de cada FS o promediarlos.
        // O alternativamente, sumar el XT agregado en cada salto (que no guardamos explicitamente en la lista final, pero podemos inferir o calcular).
        // Sin embargo, una metrica razonable es el PROMEDIO del XT acumulado en todos los FS utilizados al final de la ruta.

        BigDecimal sumXT = BigDecimal.ZERO;
        for (BigDecimal xtVal : routeCrosstalkPerFS) {
            sumXT = sumXT.add(xtVal);
        }
        // Usamos el promedio de los FS
        BigDecimal avgOverFS = sumXT.divide(new BigDecimal(demand.getFs()), java.math.RoundingMode.HALF_UP);
        totalXT = avgOverFS; // Simplificacion: Usamos el promedio por FS como metrica de "Total XT" percibido


        result.setSuccess(true);
        result.setAssignedCores(currentCores);
        result.setCrosstalkNeighbors(neighborCounts);
        result.setMaxDistance(maxDist);
        result.setCoreSwitches(coreSwitches);
        result.setTotalCrosstalk(totalXT);
        result.setAvgCrosstalk(totalXT.divide(new BigDecimal(links.size()), java.math.RoundingMode.HALF_UP)); // Avg por enlace

        return result;
    }

    // <editor-fold desc="Helper methods for spectrum assignment and crosstalk calculation" defaultstate="collapsed">
    private static Comparator<AllocationResult> getComparator(RouteSelectionStrategy strategy) {
        switch (strategy) {
            case MIN_CORE_SWITCHES:
                return Comparator.comparingInt(AllocationResult::getCoreSwitches);
            case MIN_TOTAL_XT:
                return Comparator.comparing(AllocationResult::getTotalCrosstalk);
            case MIN_AVG_XT:
                return Comparator.comparing(AllocationResult::getAvgCrosstalk);
            
            // Pairs
            case MIN_CS_THEN_TOTAL_XT:
                return Comparator.comparingInt(AllocationResult::getCoreSwitches)
                        .thenComparing(AllocationResult::getTotalCrosstalk);
            case MIN_CS_THEN_AVG_XT:
                return Comparator.comparingInt(AllocationResult::getCoreSwitches)
                        .thenComparing(AllocationResult::getAvgCrosstalk);
            case MIN_TOTAL_XT_THEN_CS:
                return Comparator.comparing(AllocationResult::getTotalCrosstalk)
                        .thenComparingInt(AllocationResult::getCoreSwitches);
            case MIN_AVG_XT_THEN_CS:
                return Comparator.comparing(AllocationResult::getAvgCrosstalk)
                        .thenComparingInt(AllocationResult::getCoreSwitches);
            case MIN_TOTAL_XT_THEN_AVG_XT:
                return Comparator.comparing(AllocationResult::getTotalCrosstalk)
                        .thenComparing(AllocationResult::getAvgCrosstalk);
            case MIN_AVG_XT_THEN_TOTAL_XT:
                return Comparator.comparing(AllocationResult::getAvgCrosstalk)
                         .thenComparing(AllocationResult::getTotalCrosstalk);

            // Triples
            case MIN_CS_THEN_TOTAL_XT_THEN_AVG_XT:
                 return Comparator.comparingInt(AllocationResult::getCoreSwitches)
                        .thenComparing(AllocationResult::getTotalCrosstalk)
                        .thenComparing(AllocationResult::getAvgCrosstalk);
            case MIN_CS_THEN_AVG_XT_THEN_TOTAL_XT:
                 return Comparator.comparingInt(AllocationResult::getCoreSwitches)
                        .thenComparing(AllocationResult::getAvgCrosstalk)
                        .thenComparing(AllocationResult::getTotalCrosstalk);
            case MIN_TOTAL_XT_THEN_CS_THEN_AVG_XT:
                 return Comparator.comparing(AllocationResult::getTotalCrosstalk)
                        .thenComparingInt(AllocationResult::getCoreSwitches)
                        .thenComparing(AllocationResult::getAvgCrosstalk);
            case MIN_TOTAL_XT_THEN_AVG_XT_THEN_CS:
                 return Comparator.comparing(AllocationResult::getTotalCrosstalk)
                        .thenComparing(AllocationResult::getAvgCrosstalk)
                        .thenComparingInt(AllocationResult::getCoreSwitches);
            case MIN_AVG_XT_THEN_CS_THEN_TOTAL_XT:
                 return Comparator.comparing(AllocationResult::getAvgCrosstalk)
                        .thenComparingInt(AllocationResult::getCoreSwitches)
                        .thenComparing(AllocationResult::getTotalCrosstalk);
            case MIN_AVG_XT_THEN_TOTAL_XT_THEN_CS:
                 return Comparator.comparing(AllocationResult::getAvgCrosstalk)
                        .thenComparing(AllocationResult::getTotalCrosstalk)
                        .thenComparingInt(AllocationResult::getCoreSwitches);

            // Fragmentation Strategies
            case MIN_FS_INDEX:
                return Comparator.comparingInt(AllocationResult::getFsIndex);
            case MIN_CS_THEN_FS_INDEX:
                return Comparator.comparingInt(AllocationResult::getCoreSwitches)
                        .thenComparingInt(AllocationResult::getFsIndex);
            case MIN_AVG_XT_THEN_FS_INDEX:
                return Comparator.comparing(AllocationResult::getAvgCrosstalk)
                        .thenComparingInt(AllocationResult::getFsIndex);
            case MIN_TOTAL_XT_THEN_FS_INDEX:
                return Comparator.comparing(AllocationResult::getTotalCrosstalk)
                        .thenComparingInt(AllocationResult::getFsIndex);

            default:
                 return Comparator.comparing(AllocationResult::getFsIndex);
        }
    }

    private static void ordenarKShortestPaths(List<GraphPath<Integer, Link>> kspPaths) {
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
            boolean tied = false;
            for (int coreIdx : coresByFreeFS) {
                if (coreIdx != 0 && freeCounts[coreIdx] == core0Free) {
                    tied = true;
                    break;
                }
            }
            if (tied) {
                coresByFreeFS.remove(Integer.valueOf(0));
                coresByFreeFS.add(0);
            }
        }
        return coresByFreeFS;
    }

    private static Boolean isFSBlockFree(List<FrequencySlot> bloqueFS) {
        for (FrequencySlot fs : bloqueFS) {
            if (!fs.isFree()) {
                return false;
            }
        }
        return true;
    }

    private static Boolean isFsBlockCrosstalkFree(Link link, int core, int index, List<FrequencySlot> fss, BigDecimal maxCrosstalk, List<BigDecimal> crosstalkRuta) {
        int v_crosstalk = CalculaVecinosConCrosstalk(link, core, index, fss.size());

        for (int j = index; j < fss.size(); j++) {
            BigDecimal crosstalkActual = crosstalkRuta.get(j).add(fss.get(j).getCrosstalk());
            if (crosstalkActual.compareTo(maxCrosstalk) > 0) {
                if (v_crosstalk > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private static Boolean isNextToCrosstalkFreeCores(Link link, BigDecimal maxCrosstalk, Integer core, Integer fsIndexBegin, Integer fsWidth, Double crosstalkPerUnitLength) {
        List<Integer> vecinos = Utils.getCoreVecinos(core);
        int v_crosstalk = CalculaVecinosConCrosstalk(link, core, fsIndexBegin, fsWidth);
        for (Integer coreVecino : vecinos) {
            for (Integer i = fsIndexBegin; i < fsIndexBegin + fsWidth; i++) {
                FrequencySlot fsVecino = link.getCores().get(coreVecino).getFrequencySlots().get(i);
                if (!fsVecino.isFree()) {
                    BigDecimal crosstalkASumar = Utils.toDB(Utils.XT(v_crosstalk, crosstalkPerUnitLength, link.getDistance()));
                    BigDecimal crosstalk = fsVecino.getCrosstalk().add(crosstalkASumar);
                    if (crosstalk.compareTo(maxCrosstalk) >= 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static int CalculaVecinosConCrosstalk(Link link, Integer core, Integer fsIndexBegin, Integer fsWidth) {
        Integer vecino_afectado = 0;
        List<Integer> vecinos = Utils.getCoreVecinos(core);
        for (Integer coreVecino : vecinos) {
            for (Integer i = fsIndexBegin; i < fsIndexBegin + fsWidth; i++) {
                FrequencySlot fsVecino = link.getCores().get(coreVecino).getFrequencySlots().get(i);
                if (!fsVecino.isFree()) {
                    vecino_afectado++;
                    break;
                }
            }
        }
        return vecino_afectado;
    }

    private static void Assigna_idruta(EstablishedRoute establishedRoute) {
        for (int i = 0; i < establishedRoute.getPath().size(); i++) {
            int core_index = establishedRoute.getPathCores().get(i);
            establishedRoute.getPath().get(i).getCores().get(core_index).getId_rutas().add(establishedRoute.getId());
        }
    }
    // </editor-fold>

    // <editor-fold desc="Deprecated Methods" defaultstate="collapsed">
    @Deprecated
    /**
     * @deprecated Use isFsBlockCrosstalkFree instead.
     */
    private static Boolean BloqueFsToleraCrosstalkFinal(List<List<FrequencySlot>> bloques, int index, List<Link> enlaces, List<Integer> Cores, int tamanhobloque, BigDecimal maxCrosstalk, List<BigDecimal> crosstalkRuta) {
        int indice = 0; 
        int v_crosstalk = 0;

        for (List<FrequencySlot> bloque : bloques) {
            Link enlace = enlaces.get(indice);
            Integer core = Cores.get(indice);
            v_crosstalk = CalculaVecinosConCrosstalk(enlace, core, index, bloque.size());
            for (int i = index; i < bloque.size(); i++) {
                BigDecimal crosstalkActual = crosstalkRuta.get(i).add(bloque.get(i).getCrosstalk());
                if (crosstalkActual.compareTo(maxCrosstalk) > 0) {
                    if (v_crosstalk > 0) 
                    {
                        return false; 
                    }
                }
            }
            indice++;
        }
        return true;
    }

    @Deprecated
    /**
     * @deprecated Use isNextToCrosstalkFreeCores instead.
     */
    private static boolean ToleraCrosstalkVecinos(List<Integer> cores, List<Link> enlaces, BigDecimal maxCrosstalk, int fsIndexBegin, int fsWidth, BigDecimal crosstalkRuta) {
        for (int j = 0; j < cores.size(); j++) {
            List<Integer> vecinos = Utils.getCoreVecinos(j);
            for (Integer coreVecino : vecinos) {
                for (Integer i = fsIndexBegin; i < fsIndexBegin + fsWidth; i++) {
                    FrequencySlot fsVecino = enlaces.get(j).getCores().get(coreVecino).getFrequencySlots().get(i);
                    if (!fsVecino.isFree()) {
                        BigDecimal crosstalk = fsVecino.getCrosstalk().add(crosstalkRuta);
                        if (crosstalk.compareTo(maxCrosstalk) >= 0) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }
    // </editor-fold>

}
