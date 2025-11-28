package com.orca.pbl4.ui.process;

import com.orca.pbl4.core.model.ProcessRow;
import com.orca.pbl4.service.process.DefaultProcessManager;
import com.orca.pbl4.service.process.ProcessManager;
import com.orca.pbl4.service.process.ProcessSignalService;
import com.orca.pbl4.service.process.ProcessSortKey;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class ProcessTablePanel extends JPanel {

    private final ProcessManager manager;
    private final ProcessTableModel tableModel;
    private final JTable table;
    private Integer rememberedPid = null; // Lưu PID đã chọn để restore sau refresh
    private boolean isRestoringSelection = false; // Flag để tránh ListSelectionListener can thiệp khi restore

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

        // Listener để cập nhật rememberedPid khi selection thay đổi
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !isRestoringSelection) {
                ProcessRow selectedRow = getSelectedRow();
                if (selectedRow != null) {
                    rememberedPid = selectedRow.getPid();
                }
            }
        });

        // Xử lý phím lên/xuống
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int selectedRow = table.getSelectedRow();
                int rowCount = table.getRowCount();

                if (rowCount == 0) return; // Không có dòng nào

                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    if (selectedRow < 0) {
                        // Chưa có selection: chọn dòng đầu tiên
                        table.setRowSelectionInterval(0, 0);
                        table.requestFocusInWindow();
                        e.consume();
                    }
                    // Nếu đã có selection, JTable sẽ tự xử lý di chuyển lên
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (selectedRow < 0) {
                        // Chưa có selection: chọn dòng đầu tiên
                        table.setRowSelectionInterval(0, 0);
                        table.requestFocusInWindow();
                        e.consume();
                    }
                    // Nếu đã có selection, JTable sẽ tự xử lý di chuyển xuống
                }
            }
        });

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

    private void restoreSelection() {
        if (rememberedPid == null) return;

        isRestoringSelection = true;
        try {
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
        } finally {
            isRestoringSelection = false;
        }
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
            case KILL -> handleKill(row);
            case STOP -> handleStop(row);
            case CONTINUE -> handleContinue(row);
            case RENICE -> handleRenice(row);
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

    private void handleKill(ProcessRow row) {
        if (row == null) return;
        if (!confirmAction("Kill", "Are you sure you want to kill this process?")) {
            return;
        }
        
        boolean requireRoot = shouldRequireRoot(row);
        sendSignalAsync(row.getPid(), "TERM", requireRoot, "Kill", () -> {
            manager.refreshSnapshot();
        });
    }

    private void handleStop(ProcessRow row) {
        if (row == null) return;
        if (!confirmAction("Stop", "Are you sure you want to stop this process?")) {
            return;
        }
        
        boolean requireRoot = shouldRequireRoot(row);
        sendSignalAsync(row.getPid(), "STOP", requireRoot, "Stop", () -> {
            manager.refreshSnapshot();
        });
    }

    private void handleContinue(ProcessRow row) {
        if (row == null) return;
        boolean requireRoot = shouldRequireRoot(row);
        sendSignalAsync(row.getPid(), "CONT", requireRoot, "Continue", () -> {
            manager.refreshSnapshot();
        });
    }

    private void handleRenice(ProcessRow row) {
        if (row == null) return;
        
        // Lấy thông tin process để biết nice value hiện tại
        com.orca.pbl4.core.model.ProcessInfo info = manager.getProcessDetail(row.getPid());
        int currentNice = info != null ? info.getNice() : 0;
        
        // Tạo combo box với các mức priority giống ProcessDetailDialog
        String[] priorities = {
            "Very High",
            "High",
            "Above Normal",
            "Normal",
            "Below Normal",
            "Low",
            "Very Low"
        };
        
        String currentPriority = resolvePriorityFromNice(currentNice);
        JComboBox<String> comboBox = new JComboBox<>(priorities);
        comboBox.setSelectedItem(currentPriority);
        comboBox.setFont(new Font("SansSerif", Font.PLAIN, 12));
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.add(new JLabel("Select priority level:"), BorderLayout.NORTH);
        panel.add(comboBox, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        int result = JOptionPane.showConfirmDialog(
            this,
            panel,
            "Change Priority",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        
        if (result == JOptionPane.OK_OPTION) {
            String selectedPriority = (String) comboBox.getSelectedItem();
            if (selectedPriority != null) {
                int niceValue = mapPriorityToNice(selectedPriority);
                boolean requireRoot = shouldRequireRoot(row);
                
                ProcessSignalService signalService = getSignalService();
                if (signalService != null) {
                    signalService.reniceAsync(row.getPid(), niceValue, requireRoot, r -> {
                        switch (r) {
                            case SUCCESS -> {
                                JOptionPane.showMessageDialog(this,
                                    "Priority changed to " + selectedPriority + " successfully.",
                                    "Success", JOptionPane.INFORMATION_MESSAGE);
                                manager.refreshSnapshot();
                            }
                            case PERMISSION_DENIED -> {
                                showError("Change Priority",
                                    "Permission denied. You may need root privileges.");
                            }
                            case USER_CANCELLED -> {
                                showError("Change Priority",
                                    "Change priority cancelled or wrong password.");
                            }
                            case PROCESS_NOT_FOUND -> {
                                showError("Change Priority",
                                    "Process not found. It may have already terminated.");
                            }
                            case UNKNOWN_ERROR -> {
                                showError("Change Priority",
                                    "Unknown error while changing priority.");
                            }
                        }
                    });
                } else {
                    // Fallback: dùng method cũ
                    if (manager.reniceProcess(row.getPid(), niceValue)) {
                        JOptionPane.showMessageDialog(this,
                                "Priority changed to " + selectedPriority + " successfully.",
                                "Success", JOptionPane.INFORMATION_MESSAGE);
                        manager.refreshSnapshot();
                    } else {
                        showError("Change Priority",
                            "Failed to change priority. You may not have permission.");
                    }
                }
            }
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

    private void sendSignalAsync(int pid, String signal, boolean requireRoot, String actionName, Runnable onSuccess) {
        ProcessSignalService signalService = getSignalService();
        if (signalService == null) {
            // Fallback: dùng method cũ
            boolean success = switch (signal) {
                case "TERM", "KILL" -> manager.killProcess(pid);
                case "STOP" -> manager.stopProcess(pid);
                case "CONT" -> manager.continueProcess(pid);
                default -> false;
            };
            if (success) {
                if (onSuccess != null) onSuccess.run();
            } else {
                showError(actionName, "Failed to " + actionName.toLowerCase() + " process. Permission denied or process not found.");
            }
            return;
        }

        signalService.sendSignal(pid, signal, requireRoot, result -> {
            switch (result) {
                case SUCCESS -> {
                    if (onSuccess != null) onSuccess.run();
                }
                case PERMISSION_DENIED -> {
                    showError(actionName, "Permission denied. You may need root privileges to " + actionName.toLowerCase() + " this process.");
                }
                case PROCESS_NOT_FOUND -> {
                    showError(actionName, "Process not found. It may have already terminated.");
                }
                case USER_CANCELLED -> {
                    showError(actionName, actionName + " failed: authentication cancelled or wrong password.");
                }
                case UNKNOWN_ERROR -> {
                    showError(actionName, "Unknown error while sending signal.");
                }
            }
        });
    }

    private ProcessSignalService getSignalService() {
        if (manager instanceof DefaultProcessManager) {
            return ((DefaultProcessManager) manager).getSignalService();
        }
        return null;
    }

    private boolean shouldRequireRoot(ProcessRow row) {
        if (row == null) return false;
        String user = row.getUser();
        if (user == null) return false;
        String currentUser = System.getProperty("user.name");
        // Nếu process thuộc user khác hoặc root thì có thể cần quyền root
        return !user.equals(currentUser) || "root".equals(user);
    }

    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title + " Error", JOptionPane.ERROR_MESSAGE);
    }

    private boolean confirmAction(String action, String message) {
        return JOptionPane.showConfirmDialog(this,
                message,
                action + " Process",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private int mapPriorityToNice(String priority) {
        return switch (priority) {
            case "Very High" -> -15;
            case "High" -> -10;
            case "Above Normal" -> -5;
            case "Normal" -> 0;
            case "Below Normal" -> 5;
            case "Low" -> 10;
            case "Very Low" -> 15;
            default -> 0;
        };
    }

    private String resolvePriorityFromNice(int nice) {
        if (nice <= -15) return "Very High";
        if (nice <= -10) return "High";
        if (nice < 0) return "Above Normal";
        if (nice == 0) return "Normal";
        if (nice <= 5) return "Below Normal";
        if (nice <= 10) return "Low";
        return "Very Low";
    }
}
