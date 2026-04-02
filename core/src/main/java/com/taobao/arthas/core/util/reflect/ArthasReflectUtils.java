package com.taobao.arthas.core.util.reflect;

import com.taobao.arthas.core.util.ArthasCheckUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Arthas 反射工具类
 * 提供丰富的反射操作功能，包括：
 * 1. 扫描指定包路径下的所有类
 * 2. 获取类的所有字段（包括父类）
 * 3. 设置和获取字段的值
 * 4. 类型转换
 * 5. 动态定义类
 *
 * Created by vlinux on 15/5/18.
 */
public class ArthasReflectUtils {

    /**
     * 从指定包中获取所有的 Class
     * 该方法会扫描指定包路径下的所有类，包括文件系统和 JAR 包中的类
     *
     * @param loader 类加载器，用于加载类
     * @param packname 包名称，格式如 "com.taobao.arthas"
     * @return 包路径下所有类的集合，使用 LinkedHashSet 保持插入顺序
     * <p>
     * 代码摘抄自 http://www.oschina.net/code/snippet_129830_8767</p>
     */
    public static Set<Class<?>> getClasses(final ClassLoader loader, final String packname) {

        // 创建类集合，使用 LinkedHashSet 保持插入顺序
        Set<Class<?>> classes = new LinkedHashSet<Class<?>>();

        // 保存包名，后续会根据 JAR 包中的路径动态更新
        String packageName = packname;

        // 将包名中的点号替换为路径分隔符，例如 "com.example" -> "com/example"
        String packageDirName = packageName.replace('.', '/');

        // 定义枚举集合，用于存储所有匹配的 URL 资源
        Enumeration<URL> dirs;

        try {
            // 使用指定的类加载器获取包路径下的所有资源
            dirs = loader.getResources(packageDirName);

            // 循环遍历所有找到的资源
            while (dirs.hasMoreElements()) {
                // 获取下一个资源 URL
                URL url = dirs.nextElement();

                // 获取 URL 的协议类型（file、jar 等）
                String protocol = url.getProtocol();

                // 如果协议是 file，表示类在文件系统中
                if ("file".equals(protocol)) {
                    // 获取包的物理路径，并进行 URL 解码（处理中文等特殊字符）
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");

                    // 以文件的方式扫描整个包下的文件，并添加到集合中
                    findAndAddClassesInPackageByFile(packageName, filePath,
                            true, classes);

                // 如果协议是 jar，表示类在 JAR 包中
                } else if ("jar".equals(protocol)) {
                    JarFile jar;
                    try {
                        // 打开 JAR 包连接并获取 JarFile 对象
                        jar = ((JarURLConnection) url.openConnection())
                                .getJarFile();

                        // 获取 JAR 包中所有条目的枚举
                        Enumeration<JarEntry> entries = jar.entries();

                        // 遍历 JAR 包中的每个条目
                        while (entries.hasMoreElements()) {
                            // 获取 JAR 中的实体，可能是目录、class 文件或其他文件（如 META-INF 等）
                            JarEntry entry = entries.nextElement();
                            String name = entry.getName();

                            // 如果路径以 / 开头，去掉开头的 /
                            if (name.charAt(0) == '/') {
                                // 获取去掉开头的 / 后的字符串
                                name = name.substring(1);
                            }

                            // 检查条目路径是否以目标包路径开头
                            if (name.startsWith(packageDirName)) {
                                // 查找最后一个 / 的位置
                                int idx = name.lastIndexOf('/');

                                // 如果找到了 /，说明有包路径
                                if (idx != -1) {
                                    // 提取包名，并将 / 替换为 .
                                    packageName = name.substring(0, idx)
                                            .replace('/', '.');
                                }

                                // 检查是否是 .class 文件且不是目录
                                if (name.endsWith(".class") && !entry.isDirectory()) {
                                    // 去掉包名前缀和 .class 后缀，获取真正的类名
                                    String className = name.substring(
                                            packageName.length() + 1,
                                            name.length() - 6);

                                    try {
                                        // 加载类并添加到集合中
                                        // 使用 Class.forName 会初始化类（执行 static 块）
                                        classes.add(Class
                                                .forName(packageName + '.'
                                                        + className));
                                    } catch (ClassNotFoundException e) {
                                        // 类加载失败，忽略该类
                                        // 可能是由于依赖缺失或其他原因
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        // JAR 包读取失败，忽略该异常
                    }
                }
            }
        } catch (IOException e) {
            // 资源获取失败，忽略该异常
        }

        // 返回找到的所有类
        return classes;
    }

    /**
     * 以文件的形式来获取包下的所有 Class
     * 递归扫描指定目录下的所有 .class 文件并加载为 Class 对象
     *
     * @param packageName 包名，格式如 "com.taobao.arthas"
     * @param packagePath 包的物理路径，格式如 "/path/to/com/taobao/arthas"
     * @param recursive 是否递归扫描子目录
     * @param classes 用于存储找到的类的集合
     * <p/>
     * <p>
     * 代码摘抄自 http://www.oschina.net/code/snippet_129830_8767</p>
     */
    private static void findAndAddClassesInPackageByFile(String packageName,
                                                         String packagePath, final boolean recursive, Set<Class<?>> classes) {
        // 根据物理路径创建 File 对象
        File dir = new File(packagePath);

        // 检查目录是否存在或是否为目录
        if (!dir.exists() || !dir.isDirectory()) {
            // 目录不存在或不是目录，直接返回
            return;
        }

        // 获取目录下的所有文件，使用自定义过滤器
        File[] dirfiles = dir.listFiles(new FileFilter() {
            // 自定义过滤规则：如果是递归模式，包含所有目录；同时包含所有 .class 文件
            @Override
            public boolean accept(File file) {
                // 递归模式下接受目录，或者接受 .class 文件
                return (recursive && file.isDirectory())
                        || (file.getName().endsWith(".class"));
            }
        });

        // 如果找到了文件
        if (dirfiles != null) {
            // 遍历所有文件
            for (File file : dirfiles) {
                // 如果是目录，递归扫描子目录
                if (file.isDirectory()) {
                    // 递归调用，包名加上当前目录名，路径使用绝对路径
                    findAndAddClassesInPackageByFile(
                            packageName + "." + file.getName(),
                            file.getAbsolutePath(), recursive, classes);
                } else {
                    // 如果是 .class 文件，去掉 .class 后缀，只保留类名
                    String className = file.getName().substring(0,
                            file.getName().length() - 6);

                    try {
                        // 使用类加载器加载类
                        // 注意：这里使用 ClassLoader.loadClass() 而不是 Class.forName()
                        // 原因：loadClass 不会触发类的初始化（不执行 static 块），
                        //       而 forName 会初始化类，可能产生副作用
                        classes.add(Thread.currentThread().getContextClassLoader()
                                .loadClass(packageName + '.' + className));
                    } catch (ClassNotFoundException e) {
                        // 类加载失败，忽略该异常
                    }
                }
            }
        }
    }

    /**
     * 设置对象某个成员的值
     * 该方法会临时修改字段的可访问性，设置完成后恢复原始状态
     * 可以设置私有字段的值
     *
     * @param field  字段对象，通过反射获取
     * @param value  要设置的属性值
     * @param target 目标对象，该字段所属的对象实例
     * @throws IllegalArgumentException 如果类型不匹配或其他参数非法
     * @throws IllegalAccessException   如果访问被拒绝（理论上不会发生，因为已经设置了 accessible）
     */
    public static void set(Field field, Object value, Object target) throws IllegalArgumentException, IllegalAccessException {
        // 保存字段原来的可访问性状态
        final boolean isAccessible = field.isAccessible();

        try {
            // 临时设置字段为可访问，以突破 private/protected 限制
            field.setAccessible(true);

            // 设置字段值
            field.set(target, value);
        } finally {
            // 无论成功还是失败，都要恢复字段原始的可访问性状态
            field.setAccessible(isAccessible);
        }
    }

    /**
     * 获取一个类下的所有成员（包括父类、私有成员）
     * 该方法会递归获取类及其所有父类的字段
     *
     * @param clazz 目标类
     * @return 类下所有字段的集合，使用 LinkedHashSet 保持声明顺序
     */
    public static Set<Field> getFields(Class<?> clazz) {
        // 创建字段集合，使用 LinkedHashSet 保持字段的声明顺序
        final Set<Field> fields = new LinkedHashSet<Field>();

        // 获取父类的 Class 对象
        final Class<?> parentClazz = clazz.getSuperclass();

        // 获取当前类声明的所有字段（包括 private 字段）
        Collections.addAll(fields, clazz.getDeclaredFields());

        // 如果存在父类，递归获取父类的字段
        if (null != parentClazz) {
            fields.addAll(getFields(parentClazz));
        }

        // 返回包含所有字段的集合
        return fields;
    }

    /**
     * 获取一个类下的指定成员（字段）
     * 该方法会搜索类及其所有父类的字段
     *
     * @param clazz 目标类
     * @param name  属性名（字段名）
     * @return 找到的字段对象，如果未找到则返回 null
     */
    public static Field getField(Class<?> clazz, String name) {
        // 遍历类及其父类的所有字段
        for (Field field : getFields(clazz)) {
            // 使用工具类比较字段名是否相等
            if (ArthasCheckUtils.isEquals(field.getName(), name)) {
                // 找到匹配的字段，返回
                return field;
            }
        }//for

        // 未找到匹配的字段，返回 null
        return null;
    }

    /**
     * 获取对象某个成员的值
     * 该方法会临时修改字段的可访问性，获取完成后恢复原始状态
     * 可以获取私有字段的值
     *
     * @param <T> 返回值的泛型类型
     * @param target 目标对象，该字段所属的对象实例
     * @param field  目标字段对象
     * @return 目标字段的值
     * @throws IllegalArgumentException 如果目标对象不是该字段所属的类的实例
     * @throws IllegalAccessException   如果访问被拒绝（理论上不会发生，因为已经设置了 accessible）
     */
    public static <T> T getFieldValueByField(Object target, Field field) throws IllegalArgumentException, IllegalAccessException {
        // 保存字段原来的可访问性状态
        final boolean isAccessible = field.isAccessible();

        try {
            // 临时设置字段为可访问，以突破 private/protected 限制
            field.setAccessible(true);

            // 获取字段值并进行类型转换
            //noinspection unchecked
            return (T) field.get(target);
        } finally {
            // 无论成功还是失败，都要恢复字段原始的可访问性状态
            field.setAccessible(isAccessible);
        }
    }

    /**
     * 将字符串转换为指定类型
     * 目前支持 9 种类型：8 种基本类型（包括其包装类）以及字符串类型
     * 该方法常用于将配置或用户输入的字符串值转换为所需的类型
     *
     * @param <T> 目标类型的泛型参数
     * @param t     目标对象类型的 Class 对象
     * @param value 要转换的字符串值
     * @return 类型转换后的值，如果类型不支持则返回 null
     */
    @SuppressWarnings("unchecked")
    public static <T> T valueOf(Class<T> t, String value) {
        // 检查是否是 int 或 Integer 类型
        if (ArthasCheckUtils.isIn(t, int.class, Integer.class)) {
            return (T) Integer.valueOf(value);

        // 检查是否是 long 或 Long 类型
        } else if (ArthasCheckUtils.isIn(t, long.class, Long.class)) {
            return (T) Long.valueOf(value);

        // 检查是否是 double 或 Double 类型
        } else if (ArthasCheckUtils.isIn(t, double.class, Double.class)) {
            return (T) Double.valueOf(value);

        // 检查是否是 float 或 Float 类型
        } else if (ArthasCheckUtils.isIn(t, float.class, Float.class)) {
            return (T) Float.valueOf(value);

        // 检查是否是 char 或 Character 类型
        } else if (ArthasCheckUtils.isIn(t, char.class, Character.class)) {
            // 取字符串的第一个字符
            return (T) Character.valueOf(value.charAt(0));

        // 检查是否是 byte 或 Byte 类型
        } else if (ArthasCheckUtils.isIn(t, byte.class, Byte.class)) {
            return (T) Byte.valueOf(value);

        // 检查是否是 boolean 或 Boolean 类型
        } else if (ArthasCheckUtils.isIn(t, boolean.class, Boolean.class)) {
            return (T) Boolean.valueOf(value);

        // 检查是否是 short 或 Short 类型
        } else if (ArthasCheckUtils.isIn(t, short.class, Short.class)) {
            return (T) Short.valueOf(value);

        // 检查是否是 String 类型
        } else if (ArthasCheckUtils.isIn(t, String.class)) {
            return (T) value;

        // 不支持的类型，返回 null
        } else {
            return null;
        }
    }


    /**
     * 动态定义类
     * 通过反射调用 ClassLoader 的 defineClass 方法，将字节码数组动态定义为类
     * 这是一种绕过正常类加载机制的方式，可以动态创建类
     *
     * @param targetClassLoader 目标类加载器，用于定义类
     * @param className         要定义的类的全限定名
     * @param classByteArray    类的字节码数组
     * @return 定义的 Class 对象
     * @throws NoSuchMethodException     如果找不到 defineClass 方法
     * @throws InvocationTargetException 如果调用目标方法时抛出异常
     * @throws IllegalAccessException    如果访问被拒绝（理论上不会发生，因为已经设置了 accessible）
     */
    public static Class<?> defineClass(
            final ClassLoader targetClassLoader,
            final String className,
            final byte[] classByteArray) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        // 获取 ClassLoader 的 defineClass 方法
        // 该方法是 protected 的，需要通过反射获取
        final Method defineClassMethod = ClassLoader.class.getDeclaredMethod(
                "defineClass",        // 方法名
                String.class,         // 参数1：类名
                byte[].class,         // 参数2：字节码数组
                int.class,            // 参数3：偏移量
                int.class             // 参数4：长度
        );

        // 使用 synchronized 保证线程安全
        synchronized (defineClassMethod) {
            // 保存方法原来的可访问性状态
            final boolean acc = defineClassMethod.isAccessible();

            try {
                // 设置方法为可访问
                defineClassMethod.setAccessible(true);

                // 调用 defineClass 方法，传入目标类加载器和参数
                return (Class<?>) defineClassMethod.invoke(
                        targetClassLoader,  // 目标类加载器
                        className,          // 类名
                        classByteArray,     // 字节码数组
                        0,                  // 从字节码数组的起始位置开始
                        classByteArray.length // 使用整个字节码数组
                );
            } finally {
                // 无论成功还是失败，都要恢复方法原始的可访问性状态
                defineClassMethod.setAccessible(acc);
            }
        }

    }

}
