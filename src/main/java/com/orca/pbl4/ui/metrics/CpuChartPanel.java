package com.orca.pbl4.ui.metrics;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static com.orca.pbl4.ui.metrics.MetricsPanel.MAX_POINTS;


public class CpuChartPanel extends JPanel {

    private final List<Deque<Float>> coreSeries = new ArrayList<>();
    private int coreCount = 0;



    static final Color[] CORE_COLORS = {
            new Color(0xFF0000),
            new Color(0xFB8C00),
            new Color(0xBA9502),
            new Color(0x72FF7A),
            new Color(0x00B13E),
            new Color(0x0CE3FF),
            new Color(0x0084FF),
            new Color(0x8E24AA),
            new Color(0xBC3FFF),
            new Color(0xFF00A3),
            new Color(0x4A0227),
            new Color(0x1A2F70),
    };

    public CpuChartPanel() {
        setPreferredSize(new Dimension(0, 200));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createTitledBorder("CPU"));
    }

    public void setCoreCount(int cores) {
        this.coreCount = Math.max(0, cores);
        // Đảm bảo có đủ Deque cho từng core
        while (coreSeries.size() < coreCount) {
            coreSeries.add(new ArrayDeque<>());
        }
        // Xóa các Deque thừa nếu coreCount giảm
        while (coreSeries.size() > coreCount) {
            coreSeries.remove(coreSeries.size() - 1);
        }
    }

    public void addPerCoreSample(float[] perCorePercent) {
        if (perCorePercent == null || coreSeries.isEmpty()) return;

        int actualCores = Math.min(Math.min(coreCount, perCorePercent.length), coreSeries.size());
        for (int i = 0; i < actualCores; i++) {
            Deque<Float> coreDeque = coreSeries.get(i);
            float value = perCorePercent[i];
            if (Float.isNaN(value)) {
                value = 0f;
            }
            coreDeque.addLast(value);
            while (coreDeque.size() > MAX_POINTS) coreDeque.removeFirst();
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (coreSeries.stream().allMatch(Deque::isEmpty)) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int chartY = 30;
        int chartH = h - chartY - 10;

        // Vẽ grid 0-100%
        g2.setColor(new Color(0xE0E0E0));
        for (int i = 0; i <= 5; i++) {
            int y = chartY + (i * chartH / 5);
            g2.drawLine(0, y, w, y);
            String label = (100 - i * 20) + "%";
            g2.setColor(Color.GRAY);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            g2.drawString(label, 5, y + 4);
            g2.setColor(new Color(0xE0E0E0));
        }

        for (int i = 0; i < coreCount && i < coreSeries.size(); i++) {
            Deque<Float> coreDeque = coreSeries.get(i);
            if (coreDeque.isEmpty()) continue;

            float[] series = ChartUtils.buildSeries(coreDeque);
            Path2D path = ChartUtils.createSmoothPathPercent(series, w, chartY, chartH);

            Color coreColor = CORE_COLORS[i % CORE_COLORS.length];
            g2.setColor(coreColor);
            g2.setStroke(new BasicStroke(1.75f));
            g2.draw(path);
        }

        g2.dispose();
    }
}
