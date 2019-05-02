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

import static org.mvel2.MVEL.eval;
import static org.mvel2.util.CompilerTools.expectType;
import static org.mvel2.util.ParseTools.subCompileExpression;

import java.util.HashMap;

import org.mvel2.CompileException;
import org.mvel2.ParserContext;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;

/**
 * @author Christopher Brock
 */
public class IfNode extends BlockNode implements NestedStatement {

    protected ExecutableStatement condition;
    protected ExecutableStatement nestedStatement;

    protected IfNode elseIf;
    protected ExecutableStatement elseBlock;

    protected boolean idxAlloc = false;

    public IfNode(char[] expr, int start, int offset, int blockStart, int blockOffset, int fields, ParserContext pCtx) {
        super(pCtx);
        if ((this.expr = expr) == null || offset == 0) {
            throw new CompileException("statement expected", expr, start);
        }
        this.start = start;
        this.offset = offset;
        this.blockStart = blockStart;
        this.blockOffset = blockOffset;

        idxAlloc = pCtx != null && pCtx.isIndexAllocation();

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            expectType(pCtx, this.condition = (ExecutableStatement) subCompileExpression(expr, start, offset, pCtx), Boolean.class, true);

            if (pCtx != null) {
                pCtx.pushVariableScope();
            }
            this.nestedStatement = (ExecutableStatement) subCompileExpression(expr, blockStart, blockOffset, pCtx);

            if (pCtx != null) {
                pCtx.popVariableScope();
            }
        }
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if ((Boolean) condition.getValue(ctx, thisValue, factory)) {
            return nestedStatement.getValue(ctx, thisValue, idxAlloc ? factory : new MapVariableResolverFactory(new HashMap(0), factory));
        } else if (elseIf != null) {
            return elseIf.getReducedValueAccelerated(ctx, thisValue,
                    idxAlloc ? factory : new MapVariableResolverFactory(new HashMap(0), factory));
        } else if (elseBlock != null) {
            return elseBlock.getValue(ctx, thisValue, idxAlloc ? factory : new MapVariableResolverFactory(new HashMap(0), factory));
        } else {
            return null;
        }
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if ((Boolean) eval(expr, start, offset, ctx, factory)) {
            return eval(expr, blockStart, blockOffset, ctx, new MapVariableResolverFactory(new HashMap(0), factory));
        } else if (elseIf != null) {
            return elseIf.getReducedValue(ctx, thisValue, new MapVariableResolverFactory(new HashMap(0), factory));
        } else if (elseBlock != null) {
            return elseBlock.getValue(ctx, thisValue, new MapVariableResolverFactory(new HashMap(0), factory));
        } else {
            return null;
        }
    }

    public ExecutableStatement getNestedStatement() {
        return nestedStatement;
    }

    public IfNode setElseIf(IfNode elseIf) {
        return this.elseIf = elseIf;
    }

    public ExecutableStatement getElseBlock() {
        return elseBlock;
    }

    public IfNode setElseBlock(char[] block, int cursor, int offset, ParserContext ctx) {
        elseBlock = (ExecutableStatement) subCompileExpression(block, cursor, offset, ctx);
        return this;
    }

    public String toString() {
        return new String(expr, start, offset);
    }
}
