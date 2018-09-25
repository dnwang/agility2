package org.pinwheel.agility2.module;

import org.pinwheel.agility2.action.Action2;
import org.pinwheel.agility2.utils.CommonTools;
import org.pinwheel.agility2.utils.FileUtils;
import org.pinwheel.agility2.utils.IOUtils;
import org.pinwheel.agility2.utils.LogUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

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

//    public long getProgress() {
//        return progress;
//    }
//
//    public long getContentLength() {
//        return contentLength;
//    }
//
//    public boolean isDownloading() {
//        return !intercept;
//    }
//
//    private void saveInterceptParams() {
//        final InterceptParams args = new InterceptParams();
//        args.optionsList = new ArrayList<>(workers.size());
//        for (Worker worker : workers) {
//            args.optionsList.add(worker.getOptions());
//        }
//        args.contentLength = contentLength;
//        args.lastModifyTime = lastModifyTime;
//        args.fromUrl = fromUrl;
//        args.dataFile = dataFile;
//        FileUtils.delete(cfFile);
//        FileUtils.prepareDirs(cfFile);
//        try {
//            IOUtils.object2Stream(new FileOutputStream(cfFile), args);
//            LogUtils.d(TAG, "saveInterceptParams: " + cfFile);
//        } catch (FileNotFoundException e) {
//            LogUtils.e(TAG, "saveInterceptParams: " + e.getMessage());
//        }
//    }
//
//    private InterceptParams getInterceptParams() {
//        Object obj = null;
//        try {
//            obj = IOUtils.stream2Object(new FileInputStream(cfFile));
//        } catch (FileNotFoundException e) {
//            LogUtils.e(TAG, "getInterceptParams: " + e.getMessage());
//        }
//        if (null != obj) {
//            InterceptParams args = (InterceptParams) obj;
//            if (dataFile.exists()
//                    && fromUrl.equals(args.fromUrl)
//                    && dataFile.equals(args.dataFile)
//                    && !CommonTools.isEmpty(args.optionsList)) {
//                return args;
//            } else {
//                FileUtils.delete(cfFile);
//            }
//        }
//        return null;
//    }
//
//    private void notifyWorkerContentLengthChanged(Worker worker, long bufLen) {
//        if (!isDownloading()) {
//            return;
//        }
//        progress += bufLen;
//    }
//
//    private synchronized void onComplete(Worker worker) {
//        if (!isDownloading()) {
//            return;
//        }
//        if (worker.isComplete()) {
//            workers.remove(worker);
//            if (workers.isEmpty()) {
//                intercept = true;
//                FileUtils.delete(cfFile);
//                FileUtils.delete(toFile);
//                if (dataFile.renameTo(toFile)) {
//                    dividerSuccess();
//                } else {
//                    dividerError(new IllegalStateException("'dataFile' can't transfer to 'outFile'!"));
//                }
//            }
//        } else {
//            intercept = true;
//            saveInterceptParams();
//            dividerError(new InterruptedException("download worker interrupted, maybe connection error!"));
//        }
//    }

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
                FileUtils.prepareDirs(toFile);
                controller = new Controller(fromUrl, toFile);
                if (controller.prepare()) {
                    controller.allocateWorker(threadSize);
                    controller.handleBlockQueue(new Runnable() {
                        @Override
                        public void run() {
                            dividerProgress(controller.progress, controller.contentLength);
                        }
                    });
                }
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

        private BlockingQueue<Block> blockQueue = new PriorityBlockingQueue<>();
        private List<BlockGetter> workers;

        private String url;
        private File file;
        private File dataFile, cfFile;
        private long progress, contentLength, lastModifyTime;

        private Controller(String url, File file) {
            this.url = url;
            this.file = file;
            this.cfFile = new File(file.getAbsolutePath() + ".cf");
            this.dataFile = new File(file.getAbsolutePath() + ".data");
            this.lastModifyTime = Long.MIN_VALUE;
            this.contentLength = Long.MIN_VALUE;
            this.progress = 0;
        }

        private boolean prepare() {
            // remote params
            long remoteLastModifyTime = Long.MIN_VALUE;
            long remoteContentLength = Long.MIN_VALUE;
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestProperty("Accept-Encoding", "identity");
                remoteContentLength = conn.getContentLength();
                remoteLastModifyTime = conn.getLastModified();
            } catch (Exception e) {
                LogUtils.e(TAG, e.getMessage());
            } finally {
                if (null != conn) {
                    conn.disconnect();
                }
            }
            // TODO verify native config file
            contentLength = remoteContentLength;
            lastModifyTime = remoteLastModifyTime;

            return Long.MIN_VALUE != remoteLastModifyTime;
        }

        private void allocateWorker(final int maxThreadSize) {
            workers = new ArrayList<>(maxThreadSize);
            //
            if (contentLength < 2 * 2014 * 1024 // use single thread when size less than 2M
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
                        end = contentLength / maxThreadSize * (i + 1);
                    }
                    workers.add(new BlockGetter(begin, end));
                    begin = end + 1;
                }
            }
        }

        private void handleBlockQueue(Runnable periodAction) {
            RandomAccessFile accessFile = null;
            try {
                accessFile = new RandomAccessFile(dataFile, "rwd");
                accessFile.setLength(contentLength);
            } catch (Exception e) {
                IOUtils.close(accessFile);
                LogUtils.e(TAG, e.getMessage());
                intercept();
                return;
            }
            //
            for (BlockGetter worker : workers) {
                worker.executeBy(this);
            }
            //
            long dLen;
            final List<Block> availableBlock = new ArrayList<>();
            while (!intercept) {
                dLen = 0;
                availableBlock.clear();
                blockQueue.drainTo(availableBlock);
                for (Block block : availableBlock) {
                    try {
                        accessFile.seek(block.offset);
                        accessFile.write(block.data);

                        dLen += block.data.length;
                    } catch (IOException e) {
                        LogUtils.e(TAG, "write: [" + block.offset + "," + block.offset + block.data.length + "] error! " + e.getMessage());
                    }
                    block.recycle();
                }
                progress += dLen;
                periodAction.run();
            }
            // close
            IOUtils.close(accessFile);
            intercept();
        }

        private void put(Block block) {
            blockQueue.add(block);
        }

        private boolean isComplete() {
            // TODO
            return true;
        }

        private boolean transfer() {
            return this.dataFile.renameTo(this.file);
        }

        private void intercept() {
            intercept = true;
            workers.clear();
        }
    }

    private final static class BlockGetter implements Runnable {
        private final static int BUF_SIZE = 1024;

        private final long begin, end;

        BlockGetter(long begin, long end) {
            this.begin = begin;
            this.end = end;
        }

        private Controller controller;

        void executeBy(Controller controller) {
            this.controller = controller;
            AsyncHelper.INSTANCE.once(this);
        }

        @Override
        public void run() {
            if (null == controller || begin < 0 || end < 0 || begin < end) {
                LogUtils.e(TAG, "[" + this + "] exp: range error! [" + begin + ", " + end + "]");
                return;
            }
            HttpURLConnection conn = null;
            InputStream inStream = null;
            try {
                conn = (HttpURLConnection) new URL(controller.url).openConnection();
                conn.setConnectTimeout(TIME_OUT);
                conn.setReadTimeout(TIME_OUT);
                conn.setRequestProperty("Accept-Encoding", "identity");
                conn.setRequestProperty("Range", "bytes=" + begin + "-" + (Long.MAX_VALUE == end ? "" : end));
                conn.connect();
                inStream = conn.getInputStream();
                int contentLength = 0;
                byte[] buf = new byte[BUF_SIZE];
                int len;
                while ((len = inStream.read(buf)) > 0 && !controller.intercept) {
                    byte[] data = new byte[len];
                    System.arraycopy(buf, 0, data, 0, len);
                    this.controller.put(Block.obtain(contentLength, data));
                    contentLength += len;
                }
            } catch (Exception e) {
                LogUtils.e(TAG, "[" + this + "] exp: " + e.getMessage());
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
            synchronized (POOL) {
                this.offset = -1;
                this.data = null;
            }
        }

        boolean isRecycle() {
            return null == this.data;
        }

        private static final int INIT_SIZE = 12;
        private static final Set<WeakReference<Block>> POOL = new HashSet<>(INIT_SIZE);

        static {
            for (int i = 0; i < INIT_SIZE; i++) {
                POOL.add(new WeakReference<>(new Block()));
            }
        }

        static Block obtain(long offset, byte[] data) {
            synchronized (POOL) {
                Block instance = null;
                for (WeakReference<Block> ref : POOL) {
                    Block tmp = ref.get();
                    if (null != tmp && tmp.isRecycle()) {
                        instance = tmp;
                        break;
                    }
                }
                if (null == instance) {
                    instance = new Block();
                    POOL.add(new WeakReference<>(instance));
                }
                instance.offset = offset;
                instance.data = data;
                return instance;
            }
        }
    }

}