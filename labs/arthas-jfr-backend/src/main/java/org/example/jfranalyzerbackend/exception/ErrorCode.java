
package org.example.jfranalyzerbackend.exception;


public interface ErrorCode {

    default String identifier() {
        return name();
    }

    String name();

    String message();
}
