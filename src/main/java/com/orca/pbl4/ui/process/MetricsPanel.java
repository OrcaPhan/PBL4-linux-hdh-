package com.orca.pbl4.ui.process;

import com.orca.pbl4.core.model.CpuInfo;
import com.orca.pbl4.core.model.MemoryInfo;
import com.orca.pbl4.core.model.NetworkInfo;
import com.orca.pbl4.core.model.ProcessRow;
import com.orca.pbl4.core.system.SystemSnapshot;
import com.orca.pbl4.service.process.ProcessUpdateListener;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Pane hiển thị đồ thị CPU / RAM / Network.
 */
public class MetricsPanel extends JPanel implements ProcessUpdateListener {

    private final MetricGraph cpuGraph = new MetricGraph("CPU", "%", new Color(0xFF7043));
    private final MetricGraph memGraph = new MetricGraph("Memory", "%", new Color(0x43A047));
    private final MetricGraph netGraph = new MetricGraph("Network", "KB/s", new Color(0x1E88E5));

    private long prevTotal;
    private long prevIdle;
    private long prevNetBytes;
    private long prevTimestamp;

    public MetricsPanel() {
        setLayout(new GridLayout(1, 3, 12, 0));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        netGraph.setDynamicScale(true);
        add(wrap(cpuGraph));
        add(wrap(memGraph));
        add(wrap(netGraph));
    }

    private JPanel wrap(MetricGraph graph) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setBackground(new Color(0x1E1E1E));
        panel.add(graph);
        return panel;
    }

    @Override
    public synchronized void onProcessSnapshotUpdated(List<ProcessRow> rows, SystemSnapshot snapshot) {
        if (snapshot == null) return;
        CpuInfo cpu = snapshot.getCpu();
        MemoryInfo mem = snapshot.getMemory();
        NetworkInfo net = snapshot.getNetTotal();

        float cpuPercent = computeCpu(cpu);
        float memPercent = computeMem(mem);
        float netKbps = computeNet(net, snapshot.getCollectedAtNanos());

        cpuGraph.addPoint(cpuPercent);
        memGraph.addPoint(memPercent);
        netGraph.addPoint(netKbps);
    }

    private float computeCpu(CpuInfo cpu) {
        long total = cpu.getTotalTicks();
        long idle = cpu.getIdle() + cpu.getIowait();
        float percent = 0f;
        if (prevTotal != 0) {
            long deltaTotal = total - prevTotal;
            long deltaIdle = idle - prevIdle;
            if (deltaTotal > 0) percent = (deltaTotal - deltaIdle) * 100f / deltaTotal;
        }
        prevTotal = total;
        prevIdle = idle;
        return clamp(percent, 0f, 100f);
    }

    private float computeMem(MemoryInfo mem) {
        long total = mem.getTotalKB();
        long used = mem.getUsedKB();
        return total > 0 ? clamp(used * 100f / total, 0f, 100f) : 0f;
    }

    private float computeNet(NetworkInfo net, long timestamp) {
        if (net == null) return 0f;
        long bytes = net.getRxBytes() + net.getTxBytes();
        float kbps = 0f;
        if (prevTimestamp > 0 && timestamp > prevTimestamp && bytes >= prevNetBytes) {
            long deltaBytes = bytes - prevNetBytes;
            double seconds = (timestamp - prevTimestamp) / 1_000_000_000d;
            if (seconds > 0) {
                kbps = (float) ((deltaBytes / 1024d) / seconds);
            }
        }
        prevNetBytes = bytes;
        prevTimestamp = timestamp;
        return kbps;
    }

    private float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static class MetricGraph extends JPanel {
        private final Deque<Float> points = new ArrayDeque<>();
        private final int maxPoints = 120;
        private final Color color;
        private final String title;
        private final String unit;
        private boolean dynamicScale = false;
        private float maxValue = 100f;
        private float lastValue = 0f;
        private final DecimalFormat df = new DecimalFormat("0.0");

        MetricGraph(String title, String unit, Color color) {
            this.title = title;
            this.unit = unit;
            this.color = color;
            setPreferredSize(new Dimension(300, 180));
            setBackground(new Color(0x1E1E1E));
        }

        void setDynamicScale(boolean dynamic) {
            this.dynamicScale = dynamic;
        }

        void addPoint(float value) {
            lastValue = value;
            points.addLast(value);
            while (points.size() > maxPoints) {
                points.removeFirst();
            }
            if (dynamicScale) {
                float maxSeen = 1f;
                for (float v : points) maxSeen = Math.max(maxSeen, v);
                maxValue = Math.max(1f, maxSeen * 1.2f);
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(new Color(255, 255, 255, 30));
            for (int i = 1; i < 4; i++) {
                int y = i * getHeight() / 4;
                g2.drawLine(0, y, getWidth(), y);
            }

            g2.setColor(Color.WHITE);
            String label = title + " — " + df.format(lastValue) + " " + unit;
            g2.drawString(label, 10, 18);

            if (points.size() >= 2) {
                Path2D path = new Path2D.Float();
                float usableHeight = getHeight() - 30;
                float step = (float) getWidth() / (Math.max(1, points.size() - 1));
                int i = 0;
                for (float v : points) {
                    float ratio = Math.min(1f, v / maxValue);
                    float x = i * step;
                    float y = getHeight() - 10 - ratio * usableHeight;
                    if (i == 0) path.moveTo(x, y);
                    else path.lineTo(x, y);
                    i++;
                }
                g2.setColor(color);
                g2.setStroke(new BasicStroke(2f));
                g2.draw(path);
            }

            g2.dispose();
        }
    }
}

