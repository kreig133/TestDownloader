package com.github.kreig133.downloader.http;

import com.github.kreig133.downloader.DownloadResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by eduardshangareev on 19/05/14.
 */
public class HTTPDownloadWorker implements DownloadResponse, Runnable {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    //needed for blocking other threads that called getOrWait method
    //when state was changed to ERROR, CANCELED_BY_USER or COMPLETED then latch will be open
    private final CountDownLatch completeLatch = new CountDownLatch(1);
    //needed for prevent other thread call stop before we will process previous resume call
    private final Semaphore      stopping      = new Semaphore(1);
    //lock for atomic update state and result or state and error
    private final ReentrantReadWriteLock lock                = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock   readLock = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();


    private final    String job;
    private final    URL    url;

    //current downloading task state
    private final AtomicReference<State> state = new AtomicReference<>(State.NEW);
    private volatile Throwable error = null;
    private volatile File     result = null;

    public HTTPDownloadWorker(String urlString) {
        URL urlTemp;
        job = urlString;

        try {
            urlTemp = new URL(urlString);
        } catch (MalformedURLException e) {
            urlTemp = null;
            error = e; //object not escaped, synchronization not necessary
            state.set(State.ERROR);
            completeLatch.countDown();
            log.error("Mailformed URL : {} . State changed to ERROR.", urlString);
        }

        this.url = urlTemp;
    }

    @Override public void cancel() {

        if( state.compareAndSet(State.STOPPED, State.CANCELED_BY_USER) ||
            state.compareAndSet(State.NEW,     State.CANCELED_BY_USER) ||
            state.compareAndSet(State.STARTED, State.CANCELED_BY_USER) ) {

            completeLatch.countDown();

            synchronized (url) {
                url.notify();
            }

            log.info("state changed to CANCELED_BY_USER");
        }
    }

    @Override public void stop() {
        try {
            if(state.compareAndSet(State.NEW, State.STOPPED)) {
                log.info("state change to STOPPED");
            } else if(state.get() == State.STARTED) {
                stopping.acquire();
                if(state.compareAndSet(State.STARTED, State.STOPPED)) {
                    log.info("state change to STOPPED");
                }
                stopping.release();
            }
        } catch (InterruptedException e) {
            log.error("OMG", e);
        }
    }

    @Override public void resume() {
        if (state.get() == State.STOPPED) {
            synchronized (url) {
                if (state.compareAndSet(State.STOPPED, State.STARTED)) {
                    url.notify();
                    log.info("Task was resumed(notified)");
                }
            }
        }
    }

    @Override public State getState() {
        readLock.lock();

        try {
            return state.get();
        } finally {
            readLock.unlock();
        }

    }


    @Override public Throwable getError() {
        readLock.lock();

        try {
            return error;
        } finally {
            readLock.unlock();
        }
    }

    @Override public String getJob() {
        return job;
    }

    @Override public File getOrWait() throws InterruptedException {
        completeLatch.await();
        return result;
    }

    @Override public File getOrNull() {
        readLock.lock();

        try {
            return result;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void run() {
        BufferedInputStream in = null;
        FileOutputStream fout = null;
        File tempFile = null;

        try {
            if (state.get() == State.CANCELED_BY_USER || state.get() == State.ERROR) return;

            if (state.compareAndSet(State.NEW, State.STARTED)) {
                log.debug("state changed to STARTED");
            }

            tempFile = File.createTempFile("downloader", null);
            in       = new BufferedInputStream(url.openStream());
            fout     = new FileOutputStream(tempFile);

            int window = 8 * 1024;

            final byte data[] = new byte[window];
            int count;

            while ((count = in.read(data, 0, window)) != -1) {
                fout.write(data, 0, count);
//                log.debug("chunk of data was writed");

                if (cancelationNeeded()) return;
            }

            writeLock.lock();
            if (state.compareAndSet(State.STARTED, State.COMPLETED) ||
                    state.compareAndSet(State.STOPPED, State.COMPLETED)) {

                result = tempFile;
                log.debug("state changed to COMPLETED");
            }
            writeLock.unlock();

            completeLatch.countDown();
        } catch (Exception e) {
            log.error("error occured during downloading", e);

            writeLock.lock();
            if (state.compareAndSet(State.STARTED, State.ERROR) ||
                    state.compareAndSet(State.STOPPED, State.ERROR)) {

                error = e;
                log.error("state changed to FILE_NOT_FOUND");
            }
            writeLock.unlock();

            completeLatch.countDown();
        } finally {
            if (in != null)
                try { in.close(); } catch (Exception e) { }

            if (fout != null)
                try { fout.close(); } catch (Exception e) { }

            if (state.get() != State.COMPLETED && tempFile!= null &&  tempFile.exists())
                tempFile.delete();
        }
    }

    private boolean cancelationNeeded() throws InterruptedException {
        if(state.get() == State.STOPPED) {
            log.warn("state is STOPPED, be blocked");

            stopping.acquire();
            try {
                synchronized (url){
                    url.wait(3 * 60 * 1000);

                    writeLock.lock();
                    try {
                        if(state.compareAndSet(State.STOPPED, State.ERROR)) {
                            String message = "Task was canceled after 3 min waiting.";
                            error = new TimeoutException(message);
                            log.error(message);
                            return true;
                        }
                    } finally {
                        writeLock.unlock();
                    }

                }
            } finally {
                stopping.release();
            }
        }

        return state.get() == State.CANCELED_BY_USER;
    }
}
