package org.pinwheel.agility2.module.net.filter;

import android.os.Build;
import android.text.TextUtils;

import org.pinwheel.agility2.module.net.AbsApi;
import org.pinwheel.agility2.module.net.DELETE;
import org.pinwheel.agility2.module.net.GET;
import org.pinwheel.agility2.module.net.HttpRequest;
import org.pinwheel.agility2.module.net.HttpResponse;
import org.pinwheel.agility2.module.net.POST;
import org.pinwheel.agility2.module.net.PUT;
import org.pinwheel.agility2.utils.CommonTools;
import org.pinwheel.agility2.utils.Converter;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Copyright (C), 2017 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2017/8/25,10:11
 * @see
 */
public final class ApiConvertFilter extends Converter.Filter {

    public static final String KEY_REQUEST_OBJ = ApiConvertFilter.class.getSimpleName() + "_REQUEST_OBJ";

    @Override
    protected Object onInput(final Object obj, final Map<String, Object> args) {
        if (!(obj instanceof AbsApi)) {
            return obj;
        }
        final AbsApi api = (AbsApi) obj;

        final String method;
        final String tmpEndpoint;
        final Class<?> cls = api.getClass();
        if (cls.isAnnotationPresent(POST.class)) {
            method = "POST";
            tmpEndpoint = cls.getAnnotation(POST.class).value();
        } else if (cls.isAnnotationPresent(GET.class)) {
            method = "GET";
            tmpEndpoint = cls.getAnnotation(GET.class).value();
        } else if (cls.isAnnotationPresent(PUT.class)) {
            method = "PUT";
            tmpEndpoint = cls.getAnnotation(PUT.class).value();
        } else if (cls.isAnnotationPresent(DELETE.class)) {
            method = "DELETE";
            tmpEndpoint = cls.getAnnotation(DELETE.class).value();
        } else {
            throw new RuntimeException(cls.getSimpleName() + " can't find http method annotation");
        }

        final Map<String, String> params = api.getParams();
        final String endpoint = convertEndpoint(tmpEndpoint, params);
        final String url;
        // complete final request url
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
            // url have no change
            url = api.getDomain() + endpoint;
        } else if ("GET".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
            final String tmpUrl = api.getDomain() + endpoint;
            final String pairsStr = CommonTools.convertPairsToString(params, "UTF-8");
            params.clear();
            // value pairs in url
            if (!TextUtils.isEmpty(pairsStr)) {
                if (tmpUrl.contains("?")) {
                    url = tmpUrl.endsWith("?") ? (tmpUrl + pairsStr) : (tmpUrl + "&" + pairsStr);
                } else {
                    url = tmpUrl + "?" + pairsStr;
                }
            } else {
                url = tmpUrl;
            }
        } else {
            throw new RuntimeException(cls.getSimpleName() + " unknown http method, the converter can't complete http url !");
        }

        args.put(KEY_REQUEST_OBJ, api);

        final HttpRequest request = new HttpRequest();
        request.setUrl(url);
        request.setMethod(method);
        request.addHeaders(api.getHeaders());
        request.setBody(api.onConvertRequestBody(params));
        return request;
    }

    @Override
    protected Object onOutput(final Object obj, final Map<String, Object> args) {
        if (!(obj instanceof HttpResponse)) {
            return obj;
        }
        final HttpResponse response = (HttpResponse) obj;
        final AbsApi.AbsResp apiResponse = new AbsApi.AbsResp();
        // format response data obj
        final AbsApi api = (AbsApi) args.get(KEY_REQUEST_OBJ);
        if (null != api) {
            try {
                apiResponse.obj = api.onConvertResponse(response.getBody());
            } catch (Exception e) {
                Exception superExp = response.getError();
                if (null != superExp && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    superExp.addSuppressed(e);
                } else {
                    response.setError(e);
                }
                LogoutFilter.logout(true, response.getRequestUid(), api.getClass().getSimpleName() + " convert response error !");
                LogoutFilter.logout(true, response.getRequestUid(), e.getMessage());
            }
        }
        HttpResponse.copy(response, apiResponse);
        apiResponse.setBody(null);
        return apiResponse;
    }

    private static String convertEndpoint(final String endpoint, final Map<String, String> params) {
        if (!TextUtils.isEmpty(endpoint) && null != params) {
            Matcher matcher = Pattern.compile("[{]@(.*?)[}]").matcher(endpoint);
            StringBuffer newEndpoint = new StringBuffer();
            while (matcher.find()) {
                String value = params.remove(matcher.group(1));
                matcher.appendReplacement(newEndpoint, value);
            }
            matcher.appendTail(newEndpoint);
            return newEndpoint.toString();
        } else {
            return endpoint;
        }
    }

}