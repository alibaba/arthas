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
package org.mvel2.optimizers.impl.refl.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mvel2.compiler.Accessor;
import org.mvel2.integration.VariableResolverFactory;

/**
 * @author Christopher Brock
 */
public class ListCreator implements Accessor {

    private Accessor[] values;

    public ListCreator(Accessor[] values) {
        this.values = values;
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        Object[] template = new Object[getValues().length];
        for (int i = 0; i < getValues().length; i++) {
            template[i] = getValues()[i].getValue(ctx, elCtx, variableFactory);
        }
        return new ArrayList<Object>(Arrays.asList(template));
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        return null;
    }

    public Class getKnownEgressType() {
        return List.class;
    }

    public Accessor[] getValues() {
        return values;
    }
}
