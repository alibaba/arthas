package com.taobao.arthas.mcp.server.tool;

import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.definition.ToolDefinition;
import com.taobao.arthas.mcp.server.tool.definition.ToolDefinitions;
import com.taobao.arthas.mcp.server.tool.execution.DefaultToolCallResultConverter;
import com.taobao.arthas.mcp.server.tool.execution.ToolCallResultConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 默认工具回调提供者实现
 * <p>
 * 扫描类路径下带有@Tool注解的方法，并将它们注册为工具回调。
 * 用户必须在使用{@link #getToolCallbacks()}之前调用{@link #setToolBasePackage(String)}配置要扫描的包。
 * </p>
 * <p>
 * 支持从文件系统和JAR包中扫描类文件。
 * </p>
 */
public class DefaultToolCallbackProvider implements ToolCallbackProvider {
    private static final Logger logger = LoggerFactory.getLogger(DefaultToolCallbackProvider.class);

    /**
     * 工具调用结果转换器
     * <p>
     * 用于将工具执行结果转换为统一的字符串格式
     * </p>
     */
    private final ToolCallResultConverter toolCallResultConverter;

    /**
     * 工具回调数组缓存
     * <p>
     * 使用双重检查锁定实现懒加载和线程安全
     * </p>
     */
    private ToolCallback[] toolCallbacks;

    /**
     * 要扫描的基础包名
     * <p>
     * 必须在使用前设置
     * </p>
     */
    private String toolBasePackage;

    /**
     * 构造函数
     * <p>
     * 初始化默认的工具调用结果转换器
     * </p>
     */
    public DefaultToolCallbackProvider() {
        this.toolCallResultConverter = new DefaultToolCallResultConverter();
    }

    /**
     * 设置要扫描的基础包名
     *
     * @param toolBasePackage 基础包名（如：com.example.tools）
     */
    public void setToolBasePackage(String toolBasePackage) {
        this.toolBasePackage = toolBasePackage;
    }

    /**
     * 获取工具回调数组
     * <p>
     * 使用双重检查锁定实现线程安全的懒加载：
     * 1. 第一次检查：避免不必要的同步
     * 2. 同步块：保证只有一个线程执行扫描
     * 3. 第二次检查：避免重复扫描
     * </p>
     *
     * @return 工具回调数组
     */
    @Override
    public ToolCallback[] getToolCallbacks() {
        // 第一次检查：是否已经初始化
        if (toolCallbacks == null) {
            // 同步代码块：保证线程安全
            synchronized (this) {
                // 第二次检查：防止其他线程已经初始化
                if (toolCallbacks == null) {
                    // 扫描并创建工具回调
                    toolCallbacks = scanForToolCallbacks();
                }
            }
        }
        return toolCallbacks;
    }

    /**
     * 扫描并创建工具回调
     * <p>
     * 扫描指定包下的所有类，查找带有@Tool注解的方法并创建工具回调
     * </p>
     *
     * @return 工具回调数组
     */
    private ToolCallback[] scanForToolCallbacks() {
        List<ToolCallback> callbacks = new ArrayList<>();
        try {
            logger.info("Starting to scan for tool callbacks in package: {}", toolBasePackage);
            // 递归扫描包中的所有工具方法
            scanPackageForToolMethods(toolBasePackage, callbacks);
            logger.info("Found {} tool callbacks", callbacks.size());
        } catch (Exception e) {
            logger.error("Failed to scan for tool callbacks: {}", e.getMessage(), e);
        }
        return callbacks.toArray(new ToolCallback[0]);
    }

    /**
     * 扫描指定包中的工具方法
     * <p>
     * 支持从文件系统和JAR包中扫描类文件
     * </p>
     *
     * @param packageName 包名
     * @param callbacks 工具回调列表（输出参数）
     * @throws IOException 如果IO操作失败
     */
    private void scanPackageForToolMethods(String packageName, List<ToolCallback> callbacks) throws IOException {
        // 将包名转换为路径格式（如：com.example -> com/example）
        String packageDirName = packageName.replace('.', '/');
        // 获取类加载器
        ClassLoader classLoader = DefaultToolCallbackProvider.class.getClassLoader();
        logger.info("Using classloader: {} for scanning package: {}", classLoader, packageName);

        // 获取包路径下的所有资源
        Enumeration<URL> resources = classLoader.getResources(packageDirName);
        if (!resources.hasMoreElements()) {
            logger.warn("No resources found for package: {}", packageName);
            return;
        }

        // 遍历所有资源
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            String protocol = resource.getProtocol();
            logger.info("Found resource: {} with protocol: {}", resource, protocol);

            // 处理文件系统中的资源
            if ("file".equals(protocol)) {
                // URL解码文件路径（处理中文和特殊字符）
                String filePath = URLDecoder.decode(resource.getFile(), StandardCharsets.UTF_8.name());
                logger.info("Scanning directory: {}", filePath);
                // 扫描目录中的类文件
                scanDirectory(new File(filePath), packageName, callbacks);
            }
            // 处理JAR包中的资源
            else if ("jar".equals(protocol)) {
                JarURLConnection jarConn = (JarURLConnection) resource.openConnection();
                try (JarFile jarFile = jarConn.getJarFile()) {
                    logger.info("Scanning jar file: {}", jarFile.getName());
                    // 扫描JAR包中的类文件
                    scanJarEntries(jarFile, packageDirName, callbacks);
                }
            } else {
                logger.warn("Unsupported protocol: {} for resource: {}", protocol, resource);
            }
        }
    }

    /**
     * 扫描目录中的类文件
     * <p>
     * 递归扫描目录及子目录中的所有.class文件
     * </p>
     *
     * @param directory 要扫描的目录
     * @param packageName 对应的包名
     * @param callbacks 工具回调列表（输出参数）
     */
    private void scanDirectory(File directory, String packageName, List<ToolCallback> callbacks) {
        // 检查目录是否存在且是有效目录
        if (!directory.exists() || !directory.isDirectory()) {
            logger.warn("Directory does not exist or is not a directory: {}", directory);
            return;
        }
        // 获取目录中的所有文件和子目录
        File[] files = directory.listFiles();
        if (files == null) {
            logger.warn("Failed to list files in directory: {}", directory);
            return;
        }
        // 遍历目录中的所有文件
        for (File file : files) {
            // 如果是子目录，递归扫描
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), callbacks);
            }
            // 如果是.class文件，处理该类
            else if (file.getName().endsWith(".class")) {
                // 从文件名构建完整类名（去掉.class后缀）
                String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                logger.debug("Processing class: {}", className);
                // 处理类中的工具方法
                processClass(className, callbacks);
            }
        }
    }

    /**
     * 扫描JAR包中的类文件
     * <p>
     * 遍历JAR包中的所有条目，找出指定包下的.class文件
     * </p>
     *
     * @param jarFile JAR文件对象
     * @param packageDirName 包路径
     * @param callbacks 工具回调列表（输出参数）
     */
    private void scanJarEntries(JarFile jarFile, String packageDirName, List<ToolCallback> callbacks) {
        // 获取JAR包中的所有条目
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            // 检查是否是指定包下的.class文件
            if (name.startsWith(packageDirName) && name.endsWith(".class")) {
                // 从条目名称构建完整类名（路径替换为包名，去掉.class后缀）
                String className = name.substring(0, name.length() - 6).replace('/', '.');
                logger.debug("Processing jar entry: {}", className);
                // 处理类中的工具方法
                processClass(className, callbacks);
            }
        }
    }

    /**
     * 处理单个类
     * <p>
     * 加载类并查找所有带有@Tool注解的方法，然后注册为工具回调
     * </p>
     *
     * @param className 完整类名
     * @param callbacks 工具回调列表（输出参数）
     */
    private void processClass(String className, List<ToolCallback> callbacks) {
        try {
            // 加载类（不初始化，不运行静态代码块）
            Class<?> clazz = Class.forName(className, false, DefaultToolCallbackProvider.class.getClassLoader());
            // 跳过接口、枚举和注解类型
            if (clazz.isInterface() || clazz.isEnum() || clazz.isAnnotation()) {
                return;
            }
            // 遍历类中声明的所有方法
            for (Method method : clazz.getDeclaredMethods()) {
                // 检查方法是否有@Tool注解
                if (method.isAnnotationPresent(Tool.class)) {
                    // 注册工具方法
                    registerToolMethod(clazz, method, callbacks);
                }
            }
        } catch (Throwable t) {
            logger.warn("Error loading class {}: {}", className, t.getMessage(), t);
        }
    }

    /**
     * 注册工具方法
     * <p>
     * 创建工具定义、实例化工具对象、构建工具回调并添加到列表中
     * </p>
     *
     * @param clazz 类对象
     * @param method 方法对象
     * @param callbacks 工具回调列表（输出参数）
     */
    private void registerToolMethod(Class<?> clazz, Method method, List<ToolCallback> callbacks) {
        try {
            // 从方法注解中提取工具定义
            ToolDefinition toolDefinition = ToolDefinitions.from(method);
            // 如果是静态方法，toolObject为null；否则创建实例
            Object toolObject = Modifier.isStatic(method.getModifiers()) ? null : clazz.getDeclaredConstructor().newInstance();
            // 使用Builder模式创建工具回调
            ToolCallback callback = DefaultToolCallback.builder()
                    .toolDefinition(toolDefinition)
                    .toolMethod(method)
                    .toolObject(toolObject)
                    .toolCallResultConverter(toolCallResultConverter)
                    .build();
            // 添加到回调列表
            callbacks.add(callback);
            logger.info("Registered tool: {} from class: {}", toolDefinition.getName(), clazz.getName());
        } catch (Exception e) {
            logger.error("Failed to register tool {}.{}, error: {}",
                    clazz.getName(), method.getName(), e.getMessage(), e);
        }
    }
}
