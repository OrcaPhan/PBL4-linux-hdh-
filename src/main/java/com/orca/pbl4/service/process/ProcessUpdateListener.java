package com.orca.pbl4.service.process;

import com.orca.pbl4.core.model.ProcessRow;
import com.orca.pbl4.core.system.SystemSnapshot;

import java.util.List;

/**
 * Listener để UI nhận được khi ProcessManager refresh snapshot mới.
 */
public interface ProcessUpdateListener {
    void onProcessSnapshotUpdated(List<ProcessRow> rows, SystemSnapshot snapshot);
}
