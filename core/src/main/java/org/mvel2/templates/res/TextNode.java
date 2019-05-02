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

import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.templates.TemplateRuntime;
import org.mvel2.templates.util.TemplateOutputStream;

public class TextNode extends Node {

    public TextNode(int begin, int end) {
        this.begin = begin;
        this.end = end;
    }

    public TextNode(int begin, int end, ExpressionNode next) {
        this.begin = begin;
        this.end = end;
        this.next = next;
    }

    public Object eval(TemplateRuntime runtime, TemplateOutputStream appender, Object ctx, VariableResolverFactory factory) {
        int len = end - begin;
        if (len != 0) {
            appender.append(new String(runtime.getTemplate(), begin, len));
        }
        return next != null ? next.eval(runtime, appender, ctx, factory) : null;
    }

    public String toString() {
        return "TextNode(" + begin + "," + end + ")";
    }

    public boolean demarcate(Node terminatingNode, char[] template) {
        return false;
    }

    public void calculateContents(char[] template) {
    }
}
