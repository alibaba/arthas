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
 * 反射工具类 Created by vlinux on 15/5/18.
 */
public class ArthasReflectUtils {

    /**
     * 从包package中获取所有的Class
     *
     * @param packname 包名称
     * @return 包路径下所有类集合
     * <p>
     * 代码摘抄自 http://www.oschina.net/code/snippet_129830_8767</p>
     */
    public static Set<Class<?>> getClasses(final ClassLoader loader, final String packname) {

        // 第一个class类的集合
        Set<Class<?>> classes = new LinkedHashSet<Class<?>>();
        // 是否循环迭代
        // 获取包的名字 并进行替换
        String packageName = packname;
        String packageDirName = packageName.replace('.', '/');
        // 定义一个枚举的集合 并进行循环来处理这个目录下的things
        Enumeration<URL> dirs;
        try {
//            dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
            dirs = loader.getResources(packageDirName);
            // 循环迭代下去
            while (dirs.hasMoreElements()) {
                // 获取下一个元素
                URL url = dirs.nextElement();
                // 得到协议的名称
                String protocol = url.getProtocol();
                // 如果是以文件的形式保存在服务器上
                if ("file".equals(protocol)) {
//					System.err.println("file类型的扫描");
                    // 获取包的物理路径
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    // 以文件的方式扫描整个包下的文件 并添加到集合中
                    findAndAddClassesInPackageByFile(packageName, filePath,
                            true, classes);
                } else if ("jar".equals(protocol)) {
                    // 如果是jar包文件
                    // 定义一个JarFile
//					System.err.println("jar类型的扫描");
                    JarFile jar;
                    try {
                        // 获取jar
                        jar = ((JarURLConnection) url.openConnection())
                                .getJarFile();
                        // 从此jar包 得到一个枚举类
                        Enumeration<JarEntry> entries = jar.entries();
                        // 同样的进行循环迭代
                        while (entries.hasMoreElements()) {
                            // 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
                            JarEntry entry = entries.nextElement();
                            String name = entry.getName();
                            // 如果是以/开头的
                            if (name.charAt(0) == '/') {
                                // 获取后面的字符串
                                name = name.substring(1);
                            }
                            // 如果前半部分和定义的包名相同
                            if (name.startsWith(packageDirName)) {
                                int idx = name.lastIndexOf('/');
                                // 如果以"/"结尾 是一个包
                                if (idx != -1) {
                                    // 获取包名 把"/"替换成"."
                                    packageName = name.substring(0, idx)
                                            .replace('/', '.');
                                }
                                // 如果是一个.class文件 而且不是目录
                                if (name.endsWith(".class") && !entry.isDirectory()) {
                                    // 去掉后面的".class" 获取真正的类名
                                    String className = name.substring(
                                            packageName.length() + 1,
                                            name.length() - 6);
                                    try {
                                        // 添加到classes
                                        classes.add(Class
                                                .forName(packageName + '.'
                                                        + className));
                                    } catch (ClassNotFoundException e) {
                                        // log
                                        // .error("添加用户自定义视图类错误 找不到此类的.class文件");
//											e.printStackTrace();
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        // log.error("在扫描用户定义视图时从jar包获取文件出错");
//						e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
//			e.printStackTrace();
        }

        return classes;
    }

    /**
     * 以文件的形式来获取包下的所有Class
     * <p/>
     * <p>
     * 代码摘抄自 http://www.oschina.net/code/snippet_129830_8767</p>
     */
    private static void findAndAddClassesInPackageByFile(String packageName,
                                                         String packagePath, final boolean recursive, Set<Class<?>> classes) {
        // 获取此包的目录 建立一个File
        File dir = new File(packagePath);
        // 如果不存在或者 也不是目录就直接返回
        if (!dir.exists() || !dir.isDirectory()) {
            // log.warn("用户定义包名 " + packageName + " 下没有任何文件");
            return;
        }
        // 如果存在 就获取包下的所有文件 包括目录
        File[] dirfiles = dir.listFiles(new FileFilter() {
            // 自定义过滤规则 如果可以循环(包含子目录) 或则是以.class结尾的文件(编译好的java类文件)
            @Override
            public boolean accept(File file) {
                return (recursive && file.isDirectory())
                        || (file.getName().endsWith(".class"));
            }
        });
        if (dirfiles != null) {
            // 循环所有文件
            for (File file : dirfiles) {
                // 如果是目录 则继续扫描
                if (file.isDirectory()) {
                    findAndAddClassesInPackageByFile(
                            packageName + "." + file.getName(),
                            file.getAbsolutePath(), recursive, classes);
                } else {
                    // 如果是java类文件 去掉后面的.class 只留下类名
                    String className = file.getName().substring(0,
                            file.getName().length() - 6);
                    try {
                        // 添加到集合中去
                        // classes.add(Class.forName(packageName + '.' +
                        // className));
                        // 经过回复同学的提醒，这里用forName有一些不好，会触发static方法，没有使用classLoader的load干净
                        classes.add(Thread.currentThread().getContextClassLoader()
                                .loadClass(packageName + '.' + className));
                    } catch (ClassNotFoundException e) {
                        // log.error("添加用户自定义视图类错误 找不到此类的.class文件");
                        //                    e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 设置对象某个成员的值
     *
     * @param field  属性对象
     * @param value  属性值
     * @param target 目标对象
     * @throws IllegalArgumentException 非法参数
     * @throws IllegalAccessException   非法进入
     */
    public static void set(Field field, Object value, Object target) throws IllegalArgumentException, IllegalAccessException {
        final boolean isAccessible = field.isAccessible();
        try {
            field.setAccessible(true);
            field.set(target, value);
        } finally {
            field.setAccessible(isAccessible);
        }
    }

    /**
     * 获取一个类下的所有成员(包括父类、私有成员)
     *
     * @param clazz 目标类
     * @return 类下所有属性
     */
    public static Set<Field> getFields(Class<?> clazz) {
        final Set<Field> fields = new LinkedHashSet<Field>();
        final Class<?> parentClazz = clazz.getSuperclass();
        Collections.addAll(fields, clazz.getDeclaredFields());
        if (null != parentClazz) {
            fields.addAll(getFields(parentClazz));
        }
        return fields;
    }

    /**
     * 获取一个类下的指定成员
     *
     * @param clazz 目标类
     * @param name  属性名
     * @return 属性
     */
    public static Field getField(Class<?> clazz, String name) {
        for (Field field : getFields(clazz)) {
            if (ArthasCheckUtils.isEquals(field.getName(), name)) {
                return field;
            }
        }//for
        return null;
    }

    /**
     * 获取对象某个成员的值
     *
     * @param <T>
     * @param target 目标对象
     * @param field  目标属性
     * @return 目标属性值
     * @throws IllegalArgumentException 非法参数
     * @throws IllegalAccessException   非法进入
     */
    public static <T> T getFieldValueByField(Object target, Field field) throws IllegalArgumentException, IllegalAccessException {
        final boolean isAccessible = field.isAccessible();
        try {
            field.setAccessible(true);
            //noinspection unchecked
            return (T) field.get(target);
        } finally {
            field.setAccessible(isAccessible);
        }
    }

    /**
     * 将字符串转换为指定类型，目前只支持9种类型：8种基本类型（包括其包装类）以及字符串
     *
     * @param t     目标对象类型
     * @param value 目标值
     * @return 类型转换后的值
     */
    @SuppressWarnings("unchecked")
    public static <T> T valueOf(Class<T> t, String value) {
        if (ArthasCheckUtils.isIn(t, int.class, Integer.class)) {
            return (T) Integer.valueOf(value);
        } else if (ArthasCheckUtils.isIn(t, long.class, Long.class)) {
            return (T) Long.valueOf(value);
        } else if (ArthasCheckUtils.isIn(t, double.class, Double.class)) {
            return (T) Double.valueOf(value);
        } else if (ArthasCheckUtils.isIn(t, float.class, Float.class)) {
            return (T) Float.valueOf(value);
        } else if (ArthasCheckUtils.isIn(t, char.class, Character.class)) {
            return (T) Character.valueOf(value.charAt(0));
        } else if (ArthasCheckUtils.isIn(t, byte.class, Byte.class)) {
            return (T) Byte.valueOf(value);
        } else if (ArthasCheckUtils.isIn(t, boolean.class, Boolean.class)) {
            return (T) Boolean.valueOf(value);
        } else if (ArthasCheckUtils.isIn(t, short.class, Short.class)) {
            return (T) Short.valueOf(value);
        } else if (ArthasCheckUtils.isIn(t, String.class)) {
            return (T) value;
        } else {
            return null;
        }
    }


    /**
     * 定义类
     *
     * @param targetClassLoader 目标classloader
     * @param className         类名称
     * @param classByteArray    类字节码数组
     * @return 定义的类
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public static Class<?> defineClass(
            final ClassLoader targetClassLoader,
            final String className,
            final byte[] classByteArray) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        final Method defineClassMethod = ClassLoader.class.getDeclaredMethod(
                "defineClass",
                String.class,
                byte[].class,
                int.class,
                int.class
        );

        synchronized (defineClassMethod) {
            final boolean acc = defineClassMethod.isAccessible();
            try {
                defineClassMethod.setAccessible(true);
                return (Class<?>) defineClassMethod.invoke(
                        targetClassLoader,
                        className,
                        classByteArray,
                        0,
                        classByteArray.length
                );
            } finally {
                defineClassMethod.setAccessible(acc);
            }
        }


    }

}
