package com.taobao.arthas.compiler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

/**
 * 动态Java文件管理器
 *
 * <p>该类继承自ForwardingJavaFileManager，用于管理Java编译过程中的文件操作。
 * 它是内存编译器的核心组件之一，负责：</p>
 *
 * <ul>
 *   <li>将编译后的字节码存储在内存中，而非写入磁盘</li>
 *   <li>支持从自定义类加载器中查找类文件</li>
 *   <li>管理编译输出，使其可以直接在内存中使用</li>
 *   <li>合并标准类路径和自定义类加载器的类文件</li>
 * </ul>
 *
 * <p>这个实现使得Java编译器可以将编译结果直接输出到内存，
 * 从而实现动态编译和立即加载执行的功能。</p>
 */
public class DynamicJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {

    /**
     * 超级位置名称数组
     *
     * <p>定义了需要委托给父类FileManager处理的标准位置。
     * 包括：</p>
     * <ul>
     *   <li>PLATFORM_CLASS_PATH: 平台类路径</li>
     *   <li>SYSTEM_MODULES: JPMS（Java Platform Module System）系统模块位置</li>
     * </ul>
     */
    private static final String[] superLocationNames = { StandardLocation.PLATFORM_CLASS_PATH.name(),
            /** JPMS StandardLocation.SYSTEM_MODULES **/
            "SYSTEM_MODULES" };

    /**
     * 包内部查找器
     *
     * <p>用于从自定义类加载器中查找类文件和包内容。
     * 这个查找器能够扫描类加载器可访问的所有类文件。</p>
     */
    private final PackageInternalsFinder finder;

    /**
     * 动态类加载器
     *
     * <p>用于加载编译生成的类。这个类加载器维护了
     * 编译过程中生成的所有类的字节码。</p>
     */
    private final DynamicClassLoader classLoader;

    /**
     * 内存字节码列表
     *
     * <p>存储编译过程中生成的所有类的字节码对象。
     * 每个MemoryByteCode对象包含一个类的编译结果。</p>
     */
    private final List<MemoryByteCode> byteCodes = new ArrayList<MemoryByteCode>();

    /**
     * 构造函数 - 创建动态Java文件管理器
     *
     * @param fileManager 底层的Java文件管理器，用于标准文件操作
     * @param classLoader 动态类加载器，用于加载编译生成的类
     */
    public DynamicJavaFileManager(JavaFileManager fileManager, DynamicClassLoader classLoader) {
        super(fileManager);
        this.classLoader = classLoader;
        // 初始化包内部查找器，用于从类加载器中查找类
        this.finder = new PackageInternalsFinder(classLoader);
    }

    /**
     * 获取Java文件的输出对象
     *
     * <p>当编译器需要输出编译结果时调用此方法。
     * 该方法会创建或返回一个MemoryByteCode对象来接收字节码，
     * 而不是将其写入磁盘文件。</p>
     *
     * @param location 输出位置
     * @param className 要输出的类的全限定名
     * @param kind 文件类型（通常是CLASS）
     * @param sibling 相关的文件对象（通常为null）
     * @return Java文件对象，用于接收编译输出的字节码
     * @throws IOException 如果发生I/O错误
     */
    @Override
    public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String className,
                    JavaFileObject.Kind kind, FileObject sibling) throws IOException {

        // 检查是否已经为该类创建了字节码对象
        for (MemoryByteCode byteCode : byteCodes) {
            if (byteCode.getClassName().equals(className)) {
                return byteCode;
            }
        }

        // 创建新的内存字节码对象
        MemoryByteCode innerClass = new MemoryByteCode(className);
        byteCodes.add(innerClass);
        // 将编译结果注册到类加载器中，使其可以被加载使用
        classLoader.registerCompiledSource(innerClass);
        return innerClass;

    }

    /**
     * 获取类加载器
     *
     * <p>返回用于加载编译类的类加载器。
     * 这样编译器就可以使用自定义的类加载器来解析类依赖。</p>
     *
     * @param Location 位置（通常未使用）
     * @return 动态类加载器实例
     */
    @Override
    public ClassLoader getClassLoader(JavaFileManager.Location location) {
        return classLoader;
    }

    /**
     * 推断Java文件对象的二进制名称
     *
     * <p>根据文件对象推断其对应的类的全限定名。
     * 对于自定义的Java文件对象，直接返回其类名；
     * 对于标准文件对象，委托给父类处理。</p>
     *
     * @param location 文件位置
     * @param file Java文件对象
     * @return 类的二进制名称（全限定名）
     */
    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        // 如果是自定义的Java文件对象，直接返回其类名
        if (file instanceof CustomJavaFileObject) {
            return ((CustomJavaFileObject) file).getClassName();
        } else {
            /**
             * 如果不是CustomJavaFileObject，则来自标准文件管理器
             * 让它处理该文件
             */
            return super.inferBinaryName(location, file);
        }
    }

    /**
     * 列出指定位置和包名下的所有Java文件对象
     *
     * <p>该方法支持从多个来源查找类文件：
     * <ul>
     *   <li>对于标准位置（PLATFORM_CLASS_PATH、SYSTEM_MODULES），委托给父类处理</li>
     *   <li>对于CLASS_PATH位置，合并标准类路径和自定义类加载器的类文件</li>
     *   <li>对于其他位置，委托给父类处理</li>
     * </ul>
     * </p>
     *
     * @param location 要搜索的位置（如CLASS_PATH）
     * @param packageName 包名（如"com.example"）
     * @param kinds 要查找的文件类型集合
     * @param recurse 是否递归搜索子包
     * @return 可迭代的Java文件对象集合
     * @throws IOException 如果发生I/O错误
     */
    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds,
                                         boolean recurse) throws IOException {
        // 检查是否是标准位置
        if (location instanceof StandardLocation) {
            String locationName = ((StandardLocation) location).name();
            // 检查是否是需要委托给父类处理的位置
            for (String name : superLocationNames) {
                if (name.equals(locationName)) {
                    return super.list(location, packageName, kinds, recurse);
                }
            }
        }

        // 合并来自指定类加载器的Java文件对象
        if (location == StandardLocation.CLASS_PATH && kinds.contains(JavaFileObject.Kind.CLASS)) {
            // 合并父类查找结果和自定义查找器的结果
            return new IterableJoin<JavaFileObject>(super.list(location, packageName, kinds, recurse),
                    finder.find(packageName));
        }

        // 其他情况委托给父类处理
        return super.list(location, packageName, kinds, recurse);
    }

    /**
     * 可迭代对象连接器
     *
     * <p>将两个可迭代对象连接在一起，作为一个统一的可迭代对象使用。
     * 这在需要合并来自不同源的类文件时非常有用。</p>
     *
     * @param <T> 可迭代对象的元素类型
     */
    static class IterableJoin<T> implements Iterable<T> {
        /**
         * 第一个可迭代对象
         */
        private final Iterable<T> first;

        /**
         * 第二个可迭代对象
         */
        private final Iterable<T> next;

        /**
         * 构造函数 - 创建可迭代对象连接器
         *
         * @param first 第一个可迭代对象
         * @param next 第二个可迭代对象
         */
        public IterableJoin(Iterable<T> first, Iterable<T> next) {
            this.first = first;
            this.next = next;
        }

        /**
         * 获取迭代器
         *
         * <p>返回一个能够依次迭代两个可迭代对象的迭代器。</p>
         *
         * @return 连接后的迭代器
         */
        @Override
        public Iterator<T> iterator() {
            return new IteratorJoin<T>(first.iterator(), next.iterator());
        }
    }

    /**
     * 迭代器连接器
     *
     * <p>将两个迭代器连接在一起，作为一个统一的迭代器使用。
     * 首先遍历完第一个迭代器的所有元素，然后遍历第二个迭代器。</p>
     *
     * @param <T> 迭代器的元素类型
     */
    static class IteratorJoin<T> implements Iterator<T> {
        /**
         * 第一个迭代器
         */
        private final Iterator<T> first;

        /**
         * 第二个迭代器
         */
        private final Iterator<T> next;

        /**
         * 构造函数 - 创建迭代器连接器
         *
         * @param first 第一个迭代器
         * @param next 第二个迭代器
         */
        public IteratorJoin(Iterator<T> first, Iterator<T> next) {
            this.first = first;
            this.next = next;
        }

        /**
         * 检查是否还有下一个元素
         *
         * <p>只要两个迭代器中任何一个还有元素，就返回true。</p>
         *
         * @return 如果还有更多元素则返回true，否则返回false
         */
        @Override
        public boolean hasNext() {
            return first.hasNext() || next.hasNext();
        }

        /**
         * 获取下一个元素
         *
         * <p>优先从第一个迭代器获取元素，只有在第一个迭代器耗尽后
         * 才从第二个迭代器获取元素。</p>
         *
         * @return 下一个元素
         */
        @Override
        public T next() {
            // 优先从第一个迭代器获取
            if (first.hasNext()) {
                return first.next();
            }
            // 第一个迭代器耗尽，从第二个迭代器获取
            return next.next();
        }

        /**
         * 移除当前元素
         *
         * <p>不支持此操作，抛出UnsupportedOperationException。</p>
         *
         * @throws UnsupportedOperationException 始终抛出此异常
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }
    }
}
