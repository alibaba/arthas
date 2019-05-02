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

import java.util.Map;

import org.mvel2.compiler.AccessorNode;
import org.mvel2.integration.VariableResolverFactory;

public class MapAccessor implements AccessorNode {

    private AccessorNode nextNode;
    private Object property;

    public MapAccessor() {
    }

    public MapAccessor(Object property) {
        this.property = property;
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory vrf) {
        if (nextNode != null) {
            return nextNode.getValue(((Map) ctx).get(property), elCtx, vrf);
        } else {
            return ((Map) ctx).get(property);
        }
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory vars, Object value) {
        if (nextNode != null) {
            return nextNode.setValue(((Map) ctx).get(property), elCtx, vars, value);
        } else {
            //noinspection unchecked
            ((Map) ctx).put(property, value);
            return value;
        }
    }

    public Object getProperty() {
        return property;
    }

    public void setProperty(Object property) {
        this.property = property;
    }

    public AccessorNode getNextNode() {
        return nextNode;
    }

    public AccessorNode setNextNode(AccessorNode nextNode) {
        return this.nextNode = nextNode;
    }

    public String toString() {
        return "Map Accessor -> [" + property + "]";
    }

    public Class getKnownEgressType() {
        return Object.class;
    }
}
