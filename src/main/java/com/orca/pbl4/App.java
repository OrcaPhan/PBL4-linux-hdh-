package com.orca.pbl4;

import com.orca.pbl4.ui.MainFrame;

import javax.swing.*;

/**
 * Entry point của ORCA System Monitor
 */
public class App {
    public static void main(String[] args) {
        // Set Look and Feel to system default
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Launch GUI on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
            frame.startMonitoring();
        });
    }
}
