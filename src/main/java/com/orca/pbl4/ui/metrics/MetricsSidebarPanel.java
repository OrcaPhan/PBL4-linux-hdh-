package com.orca.pbl4.ui.metrics;

import com.orca.pbl4.core.model.MemoryInfo;

import javax.swing.*;
import java.awt.*;

import static com.orca.pbl4.ui.metrics.MetricsPanel.DF1;
import static com.orca.pbl4.ui.metrics.MetricsPanel.DF2;

public class MetricsSidebarPanel extends JPanel {

    private final JLabel lblCpuPercent = new JLabel("0.0%");
    private final JLabel lblCpuCores = new JLabel("0 cores");

    private final JLabel lblMemPercent = new JLabel("0.0%");
    private final JLabel lblCachePercent = new JLabel("0.0%");
    private final JLabel lblSwap = new JLabel("not available");
    private final MemoryGaugePanel memGauge = new MemoryGaugePanel();

    private final JLabel lblNetDown = new JLabel("0 bytes/s");
    private final JLabel lblNetUp = new JLabel("0 bytes/s");

    private MemoryInfo lastMem;
    private float lastMemPercent;

    public MetricsSidebarPanel() {
        setLayout(new GridLayout(3, 1, 0, 8));
        setPreferredSize(new Dimension(220, 0));
        setBackground(new Color(0xF5F5F5));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 0));

        // CPU block
        JPanel cpuPanel = new JPanel();
        cpuPanel.setOpaque(false);
        cpuPanel.setLayout(new BoxLayout(cpuPanel, BoxLayout.Y_AXIS));
        cpuPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        cpuPanel.add(label("CPU", true));
        cpuPanel.add(lblCpuPercent);
        cpuPanel.add(lblCpuCores);
        cpuPanel.add(Box.createVerticalGlue());

        // Memory block
        JPanel memPanel = new JPanel();
        memPanel.setOpaque(false);
        memPanel.setLayout(new BoxLayout(memPanel, BoxLayout.Y_AXIS));
        memPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        memPanel.add(label("Memory", true));
        memPanel.add(lblMemPercent);
        memPanel.add(Box.createVerticalStrut(6));
        memPanel.add(label("Cache", false));
        memPanel.add(lblCachePercent);
        memGauge.setAlignmentX(Component.LEFT_ALIGNMENT);
        memPanel.add(memGauge);
        memPanel.add(Box.createVerticalStrut(6));
        memPanel.add(label("Swap", false));
        memPanel.add(lblSwap);
        memPanel.add(Box.createVerticalGlue());

        // Network block
        JPanel netPanel = new JPanel();
        netPanel.setOpaque(false);
        netPanel.setLayout(new BoxLayout(netPanel, BoxLayout.Y_AXIS));
        netPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        netPanel.add(label("Network", true));
        netPanel.add(label("Receiving", false));
        netPanel.add(lblNetDown);
        netPanel.add(label("Sending", false));
        netPanel.add(lblNetUp);
        netPanel.add(Box.createVerticalStrut(4));
        netPanel.add(new JLabel("Range: auto 0 – 100 MB/s"));
        netPanel.add(Box.createVerticalGlue());

        add(cpuPanel);
        add(memPanel);
        add(netPanel);
    }

    private JLabel label(String text, boolean bold) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, 12));
        return l;
    }

    // ==== update từ MetricsPanel ====
    public void updateCpu(String percentText, int cores) {
        lblCpuPercent.setText(percentText);
        lblCpuCores.setText(cores + " cores");
    }

    public void updateMemory(String percentText, MemoryInfo mem) {
        lblMemPercent.setText(percentText);
        this.lastMemPercent = Float.parseFloat(percentText.replace("%", ""));
        this.lastMem = mem;
        lblCachePercent.setText((float)lastMem.getCachedKB()/(1024*1024)+" GB");
        memGauge.repaint();
    }

    public void updateSwap(String text) {
        lblSwap.setText(text);
    }

    public void updateNetwork(String down, String up) {
        lblNetDown.setText(down);
        lblNetUp.setText(up);
    }

    // ==== RAM gauge ====
    private class MemoryGaugePanel extends JPanel {
        MemoryGaugePanel() {
            setPreferredSize(new Dimension(140, 90));
            setMaximumSize(new Dimension(140, 90));
            setBackground(new Color(0xF5F5F5));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (lastMem == null) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            long total = lastMem.getTotalKB();
            long used = lastMem.getUsedKB();
            long cached = lastMem.getCachedKB();
            long free = Math.max(0, total - used - cached);

            if (total > 0) {
                float usedAngle = 360f * used / total;
                float cachedAngle = 360f * cached / total;
                float freeAngle = 360f - usedAngle - cachedAngle;

                int donutSize = 60;
                int donutX = 10;
                int donutY = 5;

                int donutW = donutSize;
                int donutH = donutSize;

                int start = 90;

                // free
                g2.setColor(new Color(0x6BCF7F));
                g2.fillArc(donutX, donutY, donutW, donutH, start, Math.round(-freeAngle));
                start -= freeAngle;

                // cached
                g2.setColor(new Color(0xFFD93D));
                g2.fillArc(donutX, donutY, donutW, donutH, start, Math.round(-cachedAngle));
                start -= cachedAngle;

                // used
                g2.setColor(new Color(0xFF6B6B));
                g2.fillArc(donutX, donutY, donutW, donutH, start, Math.round(-usedAngle));

                // hole
                g2.setColor(getBackground());
                int inner = 30;
                int innerX = donutX + (donutW - inner) / 2;
                int innerY = donutY + (donutH - inner) / 2;
                g2.fillOval(innerX, innerY, inner, inner);

                g2.setColor(Color.DARK_GRAY);
                g2.drawOval(donutX, donutY, donutW, donutH);

                // % ở giữa
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
                g2.setColor(Color.BLACK);
                g2.drawString(DF1.format(lastMemPercent) + "%", innerX + 4, innerY + inner / 2 + 3);

                // text used / total
                g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
                String memLabel = formatMemory(used) + " / " + formatMemory(total);
                g2.drawString(memLabel, donutX, donutY + donutH + 15);
            }

            g2.dispose();
        }

        private String formatMemory(long kb) {
            if (kb < 1024) return kb + " KB";
            float mb = kb / 1024f;
            if (mb < 1024) return DF1.format(mb) + " MB";
            return DF2.format(mb / 1024f) + " GB";
        }
    }
}
