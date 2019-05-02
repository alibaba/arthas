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

package org.mvel2.templates.res;

import org.mvel2.MVEL;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.SimpleTemplateRegistry;
import org.mvel2.templates.TemplateRuntime;
import org.mvel2.templates.util.TemplateOutputStream;

public class DeclareNode extends Node {

    private Node nestedNode;

    public DeclareNode(int begin, String name, char[] template, int start, int end) {
        this.begin = begin;
        this.name = name;
        this.contents = template;
        this.cStart = start;
        this.cEnd = end - 1;
        this.end = end;
        //    this.contents = subset(template, this.cStart = start, (this.end = this.cEnd = end) - start - 1);
    }

    public Object eval(TemplateRuntime runtime, TemplateOutputStream appender, Object ctx, VariableResolverFactory factory) {
        if (runtime.getNamedTemplateRegistry() == null) {
            runtime.setNamedTemplateRegistry(new SimpleTemplateRegistry());
        }

        runtime.getNamedTemplateRegistry().addNamedTemplate(MVEL.eval(contents, cStart, cEnd - cStart, ctx, factory, String.class),
                new CompiledTemplate(runtime.getTemplate(), nestedNode));

        return next != null ? next.eval(runtime, appender, ctx, factory) : null;
    }

    public boolean demarcate(Node terminatingNode, char[] template) {
        Node n = nestedNode = next;

        while (n.getNext() != null)
            n = n.next;

        n.next = new EndNode();

        next = terminus;
        return false;
    }
}
