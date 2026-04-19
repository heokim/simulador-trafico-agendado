package py.una.pol.simulador.eon.utils;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;

public class GraphAnalyticsUtils {

    /**
     * Genera un gráfico de Erlang y % de bloqueo sobre el tiempo de la simulación.
     */
    public static void guardarGraficoErlang(
            double[] tiempos,
            double[] erlangs,
            double[] bloqueos,
            int totalTiempo,
            String fileName,
            String topologia,
            String valorH
    ) throws IOException {
        
        XYSeries seriesErlang = new XYSeries("Erlang");
        XYSeries seriesBloqueo = new XYSeries("% Bloqueo Acumulado");

        for (int i = 0; i < tiempos.length; i++) {
            seriesErlang.add(tiempos[i], erlangs[i]);
            seriesBloqueo.add(tiempos[i], bloqueos[i]);
        }

        XYSeriesCollection dataset1 = new XYSeriesCollection();
        dataset1.addSeries(seriesErlang);

        XYSeriesCollection dataset2 = new XYSeriesCollection();
        dataset2.addSeries(seriesBloqueo);

        // Crear gráfico base
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Evolución de Tráfico y Bloqueos",
                "Unidad de tiempo de simulación",
                "Erlang Generado",
                dataset1
        );

        // Personalizar subtítulo
        chart.addSubtitle(new TextTitle("Topología: " + topologia + " | Umbral de XT: " + valorH,
                new Font("Dialog", Font.BOLD, 12)));

        XYPlot plot = chart.getXYPlot();

        // Configuración eje Y primario (Erlang)
        XYLineAndShapeRenderer renderer1 = new XYLineAndShapeRenderer();
        renderer1.setSeriesPaint(0, Color.BLUE);
        renderer1.setSeriesStroke(0, new BasicStroke(2.0f));
        plot.setRenderer(0, renderer1);

        // Eje Y secundario (% Bloqueo)
        NumberAxis axis2 = new NumberAxis("% Bloqueo");
        plot.setRangeAxis(1, axis2);
        plot.setDataset(1, dataset2);
        plot.mapDatasetToRangeAxis(1, 1);

        XYLineAndShapeRenderer renderer2 = new XYLineAndShapeRenderer();
        renderer2.setSeriesPaint(0, Color.RED);
        renderer2.setSeriesStroke(0, new BasicStroke(2.0f));
        plot.setRenderer(1, renderer2);

        // Pintar las particiones (Baja, Media, Alta) en el fondo del plot
        double slice = totalTiempo / 5.0;
        
        Color colorBajo = new Color(0, 255, 0, 40); // Verde suave
        Color colorMedio = new Color(255, 255, 0, 40); // Amarillo suave
        Color colorAlto = new Color(255, 0, 0, 40); // Rojo suave
        
        addMarker(plot, 0, slice, "Tráfico Bajo", colorBajo);
        addMarker(plot, slice, slice * 2, "Tráfico Medio", colorMedio);
        addMarker(plot, slice * 2, slice * 3, "Tráfico Alto", colorAlto);
        addMarker(plot, slice * 3, slice * 4, "Tráfico Medio", colorMedio);
        addMarker(plot, slice * 4, totalTiempo, "Tráfico Bajo", colorBajo);

        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        // Guardar archivo
        ChartUtils.saveChartAsPNG(new File(fileName), chart, 1000, 600);
    }
    
    private static void addMarker(XYPlot plot, double start, double end, String label, Color color) {
        IntervalMarker marker = new IntervalMarker(start, end);
        marker.setLabel(label);
        marker.setLabelFont(new Font("Dialog", Font.ITALIC, 11));
        marker.setLabelAnchor(RectangleAnchor.TOP_LEFT);
        marker.setLabelTextAnchor(TextAnchor.TOP_LEFT);
        marker.setPaint(color);
        plot.addDomainMarker(marker, Layer.BACKGROUND);
    }
}
