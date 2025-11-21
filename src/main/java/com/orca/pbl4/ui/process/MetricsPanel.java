//package com.orca.pbl4.ui.process;
//
//import com.orca.pbl4.core.model.CpuInfo;
//import com.orca.pbl4.core.model.MemoryInfo;
//import com.orca.pbl4.core.model.NetworkInfo;
//import com.orca.pbl4.core.model.ProcessRow;
//import com.orca.pbl4.core.system.SystemSnapshot;
//import com.orca.pbl4.service.process.ProcessUpdateListener;
//
//import javax.swing.*;
//import java.awt.*;
//import java.awt.geom.Ellipse2D;
//import java.awt.geom.Path2D;
//import java.text.DecimalFormat;
//import java.util.ArrayDeque;
//import java.util.Deque;
//import java.util.List;
//
///**
// * Panel hiển thị đồ thị System Monitor với sidebar + 3 biểu đồ.
// */
//public class MetricsPanel extends JPanel implements ProcessUpdateListener {
//
//    // 60 điểm -> tuỳ vào period của Timer bên ngoài
//    private static final int MAX_POINTS = 60;
//    static final DecimalFormat DF1 = new DecimalFormat("0.0");
//    static final DecimalFormat DF2 = new DecimalFormat("0.00");
//
//    // Sidebar labels
//    private final JLabel lblCpuPercent = new JLabel("0.0%");
//    private final JLabel lblCpuCores = new JLabel("0 cores");
//    private final JLabel lblMemPercent = new JLabel("0.0%");
//    private final JLabel lblSwap = new JLabel("not available");
//    private final JLabel lblNetDown = new JLabel("0 bytes/s");
//    private final JLabel lblNetUp = new JLabel("0 bytes/s");
//
//    // RAM gauge ở sidebar
//    private final MemoryGaugePanel memGaugePanel = new MemoryGaugePanel();
//
//    // Charts
//    private final CpuChart cpuChart = new CpuChart();
//    private final MemoryChart memChart = new MemoryChart();
//    private final NetworkChart netChart = new NetworkChart();
//
//    // State for delta calculation
//    private long prevCpuTotal = 0;
//    private long prevCpuIdle = 0;
//    private long prevNetRx = 0;
//    private long prevNetTx = 0;
//    private long prevTimestamp = 0;
//
//    // State để sidebar/gauge dùng
//    private MemoryInfo lastMem;
//    private float lastMemPercent = 0f;
//
//    public MetricsPanel() {
//        setLayout(new BorderLayout());
//        setBackground(Color.WHITE);
//
//        JPanel sidebar = createSidebar();
//        add(sidebar, BorderLayout.WEST);
//
//        JPanel chartsPanel = new JPanel(new GridLayout(3, 1, 0, 8));
//        chartsPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
//        chartsPanel.setBackground(Color.WHITE);
//        chartsPanel.add(cpuChart);
//        chartsPanel.add(memChart);
//        chartsPanel.add(netChart);
//        add(chartsPanel, BorderLayout.CENTER);
//    }
//
//    private JPanel createSidebar() {
//        // sidebar chia 3 block cao đều -> khớp 3 chart
//        JPanel sidebar = new JPanel(new GridLayout(3, 1, 0, 8));
//        sidebar.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 0));
//        sidebar.setBackground(new Color(0xF5F5F5));
//        sidebar.setPreferredSize(new Dimension(220, 0));
//
//        // ----- CPU block -----
//        JPanel cpuPanel = new JPanel();
//        cpuPanel.setOpaque(false);
//        cpuPanel.setLayout(new BoxLayout(cpuPanel, BoxLayout.Y_AXIS));
//        cpuPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
//        cpuPanel.add(createLabel("CPU", true));
//        cpuPanel.add(lblCpuPercent);
//        cpuPanel.add(lblCpuCores);
//        cpuPanel.add(Box.createVerticalGlue());
//
//        // ----- Memory block -----
//        JPanel memPanel = new JPanel();
//        memPanel.setOpaque(false);
//        memPanel.setLayout(new BoxLayout(memPanel, BoxLayout.Y_AXIS));
//        memPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
//
//        memPanel.add(createLabel("Memory", true));
//        memPanel.add(lblMemPercent);
//        memPanel.add(Box.createVerticalStrut(6));
//
//        memGaugePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
//        memPanel.add(memGaugePanel);
//        memPanel.add(Box.createVerticalStrut(6));
//
//        memPanel.add(createLabel("Swap", false));
//        memPanel.add(lblSwap);
//        memPanel.add(Box.createVerticalGlue());
//
//        // ----- Network block -----
//        JPanel netPanel = new JPanel();
//        netPanel.setOpaque(false);
//        netPanel.setLayout(new BoxLayout(netPanel, BoxLayout.Y_AXIS));
//        netPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
//        netPanel.add(createLabel("Network", true));
//        netPanel.add(createLabel("Receiving", false));
//        netPanel.add(lblNetDown);
//        netPanel.add(createLabel("Sending", false));
//        netPanel.add(lblNetUp);
//        netPanel.add(Box.createVerticalStrut(4));
//        netPanel.add(new JLabel("Range: 0 bytes/s – 100 MB/s"));
//        netPanel.add(Box.createVerticalGlue());
//
//        sidebar.add(cpuPanel);
//        sidebar.add(memPanel);
//        sidebar.add(netPanel);
//
//        return sidebar;
//    }
//
//    private JLabel createLabel(String text, boolean bold) {
//        JLabel lbl = new JLabel(text);
//        lbl.setFont(new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, 12));
//        return lbl;
//    }
//
//    @Override
//    public synchronized void onProcessSnapshotUpdated(List<ProcessRow> rows, SystemSnapshot snapshot) {
//        if (snapshot == null) return;
//
//        SwingUtilities.invokeLater(() -> {
//            CpuInfo cpu = snapshot.getCpu();
//            MemoryInfo mem = snapshot.getMemory();
//            NetworkInfo net = snapshot.getNetTotal();
//
//            // CPU
//            float cpuPercent = computeCpuPercent(cpu);
//            int cores = cpu.getCoreCount() > 0 ? cpu.getCoreCount()
//                    : Runtime.getRuntime().availableProcessors();
//            lblCpuPercent.setText(DF1.format(cpuPercent) + "%");
//            lblCpuCores.setText(cores + " cores");
//            cpuChart.addPoint(cpuPercent);
//
//            // Memory
//            float memPercent = computeMemPercent(mem);
//            lastMemPercent = memPercent;
//            lastMem = mem;
//            lblMemPercent.setText(DF1.format(memPercent) + "%");
//
//            long swapTotal = mem.getSwapTotalKB();
//            if (swapTotal > 0) {
//                long swapUsed = mem.getSwapUsedKB();
//                float swapPercent = swapUsed * 100f / swapTotal;
//                lblSwap.setText(DF1.format(swapPercent) + "%");
//            } else {
//                lblSwap.setText("not available");
//            }
//            memChart.addPoint(memPercent);
//            memGaugePanel.repaint();
//
//            // Network
//            float[] netRates = computeNetRates(net, snapshot.getCollectedAtNanos());
//            float rxKbps = netRates[0];
//            float txKbps = netRates[1];
//            lblNetDown.setText(formatBytesPerSec(rxKbps * 1024));
//            lblNetUp.setText(formatBytesPerSec(txKbps * 1024));
//            netChart.addPoints(rxKbps, txKbps);
//        });
//    }
//
//    private float computeCpuPercent(CpuInfo cpu) {
//        long total = cpu.getTotalTicks();
//        long idle = cpu.getIdle() + cpu.getIowait();
//        float percent = 0f;
//        if (prevCpuTotal > 0) {
//            long deltaTotal = total - prevCpuTotal;
//            long deltaIdle = idle - prevCpuIdle;
//            if (deltaTotal > 0) {
//                percent = (deltaTotal - deltaIdle) * 100f / deltaTotal;
//            }
//        }
//        prevCpuTotal = total;
//        prevCpuIdle = idle;
//        return Math.max(0f, Math.min(100f, percent));
//    }
//
//    private float computeMemPercent(MemoryInfo mem) {
//        long total = mem.getTotalKB();
//        long used = mem.getUsedKB();
//        return total > 0
//                ? Math.max(0f, Math.min(100f, used * 100f / total))
//                : 0f;
//    }
//
//    private float[] computeNetRates(NetworkInfo net, long timestamp) {
//        float rxKbps = 0f;
//        float txKbps = 0f;
//        if (net != null && prevTimestamp > 0 && timestamp > prevTimestamp) {
//            long deltaRx = net.getRxBytes() - prevNetRx;
//            long deltaTx = net.getTxBytes() - prevNetTx;
//            double seconds = (timestamp - prevTimestamp) / 1_000_000_000d;
//            if (seconds > 0) {
//                rxKbps = (float) ((deltaRx / 1024d) / seconds);
//                txKbps = (float) ((deltaTx / 1024d) / seconds);
//            }
//        }
//        if (net != null) {
//            prevNetRx = net.getRxBytes();
//            prevNetTx = net.getTxBytes();
//        }
//        prevTimestamp = timestamp;
//        return new float[]{Math.max(0f, rxKbps), Math.max(0f, txKbps)};
//    }
//
//    private String formatBytesPerSec(float bytesPerSec) {
//        if (bytesPerSec < 1024) return DF1.format(bytesPerSec) + " bytes/s";
//        if (bytesPerSec < 1024 * 1024) return DF1.format(bytesPerSec / 1024f) + " KB/s";
//        return DF2.format(bytesPerSec / (1024f * 1024f)) + " MB/s";
//    }
//
//    // ===== helper: build series đủ MAX_POINTS, dồn điểm thật về bên phải
//    private static float[] buildSeries(Deque<Float> src) {
//        float[] arr = new float[MAX_POINTS];
//        if (src.isEmpty()) return arr;
//
//        float first = src.peekFirst();
//        for (int i = 0; i < MAX_POINTS; i++) {
//            arr[i] = first;
//        }
//        int n = src.size();
//        int idx = Math.max(0, MAX_POINTS - n);
//        for (Float v : src) {
//            if (idx >= 0 && idx < MAX_POINTS) {
//                arr[idx] = v;
//            }
//            idx++;
//        }
//        return arr;
//    }
//
//    private static String formatMemory(long kb) {
//        if (kb < 1024) return kb + " KB";
//        float mb = kb / 1024f;
//        if (mb < 1024) return DF1.format(mb) + " MB";
//        return DF2.format(mb / 1024f) + " GB";
//    }
//
//    // ========== CPU Chart ==========
//    private static class CpuChart extends JPanel {
//        private final Deque<Float> points = new ArrayDeque<>();
//        private static final Color LINE_COLOR = new Color(0xFF7043);
//
//        CpuChart() {
//            setPreferredSize(new Dimension(0, 200));
//            setBackground(Color.WHITE);
//            setBorder(BorderFactory.createTitledBorder("CPU"));
//        }
//
//        void addPoint(float value) {
//            points.addLast(value);
//            while (points.size() > MAX_POINTS) points.removeFirst();
//            repaint();
//        }
//
//        @Override
//        protected void paintComponent(Graphics g) {
//            super.paintComponent(g);
//            Graphics2D g2 = (Graphics2D) g.create();
//            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//
//            int w = getWidth();
//            int h = getHeight();
//            int chartY = 30;
//            int chartH = h - chartY - 10;
//
//            g2.setColor(new Color(0xE0E0E0));
//            for (int i = 0; i <= 5; i++) {
//                int y = chartY + (i * chartH / 5);
//                g2.drawLine(0, y, w, y);
//                String label = (100 - i * 20) + "%";
//                g2.setColor(Color.GRAY);
//                g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
//                g2.drawString(label, 5, y + 4);
//                g2.setColor(new Color(0xE0E0E0));
//            }
//
//            if (points.isEmpty()) {
//                g2.dispose();
//                return;
//            }
//
//            float[] series = buildSeries(points);
//            float stepX = (float) (w - 20) / (MAX_POINTS - 1);
//
//            Path2D path = new Path2D.Float();
//            for (int i = 0; i < MAX_POINTS; i++) {
//                float v = series[i];
//                float x = 10 + i * stepX;
//                float ratio = v / 100f;
//                float y = chartY + chartH - ratio * chartH;
//                if (i == 0) {
//                    path.moveTo(x, y);
//                    g2.setColor(LINE_COLOR);
//                    g2.fill(new Ellipse2D.Float(x - 3, y - 3, 6, 6));
//                } else {
//                    path.lineTo(x, y);
//                }
//            }
//
//            g2.setColor(LINE_COLOR);
//            g2.setStroke(new BasicStroke(2f));
//            g2.draw(path);
//
//            if (!points.isEmpty()) {
//                float last = points.getLast();
//                g2.setColor(Color.BLACK);
//                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
//                g2.drawString(DF1.format(last) + "%", w - 60, 20);
//            }
//
//            g2.dispose();
//        }
//    }
//
//    // ========== Memory Chart ==========
//    private static class MemoryChart extends JPanel {
//        private final Deque<Float> points = new ArrayDeque<>();
//        private static final Color LINE_COLOR = new Color(0x43A047);
//
//        MemoryChart() {
//            setPreferredSize(new Dimension(0, 200));
//            setBackground(Color.WHITE);
//            setBorder(BorderFactory.createTitledBorder("Memory and Swap"));
//        }
//
//        void addPoint(float percent) {
//            points.addLast(percent);
//            while (points.size() > MAX_POINTS) points.removeFirst();
//            repaint();
//        }
//
//        @Override
//        protected void paintComponent(Graphics g) {
//            super.paintComponent(g);
//            Graphics2D g2 = (Graphics2D) g.create();
//            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//
//            int w = getWidth();
//            int h = getHeight();
//
//            int chartY = 30;
//            int chartH = h - chartY - 10;
//
//            g2.setColor(new Color(0xE0E0E0));
//            for (int i = 0; i <= 5; i++) {
//                int y = chartY + (i * chartH / 5);
//                g2.drawLine(0, y, w, y);
//                String label = (100 - i * 20) + "%";
//                g2.setColor(Color.GRAY);
//                g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
//                g2.drawString(label, 5, y + 4);
//                g2.setColor(new Color(0xE0E0E0));
//            }
//
//            if (!points.isEmpty()) {
//                float[] series = buildSeries(points);
//                float stepX = (float) (w - 20) / (MAX_POINTS - 1);
//                Path2D path = new Path2D.Float();
//                for (int i = 0; i < MAX_POINTS; i++) {
//                    float v = series[i];
//                    float x = 10 + i * stepX;
//                    float ratio = v / 100f;
//                    float y = chartY + chartH - ratio * chartH;
//                    if (i == 0) path.moveTo(x, y);
//                    else path.lineTo(x, y);
//                }
//                g2.setColor(LINE_COLOR);
//                g2.setStroke(new BasicStroke(2f));
//                g2.draw(path);
//
//                float last = points.getLast();
//                g2.setColor(Color.BLACK);
//                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
//                g2.drawString(DF1.format(last) + "%", w - 60, 20);
//            }
//
//            g2.dispose();
//        }
//    }
//
//    // ========== RAM Gauge trong sidebar ==========
//    private class MemoryGaugePanel extends JPanel {
//        MemoryGaugePanel() {
//            setPreferredSize(new Dimension(140, 90));
//            setMaximumSize(new Dimension(140, 90));
//            setBackground(new Color(0xF5F5F5));
//        }
//
//        @Override
//        protected void paintComponent(Graphics g) {
//            super.paintComponent(g);
//            if (lastMem == null) return;
//
//            Graphics2D g2 = (Graphics2D) g.create();
//            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//
//            long total = lastMem.getTotalKB();
//            long used = lastMem.getUsedKB();
//            long cached = lastMem.getCachedKB();
//            long free = Math.max(0, total - used - cached);
//
//            if (total > 0) {
//                float usedAngle = 360f * used / total;
//                float cachedAngle = 360f * cached / total;
//                float freeAngle = 360f - usedAngle - cachedAngle;
//
//                int donutSize = 60;
//                int donutX = 10;
//                int donutY = 5;
//
//                int donutW = donutSize;
//                int donutH = donutSize;
//
//                int start = 90;
//
//                g2.setColor(new Color(0x6BCF7F));
//                g2.fillArc(donutX, donutY, donutW, donutH, start, Math.round(-freeAngle));
//                start -= freeAngle;
//
//                g2.setColor(new Color(0xFFD93D));
//                g2.fillArc(donutX, donutY, donutW, donutH, start, Math.round(-cachedAngle));
//                start -= cachedAngle;
//
//                g2.setColor(new Color(0xFF6B6B));
//                g2.fillArc(donutX, donutY, donutW, donutH, start, Math.round(-usedAngle));
//
//                g2.setColor(getBackground());
//                int inner = 30;
//                int innerX = donutX + (donutW - inner) / 2;
//                int innerY = donutY + (donutH - inner) / 2;
//                g2.fillOval(innerX, innerY, inner, inner);
//
//                g2.setColor(Color.DARK_GRAY);
//                g2.drawOval(donutX, donutY, donutW, donutH);
//
//                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
//                g2.setColor(Color.BLACK);
//                g2.drawString(DF1.format(lastMemPercent) + "%", innerX + 4, innerY + inner / 2 + 3);
//
//                g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
//                String memLabel = formatMemory(used) + " / " + formatMemory(total);
//                g2.drawString(memLabel, donutX, donutY + donutH + 15);
//            }
//
//            g2.dispose();
//        }
//    }
//
//    // ========== Network Chart ==========
//    private static class NetworkChart extends JPanel {
//        private final Deque<Float> rxPoints = new ArrayDeque<>();
//        private final Deque<Float> txPoints = new ArrayDeque<>();
//
//        // max hiện tại (KB/s), chọn trong {300KB, 1MB, 5MB, 10MB, 25MB, 50MB, 100MB}
//        private float maxKbps = 300f; // default 300 KB/s
//
//        private static final float[] LEVELS_KBPS = new float[]{
//                300f,
//                1024f,
//                5 * 1024f,
//                10 * 1024f,
//                25 * 1024f,
//                50 * 1024f,
//                100 * 1024f
//        };
//
//        private static final Color RX_COLOR = new Color(0x1E88E5);
//        private static final Color TX_COLOR = new Color(0xE53935);
//
//        NetworkChart() {
//            setPreferredSize(new Dimension(0, 200));
//            setBackground(Color.WHITE);
//            setBorder(BorderFactory.createTitledBorder("Network"));
//        }
//
//        void addPoints(float rxKbps, float txKbps) {
//            rxPoints.addLast(rxKbps);
//            txPoints.addLast(txKbps);
//            while (rxPoints.size() > MAX_POINTS) rxPoints.removeFirst();
//            while (txPoints.size() > MAX_POINTS) txPoints.removeFirst();
//
//            float peak = 0f;
//            for (Float v : rxPoints) peak = Math.max(peak, v);
//            for (Float v : txPoints) peak = Math.max(peak, v);
//
//            if (peak <= 0) {
//                maxKbps = LEVELS_KBPS[0];
//            } else {
//                for (float level : LEVELS_KBPS) {
//                    if (peak <= level * 0.8f) {
//                        maxKbps = level;
//                        break;
//                    }
//                    maxKbps = LEVELS_KBPS[LEVELS_KBPS.length - 1];
//                }
//            }
//
//            repaint();
//        }
//
//        @Override
//        protected void paintComponent(Graphics g) {
//            super.paintComponent(g);
//            Graphics2D g2 = (Graphics2D) g.create();
//            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//
//            int w = getWidth();
//            int h = getHeight();
//            int chartY = 30;
//            int chartH = h - chartY - 10;
//
//            // Grid + nhãn
//            g2.setColor(new Color(0xE0E0E0));
//            for (int i = 0; i <= 5; i++) {
//                int y = chartY + (i * chartH / 5);
//                g2.drawLine(0, y, w, y);
//                float valueKbps = maxKbps - i * (maxKbps / 5f);
//                String label;
//                float mbps = valueKbps / 1024f;
//                if (mbps < 1f) label = DF1.format(valueKbps) + " KB/s";
//                else label = DF1.format(mbps) + " MB/s";
//                g2.setColor(Color.GRAY);
//                g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
//                g2.drawString(label, 5, y + 4);
//                g2.setColor(new Color(0xE0E0E0));
//            }
//
//            if (rxPoints.isEmpty() && txPoints.isEmpty()) {
//                g2.dispose();
//                return;
//            }
//
//            drawLine(g2, rxPoints, RX_COLOR, w, chartH, chartY, maxKbps);
//            drawLine(g2, txPoints, TX_COLOR, w, chartH, chartY, maxKbps);
//
//            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
//            if (!rxPoints.isEmpty()) {
//                float rxLast = rxPoints.getLast();
//                g2.setColor(RX_COLOR);
//                g2.fillRect(w - 200, 5, 12, 12);
//                g2.setColor(Color.BLACK);
//                g2.drawString("Receiving " + formatRate(rxLast), w - 185, 15);
//            }
//            if (!txPoints.isEmpty()) {
//                float txLast = txPoints.getLast();
//                g2.setColor(TX_COLOR);
//                g2.fillRect(w - 200, 20, 12, 12);
//                g2.setColor(Color.BLACK);
//                g2.drawString("Sending " + formatRate(txLast), w - 185, 30);
//            }
//
//            g2.dispose();
//        }
//
//        private void drawLine(Graphics2D g2, Deque<Float> src, Color color,
//                              int w, int chartH, int chartY, float maxKbps) {
//            if (src.isEmpty()) return;
//
//            float[] series = buildSeries(src);
//            float stepX = (float) (w - 20) / (MAX_POINTS - 1);
//            Path2D path = new Path2D.Float();
//
//            for (int i = 0; i < MAX_POINTS; i++) {
//                float v = series[i];
//                float ratio = Math.min(1f, v / maxKbps);
//                float x = 10 + i * stepX;
//                float y = chartY + chartH - ratio * chartH;
//                if (i == 0) path.moveTo(x, y);
//                else path.lineTo(x, y);
//            }
//
//            g2.setColor(color);
//            g2.setStroke(new BasicStroke(2f));
//            g2.draw(path);
//        }
//
//        private String formatRate(float kbps) {
//            if (kbps < 1024f) return DF1.format(kbps) + " KB/s";
//            return DF2.format(kbps / 1024f) + " MB/s";
//        }
//    }
//}
