package py.una.pol.simulador.eon.models;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class AllocationResult {
    private boolean success = false;
    private int fsIndex;
    private boolean crosstalkError = false;
    private boolean fragmentationError = false;
    private boolean capacityError = false;

    private List<Integer> assignedCores;
    private List<Integer> crosstalkNeighbors;
    private int maxDistance;
    
    private int coreSwitches = 0;
    private BigDecimal startCrosstalk = BigDecimal.ZERO;
    private BigDecimal avgCrosstalk = BigDecimal.ZERO;
    private BigDecimal totalCrosstalk = BigDecimal.ZERO;
}
