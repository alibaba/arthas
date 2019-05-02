package org.mvel2.integration.impl;

import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.VariableResolverFactory;

/**
 * @author Mike Brock
 */
public class StackDelimiterResolverFactory extends StackDemarcResolverFactory {

    public StackDelimiterResolverFactory(VariableResolverFactory delegate) {
        super(delegate);
    }

    public VariableResolver createVariable(String name, Object value) {
        VariableResolverFactory delegate = getDelegate();
        VariableResolverFactory nextFactory = delegate.getNextFactory();
        delegate.setNextFactory(null);
        VariableResolver resolver = delegate.createVariable(name, value);
        delegate.setNextFactory(nextFactory);
        return resolver;
    }
}
