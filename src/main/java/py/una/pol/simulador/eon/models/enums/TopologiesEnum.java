package py.una.pol.simulador.eon.models.enums;

/**
 * Enumerator for the topologies supported on the simulator
 *
 * @author Néstor E. Reinoso Wood
 */
public enum TopologiesEnum {

    /**
     * Topología USNet
     */
    USNET("USNET", "topologies/usnet.json"),
    /**
     * Topología NSFNET
     */
    NSFNET("NSFNET", "topologies/nsfnet.json"),
    /**
     * Topología EUNET
     */
    EUNET("EUNET", "topologies/eunet.json"),
    /**
     * Topología JPNNET
     */
    JPNNET("JPNNET", "topologies/jpn-net.json"),
    /*
     * Topologia de prueba Cuadrado homogeneo
     */
    CUADRADO("CUADRADO", "topologies/cuadrado.json");

    private final String label;

    private final String filePath;

    /**
     * Etiqueta de la topología
     *
     * @return Etiqueta de la topología
     */
    public String label() {
        return label;
    }

    /**
     * Ubicación del archivo de configuración
     *
     * @return Ubicación del archivo de configuración
     */
    public String filePath() {
        return filePath;
    }

    /**
     * Enum constructor
     *
     * @param label Label of the topology
     * @param filePath Path of the resource file used to create the topology
     */
    private TopologiesEnum(String label, String filePath) {
        this.label = label;
        this.filePath = filePath;
    }
}
