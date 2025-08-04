
package org.example.jfranalyzerbackend.exception;

/**
 * An error code is normally an enum instance.
 * Since it is not appropriate to define all error codes in a single enum class (different modules may have different error codes),
 * we define this interface to represent an error code.
 */
public interface ErrorCode {
    /**
     * @return the identifier of this error code
     */
    default String identifier() {
        return name();
    }

    /**
     * @return the name of this error code
     */
    String name();

    /**
     * @return the message of this error code
     */
    String message();
}
