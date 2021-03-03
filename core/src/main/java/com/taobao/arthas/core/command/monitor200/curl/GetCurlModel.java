package com.taobao.arthas.core.command.monitor200.curl;

import java.util.Date;

import com.taobao.arthas.core.command.model.ResultModel;

/**
 * @author zhaoyuening
 */
public class GetCurlModel extends ResultModel {

    private Date ts;
    private String className;
    private String methodName;

    public GetCurlModel(Class clazz, String className, String methodName) {
        this.curl = new RequestCurl(clazz).toString();
        this.ts = new Date();
        this.className = className;
        this.methodName = methodName;
    }

    private String curl;

    @Override
    public String getType() {
        return "getcurl";
    }

    public String getCurl() {
        return curl;
    }

    public Date getTs() {
        return ts;
    }

    public String getClassName() {
        return this.className;
    }

    public String getMethodName() {
        return this.methodName;
    }
}
