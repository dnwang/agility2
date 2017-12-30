package org.pinwheel.agility2.utils;

import android.os.Handler;
import android.os.Looper;

import org.pinwheel.agility2.action.Action1;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Copyright (C), 2017 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2017/8/24,16:36
 * @see
 */
public final class Converter {

    private final List<Filter> filters = new CopyOnWriteArrayList<>();

    public Converter addFilter(final Filter filter) {
        return addFilter(filter, filters.size());
    }

    public Converter addFilter(final Filter filter, int index) {
        filters.add(index, filter);
        return this;
    }

    public Converter removeFilter(final int index) {
        if (index >= 0 && index < filters.size()) {
            filters.remove(index);
        }
        return this;
    }

    public Converter removeFilter(final Filter filter) {
        if (null != filter && filters.contains(filter)) {
            filters.remove(filter);
        }
        return this;
    }

    public Converter setFilter(final Filter... filters) {
        this.filters.clear();
        if (null != filters) {
            Collections.addAll(this.filters, filters);
        }
        return this;
    }

    public Converter setFilter(final List<Filter> filters) {
        this.filters.clear();
        if (null != filters) {
            this.filters.addAll(filters);
        }
        return this;
    }

    public Object execute(final Object sourceObj) {
        return runFilters(sourceObj);
    }

    public void cancel() {
        if (!filters.isEmpty()) {
            for (Filter filter : filters) {
                filter.onCancel();
            }
        }
    }

    public Converter onProgress(Action1<Float> action) {
        progressAction = action;
        return this;
    }

    private Action1<Float> progressAction;
    private Handler mainHandler;

    private void dispatchProgress(final float percent) {
        if (null != progressAction) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                progressAction.call(percent);
            } else {
                if (null == mainHandler) {
                    mainHandler = new Handler(Looper.getMainLooper());
                }
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        progressAction.call(percent);
                    }
                });
            }
        }
    }

    private void setIntercept() {
        isIntercept = true;
    }

    private boolean isIntercept;

    private Object runFilters(Object obj) {
        if (filters.isEmpty()) {
            return obj;
        }
        final Stack<Filter> handleStack = new Stack<>();
        final Map<String, Object> args = new HashMap<>();
        // handle input
        for (Filter filter : filters) {
            // set converter
            filter.converter = this;
            // do input
            obj = filter.onInput(obj, args);
            handleStack.push(filter);
            if (isIntercept) {
                break;
            }
        }
        // do output
        while (!handleStack.isEmpty()) {
            obj = handleStack.pop().onOutput(obj, args);
        }
        return obj;
    }

    /**
     * Convert Filter
     */
    public static class Filter {

        private Converter converter;

        protected final void setIntercept() {
            this.converter.setIntercept();
        }

        protected final void setProgress(float percent) {
            this.converter.dispatchProgress(percent);
        }

        protected void onCancel() {
        }

        protected Object onInput(final Object obj, final Map<String, Object> args) {
            return obj;
        }

        protected Object onOutput(final Object obj, final Map<String, Object> args) {
            return obj;
        }

    }
}