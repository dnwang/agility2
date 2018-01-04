package org.pinwheel.agility2.module.net.filter;

import android.text.TextUtils;

import org.pinwheel.agility2.module.net.HttpRequest;
import org.pinwheel.agility2.module.net.HttpResponse;
import org.pinwheel.agility2.utils.Converter;
import org.pinwheel.agility2.utils.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Copyright (C), 2017 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2017/8/29,9:05
 */
public final class GZipFilter extends Converter.Filter {

    @Override
    protected Object onInput(final Object obj, final Map<String, Object> args) {
        if (!(obj instanceof HttpRequest)) {
            return obj;
        }
        final HttpRequest request = (HttpRequest) obj;
        final String encoding = request.getHeader("Accept-Encoding");
        if (TextUtils.isEmpty(encoding) || !encoding.contains("gzip")) {
            return obj;
        }
        // gzip set body
        ByteArrayOutputStream byteOutStream = null;
        try {
            byte[] body = request.getBody();
            byteOutStream = new ByteArrayOutputStream();
            IOUtils.bytes2Stream(new GZIPOutputStream(byteOutStream), body);
            body = byteOutStream.toByteArray();
            request.setBody(body);
        } catch (IOException e) {
            LogoutFilter.logout(true, request.getUid(), e.getMessage());
        } finally {
            IOUtils.close(byteOutStream);
        }
        return obj;
    }

    @Override
    protected Object onOutput(final Object obj, final Map<String, Object> args) {
        if (!(obj instanceof HttpResponse)) {
            return obj;
        }
        final HttpResponse response = (HttpResponse) obj;
        final String encoding = response.getHeader("Content-Encoding");
        if (TextUtils.isEmpty(encoding) || !encoding.contains("gzip")) {
            return obj;
        }
        // gzip set body
        try {
            byte[] body = response.getBody();
            body = IOUtils.stream2Bytes(new GZIPInputStream(new ByteArrayInputStream(body)));
            response.setBody(body);
        } catch (IOException e) {
            LogoutFilter.logout(true, response.getRequestUid(), e.getMessage());
        }
        return obj;
    }

}