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

import static org.mvel2.MVEL.getProperty;
import static org.mvel2.util.ParseTools.getBestCandidate;

import java.lang.reflect.Method;

import org.mvel2.CompileException;
import org.mvel2.compiler.AccessorNode;
import org.mvel2.integration.PropertyHandler;
import org.mvel2.integration.VariableResolverFactory;

public class GetterAccessorNH implements AccessorNode {

    public static final Object[] EMPTY = new Object[0];
    private final Method method;
    private AccessorNode nextNode;
    private PropertyHandler nullHandler;

    public GetterAccessorNH(Method method, PropertyHandler nullHandler) {
        this.method = method;
        this.nullHandler = nullHandler;
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory vars) {
        try {
            return nullHandle(method.getName(), method.invoke(ctx, EMPTY), ctx, elCtx, vars);
        } catch (IllegalArgumentException e) {
            if (ctx != null && method.getDeclaringClass() != ctx.getClass()) {
                Method o = getBestCandidate(EMPTY, method.getName(), ctx.getClass(), ctx.getClass().getMethods(), true);
                if (o != null) {
                    return executeOverrideTarget(o, ctx, elCtx, vars);
                }
            }
            /**
             * HACK: Try to access this another way.
             */

            return nullHandle(method.getName(), getProperty(method.getName() + "()", ctx), ctx, elCtx, vars);
        } catch (Exception e) {
            throw new RuntimeException("cannot invoke getter: " + method.getName() + " [declr.class: "
                    + method.getDeclaringClass().getName() + "; act.class: " + (ctx != null ? ctx.getClass().getName() : "null") + "]", e);
        }
    }

    public Method getMethod() {
        return method;
    }

    public AccessorNode setNextNode(AccessorNode nextNode) {
        return this.nextNode = nextNode;
    }

    public AccessorNode getNextNode() {
        return nextNode;
    }

    public String toString() {
        return method.getDeclaringClass().getName() + "." + method.getName();
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory vars, Object value) {
        try {
            Object v = method.invoke(ctx, EMPTY);
            if (v == null) v = nullHandler.getProperty(method.getName(), ctx, vars);
            return nextNode.setValue(v, elCtx, vars, value);

        } catch (IllegalArgumentException e) {
            /**
             * HACK: Try to access this another way.
             */

            Object v = getProperty(method.getName() + "()", ctx);
            if (v == null) v = nullHandler.getProperty(method.getName(), ctx, vars);
            return nextNode.setValue(v, elCtx, vars, value);
        } catch (CompileException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("error " + method.getName() + ": " + e.getClass().getName() + ":" + e.getMessage(), e);
        }
    }

    public Class getKnownEgressType() {
        return method.getReturnType();
    }

    private Object executeOverrideTarget(Method o, Object ctx, Object elCtx, VariableResolverFactory vars) {
        try {
            return nullHandle(o.getName(), o.invoke(ctx, EMPTY), ctx, elCtx, vars);
        } catch (Exception e2) {
            throw new RuntimeException("unable to invoke method", e2);
        }
    }

    private Object nullHandle(String name, Object v, Object ctx, Object elCtx, VariableResolverFactory vars) {
        if (v != null) {
            if (nextNode != null) {
                return nextNode.getValue(v, elCtx, vars);
            } else {
                return v;
            }
        } else {
            if (nextNode != null) {
                return nextNode.getValue(nullHandler.getProperty(name, ctx, vars), elCtx, vars);
            } else {
                return nullHandler.getProperty(name, ctx, vars);
            }
        }
    }
}