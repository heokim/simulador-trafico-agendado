package py.una.pol.simulador.eon.models.enums;

public enum XTPerUnitLenght {

    H1((2 * Math.pow(0.0035, 2) * 0.080) / (4000000 * 0.000045)),
    H2((2 * Math.pow(0.00040, 2) * 0.050) / (4000000 * 0.000040)),
    H3((2 * Math.pow(0.0000316, 2) * 0.055) / (4000000 * 0.000045));

    private final double value;

    XTPerUnitLenght(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    public static XTPerUnitLenght fromString(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Input nulo");
        }
        switch (input.trim().toUpperCase()) {
            case "H1":
                return H1;
            case "H2":
                return H2;
            case "H3":
                return H3;
            default:
                throw new IllegalArgumentException("Valor inválido: " + input + ". Use H1, H2 o H3.");
        }
    }
}
