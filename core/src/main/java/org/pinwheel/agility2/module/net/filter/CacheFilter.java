package org.pinwheel.agility2.module.net.filter;

import org.pinwheel.agility2.module.net.AbsApi;
import org.pinwheel.agility2.module.net.HttpRequest;
import org.pinwheel.agility2.module.net.HttpResponse;
import org.pinwheel.agility2.utils.CommonTools;
import org.pinwheel.agility2.utils.Converter;
import org.pinwheel.agility2.utils.FileUtils;
import org.pinwheel.agility2.utils.IOUtils;
import org.pinwheel.agility2.utils.LogUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Map;

/**
 * Copyright (C), 2017 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2017/11/07,9:35
 */
public final class CacheFilter extends Converter.Filter {

    private static final String TAG = CacheFilter.class.getSimpleName();

    private static final File PATH = new File(CommonTools.getApplication().getFilesDir(), "cache/api");

    @Override
    protected Object onInput(final Object obj, final Map<String, Object> args) {
        if (!(obj instanceof HttpRequest)) {
            return obj;
        }
        final AbsApi api = (AbsApi) args.get(ApiConvertFilter.KEY_REQUEST_OBJ);
        if (null == api) {
            return obj;
        }
        final String key = api.getCacheKey();
        if (api.getCacheTimeOut() <= 0 || CommonTools.isEmpty(key)) {
            return obj;
        }
        // find cache
        final File cacheFile = queryCache(key);
        if (null != cacheFile) {
            final boolean hasNetwork = CommonTools.isNetworkConnected(CommonTools.getApplication());
            if (!hasNetwork || cacheFile.lastModified() + api.getCacheTimeOut() > System.currentTimeMillis()) {
                try {
                    final Object cacheObj = IOUtils.stream2Object(new FileInputStream(cacheFile));
                    if (null != cacheObj && (cacheObj instanceof HttpResponse)) {
                        LogUtils.d(TAG, ">> find api cache, response now !");
                        setIntercept();
                        return cacheObj;
                    }
                } catch (Exception ignore) {
                    removeCache(cacheFile);
                }
            } else {
                removeCache(cacheFile);
            }
        }
        return obj;
    }

    @Override
    protected Object onOutput(final Object obj, final Map<String, Object> args) {
        if (!(obj instanceof HttpResponse)) {
            return obj;
        }
        final AbsApi api = (AbsApi) args.get(ApiConvertFilter.KEY_REQUEST_OBJ);
        if (null == api) {
            return obj;
        }
        final String key = api.getCacheKey();
        if (api.getCacheTimeOut() <= 0 || CommonTools.isEmpty(key)) {
            return obj;
        }
        // save cache
        HttpResponse response = (HttpResponse) obj;
        final int code = response.getCode();
        if (200 <= code && code < 300 && null != response.getBody()) {
            insertCache(key, (HttpResponse) obj);
        }
        return obj;
    }

    private static File queryCache(String key) {
        if (!CommonTools.isEmpty(key)) {
            File cache = new File(PATH, key);
            if (cache.exists()) {
                return cache;
            }
        }
        return null;
    }

    private static boolean insertCache(String key, HttpResponse response) {
        if (!CommonTools.isEmpty(key) && null != response) {
            File cache = new File(PATH, key);
            FileUtils.delete(cache);
            FileUtils.prepareDirs(cache);
            try {
                IOUtils.object2Stream(new FileOutputStream(cache), response);
                return true;
            } catch (Exception ignore) {
            }
        }
        return false;
    }

    private static void removeCache(File cache) {
        FileUtils.delete(cache);
    }
}