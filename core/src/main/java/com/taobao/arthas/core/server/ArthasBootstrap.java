// Arthas核心服务器启动类包
package com.taobao.arthas.core.server;

// Java标准库导入
import java.arthas.SpyAPI;
import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Method;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarFile;

// 第三方库导入
import com.alibaba.arthas.deps.ch.qos.logback.classic.LoggerContext;
import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.arthas.tunnel.client.TunnelClient;
import com.alibaba.bytekit.asm.instrument.InstrumentConfig;
import com.alibaba.bytekit.asm.instrument.InstrumentParseResult;
import com.alibaba.bytekit.asm.instrument.InstrumentTransformer;
import com.alibaba.bytekit.asm.matcher.SimpleClassMatcher;
import com.alibaba.bytekit.utils.AsmUtils;
import com.alibaba.bytekit.utils.IOUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;

// Arthas通用工具类导入
import com.taobao.arthas.common.AnsiLog;
import com.taobao.arthas.common.ArthasConstants;
import com.taobao.arthas.common.PidUtils;
import com.taobao.arthas.common.SocketUtils;

// Arthas核心组件导入
import com.taobao.arthas.core.advisor.Enhancer;
import com.taobao.arthas.core.advisor.TransformerManager;
import com.taobao.arthas.core.command.BuiltinCommandPack;
import com.taobao.arthas.core.command.CommandExecutorImpl;
import com.taobao.arthas.core.command.view.ResultViewResolver;
import com.taobao.arthas.core.config.BinderUtils;
import com.taobao.arthas.core.config.Configure;
import com.taobao.arthas.core.config.FeatureCodec;
import com.taobao.arthas.core.env.ArthasEnvironment;
import com.taobao.arthas.core.env.MapPropertySource;
import com.taobao.arthas.core.env.PropertiesPropertySource;
import com.taobao.arthas.core.env.PropertySource;
import com.taobao.arthas.core.security.SecurityAuthenticator;
import com.taobao.arthas.core.security.SecurityAuthenticatorImpl;
import com.taobao.arthas.core.server.instrument.ClassLoader_Instrument;
import com.taobao.arthas.core.shell.ShellServer;
import com.taobao.arthas.core.shell.ShellServerOptions;
import com.taobao.arthas.core.shell.command.CommandResolver;
import com.taobao.arthas.core.shell.handlers.BindHandler;
import com.taobao.arthas.core.shell.history.HistoryManager;
import com.taobao.arthas.core.shell.history.impl.HistoryManagerImpl;
import com.taobao.arthas.core.shell.impl.ShellServerImpl;
import com.taobao.arthas.core.shell.session.SessionManager;
import com.taobao.arthas.core.shell.session.impl.SessionManagerImpl;
import com.taobao.arthas.core.shell.term.impl.HttpTermServer;
import com.taobao.arthas.core.shell.term.impl.http.api.HttpApiHandler;
import com.taobao.arthas.core.shell.term.impl.http.session.HttpSessionManager;
import com.taobao.arthas.core.shell.term.impl.httptelnet.HttpTelnetTermServer;

// Arthas工具类导入
import com.taobao.arthas.core.util.ArthasBanner;
import com.taobao.arthas.core.util.FileUtils;
import com.taobao.arthas.core.util.IPUtils;
import com.taobao.arthas.core.util.InstrumentationUtils;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.UserStatUtil;
import com.taobao.arthas.core.util.affect.EnhancerAffect;
import com.taobao.arthas.core.util.matcher.WildcardMatcher;

// MCP相关导入
import com.taobao.arthas.core.mcp.ArthasMcpBootstrap;
import com.taobao.arthas.mcp.server.CommandExecutor;
import com.taobao.arthas.mcp.server.protocol.server.handler.McpHttpRequestHandler;

// Netty相关导入
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;


/**
 * Arthas启动引导类
 * 负责初始化和启动Arthas服务器，包括配置加载、组件初始化、服务绑定等功能
 *
 * @author vlinux on 15/5/2.
 * @author hengyunabc
 */
public class ArthasBootstrap {
    // Arthas Spy JAR文件名
    private static final String ARTHAS_SPY_JAR = "arthas-spy.jar";
    // Arthas主目录系统属性名
    public static final String ARTHAS_HOME_PROPERTY = "arthas.home";
    // Arthas主目录路径缓存
    private static String ARTHAS_HOME = null;

    // 配置文件名属性名
    public static final String CONFIG_NAME_PROPERTY = "arthas.config.name";
    // 配置文件位置属性名
    public static final String CONFIG_LOCATION_PROPERTY = "arthas.config.location";
    // 配置覆盖所有属性名
    public static final String CONFIG_OVERRIDE_ALL = "arthas.config.overrideAll";

    // ArthasBootstrap单例实例
    private static ArthasBootstrap arthasBootstrap;

    // Arthas环境配置对象
    private ArthasEnvironment arthasEnvironment;
    // Arthas配置对象
    private Configure configure;

    // 是否已绑定的原子标志
    private AtomicBoolean isBindRef = new AtomicBoolean(false);
    // JVM增强工具实例
    private Instrumentation instrumentation;
    // 类加载器增强转换器
    private InstrumentTransformer classLoaderInstrumentTransformer;
    // 关闭钩子线程
    private Thread shutdown;
    // Shell服务器实例
    private ShellServer shellServer;
    // 定时执行服务
    private ScheduledExecutorService executorService;
    // 会话管理器
    private SessionManager sessionManager;
    // 隧道客户端
    private TunnelClient tunnelClient;

    // 输出路径目录
    private File outputPath;

    // 日志上下文
    private static LoggerContext loggerContext;
    // Netty工作线程组
    private EventExecutorGroup workerGroup;

    // 定时器，用于定时任务
    private Timer timer = new Timer("arthas-timer", true);

    // 转换器管理器，管理类的增强转换
    private TransformerManager transformerManager;

    // 结果视图解析器
    private ResultViewResolver resultViewResolver;

    // 历史记录管理器
    private HistoryManager historyManager;

    // HTTP API处理器
    private HttpApiHandler httpApiHandler;

    // MCP HTTP请求处理器
    private McpHttpRequestHandler mcpRequestHandler;
    // Arthas MCP启动引导实例
    private ArthasMcpBootstrap arthasMcpBootstrap;

    // HTTP会话管理器
    private HttpSessionManager httpSessionManager;
    // 安全认证器
    private SecurityAuthenticator securityAuthenticator;

    /**
     * 私有构造函数，创建ArthasBootstrap实例
     * 执行完整的初始化流程
     *
     * @param instrumentation JVM增强工具实例
     * @param args 启动参数映射
     * @throws Throwable 初始化过程中的任何异常
     */
    private ArthasBootstrap(Instrumentation instrumentation, Map<String, String> args) throws Throwable {
        this.instrumentation = instrumentation;

        // 初始化Fastjson配置
        initFastjson();

        // 1. 初始化Spy API
        initSpy();
        // 2. 初始化Arthas环境配置
        initArthasEnvironment(args);

        // 设置输出路径
        String outputPathStr = configure.getOutputPath();
        if (outputPathStr == null) {
            outputPathStr = ArthasConstants.ARTHAS_OUTPUT;
        }
        outputPath = new File(outputPathStr);
        outputPath.mkdirs();

        // 3. 初始化日志系统
        loggerContext = LogUtil.initLogger(arthasEnvironment);

        // 4. 增强ClassLoader，解决某些ClassLoader加载不到SpyAPI的问题
        enhanceClassLoader();
        // 5. 初始化各种Bean组件
        initBeans();

        // 6. 启动并绑定Agent服务器
        bind(configure);

        // 创建命令执行服务线程池
        executorService = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                final Thread t = new Thread(r, "arthas-command-execute");
                t.setDaemon(true);
                return t;
            }
        });

        // 创建关闭钩子线程
        shutdown = new Thread("as-shutdown-hooker") {

            @Override
            public void run() {
                ArthasBootstrap.this.destroy();
            }
        };

        // 初始化转换器管理器
        transformerManager = new TransformerManager(instrumentation);
        // 注册JVM关闭钩子
        Runtime.getRuntime().addShutdownHook(shutdown);
    }

    /**
     * 初始化Fastjson配置
     * 配置忽略getter错误和将非字符串键写入为字符串
     */
    private void initFastjson() {
        // 忽略getter错误 #1661
        // #2081
        JSON.config(JSONWriter.Feature.IgnoreErrorGetter, JSONWriter.Feature.WriteNonStringKeyAsString);
    }

    /**
     * 初始化各种Bean组件
     * 包括结果视图解析器和历史记录管理器
     */
    private void initBeans() {
        this.resultViewResolver = new ResultViewResolver();
        this.historyManager = new HistoryManagerImpl();
    }

    /**
     * 初始化Spy API
     * 将Spy JAR添加到BootstrapClassLoader的搜索路径中
     *
     * @throws Throwable 初始化过程中的任何异常
     */
    private void initSpy() throws Throwable {
        // TODO 初始化 SpyImpl ?

        // 获取Bootstrap类加载器（系统类加载器的父类加载器）
        ClassLoader parent = ClassLoader.getSystemClassLoader().getParent();
        Class<?> spyClass = null;
        if (parent != null) {
            try {
                // 尝试加载SpyAPI类
                spyClass =parent.loadClass("java.arthas.SpyAPI");
            } catch (Throwable e) {
                // 忽略加载失败
            }
        }
        // 如果Spy类未加载，则需要添加到BootstrapClassLoader
        if (spyClass == null) {
            CodeSource codeSource = ArthasBootstrap.class.getProtectionDomain().getCodeSource();
            if (codeSource != null) {
                // 找到arthas-core.jar文件
                File arthasCoreJarFile = new File(codeSource.getLocation().toURI().getSchemeSpecificPart());
                // 找到spy jar文件
                File spyJarFile = new File(arthasCoreJarFile.getParentFile(), ARTHAS_SPY_JAR);
                // 将spy jar添加到BootstrapClassLoader的搜索路径
                instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(spyJarFile));
            } else {
                throw new IllegalStateException("can not find " + ARTHAS_SPY_JAR);
            }
        }
    }

    /**
     * 增强ClassLoader
     * 通过字节码增强ClassLoader的loadClass方法，解决某些ClassLoader加载不到SpyAPI的问题
     *
     * @throws IOException IO异常
     * @throws UnmodifiableClassException 类不可修改异常
     */
    void enhanceClassLoader() throws IOException, UnmodifiableClassException {
        // 如果没有配置需要增强的ClassLoader，直接返回
        if (configure.getEnhanceLoaders() == null) {
            return;
        }
        // 解析需要增强的ClassLoader类名列表
        Set<String> loaders = new HashSet<String>();
        for (String s : configure.getEnhanceLoaders().split(",")) {
            loaders.add(s.trim());
        }

        // 增强 ClassLoader#loadClass 方法，解决一些ClassLoader加载不到 SpyAPI的问题
        // https://github.com/alibaba/arthas/issues/1596
        byte[] classBytes = IOUtils.getBytes(ArthasBootstrap.class.getClassLoader()
                .getResourceAsStream(ClassLoader_Instrument.class.getName().replace('.', '/') + ".class"));

        // 创建类匹配器
        SimpleClassMatcher matcher = new SimpleClassMatcher(loaders);
        // 创建增强配置
        InstrumentConfig instrumentConfig = new InstrumentConfig(AsmUtils.toClassNode(classBytes), matcher);

        // 解析增强配置并创建转换器
        InstrumentParseResult instrumentParseResult = new InstrumentParseResult();
        instrumentParseResult.addInstrumentConfig(instrumentConfig);
        classLoaderInstrumentTransformer = new InstrumentTransformer(instrumentParseResult);
        // 添加转换器到JVM
        instrumentation.addTransformer(classLoaderInstrumentTransformer, true);

        // 执行类转换
        if (loaders.size() == 1 && loaders.contains(ClassLoader.class.getName())) {
            // 如果只增强 java.lang.ClassLoader，可以减少查找过程
            instrumentation.retransformClasses(ClassLoader.class);
        } else {
            // 批量转换多个类
            InstrumentationUtils.trigerRetransformClasses(instrumentation, loaders);
        }
    }
    
    /**
     * 初始化Arthas环境配置
     * 加载配置项并设置优先级：命令行参数 > 系统环境变量 > 系统属性 > arthas.properties
     *
     * @param argsMap 命令行参数映射
     * @throws IOException IO异常
     */
    private void initArthasEnvironment(Map<String, String> argsMap) throws IOException {
        if (arthasEnvironment == null) {
            arthasEnvironment = new ArthasEnvironment();
        }

        /**
         * <pre>
         * 配置优先级（从高到低）：
         * 脚本里传过来的配置项，即命令行参数 > System Env > System Properties > arthas.properties
         * arthas.properties 提供一个配置项，可以反转优先级。 arthas.config.overrideAll=true
         * https://github.com/alibaba/arthas/issues/986
         * </pre>
         */
        Map<String, Object> copyMap;
        if (argsMap != null) {
            copyMap = new HashMap<String, Object>(argsMap);
            // 添加 arthas.home 属性
            if (!copyMap.containsKey(ARTHAS_HOME_PROPERTY)) {
                copyMap.put(ARTHAS_HOME_PROPERTY, arthasHome());
            }
        } else {
            copyMap = new HashMap<String, Object>(1);
            copyMap.put(ARTHAS_HOME_PROPERTY, arthasHome());
        }

        // 创建映射属性源并添加到环境最前面（优先级最高）
        MapPropertySource mapPropertySource = new MapPropertySource("args", copyMap);
        arthasEnvironment.addFirst(mapPropertySource);

        // 尝试加载arthas.properties配置文件
        tryToLoadArthasProperties();

        // 创建配置对象并绑定环境属性
        configure = new Configure();
        BinderUtils.inject(arthasEnvironment, configure);
    }

    /**
     * 获取Arthas主目录路径
     * 首先尝试从类的CodeSource获取，如果失败则使用当前目录
     *
     * @return Arthas主目录的绝对路径
     */
    private static String arthasHome() {
        if (ARTHAS_HOME != null) {
            return ARTHAS_HOME;
        }
        // 从类的保护域获取代码源位置
        CodeSource codeSource = ArthasBootstrap.class.getProtectionDomain().getCodeSource();
        if (codeSource != null) {
            try {
                // 获取jar文件所在目录的父目录
                ARTHAS_HOME = new File(codeSource.getLocation().toURI().getSchemeSpecificPart()).getParentFile().getAbsolutePath();
            } catch (Throwable e) {
                AnsiLog.error("try to find arthas.home from CodeSource error", e);
            }
        }
        // 如果获取失败，使用当前目录
        if (ARTHAS_HOME == null) {
            ARTHAS_HOME = new File("").getAbsolutePath();
        }
        return ARTHAS_HOME;
    }

    /**
     * 从环境中解析属性值，支持占位符替换
     * 如果属性不存在，返回默认值
     *
     * @param arthasEnvironment Arthas环境对象
     * @param key 属性键
     * @param defaultValue 默认值
     * @return 解析后的属性值
     */
    static String reslove(ArthasEnvironment arthasEnvironment, String key, String defaultValue) {
        String value = arthasEnvironment.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        // 解析属性中的占位符（如 ${xxx}）
        return arthasEnvironment.resolvePlaceholders(value);
    }

    /**
     * 尝试加载arthas.properties配置文件
     * 根据配置的overrideAll属性决定配置文件加载的优先级
     *
     * @throws IOException IO异常
     */
    private void tryToLoadArthasProperties() throws IOException {
        // 解析配置文件位置属性中的占位符
        this.arthasEnvironment.resolvePlaceholders(CONFIG_LOCATION_PROPERTY);

        // 获取配置文件位置，如果未指定则使用arthas主目录
        String location = reslove(arthasEnvironment, CONFIG_LOCATION_PROPERTY, null);

        if (location == null) {
            location = arthasHome();
        }

        // 获取配置文件名，默认为arthas
        String configName = reslove(arthasEnvironment, CONFIG_NAME_PROPERTY, "arthas");

        if (location != null) {
            // 如果location不是以.properties结尾，则拼接配置文件名
            if (!location.endsWith(".properties")) {
                location = new File(location, configName + ".properties").getAbsolutePath();
            }
            // 如果配置文件存在，则加载
            if (new File(location).exists()) {
                Properties properties = FileUtils.readProperties(location);

                // 确定是否覆盖所有配置
                boolean overrideAll = false;
                if (arthasEnvironment.containsProperty(CONFIG_OVERRIDE_ALL)) {
                    overrideAll = arthasEnvironment.getRequiredProperty(CONFIG_OVERRIDE_ALL, boolean.class);
                } else {
                    overrideAll = Boolean.parseBoolean(properties.getProperty(CONFIG_OVERRIDE_ALL, "false"));
                }

                // 根据overrideAll决定配置文件的优先级
                PropertySource<?> propertySource = new PropertiesPropertySource(location, properties);
                if (overrideAll) {
                    // 覆盖所有，配置文件优先级最高
                    arthasEnvironment.addFirst(propertySource);
                } else {
                    // 不覆盖，配置文件优先级最低
                    arthasEnvironment.addLast(propertySource);
                }
            }
        }

    }

    /**
     * 启动并绑定Arthas服务器
     * 执行完整的服务器启动流程，包括端口绑定、组件初始化等
     *
     * @param configure 配置信息
     * @throws Throwable 服务器启动失败时抛出异常
     */
    private void bind(Configure configure) throws Throwable {

        long start = System.currentTimeMillis();

        // 使用原子操作确保只绑定一次
        if (!isBindRef.compareAndSet(false, true)) {
            throw new IllegalStateException("already bind");
        }

        // 初始化随机端口（如果配置为0，则自动查找可用端口）
        if (configure.getTelnetPort() != null && configure.getTelnetPort() == 0) {
            int newTelnetPort = SocketUtils.findAvailableTcpPort();
            configure.setTelnetPort(newTelnetPort);
            logger().info("generate random telnet port: " + newTelnetPort);
        }
        if (configure.getHttpPort() != null && configure.getHttpPort() == 0) {
            int newHttpPort = SocketUtils.findAvailableTcpPort();
            configure.setHttpPort(newHttpPort);
            logger().info("generate random http port: " + newHttpPort);
        }
        // 尝试查找应用名称
        if (configure.getAppName() == null) {
            configure.setAppName(System.getProperty(ArthasConstants.PROJECT_NAME,
                    System.getProperty(ArthasConstants.SPRING_APPLICATION_NAME, null)));
        }

        // 启动隧道客户端（如果配置了隧道服务器）
        try {
            if (configure.getTunnelServer() != null) {
                tunnelClient = new TunnelClient();
                tunnelClient.setAppName(configure.getAppName());
                tunnelClient.setId(configure.getAgentId());
                tunnelClient.setTunnelServerUrl(configure.getTunnelServer());
                tunnelClient.setVersion(ArthasBanner.version());
                ChannelFuture channelFuture = tunnelClient.start();
                channelFuture.await(10, TimeUnit.SECONDS);
            }
        } catch (Throwable t) {
            logger().error("start tunnel client error", t);
        }

        try {
            // 创建Shell服务器选项
            ShellServerOptions options = new ShellServerOptions()
                            .setInstrumentation(instrumentation)
                            .setPid(PidUtils.currentLongPid())
                            .setWelcomeMessage(ArthasBanner.welcome());
            if (configure.getSessionTimeout() != null) {
                options.setSessionTimeout(configure.getSessionTimeout() * 1000);
            }

            // 创建HTTP会话管理器
            this.httpSessionManager = new HttpSessionManager();
            // 安全检查：如果监听0.0.0.0且没有配置密码，强制生成密码
            if (IPUtils.isAllZeroIP(configure.getIp()) && StringUtils.isBlank(configure.getPassword())) {
                // 当 listen 0.0.0.0 时，强制生成密码，防止被远程连接
                String errorMsg = "Listening on 0.0.0.0 is very dangerous! External users can connect to your machine! "
                        + "No password is currently configured. " + "Therefore, a default password is generated, "
                        + "and clients need to use the password to connect!";
                AnsiLog.error(errorMsg);
                configure.setPassword(StringUtils.randomString(64));
                AnsiLog.error("Generated arthas password: " + configure.getPassword());

                logger().error(errorMsg);
                logger().info("Generated arthas password: " + configure.getPassword());
            }

            // 创建安全认证器
            this.securityAuthenticator = new SecurityAuthenticatorImpl(configure.getUsername(), configure.getPassword());

            // 创建Shell服务器实例
            shellServer = new ShellServerImpl(options);

            // 收集禁用的命令列表
            List<String> disabledCommands = new ArrayList<String>();
            if (configure.getDisabledCommands() != null) {
                String[] strings = StringUtils.tokenizeToStringArray(configure.getDisabledCommands(), ",");
                if (strings != null) {
                    disabledCommands.addAll(Arrays.asList(strings));
                }
            }
            // 创建内置命令包
            BuiltinCommandPack builtinCommands = new BuiltinCommandPack(disabledCommands);
            List<CommandResolver> resolvers = new ArrayList<CommandResolver>();
            resolvers.add(builtinCommands);

            // 创建Netty工作线程组
            workerGroup = new NioEventLoopGroup(new DefaultThreadFactory("arthas-TermServer", true));

            // TODO: 发现用户提供的命令解析器
            // 注册Telnet服务器
            if (configure.getTelnetPort() != null && configure.getTelnetPort() > 0) {
                logger().info("try to bind telnet server, host: {}, port: {}.", configure.getIp(), configure.getTelnetPort());
                shellServer.registerTermServer(new HttpTelnetTermServer(configure.getIp(), configure.getTelnetPort(),
                        options.getConnectionTimeout(), workerGroup, httpSessionManager));
            } else {
                logger().info("telnet port is {}, skip bind telnet server.", configure.getTelnetPort());
            }
            // 注册HTTP服务器
            if (configure.getHttpPort() != null && configure.getHttpPort() > 0) {
                logger().info("try to bind http server, host: {}, port: {}.", configure.getIp(), configure.getHttpPort());
                shellServer.registerTermServer(new HttpTermServer(configure.getIp(), configure.getHttpPort(),
                        options.getConnectionTimeout(), workerGroup, httpSessionManager));
            } else {
                // 如果配置了隧道服务器，监听本地地址用于VM间通信
                if (configure.getTunnelServer() != null) {
                    shellServer.registerTermServer(new HttpTermServer(configure.getIp(), configure.getHttpPort(),
                            options.getConnectionTimeout(), workerGroup, httpSessionManager));
                }
                logger().info("http port is {}, skip bind http server.", configure.getHttpPort());
            }

            // 注册命令解析器
            for (CommandResolver resolver : resolvers) {
                shellServer.registerCommandResolver(resolver);
            }

            // 启动服务器监听
            shellServer.listen(new BindHandler(isBindRef));
            // 检查是否绑定成功
            if (!isBind()) {
                throw new IllegalStateException("Arthas failed to bind telnet or http port! Telnet port: "
                        + String.valueOf(configure.getTelnetPort()) + ", http port: "
                        + String.valueOf(configure.getHttpPort()));
            }

            // 创建HTTP API会话管理器
            sessionManager = new SessionManagerImpl(options, shellServer.getCommandManager(), shellServer.getJobController());
            // 创建HTTP API处理器
            httpApiHandler = new HttpApiHandler(historyManager, sessionManager);

            // 启动MCP服务器（如果配置了MCP端点）
            String mcpEndpoint = configure.getMcpEndpoint();
            String mcpProtocol = configure.getMcpProtocol();
            if (mcpEndpoint != null && !mcpEndpoint.trim().isEmpty()) {
                logger().info("try to start mcp server, endpoint: {}, protocol: {}.", mcpEndpoint, mcpProtocol);
                CommandExecutor commandExecutor = new CommandExecutorImpl(sessionManager);
                this.arthasMcpBootstrap = new ArthasMcpBootstrap(commandExecutor, mcpEndpoint, mcpProtocol);
                this.mcpRequestHandler = this.arthasMcpBootstrap.start().getMcpRequestHandler();
            }
            // 输出服务器启动信息
            logger().info("as-server listening on network={};telnet={};http={};timeout={};mcp={};mcpProtocol={};", configure.getIp(),
                    configure.getTelnetPort(), configure.getHttpPort(), options.getConnectionTimeout(), configure.getMcpEndpoint(), configure.getMcpProtocol());

            // 异步上报启动次数
            if (configure.getStatUrl() != null) {
                logger().info("arthas stat url: {}", configure.getStatUrl());
            }
            UserStatUtil.setStatUrl(configure.getStatUrl());
            UserStatUtil.setAgentId(configure.getAgentId());
            UserStatUtil.arthasStart();

            // 初始化SpyAPI
            try {
                SpyAPI.init();
            } catch (Throwable e) {
                // 忽略初始化异常
            }

            logger().info("as-server started in {} ms", System.currentTimeMillis() - start);
        } catch (Throwable e) {
            // 发生异常时销毁资源
            logger().error("Error during start as-server", e);
            destroy();
            throw e;
        }
    }

    /**
     * 关闭工作线程组
     * 优雅地关闭Netty工作线程组
     */
    private void shutdownWorkGroup() {
        if (workerGroup != null) {
            workerGroup.shutdownGracefully(200, 200, TimeUnit.MILLISECONDS);
            workerGroup = null;
        }
    }

    /**
     * 判断服务端是否已经启动
     *
     * @return true:服务端已经启动;false:服务端关闭
     */
    public boolean isBind() {
        return isBindRef.get();
    }

    /**
     * 重置所有增强的类
     * 清除所有类的增强状态
     *
     * @return 增强影响对象，包含重置的详细信息
     * @throws UnmodifiableClassException 类不可修改异常
     */
    public EnhancerAffect reset() throws UnmodifiableClassException {
        return Enhancer.reset(this.instrumentation, new WildcardMatcher("*"));
    }

    /**
     * 销毁Arthas服务器
     * 清理所有资源，关闭所有服务
     * 注意：在调用destroy()之前应该先调用reset()
     */
    public void destroy() {
        // 停止MCP服务器
        if (this.arthasMcpBootstrap != null) {
            try {
                // stop 时需要主动关闭 mcp keep-alive 调度线程，避免 stop 后残留线程导致 ArthasClassLoader 无法回收
                this.arthasMcpBootstrap.shutdown();
            } catch (Throwable e) {
                logger().error("stop mcp server error", e);
            } finally {
                this.arthasMcpBootstrap = null;
                this.mcpRequestHandler = null;
            }
        }
        // 关闭Shell服务器
        if (shellServer != null) {
            shellServer.close();
            shellServer = null;
        }
        // 关闭会话管理器
        if (sessionManager != null) {
            sessionManager.close();
            sessionManager = null;
        }
        // 停止HTTP会话管理器
        if (this.httpSessionManager != null) {
            httpSessionManager.stop();
        }
        // 取消定时器
        if (timer != null) {
            timer.cancel();
        }
        // 停止隧道客户端
        if (this.tunnelClient != null) {
            try {
                tunnelClient.stop();
            } catch (Throwable e) {
                logger().error("stop tunnel client error", e);
            }
        }
        // 关闭执行服务
        if (executorService != null) {
            executorService.shutdownNow();
        }
        // 销毁转换器管理器
        if (transformerManager != null) {
            transformerManager.destroy();
        }
        // 移除类加载器增强转换器
        if (classLoaderInstrumentTransformer != null) {
            instrumentation.removeTransformer(classLoaderInstrumentTransformer);
        }
        // 清除Spy类中的引用
        cleanUpSpyReference();
        // 关闭工作线程组
        shutdownWorkGroup();
        // 销毁用户统计工具
        UserStatUtil.destroy();
        // 移除关闭钩子
        if (shutdown != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdown);
            } catch (Throwable t) {
                // 忽略异常
            }
        }
        logger().info("as-server destroy completed.");
        // 停止日志上下文
        if (loggerContext != null) {
            loggerContext.stop();
        }
    }

    /**
     * 获取ArthasBootstrap单例实例
     * 使用字符串参数版本
     *
     * @param instrumentation JVM增强工具实例
     * @param args 命令行参数字符串
     * @return ArthasBootstrap单例实例
     * @throws Throwable 初始化过程中的异常
     */
    public synchronized static ArthasBootstrap getInstance(Instrumentation instrumentation, String args) throws Throwable {
        if (arthasBootstrap != null) {
            return arthasBootstrap;
        }

        // 将命令行参数字符串解析为映射
        Map<String, String> argsMap = FeatureCodec.DEFAULT_COMMANDLINE_CODEC.toMap(args);
        // 给所有配置加上 "arthas." 前缀
        Map<String, String> mapWithPrefix = new HashMap<String, String>(argsMap.size());
        for (Entry<String, String> entry : argsMap.entrySet()) {
            mapWithPrefix.put("arthas." + entry.getKey(), entry.getValue());
        }
        return getInstance(instrumentation, mapWithPrefix);
    }

    /**
     * 获取ArthasBootstrap单例实例
     * 使用映射参数版本
     *
     * @param instrumentation JVM增强工具实例
     * @param args 命令行参数映射
     * @return ArthasBootstrap单例实例
     * @throws Throwable 初始化过程中的异常
     */
    public synchronized static ArthasBootstrap getInstance(Instrumentation instrumentation, Map<String, String> args) throws Throwable {
        if (arthasBootstrap == null) {
            arthasBootstrap = new ArthasBootstrap(instrumentation, args);
        }
        return arthasBootstrap;
    }

    /**
     * 获取ArthasBootstrap单例实例
     * 必须在初始化后调用
     *
     * @return ArthasBootstrap单例实例
     */
    public static ArthasBootstrap getInstance() {
        if (arthasBootstrap == null) {
            throw new IllegalStateException("ArthasBootstrap must be initialized before!");
        }
        return arthasBootstrap;
    }

    /**
     * 在执行服务中执行任务
     *
     * @param command 要执行的Runnable任务
     */
    public void execute(Runnable command) {
        executorService.execute(command);
    }

    /**
     * 清除SpyAPI里的引用
     * 清理Spy相关资源，避免内存泄漏
     */
    private void cleanUpSpyReference() {
        try {
            // 设置为空操作Spy
            SpyAPI.setNopSpy();
            // 销毁Spy
            SpyAPI.destroy();
        } catch (Throwable e) {
            // 忽略异常
        }
        // 重置Arthas类加载器
        // AgentBootstrap.resetArthasClassLoader();
        try {
            Class<?> clazz = ClassLoader.getSystemClassLoader().loadClass("com.taobao.arthas.agent334.AgentBootstrap");
            Method method = clazz.getDeclaredMethod("resetArthasClassLoader");
            method.invoke(null);
        } catch (Throwable e) {
            // 忽略异常
        }
    }

    /**
     * 获取隧道客户端
     *
     * @return 隧道客户端实例
     */
    public TunnelClient getTunnelClient() {
        return tunnelClient;
    }

    /**
     * 获取Shell服务器
     *
     * @return Shell服务器实例
     */
    public ShellServer getShellServer() {
        return shellServer;
    }

    /**
     * 获取会话管理器
     *
     * @return 会话管理器实例
     */
    public SessionManager getSessionManager() {
        return sessionManager;
    }

    /**
     * 获取定时器
     *
     * @return 定时器实例
     */
    public Timer getTimer() {
        return this.timer;
    }

    /**
     * 获取定时执行服务
     *
     * @return 定时执行服务实例
     */
    public ScheduledExecutorService getScheduledExecutorService() {
        return this.executorService;
    }

    /**
     * 获取JVM增强工具实例
     *
     * @return JVM增强工具实例
     */
    public Instrumentation getInstrumentation() {
        return this.instrumentation;
    }

    /**
     * 获取转换器管理器
     *
     * @return 转换器管理器实例
     */
    public TransformerManager getTransformerManager() {
        return this.transformerManager;
    }

    /**
     * 获取日志记录器
     *
     * @return 日志记录器实例
     */
    private Logger logger() {
        return LoggerFactory.getLogger(this.getClass());
    }

    /**
     * 获取结果视图解析器
     *
     * @return 结果视图解析器实例
     */
    public ResultViewResolver getResultViewResolver() {
        return resultViewResolver;
    }

    /**
     * 获取历史记录管理器
     *
     * @return 历史记录管理器实例
     */
    public HistoryManager getHistoryManager() {
        return historyManager;
    }

    /**
     * 获取HTTP API处理器
     *
     * @return HTTP API处理器实例
     */
    public HttpApiHandler getHttpApiHandler() {
        return httpApiHandler;
    }

    /**
     * 获取MCP HTTP请求处理器
     *
     * @return MCP HTTP请求处理器实例
     */
    public McpHttpRequestHandler getMcpRequestHandler() {
        return mcpRequestHandler;
    }

    /**
     * 获取输出路径
     *
     * @return 输出路径目录
     */
    public File getOutputPath() {
        return outputPath;
    }

    /**
     * 获取安全认证器
     *
     * @return 安全认证器实例
     */
    public SecurityAuthenticator getSecurityAuthenticator() {
        return securityAuthenticator;
    }

    /**
     * 获取配置对象
     *
     * @return 配置对象实例
     */
    public Configure getConfigure() {
        return configure;
    }
}
