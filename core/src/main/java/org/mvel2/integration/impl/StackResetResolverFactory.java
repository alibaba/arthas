package org.mvel2.integration.impl;

import java.util.Set;

import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.VariableResolverFactory;

/**
 * @author Mike Brock
 */
public class StackResetResolverFactory implements VariableResolverFactory {

    private VariableResolverFactory delegate;

    public StackResetResolverFactory(VariableResolverFactory delegate) {
        delegate.setTiltFlag(false);
        this.delegate = delegate;
    }

    public VariableResolver createVariable(String name, Object value) {
        return delegate.createVariable(name, value);
    }

    public VariableResolver createIndexedVariable(int index, String name, Object value) {
        return delegate.createIndexedVariable(index, name, value);
    }

    public VariableResolver createVariable(String name, Object value, Class<?> type) {
        return delegate.createVariable(name, value, type);
    }

    public VariableResolver createIndexedVariable(int index, String name, Object value, Class<?> typee) {
        return delegate.createIndexedVariable(index, name, value, typee);
    }

    public VariableResolver setIndexedVariableResolver(int index, VariableResolver variableResolver) {
        return delegate.setIndexedVariableResolver(index, variableResolver);
    }

    public VariableResolverFactory getNextFactory() {
        return delegate.getNextFactory();
    }

    public VariableResolverFactory setNextFactory(VariableResolverFactory resolverFactory) {
        return delegate.setNextFactory(resolverFactory);
    }

    public VariableResolver getVariableResolver(String name) {
        return delegate.getVariableResolver(name);
    }

    public VariableResolver getIndexedVariableResolver(int index) {
        return delegate.getIndexedVariableResolver(index);
    }

    public boolean isTarget(String name) {
        return delegate.isTarget(name);
    }

    public boolean isResolveable(String name) {
        return delegate.isResolveable(name);
    }

    public Set<String> getKnownVariables() {
        return delegate.getKnownVariables();
    }

    public int variableIndexOf(String name) {
        return delegate.variableIndexOf(name);
    }

    public boolean isIndexedFactory() {
        return delegate.isIndexedFactory();
    }

    public boolean tiltFlag() {
        return delegate.tiltFlag();
    }

    public void setTiltFlag(boolean tilt) {
        if (!delegate.tiltFlag()) {
            delegate.setTiltFlag(tilt);
        }
    }

    public VariableResolverFactory getDelegate() {
        return delegate;
    }
}
