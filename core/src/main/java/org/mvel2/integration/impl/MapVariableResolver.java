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

import java.util.Map;

import org.mvel2.integration.VariableResolver;

public class MapVariableResolver implements VariableResolver {

    private String name;
    private Class<?> knownType;
    private Map<String, Object> variableMap;

    public MapVariableResolver(Map<String, Object> variableMap, String name) {
        this.variableMap = variableMap;
        this.name = name;
    }

    public MapVariableResolver(Map<String, Object> variableMap, String name, Class knownType) {
        this.name = name;
        this.knownType = knownType;
        this.variableMap = variableMap;
    }

    public void setStaticType(Class knownType) {
        this.knownType = knownType;
    }

    public void setVariableMap(Map<String, Object> variableMap) {
        this.variableMap = variableMap;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class getType() {
        return knownType;
    }

    public Object getValue() {
        return variableMap.get(name);
    }

    public void setValue(Object value) {
        if (knownType != null && value != null && value.getClass() != knownType) {
            if (!canConvert(knownType, value.getClass())) {
                throw new RuntimeException("cannot assign " + value.getClass().getName() + " to type: " + knownType.getName());
            }
            try {
                value = convert(value, knownType);
            } catch (Exception e) {
                throw new RuntimeException("cannot convert value of " + value.getClass().getName() + " to: " + knownType.getName());
            }
        }

        //noinspection unchecked
        variableMap.put(name, value);
    }

    public int getFlags() {
        return 0;
    }
}
