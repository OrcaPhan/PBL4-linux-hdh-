package com.orca.pbl4.ui.process;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.util.function.Consumer;

/**
 * Context menu cho bảng tiến trình.
 */
public class ProcessContextMenu extends JPopupMenu {

    public ProcessContextMenu(Consumer<ProcessAction> actionConsumer) {
        JMenuItem kill = new JMenuItem("Kill");
        kill.addActionListener(e -> actionConsumer.accept(ProcessAction.KILL));
        add(kill);

        JMenuItem stop = new JMenuItem("Stop");
        stop.addActionListener(e -> actionConsumer.accept(ProcessAction.STOP));
        add(stop);

        JMenuItem cont = new JMenuItem("Continue");
        cont.addActionListener(e -> actionConsumer.accept(ProcessAction.CONTINUE));
        add(cont);

        JMenuItem renice = new JMenuItem("Renice...");
        renice.addActionListener(e -> actionConsumer.accept(ProcessAction.RENICE));
        add(renice);

        JMenuItem detail = new JMenuItem("Properties...");
        detail.addActionListener(e -> actionConsumer.accept(ProcessAction.DETAIL));
        add(detail);
    }
}

