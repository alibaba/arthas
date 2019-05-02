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

package org.mvel2.templates;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SimpleTemplateRegistry implements TemplateRegistry {

    private Map<String, CompiledTemplate> NAMED_TEMPLATES = new HashMap<String, CompiledTemplate>();

    public void addNamedTemplate(String name, CompiledTemplate template) {
        NAMED_TEMPLATES.put(name, template);
    }

    public CompiledTemplate getNamedTemplate(String name) {
        CompiledTemplate t = NAMED_TEMPLATES.get(name);
        if (t == null) throw new TemplateError("no named template exists '" + name + "'");
        return t;
    }

    public Iterator iterator() {
        return NAMED_TEMPLATES.keySet().iterator();
    }

    public Set<String> getNames() {
        return NAMED_TEMPLATES.keySet();
    }

    public boolean contains(String name) {
        return NAMED_TEMPLATES.containsKey(name);
    }
}
