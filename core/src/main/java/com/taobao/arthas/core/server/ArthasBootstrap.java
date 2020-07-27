package com.taobao.arthas.core.server;

import java.arthas.SpyAPI;
import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URI;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
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
import com.taobao.arthas.common.AnsiLog;
import com.taobao.arthas.common.PidUtils;
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
import com.taobao.arthas.core.shell.term.impl.httptelnet.HttpTelnetTermServer;
import com.taobao.arthas.core.util.ArthasBanner;
import com.taobao.arthas.core.util.FileUtils;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.UserStatUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;


/**
 * @author vlinux on 15/5/2.
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
    private Thread shutdown;
    private ShellServer shellServer;
    private ScheduledExecutorService executorService;
    private SessionManager sessionManager;
    private TunnelClient tunnelClient;

    private File arthasOutputDir;

    private static LoggerContext loggerContext;
    private EventExecutorGroup workerGroup;

    private Timer timer = new Timer("arthas-timer", true);

    private TransformerManager transformerManager;

    private ResultViewResolver resultViewResolver;

    private HistoryManager historyManager;

    private HttpApiHandler httpApiHandler;

    private ArthasBootstrap(Instrumentation instrumentation, Map<String, String> args) throws Throwable {
        this.instrumentation = instrumentation;

        String outputPath = System.getProperty("arthas.output.dir", "arthas-output");
        arthasOutputDir = new File(outputPath);
        arthasOutputDir.mkdirs();

        // 1. initSpy()
        initSpy(instrumentation);
        // 2. ArthasEnvironment
        initArthasEnvironment(args);
        // 3. init logger
        loggerContext = LogUtil.initLooger(arthasEnvironment);

        // 4. init beans
        initBeans();

        // 5. start agent server
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

    private void initBeans() {
        this.resultViewResolver = new ResultViewResolver();

        this.historyManager = new HistoryManagerImpl();
    }

    private static void initSpy(Instrumentation instrumentation) throws Throwable {
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
        Map<String, String> copyMap = new HashMap<String, String>(argsMap);
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

            PropertySource propertySource = new PropertiesPropertySource(location, properties);
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

        String agentId = null;
        try {
            if (configure.getTunnelServer() != null && configure.getHttpPort() > 0) {
                tunnelClient = new TunnelClient();
                tunnelClient.setId(configure.getAgentId());
                tunnelClient.setTunnelServerUrl(configure.getTunnelServer());
                // ws://127.0.0.1:8563/ws
                String host = "127.0.0.1";
                if(configure.getIp() != null) {
                    host = configure.getIp();
                }
                URI uri = new URI("ws", null, host, configure.getHttpPort(), "/ws", null, null);
                tunnelClient.setLocalServerUrl(uri.toString());
                ChannelFuture channelFuture = tunnelClient.start();
                channelFuture.await(10, TimeUnit.SECONDS);
                if(channelFuture.isSuccess()) {
                    agentId = tunnelClient.getId();
                }
            }
        } catch (Throwable t) {
            logger().error("start tunnel client error", t);
        }

        try {
            ShellServerOptions options = new ShellServerOptions()
                            .setInstrumentation(instrumentation)
                            .setPid(PidUtils.currentLongPid())
                            .setSessionTimeout(configure.getSessionTimeout() * 1000);

            if (agentId != null) {
                Map<String, String> welcomeInfos = new HashMap<String, String>();
                welcomeInfos.put("id", agentId);
                options.setWelcomeMessage(ArthasBanner.welcome(welcomeInfos));
            }

            shellServer = new ShellServerImpl(options, this);
            BuiltinCommandPack builtinCommands = new BuiltinCommandPack();
            List<CommandResolver> resolvers = new ArrayList<CommandResolver>();
            resolvers.add(builtinCommands);

            //worker group
            workerGroup = new NioEventLoopGroup(new DefaultThreadFactory("arthas-TermServer", true));

            // TODO: discover user provided command resolver
            if (configure.getTelnetPort() > 0) {
                shellServer.registerTermServer(new HttpTelnetTermServer(configure.getIp(), configure.getTelnetPort(),
                        options.getConnectionTimeout(), workerGroup));
            } else {
                logger().info("telnet port is {}, skip bind telnet server.", configure.getTelnetPort());
            }
            if (configure.getHttpPort() > 0) {
                shellServer.registerTermServer(new HttpTermServer(configure.getIp(), configure.getHttpPort(),
                        options.getConnectionTimeout(), workerGroup));
            } else {
                logger().info("http port is {}, skip bind http server.", configure.getHttpPort());
            }

            for (CommandResolver resolver : resolvers) {
                shellServer.registerCommandResolver(resolver);
            }

            shellServer.listen(new BindHandler(isBindRef));

            //http api session manager
            sessionManager = new SessionManagerImpl(options, this, shellServer.getCommandManager(), shellServer.getJobController());
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
            logger().error("Error during bind to port " + configure.getTelnetPort(), e);
            if (shellServer != null) {
                shellServer.close();
            }
            if (sessionManager != null){
                sessionManager.close();
            }
            shutdownWorkGroup();
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

    public void destroy() {
        timer.cancel();
        if (this.tunnelClient != null) {
            try {
                tunnelClient.stop();
            } catch (Throwable e) {
                logger().error("stop tunnel client error", e);
            }
        }
        executorService.shutdownNow();
        transformerManager.destroy();
        UserStatUtil.destroy();
        shutdownWorkGroup();
        // clear the reference in Spy class.
        cleanUpSpyReference();
        try {
            Runtime.getRuntime().removeShutdownHook(shutdown);
        } catch (Throwable t) {
            // ignore
        }
        logger().info("as-server destroy completed.");
        if (loggerContext != null) {
            loggerContext.stop();
        }
        shellServer = null;
        sessionManager = null;
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
            e.printStackTrace();
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
}
