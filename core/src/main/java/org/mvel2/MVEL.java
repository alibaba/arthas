/**
 * MVEL 2.0
 * Copyright (C) 2007  MVFLEX/Valhalla Project and the Codehaus
 * Mike Brock, Dhanji Prasanna, John Graham, Mark Proctor
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mvel2;

import static java.lang.Boolean.getBoolean;
import static java.lang.String.valueOf;
import static org.mvel2.DataConversion.convert;
import static org.mvel2.MVELRuntime.execute;
import static org.mvel2.util.ParseTools.loadFromFile;
import static org.mvel2.util.ParseTools.optimizeTree;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.mvel2.compiler.CompiledAccExpression;
import org.mvel2.compiler.CompiledExpression;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.compiler.ExpressionCompiler;
import org.mvel2.integration.Interceptor;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.CachedMapVariableResolverFactory;
import org.mvel2.integration.impl.CachingMapVariableResolverFactory;
import org.mvel2.integration.impl.ClassImportResolverFactory;
import org.mvel2.integration.impl.ImmutableDefaultFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.mvel2.optimizers.impl.refl.nodes.GetterAccessor;

/**
 * The MVEL convienence class is a collection of static methods that provides a set of easy integration points for
 * MVEL.  The vast majority of MVEL's core functionality can be directly accessed through methods in this class.
 */
public class MVEL {

    public static final String NAME = "MVEL (MVFLEX Expression Language)";
    public static final String VERSION = "2.3";
    public static final String VERSION_SUB = "0";
    public static final String CODENAME = "liberty";
    public static boolean INVOKED_METHOD_EXCEPTIONS_BUBBLE = getBoolean("mvel2.invoked_meth_exceptions_bubble");
    public static boolean COMPILER_OPT_ALLOW_NAKED_METH_CALL = getBoolean("mvel2.compiler.allow_naked_meth_calls");
    public static boolean COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = getBoolean("mvel2.compiler.allow_override_all_prophandling");
    public static boolean COMPILER_OPT_ALLOW_RESOLVE_INNERCLASSES_WITH_DOTNOTATION = getBoolean(
            "mvel2.compiler.allow_resolve_inner_classes_with_dotnotation");
    public static boolean COMPILER_OPT_SUPPORT_JAVA_STYLE_CLASS_LITERALS = getBoolean("mvel2.compiler.support_java_style_class_literals");
    public static boolean COMPILER_OPT_ALLOCATE_TYPE_LITERALS_TO_SHARED_SYMBOL_TABLE = getBoolean(
            "mvel2.compiler.allocate_type_literals_to_shared_symbol_table");
    static boolean DEBUG_FILE = getBoolean("mvel2.debug.fileoutput");
    static String ADVANCED_DEBUGGING_FILE = System.getProperty("mvel2.debugging.file") == null ? "mvel_debug.txt" : System
            .getProperty("mvel2.debugging.file");
    static boolean ADVANCED_DEBUG = getBoolean("mvel2.advanced_debugging");
    static boolean WEAK_CACHE = getBoolean("mvel2.weak_caching");
    static boolean NO_JIT = getBoolean("mvel2.disable.jit");
    static boolean OPTIMIZER = true;

    static {
        if (System.getProperty("mvel2.optimizer") != null) {
            OPTIMIZER = getBoolean("mvel2.optimizer");
        }
    }

    private MVEL() {
    }

    public static boolean isAdvancedDebugging() {
        return ADVANCED_DEBUG;
    }

    public static String getDebuggingOutputFileName() {
        return ADVANCED_DEBUGGING_FILE;
    }

    public static boolean isFileDebugging() {
        return DEBUG_FILE;
    }

    /**
     * Evaluate an expression and return the value.
     *
     * @param expression A String containing the expression to be evaluated.
     * @return the resultant value
     */
    public static Object eval(String expression) {
        return new MVELInterpretedRuntime(expression, new ImmutableDefaultFactory()).parse();
    }

    /**
     * Evaluate an expression against a context object.  Expressions evaluated against a context object are designed
     * to treat members of that context object as variables in the expression.  For example:
     * <pre><code>
     * MVEL.eval("foo == 1", ctx);
     * </code></pre>
     * In this case, the identifier <tt>foo</tt> would be resolved against the <tt>ctx</tt> object.  So it would have
     * the equivalent of: <tt>ctc.getFoo() == 1</tt> in Java.
     *
     * @param expression A String containing the expression to be evaluated.
     * @param ctx        The context object to evaluate against.
     * @return The resultant value
     */
    public static Object eval(String expression, Object ctx) {
        return new MVELInterpretedRuntime(expression, ctx, new ImmutableDefaultFactory()).parse();
    }

    /**
     * Evaluate an expression with externally injected variables via a {@link VariableResolverFactory}.  A factory
     * provides the means by which MVEL can resolve external variables.  MVEL contains a straight-forward implementation
     * for wrapping Maps: {@link MapVariableResolverFactory}, which is used implicitly when calling overloaded methods
     * in this class that use Maps.
     * <p/>
     * An example:
     * <pre><code>
     * Map varsMap = new HashMap();
     * varsMap.put("x", 5);
     * varsMap.put("y", 2);
     * <p/>
     * VariableResolverFactory factory = new MapVariableResolverFactory(varsMap);
     * <p/>
     * Integer i = (Integer) MVEL.eval("x * y", factory);
     * <p/>
     * assert i == 10;
     * </code></pre>
     *
     * @param expression      A String containing the expression to be evaluated.
     * @param resolverFactory The instance of the VariableResolverFactory to be used.
     * @return The resultant value.
     */
    public static Object eval(String expression, VariableResolverFactory resolverFactory) {
        return new MVELInterpretedRuntime(expression, resolverFactory).parse();
    }

    /**
     * Evaluates an expression against a context object and injected variables from a {@link VariableResolverFactory}.
     * This method of execution will prefer to find variables from the factory and <em>then</em> from the context.
     *
     * @param expression      A string containing the expression to be evaluated
     * @param ctx             The context object to evaluate against.
     * @param resolverFactory The instance of the VariableResolverFactory to be used.
     * @return The resultant value
     * @see #eval(String, org.mvel2.integration.VariableResolverFactory)
     */
    public static Object eval(String expression, Object ctx, VariableResolverFactory resolverFactory) {
        return new MVELInterpretedRuntime(expression, ctx, resolverFactory).parse();
    }

    /**
     * Evaluates an expression against externally injected variables.  This is a wrapper convenience method which
     * wraps the provided Map of vars in a {@link MapVariableResolverFactory}
     *
     * @param expression A string containing the expression to be evaluated.
     * @param vars       A map of vars to be injected
     * @return The resultant value
     * @see #eval(String, org.mvel2.integration.VariableResolverFactory)
     */
    public static Object eval(String expression, Map<String, Object> vars) {
        CachingMapVariableResolverFactory factory = new CachingMapVariableResolverFactory(vars);
        try {
            return new MVELInterpretedRuntime(expression, null, factory).parse();
        } finally {
            factory.externalize();
        }
    }

    /**
     * Evaluates an expression against a context object and externally injected variables.  This is a wrapper
     * convenience method which wraps the provided Map of vars in a {@link MapVariableResolverFactory}
     *
     * @param expression A string containing the expression to be evaluated.
     * @param ctx        The context object to evaluate against.
     * @param vars       A map of vars to be injected
     * @return The resultant value
     * @see #eval(String, VariableResolverFactory)
     */
    public static Object eval(String expression, Object ctx, Map<String, Object> vars) {
        CachingMapVariableResolverFactory factory = new CachingMapVariableResolverFactory(vars);
        try {
            return new MVELInterpretedRuntime(expression, ctx, factory).parse();
        } finally {
            factory.externalize();
        }
    }

    /**
     * Evaluates an expression and, if necessary, coerces the resultant value to the specified type. Example:
     * <pre><code>
     * Float output = MVEL.eval("5 + 5", Float.class);
     * </code></pre>
     * <p/>
     * This converts an expression that would otherwise return an <tt>Integer</tt> to a <tt>Float</tt>.
     *
     * @param expression A string containing the expression to be evaluated.
     * @param toType     The target type that the resultant value will be converted to, if necessary.
     * @return The resultant value.
     */
    public static <T> T eval(String expression, Class<T> toType) {
        return convert(new MVELInterpretedRuntime(expression).parse(), toType);
    }

    /**
     * Evaluates an expression against a context object and, if necessary, coerces the resultant value to the specified
     * type.
     *
     * @param expression A string containing the expression to be evaluated.
     * @param ctx        The context object to evaluate against.
     * @param toType     The target type that the resultant value will be converted to, if necessary.
     * @return The resultant value
     * @see #eval(String, Class)
     */
    public static <T> T eval(String expression, Object ctx, Class<T> toType) {
        return convert(new MVELInterpretedRuntime(expression, ctx).parse(), toType);
    }

    /**
     * Evaluates an expression against externally injected variables and, if necessary, coerces the resultant value
     * to the specified type.
     *
     * @param expression A string containing the expression to be evaluated
     * @param vars       The variables to be injected
     * @param toType     The target type that the resultant value will be converted to, if necessary.
     * @return The resultant value
     * @see #eval(String, VariableResolverFactory)
     * @see #eval(String, Class)
     */
    public static <T> T eval(String expression, VariableResolverFactory vars, Class<T> toType) {
        return convert(new MVELInterpretedRuntime(expression, null, vars).parse(), toType);
    }

    /**
     * Evaluates an expression against externally injected variables.  The resultant value is coerced to the specified
     * type if necessary. This is a wrapper convenience method which wraps the provided Map of vars in a{@link MapVariableResolverFactory}
     *
     * @param expression A string containing the expression to be evaluated.
     * @param vars       A map of vars to be injected
     * @param toType     The target type the resultant value will be converted to, if necessary.
     * @return The resultant value
     * @see #eval(String, org.mvel2.integration.VariableResolverFactory)
     */
    public static <T> T eval(String expression, Map<String, Object> vars, Class<T> toType) {
        CachingMapVariableResolverFactory factory = new CachingMapVariableResolverFactory(vars);
        try {
            return convert(new MVELInterpretedRuntime(expression, null, factory).parse(), toType);
        } finally {
            factory.externalize();
        }
    }

    /**
     * Evaluates an expression against a context object and externally injected variables.  If necessary, the resultant
     * value is coerced to the specified type.
     *
     * @param expression A string containing the expression to be evaluated.
     * @param ctx        The context object to evaluate against
     * @param vars       The vars to be injected
     * @param toType     The target type that the resultant value will be converted to, if necessary.
     * @return The resultant value.
     * @see #eval(String, Object, VariableResolverFactory)
     * @see #eval(String, Class)
     */
    public static <T> T eval(String expression, Object ctx, VariableResolverFactory vars, Class<T> toType) {
        return convert(new MVELInterpretedRuntime(expression, ctx, vars).parse(), toType);
    }

    /**
     * Evaluates an expression against a context object and externally injected variables.  If necessary, the resultant
     * value is coerced to the specified type.
     *
     * @param expression A string containing the expression to be evaluated.
     * @param ctx        The context object to evaluate against
     * @param vars       A Map of variables to be injected.
     * @param toType     The target type that the resultant value will be converted to, if necessary.
     * @return The resultant value.
     * @see #eval(String, Object, VariableResolverFactory)
     * @see #eval(String, Class)
     */
    public static <T> T eval(String expression, Object ctx, Map<String, Object> vars, Class<T> toType) {
        CachingMapVariableResolverFactory factory = new CachingMapVariableResolverFactory(vars);
        try {
            return convert(new MVELInterpretedRuntime(expression, ctx, factory).parse(), toType);
        } finally {
            factory.externalize();
        }
    }

    /**
     * Evaluates an expression and returns the resultant value as a String.
     *
     * @param expression A string containing the expressino to be evaluated.
     * @return The resultant value
     */
    public static String evalToString(String expression) {
        return valueOf(eval(expression));
    }

    /**
     * Evaluates an expression and returns the resultant value as a String.
     *
     * @param expression A string containing the expressino to be evaluated.
     * @param ctx        The context object to evaluate against
     * @return The resultant value
     * @see #eval(String, Object)
     */
    public static String evalToString(String expression, Object ctx) {
        return valueOf(eval(expression, ctx));
    }

    /**
     * Evaluates an expression and returns the resultant value as a String.
     *
     * @param expression A string containing the expressino to be evaluated.
     * @param vars       The variables to be injected
     * @return The resultant value
     * @see #eval(String, VariableResolverFactory)
     */
    public static String evalToString(String expression, VariableResolverFactory vars) {
        return valueOf(eval(expression, vars));
    }

    /**
     * Evaluates an expression and returns the resultant value as a String.
     *
     * @param expression A string containing the expressino to be evaluated.
     * @param vars       A Map of variables to be injected
     * @return The resultant value
     * @see #eval(String, Map)
     */
    public static String evalToString(String expression, Map vars) {
        return valueOf(eval(expression, vars));
    }

    /**
     * Evaluates an expression and returns the resultant value as a String.
     *
     * @param expression A string containing the expressino to be evaluated.
     * @param ctx        The context object to evaluate against.
     * @param vars       The variables to be injected
     * @return The resultant value
     * @see #eval(String, Map)
     */
    public static String evalToString(String expression, Object ctx, VariableResolverFactory vars) {
        return valueOf(eval(expression, ctx, vars));
    }

    /**
     * Evaluates an expression and returns the resultant value as a String.
     *
     * @param expression A string containing the expressino to be evaluated.
     * @param ctx        The context object to evaluate against.
     * @param vars       A Map of variables to be injected
     * @return The resultant value
     * @see #eval(String, Map)
     */
    public static String evalToString(String expression, Object ctx, Map vars) {
        return valueOf(eval(expression, ctx, vars));
    }

    /**
     * Evaluate an expression and return the value.
     *
     * @param expression A char[] containing the expression to be evaluated.
     * @return The resultant value
     * @see #eval(String)
     */
    public static Object eval(char[] expression) {
        return new MVELInterpretedRuntime(expression, new ImmutableDefaultFactory()).parse();
    }

    /**
     * Evaluate an expression against a context object and return the value
     *
     * @param expression A char[] containing the expression to be evaluated.
     * @param ctx        The context object to evaluate against
     * @return The resultant value
     * @see #eval(String, Object)
     */
    public static Object eval(char[] expression, Object ctx) {
        return new MVELInterpretedRuntime(expression, ctx).parse();
    }

    public static <T> T eval(char[] expression, Class<T> type) {
        return convert(new MVELInterpretedRuntime(expression).parse(), type);
    }

    /**
     * Evaluate an expression against a context object and return the value
     *
     * @param expression A char[] containing the expression to be evaluated.
     * @param ctx        The context object to evaluate against
     * @param vars       The variables to be injected
     * @return The resultant value
     * @see #eval(String, Object, VariableResolverFactory)
     */
    public static Object eval(char[] expression, Object ctx, VariableResolverFactory vars) {
        return new MVELInterpretedRuntime(expression, ctx, vars).parse();
    }

    public static Object eval(char[] expression, int start, int offset, Object ctx, VariableResolverFactory vars) {
        return new MVELInterpretedRuntime(expression, start, offset, ctx, vars).parse();
    }

    public static <T> T eval(char[] expression, int start, int offset, Object ctx, VariableResolverFactory vars, Class<T> toType) {
        return convert(new MVELInterpretedRuntime(expression, start, offset, ctx, vars).parse(), toType);
    }

    /**
     * Evaluate an expression against a context object and return the value
     *
     * @param expression A char[] containing the expression to be evaluated.
     * @param ctx        The context object to evaluate against
     * @param vars       A Map of variables to be injected
     * @return The resultant value
     * @see #eval(String, Object, Map)
     */
    public static Object eval(char[] expression, Object ctx, Map vars) {
        return new MVELInterpretedRuntime(expression, ctx, vars).parse();
    }

    /**
     * Evaluate an expression with a context object and injected variables and return the value. If necessary convert
     * the resultant value to the specified type.
     *
     * @param expression A char[] containing the expression to be evaluated.
     * @param ctx        The context object to evaluate against
     * @param vars       A Map of variables to be injected
     * @param toType     The target type the resultant value will be converted to, if necessary.
     * @return The resultant value
     * @see #eval(String, Object, Map, Class)
     */
    public static <T> T eval(char[] expression, Object ctx, Map<String, Object> vars, Class<T> toType) {
        return convert(new MVELInterpretedRuntime(expression, ctx, vars).parse(), toType);
    }

    /**
     * Evaluate an expression with a context object and return the value. If necessary convert
     * the resultant value to the specified type.
     *
     * @param expression A char[] containing the expression to be evaluated.
     * @param ctx        The context object to evaluate against
     * @param toType     The target type the resultant value will be converted to, if necessary.
     * @return The resultant value
     * @see #eval(String, Object, Class)
     */
    public static <T> T eval(char[] expression, Object ctx, Class<T> toType) {
        return convert(new MVELInterpretedRuntime(expression, ctx).parse(), toType);
    }

    /**
     * Evaluate an expression with a context object and injected variables and return the value. If necessary convert
     * the resultant value to the specified type.
     *
     * @param expression A char[] containing the expression to be evaluated.
     * @param ctx        The context object to evaluate against
     * @param vars       The variables to be injected
     * @param toType     The target type the resultant value will be converted to, if necessary.
     * @return The resultant value
     * @see #eval(String, Object, VariableResolverFactory, Class)
     */
    public static <T> T eval(char[] expression, Object ctx, VariableResolverFactory vars, Class<T> toType) {
        return convert(new MVELInterpretedRuntime(expression, ctx, vars).parse(), toType);
    }

    /**
     * Evaluate an expression with injected variables and return the value. If necessary convert
     * the resultant value to the specified type.
     *
     * @param expression A char[] containing the expression to be evaluated.
     * @param vars       The variables to be injected
     * @param toType     The target type the resultant value will be converted to, if necessary.
     * @return The resultant value
     * @see #eval(String, VariableResolverFactory, Class)
     */
    public static <T> T eval(char[] expression, VariableResolverFactory vars, Class<T> toType) {
        return convert(new MVELInterpretedRuntime(expression, null, vars).parse(), toType);
    }

    /**
     * Evaluate an expression with injected variables and return the resultant value. If necessary convert
     * the resultant value to the specified type.
     *
     * @param expression A char[] containing the expression to be evaluated.
     * @param vars       The variables to be injected
     * @param toType     The target type the resultant value will be converted to, if necessary.
     * @return The resultant value
     * @see #eval(String, Map, Class)
     */
    public static <T> T eval(char[] expression, Map<String, Object> vars, Class<T> toType) {
        return convert(new MVELInterpretedRuntime(expression, null, vars).parse(), toType);
    }

    /**
     * Evaluate a script from a file and return the resultant value.
     *
     * @param file The file to process
     * @return The resultant value
     * @throws IOException Exception thrown if there is an IO problem accessing the file.
     */
    public static Object evalFile(File file) throws IOException {
        return _evalFile(file, null, new CachedMapVariableResolverFactory(new HashMap()));
    }

    public static Object evalFile(File file, String encoding) throws IOException {
        return _evalFile(file, encoding, null, new CachedMapVariableResolverFactory(new HashMap()));
    }

    /**
     * Evaluate a script from a file, against a context object and return the resultant value.
     *
     * @param file The file to process
     * @param ctx  The context to evaluate the script against.
     * @return The resultant value
     * @throws IOException Exception thrown if there is an IO problem accessing the file.
     */
    public static Object evalFile(File file, Object ctx) throws IOException {
        return _evalFile(file, ctx, new CachedMapVariableResolverFactory(new HashMap()));
    }

    public static Object evalFile(File file, String encoding, Object ctx) throws IOException {
        return _evalFile(file, encoding, ctx, new CachedMapVariableResolverFactory(new HashMap()));
    }

    /**
     * Evaluate a script from a file with injected variables and return the resultant value.
     *
     * @param file The file to process
     * @param vars Variables to be injected
     * @return The resultant value
     * @throws IOException Exception thrown if there is an IO problem accessing the file.
     */
    public static Object evalFile(File file, Map<String, Object> vars) throws IOException {
        CachingMapVariableResolverFactory factory = new CachingMapVariableResolverFactory(vars);
        try {
            return _evalFile(file, null, factory);
        } finally {
            factory.externalize();
        }
    }

    /**
     * Evaluate a script from a file with injected variables and a context object, then return the resultant value.
     *
     * @param file The file to process
     * @param ctx  The context to evaluate the script against.
     * @param vars Variables to be injected
     * @return The resultant value
     * @throws IOException Exception thrown if there is an IO problem accessing the file.
     */
    public static Object evalFile(File file, Object ctx, Map<String, Object> vars) throws IOException {
        CachingMapVariableResolverFactory factory = new CachingMapVariableResolverFactory(vars);
        try {
            return _evalFile(file, ctx, factory);
        } finally {
            factory.externalize();
        }
    }

    public static Object evalFile(File file, String encoding, Object ctx, Map<String, Object> vars) throws IOException {
        CachingMapVariableResolverFactory factory = new CachingMapVariableResolverFactory(vars);
        try {
            return _evalFile(file, encoding, ctx, factory);
        } finally {
            factory.externalize();
        }
    }

    /**
     * Evaluate a script from a file with injected variables and a context object, then return the resultant value.
     *
     * @param file The file to process
     * @param ctx  The context to evaluate the script against.
     * @param vars Variables to be injected
     * @return The resultant value
     * @throws IOException Exception thrown if there is an IO problem accessing the file.
     */
    public static Object evalFile(File file, Object ctx, VariableResolverFactory vars) throws IOException {
        return _evalFile(file, ctx, vars);
    }

    public static Object evalFile(File file, String encoding, Object ctx, VariableResolverFactory vars) throws IOException {
        return _evalFile(file, encoding, ctx, vars);
    }

    private static Object _evalFile(File file, Object ctx, VariableResolverFactory factory) throws IOException {
        return _evalFile(file, null, ctx, factory);
    }

    private static Object _evalFile(File file, String encoding, Object ctx, VariableResolverFactory factory) throws IOException {
        return eval(loadFromFile(file, encoding), ctx, factory);
    }

    /**
     * Evaluate an expression in Boolean-only mode against a root context object and injected variables.
     *
     * @param expression A string containing the expression to be evaluated.
     * @param ctx        The context against which to evaluate the expression
     * @param vars       The variables to be injected
     * @return The resultant value as a Boolean
     */
    public static Boolean evalToBoolean(String expression, Object ctx, Map<String, Object> vars) {
        return eval(expression, ctx, vars, Boolean.class);
    }

    /**
     * Evaluate an expression in Boolean-only mode against a root context object.
     *
     * @param expression A string containing the expression to be evaluated.
     * @param ctx        The context against which to evaluate the expression
     * @return The resultant value as a Boolean
     */
    public static Boolean evalToBoolean(String expression, Object ctx) {
        return eval(expression, ctx, new ImmutableDefaultFactory(), Boolean.class);
    }

    /**
     * Evaluate an expression in Boolean-only mode against a root context object and injected variables.
     *
     * @param expression A string containing the expression to be evaluated.
     * @param ctx        The context against which to evaluate the expression
     * @param vars       The variables to be injected
     * @return The resultant value as a Boolean
     */
    public static Boolean evalToBoolean(String expression, Object ctx, VariableResolverFactory vars) {
        return eval(expression, ctx, vars, Boolean.class);
    }

    /**
     * Evaluate an expression in Boolean-only with injected variables.
     *
     * @param expression A string containing the expression to be evaluated.
     * @param vars       The variables to be injected
     * @return The resultant value as a Boolean
     */
    public static Boolean evalToBoolean(String expression, VariableResolverFactory vars) {
        return eval(expression, vars, Boolean.class);
    }

    /**
     * Evaluate an expression in Boolean-only with injected variables.
     *
     * @param expression A string containing the expression to be evaluated.
     * @param vars       The variables to be injected
     * @return The resultant value as a Boolean
     */
    public static Boolean evalToBoolean(String expression, Map<String, Object> vars) {
        return evalToBoolean(expression, null, vars);
    }

    /**
     * Performs an analysis compileShared, which will populate the ParserContext with type, input and variable information,
     * but will not produce a payload.
     *
     * @param expression - the expression to analyze
     * @param ctx        - the parser context
     */
    public static void analysisCompile(char[] expression, ParserContext ctx) {
        ExpressionCompiler compiler = new ExpressionCompiler(expression, ctx);
        compiler.setVerifyOnly(true);
        compiler.compile();
    }

    public static void analysisCompile(String expression, ParserContext ctx) {
        analysisCompile(expression.toCharArray(), ctx);
    }

    public static Class analyze(char[] expression, ParserContext ctx) {
        ExpressionCompiler compiler = new ExpressionCompiler(expression, ctx);
        compiler.setVerifyOnly(true);
        compiler.compile();
        return compiler.getReturnType();
    }

    public static Class analyze(String expression, ParserContext ctx) {
        return analyze(expression.toCharArray(), ctx);
    }

    /**
     * Compiles an expression and returns a Serializable object containing the compiled expression.  The returned value
     * can be reused for higher-performance evaluation of the expression.  It is used in a straight forward way:
     * <pre><code>
     * <p/>
     * // Compile the expression
     * Serializable compiled = MVEL.compileExpression("x * 10");
     * <p/>
     * // Create a Map to hold the variables.
     * Map vars = new HashMap();
     * <p/>
     * // Create a factory to envelop the variable map
     * VariableResolverFactory factory = new MapVariableResolverFactory(vars);
     * <p/>
     * int total = 0;
     * for (int i = 0; i < 100; i++) {
     * // Update the 'x' variable.
     * vars.put("x", i);
     * <p/>
     * // Execute the expression against the compiled payload and factory, and add the result to the total variable.
     * total += (Integer) MVEL.executeExpression(compiled, factory);
     * }
     * <p/>
     * // Total should be 49500
     * assert total == 49500;
     * </code></pre>
     * <p/>
     * The above example demonstrates a compiled expression being reused ina tight, closed, loop.  Doing this greatly
     * improves performance as re-parsing of the expression is not required, and the runtime can dynamically compileShared
     * the expression to bytecode of necessary.
     *
     * @param expression A String contaiing the expression to be compiled.
     * @return The cacheable compiled payload.
     */
    public static Serializable compileExpression(String expression) {
        return compileExpression(expression, null, null, null);
    }

    /**
     * Compiles an expression and returns a Serializable object containing the compiled expression.  This method
     * also accept a Map of imports.  The Map's keys are String's representing the imported, short-form name of the
     * Classes or Methods imported.  An import of a Method is essentially a static import.  This is a substitute for
     * needing to declare <tt>import</tt> statements within the actual script.
     * <p/>
     * <pre><code>
     * Map imports = new HashMap();
     * imports.put("HashMap", java.util.HashMap.class); // import a class
     * imports.put("time", MVEL.getStaticMethod(System.class, "currentTimeMillis", new Class[0])); // import a static method
     * <p/>
     * // Compile the expression
     * Serializable compiled = MVEL.compileExpression("map = new HashMap(); map.put('time', time()); map.time");
     * <p/>
     * // Execute with a blank Map to allow vars to be declared.
     * Long val = (Long) MVEL.executeExpression(compiled, new HashMap());
     * <p/>
     * assert val > 0;
     * </code></pre>
     *
     * @param expression A String contaiing the expression to be compiled.
     * @param imports    A String-Class/String-Method pair Map containing imports for the compiler.
     * @return The cacheable compiled payload.
     */
    public static Serializable compileExpression(String expression, Map<String, Object> imports) {
        return compileExpression(expression, imports, null, null);
    }

    /**
     * Compiles an expression and returns a Serializable object containing the compiled expression. This method
     * accepts a Map of imports and Interceptors.  See {@link #compileExpression(String, Map)} for information on
     * imports.  The imports parameter in this method is <em>optional</em> and it is safe to pass a <tt>null</tt>
     * value.<br/>{@link org.mvel2.integration.Interceptor Interceptors} are markers within an expression that allow external hooks
     * to be tied into the expression.
     * <p/>
     * <pre><code>
     * // Create a Map to hold the interceptors.
     * Map interceptors = new HashMap();
     * <p/>
     * // Create a simple interceptor.
     * Interceptor logInterceptor = new Interceptor() {
     * public int doBefore(ASTNode node, VariableResolverFactory factory) {
     * System.out.println("Interceptor called before!");
     * }
     * <p/>
     * public int doAfter(Object exitValue, ASTNode node, VariableResolverFactory factory) {
     * System.out.println("Interceptor called after!");
     * }
     * };
     * <p/>
     * // Add the interceptor to the Map.
     * interceptors.put("log", logInterceptor);
     * <p/>
     * // Create an expression
     * String expr = "list = [1,2,3,4,5]; @log for (item : list) { System.out.println(item); };
     * <p/>
     * Serializable compiled = MVEL.compileExpression(expr, null, interceptors);
     * <p/>
     * // Execute expression with a blank Map to allow vars to be declared.
     * MVEL.executeExpression(compiled, new HashMap());
     * </code></pre>
     * <p/>
     * The above example demonstrates inserting an interceptor into a piece of code.  The <tt>@log</tt> interceptor
     * wraps the subsequent statement.  In this case, the interceptor is fired before the <tt>for</tt> loop and
     * after the <tt>for</tt> loop finishes.
     *
     * @param expression   A String containing the expression to be evaluated.
     * @param imports      A String-Class/String-Method pair Map containing imports for the compiler.
     * @param interceptors A Map of registered interceptors.
     * @return A cacheable compiled payload.
     */
    public static Serializable compileExpression(String expression, Map<String, Object> imports, Map<String, Interceptor> interceptors) {
        return compileExpression(expression, imports, interceptors, null);
    }

    /**
     * Compiles an expression, and accepts a {@link ParserContext} instance.  The ParserContext object is the
     * fine-grained configuration object for the MVEL parser and compiler.
     *
     * @param expression A string containing the expression to be compiled.
     * @param ctx        The parser context
     * @return A cacheable compiled payload.
     */
    public static Serializable compileExpression(String expression, ParserContext ctx) {
        return optimizeTree(new ExpressionCompiler(expression, ctx).compile());
    }

    public static Serializable compileExpression(char[] expression, int start, int offset, ParserContext ctx) {
        ExpressionCompiler c = new ExpressionCompiler(expression, start, offset, ctx);
        return optimizeTree(c._compile());
    }

    public static Serializable compileExpression(String expression, Map<String, Object> imports, Map<String, Interceptor> interceptors,
            String sourceName) {
        return compileExpression(expression, new ParserContext(imports, interceptors, sourceName));
    }

    public static Serializable compileExpression(char[] expression, ParserContext ctx) {
        return optimizeTree(new ExpressionCompiler(expression, ctx).compile());
    }

    /**
     * Compiles an expression and returns a Serializable object containing the compiled
     * expression.
     *
     * @param expression   The expression to be compiled
     * @param imports      Imported classes
     * @param interceptors Map of named interceptos
     * @param sourceName   The name of the source file being evaluated (optional)
     * @return The cacheable compiled payload
     */
    public static Serializable compileExpression(char[] expression, Map<String, Object> imports, Map<String, Interceptor> interceptors,
            String sourceName) {
        return compileExpression(expression, new ParserContext(imports, interceptors, sourceName));
    }

    public static Serializable compileExpression(char[] expression) {
        return compileExpression(expression, null, null, null);
    }

    public static Serializable compileExpression(char[] expression, Map<String, Object> imports) {
        return compileExpression(expression, imports, null, null);
    }

    public static Serializable compileExpression(char[] expression, Map<String, Object> imports, Map<String, Interceptor> interceptors) {
        return compileExpression(expression, imports, interceptors, null);
    }

    public static Serializable compileGetExpression(String expression) {
        return new CompiledAccExpression(expression.toCharArray(), Object.class, new ParserContext());
    }

    public static Serializable compileGetExpression(String expression, ParserContext ctx) {
        return new CompiledAccExpression(expression.toCharArray(), Object.class, ctx);
    }

    public static Serializable compileGetExpression(char[] expression) {
        return new CompiledAccExpression(expression, Object.class, new ParserContext());
    }

    public static Serializable compileGetExpression(char[] expression, ParserContext ctx) {
        return new CompiledAccExpression(expression, Object.class, ctx);
    }

    public static Serializable compileSetExpression(String expression) {
        return new CompiledAccExpression(expression.toCharArray(), Object.class, new ParserContext());
    }

    public static Serializable compileSetExpression(String expression, ParserContext ctx) {
        return new CompiledAccExpression(expression.toCharArray(), Object.class, ctx);
    }

    public static Serializable compileSetExpression(String expression, Class ingressType, ParserContext ctx) {
        return new CompiledAccExpression(expression.toCharArray(), ingressType, ctx);
    }

    public static Serializable compileSetExpression(char[] expression) {
        return new CompiledAccExpression(expression, Object.class, new ParserContext());
    }

    public static Serializable compileSetExpression(char[] expression, ParserContext ctx) {
        return new CompiledAccExpression(expression, Object.class, ctx);
    }

    public static Serializable compileSetExpression(char[] expression, int start, int offset, ParserContext ctx) {
        return new CompiledAccExpression(expression, start, offset, Object.class, ctx);
    }

    public static Serializable compileSetExpression(char[] expression, Class ingressType, ParserContext ctx) {
        return new CompiledAccExpression(expression, ingressType, ctx);
    }

    public static void executeSetExpression(Serializable compiledSet, Object ctx, Object value) {
        ((CompiledAccExpression) compiledSet).setValue(ctx, ctx, new ImmutableDefaultFactory(), value);
    }

    public static void executeSetExpression(Serializable compiledSet, Object ctx, VariableResolverFactory vrf, Object value) {
        ((CompiledAccExpression) compiledSet).setValue(ctx, ctx, vrf, value);
    }

    public static Object executeExpression(Object compiledExpression) {
        return ((ExecutableStatement) compiledExpression).getValue(null, new ImmutableDefaultFactory());
    }

    /**
     * Executes a compiled expression.
     *
     * @param compiledExpression -
     * @param ctx                -
     * @param vars               -
     * @return -
     * @see #compileExpression(String)
     */
    @SuppressWarnings({ "unchecked" })
    public static Object executeExpression(final Object compiledExpression, final Object ctx, final Map vars) {
        CachingMapVariableResolverFactory factory = vars != null ? new CachingMapVariableResolverFactory(vars) : null;
        try {
            return ((ExecutableStatement) compiledExpression).getValue(ctx, factory);
        } finally {
            if (factory != null) factory.externalize();
        }
    }

    public static Object executeExpression(final Object compiledExpression, final Object ctx,
            final VariableResolverFactory resolverFactory) {
        return ((ExecutableStatement) compiledExpression).getValue(ctx, resolverFactory);
    }

    /**
     * Executes a compiled expression.
     *
     * @param compiledExpression -
     * @param factory            -
     * @return -
     * @see #compileExpression(String)
     */
    public static Object executeExpression(final Object compiledExpression, final VariableResolverFactory factory) {
        return ((ExecutableStatement) compiledExpression).getValue(null, factory);
    }

    /**
     * Executes a compiled expression.
     *
     * @param compiledExpression -
     * @param ctx                -
     * @return -
     * @see #compileExpression(String)
     */
    public static Object executeExpression(final Object compiledExpression, final Object ctx) {
        return ((ExecutableStatement) compiledExpression).getValue(ctx, new ImmutableDefaultFactory());
    }

    /**
     * Executes a compiled expression.
     *
     * @param compiledExpression -
     * @param vars               -
     * @return -
     * @see #compileExpression(String)
     */
    @SuppressWarnings({ "unchecked" })
    public static Object executeExpression(final Object compiledExpression, final Map vars) {
        CachingMapVariableResolverFactory factory = new CachingMapVariableResolverFactory(vars);
        try {
            return ((ExecutableStatement) compiledExpression).getValue(null, factory);
        } finally {
            factory.externalize();
        }
    }

    /**
     * Execute a compiled expression and convert the result to a type
     *
     * @param compiledExpression -
     * @param ctx                -
     * @param vars               -
     * @param toType             -
     * @return -
     */
    @SuppressWarnings({ "unchecked" })
    public static <T> T executeExpression(final Object compiledExpression, final Object ctx, final Map vars, Class<T> toType) {
        return convert(executeExpression(compiledExpression, ctx, vars), toType);
    }

    public static <T> T executeExpression(final Object compiledExpression, final Object ctx, final VariableResolverFactory vars,
            Class<T> toType) {
        return convert(executeExpression(compiledExpression, ctx, vars), toType);
    }

    /**
     * Execute a compiled expression and convert the result to a type
     *
     * @param compiledExpression -
     * @param vars               -
     * @param toType             -
     * @return -
     */
    @SuppressWarnings({ "unchecked" })
    public static <T> T executeExpression(final Object compiledExpression, Map vars, Class<T> toType) {
        return convert(executeExpression(compiledExpression, vars), toType);
    }

    /**
     * Execute a compiled expression and convert the result to a type.
     *
     * @param compiledExpression -
     * @param ctx                -
     * @param toType             -
     * @return -
     */
    public static <T> T executeExpression(final Object compiledExpression, final Object ctx, Class<T> toType) {
        return convert(executeExpression(compiledExpression, ctx), toType);
    }

    public static void executeExpression(Iterable<CompiledExpression> compiledExpression) {
        for (CompiledExpression ce : compiledExpression) {
            ce.getValue(null, null);
        }
    }

    public static void executeExpression(Iterable<CompiledExpression> compiledExpression, Object ctx) {
        for (CompiledExpression ce : compiledExpression) {
            ce.getValue(ctx, null);
        }
    }

    public static void executeExpression(Iterable<CompiledExpression> compiledExpression, Map vars) {
        CachingMapVariableResolverFactory factory = new CachingMapVariableResolverFactory(vars);
        executeExpression(compiledExpression, null, factory);
        factory.externalize();
    }

    public static void executeExpression(Iterable<CompiledExpression> compiledExpression, Object ctx, Map vars) {
        CachingMapVariableResolverFactory factory = new CachingMapVariableResolverFactory(vars);
        executeExpression(compiledExpression, ctx, factory);
        factory.externalize();
    }

    public static void executeExpression(Iterable<CompiledExpression> compiledExpression, Object ctx, VariableResolverFactory vars) {
        for (CompiledExpression ce : compiledExpression) {
            ce.getValue(ctx, vars);
        }
    }

    public static Object[] executeAllExpression(Serializable[] compiledExpressions, Object ctx, VariableResolverFactory vars) {
        if (compiledExpressions == null) return GetterAccessor.EMPTY;
        Object[] o = new Object[compiledExpressions.length];
        for (int i = 0; i < compiledExpressions.length; i++) {
            o[i] = executeExpression(compiledExpressions[i], ctx, vars);
        }
        return o;
    }

    public static Object executeDebugger(CompiledExpression expression, Object ctx, VariableResolverFactory vars) {
        if (expression.isImportInjectionRequired()) {
            return execute(true, expression, ctx, new ClassImportResolverFactory(expression.getParserConfiguration(), vars, false));
        } else {
            return execute(true, expression, ctx, vars);
        }
    }

    public static String parseMacros(String input, Map<String, Macro> macros) {
        return new MacroProcessor(macros).parse(input);
    }

    public static String preprocess(char[] input, PreProcessor[] preprocessors) {
        char[] ex = input;
        for (PreProcessor proc : preprocessors) {
            ex = proc.parse(ex);
        }
        return new String(ex);
    }

    public static String preprocess(String input, PreProcessor[] preprocessors) {
        return preprocess(input.toCharArray(), preprocessors);
    }

    public static Object getProperty(String property, Object ctx) {
        return PropertyAccessor.get(property, ctx);
    }

    public static void setProperty(Object ctx, String property, Object value) {
        PropertyAccessor.set(ctx, property, value);
    }

    /**
     * A simple utility method to get a static method from a class with no checked exception.  With throw a
     * RuntimeException if the method is not found or is not a static method.
     *
     * @param cls        The class containing the static method
     * @param methodName The method name
     * @param signature  The signature of the method
     * @return An instance of the Method
     */
    public static Method getStaticMethod(Class cls, String methodName, Class[] signature) {
        try {
            Method m = cls.getMethod(methodName, signature);
            if ((m.getModifiers() & Modifier.STATIC) == 0) throw new RuntimeException("method not a static method: " + methodName);
            return m;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("no such method: " + methodName);
        }
    }
}
