package org.pinwheel.demo;

import android.app.Activity;
import android.app.Application;

import org.pinwheel.agility2.utils.CommonTools;
import org.pinwheel.agility2.utils.LogUtils;

public final class MainActivity extends AbsTesterActivity {

    private static final String FILE_AUTHORITIES = "org.pinwheel.agility2.fileprovider";

    @Tester(title = "tester")
    void tester() {
        // TODO: 30/12/2017
        Application app = CommonTools.getApplication();
        Activity activity = CommonTools.getTopActivity();

        LogUtils.d(app);
        LogUtils.d("---------------------");
        LogUtils.d(activity);
    }

}
