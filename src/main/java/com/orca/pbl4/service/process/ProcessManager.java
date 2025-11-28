package com.orca.pbl4.service.process;

import com.orca.pbl4.core.model.HandleInfo;
import com.orca.pbl4.core.model.ProcessInfo;
import com.orca.pbl4.core.model.ProcessRow;
import com.orca.pbl4.core.model.ThreadInfo;
import com.orca.pbl4.core.system.SystemSnapshot;

import java.util.List;

public interface ProcessManager {


    void refreshSnapshot();
    boolean killProcess(int pid);
    boolean stopProcess(int pid);
    boolean continueProcess(int pid);
    boolean reniceProcess(int pid, int niceValue);
    ProcessInfo getProcessInfo(int pid);
    List<ThreadInfo> getThreads(int pid);
    List<HandleInfo> getHandles(int pid);
    ProcessInfo getProcessDetail(int pid);
    SystemSnapshot getCurrentSnapshot();
    void addUpdateListener(ProcessUpdateListener listener);
    void removeUpdateListener(ProcessUpdateListener listener);
}
