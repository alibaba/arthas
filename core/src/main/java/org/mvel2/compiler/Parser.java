package org.mvel2.compiler;

/**
 * @author Mike Brock .
 */
public interface Parser {

    public int getCursor();

    public char[] getExpression();
}
