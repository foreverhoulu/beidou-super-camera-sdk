package com.beidou.superior.camera.utils;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraThreadPool {

    static Timer timerFocus = null;

    /*
     * Focus frequency
     */
    static final long cameraScanInterval = 2000;

    /*
     * Thread pool size
     */
    private static int poolCount = Runtime.getRuntime().availableProcessors();

    private static ExecutorService fixedThreadPool = Executors.newFixedThreadPool(poolCount);

    /**
     * Thread pool execute
     * @param runnable task
     */
    public static void execute(Runnable runnable) {
        fixedThreadPool.execute(runnable);
    }

    /**
     * create AutoFocus Timer Task
     * @param runnable task
     * @return Timer Timer
     */
    public static Timer createAutoFocusTimerTask(final Runnable runnable) {
        if (timerFocus != null) {
            return timerFocus;
        }
        timerFocus = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                runnable.run();
            }
        };
        timerFocus.scheduleAtFixedRate(task, 0, cameraScanInterval);
        return timerFocus;
    }

    /**
     * cancel AutoFocus Timer Task
     */
    public static void cancelAutoFocusTimer() {
        if (timerFocus != null) {
            timerFocus.cancel();
            timerFocus = null;
        }
    }
}
