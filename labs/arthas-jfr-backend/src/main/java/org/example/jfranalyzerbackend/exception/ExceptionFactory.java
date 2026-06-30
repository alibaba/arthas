package org.example.jfranalyzerbackend.exception;

/**
 * 异常工厂类，用于创建各种异常
 */
public class ExceptionFactory {
    
    /**
     * 创建通用异常
     */
    public static CommonException createCommonException(ErrorCode errorCode) {
        return new CommonException(errorCode);
    }
    
    /**
     * 创建带消息的通用异常
     */
    public static CommonException createCommonException(ErrorCode errorCode, String message) {
        return new CommonException(errorCode, message);
    }
    
    /**
     * 创建带原因的通用异常
     */
    public static CommonException createCommonException(ErrorCode errorCode, Throwable cause) {
        return new CommonException(errorCode, cause);
    }
    
    /**
     * 创建带原因的通用异常（使用默认错误码）
     */
    public static CommonException createCommonException(Throwable cause) {
        return new CommonException(cause);
    }
    
    /**
     * 创建带消息和原因的通用异常
     */
    public static CommonException createCommonException(ErrorCode errorCode, String message, Throwable cause) {
        return new CommonException(errorCode, message, cause);
    }
    
    /**
     * 创建分析异常
     */
    public static ProfileAnalysisException createProfileAnalysisException(String message) {
        return new ProfileAnalysisException(message);
    }
    
    /**
     * 创建带原因的分析异常
     */
    public static ProfileAnalysisException createProfileAnalysisException(String message, Throwable cause) {
        return new ProfileAnalysisException(message, cause);
    }
    
    /**
     * 创建带原因的分析异常
     */
    public static ProfileAnalysisException createProfileAnalysisException(Throwable cause) {
        return new ProfileAnalysisException(cause);
    }
}
