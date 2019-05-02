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

import static java.lang.String.copyValueOf;
import static org.mvel2.util.ParseTools.isWhitespace;
import static org.mvel2.util.ParseTools.repeatChar;

import java.util.Collections;
import java.util.List;

import org.mvel2.util.StringAppender;

/**
 * Standard exception thrown for all general compileShared and some runtime failures.
 */
public class CompileException extends RuntimeException {

    private char[] expr;

    private int cursor = 0;
    private int msgOffset = 0;

    private int lineNumber = 1;
    private int column = 0;

    private int lastLineStart = 0;

    private List<ErrorDetail> errors;

    private Object evaluationContext;

    public CompileException(String message, List<ErrorDetail> errors, char[] expr, int cursor, ParserContext ctx) {
        super(message);
        this.expr = expr;
        this.cursor = cursor;

        if (!errors.isEmpty()) {
            ErrorDetail detail = errors.iterator().next();
            this.cursor = detail.getCursor();
            this.lineNumber = detail.getLineNumber();
            this.column = detail.getColumn();
        }

        this.errors = errors;
    }

    public CompileException(String message, char[] expr, int cursor, Throwable e) {
        super(message, e);
        this.expr = expr;
        this.cursor = cursor;
    }

    public CompileException(String message, char[] expr, int cursor) {
        super(message);
        this.expr = expr;
        this.cursor = cursor;
    }

    public void setEvaluationContext(Object evaluationContext) {
        this.evaluationContext = evaluationContext;
    }

    public String toString() {
        return generateErrorMessage();
    }

    @Override
    public String getMessage() {
        return generateErrorMessage();
    }

    private void calcRowAndColumn() {
        if (lineNumber > 1 || column > 1) return;

        int row = 1;
        int col = 1;

        if ((lineNumber != 0 && column != 0) || expr == null || expr.length == 0) return;

        for (int i = 0; i < cursor && i < expr.length; i++) {
            switch (expr[i]) {
                case '\r':
                    continue;
                case '\n':
                    row++;
                    col = 1;
                    break;

                default:
                    col++;
            }
        }

        this.lineNumber = row;
        this.column = col;
    }

    private CharSequence showCodeNearError(char[] expr, int cursor) {
        if (expr == null) return "Unknown";

        int start = cursor - 20;
        int end = (cursor + 30);

        if (end > expr.length) {
            end = expr.length;
            start -= 30;
        }

        if (start < 0) {
            start = 0;
        }

        String cs;

        int firstCr;
        int lastCr;

        try {
            cs = copyValueOf(expr, start, end - start).trim();
        } catch (StringIndexOutOfBoundsException e) {
            throw e;
        }

        int matchStart = -1;
        int matchOffset = 0;
        String match = null;

        if (cursor < end) {
            matchStart = cursor;
            if (matchStart > 0) {
                while (matchStart > 0 && !isWhitespace(expr[matchStart - 1])) {
                    matchStart--;
                }
            }

            matchOffset = cursor - matchStart;

            match = new String(expr, matchStart, expr.length - matchStart);
            Makematch: for (int i = 0; i < match.length(); i++) {
                switch (match.charAt(i)) {
                    case '\n':
                    case ')':
                        match = match.substring(0, i);
                        break Makematch;
                }
            }

            if (match.length() >= 30) {
                match = match.substring(0, 30);
            }
        }

        do {
            firstCr = cs.indexOf('\n');
            lastCr = cs.lastIndexOf('\n');

            if (firstCr == -1) break;

            int matchIndex = match == null ? 0 : cs.indexOf(match);

            if (firstCr != -1 && firstCr == lastCr) {
                if (firstCr > matchIndex) {
                    cs = cs.substring(0, firstCr);
                } else if (firstCr < matchIndex) {
                    cs = cs.substring(firstCr + 1, cs.length());
                }
            } else if (firstCr < matchIndex) {
                cs = cs.substring(firstCr + 1, lastCr);
            } else {
                cs = cs.substring(0, firstCr);
            }
        } while (true);

        String trimmed = cs.trim();

        if (match != null) {
            msgOffset = trimmed.indexOf(match) + matchOffset;
        } else {
            msgOffset = cs.length() - (cs.length() - trimmed.length());
        }

        if (msgOffset == 0 && matchOffset == 0) {
            msgOffset = cursor;
        }

        return trimmed;
    }

    public CharSequence getCodeNearError() {
        return showCodeNearError(expr, cursor);
    }

    private String generateErrorMessage() {
        StringAppender appender = new StringAppender().append("[Error: " + super.getMessage() + "]\n");

        int offset = appender.length();

        appender.append("[Near : {... ");

        offset = appender.length() - offset;

        appender.append(showCodeNearError(expr, cursor)).append(" ....}]\n").append(repeatChar(' ', offset));

        if (msgOffset < 0) msgOffset = 0;

        appender.append(repeatChar(' ', msgOffset)).append('^');

        calcRowAndColumn();

        if (evaluationContext != null) {
            appender.append("\n").append("In ").append(evaluationContext);
        } else if (lineNumber != -1) {
            appender.append("\n").append("[Line: " + lineNumber + ", Column: " + (column) + "]");
        }
        return appender.toString();
    }

    public char[] getExpr() {
        return expr;
    }

    public void setExpr(char[] expr) {
        this.expr = expr;
    }

    public int getCursor() {
        return cursor;
    }

    public void setCursor(int cursor) {
        this.cursor = cursor;
    }

    public List<ErrorDetail> getErrors() {
        return errors != null ? errors : Collections.<ErrorDetail> emptyList();
    }

    public void setErrors(List<ErrorDetail> errors) {
        this.errors = errors;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public int getCursorOffet() {
        return this.msgOffset;
    }

    public int getLastLineStart() {
        return lastLineStart;
    }

    public void setLastLineStart(int lastLineStart) {
        this.lastLineStart = lastLineStart;
    }
}
