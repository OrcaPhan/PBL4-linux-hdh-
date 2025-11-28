package com.orca.pbl4.ui.process;

import com.orca.pbl4.core.model.ProcessRow;
import com.orca.pbl4.core.system.SystemSnapshot;
import com.orca.pbl4.service.process.ProcessManager;
import com.orca.pbl4.service.process.ProcessSortKey;
import com.orca.pbl4.service.process.ProcessUpdateListener;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * TableModel hiển thị tiến trình. Lắng nghe sự kiện từ ProcessManager.
 */
public class ProcessTableModel extends AbstractTableModel implements ProcessUpdateListener {

    private static final String[] COLUMNS = {
            "PID", "User", "Name", "CPU %", "MEM %", "RSS", "State", "Priority"
    };

    private final ProcessManager manager;
    private List<ProcessRow> latestRows = new ArrayList<>();
    private List<ProcessRow> visibleRows = new ArrayList<>();
    private String searchText = "";
    private ProcessSortKey sortKey = ProcessSortKey.CPU_PERCENT;
    private boolean descending = true;

    public ProcessTableModel(ProcessManager manager) {
        this.manager = manager;
        this.manager.addUpdateListener(this);
    }

    public ProcessRow getRow(int modelIndex) {
        return visibleRows.get(modelIndex);
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText != null ? searchText.trim() : "";
        applyTransform();
    }

    public void toggleSort(ProcessSortKey key) {
        if (key == null) return;
        if (sortKey == key) {
            descending = !descending;
        } else {
            sortKey = key;
            descending = true;
        }
        applyTransform();
    }

    public ProcessSortKey getSortKey() {
        return sortKey;
    }

    public boolean isDescending() {
        return descending;
    }

    @Override
    public int getRowCount() {
        return visibleRows.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ProcessRow row = visibleRows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> row.getPid();
            case 1 -> row.getUser();
            case 2 -> row.getName();
            case 3 -> row.getCpuPercent();
            case 4 -> row.getMemPercent();
            case 5 -> row.getMemoryStr();
            case 6 -> String.valueOf(row.getState());
            case 7 -> row.getPriority();
            default -> "";
        };
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case 0 -> Integer.class;
            case 3, 4 -> Float.class;
            default -> String.class;
        };
    }

    @Override
    public void onProcessSnapshotUpdated(List<ProcessRow> rows, SystemSnapshot snapshot) {
        this.latestRows = new ArrayList<>(rows);
        applyTransform();
    }

    private void applyTransform() {
        List<ProcessRow> filtered = new ArrayList<>();
        String q = searchText == null ? "" : searchText.toLowerCase(Locale.ROOT);

        for (ProcessRow row : latestRows) {
            if (!matchesSearch(row, q)) continue;
            filtered.add(row);
        }

        filtered.sort(makeComparator(sortKey, descending));
        this.visibleRows = filtered;
        fireTableDataChanged();
    }

    private boolean matchesSearch(ProcessRow row, String q) {
        if (q == null || q.isBlank()) return true;
        try {
            int pid = Integer.parseInt(q);
            return row.getPid() == pid;
        } catch (NumberFormatException ignored) {}

        return (row.getName() != null && row.getName().toLowerCase(Locale.ROOT).contains(q))
                || (row.getUser() != null && row.getUser().toLowerCase(Locale.ROOT).contains(q));
    }

    private java.util.Comparator<ProcessRow> makeComparator(ProcessSortKey key, boolean desc) {
        ProcessSortKey k = key != null ? key : ProcessSortKey.CPU_PERCENT;
        java.util.Comparator<ProcessRow> cmp = switch (k) {
            case PID -> java.util.Comparator.comparingInt(ProcessRow::getPid);
            case USER -> java.util.Comparator.comparing(ProcessRow::getUser, String.CASE_INSENSITIVE_ORDER);
            case NAME -> java.util.Comparator.comparing(ProcessRow::getName, String.CASE_INSENSITIVE_ORDER);
            case CPU_PERCENT -> java.util.Comparator.comparingDouble(ProcessRow::getCpuPercent);
            case MEMORY_PERCENT -> java.util.Comparator.comparingDouble(ProcessRow::getMemPercent);
            case RSS -> java.util.Comparator.comparing(ProcessRow::getMemoryStr, String.CASE_INSENSITIVE_ORDER);
            case STATE -> java.util.Comparator.comparingInt(r -> r.getState());
            case NICE -> java.util.Comparator.comparing(ProcessRow::getPriority, String.CASE_INSENSITIVE_ORDER);
        };
        return desc ? cmp.reversed() : cmp;
    }
}