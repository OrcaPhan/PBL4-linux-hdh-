package com.orca.pbl4.ui.metrics;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.ArrayDeque;
import java.util.Deque;

import static com.orca.pbl4.ui.metrics.MetricsPanel.DF1;
import static com.orca.pbl4.ui.metrics.MetricsPanel.DF2;
import static com.orca.pbl4.ui.metrics.MetricsPanel.MAX_POINTS;

public class NetworkChartPanel extends JPanel {

    private final Deque<Float> rxPoints = new ArrayDeque<>();
    private final Deque<Float> txPoints = new ArrayDeque<>();

    // 7 mức scale từ 300KB/s -> 100MB/s
    private static final float[] LEVELS_KBPS = new float[]{
            300f,
            1024f,
            5 * 1024f,
            10 * 1024f,
            25 * 1024f,
            50 * 1024f,
            100 * 1024f
    };
    private float maxKbps = LEVELS_KBPS[0];

    private static final Color RX_COLOR = new Color(0x1E88E5);
    private static final Color TX_COLOR = new Color(0xE53935);

    public NetworkChartPanel() {
        setPreferredSize(new Dimension(0, 200));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createTitledBorder("Network"));
    }

    public void addPoints(float rxKbps, float txKbps) {
        rxPoints.addLast(rxKbps);
        txPoints.addLast(txKbps);
        while (rxPoints.size() > MAX_POINTS) rxPoints.removeFirst();
        while (txPoints.size() > MAX_POINTS) txPoints.removeFirst();

        float peak = 0f;
        for (Float v : rxPoints) peak = Math.max(peak, v);
        for (Float v : txPoints) peak = Math.max(peak, v);

        if (peak <= 0) {
            maxKbps = LEVELS_KBPS[0];
        } else {
            for (float level : LEVELS_KBPS) {
                if (peak <= level * 0.8f) {
                    maxKbps = level;
                    break;
                }
                maxKbps = LEVELS_KBPS[LEVELS_KBPS.length - 1];
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (rxPoints.isEmpty() && txPoints.isEmpty()) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int chartY = 30;
        int chartH = h - chartY - 10;

        // grid
        g2.setColor(new Color(0xE0E0E0));
        for (int i = 0; i <= 5; i++) {
            int y = chartY + (i * chartH / 5);
            g2.drawLine(0, y, w, y);
            float valueKbps = maxKbps - i * (maxKbps / 5f);
            String label;
            float mbps = valueKbps / 1024f;
            if (mbps < 1f) label = DF1.format(valueKbps) + " KB/s";
            else label = DF1.format(mbps) + " MB/s";
            g2.setColor(Color.GRAY);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            g2.drawString(label, 5, y + 4);
            g2.setColor(new Color(0xE0E0E0));
        }

        // receiving
        if (!rxPoints.isEmpty()) {
            float[] rxSeries = ChartUtils.buildSeries(rxPoints);
            Path2D rxPath = ChartUtils.createSmoothPathRate(rxSeries, w, chartY, chartH, maxKbps);
            g2.setColor(RX_COLOR);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(rxPath);
        }

        // sending
        if (!txPoints.isEmpty()) {
            float[] txSeries = ChartUtils.buildSeries(txPoints);
            Path2D txPath = ChartUtils.createSmoothPathRate(txSeries, w, chartY, chartH, maxKbps);
            g2.setColor(TX_COLOR);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(txPath);
        }

        // legend
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        if (!rxPoints.isEmpty()) {
            float rxLast = rxPoints.getLast();
            g2.setColor(RX_COLOR);
            g2.fillRect(w - 200, 5, 12, 12);
            g2.setColor(Color.BLACK);
            g2.drawString("Receiving " + formatRate(rxLast), w - 185, 15);
        }
        if (!txPoints.isEmpty()) {
            float txLast = txPoints.getLast();
            g2.setColor(TX_COLOR);
            g2.fillRect(w - 200, 20, 12, 12);
            g2.setColor(Color.BLACK);
            g2.drawString("Sending " + formatRate(txLast), w - 185, 30);
        }

        g2.dispose();
    }

    private String formatRate(float kbps) {
        if (kbps < 1024f) return DF1.format(kbps) + " KB/s";
        return DF2.format(kbps / 1024f) + " MB/s";
    }
}
