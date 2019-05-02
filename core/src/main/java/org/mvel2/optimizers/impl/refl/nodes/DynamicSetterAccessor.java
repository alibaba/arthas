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

import java.lang.reflect.Method;

import org.mvel2.compiler.AccessorNode;
import org.mvel2.integration.VariableResolverFactory;

@SuppressWarnings({ "unchecked" })
public class DynamicSetterAccessor implements AccessorNode {
    //  private AccessorNode nextNode;

    public static final Object[] EMPTY = new Object[0];
    private final Method method;
    private Class targetType;

    public DynamicSetterAccessor(Method method) {
        this.method = method;
        this.targetType = method.getParameterTypes()[0];
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        try {
            return method.invoke(ctx, convert(value, targetType));
        } catch (Exception e) {
            throw new RuntimeException("error binding property", e);
        }
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory vars) {
        return null;
    }

    public Method getMethod() {
        return method;
    }

    public AccessorNode setNextNode(AccessorNode nextNode) {
        return null;
    }

    public AccessorNode getNextNode() {
        return null;
    }

    public String toString() {
        return method.getDeclaringClass().getName() + "." + method.getName();
    }

    public Class getKnownEgressType() {
        return targetType;
    }
}
