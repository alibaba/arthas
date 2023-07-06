package com.alibaba.arthas.tunnel.common.remote.response;

/**
 * @author qiyue.zhang@aloudata.com
 * @description ResponseCode
 * @date 2023/6/15 19:16
 */
public enum ResponseCode {
    SUCCESS(200, "Response ok"),
    
    FAIL(400, "Response fail");
    
    private int code;
    
    private String desc;
    
    ResponseCode(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    /**
     * Getter method for property <tt>code</tt>.
     *
     * @return property value of code
     */
    public int getCode() {
        return code;
    }
    
    /**
     * Getter method for property <tt>desc</tt>.
     *
     * @return property value of desc
     */
    public String getDesc() {
        return desc;
    }
}
