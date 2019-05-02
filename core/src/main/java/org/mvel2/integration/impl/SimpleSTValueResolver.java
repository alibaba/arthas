/**
 * MVEL 2.0
 * Copyright (C) 2007 The Codehaus
 * Mike Brock, Dhanji Prasanna, John Graham, Mark Proctor
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
 */

package org.mvel2.integration.impl;

import static org.mvel2.DataConversion.canConvert;
import static org.mvel2.DataConversion.convert;

import org.mvel2.integration.VariableResolver;

public class SimpleSTValueResolver implements VariableResolver {

    private Object value;
    private Class type;
    private boolean updated = false;

    public SimpleSTValueResolver(Object value, Class type) {
        this.value = handleTypeCoercion(type, value);
        this.type = type;
    }

    public SimpleSTValueResolver(Object value, Class type, boolean updated) {
        this.value = handleTypeCoercion(type, value);
        this.type = type;
        this.updated = updated;
    }

    private static Object handleTypeCoercion(Class type, Object value) {
        if (type != null && value != null && value.getClass() != type) {
            if (!canConvert(type, value.getClass())) {
                throw new RuntimeException("cannot assign " + value.getClass().getName() + " to type: " + type.getName());
            }
            try {
                return convert(value, type);
            } catch (Exception e) {
                throw new RuntimeException("cannot convert value of " + value.getClass().getName() + " to: " + type.getName());
            }
        }
        return value;
    }

    public String getName() {
        return null;
    }

    public Class getType() {
        return type;
    }

    public void setStaticType(Class type) {
        this.type = type;
    }

    public int getFlags() {
        return updated ? -1 : 0;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        updated = true;
        this.value = handleTypeCoercion(type, value);
    }

}