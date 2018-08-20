package org.pinwheel.agility2.module.net.filter;

import org.pinwheel.agility2.module.net.HttpRequest;
import org.pinwheel.agility2.module.net.HttpResponse;
import org.pinwheel.agility2.utils.Converter;
import org.pinwheel.agility2.utils.IOUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Copyright (C), 2016 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2017/2/23,11:25
 */
public final class HttpURLConnectionFilter extends Converter.Filter {

    @Override
    protected Object onInput(final Object obj, final Map<String, Object> args) {
        if (!(obj instanceof HttpRequest)) {
            return null;
        }
        final HttpRequest httpRequest = (HttpRequest) obj;
        final HttpResponse response = HttpResponse.of(httpRequest);
        // do request
        HttpURLConnection connection = null;
        OutputStream outputStream = null;
        try {
            // url
            connection = (HttpURLConnection) new URL(httpRequest.getUrl()).openConnection();
            // method
            connection.setRequestMethod(httpRequest.getMethod().toUpperCase());
            // header
            Map<String, String> headers = httpRequest.getHeaders();
            if (null != headers) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            // time
            int connectTimeOut = httpRequest.getConnectTimeOut();
            if (connectTimeOut > 0) {
                connection.setConnectTimeout(connectTimeOut);
            }
            int readTimeOut = httpRequest.getReadTimeOut();
            if (readTimeOut > 0) {
                connection.setReadTimeout(readTimeOut);
            }
            // execute
//            connection.setDoOutput(true);
            connection.connect();
            // body
            byte[] body = httpRequest.getBody();
            if (null != body) {
                outputStream = connection.getOutputStream();
                outputStream.write(body, 0, body.length);
                outputStream.flush();
            }
            // set response params
            fillResponse(response, connection);
        } catch (Exception e) {
            response.setError(e);
        } finally {
            IOUtils.close(outputStream);
            if (null != connection) {
                connection.disconnect();
            }
        }
        return response;
    }

    private static void fillResponse(final HttpResponse response, HttpURLConnection connection) {
        InputStream inputStream = null;
        try {
            int code = connection.getResponseCode();
            // code
            response.setCode(code);
            // body
            if (code < HttpURLConnection.HTTP_BAD_REQUEST) {
                inputStream = connection.getInputStream();
            } else {
                inputStream = connection.getErrorStream();
            }
            byte[] body = IOUtils.stream2Bytes(inputStream);
            response.setBody(body);
            // header
            Map<String, List<String>> headers = connection.getHeaderFields();
            if (null != headers) {
                for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                    String tmp = "";
                    List<String> values = entry.getValue();
                    if (null != values) {
                        for (String string : values) {
                            tmp += (", " + string);
                        }
                        tmp = tmp.replaceFirst(", ", "");
                    }
                    response.addHeader(entry.getKey(), tmp);
                }
            }
        } catch (Exception e) {
            response.setError(e);
        } finally {
            IOUtils.close(inputStream);
        }
    }

}