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

import java.lang.reflect.Array;

import org.mvel2.compiler.AccessorNode;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.util.ParseTools;

public class ArrayAccessorNest implements AccessorNode {

    private AccessorNode nextNode;
    private ExecutableStatement index;

    private Class baseComponentType;
    private boolean requireConversion;

    public ArrayAccessorNest() {
    }

    public ArrayAccessorNest(String index) {
        this.index = (ExecutableStatement) ParseTools.subCompileExpression(index.toCharArray());
    }

    public ArrayAccessorNest(ExecutableStatement stmt) {
        this.index = stmt;
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory vars) {
        if (nextNode != null) {
            return nextNode.getValue(((Object[]) ctx)[(Integer) index.getValue(ctx, elCtx, vars)], elCtx, vars);
        } else {
            return ((Object[]) ctx)[(Integer) index.getValue(ctx, elCtx, vars)];
        }
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory vars, Object value) {
        if (nextNode != null) {
            return nextNode.setValue(((Object[]) ctx)[(Integer) index.getValue(ctx, elCtx, vars)], elCtx, vars, value);
        } else {
            if (baseComponentType == null) {
                baseComponentType = ParseTools.getBaseComponentType(ctx.getClass());
                requireConversion = baseComponentType != value.getClass() && !baseComponentType.isAssignableFrom(value.getClass());
            }

            if (requireConversion) {
                Object o = convert(value, baseComponentType);
                Array.set(ctx, (Integer) index.getValue(ctx, elCtx, vars), o);
                return o;
            } else {
                Array.set(ctx, (Integer) index.getValue(ctx, elCtx, vars), value);
                return value;
            }
        }
    }

    public ExecutableStatement getIndex() {
        return index;
    }

    public void setIndex(ExecutableStatement index) {
        this.index = index;
    }

    public AccessorNode getNextNode() {
        return nextNode;
    }

    public AccessorNode setNextNode(AccessorNode nextNode) {
        return this.nextNode = nextNode;
    }

    public Class getKnownEgressType() {
        return baseComponentType;
    }

    public String toString() {
        return "Array Accessor -> [" + index + "]";
    }
}
