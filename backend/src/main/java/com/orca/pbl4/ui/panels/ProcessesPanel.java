package com.orca.pbl4.ui.panels;

import com.orca.pbl4.core.model.ProcessInfo;
import com.orca.pbl4.core.system.SystemMonitor;
import com.orca.pbl4.core.system.SystemSnapshot;
import com.orca.pbl4.ui.components.ProcessTableModel;
import com.orca.pbl4.ui.components.SystemInfoPanel;
import com.orca.pbl4.ui.dialogs.ProcessDetailsDialog;
import com.orca.pbl4.ui.utils.DataRefresher;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Panel hiển thị danh sách processes
 * Tương tự tab "Processes" trong Task Manager
 */
public class ProcessesPanel extends JPanel {
    
    private final SystemMonitor systemMonitor;
    private SystemInfoPanel systemInfoPanel;
    private JTable processTable;
    private ProcessTableModel tableModel;
    private JTextField searchField;
    private JButton killButton;
    private JButton detailsButton;
    
    private DataRefresher refresher;
    private SystemSnapshot previousSnapshot;
    
    public ProcessesPanel(SystemMonitor systemMonitor) {
        this.systemMonitor = systemMonitor;
        initializeUI();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Top: System info (CPU, Memory summary)
        systemInfoPanel = new SystemInfoPanel();
        add(systemInfoPanel, BorderLayout.NORTH);
        
        // Center: Process table
        createProcessTable();
        JScrollPane scrollPane = new JScrollPane(processTable);
        add(scrollPane, BorderLayout.CENTER);
        
        // Bottom: Actions
        createActionsPanel();
    }
    
    private void createProcessTable() {
        tableModel = new ProcessTableModel();
        processTable = new JTable(tableModel);
        
        // Enable sorting
        TableRowSorter<ProcessTableModel> sorter = new TableRowSorter<>(tableModel);
        processTable.setRowSorter(sorter);
        
        // Table properties
        processTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        processTable.setAutoCreateRowSorter(true);
        processTable.getTableHeader().setReorderingAllowed(false);
        
        // Column widths
        processTable.getColumnModel().getColumn(0).setPreferredWidth(60);  // PID
        processTable.getColumnModel().getColumn(1).setPreferredWidth(200); // Name
        processTable.getColumnModel().getColumn(2).setPreferredWidth(100); // User
        processTable.getColumnModel().getColumn(3).setPreferredWidth(80);  // %CPU
        processTable.getColumnModel().getColumn(4).setPreferredWidth(80);  // %MEM
        processTable.getColumnModel().getColumn(5).setPreferredWidth(100); // RSS
        processTable.getColumnModel().getColumn(6).setPreferredWidth(60);  // State
        processTable.getColumnModel().getColumn(7).setPreferredWidth(60);  // Nice
        
        // Double click to show details
        processTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    showProcessDetails();
                }
            }
        });
    }
    
    private void createActionsPanel() {
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        // Search/Filter
        actionsPanel.add(new JLabel("Filter:"));
        searchField = new JTextField(20);
        searchField.addActionListener(e -> filterTable());
        actionsPanel.add(searchField);
        
        JButton filterButton = new JButton("Search");
        filterButton.addActionListener(e -> filterTable());
        actionsPanel.add(filterButton);
        
        actionsPanel.add(Box.createHorizontalStrut(20));
        
        // Kill process button
        killButton = new JButton("Kill Process");
        killButton.addActionListener(e -> killSelectedProcess());
        killButton.setEnabled(false);
        actionsPanel.add(killButton);
        
        // Details button
        detailsButton = new JButton("Details...");
        detailsButton.addActionListener(e -> showProcessDetails());
        detailsButton.setEnabled(false);
        actionsPanel.add(detailsButton);
        
        // Refresh button
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshNow());
        actionsPanel.add(refreshButton);
        
        // Enable/disable buttons based on selection
        processTable.getSelectionModel().addListSelectionListener(e -> {
            boolean hasSelection = processTable.getSelectedRow() != -1;
            killButton.setEnabled(hasSelection);
            detailsButton.setEnabled(hasSelection);
        });
        
        add(actionsPanel, BorderLayout.SOUTH);
    }
    
    private void filterTable() {
        String text = searchField.getText().trim();
        TableRowSorter<ProcessTableModel> sorter = 
            (TableRowSorter<ProcessTableModel>) processTable.getRowSorter();
        
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
        }
    }
    
    private void killSelectedProcess() {
        int selectedRow = processTable.getSelectedRow();
        if (selectedRow == -1) return;
        
        int modelRow = processTable.convertRowIndexToModel(selectedRow);
        int pid = (int) tableModel.getValueAt(modelRow, 0);
        String name = (String) tableModel.getValueAt(modelRow, 1);
        
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to kill process:\n" + name + " (PID: " + pid + ")?",
            "Confirm Kill Process",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Runtime.getRuntime().exec("kill -9 " + pid);
                JOptionPane.showMessageDialog(this, "Kill signal sent to process " + pid);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Failed to kill process: " + ex.getMessage(),
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void showProcessDetails() {
        int selectedRow = processTable.getSelectedRow();
        if (selectedRow == -1) return;
        
        int modelRow = processTable.convertRowIndexToModel(selectedRow);
        ProcessInfo process = tableModel.getProcessAt(modelRow);
        
        if (process != null) {
            ProcessDetailsDialog dialog = new ProcessDetailsDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                process,
                systemMonitor
            );
            dialog.setVisible(true);
        }
    }
    
    public void startRefresh() {
        refresher = new DataRefresher(1000, this::updateData);
        refresher.start();
    }
    
    public void stopRefresh() {
        if (refresher != null) {
            refresher.stop();
        }
    }
    
    public void refreshNow() {
        updateData();
    }
    
    private void updateData() {
        try {
            // Get current snapshot
            SystemSnapshot currentSnapshot = systemMonitor.getSystemSnapshot(false);
            
            // Calculate CPU and memory usage
            if (previousSnapshot != null) {
                calculateMetrics(previousSnapshot, currentSnapshot);
            }
            
            // Update UI on EDT
            SwingUtilities.invokeLater(() -> {
                // Update system info panel
                systemInfoPanel.update(
                    previousSnapshot != null ? previousSnapshot.getCpu() : null,
                    currentSnapshot.getCpu(),
                    currentSnapshot.getMemory()
                );
                
                // Update table
                tableModel.setProcesses(currentSnapshot.getProcesses());
            });
            
            // Save for next iteration
            previousSnapshot = currentSnapshot;
            
        } catch (Exception e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> 
                JOptionPane.showMessageDialog(this, 
                    "Error updating data: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE)
            );
        }
    }
    
    private void calculateMetrics(SystemSnapshot prev, SystemSnapshot current) {
        long deltaTotalCpu = current.getCpu().getTotalTicks() - prev.getCpu().getTotalTicks();
        long memTotalKB = Math.max(1, current.getMemory().getTotalKB());
        
        // Build map of previous ticks
        Map<Integer, Long> prevTicks = new HashMap<>();
        for (ProcessInfo p : prev.getProcesses()) {
            prevTicks.put(p.getPid(), p.getProcCpuTicks());
        }
        
        // Calculate %CPU and %MEM for each process
        for (ProcessInfo p : current.getProcesses()) {
            long currentTicks = p.getProcCpuTicks();
            long previousTicks = prevTicks.getOrDefault(p.getPid(), currentTicks);
            long delta = Math.max(0, currentTicks - previousTicks);
            
            // Calculate CPU %
            if (deltaTotalCpu > 0) {
                float cpuPct = (float) (100.0 * delta / deltaTotalCpu);
                p.setCpuPercent(cpuPct);
            } else {
                p.setCpuPercent(0.0f);
            }
            
            // Calculate Memory %
            long rssPages = p.getRssPages();
            long rssKB = rssPages * 4; // 4KB per page
            float memPct = (float) (100.0 * rssKB / memTotalKB);
            p.setMemoryPercent(memPct);
        }
    }
}
