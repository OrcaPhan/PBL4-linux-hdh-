package com.orca.pbl4.ui.process;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.FlowLayout;
import java.util.function.Consumer;

/**
 * Thanh công cụ điều khiển bảng tiến trình.
 */
public class ProcessToolbarPanel extends JPanel {

    private final JTextField txtSearch = new JTextField(20);

    private Runnable refreshAction = () -> {};
    private Runnable killAction = () -> {};
    private Runnable stopAction = () -> {};
    private Runnable continueAction = () -> {};
    private Runnable showMetricsAction = () -> {};
    private Consumer<String> searchConsumer = s -> {};

    public ProcessToolbarPanel() {
        super(new FlowLayout(FlowLayout.LEFT, 8, 4));

        add(new JLabel("Search:"));
        add(txtSearch);

        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(e -> refreshAction.run());
        add(btnRefresh);

        JButton btnKill = new JButton("Kill");
        btnKill.addActionListener(e -> killAction.run());
        add(btnKill);

        JButton btnStop = new JButton("Stop");
        btnStop.addActionListener(e -> stopAction.run());
        add(btnStop);

        JButton btnContinue = new JButton("Continue");
        btnContinue.addActionListener(e -> continueAction.run());
        add(btnContinue);

        JButton btnMetrics = new JButton("Metrics");
        btnMetrics.addActionListener(e -> showMetricsAction.run());
        add(btnMetrics);

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { submit(); }
            @Override public void removeUpdate(DocumentEvent e) { submit(); }
            @Override public void changedUpdate(DocumentEvent e) { submit(); }
            private void submit() { searchConsumer.accept(txtSearch.getText()); }
        });
    }

    public void setRefreshAction(Runnable refreshAction) {
        this.refreshAction = refreshAction != null ? refreshAction : () -> {};
    }

    public void setKillAction(Runnable killAction) {
        this.killAction = killAction != null ? killAction : () -> {};
    }

    public void setStopAction(Runnable stopAction) {
        this.stopAction = stopAction != null ? stopAction : () -> {};
    }

    public void setContinueAction(Runnable continueAction) {
        this.continueAction = continueAction != null ? continueAction : () -> {};
    }

    public void setShowMetricsAction(Runnable showMetricsAction) {
        this.showMetricsAction = showMetricsAction != null ? showMetricsAction : () -> {};
    }

    public void setSearchConsumer(Consumer<String> searchConsumer) {
        this.searchConsumer = searchConsumer != null ? searchConsumer : s -> {};
    }
}