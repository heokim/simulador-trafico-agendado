/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package py.una.pol.simulador.eon.utils;

import com.mxgraph.layout.mxFastOrganicLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxStyleUtils;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import javax.imageio.ImageIO;
import org.jgrapht.Graph;
import org.jgrapht.ext.JGraphXAdapter;
import py.una.pol.simulador.eon.models.Link;

/**
 * Utilería para el manejo de gráficos
 *
 * @author Néstor E. Reinoso Wood
 */
public class GraphUtils {

    /**
     * Crea una imagen de la topología utilizada
     *
     * @param g Grafo de la red
     * @param fileName Nombre del archivo a generar
     * @throws IOException Error de I/O
     */
    public static void createImage(Graph<Integer, Link> g, String fileName) throws IOException {
        JGraphXAdapter<Integer, Link> graphAdapter
                = new JGraphXAdapter<>(g);
        mxIGraphLayout layout = new mxFastOrganicLayout(graphAdapter);
        mxGraphComponent graphComponent = new mxGraphComponent(graphAdapter);
        mxGraphModel graphModel = (mxGraphModel) graphComponent.getGraph().getModel();

        Collection<Object> cells = graphModel.getCells().values();
        // This part to remove arrow from edge
        mxStyleUtils.setCellStyles(graphComponent.getGraph().getModel(),
                cells.toArray(), mxConstants.STYLE_ENDARROW, mxConstants.NONE);

        layout.execute(graphAdapter.getDefaultParent());

        File imgFile = new File(fileName + ".png");
        imgFile.createNewFile();

        BufferedImage image
                = mxCellRenderer.createBufferedImage(graphAdapter, null, 2, Color.WHITE, true, null);
        imgFile = new File(fileName);
        ImageIO.write(image, "PNG", imgFile);
    }
}
