package com.alibaba.arthas.tunnel.common.remote.response;

/**
 * @author qiyue.zhang@aloudata.com
 * @description SimpleResponse
 * @date 2023/6/16 16:11
 */
public class SimpleResponse extends Response{

    private String body;
    
    public String getBody() {
        return body;
    }
    
    public void setBody(String body) {
        this.body = body;
    }
}
