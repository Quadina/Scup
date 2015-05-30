package cz.matejsimek.scup;

import cz.matejsimek.scup.DrawTool.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.*;
import java.util.prefs.Preferences;

/**
 * Created by kamil on 20.05.15.
 */
public class Paint extends javax.swing.JFrame {

    private Preferences prefs;

    private JPanel paintPanel;
    private JButton arrow;
    private JButton rectangle;
    private JButton ellipse;
    private JButton line;
    private JButton text;
    private JButton undo;
    private JButton redo;
    private JButton upload;
    private JButton save;
    private ImagePanel imgPanel;
    private JSlider size;
    private JButton color;
    private JButton blur;
    private JScrollPane scrollPanel;
    private AbstractDrawTool drawTool;
    private Point start;
    private BufferedImage buffImage;
    private ArrayList<BufferedImage> imgHistory;
    private int imgHistoryIterator;
    private ColorChooser colorChooser;
    private JButton lastAction;
    private ActionListener listener;

    public Paint(BufferedImage image) {
        super("Scup");
        init(image);
        registerListeners();
        restorePrefs();
    }

    public static BufferedImage bufferedImageClone(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    private void init(BufferedImage image) {
        try {
            setIconImage(ImageIO.read(Scup.class.getResource("resources/icon64.png")));
            java.util.List<Image> icons = new ArrayList<Image>();
            int[] sizes = {24,32,48,64,96,128,256,512};
            for (int size : sizes) {
                icons.add(ImageIO.read(Scup.class.getResource("resources/icon" + size + ".png")));
            }
            setIconImages(icons);
        } catch (IOException e) {
            e.printStackTrace();
        }
        imgHistory = new ArrayList<BufferedImage>();
        imgHistoryIterator = 0;
        colorChooser = new ColorChooser(color);
        prefs = Preferences.userNodeForPackage(cz.matejsimek.scup.Scup.class);

        setContentPane(paintPanel);

        imgHistory.add(bufferedImageClone(image));
        imgPanel.setImage(image);
        imgPanel.setPreferredSize((new Dimension(image.getWidth(), image.getHeight())));
        scrollPanel.setSize((new Dimension(image.getWidth(), image.getHeight())));
        scrollPanel.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        setSize((new Dimension(image.getWidth(), image.getHeight())));
        setResizable(false);
        setLocationByPlatform(true);
        setAlwaysOnTop(true);
        pack();

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
        color.setForeground(colorChooser.getColor().getColor());

        Rectangle maxWinBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize((new Dimension(
                (int) Math.min(getWidth(), maxWinBounds.getWidth()),
                (int) Math.min(getHeight(), maxWinBounds.getHeight())
        )));
    }

    private void performRedo() {
        try {
            BufferedImage image = imgHistory.get(imgHistory.size() - 1 + (++imgHistoryIterator));
            imgPanel.setImage(image);
            imgPanel.repaint();
        } catch (IndexOutOfBoundsException ignored) {
            imgHistoryIterator--;
        }
    }

    private void performUndo() {
        try {
            BufferedImage image = imgHistory.get(imgHistory.size() - 1 + (--imgHistoryIterator));
            imgPanel.setImage(bufferedImageClone(image));
            imgPanel.repaint();
        } catch (IndexOutOfBoundsException ignored) {
            imgHistoryIterator++;
        }
    }

    private void registerListeners() {
        paintPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK),
                "doUndo");
        paintPanel.getActionMap().put("doUndo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                performUndo();
            }
        });
        paintPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK),
                "doRedo");
        paintPanel.getActionMap().put("doRedo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                performRedo();
            }
        });
        save.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                dispose();
                prefs.putBoolean(Scup.KEY_UPLOAD, false);
                Scup.reloadConfiguration();
                Scup.processImage(imgPanel.getImage());
                prefs.putBoolean(Scup.KEY_UPLOAD, true);
                Scup.reloadConfiguration();
            }
        });
        upload.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                dispose();
                Scup.processImage(imgPanel.getImage());
            }
        });
        undo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                performUndo();
            }
        });
        redo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                performRedo();
            }
        });

        listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (lastAction != null) {
                    lastAction.setSelected(false);
                }
                if (actionEvent.getSource() == rectangle) {
                    rectangle.setSelected(true);
                    lastAction = rectangle;
                    drawTool = new RectangleDrawTool();
                    prefs.put("selected_tool", "rectangle");
                }
                if (actionEvent.getSource() == line) {
                    line.setSelected(true);
                    lastAction = line;
                    drawTool = new LineDrawTool();
                    prefs.put("selected_tool", "line");
                }
                if (actionEvent.getSource() == ellipse) {
                    ellipse.setSelected(true);
                    lastAction = ellipse;
                    drawTool = new EllipseDrawTool();
                    prefs.put("selected_tool", "ellipse");
                }
                if (actionEvent.getSource() == arrow) {
                    arrow.setSelected(true);
                    lastAction = arrow;
                    drawTool = new ArrowDrawTool();
                    prefs.put("selected_tool", "arrow");
                }
                if (actionEvent.getSource() == blur) {
                    blur.setSelected(true);
                    lastAction = blur;
                    drawTool = new BlurDrawTool();
                    prefs.put("selected_tool", "blur");
                }
                if (actionEvent.getSource() == text) {
                    text.setSelected(true);
                    lastAction = text;
                    drawTool = new TextDrawTool();
                    prefs.put("selected_tool", "text");
                    imgPanel.grabFocus();
                }
            }
        };

        imgPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                super.mouseDragged(e);
                BufferedImage image = bufferedImageClone(buffImage);
                imgPanel.setImage(image);
                draw(e);
            }
        });

        imgPanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyTyped(e);
                if (drawTool instanceof TextDrawTool) {
                    ((TextDrawTool) drawTool).keyPressed(e);
                    BufferedImage image = bufferedImageClone(buffImage);
                    imgPanel.setImage(image);
                    draw(e);
                    imgHistory.remove(imgHistory.size() - 1);
                    imgHistory.add(image);
                }
            }
        });

        imgPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                start = e.getPoint();
                buffImage = imgPanel.getImage();
                if (drawTool instanceof TextDrawTool) {
                    imgPanel.grabFocus();
                    ((TextDrawTool) drawTool).clear();
                    ((TextDrawTool) drawTool).setPosition(e.getPoint());
                    BufferedImage image = bufferedImageClone(buffImage);
                    imgPanel.setImage(image);
                    createNewHistoryEntry(image);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                if (drawTool instanceof TextDrawTool) {
                    return;
                }
                BufferedImage image = bufferedImageClone(buffImage);
                imgPanel.setImage(image);
                draw(e);
                createNewHistoryEntry(image);
            }
        });

        color.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                colorChooser.setVisible(true);
            }
        });

        arrow.addActionListener(listener);
        rectangle.addActionListener(listener);
        ellipse.addActionListener(listener);
        line.addActionListener(listener);
        text.addActionListener(listener);
        blur.addActionListener(listener);
        size.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider)e.getSource();
                if (!source.getValueIsAdjusting()) {
                    int a = (int)source.getValue();
                    prefs.putInt("selected_size", a);
                }
            }
        });
    }

    private void createNewHistoryEntry(BufferedImage image) {
        for (int i = 0; i > imgHistoryIterator; i--) {
            try {
                imgHistory.remove(imgHistory.size() - 1);
            } catch (Exception ex) {

            }
        }
        imgHistoryIterator = 0;
        imgHistory.add(image);
    }

    private void restorePrefs() {
        ActionEvent ac = null;
        String selected_tool = prefs.get("selected_tool", "");
        if (selected_tool.equals("rectangle")) {
            ac = new ActionEvent(rectangle, 0, "");
        }
        if (selected_tool.equals("line")) {
            ac = new ActionEvent(line, 0, "");
        }
        if (selected_tool.equals("ellipse")) {
            ac = new ActionEvent(ellipse, 0, "");
        }
        if (selected_tool.equals("arrow")) {
            ac = new ActionEvent(arrow, 0, "");
        }
        if (selected_tool.equals("blur")) {
            ac = new ActionEvent(blur, 0, "");
        }
        if(ac!=null){
            listener.actionPerformed(ac);
        }
        int selected_color = prefs.getInt("selected_color", Color.RED.getRGB());
        colorChooser.setColor(selected_color);
        color.setForeground(new Color(selected_color));
        size.setValue(prefs.getInt("selected_size", 2));
    }

    private Graphics2D getGraphics2D(BufferedImage image) {
        Graphics g = image.createGraphics();
        g.setColor(Color.BLACK);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int thickness = size.getValue();
        g2.setStroke(new BasicStroke(thickness));
        g2.setColor(colorChooser.getColor().getColor());

        if (drawTool instanceof ArrowDrawTool) {
            ((ArrowDrawTool) drawTool).setSize(thickness * 2); //brzydkoooo
        }
        if (drawTool instanceof TextDrawTool) {
            Font font = g.getFont().deriveFont( Font.BOLD, 8.0f + (thickness * 2));
            g2.setFont(font);
        }
        if (drawTool instanceof AbstractDrawTool) {
            drawTool.setImage(image);
            drawTool.setGraphics(g2);
        }

        return g2;
    }

    private void $$$setupUI$$$() {
        createUIComponents();
    }

    private void createUIComponents() {
        imgPanel = new ImagePanel();
    }

    private Point getRelStart(Point end) {
        Rectangle rect = imgPanel.getVisibleRect();
        Point point = new Point((int) (start.getX() - rect.getX()), (int) (start.getY() - rect.getY()));
        Dimension dimension = getRelDimension(end);

        Rectangle viewRect = scrollPanel.getViewport().getViewRect();
        return new Point(
                (int) Math.max(0, Math.min(viewRect.getX() + point.getX(), viewRect.getX() + point.getX() + dimension.getWidth())),
                (int) Math.max(0, Math.min(viewRect.getY() + point.getY(), viewRect.getY() + point.getY() + dimension.getHeight()))
        );
    }

    private Point getRelStartOrg() {
        Rectangle rect = imgPanel.getVisibleRect();
        Rectangle viewRect = scrollPanel.getViewport().getViewRect();
        return new Point((int) (viewRect.getX() + start.getX() - rect.getX()), (int) (viewRect.getY() + start.getY() - rect.getY()));
    }

    private Dimension getRelDimension(Point end) {
        Rectangle rect = imgPanel.getVisibleRect();
        return new Dimension(
                (int) Math.min(end.getX() - start.getX(), (rect.getX() + rect.getWidth()) - start.getX()),
                (int) Math.min(end.getY() - start.getY(), (rect.getY() + rect.getHeight()) - start.getY())
        );
    }

    private Dimension getRelDimensionAbs(Point end) {
        Rectangle rect = imgPanel.getVisibleRect();
        return new Dimension(
                (int) Math.abs(Math.min(end.getX() - start.getX(), (rect.getX() + rect.getWidth()) - start.getX())),
                (int) Math.abs(Math.min(end.getY() - start.getY(), (rect.getY() + rect.getHeight()) - start.getY()))
        );
    }

    private void draw(MouseEvent e) {
        Graphics2D g2 = getGraphics2D(imgPanel.getImage());

        Point relStart = getRelStart(e.getPoint());
        Dimension relDim = getRelDimensionAbs(e.getPoint());

        Point relStartOrg = getRelStartOrg();
        Dimension relDimOrg = getRelDimension(e.getPoint());

        if (drawTool != null && !(drawTool instanceof TextDrawTool)) {
            drawTool.draw(relStart, relDim, relStartOrg, relDimOrg);
        }

        g2.dispose();
        imgPanel.repaint();
    }

    private void draw(KeyEvent e) {
        Graphics2D g2 = getGraphics2D(imgPanel.getImage());

        if (drawTool instanceof TextDrawTool) {
            ((TextDrawTool)drawTool).draw();
        }

        g2.dispose();
        imgPanel.repaint();
    }
}
