package py.una.pol.simulador.eon.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Database {

    private static final String URL = "jdbc:postgresql://localhost:5432/tesis";
    private static final String USER = "admin";
    private static final String PASSWORD = "1234";

    public static Connection getConnection() {
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Conexión exitosa a la base de datos PostgreSQL.");
            return conn;
        } catch (SQLException e) {
            throw new RuntimeException("❌ Error al conectar a la base de datos: " + e.getMessage(), e);
        }
    }


    /**
     * Inserta datos en la base usando SQL parametrizado
     *
     * @param sql    sentencia SQL con parámetros (ej: "INSERT INTO usuarios(nombre, edad) VALUES(?, ?)")
     * @param params valores de los parámetros
     * @return número de filas afectadas
     */
    public static int insertData(String sql, Object... params) {
        int rowsAffected = 0;
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Cargar parámetros dinámicos
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            rowsAffected = stmt.executeUpdate();
            System.out.println("✅ Inserción realizada. Filas afectadas: " + rowsAffected);

        } catch (SQLException e) {
            System.out.println("❌ Error al insertar datos: " + e.getMessage());
        }
        return rowsAffected;
    }

    /**
     * Inserta un registro en la tabla Bloqueos
     */
    public static int insertarBloqueo(String rsa, String topologia, String tiempo,
                                      String demanda, String erlang, String h) {
        String sql = "INSERT INTO bloqueos (rsa, topologia, tiempo, demanda, erlang, h) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        int filas = 0;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, rsa);
            stmt.setString(2, topologia);
            stmt.setString(3, tiempo);
            stmt.setString(4, demanda);
            stmt.setString(5, erlang);
            stmt.setString(6, h);

            filas = stmt.executeUpdate();
            System.out.println("✅ Bloqueo insertado correctamente. Filas afectadas: " + filas);

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

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

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
            System.out.println("✅ Resumen insertado correctamente");

        } catch (SQLException e) {
            System.out.println("❌ Error al insertar Resumen: " + e.getMessage());
        }
    }

}
