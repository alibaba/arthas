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

package org.mvel2;

import static org.mvel2.Operator.AND;
import static org.mvel2.Operator.CHOR;
import static org.mvel2.Operator.END_OF_STMT;
import static org.mvel2.Operator.NOOP;
import static org.mvel2.Operator.OR;
import static org.mvel2.Operator.RETURN;
import static org.mvel2.Operator.TERNARY;
import static org.mvel2.Operator.TERNARY_ELSE;

import java.util.Map;

import org.mvel2.ast.ASTNode;
import org.mvel2.ast.Substatement;
import org.mvel2.compiler.AbstractParser;
import org.mvel2.compiler.BlankLiteral;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.ImmutableDefaultFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.mvel2.util.ErrorUtil;
import org.mvel2.util.ExecutionStack;

/**
 * The MVEL interpreted runtime, used for fast parse and execution of scripts.
 */
@SuppressWarnings({ "CaughtExceptionImmediatelyRethrown" })
public class MVELInterpretedRuntime extends AbstractParser {

    private Object holdOverRegister;

    MVELInterpretedRuntime(char[] expression, Object ctx, Map<String, Object> variables) {
        this.expr = expression;
        this.length = expr.length;
        this.ctx = ctx;
        this.variableFactory = new MapVariableResolverFactory(variables);
    }

    MVELInterpretedRuntime(char[] expression, Object ctx) {
        this.expr = expression;
        this.length = expr.length;
        this.ctx = ctx;
        this.variableFactory = new ImmutableDefaultFactory();
    }

    MVELInterpretedRuntime(String expression) {
        setExpression(expression);
        this.variableFactory = new ImmutableDefaultFactory();
    }

    MVELInterpretedRuntime(char[] expression) {
        this.length = end = (this.expr = expression).length;
    }

    public MVELInterpretedRuntime(char[] expr, Object ctx, VariableResolverFactory resolverFactory) {
        this.length = end = (this.expr = expr).length;
        this.ctx = ctx;
        this.variableFactory = resolverFactory;
    }

    public MVELInterpretedRuntime(char[] expr, int start, int offset, Object ctx, VariableResolverFactory resolverFactory) {
        this.expr = expr;
        this.start = start;
        this.end = start + offset;
        this.length = end - start;
        this.ctx = ctx;
        this.variableFactory = resolverFactory;
    }

    public MVELInterpretedRuntime(char[] expr, int start, int offset, Object ctx, VariableResolverFactory resolverFactory,
            ParserContext pCtx) {
        super(pCtx);
        this.expr = expr;
        this.start = start;
        this.end = start + offset;
        this.length = end - start;
        this.ctx = ctx;
        this.variableFactory = resolverFactory;
    }

    public MVELInterpretedRuntime(String expression, Object ctx, VariableResolverFactory resolverFactory) {
        setExpression(expression);
        this.ctx = ctx;
        this.variableFactory = resolverFactory;
    }

    public MVELInterpretedRuntime(String expression, Object ctx, VariableResolverFactory resolverFactory, ParserContext pCtx) {
        super(pCtx);
        setExpression(expression);
        this.ctx = ctx;
        this.variableFactory = resolverFactory;
    }

    MVELInterpretedRuntime(String expression, VariableResolverFactory resolverFactory) {
        setExpression(expression);
        this.variableFactory = resolverFactory;
    }

    MVELInterpretedRuntime(String expression, Object ctx) {
        setExpression(expression);
        this.ctx = ctx;
        this.variableFactory = new ImmutableDefaultFactory();
    }

    public Object parse() {
        try {
            stk = new ExecutionStack();
            dStack = new ExecutionStack();
            variableFactory.setTiltFlag(false);
            cursor = start;
            return parseAndExecuteInterpreted();
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            throw new CompileException("unexpected end of statement", expr, length);
        } catch (NullPointerException e) {
            e.printStackTrace();

            if (cursor >= length) {
                throw new CompileException("unexpected end of statement", expr, length);
            } else {
                throw e;
            }
        } catch (CompileException e) {
            throw ErrorUtil.rewriteIfNeeded(e, expr, cursor);
        }
    }

    /**
     * Main interpreter loop.
     *
     * @return value
     */
    private Object parseAndExecuteInterpreted() {
        ASTNode tk = null;
        int operator;
        lastWasIdentifier = false;

        try {
            while ((tk = nextToken()) != null) {
                holdOverRegister = null;

                if (lastWasIdentifier && lastNode.isDiscard()) {
                    stk.discard();
                }

                /**
                 * If we are at the beginning of a statement, then we immediately push the first token
                 * onto the stack.
                 */
                if (stk.isEmpty()) {
                    if ((tk.fields & ASTNode.STACKLANG) != 0) {
                        stk.push(tk.getReducedValue(stk, ctx, variableFactory));
                        Object o = stk.peek();
                        if (o instanceof Integer) {
                            arithmeticFunctionReduction((Integer) o);
                        }
                    } else {
                        stk.push(tk.getReducedValue(ctx, ctx, variableFactory));
                    }

                    /**
                     * If this is a substatement, we need to move the result into the d-stack to preserve
                     * proper execution order.
                     */
                    if (tk instanceof Substatement && (tk = nextToken()) != null) {
                        if (isArithmeticOperator(operator = tk.getOperator())) {
                            stk.push(nextToken().getReducedValue(ctx, ctx, variableFactory), operator);

                            if (procBooleanOperator(arithmeticFunctionReduction(operator)) == -1) return stk.peek();
                            else continue;
                        }
                    } else {
                        continue;
                    }
                }

                if (variableFactory.tiltFlag()) {
                    return stk.pop();
                }

                switch (procBooleanOperator(operator = tk.getOperator())) {
                    case RETURN:
                        variableFactory.setTiltFlag(true);
                        return stk.pop();
                    case OP_TERMINATE:
                        return stk.peek();
                    case OP_RESET_FRAME:
                        continue;
                    case OP_OVERFLOW:
                        if (!tk.isOperator()) {
                            if (!(stk.peek() instanceof Class)) {
                                throw new CompileException("unexpected token or unknown identifier:" + tk.getName(), expr, st);
                            }
                            variableFactory.createVariable(tk.getName(), null, (Class) stk.peek());
                        }
                        continue;
                }

                stk.push(nextToken().getReducedValue(ctx, ctx, variableFactory), operator);

                switch ((operator = arithmeticFunctionReduction(operator))) {
                    case OP_TERMINATE:
                        return stk.peek();
                    case OP_RESET_FRAME:
                        continue;
                }

                if (procBooleanOperator(operator) == OP_TERMINATE) return stk.peek();
            }

            if (holdOverRegister != null) {
                return holdOverRegister;
            }
        } catch (CompileException e) {
            throw ErrorUtil.rewriteIfNeeded(e, expr, start);
        } catch (NullPointerException e) {
            if (tk != null && tk.isOperator()) {
                CompileException ce = new CompileException(
                        "incomplete statement: " + tk.getName() + " (possible use of reserved keyword as identifier: " + tk.getName() + ")",
                        expr, st, e);

                ce.setExpr(expr);
                ce.setLineNumber(line);
                ce.setCursor(cursor);
                throw ce;
            } else {
                throw e;
            }
        }
        return stk.peek();
    }

    private int procBooleanOperator(int operator) {
        switch (operator) {
            case RETURN:
                return RETURN;
            case NOOP:
                return -2;

            case AND:
                reduceRight();

                if (!stk.peekBoolean()) {
                    if (unwindStatement(operator)) {
                        return -1;
                    } else {
                        stk.clear();
                        return OP_RESET_FRAME;
                    }
                } else {
                    stk.discard();
                    return OP_RESET_FRAME;
                }

            case OR:
                reduceRight();

                if (stk.peekBoolean()) {
                    if (unwindStatement(operator)) {
                        return OP_TERMINATE;
                    } else {
                        stk.clear();
                        return OP_RESET_FRAME;
                    }
                } else {
                    stk.discard();
                    return OP_RESET_FRAME;
                }

            case CHOR:
                if (!BlankLiteral.INSTANCE.equals(stk.peek())) {
                    return OP_TERMINATE;
                }
                break;

            case TERNARY:
                if (!stk.popBoolean()) {
                    stk.clear();

                    ASTNode tk;

                    for (;;) {
                        if ((tk = nextToken()) == null || tk.isOperator(Operator.TERNARY_ELSE)) break;
                    }
                }

                return OP_RESET_FRAME;

            case TERNARY_ELSE:
                captureToEOS();
                return OP_RESET_FRAME;

            case END_OF_STMT:
                /**
                 * Assignments are a special scenario for dealing with the stack.  Assignments are basically like
                 * held-over failures that basically kickstart the parser when an assignment operator is is
                 * encountered.  The originating token is captured, and the the parser is told to march on.  The
                 * resultant value on the stack is then used to populate the target variable.
                 *
                 * The other scenario in which we don't want to wipe the stack, is when we hit the end of the
                 * statement, because that top stack value is the value we want back from the parser.
                 */

                if (hasMore()) {
                    holdOverRegister = stk.pop();
                    stk.clear();
                }

                return OP_RESET_FRAME;
        }

        return OP_CONTINUE;
    }

    /**
     * This method peforms the equivilent of an XSWAP operation to flip the operator
     * over to the top of the stack, and loads the stored values on the d-stack onto
     * the main program stack.
     */
    private void reduceRight() {
        if (dStack.isEmpty()) return;

        Object o = stk.pop();
        stk.push(dStack.pop(), o, dStack.pop());

        reduce();
    }

    private boolean hasMore() {
        return cursor <= end;
    }

    /**
     * This method is called to unwind the current statement without any reduction or further parsing.
     *
     * @param operator -
     * @return -
     */
    private boolean unwindStatement(int operator) {
        ASTNode tk;

        switch (operator) {
            case AND:
                while ((tk = nextToken()) != null && !tk.isOperator(Operator.END_OF_STMT) && !tk.isOperator(Operator.OR)) {
                    //nothing
                }
                break;
            default:
                while ((tk = nextToken()) != null && !tk.isOperator(Operator.END_OF_STMT)) {
                    //nothing
                }
        }
        return tk == null;
    }
}
