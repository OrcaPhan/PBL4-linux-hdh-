package com.orca.pbl4.ui.process;

import com.orca.pbl4.core.model.ProcessRow;
import com.orca.pbl4.service.process.ProcessManager;
import com.orca.pbl4.service.process.ProcessSortKey;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Panel gồm toolbar + bảng tiến trình.
 */
public class ProcessTablePanel extends JPanel {

    private final ProcessManager manager;
    private final ProcessTableModel tableModel;
    private final JTable table;

    public ProcessTablePanel(ProcessManager manager, Runnable showMetricsAction) {
        super(new BorderLayout());
        this.manager = manager;
        this.tableModel = new ProcessTableModel(manager);

        ProcessToolbarPanel toolbar = new ProcessToolbarPanel();
        toolbar.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        toolbar.setSearchConsumer(tableModel::setSearchText);
        toolbar.setRefreshAction(manager::refreshSnapshot);
        toolbar.setKillAction(() -> performAction(ProcessAction.KILL));
        toolbar.setStopAction(() -> performAction(ProcessAction.STOP));
        toolbar.setContinueAction(() -> performAction(ProcessAction.CONTINUE));
        Runnable metricsAction = showMetricsAction != null ? showMetricsAction : () -> {};
        toolbar.setShowMetricsAction(metricsAction);
        add(toolbar, BorderLayout.NORTH);

        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setComponentPopupMenu(new ProcessContextMenu(this::performAction));
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handlePopupSelection(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handlePopupSelection(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    performAction(ProcessAction.DETAIL);
                }
            }
        };
        table.addMouseListener(mouseAdapter);

        JTableHeader header = table.getTableHeader();
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int viewCol = table.columnAtPoint(e.getPoint());
                ProcessSortKey key = columnToSortKey(viewCol);
                if (key != null) {
                    tableModel.toggleSort(key);
                }
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);
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
                if (target != null) new ProcessDetailDialog(manager, target.getPid()).setVisible(true);
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
            case 7 -> ProcessSortKey.NICE;
            default -> null;
        };
    }

    private void handlePopupSelection(MouseEvent e) {
        int row = table.rowAtPoint(e.getPoint());
        if (row >= 0) {
            table.setRowSelectionInterval(row, row);
        } else {
            table.clearSelection();
        }
        if (e.isPopupTrigger()) {
            table.requestFocusInWindow();
        }
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

