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

import static org.mvel2.util.ParseTools.subCompileExpression;

import org.mvel2.CompileException;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;

/**
 * @author Christopher Brock
 */
public class AssertNode extends ASTNode {

    public ExecutableStatement assertion;
    public ExecutableStatement fail;

    public AssertNode(char[] expr, int start, int offset, int fields, ParserContext pCtx) {
        super(pCtx);
        this.expr = expr;
        this.start = start;
        this.offset = offset;

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            assertion = (ExecutableStatement) subCompileExpression(expr, start, offset, pCtx);
        }
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        try {
            if (!((Boolean) assertion.getValue(ctx, thisValue, factory))) {
                throw new AssertionError("assertion failed in expression: " + new String(this.expr, start, offset));
            } else {
                return true;
            }
        } catch (ClassCastException e) {
            throw new CompileException("assertion does not contain a boolean statement", expr, start);
        }
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        try {
            if (!((Boolean) MVEL.eval(this.expr, ctx, factory))) {
                throw new AssertionError("assertion failed in expression: " + new String(this.expr, start, offset));
            } else {
                return true;
            }
        } catch (ClassCastException e) {
            throw new CompileException("assertion does not contain a boolean statement", expr, start);
        }
    }
}
