package org.pinwheel.agility2.module;

import org.pinwheel.agility2.action.Action1;
import org.pinwheel.agility2.action.Action2;
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
public final class Downloader2 {

    private static final String TAG = Downloader2.class.getSimpleName();

    private static final int TIME_OUT = 30 * 1000;

    private String fromUrl;
    private File toFile;

    private Action1<File> completeCallback;
    private Action2<Long, Long> progressCallback;

    private int threadSize = Runtime.getRuntime().availableProcessors();
    private volatile boolean intercept = true;

    private List<Worker> workers = null;

    public Downloader2() {
    }

    public Downloader2 fromUrl(String fromUrl) {
        this.fromUrl = fromUrl;
        return this;
    }

    public Downloader2 toFile(final File toFile) {
        this.toFile = toFile;
        if (null != toFile) {
            this.tmpFile = new File(toFile.getAbsolutePath() + ".tmp");
            this.cfFile = new File(tmpFile.getAbsolutePath() + ".cf");
        }
        return this;
    }

    public Downloader2 threadSize(int size) {
        threadSize = Math.max(1, size);
        return this;
    }

    @Deprecated
    public Downloader2 onProcess(Action2<Long, Long> callable) {
        progressCallback = callable;
        return this;
    }

    public Downloader2 notifyWorkerComplete(Action1<File> callable) {
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

    private boolean checkStatus() {
        if (CommonTools.isEmpty(fromUrl)
                || null == toFile
                || toFile.exists()) {
            return false;
        }
        FileUtils.prepareDirs(tmpFile);
        return true;
    }

    private File tmpFile, cfFile;

    private void saveInterceptParams() {
        final InterceptParams args = new InterceptParams();
        args.optionsList = new ArrayList<>(workers.size());
        for (Worker worker : workers) {
            args.optionsList.add(worker.getOptions());
        }
        args.contentLength = contentLength;
        args.lastModifyTime = lastModifyTime;
        args.fromUrl = fromUrl;
        args.tmpFile = tmpFile;
        FileUtils.delete(cfFile);
        FileUtils.prepareDirs(cfFile);
        try {
            IOUtils.object2Stream(new FileOutputStream(cfFile), args);
            LogUtils.d(TAG, "saveInterceptParams: " + cfFile);
        } catch (FileNotFoundException e) {
            LogUtils.e(TAG, "saveInterceptParams: " + e.getMessage());
        }
    }

    private InterceptParams getInterceptParams() {
        Object obj = null;
        try {
            obj = IOUtils.stream2Object(new FileInputStream(cfFile));
        } catch (FileNotFoundException e) {
            LogUtils.e(TAG, "getInterceptParams: " + e.getMessage());
        }
        if (null != obj) {
            InterceptParams args = (InterceptParams) obj;
            if (tmpFile.exists()
                    && fromUrl.equals(args.fromUrl)
                    && tmpFile.equals(args.tmpFile)
                    && !CommonTools.isEmpty(args.optionsList)) {
                return args;
            } else {
                FileUtils.delete(cfFile);
            }
        }
        return null;
    }

    private void notifyWorkerContentLengthChanged(Worker worker, long bufLen) {
        if (!isDownloading()) {
            return;
        }
        progress += bufLen;
    }

    private synchronized void notifyWorkerComplete(Worker worker) {
        if (!isDownloading()) {
            return;
        }
        if (worker.isComplete()) {
            workers.remove(worker);
            if (workers.isEmpty()) {
                intercept = true;
                FileUtils.delete(cfFile);
                FileUtils.delete(toFile);
                if (tmpFile.renameTo(toFile)) {
                    dividerSuccess();
                } else {
                    dividerError(new IllegalStateException("'tmpFile' can't transfer to 'outFile'!"));
                }
            }
        } else {
            intercept = true;
            saveInterceptParams();
            dividerError(new InterruptedException("download worker interrupted, maybe connection error!"));
        }
    }

    public void stop() {
        intercept = true;
        // save config
        saveInterceptParams();
    }

    private long progress, contentLength, lastModifyTime;

    public Downloader2 start() {
        if (!checkStatus()) {
            intercept = true;
            dividerError(new IllegalStateException("downloader checkStatus error"));
            return this;
        }
        intercept = false;
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
                    file = new RandomAccessFile(tmpFile, "rws");
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
                    intercept = true;
                    dividerError(connectionExp);
                    return;
                }
                final InterceptParams history = getInterceptParams();
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
                    FileUtils.delete(tmpFile);
                    FileUtils.delete(cfFile);
                    // init workers
                    workers = new CopyOnWriteArrayList<>(new ArrayList<Worker>(threadSize));
                    //
                    if (contentLength < 2 * 2014 * 1024 // use single thread when size less than 2M
                            || 1 == threadSize) {
                        // single thread
                        workers.add(new Worker(1, 0, contentLength));
                    } else {
                        long lastEnd = 0;
                        for (int i = 0; i < threadSize; i++) {
                            long end;
                            if (threadSize - 1 == i) {
                                // fix last worker end value
                                end = contentLength;
                            } else {
                                end = contentLength / threadSize * (i + 1);
                            }
                            workers.add(new Worker(i, lastEnd, end));
                            lastEnd = end + 1;
                        }
                    }
                }
                // start workers
                for (Worker worker : workers) {
                    AsyncHelper.INSTANCE.once(worker);
                }
            }
        });
        return this;
    }

    @Deprecated
    private void dividerProgress(final long progress, final long total) {
        if (progressCallback != null) {
            progressCallback.call(progress, total);
        }
    }

    private void dividerError(Exception e) {
        if (completeCallback != null) {
            completeCallback.call(null);
        }
    }

    private void dividerSuccess() {
        if (completeCallback != null) {
            completeCallback.call(toFile);
        }
    }

    private final static class InterceptParams implements Serializable {
        String fromUrl;
        File tmpFile;
        ArrayList<WorkerOptions> optionsList;

        long contentLength;
        long lastModifyTime;
    }

    private final static class WorkerOptions implements Serializable {
        String tag;
        long begin;
        long end;
        long contentLength = 0;
    }

    private final class Worker implements Runnable {
        private final static int BUF_SIZE = 1024;
        private final static int RETRY_MAX = 3;

        final long begin;
        final long end;
        long contentLength;

        final String tag;

        Worker(Object tag, long begin, long end) {
            this.tag = String.valueOf(tag);
            this.begin = begin;
            this.end = end;
            this.contentLength = 0;
            this.retryCount = 0;
        }

        Worker(WorkerOptions options) {
            this(options.tag, options.begin, options.end);
            this.contentLength = options.contentLength;
        }

        WorkerOptions getOptions() {
            final WorkerOptions options = new WorkerOptions();
            options.tag = tag;
            options.begin = begin;
            options.end = end;
            options.contentLength = contentLength;
            return options;
        }

        boolean isComplete() {
            return (end - begin) == contentLength;
        }

        int retryCount;

        @Override
        public void run() {
            boolean retryFlag = false;
            // open connection
            HttpURLConnection conn = null;
            InputStream inStream = null;
            RandomAccessFile outStream = null;
            try {
                if (begin > end && end >= 0) {
                    throw new IllegalStateException("worker download range error! [" + begin + ", " + end + "]");
                }
                final long beginOffset = begin + contentLength;
                outStream = new RandomAccessFile(tmpFile, "rws");
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
                    notifyWorkerContentLengthChanged(Worker.this, len);
                    if (intercept) {
                        break;
                    }
                }
            } catch (SocketTimeoutException e) {
                // flag retry
                retryFlag = true;
            } catch (Exception e) {
                LogUtils.e(TAG, "worker[" + tag + "] exp: " + e.getMessage());
            } finally {
                IOUtils.close(inStream);
                IOUtils.close(outStream);
                if (null != conn) {
                    conn.disconnect();
                }
                // notify complete or retry
                if (!retryFlag) {
                    notifyWorkerComplete(Worker.this);
                } else {
                    if (retryCount++ < RETRY_MAX) {
                        LogUtils.e(TAG, "worker[" + tag + "] timeOut, retry: " + retryCount);
                        run();
                    } else {
                        notifyWorkerComplete(Worker.this);
                    }
                }
            }
        }
    }

}