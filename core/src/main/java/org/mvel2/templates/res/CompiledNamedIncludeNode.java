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

import static org.mvel2.templates.util.TemplateTools.captureToEOS;

import java.io.Serializable;

import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.StackDelimiterResolverFactory;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateError;
import org.mvel2.templates.TemplateRuntime;
import org.mvel2.templates.util.TemplateOutputStream;

public class CompiledNamedIncludeNode extends Node {

    private Serializable cIncludeExpression;
    private Serializable cPreExpression;

    public CompiledNamedIncludeNode(int begin, String name, char[] template, int start, int end, ParserContext context) {
        this.begin = begin;
        this.name = name;

        this.contents = template;
        this.cStart = start;
        this.cEnd = end - 1;
        this.end = end;

        int mark = captureToEOS(contents, cStart);
        this.cIncludeExpression = MVEL.compileExpression(contents, cStart, mark - cStart, context);

        if (mark != contents.length) {
            this.cPreExpression = MVEL.compileExpression(contents, ++mark, cEnd - mark, context);
        }
    }

    public Object eval(TemplateRuntime runtime, TemplateOutputStream appender, Object ctx, VariableResolverFactory factory) {
        factory = new StackDelimiterResolverFactory(factory);
        if (cPreExpression != null) {
            MVEL.executeExpression(cPreExpression, ctx, factory);
        }

        if (next != null) {
            String namedTemplate = MVEL.executeExpression(cIncludeExpression, ctx, factory, String.class);
            CompiledTemplate ct = runtime.getNamedTemplateRegistry().getNamedTemplate(namedTemplate);

            if (ct == null) throw new TemplateError("named template does not exist: " + namedTemplate);

            return next.eval(runtime,
                    appender.append(String.valueOf(TemplateRuntime.execute(ct, ctx, factory, runtime.getNamedTemplateRegistry()))), ctx,
                    factory);

            //            return next.eval(runtime,
            //                    appender.append(String.valueOf(TemplateRuntime.execute(runtime.getNamedTemplateRegistry().getNamedTemplate(MVEL.executeExpression(cIncludeExpression, ctx, factory, String.class)), ctx, factory))), ctx, factory);
        } else {
            return appender.append(String.valueOf(TemplateRuntime.execute(
                    runtime.getNamedTemplateRegistry()
                            .getNamedTemplate(MVEL.executeExpression(cIncludeExpression, ctx, factory, String.class)),
                    ctx, factory, runtime.getNamedTemplateRegistry())));
        }
    }

    public boolean demarcate(Node terminatingNode, char[] template) {
        return false;
    }
}