package com.taobao.arthas.core.command.monitor200.curl;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Map;

/**
 * @author zhaoyuening
 * 这是对反射获取的HttpServletRequest对象的包装类
 */
class GetCurlHttpRequest {

    private Object requestObj;

    public GetCurlHttpRequest(Object requestObj) {
        this.requestObj = requestObj;
    }

    public StringBuffer getRequestURL() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method getRequestURL = requestObj.getClass().getMethod("getRequestURL");
        return (StringBuffer) getRequestURL.invoke(requestObj);
    }

    public Enumeration getParameterNames() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method getRequestURL = requestObj.getClass().getMethod("getParameterNames");
        return (Enumeration) getRequestURL.invoke(requestObj);
    }

    public String getContentType() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method getRequestURL = requestObj.getClass().getMethod("getContentType");
        return (String) getRequestURL.invoke(requestObj);
    }

    public Map<String, String[]> getParameterMap() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method getRequestURL = requestObj.getClass().getMethod("getParameterMap");
        return (Map<String, String[]>) getRequestURL.invoke(requestObj);
    }

    public String getCharacterEncoding() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method getRequestURL = requestObj.getClass().getMethod("getCharacterEncoding");
        return (String) getRequestURL.invoke(requestObj);
    }

    public String getMethod() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method getRequestURL = requestObj.getClass().getMethod("getMethod");
        return (String) getRequestURL.invoke(requestObj);
    }

    public int getContentLength() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method getRequestURL = requestObj.getClass().getMethod("getContentLength");
        return (Integer) getRequestURL.invoke(requestObj);
    }

    public InputStream getInputStream() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method getRequestURL = requestObj.getClass().getMethod("getInputStream");
        return (InputStream) getRequestURL.invoke(requestObj);
    }

    public Enumeration<String> getHeaderNames() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method getRequestURL = requestObj.getClass().getMethod("getHeaderNames");
        return (Enumeration<String>) getRequestURL.invoke(requestObj);
    }

    public String getHeader(String name) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method getRequestURL = requestObj.getClass().getMethod("getHeader", String.class);
        return (String) getRequestURL.invoke(requestObj, name);
    }
}
