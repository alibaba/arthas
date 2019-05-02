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

package org.mvel2.ast;

import static org.mvel2.debug.DebugTools.getOperatorSymbol;

import org.mvel2.CompileException;
import org.mvel2.ParserContext;
import org.mvel2.integration.VariableResolverFactory;

public class OperatorNode extends ASTNode {

    private Integer operator;

    public OperatorNode(Integer operator, char[] expr, int start, ParserContext pCtx) {
        super(pCtx);
        assert operator != null;
        this.expr = expr;
        this.literal = this.operator = operator;
        this.start = start;
    }

    public boolean isOperator() {
        return true;
    }

    public boolean isOperator(Integer operator) {
        return operator.equals(this.operator);
    }

    public Integer getOperator() {
        return operator;
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        throw new CompileException("illegal use of operator: " + getOperatorSymbol(operator), expr, start);
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        throw new CompileException("illegal use of operator: " + getOperatorSymbol(operator), expr, start);
    }
}
