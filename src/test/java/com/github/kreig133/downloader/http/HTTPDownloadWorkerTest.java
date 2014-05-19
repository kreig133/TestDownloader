package com.github.kreig133.downloader.http;

import com.github.kreig133.downloader.DownloadResponse;
import com.github.kreig133.downloader.http.HTTPDownloadWorker;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Created by eduardshangareev on 19/05/14.
 */
public class HTTPDownloadWorkerTest {

    private String NOT_EXIST_FILE = "http://i.stolica.fm/mp3a/uploads/spiridonovim_2014_05_19.mp3";
    private String     EXIST_FILE ="http://docs.spring.io/spring/docs/3.1.x/spring-framework-reference/pdf/spring-framework-reference.pdf";


    private HTTPDownloadWorker startDownloading(String url) {
        HTTPDownloadWorker response = new HTTPDownloadWorker(url);

        new Thread(response).start();
        return response;
    }


    @Test
    public void testWhenFileNotExist() throws InterruptedException {
        HTTPDownloadWorker response = startDownloading(NOT_EXIST_FILE);

        File file = response.getOrWait();

        Assert.assertEquals(DownloadResponse.State.ERROR, response.getState());
        Assert.assertTrue(response.getError() instanceof FileNotFoundException);
    }

    @Test
    public void testWhenFileExist() throws InterruptedException {
        HTTPDownloadWorker response = startDownloading(EXIST_FILE);

        File file = response.getOrWait();

        Assert.assertEquals(response.getState(), DownloadResponse.State.COMPLETED);
        Assert.assertTrue(file.exists() && file.length() > 4 * 1024 * 1024);

        file.delete();
    }




    @Test
    public void testThatStoppingTaskWork() throws InterruptedException {
        HTTPDownloadWorker response = startDownloading(EXIST_FILE);

        long start = System.currentTimeMillis();

        response.stop();

        while ((System.currentTimeMillis() - start) < 2000) {
            Assert.assertEquals(response.getState(), DownloadResponse.State.STOPPED);
            Thread.sleep(500);
        }

        response.resume();
        Thread.sleep(300);

        Assert.assertEquals(response.getState(), DownloadResponse.State.STARTED);

        File file = response.getOrWait();

        Assert.assertEquals(response.getState(), DownloadResponse.State.COMPLETED);

        file.delete();
    }



    @Test
    public void testCallingStopResumeManyTimesHavePredictableBehaivour() throws InterruptedException {
        final HTTPDownloadWorker response = startDownloading(EXIST_FILE);

        final long start = System.currentTimeMillis();

        while ((System.currentTimeMillis() - start) < 10000) {
            response.resume();
            response.stop();

            Thread.sleep(100);
            Assert.assertEquals(DownloadResponse.State.STOPPED, response.getState());
        }

        response.cancel();
    }

    @Test
    public void testDownloadingFileWhileManyOtherThreadsReadState() throws InterruptedException {
        final HTTPDownloadWorker response = startDownloading(EXIST_FILE);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                String name = Thread.currentThread().getName();
                while (true) {
                    DownloadResponse.State state = response.getState();
                    File get = response.getOrNull();

                    if (state == DownloadResponse.State.COMPLETED && get == null) {
                        System.out.println("FAIL! State is completed, get is null.");
                    }
                }
            }
        };

        new Thread(runnable).start();
        new Thread(runnable).start();
        new Thread(runnable).start();
        new Thread(runnable).start();

        File file = response.getOrWait();

        Assert.assertEquals(response.getState(), DownloadResponse.State.COMPLETED);
        Assert.assertTrue(file.exists() && file.length() > 4 * 1024 * 1024);

        file.delete();
    }
}
