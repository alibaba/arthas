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
import java.util.Map;

import org.mvel2.UnresolveablePropertyException;
import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.VariableResolverFactory;

public class DefaultLocalVariableResolverFactory extends MapVariableResolverFactory implements LocalVariableResolverFactory {

    private boolean noTilt = false;

    public DefaultLocalVariableResolverFactory() {
        super(new HashMap<String, Object>());
    }

    public DefaultLocalVariableResolverFactory(Map<String, Object> variables) {
        super(variables);
    }

    public DefaultLocalVariableResolverFactory(Map<String, Object> variables, VariableResolverFactory nextFactory) {
        super(variables, nextFactory);
    }

    public DefaultLocalVariableResolverFactory(Map<String, Object> variables, boolean cachingSafe) {
        super(variables);
    }

    public DefaultLocalVariableResolverFactory(VariableResolverFactory nextFactory) {
        super(new HashMap<String, Object>(), nextFactory);
    }

    public DefaultLocalVariableResolverFactory(VariableResolverFactory nextFactory, String[] indexedVariables) {
        super(new HashMap<String, Object>(), nextFactory);
        this.indexedVariableNames = indexedVariables;
        this.indexedVariableResolvers = new VariableResolver[indexedVariables.length];
    }

    public VariableResolver getIndexedVariableResolver(int index) {
        if (indexedVariableNames == null) return null;

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
        if (indexedVariableNames == null) return super.getVariableResolver(name);

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

    public VariableResolver createVariable(String name, Object value, Class<?> type) {
        if (indexedVariableNames == null) return super.createVariable(name, value, type);

        VariableResolver vr;
        boolean newVar = false;

        try {
            int idx;
            if ((idx = variableIndexOf(name)) != -1) {
                vr = new SimpleValueResolver(value);
                if (indexedVariableResolvers[idx] == null) {
                    indexedVariableResolvers[idx] = vr;
                }
                variableResolvers.put(indexedVariableNames[idx], vr);
                vr = indexedVariableResolvers[idx];

                newVar = true;
            } else {
                return super.createVariable(name, value, type);
            }

        } catch (UnresolveablePropertyException e) {
            vr = null;
        }

        if (!newVar && vr != null && vr.getType() != null) {
            throw new RuntimeException("variable already defined within scope: " + vr.getType() + " " + name);
        } else {
            addResolver(name, vr = new MapVariableResolver(variables, name, type)).setValue(value);
            return vr;
        }
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
