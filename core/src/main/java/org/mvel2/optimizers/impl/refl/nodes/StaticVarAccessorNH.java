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

import org.mvel2.OptimizationFailure;
import org.mvel2.compiler.AccessorNode;
import org.mvel2.integration.PropertyHandler;
import org.mvel2.integration.VariableResolverFactory;

public class StaticVarAccessorNH implements AccessorNode {

    Field field;
    private AccessorNode nextNode;
    private PropertyHandler nullHandler;

    public StaticVarAccessorNH(Field field, PropertyHandler handler) {
        this.field = field;
        this.nullHandler = handler;
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory vars) {
        try {
            Object v = field.get(ctx);
            if (v == null) v = nullHandler.getProperty(field.getName(), elCtx, vars);

            if (nextNode != null) {
                return nextNode.getValue(v, elCtx, vars);
            } else {
                return v;
            }
        } catch (Exception e) {
            throw new OptimizationFailure("unable to access static field", e);
        }
    }

    public AccessorNode getNextNode() {
        return nextNode;
    }

    public AccessorNode setNextNode(AccessorNode nextNode) {
        return this.nextNode = nextNode;
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        try {
            if (nextNode == null) {
                field.set(null, value);
            } else {
                return nextNode.setValue(field.get(null), elCtx, variableFactory, value);
            }
        } catch (Exception e) {
            throw new RuntimeException("error accessing static variable", e);
        }
        return value;
    }

    public Class getKnownEgressType() {
        return field.getClass();
    }
}