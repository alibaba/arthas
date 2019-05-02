package org.mvel2.integration.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mvel2.UnresolveablePropertyException;
import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.VariableResolverFactory;

public class CachedMapVariableResolverFactory extends BaseVariableResolverFactory {

    /**
     * Holds the instance of the variables.
     */
    protected Map<String, Object> variables;

    public CachedMapVariableResolverFactory() {
    }

    public CachedMapVariableResolverFactory(Map<String, Object> variables) {
        this.variables = variables;
        variableResolvers = new HashMap<String, VariableResolver>(variables.size() * 2);

        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            variableResolvers.put(entry.getKey(), new PrecachedMapVariableResolver(entry, entry.getKey()));
        }

    }

    public CachedMapVariableResolverFactory(Map<String, Object> variables, VariableResolverFactory nextFactory) {
        this.variables = variables;
        variableResolvers = new HashMap<String, VariableResolver>(variables.size() * 2);

        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            variableResolvers.put(entry.getKey(), new PrecachedMapVariableResolver(entry, entry.getKey()));
        }

        this.nextFactory = nextFactory;
    }

    public VariableResolver createVariable(String name, Object value) {
        VariableResolver vr;

        try {
            (vr = getVariableResolver(name)).setValue(value);
            return vr;
        } catch (UnresolveablePropertyException e) {
            addResolver(name, vr = new MapVariableResolver(variables, name)).setValue(value);
            return vr;
        }
    }

    public VariableResolver createVariable(String name, Object value, Class<?> type) {
        VariableResolver vr;
        try {
            vr = getVariableResolver(name);
        } catch (UnresolveablePropertyException e) {
            vr = null;
        }

        if (vr != null && vr.getType() != null) {
            throw new RuntimeException("variable already defined within scope: " + vr.getType() + " " + name);
        } else {
            addResolver(name, vr = new MapVariableResolver(variables, name, type)).setValue(value);
            return vr;
        }
    }

    public VariableResolver getVariableResolver(String name) {

        VariableResolver vr = variableResolvers.get(name);
        if (vr != null) {
            return vr;
        } else if (variables.containsKey(name)) {
            variableResolvers.put(name, vr = new MapVariableResolver(variables, name));
            return vr;
        } else if (nextFactory != null) {
            return nextFactory.getVariableResolver(name);
        }

        throw new UnresolveablePropertyException("unable to resolve variable '" + name + "'");
    }

    public boolean isResolveable(String name) {
        return (variableResolvers != null && variableResolvers.containsKey(name)) || (variables != null && variables.containsKey(name))
                || (nextFactory != null && nextFactory.isResolveable(name));
    }

    protected VariableResolver addResolver(String name, VariableResolver vr) {
        if (variableResolvers == null) variableResolvers = new HashMap<String, VariableResolver>();
        variableResolvers.put(name, vr);
        return vr;
    }

    public boolean isTarget(String name) {
        return variableResolvers != null && variableResolvers.containsKey(name);
    }

    public Set<String> getKnownVariables() {
        if (nextFactory == null) {
            if (variables != null) return new HashSet<String>(variables.keySet());
            return new HashSet<String>(0);
        } else {
            if (variables != null) return new HashSet<String>(variables.keySet());
            return new HashSet<String>(0);
        }
    }
}
