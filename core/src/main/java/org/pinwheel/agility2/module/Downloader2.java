package org.pinwheel.agility2.module;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
import java.net.HttpURLConnection;
import java.net.URL;
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
    private static final String TAG = Downloader2.class.getSimpleName();

    private static File getCfFile(File file) {
        return new File(file.getAbsolutePath() + ".cf");
    }

    private static File getDataFile(File file) {
        return new File(file.getAbsolutePath() + ".data");
    }

    private String url;
    private File file;
    private int threadSize;

    private Action2<File, Exception> completeAction;
    private Action2<Long, Long> progressAction;

    public Downloader2() {
        threadSize = Runtime.getRuntime().availableProcessors();
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
        final int defCount = Runtime.getRuntime().availableProcessors();
        threadSize = Math.min(Math.max(1, size), defCount * 2);
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

    public void stop() {
        if (null != controller) {
            controller.intercept();
            if (!controller.isComplete()) {
                Params.save(controller.p, getCfFile(file));
            }
        }
    }

    public Downloader2 start() {
        if (CommonTools.isEmpty(url) || null == file || file.exists()) {
            dividerError(new IllegalStateException("downloader checkStatus error"));
            return this;
        }
        controller = new Controller(new ControllerCallback() {
            @Override
            public void onProgressChanged() {
                dividerProgress(controller.p.progress, controller.p.contentLength);
            }

            @Override
            public void onWorkerEnd() {
                if (controller.isComplete() && transfer()) {
                    dividerSuccess();
                } else {
                    Params.save(controller.p, getCfFile(file));
                    dividerError(new IllegalStateException("download intercepted!"));
                }
            }
        });
        AsyncHelper.INSTANCE.once(new Runnable() {
            @Override
            public void run() {
                controller.prepare(url, file);
                controller.allocateWorker(threadSize);
            }
        });
        return this;
    }

    private Controller controller;

    private boolean transfer() {
        final File dataFile = getDataFile(file);
        final boolean result = dataFile.exists() && dataFile.renameTo(file);
        if (result) {
            FileUtils.delete(getCfFile(file));
        }
        return result;
    }

    private void dividerProgress(final long progress, final long total) {
        if (progressAction != null) {
            progressAction.call(progress, total);
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

    private final static class Controller {
        private List<Worker> workers;
        private Params p;

        private ControllerCallback callback;

        Controller(ControllerCallback callback) {
            this.callback = callback;
        }

        void prepare(String url, File file) {
            // native
            final Params nativeParams = Params.load(getCfFile(file));
            // remote
            long contentLength = -1, lastModifyTime = -1;
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestProperty("Accept-Encoding", "identity");
                conn.setDoOutput(false);
                conn.setDoInput(false);
                contentLength = conn.getContentLength();
                lastModifyTime = conn.getLastModified();
            } catch (Exception e) {
                LogUtils.e(TAG, "can't get remote params! " + e.getMessage());
            } finally {
                if (null != conn) {
                    conn.disconnect();
                }
            }
            final Params remoteParams = new Params(url, file, contentLength, lastModifyTime);
            // compare info
            if (remoteParams.equals(nativeParams)) {
                this.p = nativeParams;
            } else {
                this.p = remoteParams;
            }
        }

        void allocateWorker(final int maxThreadSize) {
            workers = new CopyOnWriteArrayList<>();
            if (null == p.blocks) {
                if (p.contentLength < 2 * 2014 * 1024 // use single thread when size less than 2M
                        || 1 == maxThreadSize) {
                    // single thread
                    workers.add(new Worker(0, Long.MAX_VALUE));
                } else {
                    long begin = 0, end;
                    for (int i = 0; i < maxThreadSize; i++) {
                        if (maxThreadSize - 1 == i) {
                            // fix last worker end value
                            end = Long.MAX_VALUE;
                        } else {
                            end = p.contentLength / maxThreadSize * (i + 1);
                        }
                        workers.add(new Worker(begin, end));
                        begin = end + 1;
                    }
                }
            } else {
                // TODO
            }
            // start workers
            for (Worker worker : workers) {
                worker.executeBy(this);
            }
        }

        void putBlock(long begin, long end) {
            p.merge(begin, end);
            callback.onProgressChanged();
        }

        void onWorkerEnd(Worker worker) {
            workers.remove(worker);
            if (isIntercept()) {
                callback.onWorkerEnd();
            }
        }

        void intercept() {
            if (!CommonTools.isEmpty(workers)) {
                for (Worker worker : workers) {
                    worker.stop();
                }
                workers.clear();
            }
        }

        boolean isIntercept() {
            return CommonTools.isEmpty(workers);
        }

        boolean isComplete() {
            if (null != p.blocks && 1 == p.blocks.size()) {
                long blockContentLength = p.blocks.getEnd(0L);
                return blockContentLength == p.contentLength - 1;
            }
            return false;
        }
    }

    private interface ControllerCallback {
        void onProgressChanged();

        void onWorkerEnd();
    }

    private final static class Params {
        final String url;
        final File file;
        final long contentLength;
        final long lastModifyTime;

        long progress = 0;
        BlockPairs blocks = null;

        private Params(String url, File file, long contentLength, long lastModifyTime) {
            this.url = url;
            this.file = file;
            this.contentLength = contentLength;
            this.lastModifyTime = lastModifyTime;
        }

        static void save(Params p, File cfFile) {
            FileUtils.delete(cfFile);
            FileUtils.prepareDirs(cfFile);
            try {
                IOUtils.string2Stream(new FileOutputStream(cfFile), p.toString());
            } catch (Exception e) {
                LogUtils.e(TAG, "save config error! " + e.getMessage());
            }
        }

        static Params load(File cfFile) {
            try {
                final String info = IOUtils.stream2String(new FileInputStream(cfFile));
                return fromString(info);
            } catch (Exception e) {
                return null;
            }
        }

        synchronized void merge(long begin, long end) {
            if (null == blocks) {
                blocks = new BlockPairs();
            }
            progress += (end - begin + 1);
            // merge
            final long leftBegin = blocks.getBegin(Math.max(0, begin - 1));
            if (BlockPairs.NULL != leftBegin) {
                begin = leftBegin;
                blocks.removeByBegin(leftBegin);
            }
            final long rightEnd = blocks.getEnd(end + 1);
            if (BlockPairs.NULL != rightEnd) {
                end = rightEnd;
                blocks.removeByEnd(rightEnd);
            }
            blocks.put(begin, end);
        }

        @Override
        public boolean equals(Object obj) {
            if (null == obj || !(obj instanceof Params)) {
                return false;
            } else {
                final Params that = (Params) obj;
                return that.url.equals(url)
                        && that.file.equals(file)
                        && that.contentLength == contentLength
                        && that.lastModifyTime == lastModifyTime;
            }
        }

        @Override
        public String toString() {
            final JSONObject json = new JSONObject();
            try {
                json.put("url", url);
                json.put("file", file.getAbsolutePath());
                json.put("contentLength", contentLength);
                json.put("lastModifyTime", lastModifyTime);
                json.put("progress", progress);
                final JSONArray blocksJson = new JSONArray();
                if (null != blocks) {
                    blocks.foreach(new Action2<Long, Long>() {
                        @Override
                        public void call(Long begin, Long end) {
                            blocksJson.put(begin + "," + end);
                        }
                    });
                }
                json.put("blocks", blocks);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return json.toString();
        }

        static Params fromString(String info) throws Exception {
            final JSONObject json = new JSONObject(info);
            String path = json.optString("file", null);
            final File file = null != path ? new File(path) : null;
            final Params params = new Params(
                    json.optString("url", null),
                    file,
                    json.optLong("contentLength", -1),
                    json.optLong("lastModifyTime", -1)
            );
            // extra
            params.progress = json.optLong("progress", 0);
            JSONArray blocksJson = json.optJSONArray("blocks");
            final int size = null == blocksJson ? 0 : blocksJson.length();
            if (size > 0) {
                params.blocks = new BlockPairs();
                for (int i = 0; i < size; i++) {
                    try {
                        String[] pair = blocksJson.getString(i).split(",");
                        params.blocks.put(Long.parseLong(pair[0]), Long.parseLong(pair[1]));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            return params;
        }
    }

    private final static class Worker implements Runnable {
        private final static int TIME_OUT = 30 * 1000;
        private final static int BUF_SIZE = 1024;
        private final static int MAX_RETRY = 3;

        private static int workerId = 0;

        private final int id;
        private final long begin, end;

        Worker(long begin, long end) {
            this.id = workerId++;

            this.begin = begin;
            this.end = end;
            if (begin < 0 || end < 0 || begin > end) {
                throw new IllegalStateException("worker #" + id + ": range error! [" + begin + ", " + end + "]");
            }
        }

        private Controller controller;
        private int retryCount = 0;
        private long offset;

        void executeBy(Controller controller) {
            this.offset = begin;
            this.retryCount = 0;
            this.controller = controller;
            AsyncHelper.INSTANCE.once(this);
        }

        void stop() {
            retryCount = -1; // mark intercept
            close();
        }

        private RandomAccessFile accessFile = null;
        private HttpURLConnection conn = null;
        private InputStream inStream = null;

        @Override
        public void run() {
            try {
                conn = (HttpURLConnection) new URL(controller.p.url).openConnection();
                conn.setConnectTimeout(TIME_OUT);
                conn.setReadTimeout(TIME_OUT);
                conn.setRequestProperty("Accept-Encoding", "identity");
                conn.setRequestProperty("Range", "bytes=" + offset + "-" + (Long.MAX_VALUE == end ? "" : end));
                conn.connect();
                inStream = conn.getInputStream();
                accessFile = new RandomAccessFile(getDataFile(controller.p.file), "rwd");
                accessFile.seek(offset);
                byte[] buf = new byte[BUF_SIZE];
                int len;
                while ((len = inStream.read(buf)) > 0) {
                    accessFile.write(buf, 0, len);
                    controller.putBlock(offset, offset + len - 1);
                    offset += len;
                }
                retryCount = -1; // mark intercept
            } catch (Exception ignore) {
                // nothing
            } finally {
                close();
                if (retryCount >= 0 && retryCount < MAX_RETRY) {
                    retryCount++;
                    LogUtils.e(TAG, "worker #" + id + ": retry " + retryCount);
                    run();
                } else {
                    LogUtils.d(TAG, "worker #" + id + ": bye! ");
                    controller.onWorkerEnd(this);
                }
            }
        }

        private void close() {
            IOUtils.close(accessFile);
            IOUtils.close(inStream);
            if (null != conn) {
                conn.disconnect();
            }
        }
    }

    private static final class BlockPairs {
        private static final long NULL = -1L;
        final long[][] pairs;
        final int capacity;

        BlockPairs() {
            this(8);
        }

        BlockPairs(final int capacity) {
            this.capacity = capacity;
            this.pairs = new long[capacity][2];
            for (int i = 0; i < capacity; i++) {
                for (int j = 0; j < pairs[i].length; j++) {
                    pairs[i][j] = NULL;
                }
            }
        }

        boolean put(long begin, long end) {
            for (int i = 0; i < capacity; i++) {
                if (NULL == pairs[i][0]) {
                    pairs[i][0] = begin;
                    pairs[i][1] = end;
                    return true;
                }
            }
            return false;
        }

        int size() {
            int count = 0;
            for (int i = 0; i < capacity; i++) {
                if (NULL != pairs[i][0]) {
                    count++;
                }
            }
            return count;
        }

        long getEnd(long begin) {
            for (int i = 0; i < capacity; i++) {
                if (begin == pairs[i][0]) {
                    return pairs[i][1];
                }
            }
            return NULL;
        }

        long getBegin(long end) {
            for (int i = 0; i < capacity; i++) {
                if (NULL != pairs[i][0] && end == pairs[i][1]) {
                    return pairs[i][0];
                }
            }
            return NULL;
        }

        boolean removeByBegin(long begin) {
            for (int i = 0; i < capacity; i++) {
                if (begin == pairs[i][0]) {
                    pairs[i][0] = NULL;
                    return true;
                }
            }
            return false;
        }

        boolean removeByEnd(long end) {
            for (int i = 0; i < capacity; i++) {
                if (NULL != pairs[i][0] && end == pairs[i][1]) {
                    pairs[i][0] = NULL;
                    return true;
                }
            }
            return false;
        }

        void foreach(Action2<Long, Long> action) {
            for (int i = 0; i < capacity; i++) {
                if (NULL != pairs[i][0]) {
                    action.call(pairs[i][0], pairs[i][1]);
                }
            }
        }

        long[][] reverse(final long begin, final long end) {
            long[][] newPairs = new long[capacity][2];
            for (int i = 0; i < capacity; i++) {
                for (int j = 0; j < pairs[i].length; j++) {
                    pairs[i][j] = NULL;
                }
            }
            // TODO

            return newPairs;
        }
    }

}