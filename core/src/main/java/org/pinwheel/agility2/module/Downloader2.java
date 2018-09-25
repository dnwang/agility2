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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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

    private Action2<File, Exception> completeAction;
    private Action2<Long, Long> progressAction;

    private int threadSize = Runtime.getRuntime().availableProcessors();

    public Downloader2() {
    }

    public Downloader2 fromUrl(String fromUrl) {
        this.fromUrl = fromUrl;
        return this;
    }

    public Downloader2 toFile(final File toFile) {
        this.toFile = toFile;
        return this;
    }

    public Downloader2 threadSize(int size) {
        threadSize = Math.max(1, size);
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
        }
    }

    private Controller controller;

    public Downloader2 start() {
        if (CommonTools.isEmpty(fromUrl) || null == toFile || toFile.exists()) {
            dividerError(new IllegalStateException("downloader checkStatus error"));
            return this;
        }
        AsyncHelper.INSTANCE.once(new Runnable() {
            @Override
            public void run() {
                controller = new Controller();
                controller.prepare(fromUrl, toFile);
                controller.allocateWorker(threadSize);
                controller.handleBlockQueue(new Runnable() {
                    @Override
                    public void run() {
                        dividerProgress(controller.p.progress, controller.p.contentLength);
                    }
                });
                if (controller.isComplete() && controller.transfer()) {
                    dividerSuccess();
                } else {
                    dividerError(new IllegalStateException("intercept download"));
                }
            }
        });
        return this;
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
            completeAction.call(toFile, null);
        }
    }

    private final static class Controller {
        private volatile boolean intercept = false;

        private BlockingQueue<Block> blockQueue = new LinkedBlockingQueue<>();
        private List<BlockGetter> workers;

        private File file;
        private Params p;

        void prepare(final String url, final File file) {
            this.file = file;
            final Params nativeParams = Params.load(new File(file.getAbsolutePath() + ".cf"));
            final Params remoteParams = Params.load(url);
            if (nativeParams.equals(remoteParams)) {
                this.p = nativeParams;
            } else {
                this.p = remoteParams;
            }
        }

        void allocateWorker(final int maxThreadSize) {
            workers = new ArrayList<>(maxThreadSize);
            // TODO
            if (p.contentLength < 2 * 2014 * 1024 // use single thread when size less than 2M
                    || 1 == maxThreadSize) {
                // single thread
                workers.add(new BlockGetter(0, Long.MAX_VALUE));
            } else {
                long begin = 0, end;
                for (int i = 0; i < maxThreadSize; i++) {
                    if (maxThreadSize - 1 == i) {
                        // fix last worker end value
                        end = Long.MAX_VALUE;
                    } else {
                        end = p.contentLength / maxThreadSize * (i + 1);
                    }
                    workers.add(new BlockGetter(begin, end));
                    begin = end + 1;
                }
            }
        }

        void handleBlockQueue(Runnable periodAction) {
            RandomAccessFile accessFile = null;
            try {
                final File dataFile = new File(this.file.getAbsolutePath() + ".data");
                FileUtils.prepareDirs(dataFile);
                accessFile = new RandomAccessFile(dataFile, "rwd");
                accessFile.setLength(p.contentLength);
                // start workers
                for (BlockGetter worker : workers) {
                    worker.executeBy(this);
                }
                // blocking write file
                Block block;
                do {
                    block = blockQueue.take();
                    accessFile.seek(block.offset);
                    accessFile.write(block.data);
                    p.merge(block);
                    block.recycle();
                    periodAction.run();

                    if (blockQueue.isEmpty() && isComplete()) {
                        break;
                    }
                } while (!intercept);
            } catch (Exception e) {
                LogUtils.e(TAG, e.getMessage());
            } finally {
                IOUtils.close(accessFile);
                intercept();
            }
        }

        void put(Block block) {
            if (block.offset >= 0 && block.data.length > 0) {
                blockQueue.add(block);
            }
        }

        boolean isComplete() {
            if (1 == p.blocks.size()) {
                long blockContentLength = p.blocks.get(0L);
                return blockContentLength == p.contentLength - 1;
            }
            return false;
        }

        boolean transfer() {
            final File dataFile = new File(file.getAbsolutePath() + ".data");
            final File cfFile = new File(file.getAbsolutePath() + ".cf");
            final boolean result = dataFile.exists() && dataFile.renameTo(file);
            if (result) {
                FileUtils.delete(cfFile);
            }
            return result;
        }

        void intercept() {
            intercept = true;
            workers.clear();
            if (!isComplete()) {
                AsyncHelper.INSTANCE.once(new Runnable() {
                    @Override
                    public void run() {
                        final File cfFile = new File(file.getAbsolutePath() + ".cf");
                        FileUtils.delete(cfFile);
                        FileUtils.prepareDirs(cfFile);
                        try {
                            IOUtils.object2Stream(new FileOutputStream(cfFile), p);
                        } catch (Exception e) {
                            LogUtils.e(TAG, "save config error! " + e.getMessage());
                        }
                    }
                });
            }
        }
    }

    private final static class Params implements Serializable {
        final String url;
        final long contentLength;
        final long lastModifyTime;

        long progress;
        Map<Long, Long> blocks;

        private Params(String url, long contentLength, long lastModifyTime) {
            this.url = url;
            this.contentLength = contentLength;
            this.lastModifyTime = lastModifyTime;
        }

        static Params load(File cfFile) {
            try {
                final Params params = (Params) IOUtils.stream2Object(new FileInputStream(cfFile));
                if (null != params && null != params.blocks) {
                    for (Map.Entry<Long, Long> b : params.blocks.entrySet()) {
                        params.progress += b.getValue() - b.getKey() + 1;
                    }
                }
                return params;
            } catch (Exception e) {
                return new Params(null, -1, -1);
            }
        }

        static Params load(String url) {
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
                LogUtils.e(TAG, "can't get remote p! " + e.getMessage());
            } finally {
                if (null != conn) {
                    conn.disconnect();
                }
            }
            return new Params(url, contentLength, lastModifyTime);
        }

        void merge(Block block) {
            if (null == blocks) {
                blocks = new LinkedHashMap<>();
            }
            progress += block.data.length;
            // merge
            long begin = block.offset;
            long end = begin + block.data.length - 1;
            Map.Entry<Long, Long> left = null, right = null;
            for (Map.Entry<Long, Long> b : blocks.entrySet()) {
                if (b.getKey() == end + 1) {
                    right = b;
                }
                if (b.getValue() == Math.max(0, begin - 1)) {
                    left = b;
                }
            }
            if (null != left) {
                begin = left.getKey();
                blocks.remove(left.getKey());
            }
            if (null != right) {
                end = right.getValue();
                blocks.remove(right.getKey());
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
                        && that.contentLength == contentLength
                        && that.lastModifyTime == lastModifyTime;
            }
        }
    }

    private final static class BlockGetter implements Runnable {
        private final static int BUF_SIZE = 1024;

        private String name;
        private final long begin, end;

        BlockGetter(long begin, long end) {
            this.begin = begin;
            this.end = end;

            this.name = this.toString();
            this.name = name.substring(name.lastIndexOf("@"));
        }

        private Controller controller;

        void executeBy(Controller controller) {
            this.controller = controller;
            AsyncHelper.INSTANCE.once(this);
        }

        @Override
        public void run() {
            if (null == controller || begin < 0 || end < 0 || begin > end) {
                LogUtils.e(TAG, "[" + this.name + "] range error! [" + begin + ", " + end + "]");
                return;
            }
            HttpURLConnection conn = null;
            InputStream inStream = null;
            try {
                conn = (HttpURLConnection) new URL(controller.p.url).openConnection();
                conn.setConnectTimeout(TIME_OUT);
                conn.setReadTimeout(TIME_OUT);
                conn.setRequestProperty("Accept-Encoding", "identity");
                conn.setRequestProperty("Range", "bytes=" + begin + "-" + (Long.MAX_VALUE == end ? "" : end));
                conn.connect();
                inStream = conn.getInputStream();
                long offset = begin;
                byte[] buf = new byte[BUF_SIZE];
                int len;
                while ((len = inStream.read(buf)) > 0 && !controller.intercept) {
                    byte[] data = new byte[len];
                    System.arraycopy(buf, 0, data, 0, len);
                    this.controller.put(Block.obtain(offset, data));
                    offset += len;
                }
            } catch (Exception e) {
                LogUtils.e(TAG, "[" + this.name + "] " + e.getMessage());
            } finally {
                IOUtils.close(inStream);
                if (null != conn) {
                    conn.disconnect();
                }
            }
        }
    }

    private final static class Block {
        long offset;
        byte[] data;

        void recycle() {
//            synchronized (POOL) {
//                this.offset = -1;
//                this.data = null;
//            }
        }

        boolean isRecycle() {
            return null == this.data;
        }

//        private static final int INIT_SIZE = 12;
//        private static final Set<WeakReference<Block>> POOL = new HashSet<>(INIT_SIZE);
//
//        static {
//            for (int i = 0; i < INIT_SIZE; i++) {
//                POOL.add(new WeakReference<>(new Block()));
//            }
//        }

        static Block obtain(long offset, byte[] data) {
            Block instance = new Block();
            instance.offset = offset;
            instance.data = data;
            return instance;

//            synchronized (POOL) {
//                Block instance = null;
//                for (WeakReference<Block> ref : POOL) {
//                    Block tmp = ref.get();
//                    if (null != tmp && tmp.isRecycle()) {
//                        instance = tmp;
//                        break;
//                    }
//                }
//                if (null == instance) {
//                    instance = new Block();
//                    POOL.add(new WeakReference<>(instance));
//                }
//                instance.offset = offset;
//                instance.data = data;
//                return instance;
//            }
        }
    }

}