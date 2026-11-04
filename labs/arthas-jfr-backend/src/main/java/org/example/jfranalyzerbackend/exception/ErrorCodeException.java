package org.example.jfranalyzerbackend.exception;


public class ErrorCodeException extends RuntimeException implements ErrorCodeAccessor {


    private final ErrorCode errorCode;

    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public ErrorCodeException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }


    public ErrorCodeException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }


    public ErrorCodeException(ErrorCode errorCode, Throwable cause) {
        super(cause);
        this.errorCode = errorCode;
    }


    public ErrorCodeException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }



}
