package org.example.jfranalyzerbackend.exception;

public class ProfileAnalysisException extends Exception{
    public ProfileAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProfileAnalysisException(Throwable cause) {
        super(cause);
    }

    public ProfileAnalysisException(String message) {
        super(message);
    }
}
