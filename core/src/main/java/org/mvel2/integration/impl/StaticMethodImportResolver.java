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

import org.mvel2.integration.VariableResolver;
import org.mvel2.util.MethodStub;

/**
 * @author Christopher Brock
 */
public class StaticMethodImportResolver implements VariableResolver {

    private String name;
    private MethodStub method;

    public StaticMethodImportResolver(String name, MethodStub method) {
        this.name = name;
        this.method = method;
    }

    public String getName() {
        return name;
    }

    public Class getType() {
        return null;
    }

    public void setStaticType(Class type) {
    }

    public int getFlags() {
        return 0;
    }

    public MethodStub getValue() {
        return method;
    }

    public void setValue(Object value) {
        this.method = (MethodStub) value;
    }
}
