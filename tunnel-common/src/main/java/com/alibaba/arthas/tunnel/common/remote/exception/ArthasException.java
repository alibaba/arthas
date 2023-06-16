package com.alibaba.arthas.tunnel.common.remote.exception;

/**
 * @author qiyue.zhang@aloudata.com
 * @description ArthasException
 * @date 2023/6/16 15:51
 */
public class ArthasException extends RuntimeException {
    public static final int SERVER_ERROR = 500;
    
    public ArthasException(int serverError, String s) {
    
    }
    
    public ArthasException() {
    
    }
}
