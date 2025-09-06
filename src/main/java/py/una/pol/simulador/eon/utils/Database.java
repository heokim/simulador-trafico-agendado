package py.una.pol.simulador.eon.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Database {

    private static Connection connection;
    private static final String URL = "jdbc:postgresql://localhost:5432/tesis";
    private static final String USER = "postgres";
    private static final String PASSWORD = "postgres";

    public static void openConnection() throws SQLException {
        connection = DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
            } catch (SQLException e) {
                System.out.println("❌ Error al cerrar conexión: " + e.getMessage());
            }
        }
    }

    public static int insertarBloqueo(String topologia, String tiempo,
                                      String demanda, String erlang, String h) {
        String sql = "INSERT INTO bloqueos ( topologia, tiempo, demanda, erlang, h) "
                + "VALUES ( ?, ?, ?, ?, ?)";
        int filas = 0;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, topologia);
            stmt.setString(2, tiempo);
            stmt.setString(3, demanda);
            stmt.setString(4, erlang);
            stmt.setString(5, h);
            filas = stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("❌ Error al insertar bloqueo: " + e.getMessage());
        }
        return filas;
    }

    public static void insertarResumen(String topologia, String erlang, String tipo_erlang,
                                       String h, String valor_h, String bloqueos, String motivo_Bloqueo,
                                       String porcentaje_motivo, String porcentaje_Bloqueo,
                                       String rutas, String diametro, String grado,
                                       String long_promedio, String factor) {
        String sql = "INSERT INTO Resumen (topologia, erlang, tipo_erlang, h, valor_h, bloqueos, " +
                "motivo_Bloqueo, porcentaje_motivo, porcentaje_Bloqueo, rutas, diametro, grado, " +
                "long_promedio, factor) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
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
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("❌ Error al insertar Resumen: " + e.getMessage());
        }
    }

    public static void insertSimulacionResumen(SimulacionResumen sim) {
        String sql = "INSERT INTO simulacion_resumen(" +
                "tiempo_inicio, tiempo_fin, tiempo_ejecucion, duracion, topologia, " +
                "numero_de_bloqueos, numero_de_rutas_establecidas, numero_de_desmandas_pospuestas, cantidad_de_demandas, " +
                "k1, k2, k3, k4, k5, " +
                "numero_de_bloqueos_por_fragmentacion, numero_de_bloqueos_por_crosstalk, numero_de_bloqueos_por_fragmentacion_de_camino, " +
                "diametro_grafo, grado_promedio, " +
                "input_demands, input_valor_h, input_decimal, input_fs_width, input_fs_range_max, input_fs_range_min, " +
                "input_capacity, input_cores, input_lambda, input_simulation_time, input_max_crosstalk, " +
                "input_t_range_min, input_t_range_max, input_erlang, input_xt_per_unit_length" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setTimestamp(1, sim.getTiempoInicio());
            ps.setTimestamp(2, sim.getTiempoFin());
            ps.setLong(3, sim.getTiempoEjecucion());
            ps.setString(4, sim.getDuracion());
            ps.setString(5, sim.getTopologia());

            ps.setInt(6, sim.getNumeroDeBloqueos());
            ps.setInt(7, sim.getNumeroDeRutasEstablecidas());
            ps.setInt(8, sim.getNumeroDeDemandasPospuestas());
            ps.setInt(9, sim.getCantidadDeDemandas());

            ps.setInt(10, sim.getK1());
            ps.setInt(11, sim.getK2());
            ps.setInt(12, sim.getK3());
            ps.setInt(13, sim.getK4());
            ps.setInt(14, sim.getK5());

            ps.setInt(15, sim.getNumeroDeBloqueosPorFragmentacion());
            ps.setInt(16, sim.getNumeroDeBloqueosPorCrosstalk());
            ps.setInt(17, sim.getNumeroDeBloqueosPorFragmentacionDeCamino());

            ps.setInt(18, sim.getDiametroGrafo());
            ps.setInt(19, sim.getGradoPromedio());

            ps.setInt(20, sim.getInputDemands());
            ps.setString(21, sim.getInputValorH());
            ps.setBigDecimal(22, sim.getInputDecimal());
            ps.setBigDecimal(23, sim.getInputFsWidth());
            ps.setInt(24, sim.getInputFsRangeMax());
            ps.setInt(25, sim.getInputFsRangeMin());
            ps.setInt(26, sim.getInputCapacity());
            ps.setInt(27, sim.getInputCores());
            ps.setInt(28, sim.getInputLambda());
            ps.setInt(29, sim.getInputSimulationTime());
            ps.setBigDecimal(30, sim.getInputMaxCrosstalk());
            ps.setInt(31, sim.getInputTRangeMin());
            ps.setInt(32, sim.getInputTRangeMax());
            ps.setInt(33, sim.getInputErlang());
            ps.setBigDecimal(34, sim.getInputXtPerUnitLength());

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
