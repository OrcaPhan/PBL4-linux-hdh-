package com.orca.pbl4.ui;

import com.orca.pbl4.core.system.SystemMonitor;
import com.orca.pbl4.ui.panels.ProcessesPanel;
import com.orca.pbl4.ui.panels.PerformancePanel;

import javax.swing.*;
import java.awt.*;

/**
 * Main window của ORCA System Monitor
 * Chứa JTabbedPane với các tab: Processes, Performance
 */
public class MainFrame extends JFrame {
    
    private final SystemMonitor systemMonitor;
    private JTabbedPane tabbedPane;
    private ProcessesPanel processesPanel;
    private PerformancePanel performancePanel;
    
    public MainFrame() {
        this.systemMonitor = new SystemMonitor();
        initializeUI();
    }
    
    private void initializeUI() {
        setTitle("ORCA System Monitor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null); // Center on screen
        
        // Tạo tabbed pane
        tabbedPane = new JTabbedPane();
        
        // Tab 1: Processes
        processesPanel = new ProcessesPanel(systemMonitor);
        tabbedPane.addTab("Processes", processesPanel);
        
        // Tab 2: Performance
        performancePanel = new PerformancePanel(systemMonitor);
        tabbedPane.addTab("Performance", performancePanel);
        
        // Add to frame
        add(tabbedPane);
        
        // Menu bar (optional)
        createMenuBar();
    }
    
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // File menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> {
            processesPanel.stopRefresh();
            performancePanel.stopRefresh();
            System.exit(0);
        });
        fileMenu.add(exitItem);
        
        // View menu
        JMenu viewMenu = new JMenu("View");
        JMenuItem refreshItem = new JMenuItem("Refresh Now");
        refreshItem.addActionListener(e -> {
            processesPanel.refreshNow();
            performancePanel.refreshNow();
        });
        viewMenu.add(refreshItem);
        
        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        
        setJMenuBar(menuBar);
    }
    
    public void startMonitoring() {
        processesPanel.startRefresh();
        performancePanel.startRefresh();
    }
    
    public static void main(String[] args) {
        // Set Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
            frame.startMonitoring();
        });
    }
}
