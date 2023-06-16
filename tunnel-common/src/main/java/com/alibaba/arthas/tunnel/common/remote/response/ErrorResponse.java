package com.alibaba.arthas.tunnel.common.remote.response;

/**
 * @author qiyue.zhang@aloudata.com
 * @description ErrorResponse
 * @date 2023/6/15 19:14
 */
public class ErrorResponse extends Response {

    public static Response build(int errorCode, String msg) {
        ErrorResponse response = new ErrorResponse();
        response.setErrorCode(errorCode);
        response.setMessage(msg);
        return response;
    }
}
