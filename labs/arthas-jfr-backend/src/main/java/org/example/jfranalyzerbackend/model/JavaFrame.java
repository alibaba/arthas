
package org.example.jfranalyzerbackend.model;

import lombok.Getter;
import lombok.Setter;

public class JavaFrame extends Frame {
    public enum Type {
        INTERPRETER("Interpreted"),
        JIT("JIT compiled"),
        INLINE("Inlined"),
        NATIVE("Native");

        final String value;

        Type(String value) {
            this.value = value;
        }

        public static Type typeOf(String value) {
            for (Type type : Type.values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException(value);
        }
    }

    private boolean isJavaFrame;

    @Setter
    @Getter
    private Type type;

    @Setter
    @Getter
    private long bci = -1;

    public boolean isJavaFrame() {
        return isJavaFrame;
    }

    public void setJavaFrame(boolean javaFrame) {
        isJavaFrame = javaFrame;
    }
}
