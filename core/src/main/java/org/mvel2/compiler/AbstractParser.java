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
package org.mvel2.compiler;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.mvel2.Operator.ADD;
import static org.mvel2.Operator.AND;
import static org.mvel2.Operator.ASSERT;
import static org.mvel2.Operator.ASSIGN;
import static org.mvel2.Operator.ASSIGN_ADD;
import static org.mvel2.Operator.ASSIGN_DIV;
import static org.mvel2.Operator.ASSIGN_MOD;
import static org.mvel2.Operator.ASSIGN_SUB;
import static org.mvel2.Operator.BW_AND;
import static org.mvel2.Operator.BW_OR;
import static org.mvel2.Operator.BW_SHIFT_LEFT;
import static org.mvel2.Operator.BW_SHIFT_RIGHT;
import static org.mvel2.Operator.BW_USHIFT_LEFT;
import static org.mvel2.Operator.BW_USHIFT_RIGHT;
import static org.mvel2.Operator.BW_XOR;
import static org.mvel2.Operator.CHOR;
import static org.mvel2.Operator.CONTAINS;
import static org.mvel2.Operator.CONVERTABLE_TO;
import static org.mvel2.Operator.DEC;
import static org.mvel2.Operator.DIV;
import static org.mvel2.Operator.DO;
import static org.mvel2.Operator.ELSE;
import static org.mvel2.Operator.END_OF_STMT;
import static org.mvel2.Operator.EQUAL;
import static org.mvel2.Operator.FOR;
import static org.mvel2.Operator.FOREACH;
import static org.mvel2.Operator.FUNCTION;
import static org.mvel2.Operator.GETHAN;
import static org.mvel2.Operator.GTHAN;
import static org.mvel2.Operator.IF;
import static org.mvel2.Operator.IMPORT;
import static org.mvel2.Operator.IMPORT_STATIC;
import static org.mvel2.Operator.INC;
import static org.mvel2.Operator.INSTANCEOF;
import static org.mvel2.Operator.ISDEF;
import static org.mvel2.Operator.LETHAN;
import static org.mvel2.Operator.LTHAN;
import static org.mvel2.Operator.MOD;
import static org.mvel2.Operator.MULT;
import static org.mvel2.Operator.NEQUAL;
import static org.mvel2.Operator.NEW;
import static org.mvel2.Operator.OR;
import static org.mvel2.Operator.POWER;
import static org.mvel2.Operator.PROJECTION;
import static org.mvel2.Operator.PROTO;
import static org.mvel2.Operator.PTABLE;
import static org.mvel2.Operator.REGEX;
import static org.mvel2.Operator.RETURN;
import static org.mvel2.Operator.SIMILARITY;
import static org.mvel2.Operator.SOUNDEX;
import static org.mvel2.Operator.STACKLANG;
import static org.mvel2.Operator.STR_APPEND;
import static org.mvel2.Operator.SUB;
import static org.mvel2.Operator.SWITCH;
import static org.mvel2.Operator.TERNARY;
import static org.mvel2.Operator.TERNARY_ELSE;
import static org.mvel2.Operator.UNTIL;
import static org.mvel2.Operator.UNTYPED_VAR;
import static org.mvel2.Operator.WHILE;
import static org.mvel2.Operator.WITH;
import static org.mvel2.ast.TypeDescriptor.getClassReference;
import static org.mvel2.util.ArrayTools.findFirst;
import static org.mvel2.util.ParseTools.balancedCaptureWithLineAccounting;
import static org.mvel2.util.ParseTools.captureStringLiteral;
import static org.mvel2.util.ParseTools.containsCheck;
import static org.mvel2.util.ParseTools.createStringTrimmed;
import static org.mvel2.util.ParseTools.handleStringEscapes;
import static org.mvel2.util.ParseTools.isArrayType;
import static org.mvel2.util.ParseTools.isDigit;
import static org.mvel2.util.ParseTools.isIdentifierPart;
import static org.mvel2.util.ParseTools.isNotValidNameorLabel;
import static org.mvel2.util.ParseTools.isPropertyOnly;
import static org.mvel2.util.ParseTools.isReservedWord;
import static org.mvel2.util.ParseTools.isWhitespace;
import static org.mvel2.util.ParseTools.opLookup;
import static org.mvel2.util.ParseTools.similarity;
import static org.mvel2.util.ParseTools.subset;
import static org.mvel2.util.PropertyTools.isEmpty;
import static org.mvel2.util.Soundex.soundex;

import java.io.Serializable;
import java.util.HashMap;
import java.util.WeakHashMap;

import org.mvel2.CompileException;
import org.mvel2.ErrorDetail;
import org.mvel2.Operator;
import org.mvel2.ParserContext;
import org.mvel2.ast.ASTNode;
import org.mvel2.ast.AssertNode;
import org.mvel2.ast.AssignmentNode;
import org.mvel2.ast.BooleanNode;
import org.mvel2.ast.DeclProtoVarNode;
import org.mvel2.ast.DeclTypedVarNode;
import org.mvel2.ast.DeepAssignmentNode;
import org.mvel2.ast.DoNode;
import org.mvel2.ast.DoUntilNode;
import org.mvel2.ast.EndOfStatement;
import org.mvel2.ast.Fold;
import org.mvel2.ast.ForEachNode;
import org.mvel2.ast.ForNode;
import org.mvel2.ast.Function;
import org.mvel2.ast.IfNode;
import org.mvel2.ast.ImportNode;
import org.mvel2.ast.IndexedAssignmentNode;
import org.mvel2.ast.IndexedDeclTypedVarNode;
import org.mvel2.ast.IndexedOperativeAssign;
import org.mvel2.ast.IndexedPostFixDecNode;
import org.mvel2.ast.IndexedPostFixIncNode;
import org.mvel2.ast.IndexedPreFixDecNode;
import org.mvel2.ast.IndexedPreFixIncNode;
import org.mvel2.ast.InlineCollectionNode;
import org.mvel2.ast.InterceptorWrapper;
import org.mvel2.ast.Invert;
import org.mvel2.ast.IsDef;
import org.mvel2.ast.LineLabel;
import org.mvel2.ast.LiteralDeepPropertyNode;
import org.mvel2.ast.LiteralNode;
import org.mvel2.ast.Negation;
import org.mvel2.ast.NewObjectNode;
import org.mvel2.ast.NewObjectPrototype;
import org.mvel2.ast.NewPrototypeNode;
import org.mvel2.ast.OperativeAssign;
import org.mvel2.ast.OperatorNode;
import org.mvel2.ast.PostFixDecNode;
import org.mvel2.ast.PostFixIncNode;
import org.mvel2.ast.PreFixDecNode;
import org.mvel2.ast.PreFixIncNode;
import org.mvel2.ast.Proto;
import org.mvel2.ast.ProtoVarNode;
import org.mvel2.ast.RedundantCodeException;
import org.mvel2.ast.RegExMatch;
import org.mvel2.ast.ReturnNode;
import org.mvel2.ast.Sign;
import org.mvel2.ast.Stacklang;
import org.mvel2.ast.StaticImportNode;
import org.mvel2.ast.Substatement;
import org.mvel2.ast.ThisWithNode;
import org.mvel2.ast.TypeCast;
import org.mvel2.ast.TypeDescriptor;
import org.mvel2.ast.TypedVarNode;
import org.mvel2.ast.Union;
import org.mvel2.ast.UntilNode;
import org.mvel2.ast.WhileNode;
import org.mvel2.ast.WithNode;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.util.ErrorUtil;
import org.mvel2.util.ExecutionStack;
import org.mvel2.util.FunctionParser;
import org.mvel2.util.ProtoParser;

/**
 * This is the core parser that the subparsers extend.
 *
 * @author Christopher Brock
 */
public class AbstractParser implements Parser, Serializable {

    public static final int LEVEL_5_CONTROL_FLOW = 5;
    public static final int LEVEL_4_ASSIGNMENT = 4;
    public static final int LEVEL_3_ITERATION = 3;
    public static final int LEVEL_2_MULTI_STATEMENT = 2;
    public static final int LEVEL_1_BASIC_LANG = 1;
    public static final int LEVEL_0_PROPERTY_ONLY = 0;
    protected static final int OP_NOT_LITERAL = -3;
    protected static final int OP_OVERFLOW = -2;
    protected static final int OP_TERMINATE = -1;
    protected static final int OP_RESET_FRAME = 0;
    protected static final int OP_CONTINUE = 1;
    protected static final int SET = 0;
    protected static final int REMOVE = 1;
    protected static final int GET = 2;
    protected static final int GET_OR_CREATE = 3;
    private static final WeakHashMap<String, char[]> EX_PRECACHE = new WeakHashMap<String, char[]>(15);
    public static HashMap<String, Object> LITERALS;
    public static HashMap<String, Object> CLASS_LITERALS;
    public static HashMap<String, Integer> OPERATORS;

    static {
        setupParser();
    }

    protected char[] expr;
    protected int cursor;
    protected int start;
    protected int length;
    protected int end;
    protected int st;
    protected int fields;
    protected boolean greedy = true;
    protected boolean lastWasIdentifier = false;
    protected boolean lastWasLineLabel = false;
    protected boolean lastWasComment = false;
    protected boolean compileMode = false;
    protected int literalOnly = -1;
    protected int lastLineStart = 0;
    protected int line = 0;
    protected ASTNode lastNode;
    protected ExecutionStack stk;
    protected ExecutionStack splitAccumulator = new ExecutionStack();
    protected ParserContext pCtx;
    protected ExecutionStack dStack;
    protected Object ctx;
    protected VariableResolverFactory variableFactory;
    protected boolean debugSymbols = false;

    protected AbstractParser() {
        pCtx = new ParserContext();
    }

    protected AbstractParser(ParserContext pCtx) {
        this.pCtx = pCtx != null ? pCtx : new ParserContext();
    }

    /**
     * This method is internally called by the static initializer for AbstractParser in order to setup the parser.
     * The static initialization populates the operator and literal tables for the parser.  In some situations, like
     * OSGi, it may be necessary to utilize this manually.
     */
    public static void setupParser() {
        if (LITERALS == null || LITERALS.isEmpty()) {
            LITERALS = new HashMap<String, Object>();
            CLASS_LITERALS = new HashMap<String, Object>();
            OPERATORS = new HashMap<String, Integer>();

            /**
             * Add System and all the class wrappers from the JCL.
             */
            CLASS_LITERALS.put("System", System.class);
            CLASS_LITERALS.put("String", String.class);
            CLASS_LITERALS.put("CharSequence", CharSequence.class);

            CLASS_LITERALS.put("Integer", Integer.class);
            CLASS_LITERALS.put("int", int.class);

            CLASS_LITERALS.put("Long", Long.class);
            CLASS_LITERALS.put("long", long.class);

            CLASS_LITERALS.put("Boolean", Boolean.class);
            CLASS_LITERALS.put("boolean", boolean.class);

            CLASS_LITERALS.put("Short", Short.class);
            CLASS_LITERALS.put("short", short.class);

            CLASS_LITERALS.put("Character", Character.class);
            CLASS_LITERALS.put("char", char.class);

            CLASS_LITERALS.put("Double", Double.class);
            CLASS_LITERALS.put("double", double.class);

            CLASS_LITERALS.put("Float", Float.class);
            CLASS_LITERALS.put("float", float.class);

            CLASS_LITERALS.put("Byte", Byte.class);
            CLASS_LITERALS.put("byte", byte.class);

            CLASS_LITERALS.put("Math", Math.class);
            CLASS_LITERALS.put("Void", Void.class);
            CLASS_LITERALS.put("Object", Object.class);
            CLASS_LITERALS.put("Number", Number.class);

            CLASS_LITERALS.put("Class", Class.class);
            CLASS_LITERALS.put("ClassLoader", ClassLoader.class);
            CLASS_LITERALS.put("Runtime", Runtime.class);
            CLASS_LITERALS.put("Thread", Thread.class);
            CLASS_LITERALS.put("Compiler", Compiler.class);
            CLASS_LITERALS.put("StringBuffer", StringBuffer.class);
            CLASS_LITERALS.put("ThreadLocal", ThreadLocal.class);
            CLASS_LITERALS.put("SecurityManager", SecurityManager.class);
            CLASS_LITERALS.put("StrictMath", StrictMath.class);

            CLASS_LITERALS.put("Exception", Exception.class);

            CLASS_LITERALS.put("Array", java.lang.reflect.Array.class);

            CLASS_LITERALS.put("StringBuilder", StringBuilder.class);

            // Setup LITERALS
            LITERALS.putAll(CLASS_LITERALS);
            LITERALS.put("true", TRUE);
            LITERALS.put("false", FALSE);

            LITERALS.put("null", null);
            LITERALS.put("nil", null);

            LITERALS.put("empty", BlankLiteral.INSTANCE);

            setLanguageLevel(Boolean.getBoolean("mvel.future.lang.support") ? 6 : 5);
        }
    }

    public static void setLanguageLevel(int level) {
        OPERATORS.clear();
        OPERATORS.putAll(loadLanguageFeaturesByLevel(level));
    }

    public static HashMap<String, Integer> loadLanguageFeaturesByLevel(int languageLevel) {
        HashMap<String, Integer> operatorsTable = new HashMap<String, Integer>();
        switch (languageLevel) {
            case 6: // prototype definition
                operatorsTable.put("proto", PROTO);

            case 5: // control flow operations
                operatorsTable.put("if", IF);
                operatorsTable.put("else", ELSE);
                operatorsTable.put("?", TERNARY);
                operatorsTable.put("switch", SWITCH);
                operatorsTable.put("function", FUNCTION);
                operatorsTable.put("def", FUNCTION);
                operatorsTable.put("stacklang", STACKLANG);

            case 4: // assignment
                operatorsTable.put("=", ASSIGN);
                operatorsTable.put("var", UNTYPED_VAR);
                operatorsTable.put("+=", ASSIGN_ADD);
                operatorsTable.put("-=", ASSIGN_SUB);
                operatorsTable.put("/=", ASSIGN_DIV);
                operatorsTable.put("%=", ASSIGN_MOD);

            case 3: // iteration
                operatorsTable.put("foreach", FOREACH);
                operatorsTable.put("while", WHILE);
                operatorsTable.put("until", UNTIL);
                operatorsTable.put("for", FOR);
                operatorsTable.put("do", DO);

            case 2: // multi-statement
                operatorsTable.put("return", RETURN);
                operatorsTable.put(";", END_OF_STMT);

            case 1: // boolean, math ops, projection, assertion, objection creation, block setters, imports
                operatorsTable.put("+", ADD);
                operatorsTable.put("-", SUB);
                operatorsTable.put("*", MULT);
                operatorsTable.put("**", POWER);
                operatorsTable.put("/", DIV);
                operatorsTable.put("%", MOD);
                operatorsTable.put("==", EQUAL);
                operatorsTable.put("!=", NEQUAL);
                operatorsTable.put(">", GTHAN);
                operatorsTable.put(">=", GETHAN);
                operatorsTable.put("<", LTHAN);
                operatorsTable.put("<=", LETHAN);
                operatorsTable.put("&&", AND);
                operatorsTable.put("and", AND);
                operatorsTable.put("||", OR);
                operatorsTable.put("or", CHOR);
                operatorsTable.put("~=", REGEX);
                operatorsTable.put("instanceof", INSTANCEOF);
                operatorsTable.put("is", INSTANCEOF);
                operatorsTable.put("contains", CONTAINS);
                operatorsTable.put("soundslike", SOUNDEX);
                operatorsTable.put("strsim", SIMILARITY);
                operatorsTable.put("convertable_to", CONVERTABLE_TO);
                operatorsTable.put("isdef", ISDEF);

                operatorsTable.put("#", STR_APPEND);

                operatorsTable.put("&", BW_AND);
                operatorsTable.put("|", BW_OR);
                operatorsTable.put("^", BW_XOR);
                operatorsTable.put("<<", BW_SHIFT_LEFT);
                operatorsTable.put("<<<", BW_USHIFT_LEFT);
                operatorsTable.put(">>", BW_SHIFT_RIGHT);
                operatorsTable.put(">>>", BW_USHIFT_RIGHT);

                operatorsTable.put("new", Operator.NEW);
                operatorsTable.put("in", PROJECTION);

                operatorsTable.put("with", WITH);

                operatorsTable.put("assert", ASSERT);
                operatorsTable.put("import", IMPORT);
                operatorsTable.put("import_static", IMPORT_STATIC);

                operatorsTable.put("++", INC);
                operatorsTable.put("--", DEC);

            case 0: // Property access and inline collections
                operatorsTable.put(":", TERNARY_ELSE);
        }
        return operatorsTable;
    }

    protected static boolean isArithmeticOperator(int operator) {
        return operator != -1 && operator < 6;
    }

    private static int asInt(final Object o) {
        return (Integer) o;
    }

    protected ASTNode nextTokenSkipSymbols() {
        ASTNode n = nextToken();
        if (n != null && n.getFields() == -1) n = nextToken();
        return n;
    }

    /**
     * Retrieve the next token in the expression.
     *
     * @return -
     */
    protected ASTNode nextToken() {
        try {
            /**
             * If the cursor is at the end of the expression, we have nothing more to do:
             * return null.
             */
            if (!splitAccumulator.isEmpty()) {
                lastNode = (ASTNode) splitAccumulator.pop();
                if (cursor >= end && lastNode instanceof EndOfStatement) {
                    return nextToken();
                } else {
                    return lastNode;
                }
            } else if (cursor >= end) {
                return null;
            }

            int brace, idx;
            int tmpStart;

            String name;
            /**
             * Because of parser recursion for sub-expression parsing, we sometimes need to remain
             * certain field states.  We do not reset for assignments, boolean mode, list creation or
             * a capture only mode.
             */

            boolean capture = false, union = false;

            if ((fields & ASTNode.COMPILE_IMMEDIATE) != 0) {
                debugSymbols = pCtx.isDebugSymbols();
            }

            if (debugSymbols) {
                if (!lastWasLineLabel) {
                    if (pCtx.getSourceFile() == null) {
                        throw new CompileException("unable to produce debugging symbols: source name must be provided.", expr, st);
                    }

                    if (!pCtx.isLineMapped(pCtx.getSourceFile())) {
                        pCtx.initLineMapping(pCtx.getSourceFile(), expr);
                    }

                    skipWhitespace();

                    if (cursor >= end) {
                        return null;
                    }

                    int line = pCtx.getLineFor(pCtx.getSourceFile(), cursor);

                    if (!pCtx.isVisitedLine(pCtx.getSourceFile(), pCtx.setLineCount(line)) && !pCtx.isBlockSymbols()) {
                        lastWasLineLabel = true;
                        pCtx.visitLine(pCtx.getSourceFile(), line);

                        return lastNode = pCtx.setLastLineLabel(new LineLabel(pCtx.getSourceFile(), line, pCtx));
                    }
                } else {
                    lastWasComment = lastWasLineLabel = false;
                }
            }

            /**
             * Skip any whitespace currently under the starting point.
             */
            skipWhitespace();

            /**
             * From here to the end of the method is the core MVEL parsing code.  Fiddling around here is asking for
             * trouble unless you really know what you're doing.
             */

            st = cursor;

            Mainloop: while (cursor != end) {
                if (isIdentifierPart(expr[cursor])) {
                    capture = true;
                    cursor++;

                    while (cursor != end && isIdentifierPart(expr[cursor]))
                        cursor++;
                }

                /**
                 * If the current character under the cursor is a valid
                 * part of an identifier, we keep capturing.
                 */

                if (capture) {
                    String t;
                    if (OPERATORS.containsKey(t = new String(expr, st, cursor - st)) && !Character.isDigit(expr[st])) {
                        switch (OPERATORS.get(t)) {
                            case NEW:
                                if (!isIdentifierPart(expr[st = cursor = trimRight(cursor)])) {
                                    throw new CompileException("unexpected character (expected identifier): " + expr[cursor], expr, st);
                                }

                                /**
                                 * Capture the beginning part of the token.
                                 */
                                do {
                                    captureToNextTokenJunction();
                                    skipWhitespace();
                                } while (cursor < end && expr[cursor] == '[');

                                /**
                                 * If it's not a dimentioned array, continue capturing if necessary.
                                 */
                                if (cursor < end && !lastNonWhite(']')) captureToEOT();

                                TypeDescriptor descr = new TypeDescriptor(expr, st, trimLeft(cursor) - st, fields);

                                if (pCtx.getFunctions().containsKey(descr.getClassName())) {
                                    return lastNode = new NewObjectPrototype(pCtx, pCtx.getFunction(descr.getClassName()));
                                }

                                if (pCtx.hasProtoImport(descr.getClassName())) {
                                    return lastNode = new NewPrototypeNode(descr, pCtx);
                                }

                                lastNode = new NewObjectNode(descr, fields, pCtx);

                                skipWhitespace();
                                if (cursor != end && expr[cursor] == '{') {
                                    if (!((NewObjectNode) lastNode).getTypeDescr().isUndimensionedArray()) {
                                        throw new CompileException("conflicting syntax: dimensioned array with initializer block", expr,
                                                st);
                                    }

                                    st = cursor;
                                    Class egressType = lastNode.getEgressType();

                                    if (egressType == null) {
                                        try {
                                            egressType = getClassReference(pCtx, descr);
                                        } catch (ClassNotFoundException e) {
                                            throw new CompileException("could not instantiate class", expr, st, e);
                                        }
                                    }

                                    cursor = balancedCaptureWithLineAccounting(expr, st, end, expr[cursor], pCtx) + 1;
                                    if (tokenContinues()) {
                                        lastNode = new InlineCollectionNode(expr, st, cursor - st, fields, egressType, pCtx);
                                        st = cursor;
                                        captureToEOT();
                                        return lastNode = new Union(expr, st + 1, cursor, fields, lastNode, pCtx);
                                    } else {
                                        return lastNode = new InlineCollectionNode(expr, st, cursor - st, fields, egressType, pCtx);
                                    }
                                } else if (((NewObjectNode) lastNode).getTypeDescr().isUndimensionedArray()) {
                                    throw new CompileException("array initializer expected", expr, st);
                                }
                                st = cursor;

                                return lastNode;

                            case ASSERT:
                                st = cursor = trimRight(cursor);
                                captureToEOS();
                                return lastNode = new AssertNode(expr, st, cursor-- - st, fields, pCtx);

                            case RETURN:
                                st = cursor = trimRight(cursor);
                                captureToEOS();
                                return lastNode = new ReturnNode(expr, st, cursor - st, fields, pCtx);

                            case IF:
                                return captureCodeBlock(ASTNode.BLOCK_IF);

                            case ELSE:
                                throw new CompileException("else without if", expr, st);

                            case FOREACH:
                                return captureCodeBlock(ASTNode.BLOCK_FOREACH);

                            case WHILE:
                                return captureCodeBlock(ASTNode.BLOCK_WHILE);

                            case UNTIL:
                                return captureCodeBlock(ASTNode.BLOCK_UNTIL);

                            case FOR:
                                return captureCodeBlock(ASTNode.BLOCK_FOR);

                            case WITH:
                                return captureCodeBlock(ASTNode.BLOCK_WITH);

                            case DO:
                                return captureCodeBlock(ASTNode.BLOCK_DO);

                            case STACKLANG:
                                return captureCodeBlock(STACKLANG);

                            case PROTO:
                                return captureCodeBlock(PROTO);

                            case ISDEF:
                                st = cursor = trimRight(cursor);
                                captureToNextTokenJunction();
                                return lastNode = new IsDef(expr, st, cursor - st, pCtx);

                            case IMPORT:
                                st = cursor = trimRight(cursor);
                                captureToEOS();
                                ImportNode importNode = new ImportNode(expr, st, cursor - st, pCtx);

                                if (importNode.isPackageImport()) {
                                    pCtx.addPackageImport(importNode.getPackageImport());
                                } else {
                                    pCtx.addImport(importNode.getImportClass().getSimpleName(), importNode.getImportClass());
                                }
                                return lastNode = importNode;

                            case IMPORT_STATIC:
                                st = cursor = trimRight(cursor);
                                captureToEOS();
                                StaticImportNode staticImportNode = new StaticImportNode(expr, st, trimLeft(cursor) - st, pCtx);
                                pCtx.addImport(staticImportNode.getMethod().getName(), staticImportNode.getMethod());
                                return lastNode = staticImportNode;

                            case FUNCTION:
                                lastNode = captureCodeBlock(FUNCTION);
                                st = cursor + 1;
                                return lastNode;

                            case UNTYPED_VAR:
                                int end;
                                st = cursor + 1;

                                while (true) {
                                    captureToEOT();
                                    end = cursor;
                                    skipWhitespace();

                                    if (cursor != end && expr[cursor] == '=') {
                                        if (end == (cursor = st)) throw new CompileException("illegal use of reserved word: var", expr, st);

                                        continue Mainloop;
                                    } else {
                                        name = new String(expr, st, end - st);
                                        if (pCtx != null && (idx = pCtx.variableIndexOf(name)) != -1) {
                                            splitAccumulator
                                                    .add(lastNode = new IndexedDeclTypedVarNode(idx, st, end - st, Object.class, pCtx));
                                        } else {
                                            splitAccumulator.add(
                                                    lastNode = new DeclTypedVarNode(name, expr, st, end - st, Object.class, fields, pCtx));
                                        }
                                    }

                                    if (cursor == this.end || expr[cursor] != ',') break;
                                    else {
                                        cursor++;
                                        skipWhitespace();
                                        st = cursor;
                                    }
                                }

                                return (ASTNode) splitAccumulator.pop();

                            case CONTAINS:
                                lastWasIdentifier = false;
                                return lastNode = new OperatorNode(Operator.CONTAINS, expr, st, pCtx);

                        }
                    }

                    skipWhitespace();

                    /**
                     * If we *were* capturing a token, and we just hit a non-identifier
                     * character, we stop and figure out what to do.
                     */
                    if (cursor != end && expr[cursor] == '(') {
                        cursor = balancedCaptureWithLineAccounting(expr, cursor, end, '(', pCtx) + 1;
                    }

                    /**
                     * If we encounter any of the following cases, we are still dealing with
                     * a contiguous token.
                     */
                    CaptureLoop: while (cursor != end) {
                        switch (expr[cursor]) {
                            case '.':
                                union = true;
                                cursor++;
                                skipWhitespace();

                                continue;

                            case '?':
                                if (lookToLast() == '.' || cursor == start) {
                                    union = true;
                                    cursor++;
                                    continue;
                                } else {
                                    break CaptureLoop;
                                }

                            case '+':
                                switch (lookAhead()) {
                                    case '+':
                                        name = new String(subArray(st, trimLeft(cursor)));
                                        if (pCtx != null && (idx = pCtx.variableIndexOf(name)) != -1) {
                                            lastNode = new IndexedPostFixIncNode(idx, pCtx);
                                        } else {
                                            lastNode = new PostFixIncNode(name, pCtx);
                                        }

                                        cursor += 2;

                                        expectEOS();

                                        return lastNode;

                                    case '=':
                                        name = createStringTrimmed(expr, st, cursor - st);
                                        st = cursor += 2;

                                        captureToEOS();

                                        if (union) {
                                            return lastNode = new DeepAssignmentNode(expr, st = trimRight(st), trimLeft(cursor) - st,
                                                    fields, ADD, name, pCtx);
                                        } else if (pCtx != null && (idx = pCtx.variableIndexOf(name)) != -1) {
                                            return lastNode = new IndexedAssignmentNode(expr, st, cursor - st, fields, ADD, name, idx,
                                                    pCtx);
                                        } else {
                                            return lastNode = new OperativeAssign(name, expr, st = trimRight(st), trimLeft(cursor) - st,
                                                    ADD, fields, pCtx);
                                        }
                                }

                                if (isDigit(lookAhead()) && cursor > 1 && (expr[cursor - 1] == 'E' || expr[cursor - 1] == 'e')
                                        && isDigit(expr[cursor - 2])) {
                                    cursor++;
                                    //     capture = true;
                                    continue Mainloop;
                                }
                                break CaptureLoop;

                            case '-':
                                switch (lookAhead()) {
                                    case '-':
                                        name = new String(subArray(st, trimLeft(cursor)));
                                        if (pCtx != null && (idx = pCtx.variableIndexOf(name)) != -1) {
                                            lastNode = new IndexedPostFixDecNode(idx, pCtx);
                                        } else {
                                            lastNode = new PostFixDecNode(name, pCtx);
                                        }
                                        cursor += 2;

                                        expectEOS();

                                        return lastNode;

                                    case '=':
                                        name = new String(expr, st, trimLeft(cursor) - st);
                                        st = cursor += 2;

                                        captureToEOS();

                                        if (union) {
                                            return lastNode = new DeepAssignmentNode(expr, st, cursor - st, fields, SUB, t, pCtx);
                                        } else if (pCtx != null && (idx = pCtx.variableIndexOf(name)) != -1) {
                                            return lastNode = new IndexedOperativeAssign(expr, st, cursor - st, SUB, idx, fields, pCtx);
                                        } else {
                                            return lastNode = new OperativeAssign(name, expr, st, cursor - st, SUB, fields, pCtx);
                                        }
                                }

                                if (isDigit(lookAhead()) && cursor > 1 && (expr[cursor - 1] == 'E' || expr[cursor - 1] == 'e')
                                        && isDigit(expr[cursor - 2])) {
                                    cursor++;
                                    capture = true;
                                    continue Mainloop;
                                }
                                break CaptureLoop;

                            /**
                             * Exit immediately for any of these cases.
                             */
                            case '!':
                            case ',':
                            case '"':
                            case '\'':
                            case ';':
                            case ':':
                                break CaptureLoop;

                            case '\u00AB': // special compact code for recursive parses
                            case '\u00BB':
                            case '\u00AC':
                            case '&':
                            case '^':
                            case '|':
                            case '*':
                            case '/':
                            case '%':
                                char op = expr[cursor];
                                if (lookAhead() == '=') {
                                    name = new String(expr, st, trimLeft(cursor) - st);

                                    st = cursor += 2;
                                    captureToEOS();

                                    if (union) {
                                        return lastNode = new DeepAssignmentNode(expr, st, cursor - st, fields, opLookup(op), t, pCtx);
                                    } else if (pCtx != null && (idx = pCtx.variableIndexOf(name)) != -1) {
                                        return lastNode = new IndexedOperativeAssign(expr, st, cursor - st, opLookup(op), idx, fields,
                                                pCtx);
                                    } else {
                                        return lastNode = new OperativeAssign(name, expr, st, cursor - st, opLookup(op), fields, pCtx);
                                    }
                                }
                                break CaptureLoop;

                            case '<':
                                if ((lookAhead() == '<' && lookAhead(2) == '=')) {
                                    name = new String(expr, st, trimLeft(cursor) - st);

                                    st = cursor += 3;
                                    captureToEOS();

                                    if (union) {
                                        return lastNode = new DeepAssignmentNode(expr, st, cursor - st, fields, BW_SHIFT_LEFT, t, pCtx);
                                    } else if (pCtx != null && (idx = pCtx.variableIndexOf(name)) != -1) {
                                        return lastNode = new IndexedOperativeAssign(expr, st, cursor - st, BW_SHIFT_LEFT, idx, fields,
                                                pCtx);
                                    } else {
                                        return lastNode = new OperativeAssign(name, expr, st, cursor - st, BW_SHIFT_LEFT, fields, pCtx);
                                    }
                                }
                                break CaptureLoop;

                            case '>':
                                if (lookAhead() == '>') {
                                    if (lookAhead(2) == '=') {
                                        name = new String(expr, st, trimLeft(cursor) - st);

                                        st = cursor += 3;
                                        captureToEOS();

                                        if (union) {
                                            return lastNode = new DeepAssignmentNode(expr, st, cursor - st, fields, BW_SHIFT_RIGHT, t,
                                                    pCtx);
                                        } else if (pCtx != null && (idx = pCtx.variableIndexOf(name)) != -1) {
                                            return lastNode = new IndexedOperativeAssign(expr, st, cursor - st, BW_SHIFT_RIGHT, idx, fields,
                                                    pCtx);
                                        } else {
                                            return lastNode = new OperativeAssign(name, expr, st, cursor - st, BW_SHIFT_RIGHT, fields,
                                                    pCtx);
                                        }
                                    } else if ((lookAhead(2) == '>' && lookAhead(3) == '=')) {
                                        name = new String(expr, st, trimLeft(cursor) - st);

                                        st = cursor += 4;
                                        captureToEOS();

                                        if (union) {
                                            return lastNode = new DeepAssignmentNode(expr, st, cursor - st, fields, BW_USHIFT_RIGHT, t,
                                                    pCtx);
                                        } else if (pCtx != null && (idx = pCtx.variableIndexOf(name)) != -1) {
                                            return lastNode = new IndexedOperativeAssign(expr, st, cursor - st, BW_USHIFT_RIGHT, idx,
                                                    fields, pCtx);
                                        } else {
                                            return lastNode = new OperativeAssign(name, expr, st, cursor - st, BW_USHIFT_RIGHT, fields,
                                                    pCtx);
                                        }
                                    }
                                }
                                break CaptureLoop;

                            case '(':
                                cursor = balancedCaptureWithLineAccounting(expr, cursor, end, '(', pCtx) + 1;
                                continue;

                            case '[':
                                cursor = balancedCaptureWithLineAccounting(expr, cursor, end, '[', pCtx) + 1;
                                continue;

                            case '{':
                                if (!union) break CaptureLoop;
                                cursor = balancedCaptureWithLineAccounting(expr, cursor, end, '{', pCtx) + 1;
                                continue;

                            case '~':
                                if (lookAhead() == '=') {
                                    // tmp = subArray(start, trimLeft(cursor));
                                    tmpStart = st;
                                    int tmpOffset = cursor - st;
                                    st = cursor += 2;

                                    captureToEOT();

                                    return lastNode = new RegExMatch(expr, tmpStart, tmpOffset, fields, st, cursor - st, pCtx);
                                }
                                break CaptureLoop;

                            case '=':
                                if (lookAhead() == '+') {
                                    name = new String(expr, st, trimLeft(cursor) - st);

                                    st = cursor += 2;

                                    if (!isNextIdentifierOrLiteral()) {
                                        throw new CompileException("unexpected symbol '" + expr[cursor] + "'", expr, st);
                                    }

                                    captureToEOS();

                                    if (pCtx != null && (idx = pCtx.variableIndexOf(name)) != -1) {
                                        return lastNode = new IndexedOperativeAssign(expr, st, cursor - st, ADD, idx, fields, pCtx);
                                    } else {
                                        return lastNode = new OperativeAssign(name, expr, st, cursor - st, ADD, fields, pCtx);
                                    }
                                } else if (lookAhead() == '-') {
                                    name = new String(expr, st, trimLeft(cursor) - st);

                                    st = cursor += 2;

                                    if (!isNextIdentifierOrLiteral()) {
                                        throw new CompileException("unexpected symbol '" + expr[cursor] + "'", expr, st);
                                    }

                                    captureToEOS();

                                    if (pCtx != null && (idx = pCtx.variableIndexOf(name)) != -1) {
                                        return lastNode = new IndexedOperativeAssign(expr, st, cursor - st, SUB, idx, fields, pCtx);
                                    } else {
                                        return lastNode = new OperativeAssign(name, expr, st, cursor - st, SUB, fields, pCtx);
                                    }
                                }
                                if (greedy && lookAhead() != '=') {
                                    cursor++;

                                    if (union) {
                                        captureToEOS();

                                        return lastNode = new DeepAssignmentNode(expr, st, cursor - st, fields | ASTNode.ASSIGN, pCtx);
                                    } else if (lastWasIdentifier) {
                                        return procTypedNode(false);
                                    } else if (pCtx != null && ((idx = pCtx.variableIndexOf(t)) != -1 && (pCtx.isIndexAllocation()))) {
                                        captureToEOS();

                                        IndexedAssignmentNode ian = new IndexedAssignmentNode(expr, st = trimRight(st),
                                                trimLeft(cursor) - st, ASTNode.ASSIGN, idx, pCtx);

                                        if (idx == -1) {
                                            pCtx.addIndexedInput(t = ian.getVarName());
                                            ian.setRegister(pCtx.variableIndexOf(t));
                                        }
                                        return lastNode = ian;
                                    } else {
                                        captureToEOS();

                                        return lastNode = new AssignmentNode(expr, st, cursor - st, fields | ASTNode.ASSIGN, pCtx);
                                    }
                                }
                                break CaptureLoop;

                            default:
                                if (cursor != end) {
                                    if (isIdentifierPart(expr[cursor])) {
                                        if (!union) {
                                            break CaptureLoop;
                                        }
                                        cursor++;
                                        while (cursor != end && isIdentifierPart(expr[cursor]))
                                            cursor++;
                                    } else if ((cursor + 1) != end && isIdentifierPart(expr[cursor + 1])) {
                                        break CaptureLoop;
                                    } else {
                                        cursor++;
                                    }
                                } else {
                                    break CaptureLoop;
                                }
                        }
                    }

                    /**
                     * Produce the token.
                     */
                    trimWhitespace();

                    return createPropertyToken(st, cursor);
                } else {
                    switch (expr[cursor]) {
                        case '.': {
                            cursor++;
                            if (isDigit(expr[cursor])) {
                                capture = true;
                                continue;
                            }
                            expectNextChar_IW('{');

                            return lastNode = new ThisWithNode(expr, st, cursor - st - 1, cursor + 1,
                                    (cursor = balancedCaptureWithLineAccounting(expr, cursor, end, '{', pCtx) + 1) - 3, fields, pCtx);
                        }

                        case '@': {
                            st++;
                            captureToEOT();

                            if (pCtx == null || (pCtx.getInterceptors() == null
                                    || !pCtx.getInterceptors().containsKey(name = new String(expr, st, cursor - st)))) {
                                throw new CompileException("reference to undefined interceptor: " + new String(expr, st, cursor - st), expr,
                                        st);
                            }

                            return lastNode = new InterceptorWrapper(pCtx.getInterceptors().get(name), nextToken(), pCtx);
                        }

                        case '=':
                            return createOperator(expr, st, (cursor += 2));

                        case '-':
                            if (lookAhead() == '-') {
                                cursor += 2;
                                skipWhitespace();
                                st = cursor;
                                captureIdentifier();

                                name = new String(subArray(st, cursor));
                                if (pCtx != null && (idx = pCtx.variableIndexOf(name)) != -1) {
                                    return lastNode = new IndexedPreFixDecNode(idx, pCtx);
                                } else {
                                    return lastNode = new PreFixDecNode(name, pCtx);
                                }
                            } else if ((cursor == start || (lastNode != null && (lastNode instanceof BooleanNode || lastNode.isOperator())))
                                    && !isDigit(lookAhead())) {

                                cursor += 1;
                                captureToEOT();
                                return new Sign(expr, st, cursor - st, fields, pCtx);
                            } else if ((cursor != start
                                    && (lastNode != null && !(lastNode instanceof BooleanNode || lastNode.isOperator())))
                                    || !isDigit(lookAhead())) {

                                return createOperator(expr, st, cursor++ + 1);
                            } else if ((cursor - 1) != start || (!isDigit(expr[cursor - 1])) && isDigit(lookAhead())) {
                                cursor++;
                                break;
                            } else {
                                throw new CompileException("not a statement", expr, st);
                            }

                        case '+':
                            if (lookAhead() == '+') {
                                cursor += 2;
                                skipWhitespace();
                                st = cursor;
                                captureIdentifier();

                                name = new String(subArray(st, cursor));
                                if (pCtx != null && (idx = pCtx.variableIndexOf(name)) != -1) {
                                    return lastNode = new IndexedPreFixIncNode(idx, pCtx);
                                } else {
                                    return lastNode = new PreFixIncNode(name, pCtx);
                                }
                            }
                            return createOperator(expr, st, cursor++ + 1);

                        case '*':
                            if (lookAhead() == '*') {
                                cursor++;
                            }
                            return createOperator(expr, st, cursor++ + 1);

                        case ';':
                            cursor++;
                            lastWasIdentifier = false;
                            return lastNode = new EndOfStatement(pCtx);

                        case '?':
                            if (cursor == start) {
                                cursor++;
                                continue;
                            }

                        case '#':
                        case '/':
                        case ':':
                        case '^':
                        case '%': {
                            return createOperator(expr, st, cursor++ + 1);
                        }

                        case '(': {
                            cursor++;

                            boolean singleToken = true;

                            skipWhitespace();
                            for (brace = 1; cursor != end && brace != 0; cursor++) {
                                switch (expr[cursor]) {
                                    case '(':
                                        brace++;
                                        break;
                                    case ')':
                                        brace--;
                                        break;
                                    case '\'':
                                        cursor = captureStringLiteral('\'', expr, cursor, end);
                                        break;
                                    case '"':
                                        cursor = captureStringLiteral('"', expr, cursor, end);
                                        break;
                                    case 'i':
                                        if (brace == 1 && isWhitespace(lookBehind()) && lookAhead() == 'n' && isWhitespace(lookAhead(2))) {

                                            for (int level = brace; cursor != end; cursor++) {
                                                switch (expr[cursor]) {
                                                    case '(':
                                                        brace++;
                                                        break;
                                                    case ')':
                                                        if (--brace < level) {
                                                            cursor++;
                                                            if (tokenContinues()) {
                                                                lastNode = new Fold(expr, trimRight(st + 1), cursor - st - 2, fields, pCtx);
                                                                if (expr[st = cursor] == '.') st++;
                                                                captureToEOT();
                                                                return lastNode = new Union(expr, st = trimRight(st), cursor - st, fields,
                                                                        lastNode, pCtx);
                                                            } else {
                                                                return lastNode = new Fold(expr, trimRight(st + 1), cursor - st - 2, fields,
                                                                        pCtx);
                                                            }
                                                        }
                                                        break;
                                                    case '\'':
                                                        cursor = captureStringLiteral('\'', expr, cursor, end);
                                                        break;
                                                    case '"':
                                                        cursor = captureStringLiteral('\"', expr, cursor, end);
                                                        break;
                                                }
                                            }

                                            throw new CompileException("unterminated projection; closing parathesis required", expr, st);
                                        }
                                        break;

                                    default:
                                        /**
                                         * Check to see if we should disqualify this current token as a potential
                                         * type-cast candidate.
                                         */

                                        if (expr[cursor] != '.') {
                                            switch (expr[cursor]) {
                                                case '[':
                                                case ']':
                                                    break;

                                                default:
                                                    if (!(isIdentifierPart(expr[cursor]) || expr[cursor] == '.')) {
                                                        singleToken = false;
                                                    }
                                            }
                                        }
                                }
                            }

                            if (brace != 0) {
                                throw new CompileException("unbalanced braces in expression: (" + brace + "):", expr, st);
                            }

                            tmpStart = -1;
                            if (singleToken) {
                                int _st;
                                TypeDescriptor tDescr = new TypeDescriptor(expr, _st = trimRight(st + 1), trimLeft(cursor - 1) - _st,
                                        fields);

                                Class cls;
                                try {
                                    if (tDescr.isClass() && (cls = getClassReference(pCtx, tDescr)) != null) {

                                        // lookahead to check if it could be a real cast
                                        boolean isCast = false;
                                        for (int i = cursor; i < expr.length; i++) {
                                            if (expr[i] == ' ' || expr[i] == '\t') continue;
                                            isCast = isIdentifierPart(expr[i]) || expr[i] == '\'' || expr[i] == '"' || expr[i] == '(';
                                            break;
                                        }

                                        if (isCast) {
                                            st = cursor;

                                            captureToEOT();
                                            //   captureToEOS();

                                            return lastNode = new TypeCast(expr, st, cursor - st, cls, fields, pCtx);
                                        }
                                    }
                                } catch (ClassNotFoundException e) {
                                    // fallthrough
                                }

                            }

                            if (tmpStart != -1) {
                                return handleUnion(handleSubstatement(new Substatement(expr, tmpStart, cursor - tmpStart, fields, pCtx)));
                            } else {
                                return handleUnion(handleSubstatement(
                                        new Substatement(expr, st = trimRight(st + 1), trimLeft(cursor - 1) - st, fields, pCtx)));
                            }
                        }

                        case '}':
                        case ']':
                        case ')': {
                            throw new CompileException("unbalanced braces", expr, st);
                        }

                        case '>': {
                            switch (expr[cursor + 1]) {
                                case '>':
                                    if (expr[cursor += 2] == '>') cursor++;
                                    return createOperator(expr, st, cursor);
                                case '=':
                                    return createOperator(expr, st, cursor += 2);
                                default:
                                    return createOperator(expr, st, ++cursor);
                            }
                        }

                        case '<': {
                            if (expr[++cursor] == '<') {
                                if (expr[++cursor] == '<') cursor++;
                                return createOperator(expr, st, cursor);
                            } else if (expr[cursor] == '=') {
                                return createOperator(expr, st, ++cursor);
                            } else {
                                return createOperator(expr, st, cursor);
                            }
                        }

                        case '\'':
                        case '"':
                            lastNode = new LiteralNode(
                                    handleStringEscapes(subset(expr, st + 1,
                                            (cursor = captureStringLiteral(expr[cursor], expr, cursor, end)) - st - 1)),
                                    String.class, pCtx);

                            cursor++;

                            if (tokenContinues()) {
                                return lastNode = handleUnion(lastNode);
                            }

                            return lastNode;

                        case '&': {
                            if (expr[cursor++ + 1] == '&') {
                                return createOperator(expr, st, ++cursor);
                            } else {
                                return createOperator(expr, st, cursor);
                            }
                        }

                        case '|': {
                            if (expr[cursor++ + 1] == '|') {
                                return createOperator(expr, st, ++cursor);
                            } else {
                                return createOperator(expr, st, cursor);
                            }
                        }

                        case '~':
                            if ((cursor++ - 1 != 0 || !isIdentifierPart(lookBehind())) && isDigit(expr[cursor])) {
                                st = cursor;
                                captureToEOT();
                                return lastNode = new Invert(expr, st, cursor - st, fields, pCtx);
                            } else if (expr[cursor] == '(') {
                                st = cursor--;
                                captureToEOT();
                                return lastNode = new Invert(expr, st, cursor - st, fields, pCtx);
                            } else {
                                if (expr[cursor] == '=') cursor++;
                                return createOperator(expr, st, cursor);
                            }

                        case '!': {
                            ++cursor;
                            if (isNextIdentifier()) {
                                if (lastNode != null && !lastNode.isOperator()) {
                                    throw new CompileException("unexpected operator '!'", expr, st);
                                }

                                st = cursor;
                                captureToEOT();
                                if ("new".equals(name = new String(expr, st, cursor - st)) || "isdef".equals(name)) {
                                    captureToEOT();
                                    return lastNode = new Negation(expr, st, cursor - st, fields, pCtx);
                                } else {
                                    return lastNode = new Negation(expr, st, cursor - st, fields, pCtx);
                                }
                            } else if (expr[cursor] == '(') {
                                st = cursor--;
                                captureToEOT();
                                return lastNode = new Negation(expr, st, cursor - st, fields, pCtx);
                            } else if (expr[cursor] == '!') {
                                // just ignore a double negation
                                ++cursor;
                                return nextToken();
                            } else if (expr[cursor] != '=') throw new CompileException("unexpected operator '!'", expr, st, null);
                            else {
                                return createOperator(expr, st, ++cursor);
                            }
                        }

                        case '[':
                        case '{':
                            cursor = balancedCaptureWithLineAccounting(expr, cursor, end, expr[cursor], pCtx) + 1;
                            if (tokenContinues()) {
                                lastNode = new InlineCollectionNode(expr, st, cursor - st, fields, pCtx);
                                st = cursor;
                                captureToEOT();
                                if (expr[st] == '.') st++;

                                return lastNode = new Union(expr, st, cursor - st, fields, lastNode, pCtx);
                            } else {
                                return lastNode = new InlineCollectionNode(expr, st, cursor - st, fields, pCtx);
                            }

                        default:
                            cursor++;
                    }
                }
            }

            if (st == cursor) return null;
            else return createPropertyToken(st, cursor);
        } catch (RedundantCodeException e) {
            return nextToken();
        } catch (NumberFormatException e) {
            throw new CompileException("badly formatted number: " + e.getMessage(), expr, st, e);
        } catch (StringIndexOutOfBoundsException e) {
            throw new CompileException("unexpected end of statement", expr, cursor, e);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new CompileException("unexpected end of statement", expr, cursor, e);
        } catch (CompileException e) {
            throw ErrorUtil.rewriteIfNeeded(e, expr, cursor);
        }
    }

    public ASTNode handleSubstatement(Substatement stmt) {
        if (stmt.getStatement() != null && stmt.getStatement().isLiteralOnly()) {
            return new LiteralNode(stmt.getStatement().getValue(null, null, null), pCtx);
        } else {
            return stmt;
        }
    }

    /**
     * Handle a union between a closed statement and a residual property chain.
     *
     * @param node an ast node
     * @return ASTNode
     */
    protected ASTNode handleUnion(ASTNode node) {
        if (cursor != end) {
            skipWhitespace();
            int union = -1;
            if (cursor < end) {
                switch (expr[cursor]) {
                    case '.':
                        union = cursor + 1;
                        break;
                    case '[':
                        union = cursor;
                }
            }

            if (union != -1) {
                captureToEOT();
                return lastNode = new Union(expr, union, cursor - union, fields, node, pCtx);
            }

        }
        return lastNode = node;
    }

    /**
     * Create an operator node.
     *
     * @param expr  an char[] containing the expression
     * @param start the start offet for the token
     * @param end   the end offset for the token
     * @return ASTNode
     */
    private ASTNode createOperator(final char[] expr, final int start, final int end) {
        lastWasIdentifier = false;
        return lastNode = new OperatorNode(OPERATORS.get(new String(expr, start, end - start)), expr, start, pCtx);
    }

    /**
     * Create a copy of an array based on a sub-range.  Works faster than System.arrayCopy() for arrays shorter than
     * 1000 elements in most cases, so the parser uses this internally.
     *
     * @param start the start offset
     * @param end   the end offset
     * @return an array
     */
    private char[] subArray(final int start, final int end) {
        if (start >= end) return new char[0];

        char[] newA = new char[end - start];
        for (int i = 0; i != newA.length; i++) {
            newA[i] = expr[i + start];
        }

        return newA;
    }

    /**
     * Generate a property token
     *
     * @param st  the start offset
     * @param end the end offset
     * @return an ast node
     */
    private ASTNode createPropertyToken(int st, int end) {
        String tmp;

        if (isPropertyOnly(expr, st, end)) {
            if (pCtx != null && pCtx.hasImports()) {
                int find;

                if ((find = findFirst('.', st, end - st, expr)) != -1) {
                    String iStr = new String(expr, st, find - st);
                    if (pCtx.hasImport(iStr)) {
                        lastWasIdentifier = true;
                        return lastNode = new LiteralDeepPropertyNode(expr, find + 1, end - find - 1, fields, pCtx.getImport(iStr), pCtx);
                    }
                } else {
                    if (pCtx.hasImport(tmp = new String(expr, st, cursor - st))) {
                        lastWasIdentifier = true;
                        return lastNode = new LiteralNode(pCtx.getStaticOrClassImport(tmp), pCtx);
                    }
                }
            }

            if (LITERALS.containsKey(tmp = new String(expr, st, end - st))) {
                lastWasIdentifier = true;
                return lastNode = new LiteralNode(LITERALS.get(tmp), pCtx);
            } else if (OPERATORS.containsKey(tmp)) {
                lastWasIdentifier = false;
                return lastNode = new OperatorNode(OPERATORS.get(tmp), expr, st, pCtx);
            } else if (lastWasIdentifier) {
                return procTypedNode(true);
            }
        }

        if (pCtx != null && isArrayType(expr, st, end)) {
            if (pCtx.hasImport(new String(expr, st, cursor - st - 2))) {
                lastWasIdentifier = true;
                TypeDescriptor typeDescriptor = new TypeDescriptor(expr, st, cursor - st, fields);

                try {
                    return lastNode = new LiteralNode(typeDescriptor.getClassReference(pCtx), pCtx);
                } catch (ClassNotFoundException e) {
                    throw new CompileException("could not resolve class: " + typeDescriptor.getClassName(), expr, st);
                }
            }
        }

        lastWasIdentifier = true;

        return lastNode = new ASTNode(expr, trimRight(st), trimLeft(end) - st, fields, pCtx);
    }

    /**
     * Process the current typed node
     *
     * @param decl node is a declaration or not
     * @return and ast node
     */
    private ASTNode procTypedNode(boolean decl) {
        while (true) {
            if (lastNode.getLiteralValue() instanceof String) {
                char[] tmp = ((String) lastNode.getLiteralValue()).toCharArray();
                TypeDescriptor tDescr = new TypeDescriptor(tmp, 0, tmp.length, 0);

                try {
                    lastNode.setLiteralValue(getClassReference(pCtx, tDescr));
                    lastNode.discard();
                } catch (Exception e) {
                    // fall through;
                }
            }

            if (lastNode.isLiteral() && lastNode.getLiteralValue() instanceof Class) {
                lastNode.discard();

                captureToEOS();

                if (decl) {
                    splitAccumulator.add(new DeclTypedVarNode(new String(expr, st, cursor - st), expr, st, cursor - st,
                            (Class) lastNode.getLiteralValue(), fields | ASTNode.ASSIGN, pCtx));
                } else {
                    captureToEOS();
                    splitAccumulator.add(
                            new TypedVarNode(expr, st, cursor - st - 1, fields | ASTNode.ASSIGN, (Class) lastNode.getLiteralValue(), pCtx));
                }
            } else if (lastNode instanceof Proto) {
                captureToEOS();
                if (decl) {
                    splitAccumulator
                            .add(new DeclProtoVarNode(new String(expr, st, cursor - st), (Proto) lastNode, fields | ASTNode.ASSIGN, pCtx));
                } else {
                    splitAccumulator.add(new ProtoVarNode(expr, st, cursor - st, fields | ASTNode.ASSIGN, (Proto) lastNode, pCtx));
                }
            }

            // this redundant looking code is needed to work with the interpreter and MVELSH properly.
            else if ((fields & ASTNode.COMPILE_IMMEDIATE) == 0) {
                if (stk.peek() instanceof Class) {
                    captureToEOS();
                    if (decl) {
                        splitAccumulator.add(new DeclTypedVarNode(new String(expr, st, cursor - st), expr, st, cursor - st,
                                (Class) stk.pop(), fields | ASTNode.ASSIGN, pCtx));
                    } else {
                        splitAccumulator.add(new TypedVarNode(expr, st, cursor - st, fields | ASTNode.ASSIGN, (Class) stk.pop(), pCtx));
                    }
                } else if (stk.peek() instanceof Proto) {
                    captureToEOS();
                    if (decl) {
                        splitAccumulator.add(
                                new DeclProtoVarNode(new String(expr, st, cursor - st), (Proto) stk.pop(), fields | ASTNode.ASSIGN, pCtx));
                    } else {
                        splitAccumulator.add(new ProtoVarNode(expr, st, cursor - st, fields | ASTNode.ASSIGN, (Proto) stk.pop(), pCtx));
                    }
                } else {
                    throw new CompileException("unknown class or illegal statement: " + lastNode.getLiteralValue(), expr, cursor);
                }
            } else {
                throw new CompileException("unknown class or illegal statement: " + lastNode.getLiteralValue(), expr, cursor);
            }

            skipWhitespace();
            if (cursor < end && expr[cursor] == ',') {
                st = ++cursor;
                splitAccumulator.add(new EndOfStatement(pCtx));
            } else {
                return (ASTNode) splitAccumulator.pop();
            }
        }
    }

    /**
     * Generate a code block token.
     *
     * @param condStart  the start offset for the condition
     * @param condEnd    the end offset for the condition
     * @param blockStart the start offset for the block
     * @param blockEnd   the end offset for the block
     * @param type       the type of block
     * @return and ast node
     */
    private ASTNode createBlockToken(final int condStart, final int condEnd, final int blockStart, final int blockEnd, int type) {
        lastWasIdentifier = false;
        cursor++;

        if (isStatementNotManuallyTerminated()) {
            splitAccumulator.add(new EndOfStatement(pCtx));
        }

        int condOffset = condEnd - condStart;
        int blockOffset = blockEnd - blockStart;

        if (blockOffset < 0) blockOffset = 0;

        switch (type) {
            case ASTNode.BLOCK_IF:
                return new IfNode(expr, condStart, condOffset, blockStart, blockOffset, fields, pCtx);
            case ASTNode.BLOCK_FOR:
                for (int i = condStart; i < condEnd; i++) {
                    if (expr[i] == ';') return new ForNode(expr, condStart, condOffset, blockStart, blockOffset, fields, pCtx);
                    else if (expr[i] == ':') break;
                }
            case ASTNode.BLOCK_FOREACH:
                return new ForEachNode(expr, condStart, condOffset, blockStart, blockOffset, fields, pCtx);
            case ASTNode.BLOCK_WHILE:
                return new WhileNode(expr, condStart, condOffset, blockStart, blockOffset, fields, pCtx);
            case ASTNode.BLOCK_UNTIL:
                return new UntilNode(expr, condStart, condOffset, blockStart, blockOffset, fields, pCtx);
            case ASTNode.BLOCK_DO:
                return new DoNode(expr, condStart, condOffset, blockStart, blockOffset, fields, pCtx);
            case ASTNode.BLOCK_DO_UNTIL:
                return new DoUntilNode(expr, condStart, condOffset, blockStart, blockOffset, pCtx);
            default:
                return new WithNode(expr, condStart, condOffset, blockStart, blockOffset, fields, pCtx);
        }
    }

    /**
     * Capture a code block by type.
     *
     * @param type the block type
     * @return an ast node
     */
    private ASTNode captureCodeBlock(int type) {
        boolean cond = true;

        ASTNode first = null;
        ASTNode tk = null;

        switch (type) {
            case ASTNode.BLOCK_IF: {
                do {
                    if (tk != null) {
                        captureToNextTokenJunction();
                        skipWhitespace();
                        cond = expr[cursor] != '{' && expr[cursor] == 'i' && expr[++cursor] == 'f'
                                && expr[cursor = incNextNonBlank()] == '(';
                    }

                    if (((IfNode) (tk = _captureBlock(tk, expr, cond, type))).getElseBlock() != null) {
                        cursor++;
                        return first;
                    }

                    if (first == null) first = tk;

                    if (cursor != end && expr[cursor] != ';') {
                        cursor++;
                    }
                } while (ifThenElseBlockContinues());

                return first;
            }

            case ASTNode.BLOCK_DO:
                skipWhitespace();
                return _captureBlock(null, expr, false, type);

            default: // either BLOCK_WITH or BLOCK_FOREACH
                captureToNextTokenJunction();
                skipWhitespace();
                return _captureBlock(null, expr, true, type);
        }
    }

    private ASTNode _captureBlock(ASTNode node, final char[] expr, boolean cond, int type) {
        skipWhitespace();
        int startCond = 0;
        int endCond = 0;

        int blockStart;
        int blockEnd;

        String name;

        /**
         * Functions are a special case we handle differently from the rest of block parsing
         */
        switch (type) {
            case FUNCTION: {
                int st = cursor;

                captureToNextTokenJunction();

                if (cursor == end) {
                    throw new CompileException("unexpected end of statement", expr, st);
                }

                /**
                 * Check to see if the name is legal.
                 */
                if (isReservedWord(name = createStringTrimmed(expr, st, cursor - st)) || isNotValidNameorLabel(name))
                    throw new CompileException("illegal function name or use of reserved word", expr, cursor);

                FunctionParser parser = new FunctionParser(name, cursor, end - cursor, expr, fields, pCtx, splitAccumulator);
                Function function = parser.parse();
                cursor = parser.getCursor();

                return lastNode = function;
            }
            case PROTO: {
                if (ProtoParser.isUnresolvedWaiting()) {
                    ProtoParser.checkForPossibleUnresolvedViolations(expr, cursor, pCtx);
                }

                int st = cursor;
                captureToNextTokenJunction();

                if (isReservedWord(name = createStringTrimmed(expr, st, cursor - st)) || isNotValidNameorLabel(name))
                    throw new CompileException("illegal prototype name or use of reserved word", expr, cursor);

                if (expr[cursor = nextNonBlank()] != '{') {
                    throw new CompileException("expected '{' but found: " + expr[cursor], expr, cursor);
                }

                cursor = balancedCaptureWithLineAccounting(expr, st = cursor + 1, end, '{', pCtx);

                ProtoParser parser = new ProtoParser(expr, st, cursor, name, pCtx, fields, splitAccumulator);
                Proto proto = parser.parse();

                pCtx.addImport(proto);

                proto.setCursorPosition(st, cursor);
                cursor = parser.getCursor();

                ProtoParser.notifyForLateResolution(proto);

                return lastNode = proto;
            }
            case STACKLANG: {
                if (expr[cursor = nextNonBlank()] != '{') {
                    throw new CompileException("expected '{' but found: " + expr[cursor], expr, cursor);
                }
                int st;
                cursor = balancedCaptureWithLineAccounting(expr, st = cursor + 1, end, '{', pCtx);

                Stacklang stacklang = new Stacklang(expr, st, cursor - st, fields, pCtx);
                cursor++;

                return lastNode = stacklang;

            }
            default:
                if (cond) {
                    if (expr[cursor] != '(') {
                        throw new CompileException("expected '(' but encountered: " + expr[cursor], expr, cursor);
                    }

                    /**
                     * This block is an: IF, FOREACH or WHILE node.
                     */

                    endCond = cursor = balancedCaptureWithLineAccounting(expr, startCond = cursor, end, '(', pCtx);

                    startCond++;
                    cursor++;
                }
        }

        skipWhitespace();

        if (cursor >= end) {
            throw new CompileException("unexpected end of statement", expr, end);
        } else if (expr[cursor] == '{') {
            blockEnd = cursor = balancedCaptureWithLineAccounting(expr, blockStart = cursor, end, '{', pCtx);
        } else {
            blockStart = cursor - 1;
            captureToEOSorEOL();
            blockEnd = cursor + 1;
        }

        if (type == ASTNode.BLOCK_IF) {
            IfNode ifNode = (IfNode) node;

            if (node != null) {
                if (!cond) {
                    return ifNode.setElseBlock(expr, st = trimRight(blockStart + 1), trimLeft(blockEnd) - st, pCtx);
                } else {
                    return ifNode
                            .setElseIf((IfNode) createBlockToken(startCond, endCond, trimRight(blockStart + 1), trimLeft(blockEnd), type));
                }
            } else {
                return createBlockToken(startCond, endCond, blockStart + 1, blockEnd, type);
            }
        } else if (type == ASTNode.BLOCK_DO) {
            cursor++;
            skipWhitespace();
            st = cursor;
            captureToNextTokenJunction();

            if ("while".equals(name = new String(expr, st, cursor - st))) {
                skipWhitespace();
                startCond = cursor + 1;
                endCond = cursor = balancedCaptureWithLineAccounting(expr, cursor, end, '(', pCtx);
                return createBlockToken(startCond, endCond, trimRight(blockStart + 1), trimLeft(blockEnd), type);
            } else if ("until".equals(name)) {
                skipWhitespace();
                startCond = cursor + 1;
                endCond = cursor = balancedCaptureWithLineAccounting(expr, cursor, end, '(', pCtx);
                return createBlockToken(startCond, endCond, trimRight(blockStart + 1), trimLeft(blockEnd), ASTNode.BLOCK_DO_UNTIL);
            } else {
                throw new CompileException("expected 'while' or 'until' but encountered: " + name, expr, cursor);
            }
        }
        // DON"T REMOVE THIS COMMENT!
        // else if (isFlag(ASTNode.BLOCK_FOREACH) || isFlag(ASTNode.BLOCK_WITH)) {
        else {
            return createBlockToken(startCond, endCond, trimRight(blockStart + 1), trimLeft(blockEnd), type);
        }
    }

    /**
     * Checking from the current cursor position, check to see if the if-then-else block continues.
     *
     * @return boolean value
     */
    protected boolean ifThenElseBlockContinues() {
        if ((cursor + 4) < end) {
            if (expr[cursor] != ';') cursor--;
            skipWhitespace();

            return expr[cursor] == 'e' && expr[cursor + 1] == 'l' && expr[cursor + 2] == 's' && expr[cursor + 3] == 'e'
                    && (isWhitespace(expr[cursor + 4]) || expr[cursor + 4] == '{');
        }
        return false;
    }

    /**
     * Checking from the current cursor position, check to see if we're inside a contiguous identifier.
     *
     * @return -
     */
    protected boolean tokenContinues() {
        if (cursor == end) return false;
        else if (expr[cursor] == '.' || expr[cursor] == '[') return true;
        else if (isWhitespace(expr[cursor])) {
            int markCurrent = cursor;
            skipWhitespace();
            if (cursor != end && (expr[cursor] == '.' || expr[cursor] == '[')) return true;
            cursor = markCurrent;
        }
        return false;
    }

    /**
     * The parser should find a statement ending condition when this is called, otherwise everything should blow up.
     */
    protected void expectEOS() {
        skipWhitespace();
        if (cursor != end && expr[cursor] != ';') {
            switch (expr[cursor]) {
                case '&':
                    if (lookAhead() == '&') return;
                    else break;
                case '|':
                    if (lookAhead() == '|') return;
                    else break;
                case '!':
                    if (lookAhead() == '=') return;
                    else break;

                case '<':
                case '>':
                    return;

                case '=': {
                    switch (lookAhead()) {
                        case '=':
                        case '+':
                        case '-':
                        case '*':
                            return;
                    }
                    break;
                }

                case '+':
                case '-':
                case '/':
                case '*':
                    if (lookAhead() == '=') return;
                    else break;
            }

            throw new CompileException("expected end of statement but encountered: " + (cursor == end ? "<end of stream>" : expr[cursor]),
                    expr, cursor);
        }
    }

    /**
     * Checks to see if the next part of the statement is an identifier part.
     *
     * @return boolean true if next part is identifier part.
     */
    protected boolean isNextIdentifier() {
        while (cursor != end && isWhitespace(expr[cursor]))
            cursor++;
        return cursor != end && isIdentifierPart(expr[cursor]);
    }

    /**
     * Capture from the current cursor position, to the end of the statement.
     */
    protected void captureToEOS() {
        while (cursor != end) {
            switch (expr[cursor]) {
                case '(':
                case '[':
                case '{':
                    if ((cursor = balancedCaptureWithLineAccounting(expr, cursor, end, expr[cursor], pCtx)) >= end) return;
                    break;

                case '"':
                case '\'':
                    cursor = captureStringLiteral(expr[cursor], expr, cursor, end);
                    break;

                case ',':
                case ';':
                case '}':
                    return;
            }
            cursor++;
        }
    }

    /**
     * From the current cursor position, capture to the end of statement, or the end of line, whichever comes first.
     */
    protected void captureToEOSorEOL() {
        while (cursor != end && (expr[cursor] != '\n' && expr[cursor] != '\r' && expr[cursor] != ';')) {
            cursor++;
        }
    }

    /**
     * Capture to the end of the current identifier under the cursor.
     */
    protected void captureIdentifier() {
        boolean captured = false;
        if (cursor == end) throw new CompileException("unexpected end of statement: EOF", expr, cursor);
        while (cursor != end) {
            switch (expr[cursor]) {
                case ';':
                    return;

                default: {
                    if (!isIdentifierPart(expr[cursor])) {
                        if (captured) return;
                        throw new CompileException("unexpected symbol (was expecting an identifier): " + expr[cursor], expr, cursor);
                    } else {
                        captured = true;
                    }
                }
            }
            cursor++;
        }
    }

    /**
     * From the current cursor position, capture to the end of the current token.
     */
    protected void captureToEOT() {
        skipWhitespace();
        do {
            switch (expr[cursor]) {
                case '(':
                case '[':
                case '{':
                    if ((cursor = balancedCaptureWithLineAccounting(expr, cursor, end, expr[cursor], pCtx)) == -1) {
                        throw new CompileException("unbalanced braces", expr, cursor);
                    }
                    break;

                case '*':
                case '/':
                case '+':
                case '%':
                case ',':
                case '=':
                case '&':
                case '|':
                case ';':
                    return;

                case '.':
                    skipWhitespace();
                    break;

                case '\'':
                    cursor = captureStringLiteral('\'', expr, cursor, end);
                    break;
                case '"':
                    cursor = captureStringLiteral('"', expr, cursor, end);
                    break;

                default:
                    if (isWhitespace(expr[cursor])) {
                        skipWhitespace();

                        if (cursor < end && expr[cursor] == '.') {
                            if (cursor != end) cursor++;
                            skipWhitespace();
                            break;
                        } else {
                            trimWhitespace();
                            return;
                        }
                    }
            }
        } while (++cursor < end);
    }

    protected boolean lastNonWhite(char c) {
        int i = cursor - 1;
        while (isWhitespace(expr[i]))
            i--;
        return c == expr[i];
    }

    /**
     * From the specified cursor position, trim out any whitespace between the current position and the end of the
     * last non-whitespace character.
     *
     * @param pos - current position
     * @return new position.
     */
    protected int trimLeft(int pos) {
        if (pos > end) pos = end;
        while (pos > 0 && pos >= st && (isWhitespace(expr[pos - 1]) || expr[pos - 1] == ';'))
            pos--;
        return pos;
    }

    /**
     * From the specified cursor position, trim out any whitespace between the current position and beginning of the
     * first non-whitespace character.
     *
     * @param pos -
     * @return -
     */
    protected int trimRight(int pos) {
        while (pos != end && isWhitespace(expr[pos]))
            pos++;
        return pos;
    }

    /**
     * If the cursor is currently pointing to whitespace, move the cursor forward to the first non-whitespace
     * character, but account for carriage returns in the script (updates parser field: line).
     */
    protected void skipWhitespace() {
        Skip: while (cursor != end) {
            switch (expr[cursor]) {
                case '\n':
                    line++;
                    lastLineStart = cursor;
                case '\r':
                    cursor++;
                    continue;
                case '/':
                    if (cursor + 1 != end) {
                        switch (expr[cursor + 1]) {
                            case '/':

                                expr[cursor++] = ' ';
                                while (cursor != end && expr[cursor] != '\n') {
                                    expr[cursor++] = ' ';
                                }
                                if (cursor != end) {
                                    cursor++;
                                }

                                line++;
                                lastLineStart = cursor;

                                continue;

                            case '*':
                                int len = end - 1;
                                int st = cursor;
                                cursor++;

                                while (cursor != len && !(expr[cursor] == '*' && expr[cursor + 1] == '/')) {
                                    cursor++;
                                }
                                if (cursor != len) {
                                    cursor += 2;
                                }

                                for (int i = st; i < cursor; i++) {
                                    expr[i] = ' ';
                                }

                                continue;

                            default:
                                break Skip;

                        }
                    }
                default:
                    if (!isWhitespace(expr[cursor])) break Skip;

            }
            cursor++;
        }
    }

    /**
     * From the current cursor position, capture to the end of the next token junction.
     */
    protected void captureToNextTokenJunction() {
        while (cursor != end) {
            switch (expr[cursor]) {
                case '{':
                case '(':
                    return;
                case '/':
                    if (expr[cursor + 1] == '*') return;
                case '[':
                    cursor = balancedCaptureWithLineAccounting(expr, cursor, end, '[', pCtx) + 1;
                    continue;
                default:
                    if (isWhitespace(expr[cursor])) {
                        return;
                    }
                    cursor++;
            }
        }
    }

    /**
     * From the current cursor position, trim backward over any whitespace to the first non-whitespace character.
     */
    protected void trimWhitespace() {
        while (cursor != 0 && isWhitespace(expr[cursor - 1]))
            cursor--;
    }

    /**
     * Set and finesse the expression, trimming an leading or proceeding whitespace.
     *
     * @param expression the expression
     */
    protected void setExpression(String expression) {
        if (expression != null && expression.length() != 0) {
            synchronized (EX_PRECACHE) {
                if ((this.expr = EX_PRECACHE.get(expression)) == null) {
                    end = length = (this.expr = expression.toCharArray()).length;

                    // trim any whitespace.
                    while (start < length && isWhitespace(expr[start]))
                        start++;

                    while (length != 0 && isWhitespace(this.expr[length - 1]))
                        length--;

                    char[] e = new char[length];

                    for (int i = 0; i != e.length; i++)
                        e[i] = expr[i];

                    EX_PRECACHE.put(expression, e);
                } else {
                    end = length = this.expr.length;
                }
            }
        }
    }

    /**
     * Return the previous non-whitespace character.
     *
     * @return -
     */
    protected char lookToLast() {
        if (cursor == start) return 0;
        int temp = cursor;
        for (;;) {
            if (temp == start || !isWhitespace(expr[--temp])) break;
        }
        return expr[temp];
    }

    /**
     * Return the last character (delta -1 of cursor position).
     *
     * @return -
     */
    protected char lookBehind() {
        if (cursor == start) return 0;
        else return expr[cursor - 1];
    }

    /**
     * Return the next character (delta 1 of cursor position).
     *
     * @return -
     */
    protected char lookAhead() {
        if (cursor + 1 != end) {
            return expr[cursor + 1];
        } else {
            return 0;
        }
    }

    /**
     * Return the character, forward of the currrent cursor position based on the specified range delta.
     *
     * @param range -
     * @return -
     */
    protected char lookAhead(int range) {
        if ((cursor + range) >= end) return 0;
        else {
            return expr[cursor + range];
        }
    }

    /**
     * Returns true if the next is an identifier or literal.
     *
     * @return true of false
     */
    protected boolean isNextIdentifierOrLiteral() {
        int tmp = cursor;
        if (tmp == end) return false;
        else {
            while (tmp != end && isWhitespace(expr[tmp]))
                tmp++;
            if (tmp == end) return false;
            char n = expr[tmp];
            return isIdentifierPart(n) || isDigit(n) || n == '\'' || n == '"';
        }
    }

    /**
     * Increment one cursor position, and move cursor to next non-blank part.
     *
     * @return cursor position
     */
    public int incNextNonBlank() {
        cursor++;
        return nextNonBlank();
    }

    /**
     * Move to next cursor position from current cursor position.
     *
     * @return cursor position
     */
    public int nextNonBlank() {
        if ((cursor + 1) >= end) {
            throw new CompileException("unexpected end of statement", expr, st);
        }
        int i = cursor;
        while (i != end && isWhitespace(expr[i]))
            i++;
        return i;
    }

    /**
     * Expect the next specified character or fail
     *
     * @param c character
     */
    public void expectNextChar_IW(char c) {
        nextNonBlank();
        if (cursor == end) throw new CompileException("unexpected end of statement", expr, st);
        if (expr[cursor] != c) throw new CompileException("unexpected character ('" + expr[cursor] + "'); was expecting: " + c, expr, st);
    }

    /**
     * NOTE: This method assumes that the current position of the cursor is at the end of a logical statement, to
     * begin with.
     * <p/>
     * Determines whether or not the logical statement is manually terminated with a statement separator (';').
     *
     * @return -
     */
    protected boolean isStatementNotManuallyTerminated() {
        if (cursor >= end) return false;
        int c = cursor;
        while (c != end && isWhitespace(expr[c]))
            c++;
        return !(c != end && expr[c] == ';');
    }

    protected void addFatalError(String message) {
        pCtx.addError(new ErrorDetail(expr, st, true, message));
    }

    protected void addFatalError(String message, int start) {
        pCtx.addError(new ErrorDetail(expr, start, true, message));
    }

    /**
     * Reduce the current operations on the stack.
     *
     * @param operator the operator
     * @return a stack control code
     */
    protected int arithmeticFunctionReduction(int operator) {
        ASTNode tk;
        int operator2;

        /**
         * If the next token is an operator, we check to see if it has a higher
         * precdence.
         */
        if ((tk = nextToken()) != null) {
            if (isArithmeticOperator(operator2 = tk.getOperator()) && PTABLE[operator2] > PTABLE[operator]) {
                stk.xswap();
                /**
                 * The current arith. operator is of higher precedence the last.
                 */

                tk = nextToken();

                /**
                 * Check to see if we're compiling or executing interpretively.  If we're compiling, we really
                 * need to stop if this is not a literal.
                 */
                if (compileMode && !tk.isLiteral()) {
                    splitAccumulator.push(tk, new OperatorNode(operator2, expr, st, pCtx));
                    return OP_OVERFLOW;
                }

                dStack.push(operator = operator2, tk.getReducedValue(ctx, ctx, variableFactory));

                while (true) {
                    ASTNode previousToken = tk;
                    // look ahead again
                    if ((tk = nextToken()) != null && (operator2 = tk.getOperator()) != -1 && operator2 != END_OF_STMT
                            && PTABLE[operator2] > PTABLE[operator]) {
                        // if we have back to back operations on the stack, we don't xswap

                        if (dStack.isReduceable()) {
                            stk.copyx2(dStack);
                        }

                        /**
                         * This operator is of higher precedence, or the same level precedence.  push to the RHS.
                         */
                        ASTNode nextToken = nextToken();
                        if (compileMode && !nextToken.isLiteral()) {
                            splitAccumulator.push(nextToken, new OperatorNode(operator2, expr, st, pCtx));
                            return OP_OVERFLOW;
                        }
                        dStack.push(operator = operator2, nextToken.getReducedValue(ctx, ctx, variableFactory));

                        continue;
                    } else if (tk != null && operator2 != -1 && operator2 != END_OF_STMT) {
                        if (PTABLE[operator2] == PTABLE[operator]) {
                            if (!dStack.isEmpty()) dreduce();
                            else {
                                while (stk.isReduceable()) {
                                    stk.xswap_op();
                                }
                            }

                            /**
                             * This operator is of the same level precedence.  push to the RHS.
                             */

                            dStack.push(operator = operator2, nextToken().getReducedValue(ctx, ctx, variableFactory));

                            continue;
                        } else {
                            /**
                             * The operator doesn't have higher precedence. Therfore reduce the LHS.
                             */
                            while (dStack.size() > 1) {
                                dreduce();
                            }

                            operator = tk.getOperator();
                            // Reduce the lesser or equal precedence operations.
                            while (stk.size() != 1 && stk.peek2() instanceof Integer
                                    && ((operator2 = (Integer) stk.peek2()) < PTABLE.length) && PTABLE[operator2] >= PTABLE[operator]) {
                                stk.xswap_op();
                            }
                        }
                    } else {
                        /**
                         * There are no more tokens.
                         */

                        if (dStack.size() > 1) {
                            dreduce();
                        }

                        if (stk.isReduceable()) stk.xswap();

                        break;
                    }

                    if ((tk = nextToken()) != null) {
                        switch (operator) {
                            case AND: {
                                if (!(stk.peekBoolean())) return OP_TERMINATE;
                                else {
                                    splitAccumulator.add(tk);
                                    return AND;
                                }
                            }
                            case OR: {
                                if ((stk.peekBoolean())) return OP_TERMINATE;
                                else {
                                    splitAccumulator.add(tk);
                                    return OR;
                                }
                            }

                            default:
                                if (compileMode && !tk.isLiteral()) {
                                    stk.push(operator, tk);
                                    return OP_NOT_LITERAL;
                                }
                                stk.push(operator, tk.getReducedValue(ctx, ctx, variableFactory));
                        }
                    }
                }
            } else if (!tk.isOperator()) {
                throw new CompileException("unexpected token: " + tk.getName(), expr, st);
            } else {
                reduce();
                splitAccumulator.push(tk);
            }
        }

        // while any values remain on the stack
        // keep XSWAPing and reducing, until there is nothing left.
        if (stk.isReduceable()) {
            while (true) {
                reduce();
                if (stk.isReduceable()) {
                    stk.xswap();
                } else {
                    break;
                }
            }
        }

        return OP_RESET_FRAME;
    }

    private void dreduce() {
        stk.copy2(dStack);
        stk.op();
    }

    /**
     * This method is called when we reach the point where we must subEval a trinary operation in the expression.
     * (ie. val1 op val2).  This is not the same as a binary operation, although binary operations would appear
     * to have 3 structures as well.  A binary structure (or also a junction in the expression) compares the
     * current state against 2 downrange structures (usually an op and a val).
     */
    protected void reduce() {
        Object v1, v2;
        int operator;
        try {
            switch (operator = (Integer) stk.pop()) {
                case ADD:
                case SUB:
                case DIV:
                case MULT:
                case MOD:
                case EQUAL:
                case NEQUAL:
                case GTHAN:
                case LTHAN:
                case GETHAN:
                case LETHAN:
                case POWER:
                    stk.op(operator);
                    break;

                case AND:
                    v1 = stk.pop();
                    stk.push(((Boolean) stk.pop()) && ((Boolean) v1));
                    break;

                case OR:
                    v1 = stk.pop();
                    stk.push(((Boolean) stk.pop()) || ((Boolean) v1));
                    break;

                case CHOR:
                    v1 = stk.pop();
                    if (!isEmpty(v2 = stk.pop()) || !isEmpty(v1)) {
                        stk.clear();
                        stk.push(!isEmpty(v2) ? v2 : v1);
                        return;
                    } else stk.push(null);
                    break;

                case REGEX:
                    stk.push(java.util.regex.Pattern.compile(String.valueOf(stk.pop()))
                            .matcher(String.valueOf(stk.pop())).matches());
                    break;

                case INSTANCEOF:
                    stk.push(((Class) stk.pop()).isInstance(stk.pop()));
                    break;

                case CONVERTABLE_TO:
                    stk.push(org.mvel2.DataConversion.canConvert(stk.peek2().getClass(), (Class) stk.pop2()));
                    break;

                case CONTAINS:
                    stk.push(containsCheck(stk.peek2(), stk.pop2()));
                    break;

                case SOUNDEX:
                    stk.push(soundex(String.valueOf(stk.pop())).equals(soundex(String.valueOf(stk.pop()))));
                    break;

                case SIMILARITY:
                    stk.push(similarity(String.valueOf(stk.pop()), String.valueOf(stk.pop())));
                    break;

                default:
                    reduceNumeric(operator);
            }
        } catch (ClassCastException e) {
            throw new CompileException("syntax error or incompatable types", expr, st, e);
        } catch (ArithmeticException e) {
            throw new CompileException("arithmetic error: " + e.getMessage(), expr, st, e);
        } catch (Exception e) {
            throw new CompileException("failed to subEval expression", expr, st, e);
        }
    }

    private void reduceNumeric(int operator) {
        Object op1 = stk.peek2();
        Object op2 = stk.pop2();
        if (op1 instanceof Integer) {
            if (op2 instanceof Integer) {
                reduce((Integer) op1, operator, (Integer) op2);
            } else {
                reduce((Integer) op1, operator, (Long) op2);
            }
        } else {
            if (op2 instanceof Integer) {
                reduce((Long) op1, operator, (Integer) op2);
            } else {
                reduce((Long) op1, operator, (Long) op2);
            }
        }
    }

    private void reduce(int op1, int operator, int op2) {
        switch (operator) {
            case BW_AND:
                stk.push(op1 & op2);
                break;

            case BW_OR:
                stk.push(op1 | op2);
                break;

            case BW_XOR:
                stk.push(op1 ^ op2);
                break;

            case BW_SHIFT_LEFT:
                stk.push(op1 << op2);
                break;

            case BW_USHIFT_LEFT:
                int iv2 = op1;
                if (iv2 < 0) iv2 *= -1;
                stk.push(iv2 << op2);
                break;

            case BW_SHIFT_RIGHT:
                stk.push(op1 >> op2);
                break;

            case BW_USHIFT_RIGHT:
                stk.push(op1 >>> op2);
                break;
        }
    }

    private void reduce(int op1, int operator, long op2) {
        switch (operator) {
            case BW_AND:
                stk.push(op1 & op2);
                break;

            case BW_OR:
                stk.push(op1 | op2);
                break;

            case BW_XOR:
                stk.push(op1 ^ op2);
                break;

            case BW_SHIFT_LEFT:
                stk.push(op1 << op2);
                break;

            case BW_USHIFT_LEFT:
                int iv2 = op1;
                if (iv2 < 0) iv2 *= -1;
                stk.push(iv2 << op2);
                break;

            case BW_SHIFT_RIGHT:
                stk.push(op1 >> op2);
                break;

            case BW_USHIFT_RIGHT:
                stk.push(op1 >>> op2);
                break;
        }
    }

    private void reduce(long op1, int operator, int op2) {
        switch (operator) {
            case BW_AND:
                stk.push(op1 & op2);
                break;

            case BW_OR:
                stk.push(op1 | op2);
                break;

            case BW_XOR:
                stk.push(op1 ^ op2);
                break;

            case BW_SHIFT_LEFT:
                stk.push(op1 << op2);
                break;

            case BW_USHIFT_LEFT:
                long iv2 = op1;
                if (iv2 < 0) iv2 *= -1;
                stk.push(iv2 << op2);
                break;

            case BW_SHIFT_RIGHT:
                stk.push(op1 >> op2);
                break;

            case BW_USHIFT_RIGHT:
                stk.push(op1 >>> op2);
                break;
        }
    }

    private void reduce(long op1, int operator, long op2) {
        switch (operator) {
            case BW_AND:
                stk.push(op1 & op2);
                break;

            case BW_OR:
                stk.push(op1 | op2);
                break;

            case BW_XOR:
                stk.push(op1 ^ op2);
                break;

            case BW_SHIFT_LEFT:
                stk.push(op1 << op2);
                break;

            case BW_USHIFT_LEFT:
                long iv2 = op1;
                if (iv2 < 0) iv2 *= -1;
                stk.push(iv2 << op2);
                break;

            case BW_SHIFT_RIGHT:
                stk.push(op1 >> op2);
                break;

            case BW_USHIFT_RIGHT:
                stk.push(op1 >>> op2);
                break;
        }
    }

    public int getCursor() {
        return cursor;
    }

    public char[] getExpression() {
        return expr;
    }

    /**
     * Set and finesse the expression, trimming an leading or proceeding whitespace.
     *
     * @param expression the expression
     */
    protected void setExpression(char[] expression) {
        end = length = (this.expr = expression).length;
        while (start < length && isWhitespace(expr[start]))
            start++;
        while (length != 0 && isWhitespace(this.expr[length - 1]))
            length--;
    }
}
