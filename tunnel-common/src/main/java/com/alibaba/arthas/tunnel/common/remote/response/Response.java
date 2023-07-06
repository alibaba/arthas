package com.alibaba.arthas.tunnel.common.remote.response;


import com.alibaba.arthas.tunnel.common.remote.Payload;

/**
 * @author qiyue.zhang@aloudata.com
 * @description Response
 * @date 2023/6/15 17:54
 */
public abstract class Response implements Payload {
    int resultCode = ResponseCode.SUCCESS.getCode();
    
    int errorCode;
    
    String message;
    
    String requestId;
    
    public int getResultCode() {
        return resultCode;
    }
    
    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }
    
    public int getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
