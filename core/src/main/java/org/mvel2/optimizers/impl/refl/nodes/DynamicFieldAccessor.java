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

import java.lang.reflect.Field;

import org.mvel2.DataConversion;
import org.mvel2.compiler.AccessorNode;
import org.mvel2.integration.VariableResolverFactory;

@SuppressWarnings({ "unchecked" })
public class DynamicFieldAccessor implements AccessorNode {

    private AccessorNode nextNode;
    private Field field;
    private Class targetType;

    public DynamicFieldAccessor() {
    }

    public DynamicFieldAccessor(Field field) {
        setField(field);
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory vars) {
        try {
            if (nextNode != null) {
                return nextNode.getValue(field.get(ctx), elCtx, vars);
            } else {
                return field.get(ctx);
            }
        } catch (Exception e) {
            throw new RuntimeException("unable to access field", e);
        }

    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        try {
            if (nextNode != null) {
                return nextNode.setValue(field.get(ctx), elCtx, variableFactory, value);
            } else {
                field.set(ctx, DataConversion.convert(value, targetType));
                return value;
            }
        } catch (Exception e) {
            throw new RuntimeException("unable to access field", e);
        }
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
        this.targetType = field.getType();
    }

    public AccessorNode getNextNode() {
        return nextNode;
    }

    public AccessorNode setNextNode(AccessorNode nextNode) {
        return this.nextNode = nextNode;
    }

    public Class getKnownEgressType() {
        return targetType;
    }
}
