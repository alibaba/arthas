/**
 * MVEL 2.0
 * Copyright (C) 2007 The Codehaus
 * Mike Brock, Dhanji Prasanna, John Graham, Mark Proctor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel2.integration.impl;

import java.util.HashMap;

import org.mvel2.ast.Function;
import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.VariableResolverFactory;

public class FunctionVariableResolverFactory extends BaseVariableResolverFactory implements LocalVariableResolverFactory {

    private Function function;
    private boolean noTilt = false;

    public FunctionVariableResolverFactory(Function function, VariableResolverFactory nextFactory, String[] indexedVariables,
            Object[] parameters) {
        this.function = function;

        this.variableResolvers = new HashMap<String, VariableResolver>();
        this.nextFactory = nextFactory;
        this.indexedVariableResolvers = new VariableResolver[(this.indexedVariableNames = indexedVariables).length];
        for (int i = 0; i < parameters.length; i++) {
            variableResolvers.put(indexedVariableNames[i], null);
            this.indexedVariableResolvers[i] = new SimpleValueResolver(parameters[i]);
            //     variableResolvers.put(indexedVariableNames[i], this.indexedVariableResolvers[i] = new SimpleValueResolver(parameters[i]));
        }
    }

    public boolean isResolveable(String name) {
        return variableResolvers.containsKey(name) || (nextFactory != null && nextFactory.isResolveable(name));
    }

    public VariableResolver createVariable(String name, Object value) {
        VariableResolver resolver = getVariableResolver(name);
        if (resolver == null) {
            int idx = increaseRegisterTableSize();
            this.indexedVariableNames[idx] = name;
            this.indexedVariableResolvers[idx] = new SimpleValueResolver(value);
            variableResolvers.put(name, null);

            //     variableResolvers.put(name, this.indexedVariableResolvers[idx] = new SimpleValueResolver(value));
            return this.indexedVariableResolvers[idx];
        } else {
            resolver.setValue(value);
            return resolver;
        }
    }

    public VariableResolver createVariable(String name, Object value, Class<?> type) {
        VariableResolver vr = this.variableResolvers != null ? this.variableResolvers.get(name) : null;
        if (vr != null && vr.getType() != null) {
            throw new RuntimeException("variable already defined within scope: " + vr.getType() + " " + name);
        } else {
            return createIndexedVariable(variableIndexOf(name), name, value);
        }
    }

    public VariableResolver createIndexedVariable(int index, String name, Object value) {
        index = index - indexOffset;
        if (indexedVariableResolvers[index] != null) {
            indexedVariableResolvers[index].setValue(value);
        } else {
            indexedVariableResolvers[index] = new SimpleValueResolver(value);
        }

        variableResolvers.put(name, null);

        return indexedVariableResolvers[index];
    }

    public VariableResolver createIndexedVariable(int index, String name, Object value, Class<?> type) {
        index = index - indexOffset;
        if (indexedVariableResolvers[index] != null) {
            indexedVariableResolvers[index].setValue(value);
        } else {
            indexedVariableResolvers[index] = new SimpleValueResolver(value);
        }
        return indexedVariableResolvers[index];
    }

    public VariableResolver getIndexedVariableResolver(int index) {
        if (indexedVariableResolvers[index] == null) {
            /**
             * If the register is null, this means we need to forward-allocate the variable onto the
             * register table.
             */
            return indexedVariableResolvers[index] = super.getVariableResolver(indexedVariableNames[index]);
        }
        return indexedVariableResolvers[index];
    }

    public VariableResolver getVariableResolver(String name) {
        int idx;
        //   if (variableResolvers.containsKey(name)) return variableResolvers.get(name);
        if ((idx = variableIndexOf(name)) != -1) {
            if (indexedVariableResolvers[idx] == null) {
                indexedVariableResolvers[idx] = new SimpleValueResolver(null);
            }
            variableResolvers.put(indexedVariableNames[idx], null);
            return indexedVariableResolvers[idx];
        }

        return super.getVariableResolver(name);
    }

    public boolean isIndexedFactory() {
        return true;
    }

    public boolean isTarget(String name) {
        return variableResolvers.containsKey(name) || variableIndexOf(name) != -1;
    }

    private int increaseRegisterTableSize() {
        String[] oldNames = indexedVariableNames;
        VariableResolver[] oldResolvers = indexedVariableResolvers;

        int newLength = oldNames.length + 1;
        indexedVariableNames = new String[newLength];
        indexedVariableResolvers = new VariableResolver[newLength];

        for (int i = 0; i < oldNames.length; i++) {
            indexedVariableNames[i] = oldNames[i];
            indexedVariableResolvers[i] = oldResolvers[i];
        }

        return newLength - 1;
    }

    public void updateParameters(Object[] parameters) {
        //    this.indexedVariableResolvers = new VariableResolver[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            this.indexedVariableResolvers[i] = new SimpleValueResolver(parameters[i]);
        }
        //        for (int i = parameters.length; i < indexedVariableResolvers.length; i++) {
        //            this.indexedVariableResolvers[i] = null;
        //        }
    }

    public VariableResolver[] getIndexedVariableResolvers() {
        return this.indexedVariableResolvers;
    }

    public void setIndexedVariableResolvers(VariableResolver[] vr) {
        this.indexedVariableResolvers = vr;
    }

    public Function getFunction() {
        return function;
    }

    public void setIndexOffset(int offset) {
        this.indexOffset = offset;
    }

    public VariableResolverFactory setNoTilt(boolean noTilt) {
        this.noTilt = noTilt;
        return this;
    }

    @Override
    public void setTiltFlag(boolean tiltFlag) {
        if (!noTilt) super.setTiltFlag(tiltFlag);
    }
}