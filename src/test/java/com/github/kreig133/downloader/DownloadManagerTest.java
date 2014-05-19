package com.github.kreig133.downloader;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Set;

/**
 * Created by eduardshangareev on 20/05/14.
 */
public class DownloadManagerTest {
    String s0 = "ftp://ftp4.uk.freebsd.org/pub/FreeBSD/doc/ru_RU.KOI8-R/books/faq/book.pdf.zip";
    String s1 = "http://www.auby.no/files/video_tests/h264_1080p_hp_4.1_40mbps_birds.mkv";
    String s2 = "http://www.auby.no/files/video_tests/vc1_1080p_ap_l3_18mbps_ac3_hddvd.mkv";
    String s3 = "http://docs.spring.io/spring/docs/3.1.x/spring-framework-reference/pdf/spring-framework-reference.pdf";
    String s4 = "http://cdn.radio-t.com/rt_podcast392.mp3";
    String s5 = "http://doc.akka.io/docs/akka/2.3.2/AkkaScala.pdf";
    String s6 = "http://i.stolica.fm/mp3/uploads/archive_audio/2014/05/19/db20e4e16471e057e24d8fd0552418ed.mp3/status_mihaila_gurevicha_2014_05_19.mp3";
    String s7 = "http://ftp.dlink.ru/pub/ADSL/DSL-G804V/Firmware/1.00.08.dm15.zip";

    @Test
    public void test() throws InterruptedException {
        DownloadManager downloadManager = new DownloadManager(4);
        Set<DownloadResponse> downloadResponses = downloadManager.download(new DownloadRequest(s0, s1, s2, s3, s4, s5, s6, s7));

        for(DownloadResponse response: downloadResponses) {
            File file = response.getOrWait();
            Assert.assertNotNull(file);
            Assert.assertEquals(DownloadResponse.State.COMPLETED, response.getState());

            System.out.println("file.getAbsolutePath() = " + file.getAbsolutePath());
            System.out.println("response.getJob() = " + response.getJob());
        }
    }
}
