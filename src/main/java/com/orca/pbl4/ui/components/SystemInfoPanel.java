package com.orca.pbl4.ui.components;

import com.orca.pbl4.core.model.CpuInfo;
import com.orca.pbl4.core.model.MemoryInfo;

import javax.swing.*;
import java.awt.*;

/**
 * Panel hiển thị thông tin tổng quan CPU và Memory
 */
public class SystemInfoPanel extends JPanel {
    
    private JLabel cpuLabel;
    private JProgressBar cpuProgressBar;
    private JLabel memoryLabel;
    private JProgressBar memoryProgressBar;
    
    public SystemInfoPanel() {
        initializeUI();
    }
    
    private void initializeUI() {
        setLayout(new GridLayout(2, 2, 10, 5));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("System Summary"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        
        // CPU
        cpuLabel = new JLabel("CPU: 0.0%");
        cpuLabel.setFont(cpuLabel.getFont().deriveFont(Font.BOLD, 14f));
        add(cpuLabel);
        
        cpuProgressBar = new JProgressBar(0, 100);
        cpuProgressBar.setStringPainted(true);
        add(cpuProgressBar);
        
        // Memory
        memoryLabel = new JLabel("Memory: 0 GB / 0 GB");
        memoryLabel.setFont(memoryLabel.getFont().deriveFont(Font.BOLD, 14f));
        add(memoryLabel);
        
        memoryProgressBar = new JProgressBar(0, 100);
        memoryProgressBar.setStringPainted(true);
        add(memoryProgressBar);
    }
    
    public void update(CpuInfo prevCpu, CpuInfo currentCpu, MemoryInfo memory) {
        // Update CPU
        if (prevCpu != null && currentCpu != null) {
            double cpuPercent = calculateCpuPercent(prevCpu, currentCpu);
            cpuLabel.setText(String.format("CPU: %.1f%%", cpuPercent));
            cpuProgressBar.setValue((int) cpuPercent);
            
            // Color coding
            if (cpuPercent > 80) {
                cpuProgressBar.setForeground(Color.RED);
            } else if (cpuPercent > 50) {
                cpuProgressBar.setForeground(Color.ORANGE);
            } else {
                cpuProgressBar.setForeground(Color.GREEN);
            }
        }
        
        // Update Memory
        if (memory != null) {
            long totalKB = memory.getTotalKB();
            long availKB = memory.getAvailableKB();
            long usedKB = totalKB - availKB;
            
            double usedGB = usedKB / 1024.0 / 1024.0;
            double totalGB = totalKB / 1024.0 / 1024.0;
            double memPercent = 100.0 * usedKB / totalKB;
            
            memoryLabel.setText(String.format("Memory: %.1f GB / %.1f GB", usedGB, totalGB));
            memoryProgressBar.setValue((int) memPercent);
            
            // Color coding
            if (memPercent > 80) {
                memoryProgressBar.setForeground(Color.RED);
            } else if (memPercent > 50) {
                memoryProgressBar.setForeground(Color.ORANGE);
            } else {
                memoryProgressBar.setForeground(Color.GREEN);
            }
        }
    }
    
    private double calculateCpuPercent(CpuInfo prev, CpuInfo current) {
        long dTotal = current.getTotalTicks() - prev.getTotalTicks();
        long dActive = (current.getUser() - prev.getUser()) +
                      (current.getNice() - prev.getNice()) +
                      (current.getSystem() - prev.getSystem()) +
                      (current.getIrq() - prev.getIrq()) +
                      (current.getSoftirq() - prev.getSoftirq()) +
                      (current.getSteal() - prev.getSteal());
        
        if (dTotal <= 0) return 0.0;
        return 100.0 * dActive / dTotal;
    }
}
