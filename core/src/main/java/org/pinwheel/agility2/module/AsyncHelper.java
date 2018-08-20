package org.pinwheel.agility2.module;

import android.os.Handler;
import android.os.Looper;

import org.pinwheel.agility2.action.Action1;
import org.pinwheel.agility2.action.Function0;
import org.pinwheel.agility2.action.Function1;
import org.pinwheel.agility2.utils.CommonTools;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Copyright (C), 2017 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 28/08/2017,21:20
 */
public enum AsyncHelper {

    INSTANCE;

    private final int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    private final int KEEP_ALIVE_TIME = 1;

    private final ExecutorService executor = new ThreadPoolExecutor(
            NUMBER_OF_CORES,
            NUMBER_OF_CORES * 2,
            KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(),
            new ThreadFactory() {
                private final AtomicInteger mCount = new AtomicInteger(1);

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "AsyncHelper #" + mCount.getAndIncrement());
                }
            },
            new RejectedExecutionHandler() {
                @Override
                public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {

                }
            }
    );
    private final Map<String, Task> tasks = new ConcurrentHashMap<>();

    public String delay(long interval, final Runnable runnable) {
        if (null == runnable) {
            throw new IllegalStateException("task params error !");
        }
        return specific(2, interval, new Action1<Integer>() {
            @Override
            public void call(Integer count) {
                if (2 == count) {
                    runnable.run();
                }
            }
        });
    }

    public String once(final Runnable runnable) {
        if (null == runnable) {
            throw new IllegalStateException("task params error !");
        }
        return once(new Function0<Object>() {
            @Override
            public Object call() {
                runnable.run();
                return null;
            }
        }, null);
    }

    public <T> String once(final Function0<T> func, Action1<T> action) {
        if (null == func) {
            throw new IllegalStateException("task params error !");
        }
        final Option option = new Option(1, 0); // once
        final Task<T> task = new Task<>(option, new Function1<T, Integer>() {
            @Override
            public T call(Integer obj0) {
                return func.call();
            }
        }, action);
        startTask(task);
        return task.id;
    }

    public String specific(int count, long interval, Function1<?, Integer> func) {
        return specific(count, interval, func, null);
    }

    public String specific(int count, long interval, Action1<Integer> action) {
        return specific(count, interval, new Function1<Integer, Integer>() {
            @Override
            public Integer call(Integer obj0) {
                return obj0;
            }
        }, action);
    }

    public <T> String specific(int count, long interval, Function1<T, Integer> func, Action1<T> action) {
        if (count <= 0 || interval <= 0 || (null == func && null == action)) {
            throw new IllegalStateException("task params error !");
        }
        final Option option = new Option(count, interval);
        final Task<T> task = new Task<>(option, func, action);
        startTask(task);
        return task.id;
    }

    public String forever(long interval, Function1<?, Integer> func) {
        return forever(interval, func, null);
    }

    public String forever(long interval, Action1<Integer> action) {
        return forever(interval, new Function1<Integer, Integer>() {
            @Override
            public Integer call(Integer obj0) {
                return obj0;
            }
        }, action);
    }

    public <T> String forever(long interval, Function1<T, Integer> func, Action1<T> action) {
        if (interval <= 0 || (null == func && null == action)) {
            throw new IllegalStateException("task params error !");
        }
        final Option option = new Option(-1, interval);// forever
        final Task<T> task = new Task<>(option, func, action);
        startTask(task);
        return task.id;
    }

    private void startTask(Task task) {
        if (null != task) {
            tasks.put(task.id, task);
            executor.execute(task.runnable);
        }
    }

    public void stopTask(String taskId) {
        if (!CommonTools.isEmpty(taskId) && tasks.containsKey(taskId)) {
            tasks.remove(taskId).stop();
        }
    }

    public void stopAll() {
        for (Map.Entry<String, Task> entry : tasks.entrySet()) {
            entry.getValue().stop();
        }
        tasks.clear();
    }

    private static final class Option {
        final int count; // < 0 forever
        final long interval;

        Option(int count, long interval) {
            this.count = count;
            this.interval = interval;
        }
    }

    private static final class Task<K> {
        volatile boolean running = false;

        final String id;
        final Option option;
        final Runnable runnable;
        final Function1<K, Integer> func;
        final Action1<K> action;
        final Handler uiHandler;

        private int count = 0;

        private Task(final Option option, final Function1<K, Integer> func, final Action1<K> action) {
            this.id = CommonTools.randomUUID();
            this.option = option;
            this.func = func;
            this.action = action;
            this.uiHandler = (null == action) ? null : new Handler(Looper.getMainLooper());

            this.runnable = new Runnable() {
                @Override
                public void run() {
                    running = true;
                    // just run once
                    while (running && (option.count < 0 || count < option.count)) {
                        final K result = (null != func) ? func.call(count + 1) : null;
                        if (null != action) {
                            uiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    action.call(result);
                                }
                            });
                        }
                        count++;
                        if (option.interval > 0) {
                            CommonTools.sleep(option.interval);
                        }
                    }
                    INSTANCE.stopTask(id);
                }
            };
        }

        void stop() {
            running = false;
        }
    }

}
