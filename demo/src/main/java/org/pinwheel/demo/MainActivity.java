package org.pinwheel.demo;

import org.pinwheel.agility2.module.AsyncHelper;
import org.pinwheel.agility2.utils.CommonTools;

public final class MainActivity extends AbsTesterActivity {

    private static final String FILE_AUTHORITIES = "org.pinwheel.agility2.fileprovider";

    @Tester(title = "finish all activities")
    void tester() {
        CommonTools.finishAllActivities();
    }

    @Tester(title = "delay")
    void tester2() {
        AsyncHelper.INSTANCE.delay(2000, new Runnable() {
            @Override
            public void run() {
                System.out.println("delay complete !!");
            }
        });
    }

}
