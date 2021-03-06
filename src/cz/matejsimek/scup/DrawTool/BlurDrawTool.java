package cz.matejsimek.scup.DrawTool;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

/**
 * Created by kamil on 20.05.15.
 */
public class BlurDrawTool extends AbstractDrawTool {
    @Override
    public void draw(Point point, Dimension dimension, Point pointOrg, Dimension dimensionOrg) {
        if (!(dimension.getHeight() > 0 && dimension.getWidth() > 0)) {
            return;
        }
        BufferedImage biSrc = image.getSubimage(
                (int) point.getX(),
                (int) point.getY(),
                (int) dimension.getWidth(),
                (int) dimension.getHeight()
        );
        BufferedImage biDest = null;
        for (int i = 0; i < 6; i++) {
            biDest = new BufferedImage(biSrc.getWidth(), biSrc.getHeight(), image.getType());
            float data[] = {0.0625f, 0.125f, 0.0625f, 0.125f, 0.25f, 0.125f, 0.0625f, 0.125f, 0.0625f};
            Kernel kernel = new Kernel(3, 3, data);
            ConvolveOp convolve = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
            convolve.filter(biSrc, biDest);
            biSrc = biDest;
        }
        graphics.drawImage(
                biDest,
                null,
                (int) point.getX(),
                (int) point.getY()
        );
    }
}
