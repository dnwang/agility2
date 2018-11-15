package org.pinwheel.demo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.pinwheel.agility2.action.Action2;
import org.pinwheel.agility2.module.Downloader2;
import org.pinwheel.agility2.utils.CommonTools;
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
    void tester4() {
        CommonTools.printTime("get activity", new Runnable() {
            @Override
            public void run() {
                final Context ctx = CommonTools.getTopActivity();
                if (null == ctx) {
                    LogUtils.d("--------> null");
                }
            }
        });
        CommonTools.printTime("starer", new Runnable() {
            @Override
            public void run() {
//                ActivityRouter.build(MainActivity.class).start();
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });
    }

    @Tester(title = "start download")
    void tester5() {
//        final String url = "https://dldir1.qq.com/weixin/android/weixin672android1340.apk";
        final String url = "http://119.29.149.68/download/shadowsocks--universal-4.5.1.apk";
        LogUtils.d(url);
        final File file = new File(Environment.getExternalStorageDirectory(), url.substring(url.lastIndexOf("/")));
        downloader = new Downloader2()
                .fromUrl(url)
                .toFile(file)
                .onProcess(new Action2<Long, Long>() {
                    @Override
                    public void call(Long progress, Long contentLength) {
                        LogUtils.d(FormatUtils.simplifyFileSize(progress) + ", "
                                + Math.round(progress * 1.0 / contentLength * 100) + "%");
                    }
                })
                .onComplete(new Action2<File, Exception>() {
                    @Override
                    public void call(File file, Exception exp) {
                        LogUtils.d("file: " + file);
                        if (null != exp) {
                            LogUtils.d("exp: " + exp.getMessage());
                        }
                    }
                })
                .start();
    }

    private Downloader2 downloader;

    @Tester(title = "stop download")
    void tester6() {
        if (null != downloader) {
            downloader.intercept();
        }
    }

    @Tester(title = "CellLayout")
    void tester7() {
        startActivity(new Intent(this, CellLayoutActivity.class));
    }

}
