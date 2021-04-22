package com.taobao.arthas.core.server;

import java.arthas.SpyAPI;
import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Method;
import java.security.CodeSource;
import java.util.ArrayList;
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
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.taobao.arthas.common.AnsiLog;
import com.taobao.arthas.common.ArthasConstants;
import com.taobao.arthas.common.PidUtils;
import com.taobao.arthas.common.SocketUtils;
import com.taobao.arthas.core.advisor.Enhancer;
import com.taobao.arthas.core.advisor.TransformerManager;
import com.taobao.arthas.core.command.BuiltinCommandPack;
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
import com.taobao.arthas.core.util.ArthasBanner;
import com.taobao.arthas.core.util.FileUtils;
import com.taobao.arthas.core.util.InstrumentationUtils;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.UserStatUtil;
import com.taobao.arthas.core.util.affect.EnhancerAffect;
import com.taobao.arthas.core.util.matcher.WildcardMatcher;

import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;


/**
 * @author vlinux on 15/5/2.
 * @author hengyunabc
 */
public class ArthasBootstrap {
    private static final String ARTHAS_SPY_JAR = "arthas-spy.jar";
    public static final String ARTHAS_HOME_PROPERTY = "arthas.home";
    private static String ARTHAS_HOME = null;

    public static final String CONFIG_NAME_PROPERTY = "arthas.config.name";
    public static final String CONFIG_LOCATION_PROPERTY = "arthas.config.location";
    public static final String CONFIG_OVERRIDE_ALL = "arthas.config.overrideAll";

    private static ArthasBootstrap arthasBootstrap;

    private ArthasEnvironment arthasEnvironment;
    private Configure configure;

    private AtomicBoolean isBindRef = new AtomicBoolean(false);
    private Instrumentation instrumentation;
    private InstrumentTransformer classLoaderInstrumentTransformer;
    private Thread shutdown;
    private ShellServer shellServer;
    private ScheduledExecutorService executorService;
    private SessionManager sessionManager;
    private TunnelClient tunnelClient;

    private File outputPath;

    private static LoggerContext loggerContext;
    private EventExecutorGroup workerGroup;

    private Timer timer = new Timer("arthas-timer", true);

    private TransformerManager transformerManager;

    private ResultViewResolver resultViewResolver;

    private HistoryManager historyManager;

    private HttpApiHandler httpApiHandler;

    private HttpSessionManager httpSessionManager;
    private SecurityAuthenticator securityAuthenticator;

    private ArthasBootstrap(Instrumentation instrumentation, Map<String, String> args) throws Throwable {
        this.instrumentation = instrumentation;

        initFastjson();

        // 1. initSpy()
        initSpy();
        // 2. ArthasEnvironment
        initArthasEnvironment(args);

        String outputPathStr = configure.getOutputPath();
        if (outputPathStr == null) {
            outputPathStr = ArthasConstants.ARTHAS_OUTPUT;
        }
        outputPath = new File(outputPathStr);
        outputPath.mkdirs();

        // 3. init logger
        loggerContext = LogUtil.initLooger(arthasEnvironment);

        // 4. 增强ClassLoader
        enhanceClassLoader();
        // 5. init beans
        initBeans();

        // 6. start agent server
        bind(configure);

        executorService = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                final Thread t = new Thread(r, "arthas-command-execute");
                t.setDaemon(true);
                return t;
            }
        });

        shutdown = new Thread("as-shutdown-hooker") {

            @Override
            public void run() {
                ArthasBootstrap.this.destroy();
            }
        };

        transformerManager = new TransformerManager(instrumentation);
        Runtime.getRuntime().addShutdownHook(shutdown);
    }

    private void initFastjson() {
        // disable  fastjson circular reference feature
        JSON.DEFAULT_GENERATE_FEATURE |= SerializerFeature.DisableCircularReferenceDetect.getMask();
        // add date format option for  fastjson
        JSON.DEFAULT_GENERATE_FEATURE |= SerializerFeature.WriteDateUseDateFormat.getMask();
        // ignore getter error #1661
        JSON.DEFAULT_GENERATE_FEATURE |= SerializerFeature.IgnoreErrorGetter.getMask();
    }

    private void initBeans() {
        this.resultViewResolver = new ResultViewResolver();
        this.historyManager = new HistoryManagerImpl();
    }

    private void initSpy() throws Throwable {
        // TODO init SpyImpl ?

        // 将Spy添加到BootstrapClassLoader
        ClassLoader parent = ClassLoader.getSystemClassLoader().getParent();
        Class<?> spyClass = null;
        if (parent != null) {
            try {
                spyClass =parent.loadClass("java.arthas.SpyAPI");
            } catch (Throwable e) {
                // ignore
            }
        }
        if (spyClass == null) {
            CodeSource codeSource = ArthasBootstrap.class.getProtectionDomain().getCodeSource();
            if (codeSource != null) {
                File arthasCoreJarFile = new File(codeSource.getLocation().toURI().getSchemeSpecificPart());
                File spyJarFile = new File(arthasCoreJarFile.getParentFile(), ARTHAS_SPY_JAR);
                instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(spyJarFile));
            } else {
                throw new IllegalStateException("can not find " + ARTHAS_SPY_JAR);
            }
        }
    }

    void enhanceClassLoader() throws IOException, UnmodifiableClassException {
        if (configure.getEnhanceLoaders() == null) {
            return;
        }
        Set<String> loaders = new HashSet<String>();
        for (String s : configure.getEnhanceLoaders().split(",")) {
            loaders.add(s.trim());
        }

        // 增强 ClassLoader#loadClsss ，解决一些ClassLoader加载不到 SpyAPI的问题
        // https://github.com/alibaba/arthas/issues/1596
        byte[] classBytes = IOUtils.getBytes(ArthasBootstrap.class.getClassLoader()
                .getResourceAsStream(ClassLoader_Instrument.class.getName().replace('.', '/') + ".class"));

        SimpleClassMatcher matcher = new SimpleClassMatcher(loaders);
        InstrumentConfig instrumentConfig = new InstrumentConfig(AsmUtils.toClassNode(classBytes), matcher);

        InstrumentParseResult instrumentParseResult = new InstrumentParseResult();
        instrumentParseResult.addInstrumentConfig(instrumentConfig);
        classLoaderInstrumentTransformer = new InstrumentTransformer(instrumentParseResult);
        instrumentation.addTransformer(classLoaderInstrumentTransformer, true);

        if (loaders.size() == 1 && loaders.contains(ClassLoader.class.getName())) {
            // 如果只增强 java.lang.ClassLoader，可以减少查找过程
            instrumentation.retransformClasses(ClassLoader.class);
        } else {
            InstrumentationUtils.trigerRetransformClasses(instrumentation, loaders);
        }
    }
    
    private void initArthasEnvironment(Map<String, String> argsMap) throws IOException {
        if (arthasEnvironment == null) {
            arthasEnvironment = new ArthasEnvironment();
        }

        /**
         * <pre>
         * 脚本里传过来的配置项，即命令行参数 > System Env > System Properties > arthas.properties
         * arthas.properties 提供一个配置项，可以反转优先级。 arthas.config.overrideAll=true
         * https://github.com/alibaba/arthas/issues/986
         * </pre>
         */
        Map<String, String> copyMap = new HashMap<String, String>();
        if (argsMap != null) {
            copyMap.putAll(argsMap);
        }
        // 添加 arthas.home
        if (!copyMap.containsKey(ARTHAS_HOME_PROPERTY)) {
            copyMap.put(ARTHAS_HOME_PROPERTY, arthasHome());
        }

        MapPropertySource mapPropertySource = new MapPropertySource("args", (Map<String, Object>)(Object)copyMap);
        arthasEnvironment.addFirst(mapPropertySource);

        tryToLoadArthasProperties();

        configure = new Configure();
        BinderUtils.inject(arthasEnvironment, configure);
    }

    private static String arthasHome() {
        if (ARTHAS_HOME != null) {
            return ARTHAS_HOME;
        }
        CodeSource codeSource = ArthasBootstrap.class.getProtectionDomain().getCodeSource();
        if (codeSource != null) {
            try {
                ARTHAS_HOME = new File(codeSource.getLocation().toURI().getSchemeSpecificPart()).getParentFile().getAbsolutePath();
            } catch (Throwable e) {
                AnsiLog.error("try to find arthas.home from CodeSource error", e);
            }
        }
        if (ARTHAS_HOME == null) {
            ARTHAS_HOME = new File("").getAbsolutePath();
        }
        return ARTHAS_HOME;
    }

    // try to load arthas.properties
    private void tryToLoadArthasProperties() throws IOException {
        this.arthasEnvironment.resolvePlaceholders(CONFIG_LOCATION_PROPERTY);

        String location = null;

        if (arthasEnvironment.containsProperty(CONFIG_LOCATION_PROPERTY)) {
            location = arthasEnvironment.resolvePlaceholders(CONFIG_LOCATION_PROPERTY);
        }

        if (location == null) {
            location = arthasHome();
        }

        String configName = "arthas";
        if (arthasEnvironment.containsProperty(CONFIG_NAME_PROPERTY)) {
            configName = arthasEnvironment.resolvePlaceholders(CONFIG_NAME_PROPERTY);
        }

        if (location != null) {
            if (!location.endsWith(".properties")) {
                location = new File(location, configName + ".properties").getAbsolutePath();
            }
        }

        if (new File(location).exists()) {
            Properties properties = FileUtils.readProperties(location);

            boolean overrideAll = false;
            if (arthasEnvironment.containsProperty(CONFIG_OVERRIDE_ALL)) {
                overrideAll = arthasEnvironment.getRequiredProperty(CONFIG_OVERRIDE_ALL, boolean.class);
            } else {
                overrideAll = Boolean.parseBoolean(properties.getProperty(CONFIG_OVERRIDE_ALL, "false"));
            }

            PropertySource<?> propertySource = new PropertiesPropertySource(location, properties);
            if (overrideAll) {
                arthasEnvironment.addFirst(propertySource);
            } else {
                arthasEnvironment.addLast(propertySource);
            }
        }
    }

    /**
     * Bootstrap arthas server
     *
     * @param configure 配置信息
     * @throws IOException 服务器启动失败
     */
    private void bind(Configure configure) throws Throwable {

        long start = System.currentTimeMillis();

        if (!isBindRef.compareAndSet(false, true)) {
            throw new IllegalStateException("already bind");
        }

        // init random port
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
        // try to find appName
        if (configure.getAppName() == null) {
            configure.setAppName(System.getProperty(ArthasConstants.PROJECT_NAME,
                    System.getProperty(ArthasConstants.SPRING_APPLICATION_NAME, null)));
        }

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
            ShellServerOptions options = new ShellServerOptions()
                            .setInstrumentation(instrumentation)
                            .setPid(PidUtils.currentLongPid())
                            .setWelcomeMessage(ArthasBanner.welcome());
            if (configure.getSessionTimeout() != null) {
                options.setSessionTimeout(configure.getSessionTimeout() * 1000);
            }

            this.httpSessionManager = new HttpSessionManager();
            this.securityAuthenticator = new SecurityAuthenticatorImpl(configure.getUsername(), configure.getPassword());

            shellServer = new ShellServerImpl(options);
            BuiltinCommandPack builtinCommands = new BuiltinCommandPack();
            List<CommandResolver> resolvers = new ArrayList<CommandResolver>();
            resolvers.add(builtinCommands);

            //worker group
            workerGroup = new NioEventLoopGroup(new DefaultThreadFactory("arthas-TermServer", true));

            // TODO: discover user provided command resolver
            if (configure.getTelnetPort() != null && configure.getTelnetPort() > 0) {
                logger().info("try to bind telnet server, host: {}, port: {}.", configure.getIp(), configure.getTelnetPort());
                shellServer.registerTermServer(new HttpTelnetTermServer(configure.getIp(), configure.getTelnetPort(),
                        options.getConnectionTimeout(), workerGroup, httpSessionManager));
            } else {
                logger().info("telnet port is {}, skip bind telnet server.", configure.getTelnetPort());
            }
            if (configure.getHttpPort() != null && configure.getHttpPort() > 0) {
                logger().info("try to bind http server, host: {}, port: {}.", configure.getIp(), configure.getHttpPort());
                shellServer.registerTermServer(new HttpTermServer(configure.getIp(), configure.getHttpPort(),
                        options.getConnectionTimeout(), workerGroup, httpSessionManager));
            } else {
                // listen local address in VM communication
                if (configure.getTunnelServer() != null) {
                    shellServer.registerTermServer(new HttpTermServer(configure.getIp(), configure.getHttpPort(),
                            options.getConnectionTimeout(), workerGroup, httpSessionManager));
                }
                logger().info("http port is {}, skip bind http server.", configure.getHttpPort());
            }

            for (CommandResolver resolver : resolvers) {
                shellServer.registerCommandResolver(resolver);
            }

            shellServer.listen(new BindHandler(isBindRef));
            if (!isBind()) {
                throw new IllegalStateException("Arthas failed to bind telnet or http port! Telnet port: "
                        + String.valueOf(configure.getTelnetPort()) + ", http port: "
                        + String.valueOf(configure.getHttpPort()));
            }

            //http api session manager
            sessionManager = new SessionManagerImpl(options, shellServer.getCommandManager(), shellServer.getJobController());
            //http api handler
            httpApiHandler = new HttpApiHandler(historyManager, sessionManager);

            logger().info("as-server listening on network={};telnet={};http={};timeout={};", configure.getIp(),
                    configure.getTelnetPort(), configure.getHttpPort(), options.getConnectionTimeout());

            // 异步回报启动次数
            if (configure.getStatUrl() != null) {
                logger().info("arthas stat url: {}", configure.getStatUrl());
            }
            UserStatUtil.setStatUrl(configure.getStatUrl());
            UserStatUtil.arthasStart();

            try {
                SpyAPI.init();
            } catch (Throwable e) {
                // ignore
            }

            logger().info("as-server started in {} ms", System.currentTimeMillis() - start);
        } catch (Throwable e) {
            logger().error("Error during start as-server", e);
            destroy();
            throw e;
        }
    }

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

    public EnhancerAffect reset() throws UnmodifiableClassException {
        return Enhancer.reset(this.instrumentation, new WildcardMatcher("*"));
    }

    /**
     * call reset() before destroy()
     */
    public void destroy() {
        if (shellServer != null) {
            shellServer.close();
            shellServer = null;
        }
        if (sessionManager != null) {
            sessionManager.close();
            sessionManager = null;
        }
        if (this.httpSessionManager != null) {
            httpSessionManager.stop();
        }
        if (timer != null) {
            timer.cancel();
        }
        if (this.tunnelClient != null) {
            try {
                tunnelClient.stop();
            } catch (Throwable e) {
                logger().error("stop tunnel client error", e);
            }
        }
        if (executorService != null) {
            executorService.shutdownNow();
        }
        if (transformerManager != null) {
            transformerManager.destroy();
        }
        if (classLoaderInstrumentTransformer != null) {
            instrumentation.removeTransformer(classLoaderInstrumentTransformer);
        }
        // clear the reference in Spy class.
        cleanUpSpyReference();
        shutdownWorkGroup();
        UserStatUtil.destroy();
        if (shutdown != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdown);
            } catch (Throwable t) {
                // ignore
            }
        }
        logger().info("as-server destroy completed.");
        if (loggerContext != null) {
            loggerContext.stop();
        }
    }

    /**
     * 单例
     *
     * @param instrumentation JVM增强
     * @return ArthasServer单例
     * @throws Throwable
     */
    public synchronized static ArthasBootstrap getInstance(Instrumentation instrumentation, String args) throws Throwable {
        if (arthasBootstrap != null) {
            return arthasBootstrap;
        }

        Map<String, String> argsMap = FeatureCodec.DEFAULT_COMMANDLINE_CODEC.toMap(args);
        // 给配置全加上前缀
        Map<String, String> mapWithPrefix = new HashMap<String, String>(argsMap.size());
        for (Entry<String, String> entry : argsMap.entrySet()) {
            mapWithPrefix.put("arthas." + entry.getKey(), entry.getValue());
        }
        return getInstance(instrumentation, mapWithPrefix);
    }

    /**
     * 单例
     *
     * @param instrumentation JVM增强
     * @return ArthasServer单例
     * @throws Throwable
     */
    public synchronized static ArthasBootstrap getInstance(Instrumentation instrumentation, Map<String, String> args) throws Throwable {
        if (arthasBootstrap == null) {
            arthasBootstrap = new ArthasBootstrap(instrumentation, args);
        }
        return arthasBootstrap;
    }

    /**
     * @return ArthasServer单例
     */
    public static ArthasBootstrap getInstance() {
        if (arthasBootstrap == null) {
            throw new IllegalStateException("ArthasBootstrap must be initialized before!");
        }
        return arthasBootstrap;
    }

    public void execute(Runnable command) {
        executorService.execute(command);
    }

    /**
     * 清除SpyAPI里的引用
     */
    private void cleanUpSpyReference() {
        try {
            SpyAPI.setNopSpy();
            SpyAPI.destroy();
        } catch (Throwable e) {
            // ignore
        }
        // AgentBootstrap.resetArthasClassLoader();
        try {
            Class<?> clazz = ClassLoader.getSystemClassLoader().loadClass("com.taobao.arthas.agent334.AgentBootstrap");
            Method method = clazz.getDeclaredMethod("resetArthasClassLoader");
            method.invoke(null);
        } catch (Throwable e) {
            // ignore
        }
    }

    public TunnelClient getTunnelClient() {
        return tunnelClient;
    }

    public ShellServer getShellServer() {
        return shellServer;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public Timer getTimer() {
        return this.timer;
    }

    public ScheduledExecutorService getScheduledExecutorService() {
        return this.executorService;
    }

    public Instrumentation getInstrumentation() {
        return this.instrumentation;
    }

    public TransformerManager getTransformerManager() {
        return this.transformerManager;
    }

    private Logger logger() {
        return LoggerFactory.getLogger(this.getClass());
    }

    public ResultViewResolver getResultViewResolver() {
        return resultViewResolver;
    }

    public HistoryManager getHistoryManager() {
        return historyManager;
    }

    public HttpApiHandler getHttpApiHandler() {
        return httpApiHandler;
    }

    public File getOutputPath() {
        return outputPath;
    }

    public SecurityAuthenticator getSecurityAuthenticator() {
        return securityAuthenticator;
    }

}
