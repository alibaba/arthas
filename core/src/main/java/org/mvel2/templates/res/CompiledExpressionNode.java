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

import static java.lang.String.valueOf;

import java.io.Serializable;

import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.templates.TemplateRuntime;
import org.mvel2.templates.util.TemplateOutputStream;

public class CompiledExpressionNode extends ExpressionNode {

    private Serializable ce;

    public CompiledExpressionNode(int begin, String name, char[] template, int start, int end, ParserContext context) {
        this.begin = begin;
        this.name = name;
        this.contents = template;
        this.cStart = start;
        this.cEnd = end - 1;
        this.end = end;
        ce = MVEL.compileExpression(template, cStart, cEnd - cStart, context);
    }

    public Object eval(TemplateRuntime runtime, TemplateOutputStream appender, Object ctx, VariableResolverFactory factory) {
        appender.append(valueOf(MVEL.executeExpression(ce, ctx, factory)));
        return next != null ? next.eval(runtime, appender, ctx, factory) : null;
    }

    public String toString() {
        return "ExpressionNode:" + name + "{" + (contents == null ? "" : new String(contents)) + "} (start=" + begin + ";end=" + end + ")";
    }
}