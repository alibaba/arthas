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

import static org.mvel2.util.ParseTools.subCompileExpression;

import java.util.List;

import org.mvel2.DataConversion;
import org.mvel2.compiler.AccessorNode;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;

public class ListAccessorNest implements AccessorNode {

    private AccessorNode nextNode;
    private ExecutableStatement index;
    private Class conversionType;

    public ListAccessorNest() {
    }

    public ListAccessorNest(String index, Class conversionType) {
        this.index = (ExecutableStatement) subCompileExpression(index.toCharArray());
        this.conversionType = conversionType;
    }

    public ListAccessorNest(ExecutableStatement index, Class conversionType) {
        this.index = index;
        this.conversionType = conversionType;
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory vars) {
        if (nextNode != null) {
            return nextNode.getValue(((List) ctx).get((Integer) index.getValue(ctx, elCtx, vars)), elCtx, vars);
        } else {
            return ((List) ctx).get((Integer) index.getValue(ctx, elCtx, vars));
        }
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory vars, Object value) {
        //noinspection unchecked

        if (nextNode != null) {
            return nextNode.setValue(((List) ctx).get((Integer) index.getValue(ctx, elCtx, vars)), elCtx, vars, value);
        } else {
            if (conversionType != null) {
                ((List) ctx).set((Integer) index.getValue(ctx, elCtx, vars), value = DataConversion.convert(value, conversionType));
            } else {
                ((List) ctx).set((Integer) index.getValue(ctx, elCtx, vars), value);
            }
            return value;
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

    public String toString() {
        return "Array Accessor -> [" + index + "]";
    }

    public Class getKnownEgressType() {
        return Object.class;
    }
}
