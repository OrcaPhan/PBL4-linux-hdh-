package com.orca.pbl4;

import com.orca.pbl4.core.model.ProcessRow;
import com.orca.pbl4.core.system.SystemMonitor;
import com.orca.pbl4.service.ProcessManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

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

        f.setLayout(new BorderLayout());
        f.add(top, BorderLayout.NORTH);
        f.add(new JScrollPane(table), BorderLayout.CENTER);

        // ---- Refresh timer
        Timer[] timerHolder = new Timer[1];

        Runnable doRefresh = () -> {
            // LẤY DỮ LIỆU MỚI MỖI LẦN (đúng yêu cầu: search/sort vẫn cập nhật)
            List<ProcessRow> fresh = safeGet(() -> pm.listProcessRows(), new ArrayList<>());

            // Filter theo search
            String q = txtSearch.getText();
            List<ProcessRow> filtered = (q == null || q.isBlank())
                    ? fresh
                    : filterRows(fresh, q.trim());

            // Sort theo combo
            String col = Objects.toString(cbSort.getSelectedItem(), "CPU %");
            boolean desc = chkDesc.isSelected();
            filtered.sort(makeComparator(col, desc));

            model.setRows(filtered);
        };

        // timer run
        timerHolder[0] = new Timer((Integer) spInterval.getValue(), e -> doRefresh.run());
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
        });

        // Search live
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            private void refresh() { doRefresh.run(); }
            public void insertUpdate(DocumentEvent e) { refresh(); }
            public void removeUpdate(DocumentEvent e) { refresh(); }
            public void changedUpdate(DocumentEvent e) { refresh(); }
        });

        // Sort change
        cbSort.addActionListener(e -> doRefresh.run());
        chkDesc.addActionListener(e -> doRefresh.run());

        // first load
        doRefresh.run();

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
