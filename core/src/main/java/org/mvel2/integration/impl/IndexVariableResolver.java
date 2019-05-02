package org.mvel2.integration.impl;

import org.mvel2.integration.VariableResolver;

public class IndexVariableResolver implements VariableResolver {

    private int indexPos;
    private Object[] vars;

    public IndexVariableResolver(int indexPos, Object[] vars) {
        this.indexPos = indexPos;
        this.vars = vars;
    }

    public String getName() {
        return null;
    }

    public Class getType() {
        return null;
    }

    public void setStaticType(Class type) {
    }

    public int getFlags() {
        return 0;
    }

    public Object getValue() {
        return vars[indexPos];
    }

    public void setValue(Object value) {
        vars[indexPos] = value;
    }
}
