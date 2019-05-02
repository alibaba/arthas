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

import org.mvel2.MVEL;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.templates.TemplateRuntime;
import org.mvel2.templates.util.TemplateOutputStream;

public class NamedIncludeNode extends Node {
    //  private char[] includeExpression;
    //  private char[] preExpression;

    int includeStart;
    int includeOffset;

    int preStart;
    int preOffset;

    public NamedIncludeNode(int begin, String name, char[] template, int start, int end) {
        this.begin = begin;
        this.name = name;
        this.contents = template;
        this.cStart = start;
        this.cEnd = end - 1;
        this.end = end;
        //    this.contents = subset(template, this.cStart = start, (this.end = this.cEnd = end) - start - 1);

        int mark = captureToEOS(contents, 0);
        includeStart = cStart;
        includeOffset = mark - cStart;
        preStart = ++mark;
        preOffset = cEnd - mark;

        //        int mark;
        //        this.includeExpression = subset(contents, 0, mark = captureToEOS(contents, 0));
        //        if (mark != contents.length) this.preExpression = subset(contents, ++mark, contents.length - mark);
    }

    public Object eval(TemplateRuntime runtime, TemplateOutputStream appender, Object ctx, VariableResolverFactory factory) {
        if (preOffset != 0) {
            MVEL.eval(contents, preStart, preOffset, ctx, factory);
        }

        if (next != null) {
            return next
                    .eval(runtime,
                            appender.append(
                                    String.valueOf(TemplateRuntime.execute(
                                            runtime.getNamedTemplateRegistry().getNamedTemplate(
                                                    MVEL.eval(contents, includeStart, includeOffset, ctx, factory, String.class)),
                                            ctx, factory))),
                            ctx, factory);
        } else {
            return appender.append(String.valueOf(TemplateRuntime.execute(runtime.getNamedTemplateRegistry()
                    .getNamedTemplate(MVEL.eval(contents, includeStart, includeOffset, ctx, factory, String.class)), ctx, factory)));
        }
    }

    public boolean demarcate(Node terminatingNode, char[] template) {
        return false;
    }
}
