package py.una.pol.simulador.eon.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Utileria para exportar metricas de la simulacion a formato CSV,
 * facilitando la generacion de graficos comparativos en Excel (ej. con vs sin agendado).
 */
public class CsvUtils {

    public static class RegistroTiempo {
        public int tiempo;
        public String tipoTrafico;
        public int erlangOfrecido;
        public int erlangCursado;
        public int bloqueosTiempo;
        public int bloqueosAcumulados;
        public int demandasTiempo;
        public int demandasAcumuladas;
        public double porcentajeBloqueoAcumulado;
        public long pospuestasTiempo;
        public int rutasEstablecidasAcumuladas;

        public RegistroTiempo(
                int tiempo,
                String tipoTrafico,
                int erlangOfrecido,
                int erlangCursado,
                int bloqueosTiempo,
                int bloqueosAcumulados,
                int demandasTiempo,
                int demandasAcumuladas,
                double porcentajeBloqueoAcumulado,
                long pospuestasTiempo,
                int rutasEstablecidasAcumuladas
        ) {
            this.tiempo = tiempo;
            this.tipoTrafico = tipoTrafico;
            this.erlangOfrecido = erlangOfrecido;
            this.erlangCursado = erlangCursado;
            this.bloqueosTiempo = bloqueosTiempo;
            this.bloqueosAcumulados = bloqueosAcumulados;
            this.demandasTiempo = demandasTiempo;
            this.demandasAcumuladas = demandasAcumuladas;
            this.porcentajeBloqueoAcumulado = porcentajeBloqueoAcumulado;
            this.pospuestasTiempo = pospuestasTiempo;
            this.rutasEstablecidasAcumuladas = rutasEstablecidasAcumuladas;
        }
    }

    /**
     * Guarda la evolucion temporal de la simulacion en un archivo CSV.
     *
     * @param fileName  Ruta o nombre del archivo CSV
     * @param registros Lista de registros por cada unidad de tiempo
     * @throws IOException Si ocurre un error al escribir el archivo
     */
    public static void guardarCsvSimulacion(String fileName, List<RegistroTiempo> registros) throws IOException {
        File file = new File(fileName);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
            // Cabecera CSV
            writer.write("tiempo,tipo_trafico,erlang_ofrecido,erlang_cursado,bloqueos_tiempo,bloqueos_acumulados,demandas_tiempo,demandas_acumuladas,porcentaje_bloqueo_acumulado,pospuestas_tiempo,rutas_establecidas_acumuladas");
            writer.newLine();

            for (RegistroTiempo r : registros) {
                writer.write(String.format(Locale.US,
                        "%d,%s,%d,%d,%d,%d,%d,%d,%.4f,%d,%d",
                        r.tiempo,
                        r.tipoTrafico,
                        r.erlangOfrecido,
                        r.erlangCursado,
                        r.bloqueosTiempo,
                        r.bloqueosAcumulados,
                        r.demandasTiempo,
                        r.demandasAcumuladas,
                        r.porcentajeBloqueoAcumulado,
                        r.pospuestasTiempo,
                        r.rutasEstablecidasAcumuladas
                ));
                writer.newLine();
            }
        }
    }
}
