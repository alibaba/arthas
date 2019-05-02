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
import org.mvel2.integration.VariableResolverFactory;

public class ItemResolverFactory extends BaseVariableResolverFactory {

    private final ItemResolver resolver;

    public ItemResolverFactory(ItemResolver resolver, VariableResolverFactory nextFactory) {
        this.resolver = resolver;
        this.nextFactory = nextFactory;
    }

    public VariableResolver createVariable(String name, Object value) {
        if (isTarget(name)) {
            resolver.setValue(value);
            return resolver;
        } else {
            return nextFactory.createVariable(name, value);
        }
    }

    public VariableResolver createVariable(String name, Object value, Class<?> type) {
        if (isTarget(name)) {
            throw new RuntimeException("variable already defined in scope: " + name);
        } else {
            return nextFactory.createVariable(name, value);
        }
    }

    public VariableResolver getVariableResolver(String name) {
        return isTarget(name) ? resolver : nextFactory.getVariableResolver(name);
    }

    public boolean isTarget(String name) {
        return resolver.getName().equals(name);
    }

    public boolean isResolveable(String name) {
        return resolver.getName().equals(name) || (nextFactory != null && nextFactory.isResolveable(name));
    }

    public static class ItemResolver implements VariableResolver {

        private final String name;
        public Object value;
        private Class type = Object.class;

        public ItemResolver(String name, Class type) {
            this.name = name;
            this.type = type;
        }

        public ItemResolver(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Class getType() {
            return type;
        }

        public void setStaticType(Class type) {
            this.type = type;
        }

        public int getFlags() {
            return 0;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }
}
