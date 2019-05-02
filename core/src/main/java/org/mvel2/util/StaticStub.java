package org.mvel2.util;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

import org.mvel2.integration.VariableResolverFactory;

/**
 * @author Mike Brock <cbrock@redhat.com>
 */
public interface StaticStub extends Serializable {

    public Object call(Object ctx, Object thisCtx, VariableResolverFactory factory, Object[] parameters)
            throws IllegalAccessException, InvocationTargetException;
}
