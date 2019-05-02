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

import org.mvel2.compiler.AccessorNode;
import org.mvel2.integration.VariableResolverFactory;

public class IndexedCharSeqAccessor implements AccessorNode {

    private AccessorNode nextNode;

    private int index;

    public IndexedCharSeqAccessor() {
    }

    public IndexedCharSeqAccessor(int index) {
        this.index = index;
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory vars) {
        if (nextNode != null) {
            return nextNode.getValue(((String) ctx).charAt(index), elCtx, vars);
        } else {
            return ((String) ctx).charAt(index);
        }
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        return nextNode.setValue(((String) ctx).charAt(index), elCtx, variableFactory, value);
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
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
        return Character.class;
    }
}
