package org.pinwheel.demo.myapplication;

import org.junit.Test;
import org.pinwheel.agility2.action.Action2;
import org.pinwheel.agility2.module.Downloader2;
import org.pinwheel.agility2.utils.FormatUtils;

import java.io.File;
import java.util.concurrent.CountDownLatch;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    private CountDownLatch countDownLatch = new CountDownLatch(1);

    private Downloader2 downloader;

    @Test
    public void startDownload() {
        final String url = "https://dldir1.qq.com/weixin/android/weixin672android1340.apk";
        System.out.println(url);
        final File file = new File("D:\\", url.substring(url.lastIndexOf("/")));
        downloader = new Downloader2()
                .fromUrl(url)
                .toFile(file)
                .threadSize(1)
                .onProcess(new Action2<Long, Long>() {
                    @Override
                    public void call(Long progress, Long contentLength) {
                        System.out.println(FormatUtils.simplifyFileSize(progress) + ", "
                                + Math.round(progress * 1.0 / contentLength * 100) + "%");
                    }
                })
                .onComplete(new Action2<File, Exception>() {
                    @Override
                    public void call(File file, Exception exp) {
                        System.out.println("file: " + file);
                        System.out.println("exp: " + exp.getMessage());
                        countDownLatch.countDown();
                    }
                })
                .start();

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void stopDownload() {
        if (null != downloader) {
            downloader.stop();
        }
    }
}