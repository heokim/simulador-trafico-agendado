package py.una.pol.simulador.eon.models.enums;

public enum CoreSelectionEnum {
    RANDOM("cores aleatorios"),
    SORTED_BY_FREE_FS("cores ordenados por cantidad de FS libres"),
    SORTED_BY_ENTROPY("cores ordenados por entropía de Shannon"),
    SORTED_BY_BFR("cores ordenados por BFR"),
    HEURISTIC_V0("heurística core v0 [1, 2, 3, 4, 5, 6, 0]"),
    HEURISTIC_V1("heurística core v1 [1, 3, 5, 2, 4, 6, 0]"),
    HEURISTIC_ORDER("heurística heuristicCoresOrder"),
    HEURISTIC_ORDER_DUAL("heurística heuristicCoresOrderDual"),
    SEQUENTIAL("cores secuenciales [0, 1, 2, 3, 4, 5, 6]");

    private final String description;

    CoreSelectionEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
