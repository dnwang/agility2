package org.pinwheel.agility2.module.net.filter;


import org.pinwheel.agility2.module.net.HttpRequest;
import org.pinwheel.agility2.module.net.HttpResponse;
import org.pinwheel.agility2.utils.Converter;
import org.pinwheel.agility2.utils.LogUtils;

import java.util.Map;

/**
 * Copyright (C), 2016 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2017/2/23,11:25
 * @see
 */
public final class LogoutFilter extends Converter.Filter {

    private static final String TAG = LogoutFilter.class.getSimpleName();

    static void logout(boolean action, String id, String obj) {
        LogUtils.d(TAG, "[" + id + "]:" + (action ? " << " : " >> ") + obj);
    }

    @Override
    protected Object onInput(final Object obj, final Map<String, Object> args) {
        if (!LogUtils.enable()) {
            return obj;
        }
        if (obj instanceof HttpRequest) {
            final HttpRequest httpRequest = (HttpRequest) obj;
            final String id = httpRequest.getUid();
            // url
            logout(false, id, httpRequest.getUrl());
            // body
            final byte[] body = httpRequest.getBody();
            if (null != body) {
                logout(false, id, new String(body));
            } else {
                logout(false, id, "request body is empty");
            }
        } else {
            logout(false, "unknown", "request type unknown !");
        }
        return obj;
    }

    @Override
    protected Object onOutput(final Object obj, final Map<String, Object> args) {
        if (!LogUtils.enable()) {
            return obj;
        }
        if (obj instanceof HttpResponse) {
            final HttpResponse httpResponse = (HttpResponse) obj;
            final String id = httpResponse.getRequestUid();
            // code
            logout(true, id, "http code: " + String.valueOf(httpResponse.getCode()));
            // body
            final byte[] body = httpResponse.getBody();
            if (null != body) {
                logout(true, id, new String(body));
            } else {
                logout(true, id, "response body is empty");
            }
            // exception
            final Exception err = httpResponse.getError();
            if (null != err) {
                logout(true, httpResponse.getRequestUid(), err.getMessage());
            }
        } else {
            logout(true, "unknown", "response type unknown !");
        }
        return obj;
    }
}