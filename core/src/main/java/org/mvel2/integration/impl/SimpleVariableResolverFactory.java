package org.mvel2.integration.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.mvel2.integration.VariableResolver;

public class SimpleVariableResolverFactory extends BaseVariableResolverFactory {

    public SimpleVariableResolverFactory(Map<String, Object> variables) {
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            createVariable(entry.getKey(), entry.getValue());
        }
    }

    public VariableResolver createVariable(String name, Object value) {
        if (variableResolvers == null) variableResolvers = new HashMap<String, VariableResolver>(5, 0.6f);
        SimpleValueResolver svr = new SimpleValueResolver(value);
        variableResolvers.put(name, svr);
        return svr;
    }

    public VariableResolver createIndexedVariable(int index, String name, Object value) {
        return null;
    }

    public VariableResolver createVariable(String name, Object value, Class<?> type) {
        if (variableResolvers == null) variableResolvers = new HashMap<String, VariableResolver>(5, 0.6f);
        SimpleSTValueResolver svr = new SimpleSTValueResolver(value, type);
        variableResolvers.put(name, svr);
        return svr;
    }

    public VariableResolver createIndexedVariable(int index, String name, Object value, Class<?> typee) {
        return null;
    }

    public VariableResolver setIndexedVariableResolver(int index, VariableResolver variableResolver) {
        return null;
    }

    public boolean isTarget(String name) {
        return variableResolvers.containsKey(name);
    }

    public boolean isResolveable(String name) {
        return variableResolvers.containsKey(name) || (nextFactory != null && nextFactory.isResolveable(name));
    }

    @Override
    public VariableResolver getVariableResolver(String name) {
        VariableResolver vr = variableResolvers.get(name);
        return vr != null ? vr : (nextFactory == null ? null : nextFactory.getVariableResolver(name));
    }

    public Set<String> getKnownVariables() {
        return variableResolvers.keySet();
    }

    public int variableIndexOf(String name) {
        return 0; //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isIndexedFactory() {
        return false; //To change body of implemented methods use File | Settings | File Templates.
    }
}
