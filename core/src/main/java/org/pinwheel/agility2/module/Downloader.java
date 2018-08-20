package org.pinwheel.agility2.module;

import org.pinwheel.agility2.action.Action2;
import org.pinwheel.agility2.action.Action3;
import org.pinwheel.agility2.utils.CommonTools;
import org.pinwheel.agility2.utils.FileUtils;
import org.pinwheel.agility2.utils.FormatUtils;
import org.pinwheel.agility2.utils.IOUtils;
import org.pinwheel.agility2.utils.LogUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Copyright (C), 2015 <br>
 * <br>
 * All rights reserved
 *
 * @author dnwang
 */
public final class Downloader {

    private static final String TAG = Downloader.class.getSimpleName();

    private static final int TIME_OUT = 30 * 1000;

    private String fromUrl;
    private File toFile;

    private Action2<Boolean, File> completeCallback;
    private Action3<Integer, Integer, Float> progressCallback;

    private int threadSize = Runtime.getRuntime().availableProcessors();
    private volatile boolean intercept = false;

    private List<Worker> workers = null;

    public Downloader() {
    }

    public Downloader fromUrl(String fromUrl) {
        this.fromUrl = fromUrl;
        return this;
    }

    public Downloader toFile(final File toFile) {
        this.toFile = toFile;
        if (null != toFile) {
            this.cfFile = new File(toFile.getAbsolutePath() + ".cf");
        }
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

    public Downloader threadSize(int size) {
        threadSize = Math.max(1, size);
        return this;
    }

    private File cfFile;

    public void stop() {
        intercept = true;
        // save config
        final ConfigBundle args = new ConfigBundle();
        args.workers = new ArrayList<>(workers.size());
        args.contentLength = contentLength;
        args.lastModifyTime = lastModifyTime;
        args.fromUrl = fromUrl;
        args.toFile = toFile;
        AsyncHelper.INSTANCE.once(new Runnable() {
            @Override
            public void run() {
                FileUtils.delete(cfFile);
                FileUtils.prepareDirs(cfFile);
                try {
                    IOUtils.object2Stream(new FileOutputStream(cfFile), args);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private long contentLength, lastModifyTime;

    public Downloader start() {
        intercept = false;
        if (!checkStatus()) {
            dividerError(new IllegalStateException("downloader params error"));
            return this;
        }
        final OnContentUpdate updateAction = new OnContentUpdate() {
            @Override
            public void call(Worker worker, long contentLength) {
                long sum = 0;
                for (Worker w : workers) {
                    sum += w.contentLength;
                }
                LogUtils.d(TAG, sum + ", " + FormatUtils.simplifyFileSize(sum));
            }
        };
        final OnConnectionEnd onConnectionEnd = new OnConnectionEnd() {
            @Override
            public void call(Worker worker, boolean isSuccess) {
                if (isSuccess) {
                    workers.remove(worker);
                    if (workers.isEmpty()) {
                        dividerSuccess();
                    }
                } else {
                    dividerError(null);
                    stop();
                    workers.clear();
                }
            }
        };
        AsyncHelper.INSTANCE.once(new Runnable() {
            @Override
            public void run() {
                Exception connectionExp = null;
                HttpURLConnection conn = null;
                RandomAccessFile file = null;
                try {
                    conn = (HttpURLConnection) new URL(fromUrl).openConnection();
                    conn.setRequestProperty("Accept-Encoding", "identity");
                    contentLength = conn.getContentLength();
                    lastModifyTime = conn.getLastModified();
                    file = new RandomAccessFile(toFile, "rw");
                    file.setLength(contentLength);
                } catch (Exception e) {
                    connectionExp = e;
                } finally {
                    IOUtils.close(file);
                    if (null != conn) {
                        conn.disconnect();
                    }
                }
                if (null != connectionExp) {
                    dividerError(connectionExp);
                    return;
                }
                final ConfigBundle history = verifyConfigFile();
                if (null != history
                        // two file have the same url
                        && contentLength == history.contentLength
                        && lastModifyTime == history.lastModifyTime) {
                    // init workers
                    workers = new CopyOnWriteArrayList<>(new ArrayList<Worker>(history.workers.size()));
                    for (Worker worker : history.workers) {
                        workers.add(worker);
                        worker.execute(updateAction, onConnectionEnd);
                    }
                } else {
                    FileUtils.delete(cfFile);
                    // init workers
                    workers = new CopyOnWriteArrayList<>(new ArrayList<Worker>(threadSize));
                    //
                    if (contentLength < 2 * 2014 * 1024 // 2M
                            || 1 == threadSize) {
                        // single thread
                        workers.add(new Worker(0, -1));
                    } else {
                        long lastEnd = 0;
                        for (int i = 0; i < threadSize; i++) {
                            long end = contentLength / threadSize * (i + 1);
                            workers.add(new Worker(lastEnd, end));
                            lastEnd = end + 1;
                        }
                        // fix last worker end value
                        workers.get(workers.size() - 1).end = contentLength;
                    }
                    for (Worker worker : workers) {
                        worker.execute(updateAction, onConnectionEnd);
                    }
                }
            }
        });
        return this;
    }

    private void dividerProgress(final int progress, final int total) {
        if (intercept) {
            return;
        }
        final float percent = (0 != total) ? (progress * 1.0f / total) : 0;
        if (progressCallback != null) {
            progressCallback.call(progress, total, percent);
        }
    }

    private void dividerError(Exception e) {
        if (intercept) {
            return;
        }
        if (completeCallback != null) {
            completeCallback.call(false, null);
        }
    }

    private void dividerSuccess() {
        if (intercept) {
            return;
        }
        if (completeCallback != null) {
            completeCallback.call(true, toFile);
        }
    }

    private boolean checkStatus() {
        if (CommonTools.isEmpty(fromUrl) || null == toFile) {
            return false;
        }
        FileUtils.delete(toFile);
        FileUtils.prepareDirs(toFile);
        return true;
    }

    private ConfigBundle verifyConfigFile() {
        Object obj = null;
        try {
            obj = IOUtils.stream2Object(new FileInputStream(cfFile));
        } catch (Exception ignore) {
            LogUtils.d("no history, " + cfFile.getAbsolutePath());
        }
        if (null != obj) {
            ConfigBundle args = (ConfigBundle) obj;
            if (fromUrl.equals(args.fromUrl)
                    && toFile.equals(args.toFile)
                    && !CommonTools.isEmpty(args.workers)) {
                FileUtils.delete(cfFile);
                return args;
            }
        }
        return null;
    }

    private static class ConfigBundle implements Serializable {
        String fromUrl;
        File toFile;
        List<Worker> workers;

        long contentLength;
        long lastModifyTime;
    }

    private interface OnContentUpdate {
        void call(Worker worker, long contentLength);
    }

    private interface OnConnectionEnd {
        void call(Worker worker, boolean isSuccess);
    }

    private final class Worker implements Serializable {

        private final static int BUF_SIZE = 1024;

        long begin;
        long end;
        long contentLength = 0;

        Worker(long begin, long end) {
            this.begin = begin;
            this.end = end;
        }

        void execute(final OnContentUpdate onContentUpdate, final OnConnectionEnd onConnectionEnd) {
            final Runnable downloadAction = new Runnable() {
                @Override
                public void run() {
                    boolean result = true;
                    HttpURLConnection conn = null;
                    InputStream inStream = null;
                    RandomAccessFile outStream = null;
                    try {
                        if (begin > end && end >= 0) {
                            throw new IllegalStateException("worker download range error! [" + begin + ", " + end + "]");
                        }
                        final long beginOffset = begin + contentLength;
                        outStream = new RandomAccessFile(toFile, "rws");
                        conn = (HttpURLConnection) new URL(fromUrl).openConnection();
                        conn.setConnectTimeout(TIME_OUT);
                        conn.setReadTimeout(TIME_OUT);
                        conn.setRequestProperty("Accept-Encoding", "identity");
                        conn.setRequestProperty("Range", "bytes=" + beginOffset + "-" + (end > 0 ? end : ""));
                        conn.connect();
                        inStream = conn.getInputStream();
                        outStream.seek(beginOffset);
                        byte[] buf = new byte[BUF_SIZE];
                        int len;
                        while (!intercept && (len = inStream.read(buf)) > 0) {
                            outStream.write(buf, 0, len);
                            contentLength += len;
                            onContentUpdate.call(Worker.this, contentLength);
                        }
                        result = !intercept;
                    } catch (Exception e) {
                        e.printStackTrace();
                        result = false;
                    } finally {
                        IOUtils.close(inStream);
                        IOUtils.close(outStream);
                        if (null != conn) {
                            conn.disconnect();
                        }
                        onConnectionEnd.call(Worker.this, result);
                    }
                }
            };
            AsyncHelper.INSTANCE.once(downloadAction);
        }
    }

}