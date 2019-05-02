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

import static org.mvel2.DataConversion.canConvert;
import static org.mvel2.DataConversion.convert;
import static org.mvel2.Operator.PTABLE;
import static org.mvel2.debug.DebugTools.getOperatorSymbol;
import static org.mvel2.math.MathProcessor.doOperations;
import static org.mvel2.util.CompilerTools.getReturnTypeFromOp;
import static org.mvel2.util.ParseTools.boxPrimitive;

import org.mvel2.CompileException;
import org.mvel2.Operator;
import org.mvel2.ParserContext;
import org.mvel2.ScriptRuntimeException;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.util.CompatibilityStrategy;
import org.mvel2.util.NullType;
import org.mvel2.util.ParseTools;

public class BinaryOperation extends BooleanNode {

    private final int operation;
    private int lType = -1;
    private int rType = -1;

    public BinaryOperation(int operation, ParserContext ctx) {
        super(ctx);
        this.operation = operation;
    }

    public BinaryOperation(int operation, ASTNode left, ASTNode right, ParserContext ctx) {
        super(ctx);
        this.operation = operation;
        if ((this.left = left) == null) {
            throw new ScriptRuntimeException("not a statement");
        }
        if ((this.right = right) == null) {
            throw new ScriptRuntimeException("not a statement");
        }

        //    if (ctx.isStrongTyping()) {
        switch (operation) {
            case Operator.ADD:
                /**
                 * In the special case of Strings, the return type may leftward propogate.
                 */
                if (left.getEgressType() == String.class || right.getEgressType() == String.class) {
                    egressType = String.class;
                    lType = ParseTools.__resolveType(left.egressType);
                    rType = ParseTools.__resolveType(right.egressType);

                    return;
                }

            default:
                egressType = getReturnTypeFromOp(operation, this.left.egressType, this.right.egressType);
                if (!ctx.isStrongTyping()) break;

                final boolean leftIsAssignableFromRight = left.getEgressType().isAssignableFrom(right.getEgressType());
                final boolean rightIsAssignableFromLeft = right.getEgressType().isAssignableFrom(left.getEgressType());

                if (!leftIsAssignableFromRight && !rightIsAssignableFromLeft) {

                    // Convert literals only when passing from String to Character or from Float to Double
                    final boolean requiresConversion = right.getEgressType() == String.class || (right.getEgressType() == Double.class
                            && (left.getEgressType() == Float.class || left.getEgressType() == float.class));

                    if (right.isLiteral() && requiresConversion && canConvert(left.getEgressType(), right.getEgressType())) {
                        Class targetType = isAritmeticOperation(operation) ? egressType : left.getEgressType();
                        this.right = new LiteralNode(convert(right.getReducedValueAccelerated(null, null, null), targetType), pCtx);
                    } else if (!(areCompatible(left.getEgressType(), right.getEgressType())
                            || ((operation == Operator.EQUAL || operation == Operator.NEQUAL)
                                    && CompatibilityStrategy.areEqualityCompatible(left.getEgressType(), right.getEgressType())))) {

                        throw new CompileException(
                                "incompatible types in statement: " + right.getEgressType() + " (compared from: " + left.getEgressType()
                                        + ")",
                                left.getExpr() != null ? left.getExpr() : right.getExpr(),
                                left.getExpr() != null ? left.getStart() : right.getStart());
                    }
                }
        }

        // }

        if (this.left.isLiteral() && this.right.isLiteral()) {
            if (this.left.egressType == this.right.egressType) {
                lType = rType = ParseTools.__resolveType(left.egressType);
            } else {
                lType = ParseTools.__resolveType(this.left.egressType);
                rType = ParseTools.__resolveType(this.right.egressType);
            }
        }
    }

    private boolean isAritmeticOperation(int operation) {
        return operation <= Operator.POWER;
    }

    private boolean areCompatible(Class<?> leftClass, Class<?> rightClass) {
        return leftClass.equals(NullType.class) || rightClass.equals(NullType.class)
                || (Number.class.isAssignableFrom(rightClass) && Number.class.isAssignableFrom(leftClass))
                || ((rightClass.isPrimitive() || leftClass.isPrimitive()) && canConvert(boxPrimitive(leftClass), boxPrimitive(rightClass)));
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return doOperations(lType, left.getReducedValueAccelerated(ctx, thisValue, factory), operation, rType,
                right.getReducedValueAccelerated(ctx, thisValue, factory));
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        throw new RuntimeException("unsupported AST operation");
    }

    public int getOperation() {
        return operation;
    }

    public BinaryOperation getRightBinary() {
        return right != null && right instanceof BinaryOperation ? (BinaryOperation) right : null;
    }

    public ASTNode getRightMost() {
        BinaryOperation n = this;
        while (n.right != null && n.right instanceof BinaryOperation) {
            n = (BinaryOperation) n.right;
        }
        return n.right;
    }

    public void setRightMost(ASTNode right) {
        BinaryOperation n = this;
        while (n.right != null && n.right instanceof BinaryOperation) {
            n = (BinaryOperation) n.right;
        }
        n.right = right;

        if (n == this) {
            if ((rType = ParseTools.__resolveType(n.right.getEgressType())) == 0) rType = -1;
        }
    }

    public int getPrecedence() {
        return PTABLE[operation];
    }

    public boolean isGreaterPrecedence(BinaryOperation o) {
        return o.getPrecedence() > PTABLE[operation];
    }

    @Override
    public boolean isLiteral() {
        return false;
    }

    public String toString() {
        return "(" + left + " " + getOperatorSymbol(operation) + " " + right + ")";
    }
}
