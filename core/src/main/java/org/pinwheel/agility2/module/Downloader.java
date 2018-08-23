package org.pinwheel.agility2.module;

import org.pinwheel.agility2.action.Action2;
import org.pinwheel.agility2.action.Action3;
import org.pinwheel.agility2.utils.CommonTools;
import org.pinwheel.agility2.utils.FileUtils;
import org.pinwheel.agility2.utils.IOUtils;
import org.pinwheel.agility2.utils.LogUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
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
    private Action3<Long, Long, Float> progressCallback;

    private int threadSize = Runtime.getRuntime().availableProcessors();
    private volatile boolean intercept = true;

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

    public Downloader threadSize(int size) {
        threadSize = Math.max(1, size);
        return this;
    }

    @Deprecated
    public Downloader onProcess(Action3<Long, Long, Float> callable) {
        progressCallback = callable;
        return this;
    }

    public Downloader onComplete(Action2<Boolean, File> callable) {
        completeCallback = callable;
        return this;
    }

    public long getProgress() {
        return progress;
    }

    public long getContentLength() {
        return contentLength;
    }

    public boolean isDownloading() {
        return !intercept;
    }

    private File cfFile;

    public void stop() {
        intercept = true;
        // save config
        saveConfig();
    }

    private void saveConfig() {
        final ConfigBundles args = new ConfigBundles();
        args.optionsList = new ArrayList<>(workers.size());
        for (Worker worker : workers) {
            args.optionsList.add(worker.getOptions());
        }
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
                    LogUtils.d(TAG, "update download config: " + cfFile);
                } catch (FileNotFoundException ignore) {
                }
            }
        });
    }

    private void clearConfig() {
        AsyncHelper.INSTANCE.once(new Runnable() {
            @Override
            public void run() {
                FileUtils.delete(cfFile);
            }
        });
    }

    private long progress, contentLength, lastModifyTime;

    public Downloader start() {
        intercept = false;
        if (!checkStatus()) {
            dividerError(new IllegalStateException("downloader params error"));
            return this;
        }
        final OnContentUpdate updateAction = new OnContentUpdate() {
            @Override
            public void call(Worker worker, long dLen) {
                if (isDownloading()) {
                    progress += dLen;
                }
            }
        };
        final OnConnectionEnd onConnectionEnd = new OnConnectionEnd() {
            @Override
            public synchronized void call(Worker worker, boolean isSuccess) {
                if (isSuccess) {
                    workers.remove(worker);
                    if (workers.isEmpty()) {
                        clearConfig();
                        dividerSuccess();
                    }
                } else if (isDownloading()) {
                    intercept = true;
                    saveConfig();
                    workers.clear();
                    dividerError(null);
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
                final ConfigBundles history = verifyConfigFile();
                if (null != history
                        // two file have the same url
                        && contentLength == history.contentLength
                        && lastModifyTime == history.lastModifyTime) {
                    // init workers
                    workers = new CopyOnWriteArrayList<>(new ArrayList<Worker>(history.optionsList.size()));
                    for (WorkerOptions options : history.optionsList) {
                        workers.add(new Worker(options));
                        progress += options.contentLength;
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
                }
                // start workers
                for (Worker worker : workers) {
                    worker.execute(updateAction, onConnectionEnd);
                }
            }
        });
        return this;
    }

    @Deprecated
    private void dividerProgress(final long progress, final long total) {
        final float percent = (0 != total) ? (progress * 1.0f / total) : 0;
        if (progressCallback != null) {
            progressCallback.call(progress, total, percent);
        }
    }

    private void dividerError(Exception e) {
        if (completeCallback != null) {
            completeCallback.call(false, null);
        }
    }

    private void dividerSuccess() {
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

    private ConfigBundles verifyConfigFile() {
        Object obj = null;
        try {
            obj = IOUtils.stream2Object(new FileInputStream(cfFile));
        } catch (FileNotFoundException ignore) {
            LogUtils.d(TAG, "can't find download config: " + cfFile);
        }
        if (null != obj) {
            ConfigBundles args = (ConfigBundles) obj;
            if (fromUrl.equals(args.fromUrl)
                    && toFile.equals(args.toFile)
                    && !CommonTools.isEmpty(args.optionsList)) {
                FileUtils.delete(cfFile);
                return args;
            }
        }
        return null;
    }

    private final static class ConfigBundles implements Serializable {
        String fromUrl;
        File toFile;
        ArrayList<WorkerOptions> optionsList;

        long contentLength;
        long lastModifyTime;
    }

    private final static class WorkerOptions implements Serializable {
        long begin;
        long end;
        long contentLength = 0;
    }

    private interface OnContentUpdate {
        void call(Worker worker, long contentLength);
    }

    private interface OnConnectionEnd {
        void call(Worker worker, boolean isSuccess);
    }

    private final class Worker {
        private final static int BUF_SIZE = 1024;
        private final static int RETRY_MAX = 3;

        long begin;
        long end;
        long contentLength;

        Worker(long begin, long end) {
            this.begin = begin;
            this.end = end;
            this.contentLength = 0;
        }

        Worker(WorkerOptions options) {
            this.begin = options.begin;
            this.end = options.end;
            this.contentLength = options.contentLength;
        }

        WorkerOptions getOptions() {
            final WorkerOptions options = new WorkerOptions();
            options.begin = begin;
            options.end = end;
            options.contentLength = contentLength;
            return options;
        }

        void execute(final OnContentUpdate onContentUpdate, final OnConnectionEnd onConnectionEnd) {
            final Runnable downloadAction = new Runnable() {
                boolean stopFlag = false;
                boolean retryFlag = false;
                int retryCount = 0;

                @Override
                public void run() {
                    retryFlag = false;

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
                        while ((len = inStream.read(buf)) > 0) {
                            outStream.write(buf, 0, len);
                            contentLength += len;
                            onContentUpdate.call(Worker.this, len);
                            if (intercept) {
                                throw new RuntimeException("call stop(), worker intercept");
                            }
                        }
                    } catch (SocketTimeoutException e) {
                        retryFlag = true;
                    } catch (Exception e) {
                        LogUtils.e(TAG, "worker[" + Worker.this + "] exp: " + e.getMessage());
                        stopFlag = true;
                    } finally {
                        IOUtils.close(inStream);
                        IOUtils.close(outStream);
                        if (null != conn) {
                            conn.disconnect();
                        }
                        if (!retryFlag) {
                            onConnectionEnd.call(Worker.this, !stopFlag);
                        } else {
                            if (retryCount++ < RETRY_MAX) {
                                LogUtils.e(TAG, "worker[" + Worker.this + "] timeOut, retry: " + retryCount);
                                run();
                            } else {
                                onConnectionEnd.call(Worker.this, false);
                            }
                        }
                    }
                }
            };
            AsyncHelper.INSTANCE.once(downloadAction);
        }
    }

}