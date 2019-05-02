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

import static org.mvel2.util.ParseTools.getBestCandidate;
import static org.mvel2.util.ParseTools.getWidenedTarget;

import java.lang.reflect.Method;

import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;

public class MethodAccessor extends InvokableAccessor {

    private Method method;

    public MethodAccessor() {
    }

    public MethodAccessor(Method method, ExecutableStatement[] parms) {
        setMethod(method);
        this.parms = parms;
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory vars) {
        if (!coercionNeeded) {
            try {
                if (nextNode != null) {
                    return nextNode.getValue(method.invoke(ctx, executeAll(elCtx, vars, method)), elCtx, vars);
                } else {
                    return method.invoke(ctx, executeAll(elCtx, vars, method));
                }
            } catch (IllegalArgumentException e) {
                if (ctx != null && method.getDeclaringClass() != ctx.getClass()) {
                    Method o = getBestCandidate(parameterTypes, method.getName(), ctx.getClass(), ctx.getClass().getMethods(), true);
                    if (o != null) {
                        return executeOverrideTarget(getWidenedTarget(o), ctx, elCtx, vars);
                    }
                }

                coercionNeeded = true;
                return getValue(ctx, elCtx, vars);
            } catch (Exception e) {
                throw new RuntimeException("cannot invoke method: " + method.getName(), e);
            }

        } else {
            try {
                if (nextNode != null) {
                    return nextNode.getValue(method.invoke(ctx, executeAndCoerce(parameterTypes, elCtx, vars, method.isVarArgs())), elCtx,
                            vars);
                } else {
                    return method.invoke(ctx, executeAndCoerce(parameterTypes, elCtx, vars, method.isVarArgs()));
                }
            } catch (IllegalArgumentException e) {
                Object[] vs = executeAndCoerce(parameterTypes, elCtx, vars, false);
                Method newMeth;
                if ((newMeth = getWidenedTarget(
                        getBestCandidate(vs, method.getName(), ctx.getClass(), ctx.getClass().getMethods(), false))) != null) {
                    return executeOverrideTarget(newMeth, ctx, elCtx, vars);
                } else {
                    throw e;
                }
            } catch (Exception e) {
                throw new RuntimeException("cannot invoke method: " + method.getName(), e);
            }
        }
    }

    private Object executeOverrideTarget(Method o, Object ctx, Object elCtx, VariableResolverFactory vars) {
        // this local field is required to make sure exception block works with the same coercionNeeded value
        // and it is not changed by another thread while setter is invoked
        boolean attemptedCoercion = coercionNeeded;
        if (!coercionNeeded) {
            try {
                try {
                    if (nextNode != null) {
                        return nextNode.getValue(o.invoke(ctx, executeAll(elCtx, vars, o)), elCtx, vars);
                    } else {
                        return o.invoke(ctx, executeAll(elCtx, vars, o));
                    }
                } catch (IllegalArgumentException e) {
                    if (attemptedCoercion) throw e;

                    coercionNeeded = true;
                    return executeOverrideTarget(o, ctx, elCtx, vars);
                }
            } catch (Exception e2) {
                throw new RuntimeException("unable to invoke method", e2);
            }
        } else {
            try {
                if (nextNode != null) {
                    return nextNode.getValue(o.invoke(ctx, executeAndCoerce(o.getParameterTypes(), elCtx, vars, o.isVarArgs())), elCtx,
                            vars);
                } else {
                    return o.invoke(ctx, executeAndCoerce(o.getParameterTypes(), elCtx, vars, o.isVarArgs()));
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("unable to invoke method (expected target: " + method.getDeclaringClass().getName() + "::"
                        + method.getName() + "; " + "actual target: " + ctx.getClass().getName() + "::" + method.getName()
                        + "; coercionNeeded=" + (coercionNeeded ? "yes" : "no") + ")");
            } catch (Exception e2) {
                throw new RuntimeException("unable to invoke method (expected target: " + method.getDeclaringClass().getName() + "::"
                        + method.getName() + "; " + "actual target: " + ctx.getClass().getName() + "::" + method.getName()
                        + "; coercionNeeded=" + (coercionNeeded ? "yes" : "no") + ")");
            }
        }
    }

    private Object[] executeAll(Object ctx, VariableResolverFactory vars, Method m) {
        if (length == 0) return GetterAccessor.EMPTY;

        Object[] vals = new Object[length];
        for (int i = 0; i < length - (m.isVarArgs() ? 1 : 0); i++) {
            vals[i] = parms[i].getValue(ctx, vars);
        }

        if (m.isVarArgs()) {
            if (parms == null) {
                vals[length - 1] = new Object[0];
            } else if (parms.length == length) {
                Object lastParam = parms[length - 1].getValue(ctx, vars);
                vals[length - 1] = lastParam == null || lastParam.getClass().isArray() ? lastParam : new Object[] { lastParam };
            } else {
                Object[] vararg = new Object[parms.length - length + 1];
                for (int i = 0; i < vararg.length; i++)
                    vararg[i] = parms[length - 1 + i].getValue(ctx, vars);
                vals[length - 1] = vararg;
            }
        }

        return vals;
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

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        try {
            return nextNode.setValue(method.invoke(ctx, executeAll(elCtx, variableFactory, method)), elCtx, variableFactory, value);
        } catch (IllegalArgumentException e) {
            if (ctx != null && method.getDeclaringClass() != ctx.getClass()) {
                Method o = getBestCandidate(parameterTypes, method.getName(), ctx.getClass(), ctx.getClass().getMethods(), true);
                if (o != null) {
                    return nextNode.setValue(executeOverrideTarget(o, ctx, elCtx, variableFactory), elCtx, variableFactory, value);
                }
            }

            coercionNeeded = true;
            return setValue(ctx, elCtx, variableFactory, value);
        } catch (Exception e) {
            throw new RuntimeException("cannot invoke method", e);
        }
    }

    public Class getKnownEgressType() {
        return method.getReturnType();
    }
}
