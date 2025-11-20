package com.orca.pbl4.ui.process;

import com.orca.pbl4.core.model.HandleInfo;
import com.orca.pbl4.core.model.ProcessInfo;
import com.orca.pbl4.core.model.ThreadInfo;
import com.orca.pbl4.service.process.ProcessManager;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;

/**
 * Dialog hiển thị chi tiết của một tiến trình.
 * TODO: bổ sung các bảng thread/handle, biểu đồ nhỏ, ...
 */
public class ProcessDetailDialog extends JDialog {

    private final ProcessManager manager;
    private final int pid;

    public ProcessDetailDialog(ProcessManager manager, int pid) {
        super();
        this.manager = manager;
        this.pid = pid;
        setTitle("Process Detail - PID " + pid);
        setSize(480, 360);
        setLocationRelativeTo(null);
        setModal(true);

        setLayout(new BorderLayout());
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        add(content, BorderLayout.CENTER);

        // TODO: render detail info properly
        ProcessInfo info = manager.getProcessInfo(pid);
        List<ThreadInfo> threads = manager.getThreads(pid);
        List<HandleInfo> handles = manager.getHandles(pid);

        content.add(new JLabel(buildSummary(info, threads, handles)), BorderLayout.NORTH);
    }

    private String buildSummary(ProcessInfo info, List<ThreadInfo> threads, List<HandleInfo> handles) {
        // TODO: format as multiline HTML label or richer UI
        return info == null
                ? "Process info not available"
                : String.format("<html>PID: %d<br>Name: %s<br>User: %s<br>Threads: %d<br>Handles: %d</html>",
                info.getPid(), info.getName(), info.getUser(),
                threads != null ? threads.size() : 0,
                handles != null ? handles.size() : 0);
    }
}

