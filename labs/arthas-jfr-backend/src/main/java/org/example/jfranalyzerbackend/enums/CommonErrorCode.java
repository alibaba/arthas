package org.example.jfranalyzerbackend.enums;


import org.example.jfranalyzerbackend.exception.ErrorCode;

public enum CommonErrorCode implements ErrorCode {
    ILLEGAL_ARGUMENT("Illegal argument"),

    VALIDATION_FAILURE("Validation failure"),

    INTERNAL_ERROR("Internal error");

    private final String message;

    CommonErrorCode(String message) {
        this.message = message;
    }

    @Override
    public String message() {
        return message;
    }
}
