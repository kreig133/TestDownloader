package com.github.kreig133.downloader;

import java.io.File;

/**
 * Created by eduardshangareev on 19/05/14.
 */
public interface DownloadResponse {

    /**
     * State of downloading task.
     *
     * Possible state transitions:
     * NEW -> STARTED, STOPPED, ERROR, CANCELED_BY_USER
     * STARTED -> STOPPED, COMPLETED, ERROR, CANCELED_BY_USER
     * STOPPED -> STARTED, ERROR, CANCELLED_BY_USER
     * COMPLETED -> None
     * ERROR -> None
     * CANCELED_BY_USER -> None
     */
    enum State {
        NEW, STARTED, STOPPED, COMPLETED, ERROR, CANCELED_BY_USER
    }

    /**
     * Cancels downloading task. Task goes into CANCELED_BY_USER state.
     */
    void cancel ();

    /**
     * Stops downloading task. Task can be resumed. After 3 min waiting task will be canceled.
     */
    void stop   ();

    /**
     * Resumes downloading task if it was stopped.
     */
    void resume();

    /**
     * @return current downloading task state
     */
    State     getState();

    /**
     * @return Throbawle if some error was occured or null otherwise
     */
    Throwable getError();

    /**
     * @return return file address which is downloaded
     */
    String    getJob  ();

    /**
     * @return downloaded file if task was successfully completed or null otherwise
     */
    File getOrNull();

    /**
     * @return downloaded file if task was successfully completed or blocks current thread until file will be downloaded
     * @throws InterruptedException
     */
    File getOrWait() throws InterruptedException;
}
