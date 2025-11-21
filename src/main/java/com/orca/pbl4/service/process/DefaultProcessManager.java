package com.orca.pbl4.service.process;

import com.orca.pbl4.core.model.HandleInfo;
import com.orca.pbl4.core.model.ProcessInfo;
import com.orca.pbl4.core.model.ProcessRow;
import com.orca.pbl4.core.model.ThreadInfo;
import com.orca.pbl4.core.system.SystemMonitor;
import com.orca.pbl4.core.system.SystemSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Triển khai mặc định dựa trên SystemMonitor + snapshot từ /proc.
 * TODO: triển khai đầy đủ logic đọc snapshot, tính delta CPU/IO, smoothing EMA và thao tác tiến trình.
 */
public class DefaultProcessManager implements ProcessManager {

    private static final float ALPHA = 0.3f;
    private static final long PAGE_KB = 4;

    private final SystemMonitor monitor;
    private final List<ProcessUpdateListener> listeners = new CopyOnWriteArrayList<>();
    private final Map<Integer, Float> smoothCpuCache = new HashMap<>();

    private SystemSnapshot previousSnapshot;
    private SystemSnapshot currentSnapshot;
    private List<ProcessRow> cachedRows = Collections.emptyList();

    public DefaultProcessManager(SystemMonitor monitor) {
        this.monitor = Objects.requireNonNull(monitor, "monitor");
    }

    @Override
    public synchronized void refreshSnapshot() {
        SystemSnapshot prev = currentSnapshot;
        if (prev == null) {
            prev = monitor.getSystemSnapshot(false);
        }
        SystemSnapshot fresh = monitor.getSystemSnapshot(false);

        previousSnapshot = prev;
        currentSnapshot = fresh;
        cachedRows = Collections.unmodifiableList(buildRows(prev, fresh));
        notifyListeners();
    }

    @Override
    public synchronized List<ProcessRow> getAllProcesses() {
        return cachedRows;
    }

    @Override
    public synchronized List<ProcessRow> queryProcesses(ProcessQuery query) {
        List<ProcessRow> base = cachedRows;
        if (query == null) return base;

        List<ProcessRow> filtered = new ArrayList<>();
        String search = normalize(query.getSearchText());
        ProcessFilter filter = query.getFilter();
        String stateFilter = normalize(query.getStateFilter());
        Set<Character> states = new HashSet<>();
        if (stateFilter != null && !"all".equals(stateFilter)) {
            for (char c : stateFilter.toCharArray()) states.add(Character.toUpperCase(c));
        }
        if (filter != null && filter.getStates() != null) {
            states.addAll(filter.getStates());
        }

        for (ProcessRow row : base) {
            if (!matchesSearch(row, search)) continue;
            if (!states.isEmpty() && !states.contains(Character.toUpperCase(row.getState()))) continue;
            if (filter != null && !filter.matchesUser(row.getUser())) continue;
            filtered.add(row);
        }

        Comparator<ProcessRow> cmp = makeComparator(query.getSortKey());
        if (cmp != null) {
            filtered.sort(query.isDescending() ? cmp.reversed() : cmp);
        }
        return filtered;
    }

    @Override
    public boolean killProcess(int pid) {
        return ProcessHandle.of(pid).map(ProcessHandle::destroy).orElse(false);
    }

    @Override
    public boolean stopProcess(int pid) {
        return sendSignal(pid, "STOP");
    }

    @Override
    public boolean continueProcess(int pid) {
        return sendSignal(pid, "CONT");
    }

    @Override
    public boolean reniceProcess(int pid, int niceValue) {
        try {
            new ProcessBuilder("renice", String.valueOf(niceValue), "-p", String.valueOf(pid))
                    .inheritIO()
                    .start()
                    .waitFor();
            return true;
        } catch (Exception ex) {
        return false;
        }
    }

    @Override
    public ProcessInfo getProcessInfo(int pid) {
        if (currentSnapshot == null) return null;
        return currentSnapshot.getProcesses().stream()
                .filter(p -> p.getPid() == pid)
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<ThreadInfo> getThreads(int pid) {
        ProcessInfo info = getProcessInfo(pid);
        return info != null ? info.getThreads() : Collections.emptyList();
    }

    @Override
    public List<HandleInfo> getHandles(int pid) {
        ProcessInfo info = getProcessInfo(pid);
        return info != null ? info.getHandles() : Collections.emptyList();
    }

    @Override
    public SystemSnapshot getCurrentSnapshot() {
        return currentSnapshot;
    }

    @Override
    public void addUpdateListener(ProcessUpdateListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeUpdateListener(ProcessUpdateListener listener) {
        listeners.remove(listener);
    }

    private List<ProcessRow> buildRows(SystemSnapshot prev, SystemSnapshot curr) {
        if (prev == null || curr == null) return Collections.emptyList();

        long deltaTotal = Math.max(1,
                curr.getCpu().getTotalTicks() - prev.getCpu().getTotalTicks());

        long totalMemKB = curr.getMemory().getTotalKB();
        if (totalMemKB > 1_000_000_000L) totalMemKB /= 1024;

        int cores = curr.getCpu().getCoreCount() > 0
                ? curr.getCpu().getCoreCount()
                : Runtime.getRuntime().availableProcessors();

        Map<Integer, ProcessInfo> prevMap = new HashMap<>();
        for (ProcessInfo info : prev.getProcesses()) {
            prevMap.put(info.getPid(), info);
        }

        List<ProcessRow> rows = new ArrayList<>();
        for (ProcessInfo cur : curr.getProcesses()) {
            ProcessInfo old = prevMap.get(cur.getPid());
            long deltaProc = 0;
            if (old != null) {
                deltaProc = Math.max(0, cur.getProcCpuTicks() - old.getProcCpuTicks());
            }

            float cpuPercent = deltaProc * 100f / deltaTotal;
//            cpuPercent *= cores;

            float smooth = smoothCpu(cur.getPid(), cpuPercent);

            long rssKB = cur.getRssPages() * PAGE_KB;
            float memPercent = (totalMemKB > 0) ? rssKB * 100f / totalMemKB : 0f;

            ProcessRow row = new ProcessRow();
            row.setPid(cur.getPid());
            row.setName(safe(cur.getName(), "?"));
            row.setUser(safe(cur.getUser(), "?"));
            row.setCpuPercent(smooth);
            row.setMemPercent(memPercent);
            row.setMemoryStr(formatKB(rssKB));
            row.setDiskRead(formatIO(cur.getIoReadBytes()));
            row.setDiskWrite(formatIO(cur.getIoWriteBytes()));
            row.setPriority(resolvePriority(cur.getNice()));
            row.setState(cur.getState() != null && !cur.getState().isEmpty()
                    ? cur.getState().charAt(0) : '?');
            rows.add(row);
        }

        rows.sort((a, b) -> Float.compare(b.getCpuPercent(), a.getCpuPercent()));
        return rows;
    }

    private float smoothCpu(int pid, float current) {
        float prev = smoothCpuCache.getOrDefault(pid, current);
        float smooth = (1 - ALPHA) * prev + ALPHA * current;
        smoothCpuCache.put(pid, smooth);
        return smooth;
    }

    private String resolvePriority(int nice) {
        if (nice <= -10) return "High";
        if (nice < 0) return "Above Normal";
        if (nice == 0) return "Normal";
        if (nice <= 10) return "Below Normal";
        return "Low";
    }

    private String formatIO(long bytes) {
        if (bytes <= 0) return "-";
        return formatKB(bytes / 1024);
    }

    private String formatKB(long kb) {
        if (kb < 1024) return kb + " KB";
        float mb = kb / 1024f;
        if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb);
        return String.format(Locale.US, "%.1f GB", mb / 1024f);
    }

    private boolean matchesSearch(ProcessRow row, String search) {
        if (search == null || search.isBlank()) return true;
        try {
            int pid = Integer.parseInt(search);
            return row.getPid() == pid;
        } catch (NumberFormatException ignored) { }

        String lower = search.toLowerCase(Locale.ROOT);
        return (row.getName() != null && row.getName().toLowerCase(Locale.ROOT).contains(lower))
                || (row.getUser() != null && row.getUser().toLowerCase(Locale.ROOT).contains(lower));
    }

    private Comparator<ProcessRow> makeComparator(ProcessSortKey key) {
        ProcessSortKey k = key != null ? key : ProcessSortKey.CPU_PERCENT;
        return switch (k) {
            case PID -> Comparator.comparingInt(ProcessRow::getPid);
            case USER -> Comparator.comparing(ProcessRow::getUser, String.CASE_INSENSITIVE_ORDER);
            case NAME -> Comparator.comparing(ProcessRow::getName, String.CASE_INSENSITIVE_ORDER);
            case CPU_PERCENT -> Comparator.comparingDouble(ProcessRow::getCpuPercent);
            case MEMORY_PERCENT -> Comparator.comparingDouble(ProcessRow::getMemPercent);
            case RSS -> Comparator.comparing(ProcessRow::getMemoryStr, String.CASE_INSENSITIVE_ORDER);
            case STATE -> Comparator.comparingInt(r -> r.getState());
            case NICE -> Comparator.comparing(ProcessRow::getPriority, String.CASE_INSENSITIVE_ORDER);
        };
    }

    private boolean sendSignal(int pid, String signal) {
        try {
            new ProcessBuilder("kill", "-" + signal, String.valueOf(pid)).start().waitFor();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private String normalize(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private void notifyListeners() {
        List<ProcessRow> snapshot = cachedRows;
        SystemSnapshot sys = currentSnapshot;
        for (ProcessUpdateListener listener : listeners) {
            try {
                listener.onProcessSnapshotUpdated(snapshot, sys);
            } catch (Exception ignored) {
            }
        }
    }
}