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

import static java.lang.Character.isJavaIdentifierPart;
import static java.lang.Thread.currentThread;
import static java.lang.reflect.Array.getLength;
import static org.mvel2.DataConversion.canConvert;
import static org.mvel2.DataConversion.convert;
import static org.mvel2.MVEL.eval;
import static org.mvel2.ast.TypeDescriptor.getClassReference;
import static org.mvel2.compiler.AbstractParser.LITERALS;
import static org.mvel2.integration.GlobalListenerFactory.notifySetListeners;
import static org.mvel2.integration.PropertyHandlerFactory.getNullMethodHandler;
import static org.mvel2.integration.PropertyHandlerFactory.getNullPropertyHandler;
import static org.mvel2.integration.PropertyHandlerFactory.getPropertyHandler;
import static org.mvel2.integration.PropertyHandlerFactory.hasNullMethodHandler;
import static org.mvel2.integration.PropertyHandlerFactory.hasNullPropertyHandler;
import static org.mvel2.integration.PropertyHandlerFactory.hasPropertyHandler;
import static org.mvel2.util.ParseTools.EMPTY_OBJ_ARR;
import static org.mvel2.util.ParseTools.balancedCapture;
import static org.mvel2.util.ParseTools.balancedCaptureWithLineAccounting;
import static org.mvel2.util.ParseTools.captureStringLiteral;
import static org.mvel2.util.ParseTools.findAbsoluteLast;
import static org.mvel2.util.ParseTools.findClass;
import static org.mvel2.util.ParseTools.getBaseComponentType;
import static org.mvel2.util.ParseTools.getBestCandidate;
import static org.mvel2.util.ParseTools.getWidenedTarget;
import static org.mvel2.util.ParseTools.isWhitespace;
import static org.mvel2.util.ParseTools.parseParameterList;
import static org.mvel2.util.ParseTools.parseWithExpressions;
import static org.mvel2.util.PropertyTools.getFieldOrAccessor;
import static org.mvel2.util.PropertyTools.getFieldOrWriteAccessor;
import static org.mvel2.util.ReflectionUtil.toNonPrimitiveType;
import static org.mvel2.util.Varargs.normalizeArgsForVarArgs;
import static org.mvel2.util.Varargs.paramTypeVarArgsSafe;

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.mvel2.ast.FunctionInstance;
import org.mvel2.ast.InvokationContextFactory;
import org.mvel2.ast.Proto;
import org.mvel2.ast.PrototypalFunctionInstance;
import org.mvel2.ast.TypeDescriptor;
import org.mvel2.integration.GlobalListenerFactory;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.ImmutableDefaultFactory;
import org.mvel2.util.ErrorUtil;
import org.mvel2.util.MethodStub;
import org.mvel2.util.ParseTools;
import org.mvel2.util.StringAppender;

@SuppressWarnings({ "unchecked" })
/**
 * The property accessor class is used for extracting properties from objects instances.
 */
public class PropertyAccessor {

    //  private static final int DONE = -1;
    private static final int NORM = 0;
    private static final int METH = 1;
    private static final int COL = 2;
    private static final int WITH = 3;
    private static final Object[] EMPTYARG = new Object[0];
    private static final Map<Class, WeakHashMap<Integer, WeakReference<Member>>> READ_PROPERTY_RESOLVER_CACHE;
    private static final Map<Class, WeakHashMap<Integer, WeakReference<Member>>> WRITE_PROPERTY_RESOLVER_CACHE;
    private static final Map<Class, WeakHashMap<Integer, WeakReference<Object[]>>> METHOD_RESOLVER_CACHE;
    private static final Map<Member, WeakReference<Class[]>> METHOD_PARMTYPES_CACHE;

    static {
        READ_PROPERTY_RESOLVER_CACHE = Collections.synchronizedMap(new WeakHashMap<Class, WeakHashMap<Integer, WeakReference<Member>>>(10));
        WRITE_PROPERTY_RESOLVER_CACHE = Collections
                .synchronizedMap(new WeakHashMap<Class, WeakHashMap<Integer, WeakReference<Member>>>(10));
        METHOD_RESOLVER_CACHE = Collections.synchronizedMap(new WeakHashMap<Class, WeakHashMap<Integer, WeakReference<Object[]>>>(10));
        METHOD_PARMTYPES_CACHE = Collections.synchronizedMap(new WeakHashMap<Member, WeakReference<Class[]>>(10));
    }

    private int start = 0;
    private int cursor = 0;
    private int st;
    private char[] property;
    private int length;
    private int end;
    private Object thisReference;
    private Object ctx;
    private Object curr;
    private Class currType = null;
    private boolean first = true;
    private boolean nullHandle = false;
    private VariableResolverFactory variableFactory;
    private ParserContext pCtx;

    public PropertyAccessor(String property, Object ctx) {
        this.length = end = (this.property = property.toCharArray()).length;
        this.ctx = ctx;
        this.variableFactory = new ImmutableDefaultFactory();
    }

    public PropertyAccessor(char[] property, Object ctx, VariableResolverFactory resolver, Object thisReference, ParserContext pCtx) {
        this.length = end = (this.property = property).length;
        this.ctx = ctx;
        this.variableFactory = resolver;
        this.thisReference = thisReference;
        this.pCtx = pCtx;
    }

    public PropertyAccessor(char[] property, int start, int offset, Object ctx, VariableResolverFactory resolver, Object thisReference,
            ParserContext pCtx) {
        this.property = property;
        this.cursor = this.st = this.start = start;
        this.length = offset;
        this.end = start + offset;
        this.ctx = ctx;
        this.variableFactory = resolver;
        this.thisReference = thisReference;
        this.pCtx = pCtx;
    }

    public static Object get(String property, Object ctx) {
        return new PropertyAccessor(property, ctx).get();
    }

    public static Object get(char[] property, int offset, int end, Object ctx, VariableResolverFactory resolver, Object thisReferece,
            ParserContext pCtx) {
        return new PropertyAccessor(property, offset, end, ctx, resolver, thisReferece, pCtx).get();
    }

    public static Object get(String property, Object ctx, VariableResolverFactory resolver, Object thisReference, ParserContext pCtx) {
        return new PropertyAccessor(property.toCharArray(), ctx, resolver, thisReference, pCtx).get();
    }

    public static void set(Object ctx, String property, Object value) {
        new PropertyAccessor(property, ctx).set(value);
    }

    public static void set(Object ctx, VariableResolverFactory resolver, String property, Object value, ParserContext pCtx) {
        new PropertyAccessor(property.toCharArray(), ctx, resolver, null, pCtx).set(value);
    }

    public static void clearPropertyResolverCache() {
        READ_PROPERTY_RESOLVER_CACHE.clear();
        WRITE_PROPERTY_RESOLVER_CACHE.clear();
        METHOD_RESOLVER_CACHE.clear();
    }

    public static void reportCacheSizes() {
        System.out.println("read property cache: " + READ_PROPERTY_RESOLVER_CACHE.size());
        for (Class cls : READ_PROPERTY_RESOLVER_CACHE.keySet()) {
            System.out.println(" [" + cls.getName() + "]: " + READ_PROPERTY_RESOLVER_CACHE.get(cls).size() + " entries.");
        }
        System.out.println("write property cache: " + WRITE_PROPERTY_RESOLVER_CACHE.size());
        for (Class cls : WRITE_PROPERTY_RESOLVER_CACHE.keySet()) {
            System.out.println(" [" + cls.getName() + "]: " + WRITE_PROPERTY_RESOLVER_CACHE.get(cls).size() + " entries.");
        }
        System.out.println("method cache: " + METHOD_RESOLVER_CACHE.size());
        for (Class cls : METHOD_RESOLVER_CACHE.keySet()) {
            System.out.println(" [" + cls.getName() + "]: " + METHOD_RESOLVER_CACHE.get(cls).size() + " entries.");
        }
    }

    private static void addReadCache(Class cls, Integer property, Member member) {
        synchronized (READ_PROPERTY_RESOLVER_CACHE) {
            WeakHashMap<Integer, WeakReference<Member>> nestedMap = READ_PROPERTY_RESOLVER_CACHE.get(cls);

            if (nestedMap == null) {
                READ_PROPERTY_RESOLVER_CACHE.put(cls, nestedMap = new WeakHashMap<Integer, WeakReference<Member>>());
            }

            nestedMap.put(property, new WeakReference<Member>(member));
        }
    }

    private static Member checkReadCache(Class cls, Integer property) {
        WeakHashMap<Integer, WeakReference<Member>> map = READ_PROPERTY_RESOLVER_CACHE.get(cls);
        if (map != null) {
            WeakReference<Member> member = map.get(property);
            if (member != null) return member.get();
        }
        return null;
    }

    private static void addWriteCache(Class cls, Integer property, Member member) {
        synchronized (WRITE_PROPERTY_RESOLVER_CACHE) {
            WeakHashMap<Integer, WeakReference<Member>> map = WRITE_PROPERTY_RESOLVER_CACHE.get(cls);
            if (map == null) {
                WRITE_PROPERTY_RESOLVER_CACHE.put(cls, map = new WeakHashMap<Integer, WeakReference<Member>>());
            }
            map.put(property, new WeakReference<Member>(member));
        }
    }

    private static Member checkWriteCache(Class cls, Integer property) {
        Map<Integer, WeakReference<Member>> map = WRITE_PROPERTY_RESOLVER_CACHE.get(cls);
        if (map != null) {
            WeakReference<Member> member = map.get(property);
            if (member != null) return member.get();
        }
        return null;
    }

    public static Class[] checkParmTypesCache(Method member) {
        WeakReference<Class[]> pt = METHOD_PARMTYPES_CACHE.get(member);
        Class[] ret;
        if (pt == null || (ret = pt.get()) == null) {
            //noinspection UnusedAssignment
            METHOD_PARMTYPES_CACHE.put(member, pt = new WeakReference<Class[]>(ret = member.getParameterTypes()));
        }
        return ret;
    }

    private static void addMethodCache(Class cls, Integer property, Method member) {
        synchronized (METHOD_RESOLVER_CACHE) {
            WeakHashMap<Integer, WeakReference<Object[]>> map = METHOD_RESOLVER_CACHE.get(cls);
            if (map == null) {
                METHOD_RESOLVER_CACHE.put(cls, map = new WeakHashMap<Integer, WeakReference<Object[]>>());
            }
            map.put(property, new WeakReference<Object[]>(new Object[] { member, member.getParameterTypes() }));
        }
    }

    private static Object[] checkMethodCache(Class cls, Integer property) {
        Map<Integer, WeakReference<Object[]>> map = METHOD_RESOLVER_CACHE.get(cls);
        if (map != null) {
            WeakReference<Object[]> ref = map.get(property);
            if (ref != null) return ref.get();
        }
        return null;
    }

    private static int createSignature(String name, String args) {
        return name.hashCode() + args.hashCode();
    }

    private Object get() {
        curr = ctx;

        try {
            if (!MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING) {
                return getNormal();
            } else {
                return getAllowOverride();
            }
        } catch (InvocationTargetException e) {
            throw new PropertyAccessException("could not access property", property, cursor, e, pCtx);
        } catch (IllegalAccessException e) {
            throw new PropertyAccessException("could not access property", property, cursor, e, pCtx);
        } catch (IndexOutOfBoundsException e) {
            if (cursor >= length) cursor = length - 1;

            throw new PropertyAccessException(
                    "array or collections index out of bounds in property: " + new String(property, cursor, length), property, cursor, e,
                    pCtx);
        } catch (CompileException e) {
            throw ErrorUtil.rewriteIfNeeded(e, property, st);
        } catch (NullPointerException e) {
            throw new PropertyAccessException("null pointer exception in property: " + new String(property), property, cursor, e, pCtx);
        } catch (Exception e) {
            throw new PropertyAccessException("unknown exception in expression: " + new String(property), property, cursor, e, pCtx);
        }
    }

    private Object getNormal() throws Exception {
        while (cursor < end) {
            switch (nextToken()) {
                case NORM:
                    curr = getBeanProperty(curr, capture());
                    break;
                case METH:
                    curr = getMethod(curr, capture());
                    break;
                case COL:
                    curr = getCollectionProperty(curr, capture());
                    break;
                case WITH:
                    curr = getWithProperty(curr);
                    break;
            }

            if (nullHandle) {
                if (curr == null) {
                    return null;
                } else {
                    nullHandle = false;
                }
            }

            first = false;
        }
        return curr;
    }

    private Object getAllowOverride() throws Exception {
        while (cursor < end) {
            switch (nextToken()) {
                case NORM:
                    if ((curr = getBeanPropertyAO(curr, capture())) == null && hasNullPropertyHandler()) {
                        curr = getNullPropertyHandler().getProperty(capture(), ctx, variableFactory);
                    }
                    break;
                case METH:
                    if ((curr = getMethod(curr, capture())) == null && hasNullMethodHandler()) {
                        curr = getNullMethodHandler().getProperty(capture(), ctx, variableFactory);
                    }
                    break;
                case COL:
                    curr = getCollectionPropertyAO(curr, capture());
                    break;
                case WITH:
                    curr = getWithProperty(curr);
                    break;
            }

            if (nullHandle) {
                if (curr == null) {
                    return null;
                } else {
                    nullHandle = false;
                }
            } else {
                if (curr == null && cursor < end) throw new NullPointerException();
            }

            first = false;
        }
        return curr;
    }

    private void set(Object value) {
        curr = ctx;

        try {
            int oLength = end;

            end = findAbsoluteLast(property);

            if ((curr = get()) == null)
                throw new PropertyAccessException("cannot bind to null context: " + new String(property, cursor, length), property, cursor,
                        pCtx);

            end = oLength;

            if (nextToken() == COL) {
                int _start = ++cursor;

                whiteSpaceSkip();

                if (cursor == length || scanTo(']')) throw new PropertyAccessException("unterminated '['", property, cursor, pCtx);

                String ex = new String(property, _start, cursor - _start);

                if (!MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING) {
                    if (curr instanceof Map) {
                        //noinspection unchecked
                        ((Map) curr).put(eval(ex, this.ctx, this.variableFactory), value);
                    } else if (curr instanceof List) {
                        //noinspection unchecked
                        ((List) curr).set(eval(ex, this.ctx, this.variableFactory, Integer.class), value);
                    } else if (hasPropertyHandler(curr.getClass())) {
                        getPropertyHandler(curr.getClass()).setProperty(ex, ctx, variableFactory, value);
                    } else if (curr.getClass().isArray()) {
                        Array.set(curr, eval(ex, this.ctx, this.variableFactory, Integer.class),
                                convert(value, getBaseComponentType(curr.getClass())));
                    } else {
                        throw new PropertyAccessException("cannot bind to collection property: " + new String(property)
                                + ": not a recognized collection type: " + ctx.getClass(), property, cursor, pCtx);
                    }

                    return;
                } else {
                    notifySetListeners(ctx, ex, variableFactory, value);

                    if (curr instanceof Map) {
                        //noinspection unchecked
                        if (hasPropertyHandler(Map.class)) getPropertyHandler(Map.class).setProperty(ex, curr, variableFactory, value);
                        else((Map) curr).put(eval(ex, this.ctx, this.variableFactory), value);
                    } else if (curr instanceof List) {
                        //noinspection unchecked
                        if (hasPropertyHandler(List.class)) getPropertyHandler(List.class).setProperty(ex, curr, variableFactory, value);
                        else((List) curr).set(eval(ex, this.ctx, this.variableFactory, Integer.class), value);
                    } else if (curr.getClass().isArray()) {
                        if (hasPropertyHandler(Array.class)) getPropertyHandler(Array.class).setProperty(ex, curr, variableFactory, value);
                        else Array.set(curr, eval(ex, this.ctx, this.variableFactory, Integer.class),
                                convert(value, getBaseComponentType(curr.getClass())));
                    } else if (hasPropertyHandler(curr.getClass())) {
                        getPropertyHandler(curr.getClass()).setProperty(ex, curr, variableFactory, value);
                    } else {
                        throw new PropertyAccessException("cannot bind to collection property: " + new String(property)
                                + ": not a recognized collection type: " + ctx.getClass(), property, cursor, pCtx);
                    }

                    return;
                }
            } else if (MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING && hasPropertyHandler(curr.getClass())) {
                getPropertyHandler(curr.getClass()).setProperty(capture(), curr, variableFactory, value);
                return;
            }

            String tk = capture();

            Member member = checkWriteCache(curr.getClass(), tk == null ? 0 : tk.hashCode());
            if (member == null) {
                addWriteCache(curr.getClass(), tk != null ? tk.hashCode() : -1,
                        (member = value != null ? getFieldOrWriteAccessor(curr.getClass(), tk,
                                value.getClass()) : getFieldOrWriteAccessor(curr.getClass(), tk)));
            }

            if (member instanceof Method) {
                Method meth = (Method) member;

                Class[] parameterTypes = checkParmTypesCache(meth);

                if (value != null && !parameterTypes[0].isAssignableFrom(value.getClass())) {
                    if (!canConvert(parameterTypes[0], value.getClass())) {
                        throw new CompileException("cannot convert type: " + value.getClass() + ": to " + meth.getParameterTypes()[0],
                                property, cursor);
                    }
                    meth.invoke(curr, convert(value, parameterTypes[0]));
                } else {
                    meth.invoke(curr, value);
                }
            } else if (member != null) {
                Field fld = (Field) member;

                if (value != null && !fld.getType().isAssignableFrom(value.getClass())) {
                    if (!canConvert(fld.getType(), value.getClass())) {
                        throw new CompileException("cannot convert type: " + value.getClass() + ": to " + fld.getType(), property, cursor);
                    }

                    fld.set(curr, convert(value, fld.getType()));
                } else {
                    fld.set(curr, value);
                }
            } else if (curr instanceof Map) {
                //noinspection unchecked
                ((Map) curr).put(eval(tk, this.ctx, this.variableFactory), value);
            } else if (curr instanceof FunctionInstance) {
                ((PrototypalFunctionInstance) curr).getResolverFactory().getVariableResolver(tk).setValue(value);
            } else {
                throw new PropertyAccessException(
                        "could not access/write property (" + tk + ") in: " + (curr == null ? "Unknown" : curr.getClass().getName()),
                        property, cursor, pCtx);
            }
        } catch (InvocationTargetException e) {
            throw new PropertyAccessException("could not access property", property, st, e, pCtx);
        } catch (IllegalAccessException e) {
            throw new PropertyAccessException("could not access property", property, st, e, pCtx);
        }
    }

    private int nextToken() {
        switch (property[st = cursor]) {
            case '[':
                return COL;
            case '{':
                if (property[cursor - 1] == '.') {
                    return WITH;
                }
                break;
            case '.':
                // ++cursor;
                while (cursor < end && isWhitespace(property[cursor]))
                    cursor++;
                if ((st + 1) != end) {
                    switch (property[cursor = ++st]) {
                        case '?':
                            cursor = ++st;
                            nullHandle = true;
                            break;
                        case '{':
                            return WITH;
                    }

                }
            case '?':
                if (cursor == start) {
                    cursor = ++st;
                    nullHandle = true;
                }
        }

        do {
            while (cursor < end && isWhitespace(property[cursor]))
                cursor++;

            if (cursor < end && property[cursor] == '.') {
                cursor++;
            } else {
                break;
            }
        } while (true);

        st = cursor;

        //noinspection StatementWithEmptyBody
        while (++cursor < end && isJavaIdentifierPart(property[cursor]));

        if (cursor < end) {
            while (isWhitespace(property[cursor]))
                cursor++;
            switch (property[cursor]) {
                case '[':
                    return COL;
                case '(':
                    return METH;
                default:
                    return 0;
            }
        }
        return 0;
    }

    private String capture() {
        return new String(property, st, trimLeft(cursor) - st);
    }

    protected int trimLeft(int pos) {
        while (pos > 0 && isWhitespace(property[pos - 1]))
            pos--;
        return pos;
    }

    private Object getBeanPropertyAO(Object ctx, String property) throws IllegalAccessException, InvocationTargetException {
        if (ctx != null && hasPropertyHandler(ctx.getClass()))
            return getPropertyHandler(ctx.getClass()).getProperty(property, ctx, variableFactory);

        GlobalListenerFactory.notifyGetListeners(ctx, property, variableFactory);

        return getBeanProperty(ctx, property);
    }

    private Object getBeanProperty(Object ctx, String property) throws IllegalAccessException, InvocationTargetException {

        if (first) {
            if ("this".equals(property)) {
                return this.ctx;
            } else if (LITERALS.containsKey(property)) {
                return LITERALS.get(property);
            } else if (variableFactory != null && variableFactory.isResolveable(property)) {
                return variableFactory.getVariableResolver(property).getValue();
            }
        }

        if (ctx != null) {
            Class<?> cls;
            if (ctx instanceof Class) {
                if (MVEL.COMPILER_OPT_SUPPORT_JAVA_STYLE_CLASS_LITERALS && "class".equals(property)) {
                    return ctx;
                }

                cls = (Class<?>) ctx;
            } else {
                cls = ctx.getClass();
            }

            Member member = checkReadCache(cls, property.hashCode());

            if (member == null) {
                addReadCache(cls, property.hashCode(), member = getFieldOrAccessor(cls, property));
            }

            if (member instanceof Method) {
                try {
                    return ((Method) member).invoke(ctx, EMPTYARG);
                } catch (IllegalAccessException e) {
                    synchronized (member) {
                        try {
                            ((Method) member).setAccessible(true);
                            return ((Method) member).invoke(ctx, EMPTYARG);
                        } finally {
                            ((Method) member).setAccessible(false);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    if (member.getDeclaringClass().equals(ctx)) {
                        try {
                            Class c = Class.forName(member.getDeclaringClass().getName() + "$" + property);

                            throw new CompileException("name collision between innerclass: " + c.getCanonicalName()
                                    + "; and bean accessor: " + property + " (" + member.toString() + ")", this.property, this.st);
                        } catch (ClassNotFoundException e2) {
                            //fallthru
                        }
                    }
                    throw e;
                }
            } else if (member != null) {
                currType = toNonPrimitiveType(((Field) member).getType());
                ((Field) member).setAccessible(true);
                return ((Field) member).get(ctx);
            } else if (ctx instanceof Map && (((Map) ctx).containsKey(property) || nullHandle)) {
                if (ctx instanceof Proto.ProtoInstance) {
                    return ((Proto.ProtoInstance) ctx).get(property).call(null, thisReference, variableFactory, EMPTY_OBJ_ARR);
                }
                return ((Map) ctx).get(property);
            } else if ("length".equals(property) && ctx.getClass().isArray()) {
                return getLength(ctx);
            } else if (ctx instanceof Class) {
                Class c = (Class) ctx;
                for (Method m : c.getMethods()) {
                    if (property.equals(m.getName())) {
                        if (pCtx != null && pCtx.getParserConfiguration() != null ? pCtx.getParserConfiguration()
                                .isAllowNakedMethCall() : MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL) {
                            m.setAccessible(true);
                            return m.invoke(ctx, EMPTY_OBJ_ARR);
                        }
                        return m;
                    }
                }

                try {
                    return findClass(variableFactory, c.getName() + "$" + property, pCtx);
                } catch (ClassNotFoundException cnfe) {
                    // fall through.
                }
            } else if (hasPropertyHandler(cls)) {
                return getPropertyHandler(cls).getProperty(property, ctx, variableFactory);
            } else if (ctx instanceof FunctionInstance) {
                return ((PrototypalFunctionInstance) ctx).getResolverFactory().getVariableResolver(property).getValue();
            }
        }

        Object tryStatic = tryStaticAccess();

        if (tryStatic != null) {
            if (tryStatic instanceof Class || tryStatic instanceof Method) return tryStatic;
            else {
                ((Field) tryStatic).setAccessible(true);
                return ((Field) tryStatic).get(null);
            }
        } else if (pCtx != null && pCtx.getParserConfiguration() != null ? pCtx.getParserConfiguration()
                .isAllowNakedMethCall() : MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL) {
            return getMethod(ctx, property);
        }

        if (ctx == null) {
            throw new PropertyAccessException("unresolvable property or identifier: " + property, this.property, st, pCtx);
        } else {
            throw new PropertyAccessException("could not access: " + property + "; in class: " + ctx.getClass().getName(), this.property,
                    st, pCtx);
        }
    }

    private void whiteSpaceSkip() {
        if (cursor < end)
            //noinspection StatementWithEmptyBody
            while (isWhitespace(property[cursor]) && ++cursor < end);
    }

    /**
     * @param c - character to scan to.
     * @return - returns true is end of statement is hit, false if the scan scar is countered.
     */
    private boolean scanTo(char c) {
        for (; cursor < end; cursor++) {
            switch (property[cursor]) {
                case '\'':
                case '"':
                    cursor = captureStringLiteral(property[cursor], property, cursor, end);
                default:
                    if (property[cursor] == c) {
                        return false;
                    }
            }

        }
        return true;
    }

    private Object getWithProperty(Object ctx) {
        int st;

        String nestParm = start == cursor ? null : new String(property, start, cursor - start - 1).trim();

        parseWithExpressions(nestParm, property, st = cursor + 1,
                (cursor = balancedCaptureWithLineAccounting(property, cursor, end, '{', pCtx)) - st, ctx, variableFactory);
        cursor++;
        return ctx;
    }

    /**
     * Handle accessing a property embedded in a collections, map, or array
     *
     * @param ctx  -
     * @param prop -
     * @return -
     * @throws Exception -
     */
    private Object getCollectionProperty(Object ctx, String prop) throws Exception {
        if (prop.length() != 0) {
            ctx = getBeanProperty(ctx, prop);
            if (ctx == null) {
                throw new NullPointerException("null pointer on indexed access for: " + prop);
            }
        }

        currType = null;

        int _start = ++cursor;

        whiteSpaceSkip();

        if (cursor == end || scanTo(']')) throw new PropertyAccessException("unterminated '['", property, cursor, pCtx);

        prop = new String(property, _start, cursor++ - _start);

        if (ctx instanceof Map) {
            return ((Map) ctx).get(eval(prop, ctx, variableFactory));
        } else if (ctx instanceof List) {
            return ((List) ctx).get((Integer) eval(prop, ctx, variableFactory));
        } else if (ctx instanceof Collection) {
            int count = (Integer) eval(prop, ctx, variableFactory);
            if (count > ((Collection) ctx).size())
                throw new PropertyAccessException("index [" + count + "] out of bounds on collections", property, cursor, pCtx);

            Iterator iter = ((Collection) ctx).iterator();
            for (int i = 0; i < count; i++)
                iter.next();
            return iter.next();
        } else if (ctx.getClass().isArray()) {
            return Array.get(ctx, (Integer) eval(prop, ctx, variableFactory));
        } else if (ctx instanceof CharSequence) {
            return ((CharSequence) ctx).charAt((Integer) eval(prop, ctx, variableFactory));
        } else {
            try {
                return getClassReference(pCtx, (Class) ctx, new TypeDescriptor(property, start, length, 0));
            } catch (Exception e) {
                throw new PropertyAccessException("illegal use of []: unknown type: " + (ctx.getClass().getName()), property, st, e, pCtx);
            }
        }
    }

    private Object getCollectionPropertyAO(Object ctx, String prop) throws Exception {
        if (prop.length() != 0) {
            ctx = getBeanProperty(ctx, prop);
        }

        currType = null;
        if (ctx == null) return null;

        int _start = ++cursor;

        whiteSpaceSkip();

        if (cursor == end || scanTo(']')) throw new PropertyAccessException("unterminated '['", property, cursor, pCtx);

        prop = new String(property, _start, cursor++ - _start);

        if (ctx instanceof Map) {
            if (hasPropertyHandler(Map.class)) return getPropertyHandler(Map.class).getProperty(prop, ctx, variableFactory);
            else return ((Map) ctx).get(eval(prop, ctx, variableFactory));
        } else if (ctx instanceof List) {
            if (hasPropertyHandler(List.class)) return getPropertyHandler(List.class).getProperty(prop, ctx, variableFactory);
            else return ((List) ctx).get((Integer) eval(prop, ctx, variableFactory));
        } else if (ctx instanceof Collection) {
            if (hasPropertyHandler(Collection.class)) return getPropertyHandler(Collection.class).getProperty(prop, ctx, variableFactory);
            else {
                int count = (Integer) eval(prop, ctx, variableFactory);
                if (count > ((Collection) ctx).size())
                    throw new PropertyAccessException("index [" + count + "] out of bounds on collections", property, cursor, pCtx);

                Iterator iter = ((Collection) ctx).iterator();
                for (int i = 0; i < count; i++)
                    iter.next();
                return iter.next();
            }
        } else if (ctx.getClass().isArray()) {
            if (hasPropertyHandler(Array.class)) return getPropertyHandler(Array.class).getProperty(prop, ctx, variableFactory);

            return Array.get(ctx, (Integer) eval(prop, ctx, variableFactory));
        } else if (ctx instanceof CharSequence) {
            if (hasPropertyHandler(CharSequence.class))
                return getPropertyHandler(CharSequence.class).getProperty(prop, ctx, variableFactory);
            else return ((CharSequence) ctx).charAt((Integer) eval(prop, ctx, variableFactory));
        } else {
            try {
                return getClassReference(pCtx, (Class) ctx, new TypeDescriptor(property, start, end - start, 0));
            } catch (Exception e) {
                throw new PropertyAccessException("illegal use of []: unknown type: " + (ctx.getClass().getName()), property, st, pCtx);
            }
        }
    }

    /**
     * Find an appropriate method, execute it, and return it's response.
     *
     * @param ctx  -
     * @param name -
     * @return -
     */
    @SuppressWarnings({ "unchecked" })
    private Object getMethod(Object ctx, String name) {
        int _start = cursor;

        String tk = cursor != end && property[cursor] == '('
                && ((cursor = balancedCapture(property, cursor, '(')) - _start) > 1 ? new String(property, _start + 1,
                        cursor - _start - 1) : "";

        cursor++;

        Object[] args;
        if (tk.length() == 0) {
            args = ParseTools.EMPTY_OBJ_ARR;
        } else {
            List<char[]> subtokens = parseParameterList(tk.toCharArray(), 0, -1);
            args = new Object[subtokens.size()];
            for (int i = 0; i < subtokens.size(); i++) {
                args[i] = eval(subtokens.get(i), thisReference, variableFactory);
            }
        }

        if (first && variableFactory != null && variableFactory.isResolveable(name)) {
            Object ptr = variableFactory.getVariableResolver(name).getValue();
            if (ptr instanceof Method) {
                ctx = ((Method) ptr).getDeclaringClass();
                name = ((Method) ptr).getName();
            } else if (ptr instanceof MethodStub) {
                ctx = ((MethodStub) ptr).getClassReference();
                name = ((MethodStub) ptr).getMethodName();
            } else if (ptr instanceof FunctionInstance) {
                ((FunctionInstance) ptr).getFunction().checkArgumentCount(args.length);
                return ((FunctionInstance) ptr).call(null, thisReference, variableFactory, args);
            } else {
                throw new OptimizationFailure("attempt to optimize a method call for a reference that does not point to a method: " + name
                        + " (reference is type: " + (ctx != null ? ctx.getClass().getName() : null) + ")");
            }

            first = false;
        }

        if (ctx == null) throw new CompileException("no such method or function: " + name, property, cursor);

        /**
         * If the target object is an instance of java.lang.Class itself then do not
         * adjust the Class scope target.
         */
        Class cls = currType != null ? currType : ((ctx instanceof Class ? (Class) ctx : ctx.getClass()));
        currType = null;

        if (cls == Proto.ProtoInstance.class) {
            return ((Proto.ProtoInstance) ctx).get(name).call(null, thisReference, variableFactory, args);
        }

        /**
         * Check to see if we have already cached this method;
         */
        Object[] cache = checkMethodCache(cls, createSignature(name, tk));

        Method m;
        Class[] parameterTypes;

        if (cache != null) {
            m = (Method) cache[0];
            parameterTypes = (Class[]) cache[1];
        } else {
            m = null;
            parameterTypes = null;
        }

        /**
         * If we have not cached the method then we need to go ahead and try to resolve it.
         */
        if (m == null) {
            /**
             * Try to find an instance method from the class target.
             */
            if ((m = getBestCandidate(args, name, cls, cls.getMethods(), false)) != null) {
                addMethodCache(cls, createSignature(name, tk), m);
                parameterTypes = m.getParameterTypes();
            }

            if (m == null) {
                /**
                 * If we didn't find anything, maybe we're looking for the actual java.lang.Class methods.
                 */
                if ((m = getBestCandidate(args, name, cls, cls.getDeclaredMethods(), false)) != null) {
                    addMethodCache(cls, createSignature(name, tk), m);
                    parameterTypes = m.getParameterTypes();
                }
            }
        }

        // If we didn't find anything and the declared class is different from the actual one try also with the actual one
        if (m == null && cls != ctx.getClass() && !(ctx instanceof Class)) {
            cls = ctx.getClass();
            if ((m = getBestCandidate(args, name, cls, cls.getDeclaredMethods(), false)) != null) {
                addMethodCache(cls, createSignature(name, tk), m);
                parameterTypes = m.getParameterTypes();
            }
        }

        if (ctx instanceof PrototypalFunctionInstance) {
            final VariableResolverFactory funcCtx = ((PrototypalFunctionInstance) ctx).getResolverFactory();
            Object prop = funcCtx.getVariableResolver(name).getValue();
            if (prop instanceof PrototypalFunctionInstance) {
                return ((PrototypalFunctionInstance) prop).call(ctx, thisReference, new InvokationContextFactory(variableFactory, funcCtx),
                        args);
            }
        }

        if (m == null) {
            StringAppender errorBuild = new StringAppender();
            for (int i = 0; i < args.length; i++) {
                errorBuild.append(args[i] != null ? args[i].getClass().getName() : null);
                if (i < args.length - 1) errorBuild.append(", ");
            }

            if ("size".equals(name) && args.length == 0 && cls.isArray()) {
                return getLength(ctx);
            }

            //      System.out.println("{ " + new String(property) + " }");

            throw new PropertyAccessException("unable to resolve method: " + cls.getName() + "." + name + "(" + errorBuild.toString()
                    + ") [arglength=" + args.length + "]", property, st, pCtx);
        } else {
            for (int i = 0; i < args.length; i++) {
                args[i] = convert(args[i], paramTypeVarArgsSafe(parameterTypes, i, m.isVarArgs()));
            }

            /**
             * Invoke the target method and return the response.
             */
            currType = toNonPrimitiveType(m.getReturnType());
            m.setAccessible(true);
            try {
                return m.invoke(ctx, normalizeArgsForVarArgs(parameterTypes, args, m.isVarArgs()));
            } catch (IllegalAccessException e) {
                try {
                    addMethodCache(cls, createSignature(name, tk), (m = getWidenedTarget(m)));
                    return m.invoke(ctx, args);
                } catch (Exception e2) {
                    throw new PropertyAccessException("unable to invoke method: " + name, property, cursor, e2, pCtx);
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new PropertyAccessException("unable to invoke method: " + name, property, cursor, e, pCtx);
            }
        }
    }

    private ClassLoader getClassLoader() {
        return pCtx != null ? pCtx.getClassLoader() : currentThread().getContextClassLoader();
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
            int last = end;
            for (int i = end - 1; i > start; i--) {
                switch (property[i]) {
                    case '.':
                        if (!meth) {
                            try {
                                String test = new String(property, start, (cursor = last) - start);

                                if (MVEL.COMPILER_OPT_SUPPORT_JAVA_STYLE_CLASS_LITERALS && test.endsWith(".class"))
                                    test = test.substring(0, test.length() - 6);

                                return getClassLoader().loadClass(test);
                            } catch (ClassNotFoundException e) {
                                Class cls = getClassLoader().loadClass(new String(property, start, i - start));
                                String name = new String(property, i + 1, end - i - 1);
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
                        for (int d = 1; i > 0 && d != 0; i--) {
                            switch (property[i]) {
                                case '}':
                                    d++;
                                    break;
                                case '{':
                                    d--;
                                    break;
                                case '"':
                                case '\'':
                                    char s = property[i];
                                    while (i > 0 && (property[i] != s && property[i - 1] != '\\'))
                                        i--;
                            }
                        }
                        break;

                    case ')':
                        i--;

                        for (int d = 1; i > 0 && d != 0; i--) {
                            switch (property[i]) {
                                case ')':
                                    d++;
                                    break;
                                case '(':
                                    d--;
                                    break;
                                case '"':
                                case '\'':
                                    char s = property[i];
                                    while (i > 0 && (property[i] != s && property[i - 1] != '\\'))
                                        i--;
                            }
                        }

                        meth = true;
                        last = i++;
                        break;

                    case '\'':
                        while (--i > 0) {
                            if (property[i] == '\'' && property[i - 1] != '\\') {
                                break;
                            }
                        }
                        break;

                    case '"':
                        while (--i > 0) {
                            if (property[i] == '"' && property[i - 1] != '\\') {
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
}
