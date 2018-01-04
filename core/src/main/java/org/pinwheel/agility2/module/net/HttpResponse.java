package org.pinwheel.agility2.module.net;

import java.util.Map;

/**
 * Copyright (C), 2016 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2017/2/23,11:08
 */
public class HttpResponse extends HttpPacket {

    public static HttpResponse of(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        response.requestUid = request.getUid();
        response.setMethod(request.getMethod());
        response.setUrl(request.getUrl());
        return response;
    }

    public static <T extends HttpResponse> T copy(HttpResponse from, T to) {
        if (null == from || null == to) {
            return to;
        }
        to.setRequestUid(from.getRequestUid());
        to.setMethod(from.getMethod());
        to.setBody(from.getBody());
        to.setUrl(from.getUrl());
        to.setCode(from.getCode());
        to.setError(from.getError());
        final Map<String, String> header = from.getHeaders();
        if (null != header && !header.isEmpty()) {
            for (Map.Entry<String, String> entry : header.entrySet()) {
                to.addHeader(entry.getKey(), entry.getValue());
            }
        }
        return to;
    }

    private String requestUid;
    private int code;
    private Exception error;

    HttpResponse() {
        code = -1;
    }

    void setRequestUid(String requestUid) {
        this.requestUid = requestUid;
    }

    public String getRequestUid() {
        return requestUid;
    }

    public int getCode() {
        return code;
    }

    public HttpResponse setCode(int code) {
        this.code = code;
        return this;
    }

    public Exception getError() {
        return error;
    }

    public HttpResponse setError(Exception e) {
        this.error = e;
        return this;
    }

}