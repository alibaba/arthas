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
import org.mvel2.templates.TemplateRuntime;
import org.mvel2.templates.util.TemplateOutputStream;
import org.mvel2.util.ParseTools;

public class IfNode extends Node {

    protected Node trueNode;
    protected Node elseNode;

    public IfNode(int begin, String name, char[] template, int start, int end) {
        super(begin, name, template, start, end);
        while (cEnd > cStart && ParseTools.isWhitespace(template[cEnd]))
            cEnd--;
    }

    public Node getTrueNode() {
        return trueNode;
    }

    public void setTrueNode(ExpressionNode trueNode) {
        this.trueNode = trueNode;
    }

    public Node getElseNode() {
        return elseNode;
    }

    public void setElseNode(ExpressionNode elseNode) {
        this.elseNode = elseNode;
    }

    public boolean demarcate(Node terminatingNode, char[] template) {
        trueNode = next;
        next = terminus;
        return true;
    }

    public Object eval(TemplateRuntime runtime, TemplateOutputStream appender, Object ctx, VariableResolverFactory factory) {
        if (cEnd == cStart || MVEL.eval(contents, cStart, cEnd - cStart, ctx, factory, Boolean.class)) {
            return trueNode.eval(runtime, appender, ctx, factory);
        }
        return next != null ? next.eval(runtime, appender, ctx, factory) : null;
    }
}
