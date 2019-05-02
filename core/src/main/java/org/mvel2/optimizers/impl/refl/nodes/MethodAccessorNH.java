/**
 * MVEL (The MVFLEX Expression Language)
 *
 * Copyright (C) 2007 Christopher Brock, MVFLEX/Valhalla Project and the Codehaus
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
 *
 */
package org.mvel2.optimizers.impl.refl.nodes;

import static org.mvel2.DataConversion.convert;
import static org.mvel2.util.ParseTools.getBestCandidate;

import java.lang.reflect.Method;

import org.mvel2.compiler.AccessorNode;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.PropertyHandler;
import org.mvel2.integration.VariableResolverFactory;

public class MethodAccessorNH implements AccessorNode {

    private AccessorNode nextNode;

    private Method method;
    private Class[] parameterTypes;
    private ExecutableStatement[] parms;
    private int length;
    private boolean coercionNeeded = false;

    private PropertyHandler nullHandler;

    public MethodAccessorNH() {
    }

    public MethodAccessorNH(Method method, ExecutableStatement[] parms, PropertyHandler handler) {
        this.method = method;
        this.length = (this.parameterTypes = this.method.getParameterTypes()).length;

        this.parms = parms;
        this.nullHandler = handler;
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory vars) {
        if (!coercionNeeded) {
            try {
                Object v = method.invoke(ctx, executeAll(elCtx, vars));
                if (v == null) nullHandler.getProperty(method.getName(), ctx, vars);

                if (nextNode != null) {
                    return nextNode.getValue(v, elCtx, vars);
                } else {
                    return v;
                }
            } catch (IllegalArgumentException e) {
                if (ctx != null && method.getDeclaringClass() != ctx.getClass()) {
                    Method o = getBestCandidate(parameterTypes, method.getName(), ctx.getClass(), ctx.getClass().getMethods(), true);
                    if (o != null) {
                        return executeOverrideTarget(o, ctx, elCtx, vars);
                    }
                }

                coercionNeeded = true;
                return getValue(ctx, elCtx, vars);
            } catch (Exception e) {
                throw new RuntimeException("cannot invoke method", e);
            }

        } else {
            try {
                if (nextNode != null) {
                    return nextNode.getValue(method.invoke(ctx, executeAndCoerce(parameterTypes, elCtx, vars)), elCtx, vars);
                } else {
                    return method.invoke(ctx, executeAndCoerce(parameterTypes, elCtx, vars));
                }
            } catch (Exception e) {
                throw new RuntimeException("cannot invoke method", e);
            }
        }
    }

    private Object executeOverrideTarget(Method o, Object ctx, Object elCtx, VariableResolverFactory vars) {
        try {
            Object v = o.invoke(ctx, executeAll(elCtx, vars));
            if (v == null) v = nullHandler.getProperty(o.getName(), ctx, vars);

            if (nextNode != null) {
                return nextNode.getValue(v, elCtx, vars);
            } else {
                return v;
            }
        } catch (Exception e2) {
            throw new RuntimeException("unable to invoke method", e2);
        }
    }

    private Object[] executeAll(Object ctx, VariableResolverFactory vars) {
        if (length == 0) return GetterAccessor.EMPTY;

        Object[] vals = new Object[length];
        for (int i = 0; i < length; i++) {
            vals[i] = parms[i].getValue(ctx, vars);
        }
        return vals;
    }

    private Object[] executeAndCoerce(Class[] target, Object elCtx, VariableResolverFactory vars) {
        Object[] values = new Object[length];
        for (int i = 0; i < length; i++) {
            //noinspection unchecked
            values[i] = convert(parms[i].getValue(elCtx, vars), target[i]);
        }
        return values;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
        this.length = (this.parameterTypes = this.method.getParameterTypes()).length;
    }

    public ExecutableStatement[] getParms() {
        return parms;
    }

    public void setParms(ExecutableStatement[] parms) {
        this.parms = parms;
    }

    public AccessorNode getNextNode() {
        return nextNode;
    }

    public AccessorNode setNextNode(AccessorNode nextNode) {
        return this.nextNode = nextNode;
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        return nextNode.setValue(ctx, elCtx, variableFactory, value);
    }

    public Class getKnownEgressType() {
        return method.getReturnType();
    }
}