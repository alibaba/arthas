package org.mvel2.util;

import static org.mvel2.util.ParseTools.balancedCaptureWithLineAccounting;

import org.mvel2.CompileException;
import org.mvel2.ParserContext;
import org.mvel2.ast.EndOfStatement;
import org.mvel2.ast.Function;

public class FunctionParser {

    private String name;

    private int cursor;
    private int length;

    private int fields;
    private char[] expr;
    private ParserContext pCtx;

    private ExecutionStack splitAccumulator;

    public FunctionParser(String functionName, int cursor, int endOffset, char[] expr, int fields, ParserContext pCtx,
            ExecutionStack splitAccumulator) {

        this.name = functionName;
        this.cursor = cursor;
        this.length = endOffset;

        this.expr = expr;
        this.fields = fields;
        this.pCtx = pCtx;
        this.splitAccumulator = splitAccumulator;
    }

    public Function parse() {
        int start = cursor;

        int startCond = 0;
        int endCond = 0;

        int blockStart;
        int blockEnd;

        int end = cursor + length;

        cursor = ParseTools.captureToNextTokenJunction(expr, cursor, end, pCtx);

        if (expr[cursor = ParseTools.nextNonBlank(expr, cursor)] == '(') {
            /**
             * If we discover an opening bracket after the function name, we check to see
             * if this function accepts parameters.
             */
            endCond = cursor = balancedCaptureWithLineAccounting(expr, startCond = cursor, end, '(', pCtx);
            startCond++;
            cursor++;

            cursor = ParseTools.skipWhitespace(expr, cursor);

            if (cursor >= end) {
                throw new CompileException("incomplete statement", expr, cursor);
            } else if (expr[cursor] == '{') {
                blockEnd = cursor = balancedCaptureWithLineAccounting(expr, blockStart = cursor, end, '{', pCtx);
            } else {
                blockStart = cursor - 1;
                cursor = ParseTools.captureToEOS(expr, cursor, end, pCtx);
                blockEnd = cursor;
            }
        } else {
            /**
             * This function has not parameters.
             */
            if (expr[cursor] == '{') {
                /**
                 * This function is bracketed.  We capture the entire range in the brackets.
                 */
                blockEnd = cursor = balancedCaptureWithLineAccounting(expr, blockStart = cursor, end, '{', pCtx);
            } else {
                /**
                 * This is a single statement function declaration.  We only capture the statement.
                 */
                blockStart = cursor - 1;
                cursor = ParseTools.captureToEOS(expr, cursor, end, pCtx);
                blockEnd = cursor;
            }
        }

        /**
         * Trim any whitespace from the captured block range.
         */
        blockStart = ParseTools.trimRight(expr, blockStart + 1);
        blockEnd = ParseTools.trimLeft(expr, start, blockEnd);

        cursor++;

        /**
         * Check if the function is manually terminated.
         */
        if (splitAccumulator != null && ParseTools.isStatementNotManuallyTerminated(expr, cursor)) {
            /**
             * Add an EndOfStatement to the split accumulator in the parser.
             */
            splitAccumulator.add(new EndOfStatement(pCtx));
        }

        /**
         * Produce the funciton node.
         */
        return new Function(name, expr, startCond, endCond - startCond, blockStart, blockEnd - blockStart, fields, pCtx);
    }

    public String getName() {
        return name;
    }

    public int getCursor() {
        return cursor;
    }
}
