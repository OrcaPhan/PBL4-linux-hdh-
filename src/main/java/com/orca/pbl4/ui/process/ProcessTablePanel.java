package com.orca.pbl4.ui.process;

import com.orca.pbl4.core.model.ProcessRow;
import com.orca.pbl4.service.process.ProcessManager;
import com.orca.pbl4.service.process.ProcessSortKey;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Panel gồm toolbar + bảng tiến trình.
 */
public class ProcessTablePanel extends JPanel {

    private final ProcessManager manager;
    private final ProcessTableModel tableModel;
    private final JTable table;
    private Integer rememberedPid = null; // Lưu PID đã chọn để restore sau refresh

    public ProcessTablePanel(ProcessManager manager, Runnable showMetricsAction) {
        super(new BorderLayout());
        this.manager = manager;
        this.tableModel = new ProcessTableModel(manager);

        ProcessToolbarPanel toolbar = new ProcessToolbarPanel();
        toolbar.setSearchConsumer(tableModel::setSearchText);
        toolbar.setRefreshAction(manager::refreshSnapshot);
        Runnable metricsAction = showMetricsAction != null ? showMetricsAction : () -> {};
        toolbar.setShowMetricsAction(metricsAction);
        add(toolbar, BorderLayout.NORTH);
        
        // Đăng ký listener để cập nhật status bar và restore selection
        manager.addUpdateListener((rows, snapshot) -> {
            SwingUtilities.invokeLater(() -> {
                updateStatusBar(rows, toolbar);
                restoreSelection();
            });
        });

        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(true);
        
        // Áp dụng style từ ProcessTableStyleUtil
        ProcessTableStyleUtil.applyTableStyle(table);
        
        // Tạo context menu cho right-click
        ProcessContextMenu contextMenu = new ProcessContextMenu(this::performAction);
        
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                
                if (SwingUtilities.isRightMouseButton(e)) {
                    // Right-click: chọn hàng nếu cần, sau đó hiển thị context menu
                    if (row >= 0) {
                        // Chọn hàng nếu chưa được chọn
                        if (!table.isRowSelected(row)) {
                            table.setRowSelectionInterval(row, row);
                        }
                        ProcessRow selectedRow = getSelectedRow();
                        if (selectedRow != null) {
                            rememberedPid = selectedRow.getPid();
                        }
                        table.requestFocusInWindow();
                    }
                    // Context menu sẽ được hiển thị trong mouseReleased
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    // Left-click: chọn hàng hoặc bỏ chọn
                    if (row >= 0) {
                        // Click vào hàng: chọn hàng
                        table.setRowSelectionInterval(row, row);
                        ProcessRow selectedRow = getSelectedRow();
                        if (selectedRow != null) {
                            rememberedPid = selectedRow.getPid();
                        }
                        table.requestFocusInWindow();
                    } else {
                        // Click vào vùng trống: bỏ chọn
                        table.clearSelection();
                        rememberedPid = null;
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    // Hiển thị context menu tại vị trí chuột
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        // Đảm bảo hàng đã được chọn
                        if (!table.isRowSelected(row)) {
                            table.setRowSelectionInterval(row, row);
                        }
                        // Hiển thị context menu
                        contextMenu.show(table, e.getX(), e.getY());
                    }
                    // Nếu click vào vùng trống, không làm gì
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                // Double-click chuột trái: mở Properties dialog
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        // Đảm bảo hàng đã được chọn
                        table.setRowSelectionInterval(row, row);
                        ProcessRow selectedRow = getSelectedRow();
                        if (selectedRow != null) {
                            rememberedPid = selectedRow.getPid();
                            // Mở dialog chi tiết tiến trình
                            performAction(ProcessAction.DETAIL);
                        }
                    }
                }
            }
        };
        table.addMouseListener(mouseAdapter);
        
        // Listener cho header: click để sort hoặc bỏ chọn
        JTableHeader header = table.getTableHeader();
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    int viewCol = table.columnAtPoint(e.getPoint());
                    if (viewCol >= 0) {
                        // Click vào cột: sort
                        ProcessSortKey key = columnToSortKey(viewCol);
                        if (key != null) {
                            tableModel.toggleSort(key);
                        }
                    } else {
                        // Click vào vùng trống của header: bỏ chọn hàng
                        table.clearSelection();
                        rememberedPid = null;
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        // Click ra ngoài table (trên scroll pane) để clear selection
        scrollPane.getViewport().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point p = SwingUtilities.convertPoint(scrollPane.getViewport(), e.getPoint(), table);
                int row = table.rowAtPoint(p);
                if (row < 0) {
                    table.clearSelection();
                    rememberedPid = null;
                }
            }
        });
        
        add(scrollPane, BorderLayout.CENTER);
    }
    
    /**
     * Khôi phục selection sau khi refresh dữ liệu.
     * KHÔNG tự động scroll để tránh nhảy lên dòng đang chọn.
     */
    private void restoreSelection() {
        if (rememberedPid == null) return;
        
        // Tìm row có PID tương ứng
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            ProcessRow row = tableModel.getRow(i);
            if (row != null && row.getPid() == rememberedPid) {
                int viewRow = table.convertRowIndexToView(i);
                if (viewRow >= 0) {
                    // Chỉ set selection, KHÔNG scroll để giữ viewport hiện tại
                    table.setRowSelectionInterval(viewRow, viewRow);
                    return;
                }
            }
        }
        
        // Nếu không tìm thấy (process đã biến mất), clear selection
        table.clearSelection();
        rememberedPid = null;
    }

    private ProcessRow getSelectedRow() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = table.convertRowIndexToModel(viewRow);
        return tableModel.getRow(modelRow);
    }

    private void performAction(ProcessAction action) {
        ProcessRow row = getSelectedRow();
        if (row == null && action != ProcessAction.DETAIL) return;

        switch (action) {
            case KILL -> {
                if (requirePassword(action, row) && manager.killProcess(row.getPid())) {
                    manager.refreshSnapshot();
                }
            }
            case STOP -> {
                if (requirePassword(action, row) && manager.stopProcess(row.getPid())) {
                    manager.refreshSnapshot();
                }
            }
            case CONTINUE -> {
                if (requirePassword(action, row) && manager.continueProcess(row.getPid())) {
                    manager.refreshSnapshot();
                }
            }
            case RENICE -> {
                if (requirePassword(action, row)) {
                    handleRenice(row);
                }
            }
            case DETAIL -> {
                ProcessRow target = row != null ? row : getSelectedRow();
                if (target != null) {
                    // Truyền supplier để dialog có thể tự refresh theo tiến trình đang chọn
                    java.util.function.Supplier<Integer> pidSupplier = () -> {
                        ProcessRow selected = getSelectedRow();
                        return selected != null ? selected.getPid() : null;
                    };
                    new ProcessDetailDialog(manager, target.getPid(), pidSupplier).setVisible(true);
                }
            }
        }
    }

    private void handleRenice(ProcessRow row) {
        if (row == null) return;
        String input = JOptionPane.showInputDialog(this, "New nice value (-20..19):", "Renice PID " + row.getPid(), JOptionPane.PLAIN_MESSAGE);
        if (input == null) return;
        try {
            int nice = Integer.parseInt(input.trim());
            if (manager.reniceProcess(row.getPid(), nice)) {
                manager.refreshSnapshot();
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private ProcessSortKey columnToSortKey(int viewCol) {
        return switch (viewCol) {
            case 0 -> ProcessSortKey.PID;
            case 1 -> ProcessSortKey.USER;
            case 2 -> ProcessSortKey.NAME;
            case 3 -> ProcessSortKey.CPU_PERCENT;
            case 4 -> ProcessSortKey.MEMORY_PERCENT;
            case 5 -> ProcessSortKey.RSS;
            case 6 -> ProcessSortKey.STATE;
            case 7 -> ProcessSortKey.NICE; // Vẫn dùng NICE vì ProcessSortKey chưa có PRIORITY
            default -> null;
        };
    }


    /**
     * Cập nhật status bar với thông tin từ danh sách tiến trình.
     */
    private void updateStatusBar(List<ProcessRow> rows, ProcessToolbarPanel toolbar) {
        if (rows == null || rows.isEmpty()) {
            toolbar.updateStatus(0, null, null);
            return;
        }

        int processCount = rows.size();
        Integer maxCpuPid = null;
        Integer maxMemPid = null;
        float maxCpu = -1f;
        float maxMem = -1f;

        for (ProcessRow row : rows) {
            if (row.getCpuPercent() > maxCpu) {
                maxCpu = row.getCpuPercent();
                maxCpuPid = row.getPid();
            }
            if (row.getMemPercent() > maxMem) {
                maxMem = row.getMemPercent();
                maxMemPid = row.getPid();
            }
        }

        toolbar.updateStatus(processCount, maxCpuPid, maxMemPid);
    }

    private boolean requirePassword(ProcessAction action, ProcessRow row) {
        if (row == null) return false;
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.add(new JLabel("Enter password to " + action.name().toLowerCase() + " PID " + row.getPid()), BorderLayout.NORTH);
        JPasswordField passwordField = new JPasswordField();
        panel.add(passwordField, BorderLayout.CENTER);
        int result = JOptionPane.showConfirmDialog(this, panel, "Authentication Required", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        return result == JOptionPane.OK_OPTION && passwordField.getPassword().length > 0;
    }
}

