
package org.example.jfranalyzerbackend.exception;


import static org.example.jfranalyzerbackend.enums.CommonErrorCode.INTERNAL_ERROR;

/**
 * 通用异常类
 * 当不知道使用哪种异常时，可以使用此异常
 */
public class CommonException extends ErrorCodeException {

    /**
     * 创建新的通用异常
     *
     * @param errorCode 错误代码
     * @param message   错误消息
     */
    public CommonException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 创建新的通用异常
     *
     * @param errorCode 错误代码
     */
    public CommonException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 创建新的通用异常
     *
     * @param message 错误消息
     */
    public CommonException(String message) {
        super(INTERNAL_ERROR, message);
    }

    /**
     * 创建新的通用异常
     *
     * @param throwable 异常原因
     */
    public CommonException(Throwable throwable) {
        super(INTERNAL_ERROR, throwable);
    }

    /**
     * 创建新的通用异常
     *
     * @param errorCode 错误代码
     * @param cause     异常原因
     */
    public CommonException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * 创建新的通用异常
     *
     * @param errorCode 错误代码
     * @param message   错误消息
     * @param cause     异常原因
     */
    public CommonException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * 创建CommonException的快捷方法
     *
     * @param errorCode 错误代码
     * @return 新的通用异常
     */
    public static CommonException CE(ErrorCode errorCode) {
        return new CommonException(errorCode);
    }
}
