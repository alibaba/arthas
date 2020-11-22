package com.taobao.arthas.common;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * from spring
 * @version $Id: ReflectUtils.java,v 1.30 2009/01/11 19:47:49 herbyderby Exp $
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ReflectUtils {

    private ReflectUtils() {
    }

    private static final Map primitives = new HashMap(8);

    private static final Map transforms = new HashMap(8);

    private static final ClassLoader defaultLoader = ReflectUtils.class.getClassLoader();

    // SPRING PATCH BEGIN
    private static final Method privateLookupInMethod;

    private static final Method lookupDefineClassMethod;

    private static final Method classLoaderDefineClassMethod;

    private static final ProtectionDomain PROTECTION_DOMAIN;

    private static final Throwable THROWABLE;

    private static final List<Method> OBJECT_METHODS = new ArrayList<Method>();

    static {
        Method privateLookupIn;
        Method lookupDefineClass;
        Method classLoaderDefineClass;
        ProtectionDomain protectionDomain;
        Throwable throwable = null;
        try {
            privateLookupIn = (Method) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    try {
                        return MethodHandles.class.getMethod("privateLookupIn", Class.class,
                                        MethodHandles.Lookup.class);
                    } catch (NoSuchMethodException ex) {
                        return null;
                    }
                }
            });
            lookupDefineClass = (Method) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    try {
                        return MethodHandles.Lookup.class.getMethod("defineClass", byte[].class);
                    } catch (NoSuchMethodException ex) {
                        return null;
                    }
                }
            });
            classLoaderDefineClass = (Method) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    return ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, Integer.TYPE,
                                    Integer.TYPE, ProtectionDomain.class);
                }
            });
            protectionDomain = getProtectionDomain(ReflectUtils.class);
            AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    Method[] methods = Object.class.getDeclaredMethods();
                    for (Method method : methods) {
                        if ("finalize".equals(method.getName())
                                        || (method.getModifiers() & (Modifier.FINAL | Modifier.STATIC)) > 0) {
                            continue;
                        }
                        OBJECT_METHODS.add(method);
                    }
                    return null;
                }
            });
        } catch (Throwable t) {
            privateLookupIn = null;
            lookupDefineClass = null;
            classLoaderDefineClass = null;
            protectionDomain = null;
            throwable = t;
        }
        privateLookupInMethod = privateLookupIn;
        lookupDefineClassMethod = lookupDefineClass;
        classLoaderDefineClassMethod = classLoaderDefineClass;
        PROTECTION_DOMAIN = protectionDomain;
        THROWABLE = throwable;
    }
    // SPRING PATCH END

    private static final String[] CGLIB_PACKAGES = { "java.lang", };

    static {
        primitives.put("byte", Byte.TYPE);
        primitives.put("char", Character.TYPE);
        primitives.put("double", Double.TYPE);
        primitives.put("float", Float.TYPE);
        primitives.put("int", Integer.TYPE);
        primitives.put("long", Long.TYPE);
        primitives.put("short", Short.TYPE);
        primitives.put("boolean", Boolean.TYPE);

        transforms.put("byte", "B");
        transforms.put("char", "C");
        transforms.put("double", "D");
        transforms.put("float", "F");
        transforms.put("int", "I");
        transforms.put("long", "J");
        transforms.put("short", "S");
        transforms.put("boolean", "Z");
    }

    public static ProtectionDomain getProtectionDomain(final Class source) {
        if (source == null) {
            return null;
        }
        return (ProtectionDomain) AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                return source.getProtectionDomain();
            }
        });
    }

    public static Constructor findConstructor(String desc) {
        return findConstructor(desc, defaultLoader);
    }

    public static Constructor findConstructor(String desc, ClassLoader loader) {
        try {
            int lparen = desc.indexOf('(');
            String className = desc.substring(0, lparen).trim();
            return getClass(className, loader).getConstructor(parseTypes(desc, loader));
        } catch (ClassNotFoundException ex) {
            throw new ReflectException(ex);
        } catch (NoSuchMethodException ex) {
            throw new ReflectException(ex);
        }
    }

    public static Method findMethod(String desc) {
        return findMethod(desc, defaultLoader);
    }

    public static Method findMethod(String desc, ClassLoader loader) {
        try {
            int lparen = desc.indexOf('(');
            int dot = desc.lastIndexOf('.', lparen);
            String className = desc.substring(0, dot).trim();
            String methodName = desc.substring(dot + 1, lparen).trim();
            return getClass(className, loader).getDeclaredMethod(methodName, parseTypes(desc, loader));
        } catch (ClassNotFoundException ex) {
            throw new ReflectException(ex);
        } catch (NoSuchMethodException ex) {
            throw new ReflectException(ex);
        }
    }

    private static Class[] parseTypes(String desc, ClassLoader loader) throws ClassNotFoundException {
        int lparen = desc.indexOf('(');
        int rparen = desc.indexOf(')', lparen);
        List params = new ArrayList();
        int start = lparen + 1;
        for (;;) {
            int comma = desc.indexOf(',', start);
            if (comma < 0) {
                break;
            }
            params.add(desc.substring(start, comma).trim());
            start = comma + 1;
        }
        if (start < rparen) {
            params.add(desc.substring(start, rparen).trim());
        }
        Class[] types = new Class[params.size()];
        for (int i = 0; i < types.length; i++) {
            types[i] = getClass((String) params.get(i), loader);
        }
        return types;
    }

    private static Class getClass(String className, ClassLoader loader) throws ClassNotFoundException {
        return getClass(className, loader, CGLIB_PACKAGES);
    }

    private static Class getClass(String className, ClassLoader loader, String[] packages)
                    throws ClassNotFoundException {
        String save = className;
        int dimensions = 0;
        int index = 0;
        while ((index = className.indexOf("[]", index) + 1) > 0) {
            dimensions++;
        }
        StringBuffer brackets = new StringBuffer(className.length() - dimensions);
        for (int i = 0; i < dimensions; i++) {
            brackets.append('[');
        }
        className = className.substring(0, className.length() - 2 * dimensions);

        String prefix = (dimensions > 0) ? brackets + "L" : "";
        String suffix = (dimensions > 0) ? ";" : "";
        try {
            return Class.forName(prefix + className + suffix, false, loader);
        } catch (ClassNotFoundException ignore) {
        }
        for (int i = 0; i < packages.length; i++) {
            try {
                return Class.forName(prefix + packages[i] + '.' + className + suffix, false, loader);
            } catch (ClassNotFoundException ignore) {
            }
        }
        if (dimensions == 0) {
            Class c = (Class) primitives.get(className);
            if (c != null) {
                return c;
            }
        } else {
            String transform = (String) transforms.get(className);
            if (transform != null) {
                try {
                    return Class.forName(brackets + transform, false, loader);
                } catch (ClassNotFoundException ignore) {
                }
            }
        }
        throw new ClassNotFoundException(save);
    }

    public static final Class[] EMPTY_CLASS_ARRAY = new Class[0];

    public static Object newInstance(Class type) {
        return newInstance(type, EMPTY_CLASS_ARRAY, null);
    }

    public static Object newInstance(Class type, Class[] parameterTypes, Object[] args) {
        return newInstance(getConstructor(type, parameterTypes), args);
    }

    public static Object newInstance(final Constructor cstruct, final Object[] args) {
        boolean flag = cstruct.isAccessible();
        try {
            if (!flag) {
                cstruct.setAccessible(true);
            }
            Object result = cstruct.newInstance(args);
            return result;
        } catch (InstantiationException e) {
            throw new ReflectException(e);
        } catch (IllegalAccessException e) {
            throw new ReflectException(e);
        } catch (InvocationTargetException e) {
            throw new ReflectException(e.getTargetException());
        } finally {
            if (!flag) {
                cstruct.setAccessible(flag);
            }
        }
    }

    public static Constructor getConstructor(Class type, Class[] parameterTypes) {
        try {
            Constructor constructor = type.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException e) {
            throw new ReflectException(e);
        }
    }

    public static String[] getNames(Class[] classes) {
        if (classes == null)
            return null;
        String[] names = new String[classes.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = classes[i].getName();
        }
        return names;
    }

    public static Class[] getClasses(Object[] objects) {
        Class[] classes = new Class[objects.length];
        for (int i = 0; i < objects.length; i++) {
            classes[i] = objects[i].getClass();
        }
        return classes;
    }

    public static Method findNewInstance(Class iface) {
        Method m = findInterfaceMethod(iface);
        if (!m.getName().equals("newInstance")) {
            throw new IllegalArgumentException(iface + " missing newInstance method");
        }
        return m;
    }

    public static Method[] getPropertyMethods(PropertyDescriptor[] properties, boolean read, boolean write) {
        Set methods = new HashSet();
        for (int i = 0; i < properties.length; i++) {
            PropertyDescriptor pd = properties[i];
            if (read) {
                methods.add(pd.getReadMethod());
            }
            if (write) {
                methods.add(pd.getWriteMethod());
            }
        }
        methods.remove(null);
        return (Method[]) methods.toArray(new Method[methods.size()]);
    }

    public static PropertyDescriptor[] getBeanProperties(Class type) {
        return getPropertiesHelper(type, true, true);
    }

    public static PropertyDescriptor[] getBeanGetters(Class type) {
        return getPropertiesHelper(type, true, false);
    }

    public static PropertyDescriptor[] getBeanSetters(Class type) {
        return getPropertiesHelper(type, false, true);
    }

    private static PropertyDescriptor[] getPropertiesHelper(Class type, boolean read, boolean write) {
        try {
            BeanInfo info = Introspector.getBeanInfo(type, Object.class);
            PropertyDescriptor[] all = info.getPropertyDescriptors();
            if (read && write) {
                return all;
            }
            List properties = new ArrayList(all.length);
            for (int i = 0; i < all.length; i++) {
                PropertyDescriptor pd = all[i];
                if ((read && pd.getReadMethod() != null) || (write && pd.getWriteMethod() != null)) {
                    properties.add(pd);
                }
            }
            return (PropertyDescriptor[]) properties.toArray(new PropertyDescriptor[properties.size()]);
        } catch (IntrospectionException e) {
            throw new ReflectException(e);
        }
    }

    public static Method findDeclaredMethod(final Class type, final String methodName, final Class[] parameterTypes)
                    throws NoSuchMethodException {

        Class cl = type;
        while (cl != null) {
            try {
                return cl.getDeclaredMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException e) {
                cl = cl.getSuperclass();
            }
        }
        throw new NoSuchMethodException(methodName);
    }

    public static List addAllMethods(final Class type, final List list) {
        if (type == Object.class) {
            list.addAll(OBJECT_METHODS);
        } else
            list.addAll(java.util.Arrays.asList(type.getDeclaredMethods()));

        Class superclass = type.getSuperclass();
        if (superclass != null) {
            addAllMethods(superclass, list);
        }
        Class[] interfaces = type.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            addAllMethods(interfaces[i], list);
        }

        return list;
    }

    public static List addAllInterfaces(Class type, List list) {
        Class superclass = type.getSuperclass();
        if (superclass != null) {
            list.addAll(Arrays.asList(type.getInterfaces()));
            addAllInterfaces(superclass, list);
        }
        return list;
    }

    public static Method findInterfaceMethod(Class iface) {
        if (!iface.isInterface()) {
            throw new IllegalArgumentException(iface + " is not an interface");
        }
        Method[] methods = iface.getDeclaredMethods();
        if (methods.length != 1) {
            throw new IllegalArgumentException("expecting exactly 1 method in " + iface);
        }
        return methods[0];
    }

    // SPRING PATCH BEGIN
    public static Class defineClass(String className, byte[] b, ClassLoader loader) throws Exception {
        return defineClass(className, b, loader, null, null);
    }

    public static Class defineClass(String className, byte[] b, ClassLoader loader, ProtectionDomain protectionDomain)
                    throws Exception {

        return defineClass(className, b, loader, protectionDomain, null);
    }

    public static Class defineClass(String className, byte[] b, ClassLoader loader, ProtectionDomain protectionDomain,
                    Class<?> contextClass) throws Exception {

        Class c = null;

        // Preferred option: JDK 9+ Lookup.defineClass API if ClassLoader matches
        if (contextClass != null && contextClass.getClassLoader() == loader && privateLookupInMethod != null
                        && lookupDefineClassMethod != null) {
            try {
                MethodHandles.Lookup lookup = (MethodHandles.Lookup) privateLookupInMethod.invoke(null, contextClass,
                                MethodHandles.lookup());
                c = (Class) lookupDefineClassMethod.invoke(lookup, b);
            } catch (InvocationTargetException ex) {
                Throwable target = ex.getTargetException();
                if (target.getClass() != LinkageError.class && target.getClass() != IllegalArgumentException.class) {
                    throw new ReflectException(target);
                }
                // in case of plain LinkageError (class already defined)
                // or IllegalArgumentException (class in different package):
                // fall through to traditional ClassLoader.defineClass below
            } catch (Throwable ex) {
                throw new ReflectException(ex);
            }
        }

        // Classic option: protected ClassLoader.defineClass method
        if (c == null && classLoaderDefineClassMethod != null) {
            if (protectionDomain == null) {
                protectionDomain = PROTECTION_DOMAIN;
            }
            Object[] args = new Object[] { className, b, 0, b.length, protectionDomain };
            try {
                if (!classLoaderDefineClassMethod.isAccessible()) {
                    classLoaderDefineClassMethod.setAccessible(true);
                }
                c = (Class) classLoaderDefineClassMethod.invoke(loader, args);
            } catch (InvocationTargetException ex) {
                throw new ReflectException(ex.getTargetException());
            } catch (Throwable ex) {
                // Fall through if setAccessible fails with InaccessibleObjectException on JDK
                // 9+
                // (on the module path and/or with a JVM bootstrapped with
                // --illegal-access=deny)
                if (!ex.getClass().getName().endsWith("InaccessibleObjectException")) {
                    throw new ReflectException(ex);
                }
            }
        }

        // Fallback option: JDK 9+ Lookup.defineClass API even if ClassLoader does not
        // match
        if (c == null && contextClass != null && contextClass.getClassLoader() != loader
                        && privateLookupInMethod != null && lookupDefineClassMethod != null) {
            try {
                MethodHandles.Lookup lookup = (MethodHandles.Lookup) privateLookupInMethod.invoke(null, contextClass,
                                MethodHandles.lookup());
                c = (Class) lookupDefineClassMethod.invoke(lookup, b);
            } catch (InvocationTargetException ex) {
                throw new ReflectException(ex.getTargetException());
            } catch (Throwable ex) {
                throw new ReflectException(ex);
            }
        }

        // No defineClass variant available at all?
        if (c == null) {
            throw new ReflectException(THROWABLE);
        }

        // Force static initializers to run.
        Class.forName(className, true, loader);
        return c;
    }
    // SPRING PATCH END

    public static int findPackageProtected(Class[] classes) {
        for (int i = 0; i < classes.length; i++) {
            if (!Modifier.isPublic(classes[i].getModifiers())) {
                return i;
            }
        }
        return 0;
    }

}
