package com.orca.pbl4.ui.utils;

import javax.swing.Timer;

/**
 * Utility class để refresh data theo interval
 */
public class DataRefresher {
    
    private final Timer timer;
    
    public DataRefresher(int intervalMs, Runnable task) {
        this.timer = new Timer(intervalMs, e -> task.run());
    }
    
    public void start() {
        timer.start();
    }
    
    public void stop() {
        timer.stop();
    }
    
    public void setInterval(int intervalMs) {
        timer.setDelay(intervalMs);
    }
}
