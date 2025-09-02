package py.una.pol.simulador.eon;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.jgrapht.Graph;

import py.una.pol.simulador.eon.models.AssignFsResponse;
import py.una.pol.simulador.eon.models.Demand;
import py.una.pol.simulador.eon.models.EstablishedRoute;
import py.una.pol.simulador.eon.models.Input;
import py.una.pol.simulador.eon.models.Link;
import py.una.pol.simulador.eon.models.enums.RSAEnum;
import py.una.pol.simulador.eon.models.enums.TopologiesEnum;
import py.una.pol.simulador.eon.rsa.Algorithms;
import py.una.pol.simulador.eon.utils.Database;
import py.una.pol.simulador.eon.utils.MathUtils;
import py.una.pol.simulador.eon.utils.Utils;
import py.una.pol.simulador.eon.utils.GraphUtils;

import java.util.Scanner;

/**
 *
 * @author Néstor E. Reinoso Wood
 */
public class SimulatorTest {

    /*
     * Variables globales
     * para identificar tipos de bloqueos
     */
    public static int contador_crosstalk = 0;
    public static int contador_frag = 0;
    public static int contador_frag_ruta = 0;

    public static Database databaseUtil = new Database();

    /**
     * Simulador
     *
     * @param args Argumentos de entrada (Vacío)
     */
    public static void main(String[] args) throws SQLException {
        databaseUtil.setConnection();

        // cuando tiempo tarda en ejecutar todo el programa
        long startTime = System.currentTimeMillis();
        try {
            // Datos de entrada
            int valor_erlang = Obtiene_Erlang();
            // Volumen de Tráfico promedio (V T): representa el volumen del tráfico promedio
            // en cada instante de tiempo T dentro de la red, medido en erlangs
            for (int erlang = valor_erlang; erlang <= valor_erlang; erlang = erlang + valor_erlang) {
                // Se obtienen los datos de entrada
                Input input = new SimulatorTest().getTestingInput(erlang);
                // Iteración de topologías a simular
                for (TopologiesEnum topology : input.getTopologies()) {
                    // Se genera la red de acuerdo a los datos de entrada
                    Graph<Integer, Link> graph = Utils.createTopology(topology, input.getCores(), input.getFsWidth(), input.getCapacity(), input.getF(), input.getNumero_h());
                    GraphUtils.createImage(graph, topology.label());
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
                                i
                        );
                        demandsQ += demands.size();
                        listaDemandas.add(demands);
                    }

                    for (Double crosstalkPerUnitLength : input.getCrosstalkPerUnitLenghtList()) {
                        for (RSAEnum algorithm : input.getAlgorithms()) {
                            graph = Utils.createTopology(topology, input.getCores(), input.getFsWidth(), input.getCapacity(), input.getF(), input.getNumero_h());
                            // Lista de rutas establecidas durante la simulación
                            List<EstablishedRoute> establishedRoutes = new ArrayList<>();
                            System.out.println("Inicializando simulación del RSA " + algorithm.label() + " para erlang: " + (erlang) + " para la topología " + topology.label() + " y H = " + crosstalkPerUnitLength.toString());
                            int demandaNumero = 1;
                            Integer camino = null;
                            int rutas = 0;
                            int bloqueos = 0;
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
                            for (int i = 0; i < input.getSimulationTime(); i++) {
                                System.out.println("Tiempo: " + (i + 1));
                                // Generación de demandas para la unidad de tiempo
                                List<Demand> demands = listaDemandas.get(i);
                                //System.out.println("Demandas a insertar: " + demands.size());
                                for (Demand demand : demands) {
                                    demandaNumero++;
                                    //System.out.println("Insertando demanda " + demandaNumero++);
                                    //k caminos más cortos entre source y destination de la demanda actual
                                    EstablishedRoute establishedRoute;
                                    switch (algorithm) {
                                        case MULTIPLES_CORES -> {
                                            establishedRoute = Algorithms.ruteoCoreMultiple(graph, demand, input.getCapacity(), input.getCores(), input.getMaxCrosstalk(), crosstalkPerUnitLength);
                                        }
                                        default -> {
                                            establishedRoute = null;
                                        }
                                    }
                                    if (establishedRoute == null || establishedRoute.getFsIndexBegin() == -1) {
                                        //Bloqueo
                                        System.out.println("BLOQUEO");
                                        demand.setBlocked(true);
                                        databaseUtil.insertarBloqueo(algorithm.label(), topology.label(), "" + i, "" + demand.getId(), "" + erlang, crosstalkPerUnitLength.toString());
                                        bloqueos++;
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

                                        rutas++;
                                        System.out.println("Ruta: " + rutas);
                                        System.out.println("Cores: " + establishedRoute.getPathCores());
                                        AssignFsResponse response = Utils.assignFs(graph, establishedRoute, crosstalkPerUnitLength);
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
                                        Utils.deallocateFs(graph, route, crosstalkPerUnitLength);
                                        establishedRoutes.remove(ri);
                                        ri--;
                                    }
                                }
                            }
                            //Determina los datos para ingresar a la base de datos
                            // los motivos de bloqueos
                            String motivo_bloqueo = MotivoBloqueo(contador_frag, contador_crosstalk);
                            String porcentaje_motivo = PorcentajeMotivo(bloqueos, contador_frag, contador_crosstalk);
                            String porcentaje = PorcentajeBloqueo(demandaNumero, bloqueos);
                            String tipo_erlang = TipoErlang(porcentaje);
                            databaseUtil.insertarResumen(topology.label(), "" + erlang, tipo_erlang, input.getNumero_h(), crosstalkPerUnitLength.toString(), "" + bloqueos, motivo_bloqueo, porcentaje_motivo, porcentaje, "" + rutas, "" + Diametro, "" + prom_grado,
                                    "" + longitud_promedio, "" + String.valueOf(input.getF()));
                            System.out.println("---------------------------------");
                            System.out.println("\nTopologia" + input.getTopologies() + "\n");
                            System.out.println("TOTAL DE BLOQUEOS: " + bloqueos);
                            System.out.println("TOTAL DE RUTAS: " + rutas);
                            System.out.println("Cantidad de demandas: " + demandaNumero);
                            System.out.println("\nRESUMEN DE DATOS \n");
                            System.out.printf("Resumen de caminos:\nk1:%d\nk2:%d\nk3:%d\nk4:%d\nk5:%d\n", k1, k2, k3, k4, k5);
                            System.out.printf("Resumen de bloqueos:\n fragmentacion = %d \n crosstalk = %d\n fragmentacion de camino = %d\n", contador_frag, contador_crosstalk, contador_frag_ruta);
                            System.out.printf("\nEl diametro del grafo es :  %d kms\n", Diametro);
                            System.out.printf("\nEl grado promedio: %d", prom_grado);
                            System.out.println(System.lineSeparator());
                        }
                    }
                }
            }
        } catch (IOException | IllegalArgumentException ex) {
            System.out.println(ex.getMessage());
        }
        // fin programa
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        System.out.println("Tiempo de ejecución: " + duration / 1000 + " segundos");
        // en formato horas, minutos y segundos
        long hours = duration / 3600000;
        long minutes = (duration % 3600000) / 60000;
        long seconds = (duration % 60000) / 1000;
        System.out.println("Tiempo de ejecución: " + hours + " horas, " + minutes + " minutos y " + seconds + " segundos");

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
        // Declaro las variables iniciales
        Scanner scanner = new Scanner(System.in);
        boolean valid = false;

        // se ingresa la topologia
        input.setTopologies(new ArrayList<>());
        while (!valid) {
            System.out.println("Ingrese el nombre de la topología (NSFNET, USNET, JPNNET):");
            String userInput = scanner.nextLine().trim().toUpperCase();
            try {
                TopologiesEnum selectedTopology = TopologiesEnum.valueOf(userInput);
                input.getTopologies().add(selectedTopology);
                System.out.println("Topología agregada: " + selectedTopology);
                valid = true;
            } catch (IllegalArgumentException e) {
                // Si no es válido, muestra mensaje y vuelve a pedir
                System.out.println("Entrada inválida. Por favor, ingrese una de las siguientes opciones: NSFNET, USNET, JPNNET.\n");
            }
        }

        valid = false;

        // se ingresa el crosstalk por unidad de longitud
        input.setCrosstalkPerUnitLenghtList(new ArrayList<>());
        while (!valid) {
            System.out.println("Ingrese el tipo de crosstak por unidad de longitud (h1,h2 o h3)");
            String userInput = scanner.nextLine().trim().toUpperCase();
            if (userInput.equals("h3") || userInput.equals("H3")) {
                input.setNumero_h("h3");
                input.getCrosstalkPerUnitLenghtList().add((2 * Math.pow(0.0000316, 2) * 0.055) / (4000000 * 0.000045));
                valid = true;
            } else if (userInput.equals("h2") || userInput.equals("H2")) {
                input.setNumero_h("h2");
                input.getCrosstalkPerUnitLenghtList().add((2 * Math.pow(0.00040, 2) * 0.050) / (4000000 * 0.000040));
                valid = true;
            } else if (userInput.equals("h1") || userInput.equals("H1")) {
                input.setNumero_h("h1");
                input.getCrosstalkPerUnitLenghtList().add((2 * Math.pow(0.0035, 2) * 0.080) / (4000000 * 0.000045));
                valid = true;
            } else {
                System.out.println("Entrada inválida. Por favor, ingrese una de las siguientes opciones: h1, h2, h3.\n");
            }
        }

        // se ingresa el factor 
        valid = false;
        while (!valid) {
            System.out.print("Ingrese un número decimal (por ejemplo 2.0): ");
            String entrada = scanner.nextLine().trim();
            try {
                double valor = Double.parseDouble(entrada); // Intenta convertir la entrada a double
                valid = true;
                input.setF(valor);
                System.out.println("Valor ingresado correctamente: " + valor);
            } catch (NumberFormatException e) {
                System.out.println("Entrada inválida. Por favor, ingrese un número válido.\n");
            }
        }
        scanner.close();

        input.setDemands(100000);
        input.setFsWidth(new BigDecimal("12.5"));
        input.setFsRangeMax(8);
        input.setFsRangeMin(2);
        input.setCapacity(325);
        input.setCores(7);
        input.setLambda(5);
        input.setErlang(erlang);
        input.setAlgorithms(new ArrayList<>());
        //input.getAlgorithms().add(RSAEnum.CORE_UNICO);
        input.getAlgorithms().add(RSAEnum.MULTIPLES_CORES);
        input.setSimulationTime(MathUtils.getSimulationTime(input.getDemands(), input.getLambda()));
        input.setMaxCrosstalk(new BigDecimal("0.003162277660168379331998893544")); // XT = -25 dB
        return input;
    }

    /**
     * Obtiene el valor para el erlang
     */
    public static int Obtiene_Erlang() {
        boolean valid = false;
        Scanner scanner = new Scanner(System.in);
        int valor = 0;
        while (!valid) {
            System.out.println("Ingrese el valor para el erlang: ");
            String entrada = scanner.nextLine().trim();
            try {
                valor = Integer.parseInt(entrada); // Intenta convertir la entrada a double
                valid = true;
                System.out.println("Valor ingresado correctamente: " + valor);

            } catch (NumberFormatException e) {
                System.out.println("Entrada inválida. Por favor, ingrese un número válido.\n");
            }
        }
        return valor;
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

    /**
     * Inserta los datos en la BD
     *
     * @param rsa       Algoritmo RSA utilizado
     * @param topologia Topología de la red
     * @param tiempo    Tiempo del bloqueo
     * @param demanda   Demanda bloqueada
     * @param erlang    Erlang de la simulación
     * @param h         Crosstalk por unidad de longitud de la simulación
     */
    @Deprecated
    public static void insertData(String rsa, String topologia, String tiempo, String demanda, String erlang, String h) {
        Connection c;
        Statement stmt;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:simulador.db");
            c.setAutoCommit(false);
            stmt = c.createStatement();
            String sql = "INSERT INTO Bloqueos (rsa, topologia, tiempo, demanda, erlang, h) "
                    + "VALUES ('" + rsa + "','" + topologia + "', '" + tiempo + "' ,'" + demanda + "', " + "'" + erlang + "', " + "'" + h + "')";
            stmt.executeUpdate(sql);
            stmt.close();
            c.commit();
            c.close();
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }

    /**
     * Funcion que inserta los datos en la Base de datos de resumen
     *
     * @param topologia
     */
    @Deprecated
    public static void InsertaDatos(String topologia, String erlang, String tipo_erlang, String h, String valor_h,
                                    String bloqueos, String motivo_Bloqueo, String porcentaje_motivo, String porcentaje_Bloqueo,
                                    String rutas, String diametro, String grado, String long_promedio, String factor) {
        Connection conexion = null;
        PreparedStatement stmt = null;
        try {
            // Cargar el driver de SQLite
            Class.forName("org.sqlite.JDBC");
            // Establecer conexión con la base de datos SQLite
            conexion = DriverManager.getConnection("jdbc:sqlite:Resumen.db");
            // Desactivar auto-commit para control de transacciones
            conexion.setAutoCommit(false);
            // Consulta SQL con placeholders (?) para evitar errores de sintaxis e inyección SQL
            String sql = "INSERT INTO Resumen (topologia, erlang, tipo_erlang, h, valor_h, bloqueos, motivo_Bloqueo, porcentaje_motivo,porcentaje_Bloqueo, rutas, diametro, grado, long_promedio, factor) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? , ?, ?, ?)";
            // Crear el PreparedStatement y asignar valores
            stmt = conexion.prepareStatement(sql);
            stmt.setString(1, topologia);
            stmt.setString(2, erlang);
            stmt.setString(3, tipo_erlang);
            stmt.setString(4, h);
            stmt.setString(5, valor_h);
            stmt.setString(6, bloqueos);
            stmt.setString(7, motivo_Bloqueo);
            stmt.setString(8, porcentaje_motivo);
            stmt.setString(9, porcentaje_Bloqueo);
            stmt.setString(10, rutas);
            stmt.setString(11, diametro);
            stmt.setString(12, grado);
            stmt.setString(13, long_promedio);
            stmt.setString(14, factor);
            // Ejecutar la inserción
            stmt.executeUpdate();
            // Confirmar la transacción
            conexion.commit();
            System.out.println("¡Datos insertados correctamente!");
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Error: " + e.getMessage());
            try {
                if (conexion != null) {
                    conexion.rollback();  // Deshacer cambios en caso de error
                }
            } catch (SQLException rollbackEx) {
                System.out.println("Error al hacer rollback: " + rollbackEx.getMessage());
            }
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conexion != null) conexion.close();
            } catch (SQLException closeEx) {
                System.out.println("Error al cerrar la conexión: " + closeEx.getMessage());
            }
        }
    }

}

