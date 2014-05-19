package com.github.kreig133.downloader;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by eduardshangareev on 19/05/14.
 */
public class DownloadController {
    private final ExecutorService executorsService;

    public DownloadController(){
        this(Runtime.getRuntime().availableProcessors()* 2);
    }

    /**
     * @param workerCount max count of parallel downloading tasks
     */
    public DownloadController(int workerCount) {
        this.executorsService = Executors.newFixedThreadPool(workerCount);
    }

    /**
     * @param task - downloading task
     */
    public void addTask(Runnable task) {
        executorsService.submit(task);
    }
}
