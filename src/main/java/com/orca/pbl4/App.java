package com.orca.pbl4;

import com.orca.pbl4.core.model.CpuInfo;
import com.orca.pbl4.core.model.MemoryInfo;
import com.orca.pbl4.core.model.NetworkInfo;
import com.orca.pbl4.core.model.ProcessRow;
import com.orca.pbl4.core.system.SystemMonitor;
import com.orca.pbl4.core.system.SystemSnapshot;
import com.orca.pbl4.service.ProcessManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.geom.Path2D;
import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * UI test đơn giản: bảng tiến trình giống System Monitor.
 * - Tự refresh mỗi giây
 * - Tìm kiếm theo PID/Name/User (lọc live)
 * - Sắp xếp theo cột chọn
 */
public class App {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::createAndShow);
    }

    private static void createAndShow() {
        JFrame f = new JFrame("PBL4 - Process Monitor (demo)");
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        f.setSize(1100, 640);
        f.setLocationRelativeTo(null);

        // ---- Core services
        SystemMonitor monitor = new SystemMonitor();
        ProcessManager pm = new ProcessManager(monitor, 1000); // windowMs=1000
        MetricsSampler metricsSampler = new MetricsSampler(monitor);

        // ---- Table
        ProcessTableModel model = new ProcessTableModel();
        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(false); // mình tự sort để giữ logic thống nhất
        table.setRowHeight(24);

        // ---- Controls: search + sort + refresh toggle
        JTextField txtSearch = new JTextField();
        txtSearch.setToolTipText("Tìm PID / tên / user ...");

        String[] sortCols = new String[]{
                "CPU %", "Memory %", "PID", "Name", "User", "Priority", "State"
        };
        JComboBox<String> cbSort = new JComboBox<>(sortCols);
        JCheckBox chkDesc = new JCheckBox("Desc", true);

        JSpinner spInterval = new JSpinner(new SpinnerNumberModel(1000, 250, 5000, 250));
        JLabel lbInterval = new JLabel("Interval (ms):");
        JToggleButton btnRun = new JToggleButton("⏵ Auto Refresh", true);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        JPanel left = new JPanel(new BorderLayout(6, 6));
        left.add(new JLabel("Search: "), BorderLayout.WEST);
        left.add(txtSearch, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        right.add(new JLabel("Sort:"));
        right.add(cbSort);
        right.add(chkDesc);
        right.add(lbInterval);
        right.add(spInterval);
        right.add(btnRun);

        top.add(left, BorderLayout.CENTER);
        top.add(right, BorderLayout.EAST);

        // ---- Metric graphs
        MetricGraphPanel cpuGraph = new MetricGraphPanel("CPU", "%", new Color(0xFF7043));
        cpuGraph.setMaxValue(100f);
        MetricGraphPanel memGraph = new MetricGraphPanel("Memory", "%", new Color(0x43A047));
        memGraph.setMaxValue(100f);
        MetricGraphPanel netGraph = new MetricGraphPanel("Network", "KB/s", new Color(0x1E88E5));
        netGraph.setDynamicScale(true);

        JPanel graphPanel = new JPanel(new GridLayout(1, 3, 12, 0));
        graphPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        graphPanel.add(cpuGraph);
        graphPanel.add(memGraph);
        graphPanel.add(netGraph);

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.add(graphPanel, BorderLayout.NORTH);
        center.add(new JScrollPane(table), BorderLayout.CENTER);

        f.setLayout(new BorderLayout());
        f.add(top, BorderLayout.NORTH);
        f.add(center, BorderLayout.CENTER);

        final List<ProcessRow>[] latestHolder = new List[]{new ArrayList<>()};

        Runnable applyFilterAndSort = () -> {
            List<ProcessRow> base = latestHolder[0];
            if (base == null) base = new ArrayList<>();
            String q = txtSearch.getText();
            List<ProcessRow> filtered = (q == null || q.isBlank())
                    ? new ArrayList<>(base)
                    : filterRows(base, q.trim());

            String col = Objects.toString(cbSort.getSelectedItem(), "CPU %");
            boolean desc = chkDesc.isSelected();
            filtered.sort(makeComparator(col, desc));
            model.setRows(filtered);
        };

        AtomicBoolean refreshInFlight = new AtomicBoolean(false);

        Runnable triggerRefresh = () -> {
            if (refreshInFlight.get()) return;
            refreshInFlight.set(true);
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                List<ProcessRow> fresh;
                MetricPoint metrics;

                @Override
                protected Void doInBackground() {
                    fresh = safeGet(pm::listProcessRows, new ArrayList<>());
                    metrics = safeGet(metricsSampler::sample, MetricPoint.empty());
                    return null;
                }

                @Override
                protected void done() {
                    refreshInFlight.set(false);
                    latestHolder[0] = (fresh != null) ? fresh : new ArrayList<>();
                    applyFilterAndSort.run();
                    if (metrics != null) {
                        cpuGraph.addPoint(metrics.cpuPercent());
                        memGraph.addPoint(metrics.memPercent());
                        netGraph.addPoint(metrics.netKbps());
                    }
                }
            };
            worker.execute();
        };

        // ---- Refresh timer
        Timer[] timerHolder = new Timer[1];
        timerHolder[0] = new Timer((Integer) spInterval.getValue(), e -> triggerRefresh.run());
        timerHolder[0].start();

        // ---- events
        btnRun.addActionListener(e -> {
            if (btnRun.isSelected()) {
                btnRun.setText("⏵ Auto Refresh");
                timerHolder[0].start();
            } else {
                btnRun.setText("⏸ Paused");
                timerHolder[0].stop();
            }
        });

        spInterval.addChangeListener(e -> {
            int ms = (Integer) spInterval.getValue();
            timerHolder[0].setDelay(ms);
            timerHolder[0].setInitialDelay(ms);
            pm.setPeriod(ms);
            triggerRefresh.run();
        });

        // Search live
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            private void refresh() { applyFilterAndSort.run(); }
            public void insertUpdate(DocumentEvent e) { refresh(); }
            public void removeUpdate(DocumentEvent e) { refresh(); }
            public void changedUpdate(DocumentEvent e) { refresh(); }
        });

        // Sort change
        cbSort.addActionListener(e -> applyFilterAndSort.run());
        chkDesc.addActionListener(e -> applyFilterAndSort.run());

        // first load
        triggerRefresh.run();

        f.setVisible(true);
    }

    // -------- Table Model --------
    static class ProcessTableModel extends AbstractTableModel {
        private final String[] cols = {
                "Process Name", "User", "% CPU", "ID", "Memory", "Disk read", "Disk write", "Priority", "State"
        };
        private final DecimalFormat df2 = new DecimalFormat("0.00");

        private List<ProcessRow> rows = new ArrayList<>();

        public void setRows(List<ProcessRow> list) {
            this.rows = (list != null) ? list : new ArrayList<>();
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int column) { return cols[column]; }

        @Override
        public Object getValueAt(int r, int c) {
            ProcessRow p = rows.get(r);
            return switch (c) {
                case 0 -> p.getName();
                case 1 -> p.getUser();
                case 2 -> df2.format(p.getCpuPercent());
                case 3 -> p.getPid();
                case 4 -> p.getMemoryStr();
                case 5 -> p.getDiskRead();
                case 6 -> p.getDiskWrite();
                case 7 -> p.getPriority();
                case 8 -> String.valueOf(p.getState());
                default -> "";
            };
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return switch (c) {
                case 2 -> String.class; // %CPU formatted
                case 3 -> Integer.class; // PID
                default -> String.class;
            };
        }
    }

    private record MetricPoint(float cpuPercent, float memPercent, float netKbps) {
        static MetricPoint empty() { return new MetricPoint(0f, 0f, 0f); }
    }

    private static class MetricGraphPanel extends JPanel {
        private final Deque<Float> points = new ArrayDeque<>();
        private final int maxPoints = 120;
        private final Color lineColor;
        private final String title;
        private final String unit;
        private float maxValue = 100f;
        private boolean dynamicScale = false;
        private float lastValue = 0f;
        private final DecimalFormat df = new DecimalFormat("0.0");

        MetricGraphPanel(String title, String unit, Color color) {
            this.title = title;
            this.unit = unit;
            this.lineColor = color;
            setOpaque(true);
            setBackground(new Color(0x1E1E1E));
            setPreferredSize(new Dimension(0, 140));
        }

        void setMaxValue(float maxValue) {
            this.maxValue = Math.max(1f, maxValue);
        }

        void setDynamicScale(boolean dynamicScale) {
            this.dynamicScale = dynamicScale;
        }

        void addPoint(float value) {
            lastValue = value;
            points.addLast(value);
            while (points.size() > maxPoints) points.removeFirst();

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
            int w = getWidth();
            int h = getHeight();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(getBackground());
            g2.fillRect(0, 0, w, h);

            g2.setColor(new Color(255, 255, 255, 30));
            for (int i = 1; i < 4; i++) {
                int y = i * h / 4;
                g2.drawLine(0, y, w, y);
            }

            g2.setColor(Color.WHITE);
            String valueLabel = unit.equals("%")
                    ? df.format(lastValue) + unit
                    : df.format(lastValue) + " " + unit;
            g2.drawString(title + " — " + valueLabel, 10, 18);

            if (points.size() < 2) {
                g2.dispose();
                return;
            }

            float usableHeight = h - 30;
            float xStep = points.size() > 1 ? (float) w / (points.size() - 1) : w;
            Path2D path = new Path2D.Float();
            int i = 0;
            for (float v : points) {
                float x = i * xStep;
                float ratio = Math.min(1f, v / maxValue);
                float y = h - 10 - ratio * usableHeight;
                if (i == 0) path.moveTo(x, y);
                else path.lineTo(x, y);
                i++;
            }

            g2.setStroke(new BasicStroke(2f));
            g2.setColor(lineColor);
            g2.draw(path);
            g2.dispose();
        }
    }

    private static class MetricsSampler {
        private final SystemMonitor monitor;
        private CpuInfo prevCpu;
        private long prevTotal;
        private long prevIdle;
        private NetworkInfo prevNetwork;
        private long prevNetBytes;
        private long prevTimestamp;

        MetricsSampler(SystemMonitor monitor) {
            this.monitor = monitor;
        }

        MetricPoint sample() {
            SystemSnapshot snap = monitor.getSystemSnapshot(false);
            CpuInfo cpu = snap.getCpu();
            MemoryInfo mem = snap.getMemory();
            NetworkInfo net = snap.getNetTotal();

            float cpuPercent = computeCpuPercent(cpu);
            float memPercent = computeMemPercent(mem);
            float netKbps = computeNetKbps(net, snap.getCollectedAtNanos());

            return new MetricPoint(
                    clamp(cpuPercent, 0f, 100f),
                    clamp(memPercent, 0f, 100f),
                    Math.max(0f, netKbps)
            );
        }

        private float computeCpuPercent(CpuInfo cpu) {
            long total = cpu.getTotalTicks();
            long idle = cpu.getIdle() + cpu.getIowait();
            float percent = 0f;
            if (prevCpu != null) {
                long deltaTotal = total - prevTotal;
                long deltaIdle = idle - prevIdle;
                if (deltaTotal > 0) {
                    percent = (deltaTotal - deltaIdle) * 100f / deltaTotal;
                }
            }
            prevCpu = cpu;
            prevTotal = total;
            prevIdle = idle;
            return percent;
        }

        private float computeMemPercent(MemoryInfo mem) {
            long total = mem.getTotalKB();
            long used = mem.getUsedKB();
            return (total > 0) ? used * 100f / total : 0f;
        }

        private float computeNetKbps(NetworkInfo net, long collectedAt) {
            if (net == null) return 0f;
            long bytes = net.getRxBytes() + net.getTxBytes();
            float kbps = 0f;
            if (prevNetwork != null && collectedAt > prevTimestamp && bytes >= prevNetBytes) {
                long deltaBytes = bytes - prevNetBytes;
                long deltaNanos = collectedAt - prevTimestamp;
                double seconds = deltaNanos / 1_000_000_000d;
                if (seconds > 0) {
                    kbps = (float) ((deltaBytes / 1024d) / seconds);
                }
            }
            prevNetwork = net;
            prevNetBytes = bytes;
            prevTimestamp = collectedAt;
            return kbps;
        }
    }

    // -------- Utils --------

    private static Comparator<ProcessRow> makeComparator(String col, boolean desc) {
        Comparator<ProcessRow> cmp;
        String key = col.toLowerCase(Locale.ROOT);
        switch (key) {
            case "pid" -> cmp = Comparator.comparingInt(ProcessRow::getPid);
            case "name" -> cmp = Comparator.comparing(ProcessRow::getName, String.CASE_INSENSITIVE_ORDER);
            case "user" -> cmp = Comparator.comparing(ProcessRow::getUser, String.CASE_INSENSITIVE_ORDER);
            case "priority" -> cmp = Comparator.comparing(ProcessRow::getPriority, String.CASE_INSENSITIVE_ORDER);
            case "state" -> cmp = Comparator.comparingInt(r -> r.getState());
            case "memory %" -> cmp = Comparator.comparingDouble(ProcessRow::getMemPercent);
            case "cpu %" -> cmp = Comparator.comparingDouble(ProcessRow::getCpuPercent);
            default -> cmp = Comparator.comparingDouble(ProcessRow::getCpuPercent);
        }
        return desc ? cmp.reversed() : cmp;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static List<ProcessRow> filterRows(List<ProcessRow> src, String q) {
        String s = q.toLowerCase(Locale.ROOT);
        // nếu là số -> lọc theo PID chính xác
        try {
            int pid = Integer.parseInt(s);
            List<ProcessRow> one = new ArrayList<>();
            for (ProcessRow r : src) if (r.getPid() == pid) one.add(r);
            if (!one.isEmpty()) return one;
        } catch (NumberFormatException ignored) {}
        // name contains hoặc user contains
        List<ProcessRow> out = new ArrayList<>();
        for (ProcessRow r : src) {
            if ((r.getName() != null && r.getName().toLowerCase(Locale.ROOT).contains(s))
                    || (r.getUser() != null && r.getUser().toLowerCase(Locale.ROOT).contains(s))) {
                out.add(r);
            }
        }
        return out;
    }

    private static <T> T safeGet(Supplier<T> s, T fallback) {
        try { return s.get(); } catch (Exception ex) { return fallback; }
    }
}
