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

public class VariableAccessor implements AccessorNode {

    private AccessorNode nextNode;
    private String property;

    public VariableAccessor(String property) {
        this.property = property;
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory vrf) {
        if (vrf == null) throw new RuntimeException("cannot access property in optimized accessor: " + property);

        if (nextNode != null) {
            return nextNode.getValue(vrf.getVariableResolver(property).getValue(), elCtx, vrf);
        } else {
            return vrf.getVariableResolver(property).getValue();
        }
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        if (nextNode != null) {
            return nextNode.setValue(variableFactory.getVariableResolver(property).getValue(), elCtx, variableFactory, value);
        } else {
            variableFactory.getVariableResolver(property).setValue(value);
        }

        return value;
    }

    public Object getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public AccessorNode getNextNode() {
        return nextNode;
    }

    public AccessorNode setNextNode(AccessorNode nextNode) {
        return this.nextNode = nextNode;
    }

    public Class getKnownEgressType() {
        return Object.class;
    }
}
