package com.github.kreig133.downloader;

import com.github.kreig133.downloader.http.HTTPDownloadWorker;
import com.google.common.collect.ImmutableSet;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by eduardshangareev on 19/05/14.
 */
public class DownloadManager {

    public enum Protocol {HTTP, FTP, WebDav}

    private final DownloadController controller;

    public DownloadManager() {
        controller = new DownloadController();
    }

    public DownloadManager(int workerMaxCount){
        controller = new DownloadController(workerMaxCount);
    }

    public Set<DownloadResponse> download(DownloadRequest request) {
        List<DownloadResponse> responses = new ArrayList<>();

        for(String address: request.getJobs()) {
            HTTPDownloadWorker job = new HTTPDownloadWorker(address);
            controller.addTask(job);
            responses.add(job);
        }

        return ImmutableSet.copyOf(responses);
    }


    public DownloadResponse download(DownloadRequest request, Protocol protocol) {
        throw new NotImplementedException();
    }
}
