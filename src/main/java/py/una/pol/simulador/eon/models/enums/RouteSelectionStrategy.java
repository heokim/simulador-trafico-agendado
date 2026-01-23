package py.una.pol.simulador.eon.models.enums;

/**
 * Strategy for route selection in the EON simulator.
 */
public enum RouteSelectionStrategy {
    // Single Metric
    MIN_CORE_SWITCHES,
    MIN_TOTAL_XT,
    MIN_AVG_XT,

    // Two Metrics
    MIN_CS_THEN_TOTAL_XT,
    MIN_CS_THEN_AVG_XT,
    MIN_TOTAL_XT_THEN_CS,
    MIN_AVG_XT_THEN_CS,
    MIN_TOTAL_XT_THEN_AVG_XT,
    MIN_AVG_XT_THEN_TOTAL_XT,

    // Three Metrics
    MIN_CS_THEN_TOTAL_XT_THEN_AVG_XT,
    MIN_CS_THEN_AVG_XT_THEN_TOTAL_XT,
    MIN_TOTAL_XT_THEN_CS_THEN_AVG_XT,
    MIN_TOTAL_XT_THEN_AVG_XT_THEN_CS,
    MIN_AVG_XT_THEN_CS_THEN_TOTAL_XT,
    MIN_AVG_XT_THEN_TOTAL_XT_THEN_CS,

    // Fragmentation Aware Strategies
    // First-Fit behavior (packing)
    MIN_FS_INDEX,
    MIN_CS_THEN_FS_INDEX,
    MIN_AVG_XT_THEN_FS_INDEX,
    MIN_TOTAL_XT_THEN_FS_INDEX
}
