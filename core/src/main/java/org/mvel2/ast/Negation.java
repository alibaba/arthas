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
import org.mvel2.util.ParseTools;

public class Negation extends ASTNode {

    private ExecutableStatement stmt;

    public Negation(char[] expr, int start, int offset, int fields, ParserContext pCtx) {
        super(pCtx);
        this.expr = expr;
        this.start = start;
        this.offset = offset;

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            if (((this.stmt = (ExecutableStatement) subCompileExpression(expr, start, offset, pCtx)).getKnownEgressType() != null)
                    && (!ParseTools.boxPrimitive(stmt.getKnownEgressType()).isAssignableFrom(Boolean.class))) {
                throw new CompileException("negation operator cannot be applied to non-boolean type", expr, start);
            }
        }
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return !((Boolean) stmt.getValue(ctx, thisValue, factory));
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        try {
            return !((Boolean) MVEL.eval(expr, start, offset, ctx, factory));
        } catch (NullPointerException e) {
            throw new CompileException("negation operator applied to a null value", expr, start, e);
        } catch (ClassCastException e) {
            throw new CompileException("negation operator applied to non-boolean expression", expr, start, e);
        }
    }

    public Class getEgressType() {
        return Boolean.class;
    }

    public ExecutableStatement getStatement() {
        return stmt;
    }
}
