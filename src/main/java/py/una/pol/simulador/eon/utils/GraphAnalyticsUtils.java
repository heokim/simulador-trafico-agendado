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

    private static final int CHART_WIDTH = 1920;
    private static final int CHART_HEIGHT = 1080;

    public static void guardarGraficoErlang(
            double[] tiempos,
            double[] erlangs,
            double[] erlangsReales,
            double[] bloqueos,
            int totalTiempo,
            String fileName,
            String topologia,
            String valorH
    ) throws IOException {

        XYSeries seriesErlang = new XYSeries("Erlang");
        XYSeries seriesErlangReal = new XYSeries("Erlang Real");
        XYSeries seriesBloqueo = new XYSeries("% Bloqueo");

        for (int i = 0; i < tiempos.length; i++) {
            seriesErlang.add(tiempos[i], erlangs[i]);
            seriesErlangReal.add(tiempos[i], erlangsReales[i]);
            seriesBloqueo.add(tiempos[i], bloqueos[i]);
        }

        XYSeriesCollection dataset1 = new XYSeriesCollection();
        dataset1.addSeries(seriesErlang);
        // dataset1.addSeries(seriesErlangReal);

        XYSeriesCollection dataset2 = new XYSeriesCollection();
        dataset2.addSeries(seriesBloqueo);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Evolucion de Trafico y Bloqueos",
                "Unidad de tiempo de simulacion",
                "Erlang",
                dataset1
        );

        chart.addSubtitle(new TextTitle("Topologia: " + topologia + " | Umbral de XT: " + valorH,
                new Font("Dialog", Font.BOLD, 12)));

        XYPlot plot = chart.getXYPlot();

        XYLineAndShapeRenderer renderer1 = new XYLineAndShapeRenderer();
        renderer1.setSeriesPaint(0, Color.BLUE);
        renderer1.setSeriesStroke(0, new BasicStroke(4.5f));
        renderer1.setSeriesShapesVisible(0, false);
        // renderer1.setSeriesPaint(1, Color.ORANGE);
        // renderer1.setSeriesStroke(1, new BasicStroke(3.0f));
        // renderer1.setSeriesShapesVisible(1, false);
        plot.setRenderer(0, renderer1);

        NumberAxis axis2 = new NumberAxis("% Bloqueo");
        axis2.setRange(0.0, 7.0);
        plot.setRangeAxis(1, axis2);
        plot.setDataset(1, dataset2);
        plot.mapDatasetToRangeAxis(1, 1);

        XYLineAndShapeRenderer renderer2 = new XYLineAndShapeRenderer();
        renderer2.setSeriesPaint(0, Color.RED);
        renderer2.setSeriesStroke(0, new BasicStroke(4.5f));
        renderer2.setSeriesShapesVisible(0, false);
        plot.setRenderer(1, renderer2);

        double slice = totalTiempo / 5.0;

        Color colorBajo = new Color(0, 255, 0, 40);
        Color colorMedio = new Color(255, 255, 0, 40);
        Color colorAlto = new Color(255, 0, 0, 40);

        addMarker(plot, 0, slice, "Trafico Bajo", colorBajo);
        addMarker(plot, slice, slice * 2, "Trafico Medio", colorMedio);
        addMarker(plot, slice * 2, slice * 3, "Trafico Alto", colorAlto);
        addMarker(plot, slice * 3, slice * 4, "Trafico Medio", colorMedio);
        addMarker(plot, slice * 4, totalTiempo, "Trafico Bajo", colorBajo);

        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        ChartUtils.saveChartAsPNG(new File(fileName), chart, CHART_WIDTH, CHART_HEIGHT);
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
