package org.pinwheel.demo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import org.pinwheel.agility2.action.Action0;
import org.pinwheel.agility2.action.Action2;
import org.pinwheel.agility2.module.Downloader2;
import org.pinwheel.agility2.utils.CommonTools;
import org.pinwheel.agility2.utils.FileUtils;
import org.pinwheel.agility2.utils.FormatUtils;
import org.pinwheel.agility2.utils.LogUtils;

import java.io.File;

public final class MainActivity extends AbsTesterActivity {

    static {
        LogUtils.setEnable(true);
    }

    private static final String FILE_AUTHORITIES = "org.pinwheel.agility2.fileprovider";

    @Tester(title = "finish all activities")
    void tester() {
        CommonTools.finishAllActivities();
    }

    @Tester(title = "tint")
    void tester2() {
        final ImageView image = new ImageView(this);
        image.setImageDrawable(CommonTools.tint(R.mipmap.ic_launcher, Color.LTGRAY));
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((ViewGroup) view.getParent()).removeView(view);
            }
        });
        final FrameLayout container = (FrameLayout) getWindow().getDecorView();
        container.addView(image, -1, -1);
    }

    @Tester(title = "start activity")
    void test3() {
        CommonTools.printTime("get activity", new Action0() {
            @Override
            public void call() {
                final Context ctx = CommonTools.getTopActivity();
                if (null == ctx) {
                    LogUtils.d("--------> null");
                }
            }
        });
        CommonTools.printTime("starer", new Action0() {
            @Override
            public void call() {
//                ActivityRouter.build(MainActivity.class).start();
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });
    }

    @Tester(title = "Start download")
    void testDownloader() {
        final File file = new File(Environment.getExternalStorageDirectory(), "Android1_0_0.pdf");
        FileUtils.delete(file);
        downloader = new Downloader2()
//                .fromUrl("https://dldir1.qq.com/weixin/android/weixin672android1340.apk")
                .fromUrl("http://47.88.52.115:8082/files/Android1_0_0.pdf")
                .toFile(file)
                .notifyWorkerComplete(new Action2<Boolean, File>() {
                    @Override
                    public void call(Boolean obj0, File obj1) {
                        Toast.makeText(MainActivity.this, String.valueOf(obj0), Toast.LENGTH_SHORT).show();
                        LogUtils.d("status: " + obj0 + ", file: " + obj1);
                        mainHandler.removeCallbacks(progressUpdater);
                    }
                })
                .start();
        mainHandler.removeCallbacks(progressUpdater);
        mainHandler.post(progressUpdater);
    }

    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable progressUpdater = new Runnable() {
        long lastProgress = 0;

        @Override
        public void run() {
            if (null != downloader) {
                long progress = downloader.getProgress();
                long contentLength = downloader.getContentLength();
                LogUtils.d(FormatUtils.simplifyFileSize(progress) + ", "
                        + Math.round(progress * 1.0 / contentLength * 100) + "%, "
                        + (progress - lastProgress) / 1000 + "kb/s");
                if (downloader.isDownloading()) {
                    mainHandler.postDelayed(this, 1000);
                }
                lastProgress = progress;
            }
        }
    };

    private Downloader2 downloader = null;

    @Tester(title = "Stop download")
    void testDownloaderStop() {
        if (null != downloader) {
            downloader.stop();
        }
        mainHandler.removeCallbacks(progressUpdater);
    }

}
