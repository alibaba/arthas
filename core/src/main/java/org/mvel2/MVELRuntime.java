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

import static org.mvel2.Operator.CHOR;
import static org.mvel2.Operator.END_OF_STMT;
import static org.mvel2.Operator.NOOP;
import static org.mvel2.Operator.RETURN;
import static org.mvel2.Operator.TERNARY;
import static org.mvel2.Operator.TERNARY_ELSE;
import static org.mvel2.util.PropertyTools.isEmpty;

import org.mvel2.ast.ASTNode;
import org.mvel2.ast.LineLabel;
import org.mvel2.compiler.CompiledExpression;
import org.mvel2.debug.Debugger;
import org.mvel2.debug.DebuggerContext;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.optimizers.OptimizerFactory;
import org.mvel2.util.ExecutionStack;

/**
 * This class contains the runtime for running compiled MVEL expressions.
 */
@SuppressWarnings({ "CaughtExceptionImmediatelyRethrown" })
public class MVELRuntime {

    // public static final ImmutableDefaultFactory IMMUTABLE_DEFAULT_FACTORY = new ImmutableDefaultFactory();
    private static ThreadLocal<DebuggerContext> debuggerContext;

    /**
     * Main interpreter.
     *
     * @param debugger        Run in debug mode
     * @param expression      The compiled expression object
     * @param ctx             The root context object
     * @param variableFactory The variable factory to be injected
     * @return The resultant value
     * @see MVEL
     */
    public static Object execute(boolean debugger, final CompiledExpression expression, final Object ctx,
            VariableResolverFactory variableFactory) {

        Object v1, v2;
        ExecutionStack stk = new ExecutionStack();

        ASTNode tk = expression.getFirstNode();
        Integer operator;

        if (tk == null) return null;
        try {
            do {
                if (tk.fields == -1) {
                    /**
                     * This may seem silly and redundant, however, when an MVEL script recurses into a block
                     * or substatement, a new runtime loop is entered.   Since the debugger state is not
                     * passed through the AST, it is not possible to forward the state directly.  So when we
                     * encounter a debugging symbol, we check the thread local to see if there is are registered
                     * breakpoints.  If we find them, we assume that we are debugging.
                     *
                     * The consequence of this of course, is that it's not ideal to compileShared expressions with
                     * debugging symbols which you plan to use in a production enviroment.
                     */
                    if (debugger || (debugger = hasDebuggerContext())) {
                        try {
                            debuggerContext.get().checkBreak((LineLabel) tk, variableFactory, expression);
                        } catch (NullPointerException e) {
                            // do nothing for now.  this isn't as calus as it seems.
                        }
                    }
                    continue;
                } else if (stk.isEmpty()) {
                    stk.push(tk.getReducedValueAccelerated(ctx, ctx, variableFactory));
                }

                if (variableFactory.tiltFlag()) {
                    return stk.pop();
                }

                switch (operator = tk.getOperator()) {
                    case RETURN:
                        variableFactory.setTiltFlag(true);
                        return stk.pop();

                    case NOOP:
                        continue;

                    case TERNARY:
                        if (!stk.popBoolean()) {
                            //noinspection StatementWithEmptyBody
                            while (tk.nextASTNode != null && !(tk = tk.nextASTNode).isOperator(TERNARY_ELSE));
                        }
                        stk.clear();
                        continue;

                    case TERNARY_ELSE:
                        return stk.pop();

                    case END_OF_STMT:
                        /**
                         * If the program doesn't end here then we wipe anything off the stack that remains.
                         * Althought it may seem like intuitive stack optimizations could be leveraged by
                         * leaving hanging values on the stack,  trust me it's not a good idea.
                         */
                        if (tk.nextASTNode != null) {
                            stk.clear();
                        }

                        continue;
                }

                stk.push(tk.nextASTNode.getReducedValueAccelerated(ctx, ctx, variableFactory), operator);

                try {
                    while (stk.isReduceable()) {
                        if ((Integer) stk.peek() == CHOR) {
                            stk.pop();
                            v1 = stk.pop();
                            v2 = stk.pop();
                            if (!isEmpty(v2) || !isEmpty(v1)) {
                                stk.clear();
                                stk.push(!isEmpty(v2) ? v2 : v1);
                            } else stk.push(null);
                        } else {
                            stk.op();
                        }
                    }
                } catch (ClassCastException e) {
                    throw new CompileException("syntax error or incomptable types", new char[0], 0, e);
                } catch (CompileException e) {
                    throw e;
                } catch (Exception e) {
                    throw new CompileException("failed to compileShared sub expression", new char[0], 0, e);
                }
            } while ((tk = tk.nextASTNode) != null);

            return stk.peek();
        } catch (NullPointerException e) {
            if (tk != null && tk.isOperator() && tk.nextASTNode != null) {
                throw new CompileException(
                        "incomplete statement: " + tk.getName() + " (possible use of reserved keyword as identifier: " + tk.getName() + ")",
                        tk.getExpr(), tk.getStart());
            } else {
                throw e;
            }
        } finally {
            OptimizerFactory.clearThreadAccessorOptimizer();
        }
    }

    /**
     * Register a debugger breakpoint.
     *
     * @param source - the source file the breakpoint is registered in
     * @param line   - the line number of the breakpoint
     */
    public static void registerBreakpoint(String source, int line) {
        ensureDebuggerContext();
        debuggerContext.get().registerBreakpoint(source, line);
    }

    /**
     * Remove a specific breakpoint.
     *
     * @param source - the source file the breakpoint is registered in
     * @param line   - the line number of the breakpoint to be removed
     */
    public static void removeBreakpoint(String source, int line) {
        if (hasDebuggerContext()) {
            debuggerContext.get().removeBreakpoint(source, line);
        }
    }

    /**
     * Tests whether or not a debugger context exist.
     *
     * @return boolean
     */
    public static boolean hasDebuggerContext() {
        return debuggerContext != null && debuggerContext.get() != null;
    }

    /**
     * Ensures that debugger context exists.
     */
    private static void ensureDebuggerContext() {
        if (debuggerContext == null) debuggerContext = new ThreadLocal<DebuggerContext>();
        if (debuggerContext.get() == null) debuggerContext.set(new DebuggerContext());
    }

    /**
     * Reset all the currently registered breakpoints.
     */
    public static void clearAllBreakpoints() {
        if (hasDebuggerContext()) {
            debuggerContext.get().clearAllBreakpoints();
        }
    }

    /**
     * Tests whether or not breakpoints have been declared.
     *
     * @return boolean
     */
    public static boolean hasBreakpoints() {
        return hasDebuggerContext() && debuggerContext.get().hasBreakpoints();
    }

    /**
     * Sets the Debugger instance to handle breakpoints.   A debugger may only be registered once per thread.
     * Calling this method more than once will result in the second and subsequent calls to simply fail silently.
     * To re-register the Debugger, you must call {@link #resetDebugger}
     *
     * @param debugger - debugger instance
     */
    public static void setThreadDebugger(Debugger debugger) {
        ensureDebuggerContext();
        debuggerContext.get().setDebugger(debugger);
    }

    /**
     * Reset all information registered in the debugger, including the actual attached Debugger and registered
     * breakpoints.
     */
    public static void resetDebugger() {
        debuggerContext = null;
    }
}
