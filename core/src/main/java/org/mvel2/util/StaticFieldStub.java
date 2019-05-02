package org.mvel2.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.mvel2.integration.VariableResolverFactory;

/**
 * @author Mike Brock <cbrock@redhat.com>
 */
public class StaticFieldStub implements StaticStub {

    private final Field field;
    private final Object cachedValue;

    public StaticFieldStub(Field field) {
        this.field = field;

        if (!field.isAccessible() || (field.getModifiers() & Modifier.STATIC) == 0) {
            throw new RuntimeException("not an accessible static field: " + field.getDeclaringClass().getName() + "." + field.getName());
        }

        try {
            cachedValue = field.get(null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("error accessing static field", e);
        }
    }

    public Object call(Object ctx, Object thisCtx, VariableResolverFactory factory, Object[] parameters) {
        return cachedValue;
    }
}
