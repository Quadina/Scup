package cz.matejsimek.scup;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.prefs.Preferences;

/**
 * Created by kamil on 20.05.15.
 */
public class Setting extends javax.swing.JFrame {

    private Preferences prefs;

    private JTextField server;
    private JCheckBox monitorAll;
    private JButton cancel;
    private JButton saveSettings;
    private JTextField directory;
    private JTextField url;
    private JPasswordField password;
    private JTextField username;
    private JPanel rootPanel;
    private JCheckBox upload;
    private JButton test;

    public Setting() {
        super("Settings");
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

        setLocationByPlatform(true);
        setContentPane(rootPanel);
        pack();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        prefs = Preferences.userNodeForPackage(cz.matejsimek.scup.Scup.class);

        server.setText(prefs.get(Scup.KEY_FTP_SERVER, "localhost"));
        username.setText(prefs.get(Scup.KEY_FTP_USERNAME, "anonymous"));
        password.setText(prefs.get(Scup.KEY_FTP_PASSWORD, ""));
        directory.setText(prefs.get(Scup.KEY_DIRECTORY, ""));
        url.setText(prefs.get(Scup.KEY_URL, "http://localhost"));
        monitorAll.setSelected(prefs.getBoolean(Scup.KEY_MONITOR_ALL, true));
        upload.setSelected(prefs.getBoolean(Scup.KEY_UPLOAD, true));

        saveSettings.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                doSaveSettings();
                dispose();
            }
        });
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                dispose();
            }
        });
        setVisible(true);
        test.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                doSaveSettings();
                if (Scup.testConnection()) {
                    JOptionPane.showMessageDialog(null, "Connected!");
                } else {
                    JOptionPane.showMessageDialog(null, "Not connected!");
                }
            }
        });
    }

    private void doSaveSettings() {
        prefs.put(Scup.KEY_FTP_SERVER, server.getText());
        prefs.put(Scup.KEY_FTP_USERNAME, username.getText());
        prefs.put(Scup.KEY_FTP_PASSWORD, password.getText());
        prefs.put(Scup.KEY_DIRECTORY, directory.getText());
        prefs.put(Scup.KEY_URL, url.getText());
        prefs.putBoolean(Scup.KEY_MONITOR_ALL, monitorAll.isSelected());
        prefs.putBoolean(Scup.KEY_UPLOAD, upload.isSelected());
        prefs.putBoolean(Scup.KEY_INITIAL_SETTINGS, false);

        Scup.reloadConfiguration();
    }
}
