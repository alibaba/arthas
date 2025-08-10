
package org.example.jfranalyzerbackend.exception;


import static org.example.jfranalyzerbackend.enums.CommonErrorCode.INTERNAL_ERROR;

/**
 * Common Exception.
 * Use this exception when you don't know what exception to use.
 */
public class CommonException extends ErrorCodeException {

    /**
     * Create a new CommonException.
     *
     * @param errorCode error code
     * @param message   message
     */
    public CommonException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Create a new CommonException.
     *
     * @param errorCode error code
     */
    public CommonException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * Create a new CommonException.
     *
     * @param message message
     */
    public CommonException(String message) {
        super(INTERNAL_ERROR, message);
    }

    /**
     * Create a new CommonException.
     *
     * @param throwable cause
     */
    public CommonException(Throwable throwable) {
        super(INTERNAL_ERROR, throwable);
    }

    /**
     * a shortcut for new CommonException(errorCode)
     *
     * @param errorCode error code
     * @return new common exception
     */
    public static CommonException CE(ErrorCode errorCode) {
        return new CommonException(errorCode);
    }
}
