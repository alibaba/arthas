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

import static java.lang.Thread.currentThread;
import static org.mvel2.Operator.NOOP;
import static org.mvel2.PropertyAccessor.get;
import static org.mvel2.optimizers.OptimizerFactory.SAFE_REFLECTIVE;
import static org.mvel2.optimizers.OptimizerFactory.getAccessorCompiler;
import static org.mvel2.optimizers.OptimizerFactory.getDefaultAccessorCompiler;
import static org.mvel2.util.CompilerTools.getInjectedImports;
import static org.mvel2.util.ParseTools.handleNumericConversion;
import static org.mvel2.util.ParseTools.isNumber;
import static org.mvel2.util.ParseTools.subArray;

import java.io.Serializable;

import org.mvel2.CompileException;
import org.mvel2.ParserConfiguration;
import org.mvel2.ParserContext;
import org.mvel2.compiler.Accessor;
import org.mvel2.debug.DebugTools;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.optimizers.AccessorOptimizer;
import org.mvel2.optimizers.OptimizationNotSupported;

@SuppressWarnings({ "ManualArrayCopy", "CaughtExceptionImmediatelyRethrown" })
public class ASTNode implements Cloneable, Serializable {

    public static final int LITERAL = 1;
    public static final int DEEP_PROPERTY = 1 << 1;
    public static final int OPERATOR = 1 << 2;
    public static final int IDENTIFIER = 1 << 3;
    public static final int COMPILE_IMMEDIATE = 1 << 4;
    public static final int NUMERIC = 1 << 5;

    public static final int INVERT = 1 << 6;
    public static final int ASSIGN = 1 << 7;

    public static final int COLLECTION = 1 << 8;
    public static final int THISREF = 1 << 9;
    public static final int INLINE_COLLECTION = 1 << 10;

    public static final int BLOCK_IF = 1 << 11;
    public static final int BLOCK_FOREACH = 1 << 12;
    public static final int BLOCK_WITH = 1 << 13;
    public static final int BLOCK_UNTIL = 1 << 14;
    public static final int BLOCK_WHILE = 1 << 15;
    public static final int BLOCK_DO = 1 << 16;
    public static final int BLOCK_DO_UNTIL = 1 << 17;
    public static final int BLOCK_FOR = 1 << 18;

    public static final int OPT_SUBTR = 1 << 19;

    public static final int FQCN = 1 << 20;

    public static final int STACKLANG = 1 << 22;

    public static final int DEFERRED_TYPE_RES = 1 << 23;
    public static final int STRONG_TYPING = 1 << 24;
    public static final int PCTX_STORED = 1 << 25;
    public static final int ARRAY_TYPE_LITERAL = 1 << 26;

    public static final int NOJIT = 1 << 27;
    public static final int DEOP = 1 << 28;

    public static final int DISCARD = 1 << 29;

    // *** //
    public int fields = 0;
    public ASTNode nextASTNode;
    protected int firstUnion;
    protected int endOfName;
    protected Class egressType;
    protected char[] expr;
    protected int start;
    protected int offset;
    protected String nameCache;
    protected Object literal;
    protected transient volatile Accessor accessor;
    protected volatile Accessor safeAccessor;
    protected int cursorPosition;
    protected ParserContext pCtx;

    protected ASTNode(ParserContext pCtx) {
        this.pCtx = pCtx;
    }

    public ASTNode(char[] expr, int start, int offset, int fields, ParserContext pCtx) {
        this(pCtx);
        this.fields = fields;
        this.expr = expr;
        this.start = start;
        this.offset = offset;

        setName(expr);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (accessor != null) {
            try {
                return accessor.getValue(ctx, thisValue, factory);
            } catch (ClassCastException ce) {
                return deop(ctx, thisValue, factory, ce);
            }
        } else {
            return optimize(ctx, thisValue, factory);
        }
    }

    private Object deop(Object ctx, Object thisValue, VariableResolverFactory factory, RuntimeException e) {
        if ((fields & DEOP) == 0) {
            accessor = null;
            fields |= DEOP | NOJIT;

            synchronized (this) {
                return getReducedValueAccelerated(ctx, thisValue, factory);
            }
        } else {
            throw e;
        }
    }

    private Object optimize(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if ((fields & DEOP) != 0) {
            fields ^= DEOP;
        }

        AccessorOptimizer optimizer;
        Object retVal = null;

        if ((fields & NOJIT) != 0 || factory != null && factory.isResolveable(nameCache)) {
            optimizer = getAccessorCompiler(SAFE_REFLECTIVE);
        } else {
            optimizer = getDefaultAccessorCompiler();
        }

        ParserContext pCtx;

        if ((fields & PCTX_STORED) != 0) {
            pCtx = (ParserContext) literal;
        } else {
            pCtx = new ParserContext(new ParserConfiguration(getInjectedImports(factory), null));
        }

        try {
            pCtx.optimizationNotify();
            setAccessor(optimizer.optimizeAccessor(pCtx, expr, start, offset, ctx, thisValue, factory, true, egressType));
        } catch (OptimizationNotSupported ne) {
            setAccessor((optimizer = getAccessorCompiler(SAFE_REFLECTIVE)).optimizeAccessor(pCtx, expr, start, offset, ctx, thisValue,
                    factory, true, null));
        }

        if (accessor == null) {
            return get(expr, start, offset, ctx, factory, thisValue, pCtx);
        }

        if (retVal == null) {
            retVal = optimizer.getResultOptPass();
        }

        if (egressType == null) {
            egressType = optimizer.getEgressType();
        }

        return retVal;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if ((fields & (LITERAL)) != 0) {
            return literal;
        } else {
            return get(expr, start, offset, ctx, factory, thisValue, pCtx);
        }
    }

    protected String getAbsoluteRootElement() {
        if ((fields & (DEEP_PROPERTY | COLLECTION)) != 0) {
            return new String(expr, start, getAbsoluteFirstPart());
        }
        return nameCache;
    }

    public Class getEgressType() {
        return egressType;
    }

    public void setEgressType(Class egressType) {
        this.egressType = egressType;
    }

    public char[] getNameAsArray() {
        return subArray(expr, start, start + offset);
    }

    private int getAbsoluteFirstPart() {
        if ((fields & COLLECTION) != 0) {
            if (firstUnion < 0 || endOfName < firstUnion) return endOfName;
            else return firstUnion;
        } else if ((fields & DEEP_PROPERTY) != 0) {
            return firstUnion;
        } else {
            return -1;
        }
    }

    public String getAbsoluteName() {
        if (firstUnion > start) {
            return new String(expr, start, getAbsoluteFirstPart() - start);
        } else {
            return getName();
        }
    }

    public String getName() {
        if (nameCache != null) {
            return nameCache;
        } else if (expr != null) {
            return nameCache = new String(expr, start, offset);
        }
        return "";
    }

    @SuppressWarnings({ "SuspiciousMethodCalls" })
    protected void setName(char[] name) {
        if (isNumber(name, start, offset)) {
            egressType = (literal = handleNumericConversion(name, start, offset)).getClass();
            if (((fields |= NUMERIC | LITERAL | IDENTIFIER) & INVERT) != 0) {
                try {
                    literal = ~((Integer) literal);
                } catch (ClassCastException e) {
                    throw new CompileException("bitwise (~) operator can only be applied to integers", expr, start);
                }
            }
            return;
        }

        this.literal = new String(name, start, offset);

        int end = start + offset;

        Scan: for (int i = start; i < end; i++) {
            switch (name[i]) {
                case '.':
                    if (firstUnion == 0) {
                        firstUnion = i;
                    }
                    break;
                case '[':
                case '(':
                    if (firstUnion == 0) {
                        firstUnion = i;
                    }
                    if (endOfName == 0) {
                        endOfName = i;
                        if (i < name.length && name[i + 1] == ']') fields |= ARRAY_TYPE_LITERAL;
                        break Scan;
                    }
            }
        }

        if ((fields & INLINE_COLLECTION) != 0) {
            return;
        }

        if (firstUnion > start) {
            fields |= DEEP_PROPERTY | IDENTIFIER;
        } else {
            fields |= IDENTIFIER;
        }
    }

    public Object getLiteralValue() {
        return literal;
    }

    public void setLiteralValue(Object literal) {
        this.literal = literal;
        this.fields |= LITERAL;
    }

    public void storeInLiteralRegister(Object o) {
        this.literal = o;
    }

    public Accessor setAccessor(Accessor accessor) {
        return this.accessor = accessor;
    }

    public boolean isIdentifier() {
        return (fields & IDENTIFIER) != 0;
    }

    public boolean isLiteral() {
        return (fields & LITERAL) != 0;
    }

    public boolean isThisVal() {
        return (fields & THISREF) != 0;
    }

    public boolean isOperator() {
        return (fields & OPERATOR) != 0;
    }

    public boolean isOperator(Integer operator) {
        return (fields & OPERATOR) != 0 && operator.equals(literal);
    }

    public Integer getOperator() {
        return NOOP;
    }

    protected boolean isCollection() {
        return (fields & COLLECTION) != 0;
    }

    public boolean isAssignment() {
        return ((fields & ASSIGN) != 0);
    }

    public boolean isDeepProperty() {
        return ((fields & DEEP_PROPERTY) != 0);
    }

    public boolean isFQCN() {
        return ((fields & FQCN) != 0);
    }

    public void setAsLiteral() {
        fields |= LITERAL;
    }

    public void setAsFQCNReference() {
        fields |= FQCN;
    }

    public int getCursorPosition() {
        return cursorPosition;
    }

    public void setCursorPosition(int cursorPosition) {
        this.cursorPosition = cursorPosition;
    }

    public boolean isDiscard() {
        return fields != -1 && (fields & DISCARD) != 0;
    }

    public void discard() {
        this.fields |= DISCARD;
    }

    public void strongTyping() {
        this.fields |= STRONG_TYPING;
    }

    public void storePctx() {
        this.fields |= PCTX_STORED;
    }

    public boolean isDebuggingSymbol() {
        return this.fields == -1;
    }

    public int getFields() {
        return fields;
    }

    public Accessor getAccessor() {
        return accessor;
    }

    public boolean canSerializeAccessor() {
        return safeAccessor != null;
    }

    public int getStart() {
        return start;
    }

    public int getOffset() {
        return offset;
    }

    public char[] getExpr() {
        return expr;
    }

    public String toString() {
        return isOperator() ? "<<" + DebugTools.getOperatorName(getOperator())
                + ">>" : (PCTX_STORED & fields) != 0 ? nameCache : new String(expr, start, offset);
    }

    protected ClassLoader getClassLoader() {
        return pCtx != null ? pCtx.getClassLoader() : currentThread().getContextClassLoader();
    }
}
