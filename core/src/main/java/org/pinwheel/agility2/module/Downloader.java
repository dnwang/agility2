package org.pinwheel.agility2.module;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import org.pinwheel.agility2.action.Action2;
import org.pinwheel.agility2.action.Action3;
import org.pinwheel.agility2.utils.CommonTools;
import org.pinwheel.agility2.utils.DigestUtils;
import org.pinwheel.agility2.utils.FileUtils;
import org.pinwheel.agility2.utils.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Copyright (C), 2015 <br>
 * <br>
 * All rights reserved
 *
 * @author dnwang
 */
public final class Downloader implements Runnable {

    private String fromUrl;
    private File toFile;

    private final Handler mainHandler;
    private Action2<Boolean, File> completeCallback;
    private Action3<Integer, Integer, Float> progressCallback;

    private boolean useCache = true;
    private volatile boolean isCanceled = false;

    public Downloader() {
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public Downloader fromUrl(String fromUrl) {
        if (!CommonTools.isEmpty(fromUrl)) {
            fromUrl = fromUrl.trim();
            final String resName = fromUrl.substring(Math.min(fromUrl.length() - 1, fromUrl.lastIndexOf("/") + 1));
            if (!CommonTools.isEmpty(resName)) {
                final String encodeResName = CommonTools.urlEncode(resName);
                fromUrl = fromUrl.replace(resName, encodeResName).replace("+", "%20");
            }
        }
        this.fromUrl = fromUrl;
        return this;
    }

    public Downloader toFile(final File toFile) {
        this.toFile = toFile;
        return this;
    }

    public Downloader onProcess(Action3<Integer, Integer, Float> callable) {
        progressCallback = callable;
        return this;
    }

    public Downloader onComplete(Action2<Boolean, File> callable) {
        completeCallback = callable;
        return this;
    }

    public Downloader cached(boolean is) {
        this.useCache = is;
        return this;
    }

    public void cancel() {
        isCanceled = true;
    }

    private void dividerProgress(final int progress, final int total) {
        if (isCanceled) {
            return;
        }
        final float percent = (0 != total) ? (progress * 1.0f / total) : 0;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (progressCallback != null) {
                    progressCallback.call(progress, total, percent);
                }
            }
        });
    }

    private void dividerError(Exception e) {
        if (isCanceled) {
            return;
        }
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (completeCallback != null) {
                    completeCallback.call(false, null);
                }
            }
        });
    }

    private void dividerSuccess() {
        if (isCanceled) {
            return;
        }
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (completeCallback != null) {
                    completeCallback.call(true, toFile);
                }
            }
        });
    }

    private String getFileKey() {
        if (!TextUtils.isEmpty(fromUrl)) {
            return DigestUtils.md5(fromUrl);
        } else {
            return null;
        }
    }

    private static final int TIME_OUT = 30 * 1000;

    private int progress;
    private int contentLength;

    @Override
    public void run() {
        isCanceled = false;
        if (!checkStatus()) {
            dividerError(new IllegalStateException("downloader params error"));
            return;
        }
        final String cacheKey = getFileKey();
        // use cache
        if (useCache) {
            final File cache = CacheManager.findCache(cacheKey);
            if (null != cache && cache.exists()) {
                contentLength = FileUtils.copy(cache, toFile);
                if (contentLength > 0) {
                    progress = contentLength;
                    dividerProgress(progress, contentLength);
                    dividerSuccess();
                    return;
                }
            }
        }
        final File newCache = CacheManager.newEmptyCache(cacheKey);
        HttpURLConnection conn = null;
        InputStream inStream = null;
        FileOutputStream outStream = null;
        try {
            outStream = new FileOutputStream(newCache);
            conn = (HttpURLConnection) new URL(fromUrl).openConnection();
            conn.setConnectTimeout(TIME_OUT);
            conn.setReadTimeout(TIME_OUT);
            conn.setDoInput(true);
            conn.connect();
            contentLength = conn.getContentLength();
            inStream = conn.getInputStream();
            byte[] buf = new byte[1024];
            int len;
            while ((len = inStream.read(buf)) > 0) {
                if (isCanceled) {
                    break;
                }
                outStream.write(buf, 0, len);
                progress += len;
                dividerProgress(progress, contentLength);
            }
            if (!isCanceled) {
                // mark complete
                CacheManager.setCacheComplete(cacheKey);
                FileUtils.copy(CacheManager.findCache(cacheKey), toFile);
            } else {
                CacheManager.removeCache(cacheKey);
            }
            dividerProgress(contentLength, contentLength);
            dividerSuccess();
        } catch (Exception e) {
            e.printStackTrace();
            dividerError(e);
        } finally {
            IOUtils.close(inStream);
            IOUtils.close(outStream);
            if (null != conn) {
                conn.disconnect();
            }
        }
    }

    private boolean checkStatus() {
        progress = 0;
        contentLength = 0;
        if (TextUtils.isEmpty(fromUrl) || null == toFile) {
            return false;
        }
        FileUtils.delete(toFile);
        FileUtils.prepareDirs(toFile);
        return true;
    }

    public Downloader execute() {
        AsyncHelper.INSTANCE.once(this);
        return this;
    }

    /**
     *
     */
    private static final class CacheManager {

        private static final File CACHE_PATH = new File(CommonTools.getApplication().getFilesDir(), "cache/downloader");

        static void removeCache(final String key) {
            if (TextUtils.isEmpty(key)) {
                return;
            }
            final File cache = new File(CACHE_PATH, key);
            if (cache.exists()) {
                FileUtils.delete(cache);
            }
            final File tmpCache = new File(CACHE_PATH, key + ".temp");
            if (tmpCache.exists()) {
                FileUtils.delete(tmpCache);
            }
        }

        static void clearCache() {
            AsyncHelper.INSTANCE.once(new Runnable() {
                @Override
                public void run() {
                    synchronized (CACHE_PATH) {
                        FileUtils.delete(CACHE_PATH);
                    }
                }
            });
        }

        static File findCache(final String key) {
            if (TextUtils.isEmpty(key)) {
                return null;
            }
            File cache = new File(CACHE_PATH, key);
            if (cache.exists()) {
                return cache;
            }
            return null;
        }

        static File newEmptyCache(final String key) {
            if (TextUtils.isEmpty(key)) {
                return null;
            }
            removeCache(key);
            final File tmpCache = new File(CACHE_PATH, key + ".temp");
            FileUtils.prepareDirs(tmpCache);
            return tmpCache;
        }

        static void setCacheComplete(final String key) {
            if (!TextUtils.isEmpty(key)) {
                File tmpCache = new File(CACHE_PATH, key + ".temp");
                tmpCache.renameTo(new File(tmpCache.getParent(), tmpCache.getName().replace(".temp", "")));
            }
        }

    }
}