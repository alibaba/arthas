package com.taobao.arthas.agent.attach;

import java.arthas.SpyAPI;
import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.zeroturnaround.zip.ZipUtil;

import net.bytebuddy.agent.ByteBuddyAgent;

/**
 * Arthas代理类
 *
 * <p>提供了在运行时attach Arthas到目标JVM的能力
 * 支持从指定目录或从classpath中解压arthas-bin.zip来初始化Arthas
 *
 * <p>主要功能：
 * <ul>
 *   <li>自动获取Instrumentation实例（使用ByteBuddyAgent）</li>
 *   <li>从指定目录或classpath加载Arthas</li>
 *   <li>创建隔离的类加载器来加载Arthas核心类</li>
 *   <li>初始化ArthasBootstrap并绑定服务端口</li>
 * </ul>
 *
 * @author hengyunabc 2020-06-22
 *
 */
public class ArthasAgent {
    /**
     * 创建临时目录时的最大尝试次数
     */
    private static final int TEMP_DIR_ATTEMPTS = 10000;

    /**
     * Arthas核心JAR包的文件名
     */
    private static final String ARTHAS_CORE_JAR = "arthas-core.jar";

    /**
     * Arthas启动引导类的全限定名
     */
    private static final String ARTHAS_BOOTSTRAP = "com.taobao.arthas.core.server.ArthasBootstrap";

    /**
     * 获取ArthasBootstrap单例实例的方法名
     */
    private static final String GET_INSTANCE = "getInstance";

    /**
     * 检查Arthas服务是否已绑定端口的方法名
     */
    private static final String IS_BIND = "isBind";

    /**
     * 错误消息
     * 当初始化失败时，保存错误信息以便后续查询
     */
    private String errorMessage;

    /**
     * Arthas配置参数映射
     * 存储传递给Arthas的各种配置参数
     */
    private Map<String, String> configMap = new HashMap<String, String>();

    /**
     * Arthas主目录
     * 指定arthas-core.jar所在的目录路径
     */
    private String arthasHome;

    /**
     * 是否静默初始化
     * 如果为true，初始化失败时不会抛出异常，只保存错误信息
     */
    private boolean slientInit;

    /**
     * JVM Instrumentation实例
     * 用于类的转换和重定义
     */
    /**
     * JVM Instrumentation实例
     * 用于类的转换和重定义
     */
    private Instrumentation instrumentation;

    /**
     * 默认构造函数
     *
     * <p>创建一个使用默认配置的ArthasAgent实例
     */
    public ArthasAgent() {
        this(null, null, false, null);
    }

    /**
     * 带配置参数的构造函数
     *
     * @param configMap Arthas配置参数映射
     */
    public ArthasAgent(Map<String, String> configMap) {
        this(configMap, null, false, null);
    }

    /**
     * 指定Arthas主目录的构造函数
     *
     * @param arthasHome Arthas主目录，应包含arthas-core.jar文件
     */
    public ArthasAgent(String arthasHome) {
        this(null, arthasHome, false, null);
    }

    /**
     * 完整参数的构造函数
     *
     * @param configMap Arthas配置参数映射，可以为null
     * @param arthasHome Arthas主目录，可以为null（将从classpath解压）
     * @param slientInit 是否静默初始化，失败时不抛异常
     * @param instrumentation Instrumentation实例，可以为null（将自动获取）
     */
    public ArthasAgent(Map<String, String> configMap, String arthasHome, boolean slientInit,
            Instrumentation instrumentation) {
        if (configMap != null) {
            this.configMap = configMap;
        }

        this.arthasHome = arthasHome;
        this.slientInit = slientInit;
        this.instrumentation = instrumentation;
    }

    /**
     * 使用默认配置attach Arthas到当前JVM
     *
     * <p>这是一个便捷方法，等同于创建ArthasAgent实例并调用init()
     */
    public static void attach() {
        new ArthasAgent().init();
    }

    /**
     * 使用指定配置参数attach Arthas到当前JVM
     *
     * <p>支持的配置参数参见：
     * @see <a href="https://arthas.aliyun.com/doc/arthas-properties.html">Arthas属性配置文档</a>
     *
     * @param configMap Arthas配置参数映射
     */
    public static void attach(Map<String, String> configMap) {
        new ArthasAgent(configMap).init();
    }

    /**
     * 使用指定的Arthas目录attach到当前JVM
     *
     * <p>use the specified arthas
     *
     * @param arthasHome Arthas目录，应包含arthas-core.jar文件
     */
    public static void attach(String arthasHome) {
        new ArthasAgent(arthasHome).init();
    }

    /**
     * 初始化Arthas并attach到当前JVM
     *
     * <p>执行以下操作：
     * <ol>
     *   <li>检查Arthas是否已在运行，避免重复初始化</li>
     *   <li>获取Instrumentation实例（如果未提供）</li>
     *   <li>确定Arthas主目录（从参数或从classpath解压）</li>
     *   <li>创建隔离的类加载器</li>
     *   <li>初始化ArthasBootstrap并绑定服务端口</li>
     * </ol>
     *
     * @throws IllegalStateException 如果初始化失败且slientInit为false
     */
    public void init() throws IllegalStateException {
        // 尝试判断arthas是否已在运行，如果是的话，直接就退出
        try {
            // 尝试加载SpyAPI类来检查Arthas是否已初始化
            Class.forName("java.arthas.SpyAPI"); // 加载不到会抛异常
            if (SpyAPI.isInited()) {
                // Arthas已在运行，直接返回
                return;
            }
        } catch (Throwable e) {
            // SpyAPI类不存在，继续初始化
            // ignore
        }

        try {
            // 如果没有提供Instrumentation实例，使用ByteBuddyAgent自动获取
            if (instrumentation == null) {
                instrumentation = ByteBuddyAgent.install();
            }

            // 检查 arthasHome
            // 如果没有指定Arthas主目录，从classpath中解压arthas-bin.zip
            if (arthasHome == null || arthasHome.trim().isEmpty()) {
                // 解压出 arthasHome
                // 从classpath中获取arthas-bin.zip资源
                URL coreJarUrl = this.getClass().getClassLoader().getResource("arthas-bin.zip");
                if (coreJarUrl != null) {
                    // 创建临时目录并解压arthas-bin.zip
                    File tempArthasDir = createTempDir();
                    ZipUtil.unpack(coreJarUrl.openStream(), tempArthasDir);
                    arthasHome = tempArthasDir.getAbsolutePath();
                } else {
                    // 无法从classpath中找到arthas-bin.zip
                    throw new IllegalArgumentException("can not getResources arthas-bin.zip from classloader: "
                            + this.getClass().getClassLoader());
                }
            }

            // find arthas-core.jar
            // 验证arthas-core.jar文件是否存在
            File arthasCoreJarFile = new File(arthasHome, ARTHAS_CORE_JAR);
            if (!arthasCoreJarFile.exists()) {
                throw new IllegalStateException("can not find arthas-core.jar under arthasHome: " + arthasHome);
            }
            // 创建专用的类加载器来加载Arthas核心类
            AttachArthasClassloader arthasClassLoader = new AttachArthasClassloader(
                    new URL[] { arthasCoreJarFile.toURI().toURL() });

            /**
             * 使用反射加载并调用ArthasBootstrap.getInstance方法
             * <pre>
             * ArthasBootstrap bootstrap = ArthasBootstrap.getInstance(inst, configMap);
             * </pre>
             */
            // 加载ArthasBootstrap类
            Class<?> bootstrapClass = arthasClassLoader.loadClass(ARTHAS_BOOTSTRAP);
            // 调用getInstance方法获取单例实例，传入Instrumentation和配置参数
            Object bootstrap = bootstrapClass.getMethod(GET_INSTANCE, Instrumentation.class, Map.class).invoke(null,
                    instrumentation, configMap);
            // 检查Arthas服务是否成功绑定到端口
            boolean isBind = (Boolean) bootstrapClass.getMethod(IS_BIND).invoke(bootstrap);
            if (!isBind) {
                // 绑定失败，抛出异常
                String errorMsg = "Arthas server port binding failed! Please check $HOME/logs/arthas/arthas.log for more details.";
                throw new RuntimeException(errorMsg);
            }
        } catch (Throwable e) {
            // 保存错误信息
            errorMessage = e.getMessage();
            // 如果不是静默模式，抛出异常
            if (!slientInit) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * 创建临时目录
     *
     * <p>在系统临时目录下创建一个唯一的Arthas临时目录
     * 目录名格式为：arthas-{当前时间戳}-{序号}
     *
     * <p>会尝试最多TEMP_DIR_ATTEMPTS次来创建目录
     *
     * @return 创建的临时目录
     * @throws IllegalStateException 如果创建失败
     */
    private static File createTempDir() {
        // 获取系统临时目录作为基础目录
        File baseDir = new File(System.getProperty("java.io.tmpdir"));
        // 生成基础目录名：arthas-时间戳-
        String baseName = "arthas-" + System.currentTimeMillis() + "-";

        // 尝试创建目录，最多尝试TEMP_DIR_ATTEMPTS次
        for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
            File tempDir = new File(baseDir, baseName + counter);
            if (tempDir.mkdir()) {
                // 目录创建成功
                return tempDir;
            }
        }
        // 所有尝试都失败，抛出异常
        throw new IllegalStateException("Failed to create directory within " + TEMP_DIR_ATTEMPTS + " attempts (tried "
                + baseName + "0 to " + baseName + (TEMP_DIR_ATTEMPTS - 1) + ')');
    }

    /**
     * 获取错误消息
     *
     * <p>返回初始化过程中产生的错误消息
     * 如果没有错误，返回null
     *
     * @return 错误消息字符串
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * 设置错误消息
     *
     * <p>通常用于在测试或特殊场景下设置错误状态
     *
     * @param errorMessage 错误消息字符串
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
