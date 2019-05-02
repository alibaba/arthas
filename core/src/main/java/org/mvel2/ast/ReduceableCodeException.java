package org.mvel2.ast;

public class ReduceableCodeException extends RuntimeException {

    private Object literal;

    public ReduceableCodeException(Object literal) {
        this.literal = literal;
    }

    public Object getLiteral() {
        return literal;
    }
}
