package com.orca.pbl4.ui.process;

import com.orca.pbl4.core.model.HandleInfo;
import com.orca.pbl4.core.model.ProcessInfo;
import com.orca.pbl4.core.model.ThreadInfo;
import com.orca.pbl4.service.process.DefaultProcessManager;
import com.orca.pbl4.service.process.ProcessManager;
import com.orca.pbl4.service.process.ProcessSignalService;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

public class ProcessDetailDialog extends JDialog {

    private static final long HZ = 100; // Clock ticks per second
    private static final long PAGE_SIZE_KB = 4;

    // Tối ưu: Tính toán boot time 1 lần duy nhất (Static initialization) để không đọc file mỗi giây
    private static final long BOOT_TIME_SECONDS;
    static {
        long bootTime = System.currentTimeMillis() / 1000;
        try {
            String uptimeStr = Files.readString(Paths.get("/proc/uptime"));
            double uptimeSeconds = Double.parseDouble(uptimeStr.split("\\s+")[0]);
            bootTime = (long) (System.currentTimeMillis() / 1000 - uptimeSeconds);
        } catch (Exception ignored) { }
        BOOT_TIME_SECONDS = bootTime;
    }

    private final ProcessManager manager;
    private int pid;
    private ProcessInfo info;
    private final Supplier<Integer> selectedPidSupplier;

    // UI Cache: Lưu trữ các Label để update text thay vì vẽ lại panel
    private final Map<String, JLabel> uiLabels = new HashMap<>();
    private JTextArea cmdlineArea;

    private javax.swing.Timer refreshTimer;
    private SwingWorker<ProcessInfo, Void> dataWorker;

    public ProcessDetailDialog(ProcessManager manager, int pid, Supplier<Integer> selectedPidSupplier) {
        super();
        this.manager = manager;
        this.pid = pid;
        this.selectedPidSupplier = selectedPidSupplier;

        setTitle("Process Detail - PID " + pid);
        setSize(600, 700);
        setLocationRelativeTo(null);
        setModal(false);

        // 1. Dựng khung UI (chỉ chạy 1 lần)
        buildUI();

        // 2. Load dữ liệu ban đầu
        loadInitialData();

        // 3. Setup Timer refresh (1 giây/lần)
        refreshTimer = new javax.swing.Timer(1000, e -> triggerBackgroundRefresh());
        refreshTimer.start();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (refreshTimer != null) refreshTimer.stop();
                if (dataWorker != null && !dataWorker.isDone()) dataWorker.cancel(true);
            }
        });
    }

    // --- Data Loading & Refresh Logic ---

    private void loadInitialData() {
        checkPidChange();
        info = manager.getProcessDetail(pid);
        if (info == null) {
            handleProcessNotFound();
        } else {
            updateUIValues(); // Update dữ liệu lên UI đã dựng sẵn
        }
    }

    private void triggerBackgroundRefresh() {
        // Nếu worker cũ chưa xong thì bỏ qua tick này
        if (dataWorker != null && !dataWorker.isDone()) return;

        dataWorker = new SwingWorker<>() {
            @Override
            protected ProcessInfo doInBackground() {
                checkPidChange(); // Kiểm tra xem người dùng có chọn dòng khác ở bảng chính không
                return manager.getProcessDetail(pid);
            }

            @Override
            protected void done() {
                try {
                    ProcessInfo newInfo = get();
                    if (newInfo == null) {
                        // Process có thể đã tắt, giữ nguyên info cũ hoặc xử lý tùy ý
                        return;
                    }
                    info = newInfo;
                    updateUIValues();
                    setTitle("Process Detail - PID " + pid);
                } catch (Exception ignored) { }
            }
        };
        dataWorker.execute();
    }

    private void checkPidChange() {
        if (selectedPidSupplier != null) {
            Integer selectedPid = selectedPidSupplier.get();
            if (selectedPid != null && selectedPid > 0 && selectedPid != pid) {
                pid = selectedPid;
            }
        }
    }

    private void handleProcessNotFound() {
        if (refreshTimer != null) refreshTimer.stop();
        JOptionPane.showMessageDialog(this,
                "Process with PID " + pid + " not found or cannot be accessed.",
                "Process Not Found", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

    // --- UI Construction (Chỉ chạy 1 lần) ---

    private void buildUI() {
        setLayout(new BorderLayout(8, 8));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Info Panel
        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("Process Information"));
        infoPanel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 8, 4, 8);
        int row = 0;

        // Tạo các dòng thông tin và lưu reference của Label giá trị vào Map
        addInfoRow(infoPanel, "Process name:", "name", gbc, row++);
        addInfoRow(infoPanel, "User:", "user", gbc, row++);
        addInfoRow(infoPanel, "State:", "state", gbc, row++);
        addInfoRow(infoPanel, "CPU %:", "cpu", gbc, row++);
        addInfoRow(infoPanel, "CPU time:", "cpuTime", gbc, row++);
        addInfoRow(infoPanel, "Memory %:", "mem", gbc, row++);
        addInfoRow(infoPanel, "RSS:", "rss", gbc, row++);
        addInfoRow(infoPanel, "Virtual memory:", "vsz", gbc, row++);
        addInfoRow(infoPanel, "Shared memory:", "shared", gbc, row++);
        addInfoRow(infoPanel, "Nice:", "nice", gbc, row++);
        addInfoRow(infoPanel, "Priority:", "priority", gbc, row++);
        addInfoRow(infoPanel, "Started:", "startTime", gbc, row++);
        addInfoRow(infoPanel, "PID:", "pid", gbc, row++);
        addInfoRow(infoPanel, "Threads:", "threads", gbc, row++);
        addInfoRow(infoPanel, "Handles:", "handles", gbc, row++);
        addInfoRow(infoPanel, "I/O Read:", "ioRead", gbc, row++);
        addInfoRow(infoPanel, "I/O Write:", "ioWrite", gbc, row++);

        // Command line area (Riêng biệt vì nó là TextArea)
        gbc.gridx = 0; gbc.gridy = row++;
        gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        infoPanel.add(new JLabel("Command line:"), gbc);

        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;
        cmdlineArea = new JTextArea();
        cmdlineArea.setEditable(false);
        cmdlineArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        cmdlineArea.setBackground(Color.WHITE);
        cmdlineArea.setBorder(BorderFactory.createLoweredBevelBorder());
        cmdlineArea.setLineWrap(true);

        JScrollPane cmdlineScroll = new JScrollPane(cmdlineArea);
        cmdlineScroll.setPreferredSize(new Dimension(0, 100));
        infoPanel.add(cmdlineScroll, gbc);

        JScrollPane mainScroll = new JScrollPane(infoPanel);
        mainScroll.getVerticalScrollBar().setUnitIncrement(16);
        mainScroll.setBorder(BorderFactory.createLineBorder(new Color(0xD0D0D0)));
        add(mainScroll, BorderLayout.CENTER);

        // Button Panel
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    private void addInfoRow(JPanel panel, String labelText, String key, GridBagConstraints gbc, int row) {
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 1; gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;

        JLabel lblTitle = new JLabel(labelText);
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD));
        panel.add(lblTitle, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblValue = new JLabel("..."); // Placeholder
        uiLabels.put(key, lblValue); // Lưu vào Map để update sau
        panel.add(lblValue, gbc);
    }

    // --- UI Updates (Chạy mỗi giây) ---

    private void updateUIValues() {
        if (info == null) return;

        setLabelText("name", safe(info.getName(), "?"));
        setLabelText("user", safe(info.getUser(), "?"));
        setLabelText("state", formatState(info.getState()));

        Float cpu = info.getCpuPercent();
        setLabelText("cpu", cpu != null ? String.format(Locale.US, "%.1f%%", cpu) : "N/A");
        setLabelText("cpuTime", formatTime(info.getProcCpuTicks() / (double) HZ));

        Float mem = info.getMemoryPercent();
        setLabelText("mem", mem != null ? String.format(Locale.US, "%.1f%%", mem) : "N/A");

        setLabelText("rss", formatMemory(info.getRssPages() * PAGE_SIZE_KB));
        setLabelText("vsz", info.getVirtualPages() > 0 ? formatMemory(info.getVirtualPages() * PAGE_SIZE_KB) : "N/A");
        setLabelText("shared", info.getSharedPages() > 0 ? formatMemory(info.getSharedPages() * PAGE_SIZE_KB) : "N/A");

        setLabelText("nice", String.valueOf(info.getNice()));
        setLabelText("priority", String.valueOf(info.getPriority()));
        setLabelText("startTime", formatStartTime(info.getStartTimeTicks()));
        setLabelText("pid", String.valueOf(info.getPid()));

        setLabelText("threads", String.valueOf(info.getThreads() != null ? info.getThreads().size() : 0));
        setLabelText("handles", String.valueOf(info.getHandles() != null ? info.getHandles().size() : 0));

        setLabelText("ioRead", info.getIoReadBytes() > 0 ? formatBytes(info.getIoReadBytes()) : "0 B");
        setLabelText("ioWrite", info.getIoWriteBytes() > 0 ? formatBytes(info.getIoWriteBytes()) : "0 B");

        String newCmd = safe(info.getCmdline(), "(no command line)");
        if (!newCmd.equals(cmdlineArea.getText())) {
            cmdlineArea.setText(newCmd);
            cmdlineArea.setCaretPosition(0);
        }
    }

    private void setLabelText(String key, String text) {
        JLabel lbl = uiLabels.get(key);
        if (lbl != null) lbl.setText(text);
    }

    // --- Button Actions ---

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Actions"));

        JButton btnKill = createStyledButton("Kill", e -> handleKill());
        JButton btnStop = createStyledButton("Stop", e -> handleSignalAction("STOP", "Stop"));
        JButton btnContinue = createStyledButton("Continue", e -> handleSignalAction("CONT", "Continue"));
        JButton btnSetNice = createStyledButton("Change Priority...", e -> handleSetPriority());
        JButton btnClose = createStyledButton("Close", e -> dispose());

        panel.add(btnKill);
        panel.add(btnStop);
        panel.add(btnContinue);
        panel.add(btnSetNice);
        panel.add(btnClose);
        return panel;
    }

    private JButton createStyledButton(String text, java.awt.event.ActionListener action) {
        JButton btn = new JButton(text);
        btn.addActionListener(action);
        ProcessTableStyleUtil.applyButtonStyle(btn);
        return btn;
    }

    // --- Logic Xử lý sự kiện (Kill, Stop, Priority) ---

    private void handleKill() {
        if (!confirmAction("Kill", "Are you sure you want to kill this process?")) return;

        // Kill riêng biệt: Thành công -> Refresh Manager -> Đóng Dialog
        boolean requireRoot = shouldRequireRoot();
        sendSignalAsync(pid, "TERM", requireRoot, "Kill", () -> {
            manager.refreshSnapshot();
            dispose(); // <--- ĐÓNG DIALOG NGAY
        });
    }

    private void handleSignalAction(String signal, String actionName) {
        boolean requireRoot = shouldRequireRoot();
        sendSignalAsync(pid, signal, requireRoot, actionName, () -> {
            manager.refreshSnapshot();
            triggerBackgroundRefresh();
        });
    }

    private void handleSetPriority() {
        String[] priorities = {"Very High", "High", "Above Normal", "Normal", "Below Normal", "Low", "Very Low"};

        String currentPriority = (info != null) ? resolvePriorityFromNice(info.getNice()) : "Normal";

        JComboBox<String> comboBox = new JComboBox<>(priorities);
        comboBox.setSelectedItem(currentPriority);
        comboBox.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.add(new JLabel("Select priority level:"), BorderLayout.NORTH);
        panel.add(comboBox, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        int result = JOptionPane.showConfirmDialog(this, panel, "Change Priority",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String selectedPriority = (String) comboBox.getSelectedItem();
            if (selectedPriority != null) {
                int niceValue = mapPriorityToNice(selectedPriority);
                boolean requireRoot = shouldRequireRoot();

                ProcessSignalService signalService = getSignalService();

                if (signalService != null) {
                    signalService.reniceAsync(pid, niceValue, requireRoot, r -> {
                        SwingUtilities.invokeLater(() -> {
                            if (r == ProcessSignalService.SignalResult.SUCCESS) {
                                JOptionPane.showMessageDialog(this, "Priority changed successfully.",
                                        "Success", JOptionPane.INFORMATION_MESSAGE);
                                manager.refreshSnapshot();
                                triggerBackgroundRefresh();
                            } else {
                                handleSignalResult(r, "Change Priority", null);
                            }
                        });
                    });
                } else {
                    // Fallback method cũ
                    if (manager.reniceProcess(pid, niceValue)) {
                        JOptionPane.showMessageDialog(this, "Priority changed successfully.",
                                "Success", JOptionPane.INFORMATION_MESSAGE);
                        manager.refreshSnapshot();
                        triggerBackgroundRefresh();
                    } else {
                        showError("Change Priority", "Failed to change priority.");
                    }
                }
            }
        }
    }

    private void sendSignalAsync(int pid, String signal, boolean requireRoot, String actionName, Runnable onSuccess) {
        ProcessSignalService signalService = getSignalService();
        if (signalService == null) {
            boolean success = switch (signal) {
                case "TERM", "KILL" -> manager.killProcess(pid);
                case "STOP" -> manager.stopProcess(pid);
                case "CONT" -> manager.continueProcess(pid);
                default -> false;
            };
            if (success && onSuccess != null) onSuccess.run();
            else if (!success) showError(actionName, "Failed to " + actionName.toLowerCase() + " process.");
            return;
        }

        signalService.sendSignal(pid, signal, requireRoot, result -> {
            SwingUtilities.invokeLater(() -> handleSignalResult(result, actionName, onSuccess));
        });
    }

    private void handleSignalResult(ProcessSignalService.SignalResult result, String actionName, Runnable onSuccess) {
        switch (result) {
            case SUCCESS -> { if (onSuccess != null) onSuccess.run(); }
            case PERMISSION_DENIED -> showError(actionName, "Permission denied. Root privileges required.");
            case PROCESS_NOT_FOUND -> showError(actionName, "Process not found.");
            case USER_CANCELLED -> showError(actionName, "Action cancelled.");
            case UNKNOWN_ERROR -> showError(actionName, "Unknown error.");
        }
    }

    // --- Helpers ---

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

    private ProcessSignalService getSignalService() {
        return (manager instanceof DefaultProcessManager) ? ((DefaultProcessManager) manager).getSignalService() : null;
    }

    private boolean shouldRequireRoot() {
        if (info == null) return false;
        String user = info.getUser();
        return user != null && (!user.equals(System.getProperty("user.name")) || "root".equals(user));
    }

    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title + " Error", JOptionPane.ERROR_MESSAGE);
    }

    private boolean confirmAction(String action, String message) {
        return JOptionPane.showConfirmDialog(this, message, action + " Process",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private String formatState(String state) {
        if (state == null || state.isEmpty()) return "?";
        return switch (state.charAt(0)) {
            case 'R' -> "R (Running)";
            case 'S' -> "S (Sleeping)";
            case 'D' -> "D (Disk sleep)";
            case 'T' -> "T (Stopped)";
            case 'Z' -> "Z (Zombie)";
            default -> state;
        };
    }

    private String formatTime(double seconds) {
        if (seconds < 60) return String.format(Locale.US, "%.2f s", seconds);
        if (seconds < 3600) return String.format(Locale.US, "%.2f m", seconds / 60);
        return String.format(Locale.US, "%.2f h", seconds / 3600);
    }

    private String formatMemory(long kb) {
        double d = kb;
        if (d < 1024) return String.format(Locale.US, "%.2f KiB", d);
        d /= 1024.0;
        if (d < 1024) return String.format(Locale.US, "%.2f MiB", d);
        return String.format(Locale.US, "%.2f GiB", d / 1024.0);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " bytes";
        return formatMemory(bytes / 1024);
    }

    private String formatStartTime(long startTimeTicks) {
        // Sử dụng BOOT_TIME_SECONDS đã tính sẵn, cực nhanh
        long startTimeSeconds = BOOT_TIME_SECONDS + (startTimeTicks / HZ);
        try {
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(startTimeSeconds), ZoneId.systemDefault())
                    .toString().replace('T', ' ');
        } catch (Exception e) {
            return startTimeTicks + " ticks";
        }
    }
}