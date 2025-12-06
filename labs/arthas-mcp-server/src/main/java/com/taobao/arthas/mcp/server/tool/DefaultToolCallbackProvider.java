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
 * Default tool callback provider implementation
 * <p>
 * Scan methods with @Tool annotations in the classpath and register them as tool callbacks.
 * <p>
 * Users must call {@link #setToolBasePackage(String)} to configure the package to scan before calling
 * {@link #getToolCallbacks()}.
 */
public class DefaultToolCallbackProvider implements ToolCallbackProvider {
    private static final Logger logger = LoggerFactory.getLogger(DefaultToolCallbackProvider.class);

    private final ToolCallResultConverter toolCallResultConverter;
    private ToolCallback[] toolCallbacks;
    private String toolBasePackage;

    public DefaultToolCallbackProvider() {
        this.toolCallResultConverter = new DefaultToolCallResultConverter();
    }

    public void setToolBasePackage(String toolBasePackage) {
        this.toolBasePackage = toolBasePackage;
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        if (toolCallbacks == null) {
            synchronized (this) {
                if (toolCallbacks == null) {
                    toolCallbacks = scanForToolCallbacks();
                }
            }
        }
        return toolCallbacks;
    }

    private ToolCallback[] scanForToolCallbacks() {
        List<ToolCallback> callbacks = new ArrayList<>();
        try {
            logger.info("Starting to scan for tool callbacks in package: {}", toolBasePackage);
            scanPackageForToolMethods(toolBasePackage, callbacks);
            logger.info("Found {} tool callbacks", callbacks.size());
        } catch (Exception e) {
            logger.error("Failed to scan for tool callbacks: {}", e.getMessage(), e);
        }
        return callbacks.toArray(new ToolCallback[0]);
    }

    private void scanPackageForToolMethods(String packageName, List<ToolCallback> callbacks) throws IOException {
        String packageDirName = packageName.replace('.', '/');
        ClassLoader classLoader = DefaultToolCallbackProvider.class.getClassLoader();
        logger.info("Using classloader: {} for scanning package: {}", classLoader, packageName);

        Enumeration<URL> resources = classLoader.getResources(packageDirName);
        if (!resources.hasMoreElements()) {
            logger.warn("No resources found for package: {}", packageName);
            return;
        }

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            String protocol = resource.getProtocol();
            logger.info("Found resource: {} with protocol: {}", resource, protocol);

            if ("file".equals(protocol)) {
                String filePath = URLDecoder.decode(resource.getFile(), StandardCharsets.UTF_8.name());
                logger.info("Scanning directory: {}", filePath);
                scanDirectory(new File(filePath), packageName, callbacks);
            } else if ("jar".equals(protocol)) {
                JarURLConnection jarConn = (JarURLConnection) resource.openConnection();
                try (JarFile jarFile = jarConn.getJarFile()) {
                    logger.info("Scanning jar file: {}", jarFile.getName());
                    scanJarEntries(jarFile, packageDirName, callbacks);
                }
            } else {
                logger.warn("Unsupported protocol: {} for resource: {}", protocol, resource);
            }
        }
    }

    private void scanDirectory(File directory, String packageName, List<ToolCallback> callbacks) {
        if (!directory.exists() || !directory.isDirectory()) {
            logger.warn("Directory does not exist or is not a directory: {}", directory);
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            logger.warn("Failed to list files in directory: {}", directory);
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), callbacks);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                logger.debug("Processing class: {}", className);
                processClass(className, callbacks);
            }
        }
    }

    private void scanJarEntries(JarFile jarFile, String packageDirName, List<ToolCallback> callbacks) {
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.startsWith(packageDirName) && name.endsWith(".class")) {
                String className = name.substring(0, name.length() - 6).replace('/', '.');
                logger.debug("Processing jar entry: {}", className);
                processClass(className, callbacks);
            }
        }
    }

    private void processClass(String className, List<ToolCallback> callbacks) {
        try {
            Class<?> clazz = Class.forName(className, false, DefaultToolCallbackProvider.class.getClassLoader());
            if (clazz.isInterface() || clazz.isEnum() || clazz.isAnnotation()) {
                return;
            }
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Tool.class)) {
                    registerToolMethod(clazz, method, callbacks);
                }
            }
        } catch (Throwable t) {
            logger.warn("Error loading class {}: {}", className, t.getMessage(), t);
        }
    }

    private void registerToolMethod(Class<?> clazz, Method method, List<ToolCallback> callbacks) {
        try {
            ToolDefinition toolDefinition = ToolDefinitions.from(method);
            Object toolObject = Modifier.isStatic(method.getModifiers()) ? null : clazz.getDeclaredConstructor().newInstance();
            ToolCallback callback = DefaultToolCallback.builder()
                    .toolDefinition(toolDefinition)
                    .toolMethod(method)
                    .toolObject(toolObject)
                    .toolCallResultConverter(toolCallResultConverter)
                    .build();
            callbacks.add(callback);
            logger.info("Registered tool: {} from class: {}", toolDefinition.getName(), clazz.getName());
        } catch (Exception e) {
            logger.error("Failed to register tool {}.{}, error: {}",
                    clazz.getName(), method.getName(), e.getMessage(), e);
        }
    }
}
