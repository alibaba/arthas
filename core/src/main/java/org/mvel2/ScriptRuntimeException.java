package org.mvel2;

/**
 * @author Mike Brock .
 */
public class ScriptRuntimeException extends RuntimeException {

    public ScriptRuntimeException() {
    }

    public ScriptRuntimeException(String message) {
        super(message);
    }

    public ScriptRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ScriptRuntimeException(Throwable cause) {
        super(cause);
    }
}
