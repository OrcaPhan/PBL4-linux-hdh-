package com.orca.pbl4.ui.dialogs;

import com.orca.pbl4.core.model.ProcessInfo;
import com.orca.pbl4.core.model.ThreadInfo;
import com.orca.pbl4.core.model.HandleInfo;
import com.orca.pbl4.core.system.SystemMonitor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Dialog hiển thị thông tin chi tiết của một process
 * Bao gồm: threads, handles, I/O stats
 */
public class ProcessDetailsDialog extends JDialog {
    
    private ProcessInfo process;
    private SystemMonitor systemMonitor;
    
    public ProcessDetailsDialog(Frame parent, ProcessInfo process, SystemMonitor systemMonitor) {
        super(parent, "Process Details - " + process.getName(), true);
        this.process = process;
        this.systemMonitor = systemMonitor;
        
        initializeUI();
        setSize(700, 500);
        setLocationRelativeTo(parent);
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        
        // Top: Basic info
        add(createInfoPanel(), BorderLayout.NORTH);
        
        // Center: Tabbed pane with threads and handles
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Threads", createThreadsPanel());
        tabbedPane.addTab("Handles", createHandlesPanel());
        tabbedPane.addTab("I/O", createIOPanel());
        add(tabbedPane, BorderLayout.CENTER);
        
        // Bottom: Close button
        JPanel buttonPanel = new JPanel();
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Process Information"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        
        addInfoRow(panel, "PID:", String.valueOf(process.getPid()));
        addInfoRow(panel, "Name:", process.getName() != null ? process.getName() : "N/A");
        addInfoRow(panel, "User:", process.getUser() != null ? process.getUser() : "N/A");
        addInfoRow(panel, "State:", process.getState() != null ? process.getState() : "N/A");
        addInfoRow(panel, "Priority:", String.valueOf(process.getPriority()));
        addInfoRow(panel, "Nice:", String.valueOf(process.getNice()));
        addInfoRow(panel, "CPU %:", process.getCpuPercent() != null ? 
            String.format("%.2f%%", process.getCpuPercent()) : "N/A");
        addInfoRow(panel, "Memory %:", process.getMemoryPercent() != null ? 
            String.format("%.2f%%", process.getMemoryPercent()) : "N/A");
        
        if (process.getCmdline() != null && !process.getCmdline().isEmpty()) {
            JPanel cmdPanel = new JPanel(new BorderLayout());
            cmdPanel.add(new JLabel("Command:"), BorderLayout.WEST);
            JTextArea cmdArea = new JTextArea(process.getCmdline());
            cmdArea.setEditable(false);
            cmdArea.setLineWrap(true);
            cmdArea.setRows(2);
            cmdPanel.add(new JScrollPane(cmdArea), BorderLayout.CENTER);
            panel.add(cmdPanel);
        }
        
        return panel;
    }
    
    private void addInfoRow(JPanel panel, String label, String value) {
        panel.add(new JLabel(label));
        panel.add(new JLabel(value));
    }
    
    private JPanel createThreadsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        String[] columns = {"TID", "Name", "State", "CPU Time"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        
        // Add thread data if available
        if (process.getThreads() != null) {
            for (ThreadInfo thread : process.getThreads()) {
                model.addRow(new Object[]{
                    thread.getTid(),
                    thread.getName() != null ? thread.getName() : "N/A",
                    thread.getState() != null ? thread.getState() : "N/A",
                    thread.getCpuTimeTicks()
                });
            }
        }
        
        JTable table = new JTable(model);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        
        JLabel countLabel = new JLabel("Total threads: " + 
            (process.getThreads() != null ? process.getThreads().size() : 0));
        panel.add(countLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createHandlesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        String[] columns = {"FD", "Type", "Target"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        
        // Add handle data if available
        if (process.getHandles() != null) {
            for (HandleInfo handle : process.getHandles()) {
                model.addRow(new Object[]{
                    handle.getFd(),
                    handle.getType() != null ? handle.getType() : "N/A",
                    handle.getTarget() != null ? handle.getTarget() : "N/A"
                });
            }
        }
        
        JTable table = new JTable(model);
        table.getColumnModel().getColumn(2).setPreferredWidth(300);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        
        JLabel countLabel = new JLabel("Total handles/FDs: " + 
            (process.getHandles() != null ? process.getHandles().size() : 0));
        panel.add(countLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createIOPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        long readBytes = process.getIoReadBytes();
        long writeBytes = process.getIoWriteBytes();
        
        addInfoRow(panel, "Read Bytes:", formatBytes(readBytes));
        addInfoRow(panel, "Write Bytes:", formatBytes(writeBytes));
        
        return panel;
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format("%.2f KB", kb);
        
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format("%.2f MB", mb);
        
        double gb = mb / 1024.0;
        return String.format("%.2f GB", gb);
    }
}
