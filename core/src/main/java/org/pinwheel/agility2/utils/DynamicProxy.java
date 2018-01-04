package org.pinwheel.agility2.utils;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.pinwheel.agility2.action.Action3;
import org.pinwheel.agility2.action.Function3;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
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

    private final Map<String, Function3<Boolean, Object, Method, Object[]>> funcMaps;
    private final Object target;

    public DynamicProxy(@NonNull Object target) {
        this.target = target;
        this.funcMaps = new HashMap<>();
    }

    private Object handler;

    public <T> T getHandler() {
        if (null == handler) {
            handler = Proxy.newProxyInstance(getClass().getClassLoader(),
                    target.getClass().getInterfaces(),
                    this);
        }
        return (T) handler;
    }

    public <T> T getHandler(Class<T> type) {
        return getHandler();
    }

    public DynamicProxy add(@NonNull String method, @NonNull final Action3<Object, Method, Object[]> action) {
        return add(method, new Function3<Boolean, Object, Method, Object[]>() {
            @Override
            public Boolean call(Object target, Method func, Object[] args) {
                action.call(target, func, args);
                return false;
            }
        });
    }

    public DynamicProxy add(@NonNull String method, @NonNull Function3<Boolean, Object, Method, Object[]> function) {
        if (!funcMaps.containsKey(method)) {
            funcMaps.put(method, function);
        }
        return this;
    }

    public DynamicProxy remove(@NonNull String method) {
        if (funcMaps.containsKey(method)) {
            funcMaps.remove(method);
        }
        return this;
    }

    public boolean contains(@NonNull String method) {
        return funcMaps.containsKey(method);
    }

    /**
     * proxy all when method is empty
     */
    @Override
    public Object invoke(Object o, Method func, Object[] args) throws Throwable {
        boolean pResult0 = false;
        boolean pResult1 = false;
        for (Map.Entry<String, Function3<Boolean, Object, Method, Object[]>> entry : funcMaps.entrySet()) {
            String pMethod = entry.getKey();
            if (TextUtils.isEmpty(pMethod)) {
                // globe
                pResult0 = entry.getValue().call(target, func, args);
            } else if (pMethod.equalsIgnoreCase(func.getName())) {
                pResult1 = entry.getValue().call(target, func, args);
            }
        }
        if (!pResult0 && !pResult1) {
            return func.invoke(target, args);
        } else {
            return null;
        }
    }
}