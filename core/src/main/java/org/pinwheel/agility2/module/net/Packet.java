package org.pinwheel.agility2.module.net;

import java.io.Serializable;

/**
 * Copyright (C), 2017 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 03/03/2017,22:13
 * @see
 */
public class Packet implements Serializable {

    private byte[] body = null;

    public byte[] getBody() {
        return body;
    }

    public Packet setBody(byte[] body) {
        this.body = body;
        return this;
    }

}