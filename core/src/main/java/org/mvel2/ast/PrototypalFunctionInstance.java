package org.mvel2.ast;

import org.mvel2.integration.VariableResolverFactory;

/**
 * @author Mike Brock
 */
public class PrototypalFunctionInstance extends FunctionInstance {

    private final VariableResolverFactory resolverFactory;

    public PrototypalFunctionInstance(Function function, VariableResolverFactory resolverFactory) {
        super(function);
        this.resolverFactory = resolverFactory;
    }

    @Override
    public Object call(Object ctx, Object thisValue, VariableResolverFactory factory, Object[] parms) {
        return function.call(ctx, thisValue, new InvokationContextFactory(factory, resolverFactory), parms);
    }

    public VariableResolverFactory getResolverFactory() {
        return resolverFactory;
    }

    public String toString() {
        return "function_prototype:" + function.getName();
    }

}
