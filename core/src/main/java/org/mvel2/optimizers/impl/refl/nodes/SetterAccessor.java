package org.mvel2.optimizers.impl.refl.nodes;

import static org.mvel2.DataConversion.convert;
import static org.mvel2.util.ParseTools.getBestCandidate;

import java.lang.reflect.Method;

import org.mvel2.compiler.AccessorNode;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.util.PropertyTools;

public class SetterAccessor implements AccessorNode {

    public static final Object[] EMPTY = new Object[0];
    private final Method method;
    private AccessorNode nextNode;
    private Class<?> targetType;
    private boolean primitive;
    private boolean coercionRequired = false;

    public SetterAccessor(Method method) {
        this.method = method;
        assert method != null;
        primitive = (this.targetType = method.getParameterTypes()[0]).isPrimitive();
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        // this local field is required to make sure exception block works with the same coercionRequired value
        // and it is not changed by another thread while setter is invoked
        boolean attemptedCoercion = coercionRequired;
        try {
            if (coercionRequired) {
                return method.invoke(ctx, convert(value, targetType));
            } else {
                return method.invoke(ctx, value == null && primitive ? PropertyTools.getPrimitiveInitialValue(targetType) : value);
            }
        } catch (IllegalArgumentException e) {
            if (ctx != null && method.getDeclaringClass() != ctx.getClass()) {
                Method o = getBestCandidate(EMPTY, method.getName(), ctx.getClass(), ctx.getClass().getMethods(), true);
                if (o != null) {
                    return executeOverrideTarget(o, ctx, value);
                }
            }

            if (!attemptedCoercion) {
                coercionRequired = true;
                return setValue(ctx, elCtx, variableFactory, value);
            }
            throw new RuntimeException("unable to bind property", e);
        } catch (Exception e) {
            throw new RuntimeException("error calling method: " + method.getDeclaringClass().getName() + "." + method.getName(), e);
        }
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory vars) {
        return null;
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

    public Class getKnownEgressType() {
        return method.getReturnType();
    }

    private Object executeOverrideTarget(Method o, Object ctx, Object value) {
        try {
            return o.invoke(ctx, convert(value, targetType));
        } catch (Exception e2) {
            throw new RuntimeException("unable to invoke method", e2);
        }
    }
}
