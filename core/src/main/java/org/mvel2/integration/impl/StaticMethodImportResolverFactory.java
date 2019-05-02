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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.mvel2.ParserContext;
import org.mvel2.integration.VariableResolver;
import org.mvel2.util.MethodStub;

/**
 * @author Christopher Brock
 */
public class StaticMethodImportResolverFactory extends BaseVariableResolverFactory {

    public StaticMethodImportResolverFactory(ParserContext ctx) {
        this.variableResolvers = new HashMap<String, VariableResolver>();
        for (Map.Entry<String, Object> entry : ctx.getImports().entrySet()) {
            if (entry.getValue() instanceof Method) {
                createVariable(entry.getKey(), entry.getValue());
            }
        }
    }

    public StaticMethodImportResolverFactory() {
        this.variableResolvers = new HashMap<String, VariableResolver>();
    }

    public VariableResolver createVariable(String name, Object value) {
        if (value instanceof Method) value = new MethodStub((Method) value);

        StaticMethodImportResolver methodResolver = new StaticMethodImportResolver(name, (MethodStub) value);
        this.variableResolvers.put(name, methodResolver);
        return methodResolver;
    }

    public VariableResolver createVariable(String name, Object value, Class<?> type) {
        return null;
    }

    public boolean isTarget(String name) {
        return this.variableResolvers.containsKey(name);
    }

    public boolean isResolveable(String name) {
        return isTarget(name) || isNextResolveable(name);
    }

    public Map<String, Method> getImportedMethods() {
        Map<String, Method> im = new HashMap<String, Method>();
        for (Map.Entry<String, VariableResolver> e : this.variableResolvers.entrySet()) {
            im.put(e.getKey(), (Method) e.getValue().getValue());
        }
        return im;
    }

}
