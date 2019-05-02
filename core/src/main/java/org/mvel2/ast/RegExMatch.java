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

import static java.lang.String.valueOf;
import static java.util.regex.Pattern.compile;
import static org.mvel2.MVEL.eval;
import static org.mvel2.util.ParseTools.subCompileExpression;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.mvel2.CompileException;
import org.mvel2.ParserContext;
import org.mvel2.compiler.ExecutableLiteral;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;

public class RegExMatch extends ASTNode {

    private ExecutableStatement stmt;
    private ExecutableStatement patternStmt;

    private int patternStart;
    private int patternOffset;
    private Pattern p;

    public RegExMatch(char[] expr, int start, int offset, int fields, int patternStart, int patternOffset, ParserContext pCtx) {
        super(pCtx);
        this.expr = expr;
        this.start = start;
        this.offset = offset;
        this.patternStart = patternStart;
        this.patternOffset = patternOffset;

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            this.stmt = (ExecutableStatement) subCompileExpression(expr, start, offset, pCtx);
            if ((this.patternStmt = (ExecutableStatement) subCompileExpression(expr, patternStart, patternOffset,
                    pCtx)) instanceof ExecutableLiteral) {

                try {
                    p = compile(valueOf(patternStmt.getValue(null, null)));
                } catch (PatternSyntaxException e) {
                    throw new CompileException("bad regular expression", expr, patternStart, e);
                }
            }
        }
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (p == null) {
            return compile(valueOf(patternStmt.getValue(ctx, thisValue, factory))).matcher(valueOf(stmt.getValue(ctx, thisValue, factory)))
                    .matches();
        } else {
            return p.matcher(valueOf(stmt.getValue(ctx, thisValue, factory))).matches();
        }
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        try {
            return compile(valueOf(eval(expr, patternStart, patternOffset, ctx, factory)))
                    .matcher(valueOf(eval(expr, start, offset, ctx, factory))).matches();
        } catch (PatternSyntaxException e) {
            throw new CompileException("bad regular expression", expr, patternStart, e);
        }
    }

    public Class getEgressType() {
        return Boolean.class;
    }

    public Pattern getPattern() {
        return p;
    }

    public ExecutableStatement getStatement() {
        return stmt;
    }

    public ExecutableStatement getPatternStatement() {
        return patternStmt;
    }
}
