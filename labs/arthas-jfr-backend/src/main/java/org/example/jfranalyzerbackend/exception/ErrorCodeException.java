package org.example.jfranalyzerbackend.exception;

/**
 * The super class of all exceptions that have an {@link ErrorCode}.
 */
public class ErrorCodeException extends RuntimeException implements ErrorCodeAccessor {

    /**
     * error code
     */
    private final ErrorCode errorCode;

    /**
     * Create a new ErrorCodeException with a specified error code
     *
     * @param errorCode error code
     */
    public ErrorCodeException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    /**
     * Create a new ErrorCodeException with a specified error code and a message
     *
     * @param errorCode error code
     * @param message   message
     */
    public ErrorCodeException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Create a new ErrorCodeException with a specified error code and a cause
     *
     * @param errorCode error code
     * @param cause     cause
     */
    public ErrorCodeException(ErrorCode errorCode, Throwable cause) {
        super(cause);
        this.errorCode = errorCode;
    }

    /**
     * Create a new ErrorCodeException with a specified error code, a message and a cause
     *
     * @param errorCode error code
     * @param message   message
     * @param cause     cause
     */
    public ErrorCodeException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * @return the error code
     */
    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
