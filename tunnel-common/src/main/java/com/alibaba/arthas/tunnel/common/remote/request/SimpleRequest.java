package com.alibaba.arthas.tunnel.common.remote.request;

/**
 * @author qiyue.zhang@aloudata.com
 * @description SimpleRequest
 * @date 2023/6/16 16:11
 */
public class SimpleRequest extends Request{

    private String body;
    
    public String getBody() {
        return body;
    }
    
    public void setBody(String body) {
        this.body = body;
    }
}
