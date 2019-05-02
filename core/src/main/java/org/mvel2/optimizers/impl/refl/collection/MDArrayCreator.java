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

import static java.lang.reflect.Array.newInstance;

import java.lang.reflect.Array;

import org.mvel2.compiler.Accessor;
import org.mvel2.integration.VariableResolverFactory;

/**
 * @author Christopher Brock
 */
public class MDArrayCreator implements Accessor {

    public Accessor[] template;
    private Class arrayType;
    private int dimension;

    public MDArrayCreator(Accessor[] template, Class arrayType, int dimension) {
        this.template = template;
        this.arrayType = arrayType;
        this.dimension = dimension;
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        if (Object.class.equals(arrayType)) {
            Object[] newArray = new Object[template.length];

            for (int i = 0; i < newArray.length; i++)
                newArray[i] = template[i].getValue(ctx, elCtx, variableFactory);

            return newArray;
        } else {
            Object newArray = newInstance(arrayType, template.length);

            for (int i = 0; i < template.length; i++) {
                Object o = template[i].getValue(ctx, elCtx, variableFactory);
                Array.set(newArray, i, o);
            }

            return newArray;
        }
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        return null;
    }

    public Class getKnownEgressType() {
        return arrayType;
    }
}