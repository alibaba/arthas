
package org.example.jfranalyzerbackend.enums;


import org.example.jfranalyzerbackend.exception.ErrorCode;

public enum ServerErrorCode implements ErrorCode {
    UNAVAILABLE("Unavailable"),
    USER_NOT_FOUND("User not found"),
    USERNAME_EXISTS("Username exists"),
    INCORRECT_PASSWORD("Incorrect password"),
    ACCESS_DENIED("Access denied"),
    FILE_NOT_FOUND("File not found"),
    FILE_DELETED("File deleted"),
    UNSUPPORTED_NAMESPACE("Unsupported namespace"),
    UNSUPPORTED_API("Unsupported API"),
    FILE_TRANSFER_INCOMPLETE("File transfer incomplete"),
    FILE_TYPE_MISMATCH("File type mismatch"),
    FILE_TRANSFER_METHOD_DISABLED("File transfer method disabled"),
    STATIC_WORKER_UNAVAILABLE("Static worker Unavailable"),
    ELASTIC_WORKER_NOT_READY("Elastic worker not ready"),
    ELASTIC_WORKER_STARTUP_FAILURE("Elastic worker startup failure"),
    NO_AVAILABLE_LOCATION("No available location"),
    ;

    private final String message;

    ServerErrorCode(String message) {
        this.message = message;
    }

    @Override
    public String message() {
        return message;
    }
}
