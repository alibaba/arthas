package com.taobao.arthas.common;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
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
 * 反射工具类
 * <p>
 * 该工具类提供了丰富的反射操作方法，包括：
 * 1. 类加载和定义（支持动态类加载）
 * 2. 方法和构造器的查找与调用
 * 3. JavaBean属性的 introspection
 * 4. 类型转换和解析
 * </p>
 * <p>
 * 代码来源于Spring框架，并进行了本地化修改和增强。
 * 特别增强了对JDK 9+模块化系统的支持，通过多种方式尝试定义类。
 * </p>
 *
 * @version $Id: ReflectUtils.java,v 1.30 2009/01/11 19:47:49 herbyderby Exp $
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ReflectUtils {

    /**
     * 私有构造函数
     * <p>
     * 防止实例化该工具类，所有方法均为静态方法。
     * </p>
     */
    private ReflectUtils() {
    }

    /**
     * 基本类型映射表
     * <p>
     * 存储基本类型名称到对应Class对象的映射关系。
     * 例如："int" -> Integer.TYPE
     * </p>
     */
    private static final Map primitives = new HashMap(8);

    /**
     * 类型转换映射表
     * <p>
     * 存储基本类型名称到JVM内部类型描述符的映射关系。
     * 例如："int" -> "I", "boolean" -> "Z"
     * 用于构造数组类型和反射调用
     * </p>
     */
    private static final Map transforms = new HashMap(8);

    /**
     * 默认类加载器
     * <p>
     * 使用ReflectUtils类本身的类加载器作为默认加载器。
     * </p>
     */
    private static final ClassLoader defaultLoader = ReflectUtils.class.getClassLoader();

    // ============ Spring框架增强部分开始 ============
    /**
     * MethodHandles.privateLookupIn方法的反射引用
     * <p>
     * JDK 9+引入的方法，用于获取对某个模块的私有访问权限。
     * 这是JDK 9+模块化系统中实现动态类定义的关键方法。
     * </p>
     */
    private static final Method privateLookupInMethod;

    /**
     * MethodHandles.Lookup.defineClass方法的反射引用
     * <p>
     * JDK 9+引入的方法，用于通过Lookup对象定义类。
     * 提供了比传统ClassLoader.defineClass更灵活的类定义方式。
     * </p>
     */
    private static final Method lookupDefineClassMethod;

    /**
     * ClassLoader.defineClass方法的反射引用
     * <p>
     * 传统的类定义方法，需要通过反射来访问（因为它是protected方法）。
     * 在JDK 9+之前的版本中，这是主要的动态类定义方式。
     * </p>
     */
    private static final Method classLoaderDefineClassMethod;

    /**
     * 保护域对象
     * <p>
     * 用于定义类时的安全策略配置。
     * 如果调用者未指定保护域，则使用此默认值。
     * </p>
     */
    private static final ProtectionDomain PROTECTION_DOMAIN;

    /**
     * 初始化过程中抛出的异常
     * <p>
     * 如果获取上述方法失败，保存异常信息供后续使用。
     * </p>
     */
    private static final Throwable THROWABLE;

    /**
     * Object类的方法列表
     * <p>
     * 缓存Object类的所有public方法（除finalize和final/static方法外）。
     * 用于快速添加Object的方法到方法列表中。
     * </p>
     */
    private static final List<Method> OBJECT_METHODS = new ArrayList<Method>();

    /**
     * 静态初始化块
     * <p>
     * 初始化各种反射方法引用和配置信息。
     * 通过AccessController.doPrivileged执行特权操作，绕过安全检查。
     * </p>
     */
    static {
        Method privateLookupIn;
        Method lookupDefineClass;
        Method classLoaderDefineClass;
        ProtectionDomain protectionDomain;
        Throwable throwable = null;
        try {
            // 尝试获取MethodHandles.privateLookupIn方法（JDK 9+）
            privateLookupIn = (Method) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    try {
                        return MethodHandles.class.getMethod("privateLookupIn", Class.class,
                                        MethodHandles.Lookup.class);
                    } catch (NoSuchMethodException ex) {
                        // JDK 9之前不存在此方法，返回null
                        return null;
                    }
                }
            });
            // 尝试获取MethodHandles.Lookup.defineClass方法（JDK 9+）
            lookupDefineClass = (Method) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    try {
                        return MethodHandles.Lookup.class.getMethod("defineClass", byte[].class);
                    } catch (NoSuchMethodException ex) {
                        // JDK 9之前不存在此方法，返回null
                        return null;
                    }
                }
            });
            // 获取ClassLoader.defineClass方法（需要通过反射访问protected方法）
            classLoaderDefineClass = (Method) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    return ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, Integer.TYPE,
                                    Integer.TYPE, ProtectionDomain.class);
                }
            });
            // 获取ReflectUtils类自身的保护域作为默认保护域
            protectionDomain = getProtectionDomain(ReflectUtils.class);
            // 收集Object类的所有方法（排除finalize和final/static方法）
            AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    Method[] methods = Object.class.getDeclaredMethods();
                    for (Method method : methods) {
                        // 跳过finalize方法和final/static方法
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
            // 如果初始化失败，将所有引用置为null
            privateLookupIn = null;
            lookupDefineClass = null;
            classLoaderDefineClass = null;
            protectionDomain = null;
            throwable = t;
        }
        // 将临时变量赋值给final字段
        privateLookupInMethod = privateLookupIn;
        lookupDefineClassMethod = lookupDefineClass;
        classLoaderDefineClassMethod = classLoaderDefineClass;
        PROTECTION_DOMAIN = protectionDomain;
        THROWABLE = throwable;
    }
    // ============ Spring框架增强部分结束 ============

    /**
     * CGLIB默认包列表
     * <p>
     * 当类名未指定完整包名时，在这些包中查找类。
     * 默认包含java.lang包。
     * </p>
     */
    private static final String[] CGLIB_PACKAGES = { "java.lang", };

    /**
     * 基本类型和类型转换表的静态初始化块
     * <p>
     * 初始化基本类型映射表和类型转换映射表。
     * 这些映射表用于类加载和类型解析。
     * </p>
     */
    static {
        // 初始化基本类型映射表：类型名 -> 基本类型Class对象
        primitives.put("byte", Byte.TYPE);
        primitives.put("char", Character.TYPE);
        primitives.put("double", Double.TYPE);
        primitives.put("float", Float.TYPE);
        primitives.put("int", Integer.TYPE);
        primitives.put("long", Long.TYPE);
        primitives.put("short", Short.TYPE);
        primitives.put("boolean", Boolean.TYPE);

        // 初始化类型转换映射表：类型名 -> JVM类型描述符
        // 这些描述符用于数组和反射操作
        transforms.put("byte", "B");
        transforms.put("char", "C");
        transforms.put("double", "D");
        transforms.put("float", "F");
        transforms.put("int", "I");
        transforms.put("long", "J");
        transforms.put("short", "S");
        transforms.put("boolean", "Z");
    }

    /**
     * 获取类的保护域
     * <p>
     * 通过特权操作获取指定类的保护域对象。
     * 保护域定义了代码来源和权限策略。
     * </p>
     *
     * @param source 要获取保护域的类对象
     * @return 类的保护域对象，如果source为null则返回null
     */
    public static ProtectionDomain getProtectionDomain(final Class source) {
        if (source == null) {
            return null;
        }
        // 通过特权操作绕过安全管理器检查
        return (ProtectionDomain) AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                return source.getProtectionDomain();
            }
        });
    }

    /**
     * 查找构造器（使用默认类加载器）
     * <p>
     * 根据描述符查找构造器，使用ReflectUtils类的类加载器。
     * 描述符格式：完整类名(参数类型1, 参数类型2, ...)
     * 例如："java.lang.String(byte[], int)"
     * </p>
     *
     * @param desc 构造器描述符
     * @return 找到的构造器对象
     * @throws ReflectException 如果类或方法未找到
     */
    public static Constructor findConstructor(String desc) {
        return findConstructor(desc, defaultLoader);
    }

    /**
     * 查找构造器（使用指定类加载器）
     * <p>
     * 根据描述符查找构造器，使用指定的类加载器。
     * 描述符格式：完整类名(参数类型1, 参数类型2, ...)
     * </p>
     *
     * @param desc 构造器描述符
     * @param loader 用于加载类的类加载器
     * @return 找到的构造器对象
     * @throws ReflectException 如果类或方法未找到
     */
    public static Constructor findConstructor(String desc, ClassLoader loader) {
        try {
            // 查找左括号位置，参数列表从左括号开始
            int lparen = desc.indexOf('(');
            // 提取类名（左括号之前的部分）
            String className = desc.substring(0, lparen).trim();
            // 获取类并查找构造器
            return getClass(className, loader).getConstructor(parseTypes(desc, loader));
        } catch (ClassNotFoundException ex) {
            throw new ReflectException(ex);
        } catch (NoSuchMethodException ex) {
            throw new ReflectException(ex);
        }
    }

    /**
     * 查找方法（使用默认类加载器）
     * <p>
     * 根据描述符查找方法，使用ReflectUtils类的类加载器。
     * 描述符格式：完整类名.方法名(参数类型1, 参数类型2, ...)
     * 例如："java.lang.String.substring(int, int)"
     * </p>
     *
     * @param desc 方法描述符
     * @return 找到的方法对象
     * @throws ReflectException 如果类或方法未找到
     */
    public static Method findMethod(String desc) {
        return findMethod(desc, defaultLoader);
    }

    /**
     * 查找方法（使用指定类加载器）
     * <p>
     * 根据描述符查找方法，使用指定的类加载器。
     * 描述符格式：完整类名.方法名(参数类型1, 参数类型2, ...)
     * </p>
     *
     * @param desc 方法描述符
     * @param loader 用于加载类的类加载器
     * @return 找到的方法对象
     * @throws ReflectException 如果类或方法未找到
     */
    public static Method findMethod(String desc, ClassLoader loader) {
        try {
            // 查找左括号位置
            int lparen = desc.indexOf('(');
            // 查找方法名前的最后一个点（类名和方法名的分隔符）
            int dot = desc.lastIndexOf('.', lparen);
            // 提取类名
            String className = desc.substring(0, dot).trim();
            // 提取方法名（点之后、左括号之前）
            String methodName = desc.substring(dot + 1, lparen).trim();
            // 获取类并查找方法
            return getClass(className, loader).getDeclaredMethod(methodName, parseTypes(desc, loader));
        } catch (ClassNotFoundException ex) {
            throw new ReflectException(ex);
        } catch (NoSuchMethodException ex) {
            throw new ReflectException(ex);
        }
    }

    /**
     * 解析方法或构造器的参数类型
     * <p>
     * 从描述符中提取参数类型字符串，并转换为Class对象数组。
     * 参数列表格式：(类型1, 类型2, ...)
     * </p>
     *
     * @param desc 方法或构造器描述符
     * @param loader 用于加载参数类型的类加载器
     * @return 参数类型的Class对象数组
     * @throws ClassNotFoundException 如果参数类型未找到
     */
    private static Class[] parseTypes(String desc, ClassLoader loader) throws ClassNotFoundException {
        // 查找参数列表的左右括号
        int lparen = desc.indexOf('(');
        int rparen = desc.indexOf(')', lparen);
        // 存储参数类型字符串的列表
        List params = new ArrayList();
        int start = lparen + 1;
        // 循环查找逗号分隔的参数类型
        for (;;) {
            int comma = desc.indexOf(',', start);
            if (comma < 0) {
                break;
            }
            // 提取逗号前的参数类型
            params.add(desc.substring(start, comma).trim());
            start = comma + 1;
        }
        // 处理最后一个参数（没有逗号的情况）
        if (start < rparen) {
            params.add(desc.substring(start, rparen).trim());
        }
        // 将参数类型字符串转换为Class对象
        Class[] types = new Class[params.size()];
        for (int i = 0; i < types.length; i++) {
            types[i] = getClass((String) params.get(i), loader);
        }
        return types;
    }

    /**
     * 获取类对象（使用默认包列表）
     * <p>
     * 根据类名加载类，如果未指定完整包名，则在默认包列表中查找。
     * </p>
     *
     * @param className 类名（可以包含数组表示符[]）
     * @param loader 类加载器
     * @return 类的Class对象
     * @throws ClassNotFoundException 如果类未找到
     */
    private static Class getClass(String className, ClassLoader loader) throws ClassNotFoundException {
        return getClass(className, loader, CGLIB_PACKAGES);
    }

    /**
     * 获取类对象（使用指定包列表）
     * <p>
     * 根据类名加载类，支持以下特性：
     * 1. 数组类型表示（如"String[]"、"int[][]"）
     * 2. 基本类型（如"int"、"boolean"）
     * 3. 未指定包名时在包列表中查找
     * </p>
     *
     * @param className 类名
     * @param loader 类加载器
     * @param packages 默认包列表
     * @return 类的Class对象
     * @throws ClassNotFoundException 如果类未找到
     */
    private static Class getClass(String className, ClassLoader loader, String[] packages)
                    throws ClassNotFoundException {
        // 保存原始类名用于异常消息
        String save = className;
        // 计算数组维度
        int dimensions = 0;
        int index = 0;
        // 查找所有"[]"，每次找到维度加1
        while ((index = className.indexOf("[]", index) + 1) > 0) {
            dimensions++;
        }
        // 构建JVM数组表示符（每维度一个'['）
        StringBuilder brackets = new StringBuilder(className.length() - dimensions);
        for (int i = 0; i < dimensions; i++) {
            brackets.append('[');
        }
        // 移除所有的"[]"
        className = className.substring(0, className.length() - 2 * dimensions);

        // 构建类名的JVM内部表示
        // 对于数组类型，格式为：[L包名.类名;
        String prefix = (dimensions > 0) ? brackets + "L" : "";
        String suffix = (dimensions > 0) ? ";" : "";
        // 首先尝试直接加载完整类名
        try {
            return Class.forName(prefix + className + suffix, false, loader);
        } catch (ClassNotFoundException ignore) {
        }
        // 如果直接加载失败，尝试在默认包列表中查找
        for (int i = 0; i < packages.length; i++) {
            try {
                return Class.forName(prefix + packages[i] + '.' + className + suffix, false, loader);
            } catch (ClassNotFoundException ignore) {
            }
        }
        // 如果是非数组类型，尝试从基本类型映射表中查找
        if (dimensions == 0) {
            Class c = (Class) primitives.get(className);
            if (c != null) {
                return c;
            }
        } else {
            // 如果是数组类型，尝试使用基本类型的JVM描述符
            String transform = (String) transforms.get(className);
            if (transform != null) {
                try {
                    // 基本类型数组格式：[I（int数组）、[Z（boolean数组）等
                    return Class.forName(brackets + transform, false, loader);
                } catch (ClassNotFoundException ignore) {
                }
            }
        }
        // 所有尝试都失败，抛出异常
        throw new ClassNotFoundException(save);
    }

    /**
     * 空的Class数组常量
     * <p>
     * 用于表示无参数的方法或构造器。
     * </p>
     */
    public static final Class[] EMPTY_CLASS_ARRAY = new Class[0];

    /**
     * 创建类的实例（无参数）
     * <p>
     * 使用无参构造器创建类的实例。
     * </p>
     *
     * @param type 要实例化的类
     * @return 新创建的实例对象
     * @throws ReflectException 如果实例化失败
     */
    public static Object newInstance(Class type) {
        return newInstance(type, EMPTY_CLASS_ARRAY, null);
    }

    /**
     * 创建类的实例（带参数）
     * <p>
     * 使用指定参数类型的构造器创建类的实例。
     * </p>
     *
     * @param type 要实例化的类
     * @param parameterTypes 构造器参数类型数组
     * @param args 构造器参数值数组
     * @return 新创建的实例对象
     * @throws ReflectException 如果实例化失败
     */
    public static Object newInstance(Class type, Class[] parameterTypes, Object[] args) {
        return newInstance(getConstructor(type, parameterTypes), args);
    }

    /**
     * 使用构造器创建实例
     * <p>
     * 通过指定的构造器对象创建实例。
     * 如果构造器不可访问，会临时设置可访问性，创建完成后恢复。
     * </p>
     *
     * @param cstruct 构造器对象
     * @param args 构造器参数数组
     * @return 新创建的实例对象
     * @throws ReflectException 如果实例化失败
     */
    public static Object newInstance(final Constructor cstruct, final Object[] args) {
        // 保存构造器原本的可访问状态
        boolean flag = cstruct.isAccessible();
        try {
            // 如果构造器原本不可访问，临时设置为可访问
            if (!flag) {
                cstruct.setAccessible(true);
            }
            // 调用构造器创建实例
            Object result = cstruct.newInstance(args);
            return result;
        } catch (InstantiationException e) {
            throw new ReflectException(e);
        } catch (IllegalAccessException e) {
            throw new ReflectException(e);
        } catch (InvocationTargetException e) {
            // 抛出目标异常（构造器内部抛出的异常）
            throw new ReflectException(e.getTargetException());
        } finally {
            // 恢复构造器原本的可访问状态
            if (!flag) {
                cstruct.setAccessible(flag);
            }
        }
    }

    /**
     * 获取构造器
     * <p>
     * 根据参数类型获取类的声明构造器，并设置为可访问。
     * </p>
     *
     * @param type 类对象
     * @param parameterTypes 构造器参数类型数组
     * @return 构造器对象
     * @throws ReflectException 如果构造器未找到
     */
    public static Constructor getConstructor(Class type, Class[] parameterTypes) {
        try {
            Constructor constructor = type.getDeclaredConstructor(parameterTypes);
            // 设置为可访问，以便调用私有构造器
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException e) {
            throw new ReflectException(e);
        }
    }

    /**
     * 获取类数组的名称数组
     * <p>
     * 将Class对象数组转换为对应的完整类名数组。
     * </p>
     *
     * @param classes Class对象数组
     * @return 类名字符串数组，如果输入为null则返回null
     */
    public static String[] getNames(Class[] classes) {
        if (classes == null)
            return null;
        String[] names = new String[classes.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = classes[i].getName();
        }
        return names;
    }

    /**
     * 获取对象数组的类型数组
     * <p>
     * 将对象数组转换为对应的Class对象数组，每个元素是对象的实际类型。
     * </p>
     *
     * @param objects 对象数组
     * @return 对应的Class对象数组
     */
    public static Class[] getClasses(Object[] objects) {
        Class[] classes = new Class[objects.length];
        for (int i = 0; i < objects.length; i++) {
            classes[i] = objects[i].getClass();
        }
        return classes;
    }

    /**
     * 查找接口的newInstance方法
    * <p>
     * 查找接口中声明的唯一方法，并验证该方法名为"newInstance"。
     * 通常用于工厂模式或回调接口。
     * </p>
     *
     * @param iface 接口类
     * @return newInstance方法对象
     * @throws IllegalArgumentException 如果接口不是单一方法接口或方法名不是newInstance
     */
    public static Method findNewInstance(Class iface) {
        Method m = findInterfaceMethod(iface);
        if (!m.getName().equals("newInstance")) {
            throw new IllegalArgumentException(iface + " missing newInstance method");
        }
        return m;
    }

    /**
     * 获取属性的读写方法
     * <p>
     * 从属性描述符数组中提取getter和setter方法。
     * </p>
     *
     * @param properties 属性描述符数组
     * @param read 是否包含读取方法（getter）
     * @param write 是否包含写入方法（setter）
     * @return 方法数组
     */
    public static Method[] getPropertyMethods(PropertyDescriptor[] properties, boolean read, boolean write) {
        Set methods = new HashSet();
        for (int i = 0; i < properties.length; i++) {
            PropertyDescriptor pd = properties[i];
            // 如果需要读取方法，添加getter
            if (read) {
                methods.add(pd.getReadMethod());
            }
            // 如果需要写入方法，添加setter
            if (write) {
                methods.add(pd.getWriteMethod());
            }
        }
        // 移除null值（某些属性可能没有getter或setter）
        methods.remove(null);
        return (Method[]) methods.toArray(new Method[methods.size()]);
    }

    /**
     * 获取Bean的所有属性
     * <p>
     * 获取类的所有JavaBean属性（包括getter和setter）。
     * </p>
     *
     * @param type 要检查的类
     * @return 所有属性的描述符数组
     */
    public static PropertyDescriptor[] getBeanProperties(Class type) {
        return getPropertiesHelper(type, true, true);
    }

    /**
     * 获取Bean的读取属性
     * <p>
     * 获取类的所有具有getter方法的属性。
     * </p>
     *
     * @param type 要检查的类
     * @return 具有getter方法的属性描述符数组
     */
    public static PropertyDescriptor[] getBeanGetters(Class type) {
        return getPropertiesHelper(type, true, false);
    }

    /**
     * 获取Bean的写入属性
     * <p>
     * 获取类的所有具有setter方法的属性。
     * </p>
     *
     * @param type 要检查的类
     * @return 具有setter方法的属性描述符数组
     */
    public static PropertyDescriptor[] getBeanSetters(Class type) {
        return getPropertiesHelper(type, false, true);
    }

    /**
     * 获取Bean属性的辅助方法
     * <p>
     * 根据读写标志筛选属性描述符。
     * </p>
     *
     * @param type 要检查的类
     * @param read 是否需要getter方法
     * @param write 是否需要setter方法
     * @return 筛选后的属性描述符数组
     */
    private static PropertyDescriptor[] getPropertiesHelper(Class type, boolean read, boolean write) {
        try {
            // 使用JavaBean内省机制获取属性信息
            // 停止在Object类，不包括Object的属性
            BeanInfo info = Introspector.getBeanInfo(type, Object.class);
            PropertyDescriptor[] all = info.getPropertyDescriptors();
            // 如果同时需要读写，返回所有属性
            if (read && write) {
                return all;
            }
            // 根据标志筛选属性
            List properties = new ArrayList(all.length);
            for (int i = 0; i < all.length; i++) {
                PropertyDescriptor pd = all[i];
                // 保留有getter或setter的属性
                if ((read && pd.getReadMethod() != null) || (write && pd.getWriteMethod() != null)) {
                    properties.add(pd);
                }
            }
            return (PropertyDescriptor[]) properties.toArray(new PropertyDescriptor[properties.size()]);
        } catch (IntrospectionException e) {
            throw new ReflectException(e);
        }
    }

    /**
     * 查找已声明的方法
     * <p>
     * 在类及其父类中查找指定方法。
     * </p>
     *
     * @param type 要查找的类
     * @param methodName 方法名
     * @param parameterTypes 参数类型数组
     * @return 找到的方法对象
     * @throws NoSuchMethodException 如果方法未找到
     */
    public static Method findDeclaredMethod(final Class type, final String methodName, final Class[] parameterTypes)
                    throws NoSuchMethodException {

        Class cl = type;
        // 从当前类开始，向上遍历父类
        while (cl != null) {
            try {
                return cl.getDeclaredMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException e) {
                // 当前类没找到，继续查找父类
                cl = cl.getSuperclass();
            }
        }
        // 所有类都没找到，抛出异常
        throw new NoSuchMethodException(methodName);
    }

    /**
     * 添加类的所有方法到列表
     * <p>
     * 递归收集类及其父类、接口的所有方法。
     * 对于Object类，使用预缓存的方法列表。
     * </p>
     *
     * @param type 要收集方法的类
     * @param list 方法列表（结果会添加到此列表）
     * @return 包含所有方法的结果列表
     */
    public static List addAllMethods(final Class type, final List list) {
        if (type == Object.class) {
            // 对于Object类，使用预缓存的方法列表
            list.addAll(OBJECT_METHODS);
        } else
            // 添加当前类的所有声明方法
            list.addAll(java.util.Arrays.asList(type.getDeclaredMethods()));

        // 递归添加父类的方法
        Class superclass = type.getSuperclass();
        if (superclass != null) {
            addAllMethods(superclass, list);
        }
        // 递归添加接口的方法
        Class[] interfaces = type.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            addAllMethods(interfaces[i], list);
        }

        return list;
    }

    /**
     * 添加类的所有接口到列表
     * <p>
     * 递归收集类及其父类的所有接口。
     * </p>
     *
     * @param type 要收集接口的类
     * @param list 接口列表（结果会添加到此列表）
     * @return 包含所有接口的结果列表
     */
    public static List addAllInterfaces(Class type, List list) {
        // 获取父类
        Class superclass = type.getSuperclass();
        if (superclass != null) {
            // 添加当前类实现的接口
            list.addAll(Arrays.asList(type.getInterfaces()));
            // 递归处理父类
            addAllInterfaces(superclass, list);
        }
        return list;
    }

    /**
     * 查找接口的唯一方法
     * <p>
     * 查找接口中声明的唯一方法（单一方法接口）。
     * 常用于函数式接口或回调接口。
     * </p>
     *
     * @param iface 接口类
     * @return 接口中声明的方法
     * @throws IllegalArgumentException 如果不是接口或接口不是单一方法
     */
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

    // ============ Spring框架增强部分开始 ============
    /**
     * 定义类（使用默认保护域和上下文类）
     * <p>
     * 通过类加载器动态定义一个新类。
     * 使用默认的保护域和上下文类。
     * </p>
     *
     * @param className 类的全限定名
     * @param b 类的字节码
     * @param loader 用于定义类的类加载器
     * @return 定义的类对象
     * @throws Exception 如果类定义失败
     */
    public static Class defineClass(String className, byte[] b, ClassLoader loader) throws Exception {
        return defineClass(className, b, loader, null, null);
    }

    /**
     * 定义类（使用指定保护域）
     * <p>
     * 通过类加载器动态定义一个新类，使用指定的保护域。
     * </p>
     *
     * @param className 类的全限定名
     * @param b 类的字节码
     * @param loader 用于定义类的类加载器
     * @param protectionDomain 保护域
     * @return 定义的类对象
     * @throws Exception 如果类定义失败
     */
    public static Class defineClass(String className, byte[] b, ClassLoader loader, ProtectionDomain protectionDomain)
                    throws Exception {

        return defineClass(className, b, loader, protectionDomain, null);
    }

    /**
     * 定义类（使用指定保护域和上下文类）
     * <p>
     * 通过类加载器动态定义一个新类。
     * 该方法会尝试多种方式来定义类，以适应不同的JDK版本和模块化限制：
     * </p>
     * <ol>
     * <li>JDK 17+：使用implLookup（通过UnsafeUtils获取）</li>
     * <li>JDK 9+：使用MethodHandles.Lookup.defineClass（类加载器匹配时）</li>
     * <li>传统方式：使用ClassLoader.defineClass反射调用</li>
     * <li>JDK 9+：使用MethodHandles.Lookup.defineClass（类加载器不匹配时）</li>
     * </ol>
     *
     * @param className 类的全限定名
     * @param b 类的字节码
     * @param loader 用于定义类的类加载器
     * @param protectionDomain 保护域（可为null）
     * @param contextClass 上下文类（可为null）
     * @return 定义的类对象
     * @throws Exception 如果类定义失败
     */
    public static Class defineClass(String className, byte[] b, ClassLoader loader, ProtectionDomain protectionDomain,
                    Class<?> contextClass) throws Exception {

        Class c = null;

        // ============ 方案1：JDK 17+使用implLookup ============
        // 在JDK 17之后，需要使用implLookup来绕过模块化系统的限制
        // 参考issue #2659
        if (c == null && classLoaderDefineClassMethod != null) {
            // 获取implLookup（具有所有权限的Lookup对象）
            Lookup implLookup = UnsafeUtils.implLookup();
            MethodHandle unreflect = implLookup.unreflect(classLoaderDefineClassMethod);

            if (protectionDomain == null) {
                protectionDomain = PROTECTION_DOMAIN;
            }
            try {
                // 通过implLookup调用ClassLoader.defineClass方法
                c = (Class) unreflect.invoke(loader, className, b, 0, b.length, protectionDomain);
            } catch (InvocationTargetException ex) {
                throw new ReflectException(ex.getTargetException());
            } catch (Throwable ex) {
                // 如果setAccessible失败并抛出InaccessibleObjectException，则忽略
                // 这种情况发生在JDK 9+的模块路径上或使用--illegal-access=deny启动时
                if (!ex.getClass().getName().endsWith("InaccessibleObjectException")) {
                    throw new ReflectException(ex);
                }
            }
        }

        // ============ 方案2：JDK 9+ Lookup.defineClass API（类加载器匹配）============
        // 这是首选方案，当类加载器匹配时使用
        if (contextClass != null && contextClass.getClassLoader() == loader && privateLookupInMethod != null
                        && lookupDefineClassMethod != null) {
            try {
                // 使用privateLookupIn获取对contextClass的私有访问权限
                MethodHandles.Lookup lookup = (MethodHandles.Lookup) privateLookupInMethod.invoke(null, contextClass,
                                MethodHandles.lookup());
                // 使用Lookup.defineClass定义类
                c = (Class) lookupDefineClassMethod.invoke(lookup, b);
            } catch (InvocationTargetException ex) {
                Throwable target = ex.getTargetException();
                // 如果是LinkageError（类已定义）或IllegalArgumentException（类在不同包中），
                // 继续尝试下面的方案
                if (target.getClass() != LinkageError.class && target.getClass() != IllegalArgumentException.class) {
                    throw new ReflectException(target);
                }
                // LinkageError（类已定义）或IllegalArgumentException（类在不同包）：
                // 继续尝试下面传统的ClassLoader.defineClass方案
            } catch (Throwable ex) {
                throw new ReflectException(ex);
            }
        }

        // ============ 方案3：传统的ClassLoader.defineClass方法 ============
        // 这是传统的类定义方式，通过反射调用protected方法
        if (c == null && classLoaderDefineClassMethod != null) {
            if (protectionDomain == null) {
                protectionDomain = PROTECTION_DOMAIN;
            }
            Object[] args = new Object[] { className, b, 0, b.length, protectionDomain };
            try {
                // 如果方法不可访问，设置为可访问
                if (!classLoaderDefineClassMethod.isAccessible()) {
                    classLoaderDefineClassMethod.setAccessible(true);
                }
                // 通过反射调用ClassLoader.defineClass方法
                c = (Class) classLoaderDefineClassMethod.invoke(loader, args);
            } catch (InvocationTargetException ex) {
                throw new ReflectException(ex.getTargetException());
            } catch (Throwable ex) {
                // 如果setAccessible失败并抛出InaccessibleObjectException，则忽略
                // 这种情况发生在JDK 9+的模块路径上或使用--illegal-access=deny启动时
                if (!ex.getClass().getName().endsWith("InaccessibleObjectException")) {
                    throw new ReflectException(ex);
                }
            }
        }

        // ============ 方案4：JDK 9+ Lookup.defineClass API（类加载器不匹配）============
        // 这是后备方案，即使类加载器不匹配也尝试使用Lookup.defineClass
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

        // ============ 所有方案都失败 ============
        if (c == null) {
            throw new ReflectException(THROWABLE);
        }

        // 强制执行类的静态初始化块
        Class.forName(className, true, loader);
        return c;
    }
    // ============ Spring框架增强部分结束 ============

    /**
     * 查找第一个非public类
     * <p>
     * 在类数组中查找第一个非public修饰的类。
     * 用于确定哪些类需要特殊的访问处理。
     * </p>
     *
     * @param classes 类数组
     * @return 第一个非public类的索引，如果所有类都是public则返回0
     */
    public static int findPackageProtected(Class[] classes) {
        for (int i = 0; i < classes.length; i++) {
            if (!Modifier.isPublic(classes[i].getModifiers())) {
                return i;
            }
        }
        return 0;
    }

}
