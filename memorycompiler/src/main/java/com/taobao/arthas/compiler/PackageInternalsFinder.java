package com.taobao.arthas.compiler;

/*-
 * #%L
 * compiler
 * %%
 * Copyright (C) 2017 - 2018 SkaLogs
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import javax.tools.JavaFileObject;
import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.stream.Collectors;

/**
 * 包内部查找器
 *
 * <p>该类用于在类加载器的类路径中查找指定包下的所有Java类文件。
 * 它是动态编译器的重要组件，负责定位和提供编译所需的依赖类。</p>
 *
 * <p>主要功能：
 * <ul>
 *   <li>从类加载器的类路径中查找指定包的所有类</li>
 *   <li>支持从JAR文件中读取类文件</li>
 *   <li>支持从本地文件系统目录中读取类文件</li>
 *   <li>维护JAR文件索引，提高查找效率</li>
 *   <li>提供标准化的JavaFileObject接口</li>
 * </ul>
 * </p>
 *
 * <p>应用场景：
 * <ul>
 *   <li>动态编译时的依赖类解析</li>
 *   <li>类路径扫描和类发现</li>
 *   <li>插件系统中的类加载</li>
 *   <li>反射和元数据提取</li>
 * </ul>
 * </p>
 */
public class PackageInternalsFinder {

    /**
     * 类加载器
     *
     * <p>用于查找和加载类文件的类加载器。
     * 这个类加载器决定了查找器的搜索范围。</p>
     */
    private final ClassLoader classLoader;

    /**
     * 类文件扩展名
     *
     * <p>Java编译后的类文件使用".class"作为扩展名。</p>
     */
    private static final String CLASS_FILE_EXTENSION = ".class";

    /**
     * JAR文件索引缓存
     *
     * <p>使用ConcurrentHashMap缓存已扫描的JAR文件索引。
     * 键为JAR文件的URI，值为对应的JarFileIndex对象。
     * 这个缓存可以显著提高重复查找的性能。</p>
     */
    private static final Map<String, JarFileIndex> INDEXS = new ConcurrentHashMap<>();

    /**
     * 构造函数 - 创建包内部查找器
     *
     * @param classLoader 用于查找类文件的类加载器
     */
    public PackageInternalsFinder(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * 查找指定包下的所有Java类文件
     *
     * <p>这个方法会在类加载器的类路径中搜索指定包的所有类文件。
     * 搜索范围包括：
     * <ul>
     *   <li>JAR文件中的类</li>
     *   <li>文件系统目录中的类</li>
     * </ul>
     * </p>
     *
     * @param packageName 要查找的包名（如"com.example"）
     * @return 该包下所有类文件的JavaFileObject列表
     * @throws IOException 如果发生I/O错误
     */
    public List<JavaFileObject> find(String packageName) throws IOException {
        // 将包名转换为路径格式（如"com/example"）
        String javaPackageName = packageName.replaceAll("\\.", "/");

        List<JavaFileObject> result = new ArrayList<JavaFileObject>();

        // 获取类加载器中该包对应的所有URL
        // 每个URL代表类路径中包含该包的一个位置（JAR或目录）
        Enumeration<URL> urlEnumeration = classLoader.getResources(javaPackageName);
        while (urlEnumeration.hasMoreElements()) { // 类路径中包含该包的每个JAR对应一个URL
            URL packageFolderURL = urlEnumeration.nextElement();
            // 处理该URL下的所有类文件，并添加到结果列表
            result.addAll(listUnder(packageName, packageFolderURL));
        }

        return result;
    }

    /**
     * 列出指定URL下的所有Java类文件
     *
     * <p>根据URL的类型（目录或JAR文件）使用不同的处理策略：
     * <ul>
     *   <li>如果是目录，调用processDir方法</li>
     *   <li>如果是JAR文件，调用processJar方法</li>
     * </ul>
     * </p>
     *
     * @param packageName 包名
     * @param packageFolderURL 包对应的URL
     * @return 该位置下所有类文件的集合
     */
    private Collection<JavaFileObject> listUnder(String packageName, URL packageFolderURL) {
        // 解码URL路径并创建File对象
        File directory = new File(decode(packageFolderURL.getFile()));
        if (directory.isDirectory()) {
            // 浏览本地.class文件 - 适用于本地执行环境
            return processDir(packageName, directory);
        } else {
            // 浏览JAR文件
            return processJar(packageName, packageFolderURL);
        }
    }

    /**
     * 处理JAR文件中的包
     *
     * <p>从JAR文件中查找指定包的所有类文件。
     * 首先尝试从缓存的索引中查找，如果索引不存在则创建索引。
     * 如果索引查找失败，使用保底的fuse方法。</p>
     *
     * @param packageName 包名
     * @param packageFolderURL JAR文件中包的URL
     * @return 该包下所有类文件的列表
     */
    private List<JavaFileObject> processJar(String packageName, URL packageFolderURL) {
        try {
            // 提取JAR文件的URI（去掉"!/"后面的部分）
            String jarUri = packageFolderURL.toExternalForm().substring(0, packageFolderURL.toExternalForm().lastIndexOf("!/"));
            // 尝试从缓存中获取JAR文件索引
            JarFileIndex jarFileIndex = INDEXS.get(jarUri);
            if (jarFileIndex == null) {
                // 索引不存在，创建新的索引
                jarFileIndex = new JarFileIndex(jarUri, URI.create(jarUri + "!/"));
                INDEXS.put(jarUri, jarFileIndex);
            }
            // 在索引中搜索指定包的类
            List<JavaFileObject> result = jarFileIndex.search(packageName);
            if (result != null) {
                return result;
            }
        } catch (Exception e) {
            // 忽略异常，使用保底方案
        }
        // 保底方案：直接遍历JAR文件条目
        return fuse(packageFolderURL);
    }

    /**
     * 保底的JAR文件处理方法
     *
     * <p>当索引方法失败时，直接遍历JAR文件的所有条目来查找类文件。
     * 这个方法虽然较慢，但更可靠，不依赖于索引结构。</p>
     *
     * @param packageFolderURL JAR文件中包的URL
     * @return 该包下所有类文件的列表
     */
    private List<JavaFileObject> fuse(URL packageFolderURL) {
        List<JavaFileObject> result = new ArrayList<JavaFileObject>();
        try {
            // 提取JAR文件的URI
            String jarUri = packageFolderURL.toExternalForm().substring(0, packageFolderURL.toExternalForm().lastIndexOf("!/"));

            // 打开JAR文件连接
            JarURLConnection jarConn = (JarURLConnection) packageFolderURL.openConnection();
            String rootEntryName = jarConn.getEntryName();

            if (rootEntryName != null) {
                // 可能为null（当自己没有类文件时）
                int rootEnd = rootEntryName.length() + 1;

                // 遍历JAR文件中的所有条目
                Enumeration<JarEntry> entryEnum = jarConn.getJarFile().entries();
                while (entryEnum.hasMoreElements()) {
                    JarEntry jarEntry = entryEnum.nextElement();
                    String name = jarEntry.getName();
                    // 筛选出指定包下的类文件
                    if (name.startsWith(rootEntryName) && name.indexOf('/', rootEnd) == -1 && name.endsWith(CLASS_FILE_EXTENSION)) {
                        // 创建类文件的URI
                        URI uri = URI.create(jarUri + "!/" + name);
                        // 将路径转换为二进制类名
                        String binaryName = name.replaceAll("/", ".");
                        binaryName = binaryName.replaceAll(CLASS_FILE_EXTENSION + "$", "");

                        // 创建并添加Java文件对象
                        result.add(new CustomJavaFileObject(binaryName, uri));
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Wasn't able to open " + packageFolderURL + " as a jar file", e);
        }
        return result;
    }

    /**
     * 处理目录中的包
     *
     * <p>从文件系统目录中查找指定包的所有类文件。
     * 这个方法适用于开发环境或未打包的类文件。</p>
     *
     * @param packageName 包名
     * @param directory 包对应的目录
     * @return 该目录下所有类文件的列表
     */
    private List<JavaFileObject> processDir(String packageName, File directory) {
        // 筛选出目录中的所有.class文件
        File[] files = directory.listFiles(item ->
                item.isFile() && getKind(item.getName()) == JavaFileObject.Kind.CLASS);
        if (files != null) {
            // 将每个文件转换为JavaFileObject
            return Arrays.stream(files).map(item -> {
                // 构造类的全限定名
                String className = packageName + "." + item.getName()
                        .replaceAll(CLASS_FILE_EXTENSION + "$", "");
                return new CustomJavaFileObject(className, item.toURI());
            }).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * 解码URL路径
     *
     * <p>将URL编码的路径字符串解码为普通字符串。
     * 如果解码失败，返回原始字符串。</p>
     *
     * @param filePath URL编码的文件路径
     * @return 解码后的文件路径
     */
    private String decode(String filePath) {
        try {
            return URLDecoder.decode(filePath, "utf-8");
        } catch (Exception e) {
            // 忽略异常，返回原始字符串
        }

        return filePath;
    }

    /**
     * 根据文件名获取Java文件类型
     *
     * <p>通过文件扩展名判断文件的类型：
     * <ul>
     *   <li>.class -> CLASS</li>
     *   <li>.java -> SOURCE</li>
     *   <li>.html -> HTML</li>
     *   <li>其他 -> OTHER</li>
     * </ul>
     * </p>
     *
     * @param name 文件名
     * @return Java文件对象的类型
     */
    public static JavaFileObject.Kind getKind(String name) {
        if (name.endsWith(JavaFileObject.Kind.CLASS.extension))
            return JavaFileObject.Kind.CLASS;
        else if (name.endsWith(JavaFileObject.Kind.SOURCE.extension))
            return JavaFileObject.Kind.SOURCE;
        else if (name.endsWith(JavaFileObject.Kind.HTML.extension))
            return JavaFileObject.Kind.HTML;
        else
            return JavaFileObject.Kind.OTHER;
    }

    /**
     * JAR文件索引
     *
     * <p>这个内部类用于缓存JAR文件的包和类结构信息。
     * 它在初始化时扫描整个JAR文件，建立包名到类列表的映射，
     * 从而加速后续的查找操作。</p>
     */
    public static class JarFileIndex {

        /**
         * JAR文件的URI
         *
         * <p>JAR文件的完整URI，包括"jar:file:"前缀和"!/"后缀。</p>
         */
        private String jarUri;

        /**
         * JAR文件的URI对象
         *
         * <p>用于打开JAR文件连接的URI对象。</p>
         */
        private URI uri;

        /**
         * 包到类列表的映射
         *
         * <p>键为包名（如"com.example"），值为该包下的所有类信息列表。
         * 每个类信息包含类名和URI。</p>
         */
        private Map<String, List<ClassUriWrapper>> packages = new HashMap<>();

        /**
         * 构造函数 - 创建JAR文件索引
         *
         * @param jarUri JAR文件的URI字符串
         * @param uri JAR文件的URI对象
         * @throws IOException 如果发生I/O错误
         */
        public JarFileIndex(String jarUri, URI uri) throws IOException {
            this.jarUri = jarUri;
            this.uri = uri;
            // 初始化时加载索引
            loadIndex();
        }

        /**
         * 加载JAR文件索引
         *
         * <p>扫描JAR文件的所有条目，建立包名到类的映射。
         * 这个方法会过滤掉非类文件和特殊的类（如package-info、module-info）。</p>
         *
         * @throws IOException 如果发生I/O错误
         */
        private void loadIndex() throws IOException {
            // 打开JAR文件连接
            JarURLConnection jarConn = (JarURLConnection) uri.toURL().openConnection();
            // 获取根条目名称（可能为null）
            String rootEntryName = jarConn.getEntryName() == null ? "" : jarConn.getEntryName();
            // 遍历JAR文件中的所有条目
            Enumeration<JarEntry> entryEnum = jarConn.getJarFile().entries();
            while (entryEnum.hasMoreElements()) {
                JarEntry jarEntry = entryEnum.nextElement();
                String entryName = jarEntry.getName();
                // 只处理类文件
                if (entryName.startsWith(rootEntryName) && entryName.endsWith(CLASS_FILE_EXTENSION)) {
                    // 提取类名：去掉.class后缀，去掉根条目名，替换路径分隔符
                    String className = entryName
                            .substring(0, entryName.length() - CLASS_FILE_EXTENSION.length())
                            .replace(rootEntryName, "")
                            .replace("/", ".");
                    // 去掉开头的点号
                    if (className.startsWith(".")) className = className.substring(1);
                    // 跳过特殊的类文件和默认包的类
                    if (className.equals("package-info")
                            || className.equals("module-info")
                            || className.lastIndexOf(".") == -1) {
                        continue;
                    }
                    // 提取包名（最后一个点号之前的部分）
                    String packageName = className.substring(0, className.lastIndexOf("."));
                    // 获取或创建该包的类列表
                    List<ClassUriWrapper> classes = packages.get(packageName);
                    if (classes == null) {
                        classes = new ArrayList<>();
                        packages.put(packageName, classes);
                    }
                    // 将类信息添加到包的类列表中
                    classes.add(new ClassUriWrapper(className, URI.create(jarUri + "!/" + entryName)));
                }
            }
        }

        /**
         * 在索引中搜索指定包的所有类
         *
         * @param packageName 要搜索的包名
         * @return 该包下所有类的JavaFileObject列表，如果包不存在返回空列表
         */
        public List<JavaFileObject> search(String packageName) {
            // 如果索引为空，返回null表示使用保底方案
            if (this.packages.isEmpty()) {
                return null;
            }
            // 如果包存在，转换为JavaFileObject列表返回
            if (this.packages.containsKey(packageName)) {
                return packages.get(packageName).stream().map(item -> {
                    return new CustomJavaFileObject(item.getClassName(), item.getUri());
                }).collect(Collectors.toList());
            }
            // 包不存在，返回空列表
            return Collections.emptyList();
        }
    }
}
