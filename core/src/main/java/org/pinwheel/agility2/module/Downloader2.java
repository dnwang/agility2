package org.pinwheel.agility2.module;

import org.pinwheel.agility2.action.Action2;
import org.pinwheel.agility2.utils.CommonTools;
import org.pinwheel.agility2.utils.FileUtils;
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
 * Copyright (C), 2018 <br>
 * <br>
 * All rights reserved
 *
 * @author dnwang
 */
public final class Downloader2 {
    private static final String TAG = "Downloader2";

    private static File getCfFile(File file) {
        return new File(file.getAbsolutePath() + ".cf");
    }

    private static File getDataFile(File file) {
        return new File(file.getAbsolutePath() + ".data");
    }

    private static final int TIME_OUT = 30 * 1000;
    private static final int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();

    private String url;
    private File file;
    private int threadSize;

    private Action2<File, Exception> completeAction;
    private Action2<Long, Long> progressAction;

    public Downloader2() {
        this.threadSize = NUMBER_OF_CORES;
    }

    public Downloader2 fromUrl(String fromUrl) {
        this.url = fromUrl;
        return this;
    }

    public Downloader2 toFile(final File toFile) {
        this.file = toFile;
        return this;
    }

    public Downloader2 threadSize(int size) {
        threadSize = Math.min(Math.max(1, size), NUMBER_OF_CORES * 2);
        return this;
    }

    public Downloader2 onProcess(Action2<Long, Long> action) {
        progressAction = action;
        return this;
    }

    public Downloader2 onComplete(Action2<File, Exception> action) {
        completeAction = action;
        return this;
    }

    public void intercept() {
        if (!isIntercepted()) {
            for (Worker worker : workers) {
                worker.stop();
            }
            ResourceInfo.save(res, getCfFile(file));
        }
    }

    public boolean isIntercepted() {
        return CommonTools.isEmpty(workers);
    }

    public boolean isComplete() {
        if (null == res || CommonTools.isEmpty(res.blocks)) {
            return false;
        }
        for (Block block : res.blocks) {
            if (!block.isAtEnd()) {
                return false;
            }
        }
        return true;
    }

    private ResourceInfo res;
    private List<Worker> workers;

    public Downloader2 start() {
        if (!isIntercepted()) {
            dividerError(new IllegalStateException("downloader already started, please call intercept method first!"));
            return this;
        }
        if (CommonTools.isEmpty(url)) {
            dividerError(new IllegalStateException("download url error!"));
            return this;
        }
        if (null == file || file.exists()) {
            dividerError(new IllegalStateException("download target file is null or it already exists!"));
            return this;
        }
        AsyncHelper.INSTANCE.once(new Runnable() {
            @Override
            public void run() {
                FileUtils.prepareDirs(file);
                LogUtils.d(TAG, "preparing... (connect remote resource)");
                final ResourceInfo historyRes = ResourceInfo.load(getCfFile(file));
                final ResourceInfo remoteRes = ResourceInfo.load(url);
                // compare info
                if (null == remoteRes) {
                    dividerError(new IllegalStateException("can't access remote resource!"));
                } else {
                    res = remoteRes.equals(historyRes) ? historyRes : remoteRes;
                    allocateWorker();
                }
            }
        });
        return this;
    }

    private void allocateWorker() {
        if (CommonTools.isEmpty(res.blocks)) {
            res.blocks = new ArrayList<>(threadSize);
            if (res.contentLength < 2 * 1014 * 1024 // use single thread when size less than 2M
                    || 1 == threadSize) {
                // single thread
                res.blocks.add(new Block(0, res.contentLength));
            } else {
                final long u = res.contentLength / threadSize;
                long begin = 0, end;
                for (int i = 0; i < threadSize; i++) {
                    if (threadSize - 1 == i) {
                        // fix last worker end value
                        end = res.contentLength;
                    } else {
                        end = u * (i + 1);
                    }
                    res.blocks.add(new Block(begin, end));
                    begin = end + 1;
                }
            }
        }
        // init workers
        if (workers == null) {
            workers = new CopyOnWriteArrayList<>();
        }
        for (Block block : res.blocks) {
            if (!block.isAtEnd()) {
                Worker worker = new Worker(block);
                workers.add(worker);
                // start worker
                AsyncHelper.INSTANCE.once(worker);
            }
        }
    }

    private boolean transfer() {
        FileUtils.delete(file);
        final File dataFile = getDataFile(file);
        final boolean result = dataFile.exists() && dataFile.renameTo(file);
        if (result) {
            FileUtils.delete(getCfFile(file));
        }
        return result;
    }

    private void onWorkerEnd(Worker worker) {
        if (null != worker) {
            workers.remove(worker);
        }
        if (isIntercepted()) {
            if (isComplete() && transfer()) {
                dividerSuccess();
            } else {
                ResourceInfo.save(res, getCfFile(file));
                dividerError(new IllegalStateException("download intercepted!"));
            }
        }
    }

    private synchronized void onProgressChanged() {
        long progress = 0;
        for (Block block : res.blocks) {
            progress += block.getProgress();
        }
        if (progressAction != null) {
            progressAction.call(progress, res.contentLength);
        }
    }

    private void dividerError(Exception e) {
        if (completeAction != null) {
            completeAction.call(null, e);
        }
    }

    private void dividerSuccess() {
        if (completeAction != null) {
            completeAction.call(file, null);
        }
    }

    private final static class ResourceInfo implements Serializable {
        final String url;
        final long contentLength;
        final long lastModifyTime;

        List<Block> blocks = null;

        private ResourceInfo(String url, long contentLength, long lastModifyTime) {
            this.url = url;
            this.contentLength = contentLength;
            this.lastModifyTime = lastModifyTime;
        }

        static void save(ResourceInfo res, File cfFile) {
            FileUtils.delete(cfFile);
            FileUtils.prepareDirs(cfFile);
            try {
                IOUtils.object2Stream(new FileOutputStream(cfFile), res);
            } catch (Exception e) {
                LogUtils.e(TAG, "can't save resource! " + e.getMessage());
            }
        }

        static ResourceInfo load(String url) {
            long contentLength = -1, lastModifyTime = -1;
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestProperty("Accept-Encoding", "identity");
                conn.setConnectTimeout(TIME_OUT);
                conn.setReadTimeout(TIME_OUT / 2);
                conn.setDoOutput(false);
                conn.setDoInput(false);
                contentLength = conn.getContentLength();
                lastModifyTime = conn.getLastModified();
            } catch (Exception e) {
                LogUtils.e(TAG, "can't access remote resource! " + e.getMessage());
            } finally {
                if (null != conn) {
                    conn.disconnect();
                }
            }
            if (-1 == contentLength || -1 == lastModifyTime) {
                return null;
            } else {
                return new ResourceInfo(url, contentLength, lastModifyTime);
            }
        }

        static ResourceInfo load(File cfFile) {
            if (!cfFile.exists()) {
                return null;
            }
            try {
                return (ResourceInfo) IOUtils.stream2Object(new FileInputStream(cfFile));
            } catch (Exception e) {
                LogUtils.e(TAG, "can't load history! " + e.getMessage());
                return null;
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (null == obj || getClass() != obj.getClass()) {
                return false;
            } else {
                final ResourceInfo that = (ResourceInfo) obj;
                return that.url.equals(url)
                        && that.contentLength == contentLength
                        && that.lastModifyTime == lastModifyTime;
            }
        }
    }

    private final static class Block implements Serializable {
        static final long FLAG_END = -999;

        final long begin, end;
        long offset;

        Block(long begin, long end) {
            this.begin = begin;
            this.end = end;
            this.offset = begin;
        }

        boolean isAtEnd() {
            return FLAG_END == offset;
        }

        long getProgress() {
            return isAtEnd() ? (end - begin + 1) : (offset - begin + 1);
        }
    }

    private static int workerId = 0;

    private final class Worker implements Runnable {
        private final static int BUF_SIZE = 1024;
        private final static int MAX_RETRY = 3;

        final static int FLAG_INTERCEPT = -1;
        final static int FLAG_COMPLETE = -2;

        private final int id;
        private final Block block;

        private int flag;

        Worker(Block block) {
            this.block = block;
            this.id = workerId++;
            this.flag = 0;
        }

        void stop() {
            flag = FLAG_INTERCEPT; // mark intercept
            close();
        }

        private void close() {
            IOUtils.close(accessFile);
            IOUtils.close(inStream);
            if (null != conn) {
                conn.disconnect();
            }
        }

        private RandomAccessFile accessFile = null;
        private HttpURLConnection conn = null;
        private InputStream inStream = null;

        private void getConnectionData() throws Exception {
            LogUtils.d(TAG, "worker #" + id + ": open connection, times:" + flag);
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(TIME_OUT);
            conn.setReadTimeout(TIME_OUT / 2);
            conn.setRequestProperty("Accept-Encoding", "identity");
            conn.setRequestProperty("Range", "bytes=" +
                    block.offset + "-" + (Long.MAX_VALUE == block.end ? "" : block.end));
            conn.connect();
            inStream = conn.getInputStream();
            accessFile = new RandomAccessFile(getDataFile(file), "rwd");
            accessFile.seek(block.offset);
            byte[] buf = new byte[BUF_SIZE];
            int len;
            while ((len = inStream.read(buf)) > 0) {
                accessFile.write(buf, 0, len);
                block.offset += len;
                onProgressChanged();
            }
        }

        @Override
        public void run() {
            do {
                try {
                    if (!block.isAtEnd()) {
                        getConnectionData();
                    }
                    flag = FLAG_COMPLETE; // mark end
                    block.offset = Block.FLAG_END;
                } catch (Exception ignore) {
                    // nothing
                } finally {
                    close();
                }
            } while (checkRetry());
            LogUtils.d(TAG, "worker #" + id + ": bye! ");
            onWorkerEnd(this);
        }

        boolean checkRetry() {
            return flag >= 0 && ++flag <= MAX_RETRY;
        }
    }

}