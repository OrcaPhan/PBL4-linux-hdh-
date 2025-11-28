package com.orca.pbl4.ui.process;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.util.function.Consumer;

/**
 * Context menu cho bảng tiến trình.
 * Hiển thị khi click chuột phải: Kill, Stop, Continue, Renice, Properties.
 */
public class ProcessContextMenu extends JPopupMenu {

    public ProcessContextMenu(Consumer<ProcessAction> actionConsumer) {
        JMenuItem kill = new JMenuItem("Kill");
        kill.addActionListener(e -> actionConsumer.accept(ProcessAction.KILL));
        ProcessTableStyleUtil.applyMenuItemStyle(kill);
        add(kill);

        JMenuItem stop = new JMenuItem("Stop");
        stop.addActionListener(e -> actionConsumer.accept(ProcessAction.STOP));
        ProcessTableStyleUtil.applyMenuItemStyle(stop);
        add(stop);

        JMenuItem cont = new JMenuItem("Continue");
        cont.addActionListener(e -> actionConsumer.accept(ProcessAction.CONTINUE));
        ProcessTableStyleUtil.applyMenuItemStyle(cont);
        add(cont);

        addSeparator();

        JMenuItem priority = new JMenuItem("Priority");
        priority.addActionListener(e -> actionConsumer.accept(ProcessAction.RENICE));
        ProcessTableStyleUtil.applyMenuItemStyle(priority);
        add(priority);

        JMenuItem properties = new JMenuItem("Properties");
        properties.addActionListener(e -> actionConsumer.accept(ProcessAction.DETAIL));
        ProcessTableStyleUtil.applyMenuItemStyle(properties);
        add(properties);
    }
}

