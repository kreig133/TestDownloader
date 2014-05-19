package com.github.kreig133.downloader;

import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * Not thread safe
 *
 * Created by eduardshangareev on 19/05/14.
 */
public class DownloadRequest {

    private final ImmutableSet<String> jobs;

    public DownloadRequest(String address) {
        jobs = ImmutableSet.of(address);
    }

    public DownloadRequest(String ... addresses){
        jobs = ImmutableSet.copyOf(addresses);
    }

    public DownloadRequest(Iterable<String> addresses){
        jobs = ImmutableSet.copyOf(addresses);
    }

    public Iterable<String> getJobs() {
        return jobs;
    }
}
