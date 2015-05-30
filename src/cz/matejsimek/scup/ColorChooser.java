package cz.matejsimek.scup;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.Preferences;

/**
 * Created by kamil on 20.05.15.
 */
public class ColorChooser extends javax.swing.JDialog {
    private JPanel rootPanel;
    private JColorChooser color;
    private JButton ok;
    private JButton parentButton;

    public ColorChooser(JButton bb) {
        parentButton = bb;
        setContentPane(rootPanel);
        setResizable(false);
        setLocationByPlatform(true);
        pack();

        color.setColor(Color.RED);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                dispose();
                parentButton.setForeground(color.getColor());
                color.setForeground(color.getColor());
                Preferences prefs = Preferences.userNodeForPackage(Scup.class);
                prefs.putInt("selected_color", color.getColor().getRGB());
                System.out.println("write: " + prefs.getInt("selected_color", 0));
            }
        });
    }

    public JColorChooser getColor() {
        return color;
    }

    public void setColor(int rgb) {
        color.setColor(new Color(rgb));
    }
}
