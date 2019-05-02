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

import static org.mvel2.util.ParseTools.captureStringLiteral;
import static org.mvel2.util.ParseTools.isIdentifierPart;
import static org.mvel2.util.ParseTools.isWhitespace;

import java.util.Map;

import org.mvel2.compiler.AbstractParser;
import org.mvel2.util.StringAppender;

/**
 * A simple, fast, macro processor.  This processor works by simply replacing a matched identifier with a set of code.
 */
public class MacroProcessor extends AbstractParser implements PreProcessor {

    private Map<String, Macro> macros;

    public MacroProcessor() {
    }

    public MacroProcessor(Map<String, Macro> macros) {
        this.macros = macros;
    }

    public char[] parse(char[] input) {
        setExpression(input);

        StringAppender appender = new StringAppender();

        int start;
        boolean macroArmed = true;
        String token;

        for (; cursor < length; cursor++) {
            start = cursor;
            while (cursor < length && isIdentifierPart(expr[cursor]))
                cursor++;
            if (cursor > start) {
                if (macros.containsKey(token = new String(expr, start, cursor - start)) && macroArmed) {
                    appender.append(macros.get(token).doMacro());
                } else {
                    appender.append(token);
                }
            }

            if (cursor < length) {
                switch (expr[cursor]) {
                    case '\\':
                        cursor++;
                        break;
                    case '/':
                        start = cursor;

                        if (cursor + 1 != length) {
                            switch (expr[cursor + 1]) {
                                case '/':
                                    while (cursor != length && expr[cursor] != '\n')
                                        cursor++;
                                    break;
                                case '*':
                                    int len = length - 1;
                                    while (cursor != len && !(expr[cursor] == '*' && expr[cursor + 1] == '/'))
                                        cursor++;
                                    cursor += 2;
                                    break;
                            }
                        }

                        if (cursor < length) cursor++;

                        appender.append(new String(expr, start, cursor - start));

                        if (cursor < length) cursor--;
                        break;

                    case '"':
                    case '\'':
                        appender.append(new String(expr, (start = cursor),
                                (cursor = captureStringLiteral(expr[cursor], expr, cursor, length)) - start));

                        if (cursor >= length) break;
                        else if (isIdentifierPart(expr[cursor])) cursor--;

                    default:
                        switch (expr[cursor]) {
                            case '.':
                                macroArmed = false;
                                break;
                            case ';':
                            case '{':
                            case '(':
                                macroArmed = true;
                                break;
                        }

                        appender.append(expr[cursor]);
                }
            }
        }

        return appender.toChars();
    }

    public String parse(String input) {
        return new String(parse(input.toCharArray()));
    }

    public Map<String, Macro> getMacros() {
        return macros;
    }

    public void setMacros(Map<String, Macro> macros) {
        this.macros = macros;
    }

    public void captureToWhitespace() {
        while (cursor < length && !isWhitespace(expr[cursor]))
            cursor++;
    }
}
