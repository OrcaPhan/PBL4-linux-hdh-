package com.orca.pbl4.ui.components;

import com.orca.pbl4.core.model.MemoryInfo;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel vẽ biểu đồ Memory usage theo thời gian
 */
public class MemoryChartPanel extends JPanel {
    
    private static final int MAX_DATA_POINTS = 60; // 60 seconds
    private List<Double> memoryHistory = new ArrayList<>();
    
    public MemoryChartPanel() {
        setPreferredSize(new Dimension(400, 200));
        setBackground(Color.WHITE);
    }
    
    public void addData(MemoryInfo memory) {
        long totalKB = memory.getTotalKB();
        long availKB = memory.getAvailableKB();
        long usedKB = totalKB - availKB;
        
        double memPercent = 100.0 * usedKB / totalKB;
        
        memoryHistory.add(memPercent);
        if (memoryHistory.size() > MAX_DATA_POINTS) {
            memoryHistory.remove(0);
        }
        
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (memoryHistory.isEmpty()) return;
        
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int width = getWidth();
        int height = getHeight();
        int padding = 40;
        
        // Draw axes
        g2.setColor(Color.GRAY);
        g2.drawLine(padding, height - padding, width - padding, height - padding); // X-axis
        g2.drawLine(padding, padding, padding, height - padding); // Y-axis
        
        // Draw labels
        g2.setColor(Color.BLACK);
        g2.drawString("100%", 5, padding);
        g2.drawString("50%", 10, (height - padding) / 2);
        g2.drawString("0%", 15, height - padding);
        
        // Draw data
        if (memoryHistory.size() > 1) {
            g2.setColor(new Color(0, 180, 0)); // Green
            g2.setStroke(new BasicStroke(2));
            
            int dataWidth = width - 2 * padding;
            int dataHeight = height - 2 * padding;
            
            for (int i = 0; i < memoryHistory.size() - 1; i++) {
                int x1 = padding + (i * dataWidth / MAX_DATA_POINTS);
                int y1 = height - padding - (int) (memoryHistory.get(i) * dataHeight / 100);
                
                int x2 = padding + ((i + 1) * dataWidth / MAX_DATA_POINTS);
                int y2 = height - padding - (int) (memoryHistory.get(i + 1) * dataHeight / 100);
                
                g2.drawLine(x1, y1, x2, y2);
            }
        }
        
        // Draw current value
        if (!memoryHistory.isEmpty()) {
            double current = memoryHistory.get(memoryHistory.size() - 1);
            g2.setColor(Color.BLACK);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 16f));
            g2.drawString(String.format("%.1f%%", current), width - 80, 30);
        }
    }
}
