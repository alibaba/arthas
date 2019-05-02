package org.mvel2.ast;

import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;

/**
* @author Mike Brock
*/
public class InvokationContextFactory extends MapVariableResolverFactory {

    private VariableResolverFactory protoContext;

    public InvokationContextFactory(VariableResolverFactory next, VariableResolverFactory protoContext) {
        this.nextFactory = next;
        this.protoContext = protoContext;
    }

    @Override
    public VariableResolver createVariable(String name, Object value) {
        if (isResolveable(name) && !protoContext.isResolveable(name)) {
            return nextFactory.createVariable(name, value);
        } else {
            return protoContext.createVariable(name, value);
        }
    }

    @Override
    public VariableResolver createVariable(String name, Object value, Class<?> type) {
        if (isResolveable(name) && !protoContext.isResolveable(name)) {
            return nextFactory.createVariable(name, value, type);
        } else {
            return protoContext.createVariable(name, value, type);
        }
    }

    @Override
    public VariableResolver getVariableResolver(String name) {
        if (isResolveable(name) && !protoContext.isResolveable(name)) {
            return nextFactory.getVariableResolver(name);
        } else {
            return protoContext.getVariableResolver(name);
        }
    }

    @Override
    public boolean isTarget(String name) {
        return protoContext.isTarget(name);
    }

    @Override
    public boolean isResolveable(String name) {
        return protoContext.isResolveable(name) || nextFactory.isResolveable(name);
    }

    @Override
    public boolean isIndexedFactory() {
        return true;
    }
}
