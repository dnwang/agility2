package org.pinwheel.demo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.pinwheel.agility2.action.Action0;
import org.pinwheel.agility2.utils.CommonTools;
import org.pinwheel.agility2.utils.LogUtils;

public final class MainActivity extends AbsTesterActivity {

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
        LogUtils.setEnable(true);
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

}
