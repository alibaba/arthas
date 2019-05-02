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
package org.mvel2.optimizers;

import static java.lang.Thread.currentThread;
import static org.mvel2.util.ParseTools.captureStringLiteral;
import static org.mvel2.util.ParseTools.findInnerClass;
import static org.mvel2.util.ParseTools.forNameWithInner;
import static org.mvel2.util.ParseTools.isIdentifierPart;
import static org.mvel2.util.ParseTools.isWhitespace;

import java.lang.reflect.Method;

import org.mvel2.CompileException;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.compiler.AbstractParser;

/**
 * @author Christopher Brock
 */
public class AbstractOptimizer extends AbstractParser {

    protected static final int BEAN = 0;
    protected static final int METH = 1;
    protected static final int COL = 2;
    protected static final int WITH = 3;

    protected boolean collection = false;
    protected boolean nullSafe = false;
    protected Class currType = null;
    protected boolean staticAccess = false;

    protected int tkStart;

    protected AbstractOptimizer() {
    }

    protected AbstractOptimizer(ParserContext pCtx) {
        super(pCtx);
    }

    /**
     * Try static access of the property, and return an instance of the Field, Method of Class if successful.
     *
     * @return - Field, Method or Class instance.
     */
    protected Object tryStaticAccess() {
        int begin = cursor;
        try {
            /**
             * Try to resolve this *smartly* as a static class reference.
             *
             * This starts at the end of the token and starts to step backwards to figure out whether
             * or not this may be a static class reference.  We search for method calls simply by
             * inspecting for ()'s.  The first union area we come to where no brackets are present is our
             * test-point for a class reference.  If we find a class, we pass the reference to the
             * property accessor along  with trailing methods (if any).
             *
             */
            boolean meth = false;
            // int end = start + length;
            int last = end;
            for (int i = end - 1; i > start; i--) {
                switch (expr[i]) {
                    case '.':
                        if (!meth) {
                            ClassLoader classLoader = pCtx != null ? pCtx.getClassLoader() : currentThread().getContextClassLoader();
                            String test = new String(expr, start, (cursor = last) - start);
                            try {
                                if (MVEL.COMPILER_OPT_SUPPORT_JAVA_STYLE_CLASS_LITERALS && test.endsWith(".class"))
                                    test = test.substring(0, test.length() - 6);

                                return Class.forName(test, true, classLoader);
                            } catch (ClassNotFoundException cnfe) {
                                try {
                                    return findInnerClass(test, classLoader, cnfe);
                                } catch (ClassNotFoundException e) { /* ignore */ }
                                Class cls = forNameWithInner(new String(expr, start, i - start), classLoader);
                                String name = new String(expr, i + 1, end - i - 1);
                                try {
                                    return cls.getField(name);
                                } catch (NoSuchFieldException nfe) {
                                    for (Method m : cls.getMethods()) {
                                        if (name.equals(m.getName())) return m;
                                    }
                                    return null;
                                }
                            }
                        }

                        meth = false;
                        last = i;
                        break;

                    case '}':
                        i--;
                        for (int d = 1; i > start && d != 0; i--) {
                            switch (expr[i]) {
                                case '}':
                                    d++;
                                    break;
                                case '{':
                                    d--;
                                    break;
                                case '"':
                                case '\'':
                                    char s = expr[i];
                                    while (i > start && (expr[i] != s && expr[i - 1] != '\\'))
                                        i--;
                            }
                        }
                        break;

                    case ')':
                        i--;

                        for (int d = 1; i > start && d != 0; i--) {
                            switch (expr[i]) {
                                case ')':
                                    d++;
                                    break;
                                case '(':
                                    d--;
                                    break;
                                case '"':
                                case '\'':
                                    char s = expr[i];
                                    while (i > start && (expr[i] != s && expr[i - 1] != '\\'))
                                        i--;
                            }
                        }

                        meth = true;
                        last = i++;
                        break;

                    case '\'':
                        while (--i > start) {
                            if (expr[i] == '\'' && expr[i - 1] != '\\') {
                                break;
                            }
                        }
                        break;

                    case '"':
                        while (--i > start) {
                            if (expr[i] == '"' && expr[i - 1] != '\\') {
                                break;
                            }
                        }
                        break;
                }
            }
        } catch (Exception cnfe) {
            cursor = begin;
        }

        return null;
    }

    protected int nextSubToken() {
        skipWhitespace();
        nullSafe = false;

        switch (expr[tkStart = cursor]) {
            case '[':
                return COL;
            case '{':
                if (expr[cursor - 1] == '.') {
                    return WITH;
                }
                break;
            case '.':
                if ((start + 1) != end) {
                    switch (expr[cursor = ++tkStart]) {
                        case '?':
                            skipWhitespace();
                            if ((cursor = ++tkStart) == end) {
                                throw new CompileException("unexpected end of statement", expr, start);
                            }
                            nullSafe = true;

                            fields = -1;
                            break;
                        case '{':
                            return WITH;
                        default:
                            if (isWhitespace(expr[tkStart])) {
                                skipWhitespace();
                                tkStart = cursor;
                            }
                    }
                } else {
                    throw new CompileException("unexpected end of statement", expr, start);
                }
                break;
            case '?':
                if (start == cursor) {
                    tkStart++;
                    cursor++;
                    nullSafe = true;
                }
        }

        //noinspection StatementWithEmptyBody
        while (++cursor < end && isIdentifierPart(expr[cursor]));

        skipWhitespace();
        if (cursor < end) {
            switch (expr[cursor]) {
                case '[':
                    return COL;
                case '(':
                    return METH;
                default:
                    return BEAN;
            }
        }

        return 0;
    }

    protected String capture() {
        /**
         * Trim off any whitespace.
         */
        return new String(expr, tkStart = trimRight(tkStart), trimLeft(cursor) - tkStart);
    }

    /**
     * Skip to the next non-whitespace position.
     */
    protected void whiteSpaceSkip() {
        if (cursor < length)
            //noinspection StatementWithEmptyBody
            while (isWhitespace(expr[cursor]) && ++cursor != length);
    }

    /**
     * @param c - character to scan to.
     * @return - returns true is end of statement is hit, false if the scan scar is countered.
     */
    protected boolean scanTo(char c) {
        for (; cursor < end; cursor++) {
            switch (expr[cursor]) {
                case '\'':
                case '"':
                    cursor = captureStringLiteral(expr[cursor], expr, cursor, end);
                default:
                    if (expr[cursor] == c) {
                        return false;
                    }
            }
        }
        return true;
    }

    protected int findLastUnion() {
        int split = -1;
        int depth = 0;

        int end = start + length;
        for (int i = end - 1; i != start; i--) {
            switch (expr[i]) {
                case '}':
                case ']':
                    depth++;
                    break;

                case '{':
                case '[':
                    if (--depth == 0) {
                        split = i;
                        collection = true;
                    }
                    break;
                case '.':
                    if (depth == 0) {
                        split = i;
                    }
                    break;
            }
            if (split != -1) break;
        }

        return split;
    }
}
