package com.orca.pbl4.ui.components;

import com.orca.pbl4.core.model.ProcessInfo;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * TableModel cho JTable hiển thị processes
 */
public class ProcessTableModel extends AbstractTableModel {
    
    private final String[] columnNames = {
        "PID", "Name", "User", "%CPU", "%MEM", "RSS", "State", "Nice"
    };
    
    private List<ProcessInfo> processes = new ArrayList<>();
    
    @Override
    public int getRowCount() {
        return processes.size();
    }
    
    @Override
    public int getColumnCount() {
        return columnNames.length;
    }
    
    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }
    
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0: // PID
            case 7: // Nice
                return Integer.class;
            case 3: // %CPU
            case 4: // %MEM
                return Float.class;
            default:
                return String.class;
        }
    }
    
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ProcessInfo process = processes.get(rowIndex);
        
        switch (columnIndex) {
            case 0: return process.getPid();
            case 1: return process.getName() != null ? process.getName() : "";
            case 2: return process.getUser() != null ? process.getUser() : "";
            case 3: return process.getCpuPercent() != null ? process.getCpuPercent() : 0.0f;
            case 4: return process.getMemoryPercent() != null ? process.getMemoryPercent() : 0.0f;
            case 5: return formatMemory(process.getRssPages());
            case 6: return process.getState() != null ? process.getState() : "";
            case 7: return process.getNice();
            default: return "";
        }
    }
    
    private String formatMemory(long rssPages) {
        long kb = rssPages * 4; // 4KB per page
        if (kb < 1024) return kb + " KB";
        
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format("%.1f MB", mb);
        
        double gb = mb / 1024.0;
        return String.format("%.2f GB", gb);
    }
    
    public void setProcesses(List<ProcessInfo> processes) {
        this.processes = new ArrayList<>(processes);
        fireTableDataChanged();
    }
    
    public ProcessInfo getProcessAt(int row) {
        if (row >= 0 && row < processes.size()) {
            return processes.get(row);
        }
        return null;
    }
}
