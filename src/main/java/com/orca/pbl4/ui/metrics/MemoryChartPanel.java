package com.orca.pbl4.ui.metrics;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.ArrayDeque;
import java.util.Deque;

import static com.orca.pbl4.ui.metrics.MetricsPanel.DF1;
import static com.orca.pbl4.ui.metrics.MetricsPanel.MAX_POINTS;

public class MemoryChartPanel extends JPanel {

    private final Deque<Float> points = new ArrayDeque<>();
    private static final Color LINE_COLOR = new Color(0x43A047);

    public MemoryChartPanel() {
        setPreferredSize(new Dimension(0, 200));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createTitledBorder("Memory and Swap"));
    }

    public void addPoint(float percent) {
        points.addLast(percent);
        while (points.size() > MAX_POINTS) points.removeFirst();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (points.isEmpty()) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int chartY = 30;
        int chartH = h - chartY - 10;

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

        float[] series = ChartUtils.buildSeries(points);
        Path2D path = ChartUtils.createSmoothPathPercent(series, w, chartY, chartH);

        g2.setColor(LINE_COLOR);
        g2.setStroke(new BasicStroke(2f));
        g2.draw(path);

        float last = points.getLast();
        g2.setColor(Color.BLACK);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        g2.drawString(DF1.format(last) + "%", w - 60, 20);

        g2.dispose();
    }
}
