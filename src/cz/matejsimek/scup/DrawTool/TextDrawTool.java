package cz.matejsimek.scup.DrawTool;

import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Created by kamil on 20.05.15.
 */
public class TextDrawTool extends AbstractDrawTool {
    private String text = "";
    private Point point;

    public void keyPressed(KeyEvent e) {
        if (isPrintableChar(e.getKeyChar())) {
            this.text += e.getKeyChar();
        }
        if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE && this.text.length() > 0) {
            this.text = this.text.substring(0, this.text.length()-1);
        }
    }

    private boolean isPrintableChar( char c ) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of( c );
        return (!Character.isISOControl(c)) &&
                c != KeyEvent.CHAR_UNDEFINED &&
                block != null &&
                block != Character.UnicodeBlock.SPECIALS;
    }

    public void setPosition(Point point) {
        this.point = point;
    }

    public void clear() {
        this.text = "";
    }

    @Override
    public void draw(Point point, Dimension dimension, Point pointOrg, Dimension dimensionOrg) {}

    public void draw() {
        if (this.point != null) {
            graphics.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            );

            graphics.drawString(
                    this.text,
                    (int) this.point.getX(),
                    (int) this.point.getY()
            );
        }
    }
}
