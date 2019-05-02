package org.mvel2.ast;

import org.mvel2.integration.VariableResolverFactory;

/**
 * @author Mike Brock
 */
public class FunctionInstance {

    protected final Function function;

    public FunctionInstance(Function function) {
        this.function = function;
    }

    public Function getFunction() {
        return function;
    }

    public Object call(Object ctx, Object thisValue, VariableResolverFactory factory, Object[] parms) {
        return function.call(ctx, thisValue, factory, parms);
    }
}
