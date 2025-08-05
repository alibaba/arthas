package org.example.jfranalyzerbackend.exception;

/**
 * All instances of this interface should have a corresponding error code.
 */
public interface ErrorCodeAccessor {

    /**
     * @return the error code
     */
    ErrorCode getErrorCode();
}
