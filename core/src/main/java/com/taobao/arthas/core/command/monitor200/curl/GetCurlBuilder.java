package com.taobao.arthas.core.command.monitor200.curl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import com.taobao.arthas.core.util.IOUtils;
import com.taobao.arthas.core.util.StringUtils;

/**
 * @author zhaoyuening
 */
class GetCurlBuilder {

    public static class Header {
        private String name;
        private String value;

        public static List<Header> parseHeaders(GetCurlHttpRequest request) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
            List<Header> headers = new ArrayList<Header>();

            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                Header header = new Header();
                header.name = headerNames.nextElement();
                if (header.name.equals("content-length")) continue;
                header.value = request.getHeader(header.name);
                headers.add(header);
            }
            return headers;
        }
    }

    protected static final String FORMAT_HEADER = "-H \"%1$s:%2$s\"";
    protected static final String FORMAT_METHOD = "-X %1$s";
    protected static final String FORMAT_BODY = "-d '%1$s'";
    protected static final String FORMAT_URL = "\"%1$s\"";
    protected static final String CONTENT_TYPE = "Content-Type";
    /**
     * 针对这一种类型的请求需要特殊处理
     * 将所有params 存入 body中
     */
    protected static final String CONTENT_TYPE_URLENCODED = "application/x-www-form-urlencoded";

    protected final String url;
    protected final String method;
    protected final String contentType;
    protected final String body;
    protected final List<String> options;
    protected final List<Header> headers;
    protected final String delimiter;

    public GetCurlBuilder(Object requestObj) throws Exception {
        GetCurlHttpRequest request = new GetCurlHttpRequest(requestObj);
        this.url = request.getRequestURL().toString() + parseParams(request);
        this.method = request.getMethod();
        this.options = Collections.singletonList("--location");
        this.delimiter = " ";
        this.contentType = request.getContentType();
        this.body = parseBody(request);
        // 填充http头
        this.headers = Header.parseHeaders(request);
    }

    private String parseParams(GetCurlHttpRequest request) throws UnsupportedEncodingException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        // 因为 application/x-www-form-urlencoded params 都传入了 body 无需了
        if (!request.getParameterNames().hasMoreElements() || CONTENT_TYPE_URLENCODED.equals(request.getContentType())) {
            return "";
        }

        return "?" + parseForParameterMap(request.getParameterMap(), request.getCharacterEncoding());
    }

    public String build() {
        List<String> parts = new ArrayList<String>();
        parts.add("curl");
        parts.addAll(options);
        parts.add(String.format(FORMAT_METHOD, method.toUpperCase()));

        for (Header header : headers) {
            final String headerPart = String.format(FORMAT_HEADER, header.name, header.value);
            parts.add(headerPart);
        }

        if (contentType != null && !containsName(headers)) {
            parts.add(String.format(FORMAT_HEADER, CONTENT_TYPE, contentType));
        }

        if (body != null) {
            parts.add(String.format(FORMAT_BODY, body));
        }

        parts.add(String.format(FORMAT_URL, url));

        return delimiter + StringUtils.join(parts.toArray()," ");
    }

    private String parseBody(GetCurlHttpRequest request) throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        // 如果是 application/x-www-form-urlencoded 将所有 params 传入 body 中
        if (request.getParameterNames().hasMoreElements() &&
                CONTENT_TYPE_URLENCODED.equals(request.getContentType())) {
            return parseForParameterMap(request.getParameterMap(), request.getCharacterEncoding());
        }

        if (request.getContentLength() <= 0) {
            return "";
        }

        return IOUtils.toString(request.getInputStream());
    }

    /**
     * 解析出 parameterMap 为字符串
     */
    protected static String parseForParameterMap(Map<String, String[]> parameterMap, String charset)
            throws UnsupportedEncodingException {
        StringBuilder bodyBuilder = new StringBuilder();

        for (Map.Entry<String, String[]> stringEntry : parameterMap.entrySet()) {
            String key = stringEntry.getKey();
            for (String value : stringEntry.getValue()) {
                if (bodyBuilder.length() > 0) {
                    bodyBuilder.append("&");
                }
                bodyBuilder.append(key);
                bodyBuilder.append("=");
                bodyBuilder.append(encodeText(value, charset));
            }
        }

        return bodyBuilder.toString();
    }
    protected boolean containsName(List<Header> headers) {
        for (Header header : headers) {
            if (header.name.equals(GetCurlBuilder.CONTENT_TYPE)) {
                return true;
            }
        }

        return false;
    }

    protected static String encodeText(String text, String charset) throws UnsupportedEncodingException {
        return URLEncoder.encode(text, charset).replace("+","%20");
    }
}
