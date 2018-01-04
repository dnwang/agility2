package org.pinwheel.agility2.module.net;

import android.app.Activity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.pinwheel.agility2.action.Action1;
import org.pinwheel.agility2.action.Action3;
import org.pinwheel.agility2.action.Function0;
import org.pinwheel.agility2.module.AsyncHelper;
import org.pinwheel.agility2.utils.Converter;
import org.pinwheel.agility2.utils.FieldUtils;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Copyright (C), 2016 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2017/2/24,10:07
 */
public class AbsApi<T> {

    static final long DEF_CACHE_TIME_OUT = 5 * 60 * 1000;

    private Action3<T, Integer, Exception> completeAction = null;
    private Action1<Float> progressAction = null;
    private volatile boolean isCanceled = false;
    private volatile long cacheTimeOut = -1;// no cache
    private final String domain; // request url = domain/endpoint/(params);

    public AbsApi(String domain) {
        this.domain = domain;
        Cacheable cacheable = getClass().getAnnotation(Cacheable.class);
        if (null != cacheable) {
            setCacheTimeOut(cacheable.value());
        }
    }

    public final String getDomain() {
        return domain;
    }

    public Map<String, String> getHeaders() {
        return null;
    }

    public String getCacheKey() {
        return null;
    }

    public Map<String, String> getParams() {
        // super
        Map<String, Object> properties = FieldUtils.getFieldWithMark((Class) this.getClass().getSuperclass(), this);
        // self
        if (null == properties) {
            properties = new HashMap<>();
        }
        properties.putAll(FieldUtils.obj2Map(this));
        properties.remove("$change");
        properties.remove("serialVersionUID");
        properties.remove("shadow$_monitor_");
        properties.remove("shadow$_klass_");
        final Gson gson = new GsonBuilder().create();
        final Map<String, String> params = new HashMap<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            Object obj = entry.getValue();
            if (obj instanceof String) {
                params.put(entry.getKey(), (String) obj);
            } else if (null != obj) {
                params.put(entry.getKey(), gson.toJson(obj));
            }
        }
        return params;
    }

    protected List<Class<? extends Converter.Filter>> getFilters() {
        return null;
    }

    public byte[] onConvertRequestBody(final Map<String, Object> params) {
        if (null != params && !params.isEmpty()) {
            return new GsonBuilder().create().toJson(params).getBytes();
        } else {
            return null;
        }
    }

    public T onConvertResponse(final byte[] body) throws Exception {
        final String json = new String(body);
        final Type type = FieldUtils.getGenericClass(this);
        if ((type instanceof Class) && String.class.getName().equals(((Class) type).getName())) {
            return (T) json;
        } else {
            return new GsonBuilder().create().fromJson(json, type);
        }
    }

    public final AbsApi<T> onComplete(Action3<T, Integer, Exception> callback) {
        this.completeAction = callback;
        return this;
    }

    public final AbsApi<T> onProgress(Action1<Float> action) {
        this.progressAction = action;
        return this;
    }

    public final T getRespObj() {
        final AbsResp<T> resp = getResp();
        return (null != resp) ? resp.obj : null;
    }

    public final AbsResp<T> getResp() {
        final List<Class<? extends Converter.Filter>> filters = getFilters();
        if (null == filters || filters.isEmpty()) {
            throw new RuntimeException("Please return special filter list by 'getFilters' method !");
        }
        final Converter converter = new Converter();
        final int size = filters.size();
        for (int i = 0; i < size; i++) {
            try {
                converter.addFilter(filters.get(i).newInstance());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Object obj = converter.onProgress(progressAction).execute(this);
        return (null != obj) ? (AbsResp) obj : null;
    }

    public final AbsApi<T> execute() {
        return executeBy(null);
    }

    public final AbsApi<T> executeBy(final Activity activity) {
        AsyncHelper.INSTANCE.once(new Function0<AbsResp<T>>() {
            @Override
            public AbsResp<T> call() {
                return getResp();
            }
        }, new Action1<AbsResp<T>>() {
            @Override
            public void call(AbsResp<T> resp) {
                if ((null == activity || !activity.isFinishing()) && null != completeAction) {
                    if (null != resp) {
                        completeAction.call(resp.obj, resp.getCode(), resp.getError());
                    } else {
                        completeAction.call(null, -99999, null);
                    }
                }
            }
        });
        return this;
    }

    public final AbsApi<T> cancel() {
        this.isCanceled = true;
        return this;
    }

    public final boolean isCanceled() {
        return isCanceled;
    }

    public final long getCacheTimeOut() {
        return cacheTimeOut;
    }

    public final AbsApi<T> setCacheTimeOut(long timeOut) {
        cacheTimeOut = timeOut;
        return this;
    }

    public final AbsApi<T> setCacheable(boolean is) {
        return setCacheTimeOut(is ? DEF_CACHE_TIME_OUT : -1);
    }

    /**
     * API request response
     */
    public static final class AbsResp<T> extends HttpResponse {
        public T obj;
    }

}