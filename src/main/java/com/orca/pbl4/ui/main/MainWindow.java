package com.orca.pbl4.ui.main;

import com.orca.pbl4.core.system.SystemMonitor;
import com.orca.pbl4.service.process.DefaultProcessManager;
import com.orca.pbl4.service.process.ProcessManager;
import com.orca.pbl4.ui.process.MetricsPanel;
import com.orca.pbl4.ui.process.ProcessTablePanel;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;

/**
 * Cửa sổ chính của ứng dụng.
 * TODO: bổ sung các tab khác (CPU, Disk, ...) nếu cần.
 */
public class MainWindow extends JFrame {

    private final ProcessManager processManager;
    private final Timer refreshTimer;

    public MainWindow() {
        super("PBL4 - Process Monitor");
        this.processManager = new DefaultProcessManager(new SystemMonitor());

        MetricsPanel metricsPanel = new MetricsPanel();
        processManager.addUpdateListener(metricsPanel);

        JTabbedPane tabs = new JTabbedPane();
        ProcessTablePanel processTablePanel = new ProcessTablePanel(processManager,
                () -> tabs.setSelectedComponent(metricsPanel));
        tabs.addTab("Processes", processTablePanel);
        tabs.addTab("Metrics", metricsPanel);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(tabs, BorderLayout.CENTER);

        refreshTimer = new Timer(1000, e -> processManager.refreshSnapshot());
        refreshTimer.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}