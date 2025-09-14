package py.una.pol.simulador.eon.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import py.una.pol.simulador.eon.models.Demand;

public class Database {

    private static Connection connection;
    private static final String URL = "jdbc:postgresql://localhost:5432/tesis";
    private static final String USER = "postgres";
    private static final String PASSWORD = "postgres";

    public void openConnection() throws SQLException {
        connection = DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
            } catch (SQLException e) {
                System.out.println("❌ Error al cerrar conexión: " + e.getMessage());
            }
        }
    }

    public int insertarBloqueo(String topologia, String tiempo,
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

    public void insertarResumen(String topologia, String erlang, String tipo_erlang,
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

    public void insertSimulacionResumen(SimulacionResumen sim) {
        String sql = "INSERT INTO simulacion_resumen(" +
                "tiempo_inicio, tiempo_fin, tiempo_ejecucion, duracion, topologia, " +
                "numero_de_bloqueos, numero_de_rutas_establecidas, numero_de_desmandas_pospuestas, cantidad_de_demandas, " +
                "k1, k2, k3, k4, k5, " +
                "numero_de_bloqueos_por_fragmentacion, numero_de_bloqueos_por_crosstalk, numero_de_bloqueos_por_fragmentacion_de_camino, " +
                "diametro_grafo, grado_promedio, " +
                "input_demands, input_valor_h, input_fs_width, input_fs_range_max, input_fs_range_min, " +
                "input_capacity, input_cores, input_lambda, input_simulation_time, input_max_crosstalk, " +
                "input_t_range_min, input_t_range_max, input_erlang, input_xt_per_unit_length, motivo_bloqueo, porcentaje_motivo," +
                "porcentaje, tipo_erlang,max_cant_pospuetas_en_un_tiempo, prom_cant_pospuetas_en_un_tiempo " +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

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
            ps.setBigDecimal(22, sim.getInputFsWidth());
            ps.setInt(23, sim.getInputFsRangeMax());
            ps.setInt(24, sim.getInputFsRangeMin());
            ps.setInt(25, sim.getInputCapacity());
            ps.setInt(26, sim.getInputCores());
            ps.setInt(27, sim.getInputLambda());
            ps.setInt(28, sim.getInputSimulationTime());
            ps.setBigDecimal(29, sim.getInputMaxCrosstalk());
            ps.setInt(30, sim.getInputTRangeMin());
            ps.setInt(31, sim.getInputTRangeMax());
            ps.setInt(32, sim.getInputErlang());
            ps.setBigDecimal(33, sim.getInputXtPerUnitLength());
            ps.setString(34, sim.getMotivoBloqueo());
            ps.setString(35, sim.getPorcentajeMotivo());
            ps.setString(36, sim.getPorcentaje());
            ps.setString(37, sim.getTipoErlang());
            ps.setInt(38, sim.getMaxCantPospuetasEnUnTiempo());
            ps.setDouble(39, sim.getPromCantPospuetasEnUnTiempo());

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int insertDemand(Demand demand) {
        String sql = "INSERT INTO demand (" +
                "simulacion_id, source, destination, fs, lifetime, blocked, ts, te, cant_pospuesto, tiempo_instalacion" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int filas = 0;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, demand.getSimulacionId());
            stmt.setInt(2, demand.getSource());
            stmt.setInt(3, demand.getDestination());
            stmt.setInt(4, demand.getFs());
            stmt.setInt(5, demand.getLifetime());

            // Manejo del Boolean (puede ser null)
            if (demand.getBlocked() != null) {
                stmt.setBoolean(6, demand.getBlocked());
            } else {
                stmt.setNull(6, java.sql.Types.BOOLEAN);
            }

            stmt.setInt(7, demand.getTs());
            stmt.setInt(8, demand.getTe());
            stmt.setInt(9, demand.getCantPospuesto());
            if (demand.getTiempoInstalacion() != null) {
                stmt.setInt(10, demand.getTiempoInstalacion());
            } else {
                stmt.setNull(10, java.sql.Types.INTEGER);
            }

            filas = stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("❌ Error al insertar demanda: " + e.getMessage());
        }
        return filas;
    }

    public long obtenerIdSimulacion() {
        String sql = "SELECT MAX(id) AS max_id FROM simulacion_resumen";
        long id = 0L;
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             var rs = stmt.executeQuery()) {
            if (rs.next()) {
                id = rs.getInt("max_id");
            }
        } catch (SQLException e) {
            return id;
        }
        return id;
    }
}
