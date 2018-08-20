package org.pinwheel.agility2.utils;

import android.text.TextUtils;

import org.pinwheel.agility2.action.Function1;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Copyright (C), 2017 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 28/10/2017,08:12
 */
public final class DynamicProxy implements InvocationHandler {

    private final Map<String, Function1<Object, Caller>> filters;
    private final Object target;

    public DynamicProxy(Object target) {
        this.target = target;
        this.filters = new LinkedHashMap<>();
    }

    public <T> T create() {
        final Object proxy = Proxy.newProxyInstance(getClass().getClassLoader(),
                target.getClass().getInterfaces(),
                this);
        return (T) proxy;
    }

    public DynamicProxy add(String method, Function1<Object, Caller> filter) {
        if (!contains(method)) {
            filters.put(method, filter);
        }
        return this;
    }

    public DynamicProxy remove(String method) {
        if (contains(method)) {
            filters.remove(method);
        }
        return this;
    }

    public DynamicProxy clear() {
        filters.clear();
        return this;
    }

    public boolean contains(String method) {
        return filters.containsKey(method);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        final Caller caller = new Caller(target, method, args);
        for (Map.Entry<String, Function1<Object, Caller>> entry : filters.entrySet()) {
            final String key = entry.getKey();
            final Function1<Object, Caller> filter = entry.getValue();
            if (TextUtils.isEmpty(key) || key.equals(method.getName())) {
                caller.result = filter.call(caller);
            }
        }
        return caller.result;
    }

    public static final class Caller {
        public final Object owner;
        public final Method method;
        public final Object[] args;

        private Object result = null;

        private Caller(Object owner, Method method, Object[] args) {
            this.owner = owner;
            this.method = method;
            this.args = args;
        }

        public final Object invoke() {
            return invoke(args);
        }

        public final Object invoke(Object[] args) {
            try {
                return method.invoke(owner, args);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

}