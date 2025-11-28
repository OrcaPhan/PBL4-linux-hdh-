package com.orca.pbl4.service.process;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ProcessSignalService {

    public enum SignalResult {
        SUCCESS,
        PERMISSION_DENIED,
        PROCESS_NOT_FOUND,
        USER_CANCELLED,
        UNKNOWN_ERROR
    }

    public void sendSignal(int pid, String signal, boolean requireRoot,
                          java.util.function.Consumer<SignalResult> callback) {
        new Thread(() -> {
            SignalResult result = doSendSignal(pid, signal, requireRoot);
            // Quay lại EDT để gọi callback
            SwingUtilities.invokeLater(() -> callback.accept(result));
        }, "signal-" + signal + "-" + pid).start();
    }

    private SignalResult doSendSignal(int pid, String signal, boolean requireRoot) {
        try {
            String sigOpt = switch (signal) {
                case "TERM" -> "-TERM";
                case "STOP" -> "-STOP";
                case "CONT" -> "-CONT";
                default -> "-TERM";
            };

            String[] cmd;
            if (requireRoot) {
                // Dùng pkexec: hệ điều hành sẽ hiện dialog mật khẩu
                cmd = new String[]{"pkexec", "/bin/kill", sigOpt, String.valueOf(pid)};
            } else {
                // Thử trực tiếp trước
                cmd = new String[]{"/bin/kill", sigOpt, String.valueOf(pid)};
            }

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            // Đọc output (stdout + stderr đã được merge) để phân biệt lỗi
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exit = p.waitFor();

            if (exit == 0) {
                return SignalResult.SUCCESS;
            }

            String errorMsg = output.toString().toLowerCase();
            
            if (exit == 1) {
                if (errorMsg.contains("no such process") || errorMsg.contains("process not found")) {
                    return SignalResult.PROCESS_NOT_FOUND;
                }
                if (requireRoot) {
                    return SignalResult.USER_CANCELLED;
                }
                return tryWithPkexec(pid, sigOpt);
            } else if (exit == 3) {
                return SignalResult.PROCESS_NOT_FOUND;
            } else if (exit == 126 || exit == 127) {
                return SignalResult.UNKNOWN_ERROR;
            }

            return SignalResult.UNKNOWN_ERROR;

        } catch (IOException e) {
            e.printStackTrace();
            return SignalResult.PERMISSION_DENIED;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SignalResult.UNKNOWN_ERROR;
        }
    }

    private SignalResult tryWithPkexec(int pid, String sigOpt) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "pkexec", "/bin/kill", sigOpt, String.valueOf(pid));
            pb.redirectErrorStream(true);
            Process p = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exit = p.waitFor();

            if (exit == 0) {
                return SignalResult.SUCCESS;
            }

            String errorMsg = output.toString().toLowerCase();
            if (exit == 1) {
                if (errorMsg.contains("no such process") || errorMsg.contains("process not found")) {
                    return SignalResult.PROCESS_NOT_FOUND;
                }
                // User cancel hoặc sai mật khẩu
                return SignalResult.USER_CANCELLED;
            } else if (exit == 3) {
                return SignalResult.PROCESS_NOT_FOUND;
            } else if (exit == 126 || exit == 127) {
                return SignalResult.UNKNOWN_ERROR;
            }

            return SignalResult.UNKNOWN_ERROR;
        } catch (IOException | InterruptedException e) {
            return SignalResult.UNKNOWN_ERROR;
        }
    }

    public void reniceAsync(int pid, int niceValue, boolean requireRoot,
                            java.util.function.Consumer<SignalResult> callback) {
        new Thread(() -> {
            SignalResult result = doRenice(pid, niceValue, requireRoot);
            // Quay lại EDT để gọi callback
            SwingUtilities.invokeLater(() -> callback.accept(result));
        }, "renice-" + pid).start();
    }

    private SignalResult doRenice(int pid, int niceValue, boolean requireRoot) {
        try {
            String[] cmd;
            if (requireRoot) {
                cmd = new String[]{
                    "pkexec", "/usr/bin/renice", String.valueOf(niceValue), "-p", String.valueOf(pid)
                };
            } else {
                // Thử trực tiếp trước
                cmd = new String[]{
                    "/usr/bin/renice", String.valueOf(niceValue), "-p", String.valueOf(pid)
                };
            }

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            // Đọc output (stdout + stderr đã được merge) để phân biệt lỗi
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exit = p.waitFor();

            if (exit == 0) {
                return SignalResult.SUCCESS;
            }

            String errorMsg = output.toString().toLowerCase();
            
            if (exit == 1) {
                if (errorMsg.contains("no such process") || errorMsg.contains("process not found")) {
                    return SignalResult.PROCESS_NOT_FOUND;
                }
                if (requireRoot) {
                    return SignalResult.USER_CANCELLED;
                }
                return tryReniceWithPkexec(pid, niceValue);
            } else if (exit == 126 || exit == 127) {
                return SignalResult.UNKNOWN_ERROR;
            }

            return SignalResult.UNKNOWN_ERROR;

        } catch (IOException e) {
            e.printStackTrace();
            return SignalResult.PERMISSION_DENIED;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SignalResult.UNKNOWN_ERROR;
        }
    }

    private SignalResult tryReniceWithPkexec(int pid, int niceValue) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "pkexec", "/usr/bin/renice", String.valueOf(niceValue), "-p", String.valueOf(pid));
            pb.redirectErrorStream(true);
            Process p = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exit = p.waitFor();

            if (exit == 0) {
                return SignalResult.SUCCESS;
            }

            String errorMsg = output.toString().toLowerCase();
            if (exit == 1) {
                if (errorMsg.contains("no such process") || errorMsg.contains("process not found")) {
                    return SignalResult.PROCESS_NOT_FOUND;
                }
                // User cancel hoặc sai mật khẩu
                return SignalResult.USER_CANCELLED;
            } else if (exit == 126 || exit == 127) {
                return SignalResult.UNKNOWN_ERROR;
            }

            return SignalResult.UNKNOWN_ERROR;
        } catch (IOException | InterruptedException e) {
            return SignalResult.UNKNOWN_ERROR;
        }
    }
}
