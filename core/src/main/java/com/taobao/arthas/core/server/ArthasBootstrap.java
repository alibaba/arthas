package com.taobao.arthas.core.server;

import java.arthas.SpyAPI;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
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
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.taobao.arthas.common.AnsiLog;
import com.taobao.arthas.common.ArthasConstants;
import com.taobao.arthas.common.PidUtils;
import com.taobao.arthas.common.SocketUtils;
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
import com.taobao.arthas.core.shell.command.Command;
import com.taobao.arthas.core.shell.command.CommandRegistry;
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
import com.taobao.arthas.core.util.IPUtils;
import com.taobao.arthas.core.util.InstrumentationUtils;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.UserStatUtil;
import com.taobao.arthas.core.util.affect.EnhancerAffect;
import com.taobao.arthas.core.util.matcher.WildcardMatcher;

import com.taobao.arthas.core.mcp.ArthasMcpBootstrap;
import com.taobao.arthas.mcp.server.CommandExecutor;
import com.taobao.arthas.mcp.server.protocol.server.handler.McpHttpRequestHandler;
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
    private static final String DEFAULT_COMMANDS_DIRECTORY = "commands";
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

    private McpHttpRequestHandler mcpRequestHandler;
    private ArthasMcpBootstrap arthasMcpBootstrap;

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
        loggerContext = LogUtil.initLogger(arthasEnvironment);

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
        // ignore getter error #1661
        // #2081
        JSON.config(JSONWriter.Feature.IgnoreErrorGetter, JSONWriter.Feature.WriteNonStringKeyAsString);
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
        Map<String, Object> copyMap;
        if (argsMap != null) {
            copyMap = new HashMap<String, Object>(argsMap);
            // 添加 arthas.home
            if (!copyMap.containsKey(ARTHAS_HOME_PROPERTY)) {
                copyMap.put(ARTHAS_HOME_PROPERTY, arthasHome());
            }
        } else {
            copyMap = new HashMap<String, Object>(1);
            copyMap.put(ARTHAS_HOME_PROPERTY, arthasHome());
        }

        MapPropertySource mapPropertySource = new MapPropertySource("args", copyMap);
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

    static String reslove(ArthasEnvironment arthasEnvironment, String key, String defaultValue) {
        String value = arthasEnvironment.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return arthasEnvironment.resolvePlaceholders(value);
    }

    // try to load arthas.properties
    private void tryToLoadArthasProperties() throws IOException {
        this.arthasEnvironment.resolvePlaceholders(CONFIG_LOCATION_PROPERTY);

        String location = reslove(arthasEnvironment, CONFIG_LOCATION_PROPERTY, null);

        if (location == null) {
            location = arthasHome();
        }

        String configName = reslove(arthasEnvironment, CONFIG_NAME_PROPERTY, "arthas");

        if (location != null) {
            if (!location.endsWith(".properties")) {
                location = new File(location, configName + ".properties").getAbsolutePath();
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

            this.securityAuthenticator = new SecurityAuthenticatorImpl(configure.getUsername(), configure.getPassword());

            shellServer = new ShellServerImpl(options);

            List<String> disabledCommands = new ArrayList<String>();
            if (configure.getDisabledCommands() != null) {
                String[] strings = StringUtils.tokenizeToStringArray(configure.getDisabledCommands(), ",");
                if (strings != null) {
                    disabledCommands.addAll(Arrays.asList(strings));
                }
            }
            BuiltinCommandPack builtinCommands = new BuiltinCommandPack(disabledCommands);
            List<CommandResolver> resolvers = new ArrayList<CommandResolver>();
            CommandResolver externalCommands = loadExternalCommandResolver(shellServer, builtinCommands);
            if (externalCommands != null) {
                resolvers.add(externalCommands);
            }
            resolvers.add(builtinCommands);

            //worker group
            workerGroup = new NioEventLoopGroup(new DefaultThreadFactory("arthas-TermServer", true));

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

            // Mcp Server
            String mcpEndpoint = configure.getMcpEndpoint();
            String mcpProtocol = configure.getMcpProtocol();
            if (mcpEndpoint != null && !mcpEndpoint.trim().isEmpty()) {
                logger().info("try to start mcp server, endpoint: {}, protocol: {}.", mcpEndpoint, mcpProtocol);
                CommandExecutor commandExecutor = new CommandExecutorImpl(sessionManager);
                this.arthasMcpBootstrap = new ArthasMcpBootstrap(commandExecutor, mcpEndpoint, mcpProtocol);
                this.mcpRequestHandler = this.arthasMcpBootstrap.start().getMcpRequestHandler();
            }
            logger().info("as-server listening on network={};telnet={};http={};timeout={};mcp={};mcpProtocol={};", configure.getIp(),
                    configure.getTelnetPort(), configure.getHttpPort(), options.getConnectionTimeout(), configure.getMcpEndpoint(), configure.getMcpProtocol());

            // 异步回报启动次数
            if (configure.getStatUrl() != null) {
                logger().info("arthas stat url: {}", configure.getStatUrl());
            }
            UserStatUtil.setStatUrl(configure.getStatUrl());
            UserStatUtil.setAgentId(configure.getAgentId());
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

    private CommandResolver loadExternalCommandResolver(ShellServer shellServer, BuiltinCommandPack builtinCommands)
                    throws Throwable {
        String arthasHome = reslove(arthasEnvironment, ARTHAS_HOME_PROPERTY, arthasHome());
        String commandLocationSummary = describeCommandLocations(configure.getCommandLocations(), arthasHome);
        List<URL> commandUrls = resolveCommandLocationUrls(configure.getCommandLocations(), arthasHome, logger());
        if (commandUrls.isEmpty()) {
            return null;
        }

        ClassLoader arthasClassLoader = ArthasBootstrap.class.getClassLoader();
        appendCommandUrls(arthasClassLoader, commandUrls);

        List<CommandResolver> externalResolvers = loadExternalCommandResolvers(arthasClassLoader, logger());
        if (externalResolvers.isEmpty()) {
            logger().warn("No external arthas command resolvers found from command locations: {}", commandLocationSummary);
            return null;
        }

        List<CommandResolver> reservedResolvers = new ArrayList<CommandResolver>();
        reservedResolvers.addAll(shellServer.getCommandManager().getResolvers());
        reservedResolvers.add(builtinCommands);

        CommandRegistry externalRegistry = createExternalCommandRegistry(reservedResolvers, externalResolvers, logger());
        if (externalRegistry == null) {
            logger().warn("No external arthas commands loaded from command locations: {}", commandLocationSummary);
            return null;
        }

        logger().info("Loaded {} external arthas commands from {} resolver(s), locations: {}.",
                        externalRegistry.commands().size(), externalResolvers.size(), commandLocationSummary);
        return externalRegistry;
    }

    static List<URL> resolveCommandLocationUrls(String commandLocations, Logger logger) throws IOException {
        return resolveCommandLocationUrls(commandLocations, arthasHome(), logger);
    }

    static List<URL> resolveCommandLocationUrls(String commandLocations, String arthasHome, Logger logger)
                    throws IOException {
        List<CommandLocation> locations = collectCommandLocations(commandLocations, arthasHome);
        if (locations.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, URL> commandUrls = new LinkedHashMap<String, URL>();
        for (CommandLocation location : locations) {
            File file = new File(location.path());
            if (!file.exists()) {
                logger.warn("Skip arthas external command location because it does not exist: {}", location.path());
                continue;
            }

            if (file.isDirectory()) {
                File[] jarFiles = file.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return pathname.isFile() && isCommandJar(pathname);
                    }
                });
                if (jarFiles == null || jarFiles.length == 0) {
                    continue;
                }
                Arrays.sort(jarFiles, new Comparator<File>() {
                    @Override
                    public int compare(File left, File right) {
                        return left.getName().compareTo(right.getName());
                    }
                });
                for (File jarFile : jarFiles) {
                    addCommandUrl(commandUrls, jarFile);
                }
            } else if (file.isFile() && isCommandJar(file)) {
                addCommandUrl(commandUrls, file);
            } else {
                logger.warn("Skip arthas external command location because it is not a jar file or directory: {}",
                                location.path());
            }
        }

        return new ArrayList<URL>(commandUrls.values());
    }

    private static List<CommandLocation> collectCommandLocations(String commandLocations, String arthasHome) {
        List<CommandLocation> locations = new ArrayList<CommandLocation>();
        String[] configuredLocations = StringUtils.tokenizeToStringArray(commandLocations, ",");
        if (configuredLocations != null && configuredLocations.length > 0) {
            for (String configuredLocation : configuredLocations) {
                locations.add(new CommandLocation(configuredLocation, false));
            }
        }

        File defaultCommandsDirectory = resolveDefaultCommandsDirectory(arthasHome);
        if (defaultCommandsDirectory != null) {
            locations.add(new CommandLocation(defaultCommandsDirectory.getAbsolutePath(), true));
        }

        return locations;
    }

    private static String describeCommandLocations(String commandLocations, String arthasHome) {
        List<String> descriptions = new ArrayList<String>();
        for (CommandLocation location : collectCommandLocations(commandLocations, arthasHome)) {
            descriptions.add(location.description());
        }

        if (descriptions.isEmpty()) {
            return "[]";
        }
        return descriptions.toString();
    }

    private static class CommandLocation {
        private final String path;
        private final boolean defaultLocation;

        CommandLocation(String path, boolean defaultLocation) {
            this.path = path;
            this.defaultLocation = defaultLocation;
        }

        String path() {
            return path;
        }

        String description() {
            if (defaultLocation) {
                return path + " (default)";
            }
            return path;
        }
    }

    private static File resolveDefaultCommandsDirectory(String arthasHome) {
        if (!StringUtils.hasText(arthasHome)) {
            return null;
        }

        File commandsDirectory = new File(arthasHome, DEFAULT_COMMANDS_DIRECTORY);
        if (commandsDirectory.isDirectory()) {
            return commandsDirectory;
        }
        return null;
    }

    static void appendCommandUrls(ClassLoader classLoader, List<URL> commandUrls) throws Throwable {
        if (commandUrls == null || commandUrls.isEmpty()) {
            return;
        }

        Method appendURLMethod;
        try {
            appendURLMethod = classLoader.getClass().getMethod("appendURL", URL.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Current Arthas ClassLoader does not support appendURL: "
                            + classLoader.getClass().getName(), e);
        }

        for (URL commandUrl : commandUrls) {
            appendURLMethod.invoke(classLoader, commandUrl);
        }
    }

    static List<CommandResolver> loadExternalCommandResolvers(ClassLoader classLoader, Logger logger) {
        List<CommandResolver> resolvers = new ArrayList<CommandResolver>();
        Iterator<CommandResolver> iterator = ServiceLoader.load(CommandResolver.class, classLoader).iterator();
        while (true) {
            try {
                if (!iterator.hasNext()) {
                    break;
                }
                resolvers.add(iterator.next());
            } catch (ServiceConfigurationError error) {
                logger.error("Load external arthas command resolver error", error);
            }
        }
        return resolvers;
    }

    static CommandRegistry createExternalCommandRegistry(List<CommandResolver> reservedResolvers,
                    List<CommandResolver> externalResolvers, Logger logger) {
        Set<String> reservedNames = collectCommandNames(reservedResolvers);
        Set<String> externalNames = new HashSet<String>();
        CommandRegistry registry = CommandRegistry.create();

        for (CommandResolver resolver : externalResolvers) {
            List<Command> commands = resolver.commands();
            if (commands == null || commands.isEmpty()) {
                continue;
            }

            for (Command command : commands) {
                if (command == null) {
                    continue;
                }

                String commandName;
                try {
                    commandName = command.name();
                } catch (Throwable t) {
                    logger.warn("Skip external arthas command because command.name() throws exception, resolver: {} ({})",
                                    resolver.getClass().getName(), codeSourceLocation(resolver.getClass()), t);
                    continue;
                }

                if (!StringUtils.hasText(commandName)) {
                    logger.warn("Skip external arthas command because command name is blank, resolver: {} ({})",
                                    resolver.getClass().getName(), codeSourceLocation(resolver.getClass()));
                    continue;
                }

                if (reservedNames.contains(commandName)) {
                    logger.warn("Skip external arthas command `{}` from resolver {} ({}) because the name is reserved.",
                                    commandName, resolver.getClass().getName(), codeSourceLocation(resolver.getClass()));
                    continue;
                }

                if (!externalNames.add(commandName)) {
                    logger.warn("Skip external arthas command `{}` from resolver {} ({}) because the name is duplicated.",
                                    commandName, resolver.getClass().getName(), codeSourceLocation(resolver.getClass()));
                    continue;
                }

                registry.registerCommand(command);
            }
        }

        if (registry.commands().isEmpty()) {
            return null;
        }
        return registry;
    }

    private static boolean isCommandJar(File file) {
        String fileName = file.getName();
        return fileName.length() >= 4 && fileName.regionMatches(true, fileName.length() - 4, ".jar", 0, 4);
    }

    private static void addCommandUrl(Map<String, URL> commandUrls, File jarFile) throws IOException {
        File canonicalFile = jarFile.getCanonicalFile();
        String filePath = canonicalFile.getAbsolutePath();
        if (!commandUrls.containsKey(filePath)) {
            commandUrls.put(filePath, canonicalFile.toURI().toURL());
        }
    }

    private static Set<String> collectCommandNames(List<CommandResolver> resolvers) {
        Set<String> names = new HashSet<String>();
        if (resolvers == null || resolvers.isEmpty()) {
            return names;
        }

        for (CommandResolver resolver : resolvers) {
            if (resolver == null) {
                continue;
            }
            List<Command> commands = resolver.commands();
            if (commands == null || commands.isEmpty()) {
                continue;
            }
            for (Command command : commands) {
                if (command == null) {
                    continue;
                }
                try {
                    String name = command.name();
                    if (StringUtils.hasText(name)) {
                        names.add(name);
                    }
                } catch (Throwable t) {
                    // ignore
                }
            }
        }
        return names;
    }

    private static String codeSourceLocation(Class<?> type) {
        try {
            CodeSource codeSource = type.getProtectionDomain().getCodeSource();
            if (codeSource != null && codeSource.getLocation() != null) {
                return codeSource.getLocation().toString();
            }
        } catch (Throwable t) {
            // ignore
        }
        return "unknown";
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

    public McpHttpRequestHandler getMcpRequestHandler() {
        return mcpRequestHandler;
    }

    public File getOutputPath() {
        return outputPath;
    }

    public SecurityAuthenticator getSecurityAuthenticator() {
        return securityAuthenticator;
    }

    public Configure getConfigure() {
        return configure;
    }
}
