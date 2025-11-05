package com.orca.pbl4.ui.panels;

import com.orca.pbl4.core.system.SystemMonitor;
import com.orca.pbl4.core.system.SystemSnapshot;
import com.orca.pbl4.ui.components.CpuChartPanel;
import com.orca.pbl4.ui.components.MemoryChartPanel;
import com.orca.pbl4.ui.utils.DataRefresher;

import javax.swing.*;
import java.awt.*;

/**
 * Panel hiển thị performance charts
 * CPU, Memory, Disk, Network graphs theo thời gian
 */
public class PerformancePanel extends JPanel {
    
    private final SystemMonitor systemMonitor;
    private CpuChartPanel cpuChart;
    private MemoryChartPanel memoryChart;
    private JLabel diskLabel;
    private JLabel networkLabel;
    
    private DataRefresher refresher;
    private SystemSnapshot previousSnapshot;
    
    public PerformancePanel(SystemMonitor systemMonitor) {
        this.systemMonitor = systemMonitor;
        initializeUI();
    }
    
    private void initializeUI() {
        setLayout(new GridLayout(2, 2, 10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // CPU Chart
        cpuChart = new CpuChartPanel();
        add(createChartPanel("CPU Usage", cpuChart));
        
        // Memory Chart
        memoryChart = new MemoryChartPanel();
        add(createChartPanel("Memory Usage", memoryChart));
        
        // Disk I/O (placeholder)
        JPanel diskPanel = new JPanel(new BorderLayout());
        diskPanel.setBorder(BorderFactory.createTitledBorder("Disk I/O"));
        diskLabel = new JLabel("Read: 0 MB/s | Write: 0 MB/s", JLabel.CENTER);
        diskPanel.add(diskLabel, BorderLayout.CENTER);
        add(diskPanel);
        
        // Network (placeholder)
        JPanel networkPanel = new JPanel(new BorderLayout());
        networkPanel.setBorder(BorderFactory.createTitledBorder("Network"));
        networkLabel = new JLabel("RX: 0 MB/s | TX: 0 MB/s", JLabel.CENTER);
        networkPanel.add(networkLabel, BorderLayout.CENTER);
        add(networkPanel);
    }
    
    private JPanel createChartPanel(String title, JPanel chart) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(chart, BorderLayout.CENTER);
        return panel;
    }
    
    public void startRefresh() {
        refresher = new DataRefresher(1000, this::updateData);
        refresher.start();
    }
    
    public void stopRefresh() {
        if (refresher != null) {
            refresher.stop();
        }
    }
    
    public void refreshNow() {
        updateData();
    }
    
    private void updateData() {
        try {
            SystemSnapshot currentSnapshot = systemMonitor.getSystemSnapshot(false);
            
            SwingUtilities.invokeLater(() -> {
                // Update CPU chart
                if (previousSnapshot != null) {
                    cpuChart.addData(previousSnapshot.getCpu(), currentSnapshot.getCpu());
                }
                
                // Update Memory chart
                memoryChart.addData(currentSnapshot.getMemory());
                
                // Update Disk I/O
                if (currentSnapshot.getDiskTotal() != null && previousSnapshot != null && previousSnapshot.getDiskTotal() != null) {
                    long readDelta = currentSnapshot.getDiskTotal().readBytes() - previousSnapshot.getDiskTotal().readBytes();
                    long writeDelta = currentSnapshot.getDiskTotal().writeBytes() - previousSnapshot.getDiskTotal().writeBytes();
                    diskLabel.setText(String.format("Read: %.2f MB/s | Write: %.2f MB/s", 
                        readDelta / 1024.0 / 1024.0, 
                        writeDelta / 1024.0 / 1024.0));
                }
                
                // Update Network
                if (currentSnapshot.getNetTotal() != null && previousSnapshot != null && previousSnapshot.getNetTotal() != null) {
                    long rxDelta = currentSnapshot.getNetTotal().getRxBytes() - previousSnapshot.getNetTotal().getRxBytes();
                    long txDelta = currentSnapshot.getNetTotal().getTxBytes() - previousSnapshot.getNetTotal().getTxBytes();
                    networkLabel.setText(String.format("RX: %.2f MB/s | TX: %.2f MB/s",
                        rxDelta / 1024.0 / 1024.0,
                        txDelta / 1024.0 / 1024.0));
                }
            });
            
            previousSnapshot = currentSnapshot;
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
