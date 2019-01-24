package org.pinwheel.agility2.compat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;

import java.io.File;

/**
 * Copyright (C), 2017 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 30/12/2017,14:19
 */
public final class SystemFunctions {

    private SystemFunctions() {
        throw new AssertionError();
    }

    public static void toGallery(Context context, String fileProviderAuthorities, File image) {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            uri = FileProvider.getUriForFile(context.getApplicationContext(), fileProviderAuthorities, image);
        } else {
            uri = Uri.fromFile(image);
        }
        intent.setDataAndType(uri, "image/*");
        if (null != intent.resolveActivity(context.getPackageManager())) {
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
        }
    }

    public static void installAPK(Context context, String fileProviderAuthorities, File apk) {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            uri = FileProvider.getUriForFile(context.getApplicationContext(), fileProviderAuthorities, apk);
        } else {
            uri = Uri.fromFile(apk);
        }
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        if (null != intent.resolveActivity(context.getPackageManager())) {
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
        }
    }

}
