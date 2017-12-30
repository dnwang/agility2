package org.pinwheel.agility2.module.net;

import java.util.UUID;

/**
 * Copyright (C), 2016 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2017/2/23,11:08
 * @see
 */
public class HttpRequest extends HttpPacket {

    private String uid;
    private int connectTimeOut;
    private int readTimeOut;

    public HttpRequest() {
        this.uid = UUID.randomUUID().toString().replace("-", "");
        this.connectTimeOut = 20 * 1000;
        this.readTimeOut = 20 * 1000;
    }

    public HttpRequest setConnectTimeOut(int connectTimeOut) {
        this.connectTimeOut = connectTimeOut;
        return this;
    }

    public int getConnectTimeOut() {
        return this.connectTimeOut;
    }

    public HttpRequest setReadTimeOut(int readTimeOut) {
        this.readTimeOut = readTimeOut;
        return this;
    }

    public int getReadTimeOut() {
        return this.readTimeOut;
    }

    public String getUid() {
        return uid;
    }

}