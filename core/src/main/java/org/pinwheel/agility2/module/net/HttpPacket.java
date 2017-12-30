package org.pinwheel.agility2.module.net;

import java.util.HashMap;
import java.util.Map;

/**
 * Copyright (C), 2016 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2017/2/23,10:59
 * @see
 */
public class HttpPacket extends Packet {

    private Map<String, String> headers;
    private String method;
    private String url;

    public HttpPacket() {
        this.method = "GET";
    }

    public String getUrl() {
        return url;
    }

    public HttpPacket setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getMethod() {
        return method;
    }

    public HttpPacket setMethod(String method) {
        this.method = method;
        return this;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public HttpPacket addHeader(String key, Object value) {
        if (null != key) {
            if (null == headers) {
                headers = new HashMap<>();
            }
            headers.put(key, String.valueOf(value));
        }
        return this;
    }

    public HttpPacket addHeaders(Map<String, Object> pairs) {
        if (null != pairs && !pairs.isEmpty()) {
            for (Map.Entry<String, Object> entry : pairs.entrySet()) {
                addHeader(entry.getKey(), entry.getValue());
            }
        }
        return this;
    }

    public HttpPacket removeHeader(String key) {
        if (null != key && null != headers && headers.containsKey(key)) {
            headers.remove(key);
        }
        return this;
    }

    public String getHeader(String key) {
        if (null != key && null != headers && headers.containsKey(key)) {
            return headers.get(key);
        }
        return null;
    }

}