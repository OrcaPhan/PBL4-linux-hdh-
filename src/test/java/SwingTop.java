

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

/**
 * SwingTop - UI Swing tối giản để test ProcessManager (mượt)
 * - Refresh mỗi giây
 * - Search (PID/name/user)
 * - Sort cột chọn
 * - Giới hạn số dòng hiển thị
 */
public class SwingTop {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SwingTop::createAndShow);
    }

    private static void createAndShow() {
        JFrame f = new JFrame("PBL4 - Process Monitor (Swing)");
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        f.setSize(1120, 680);
        f.setLocationRelativeTo(null);

        // Core services
        SystemMonitor monitor = new SystemMonitor();
        // period 1000ms, top-style CPU=true, EMA alpha=0.30f (trong constructor mặc định mình đã để vậy)
        ProcessManager pm = new ProcessManager(monitor, 1000);

        // Table
        ProcessTableModel model = new ProcessTableModel();
        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(false); // ta tự sort để cùng logic với terminal

        // Controls
        JTextField txtSearch = new JTextField();
        txtSearch.setToolTipText("Tìm theo PID / tên / user");

        String[] sortCols = {"CPU %", "Memory %", "PID", "Name", "User", "Priority", "State"};
        JComboBox<String> cbSort = new JComboBox<>(sortCols);
        JCheckBox chkDesc = new JCheckBox("Desc", true);

        JSpinner spLimit = new JSpinner(new SpinnerNumberModel(50, 10, 500, 10));
        JSpinner spInterval = new JSpinner(new SpinnerNumberModel(1000, 250, 5000, 250));
        JToggleButton btnRun = new JToggleButton("⏵ Auto Refresh", true);

        JPanel top = new JPanel(new BorderLayout(10, 10));
        JPanel left = new JPanel(new BorderLayout(6, 6));
        left.add(new JLabel("Search: "), BorderLayout.WEST);
        left.add(txtSearch, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        right.add(new JLabel("Sort:"));
        right.add(cbSort);
        right.add(chkDesc);
        right.add(new JLabel("Limit:"));
        right.add(spLimit);
        right.add(new JLabel("Interval(ms):"));
        right.add(spInterval);
        right.add(btnRun);

        top.add(left, BorderLayout.CENTER);
        top.add(right, BorderLayout.EAST);

        f.setLayout(new BorderLayout());
        f.add(top, BorderLayout.NORTH);
        f.add(new JScrollPane(table), BorderLayout.CENTER);

        // Timer refresh
        Timer[] holder = new Timer[1];

        Runnable doRefresh = () -> {
            // 1) Lấy dữ liệu mới mỗi lần
            List<ProcessRow> rows = safe(() -> pm.listProcessRows(), new ArrayList<>());

//            // 2) Search
//            String q = txtSearch.getText();
//            List<ProcessRow> filtered = pm.search(q);
//
//            // 3) Sort
//            String col = Objects.toString(cbSort.getSelectedItem(), "CPU %");
//            boolean desc = chkDesc.isSelected();
//            filtered =pm.sort(col, desc);
//
////             4) Limit
//            int limit = (Integer) spLimit.getValue();
//            if (filtered.size() > limit) {
//                filtered = new ArrayList<>(filtered.subList(0, limit));
//            }

            // 5) Set model
            model.setRows(rows);
        };

        holder[0] = new Timer((Integer) spInterval.getValue(), e -> doRefresh.run());
        holder[0].start();

        // Events
        btnRun.addActionListener(e -> {
            if (btnRun.isSelected()) {
                btnRun.setText("⏵ Auto Refresh");
                holder[0].start();
            } else {
                btnRun.setText("⏸ Paused");
                holder[0].stop();
            }
        });

        spInterval.addChangeListener(e -> holder[0].setDelay((Integer) spInterval.getValue()));

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            private void go() { doRefresh.run(); }
            public void insertUpdate(DocumentEvent e) { go(); }
            public void removeUpdate(DocumentEvent e) { go(); }
            public void changedUpdate(DocumentEvent e) { go(); }
        });

        cbSort.addActionListener(e -> doRefresh.run());
        chkDesc.addActionListener(e -> doRefresh.run());
        spLimit.addChangeListener(e -> doRefresh.run());

        // First load
        doRefresh.run();

        f.setVisible(true);
    }

    /* ================= Model ================= */

    static class ProcessTableModel extends AbstractTableModel {
        private final String[] cols = {
                "Process Name", "User", "% CPU", "PID", "Memory", "Disk read", "Disk write", "Priority", "State"
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
            switch (c) {
                case 0: return p.getName();
                case 1: return p.getUser();
                case 2: return df2.format(p.getCpuPercent());
                case 3: return p.getPid();
                case 4: return p.getMemoryStr();
                case 5: return p.getDiskRead();
                case 6: return p.getDiskWrite();
                case 7: return p.getPriority();
                case 8: return String.valueOf(p.getState());
                default: return "";
            }
        }

        @Override
        public Class<?> getColumnClass(int c) {
            if (c == 3) return Integer.class;  // PID
            return String.class;
        }
    }

    /* ================= Utils ================= */

    private static <T> T safe(SupplierEx<T> s, T fallback) {
        try { return s.get(); } catch (Exception e) { return fallback; }
    }

    private interface SupplierEx<T> { T get() throws Exception; }
}
