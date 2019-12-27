package com.taobao.arthas.core.server;

import com.taobao.arthas.core.config.Configure;
import com.alibaba.arthas.tunnel.client.TunnelClient;
import com.taobao.arthas.common.PidUtils;
import com.taobao.arthas.core.advisor.AdviceWeaver;
import com.taobao.arthas.core.command.BuiltinCommandPack;
import com.taobao.arthas.core.shell.ShellServer;
import com.taobao.arthas.core.shell.ShellServerOptions;
import com.taobao.arthas.core.shell.command.CommandResolver;
import com.taobao.arthas.core.shell.handlers.BindHandler;
import com.taobao.arthas.core.shell.impl.ShellServerImpl;
import com.taobao.arthas.core.shell.term.impl.HttpTermServer;
import com.taobao.arthas.core.shell.term.impl.httptelnet.HttpTelnetTermServer;
import com.taobao.arthas.core.util.ArthasBanner;
import com.taobao.arthas.core.util.Constants;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.UserStatUtil;
import com.taobao.middleware.logger.Logger;

import io.netty.channel.ChannelFuture;

import java.arthas.Spy;
import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * @author vlinux on 15/5/2.
 */
public class ArthasBootstrap {

    private static Logger logger = LogUtil.getArthasLogger();
    private static ArthasBootstrap arthasBootstrap;

    private AtomicBoolean isBindRef = new AtomicBoolean(false);
    private Instrumentation instrumentation;
    private Thread shutdown;
    private ShellServer shellServer;
    private ExecutorService executorService;
    private TunnelClient tunnelClient;

    private File arthasOutputDir;

    private ArthasBootstrap(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;

        String outputPath = System.getProperty("arthas.output.dir", "arthas-output");
        arthasOutputDir = new File(outputPath);
        arthasOutputDir.mkdirs();

        executorService = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                final Thread t = new Thread(r, "as-command-execute-daemon");
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

        Runtime.getRuntime().addShutdownHook(shutdown);
    }

    private static void initSpy() throws ClassNotFoundException, NoSuchMethodException {
        Class<?> adviceWeaverClass = AdviceWeaver.class;
        Method onBefore = adviceWeaverClass.getMethod(AdviceWeaver.ON_BEFORE, int.class, ClassLoader.class, String.class,
                String.class, String.class, Object.class, Object[].class);
        Method onReturn = adviceWeaverClass.getMethod(AdviceWeaver.ON_RETURN, Object.class);
        Method onThrows = adviceWeaverClass.getMethod(AdviceWeaver.ON_THROWS, Throwable.class);
        Method beforeInvoke = adviceWeaverClass.getMethod(AdviceWeaver.BEFORE_INVOKE, int.class, String.class, String.class, String.class, int.class);
        Method afterInvoke = adviceWeaverClass.getMethod(AdviceWeaver.AFTER_INVOKE, int.class, String.class, String.class, String.class, int.class);
        Method throwInvoke = adviceWeaverClass.getMethod(AdviceWeaver.THROW_INVOKE, int.class, String.class, String.class, String.class, int.class);
        Spy.init(AdviceWeaver.class.getClassLoader(), onBefore, onReturn, onThrows, beforeInvoke, afterInvoke, throwInvoke);
    }
    
    public void bind(String args) throws Throwable {
        initSpy();
        Configure configure = Configure.toConfigure(args);
        bind(configure);
    }

    /**
     * Bootstrap arthas server
     *
     * @param configure 配置信息
     * @throws IOException 服务器启动失败
     */
    public void bind(Configure configure) throws Throwable {

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
            logger.error("arthas", "start tunnel client error", t);
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
            // TODO: discover user provided command resolver
            if (configure.getTelnetPort() > 0) {
                shellServer.registerTermServer(new HttpTelnetTermServer(configure.getIp(), configure.getTelnetPort(),
                                options.getConnectionTimeout()));
            } else {
                logger.info("telnet port is {}, skip bind telnet server.", configure.getTelnetPort());
            }
            if (configure.getHttpPort() > 0) {
                shellServer.registerTermServer(new HttpTermServer(configure.getIp(), configure.getHttpPort(),
                                options.getConnectionTimeout()));
            } else {
                logger.info("http port is {}, skip bind http server.", configure.getHttpPort());
            }

            for (CommandResolver resolver : resolvers) {
                shellServer.registerCommandResolver(resolver);
            }

            shellServer.listen(new BindHandler(isBindRef));

            logger.info("as-server listening on network={};telnet={};http={};timeout={};", configure.getIp(),
                    configure.getTelnetPort(), configure.getHttpPort(), options.getConnectionTimeout());
            // 异步回报启动次数
            if (configure.getStatUrl() != null) {
                logger.info("arthas stat url: {}", configure.getStatUrl());
            }
            UserStatUtil.setStatUrl(configure.getStatUrl());
            UserStatUtil.arthasStart();

            logger.info("as-server started in {} ms", System.currentTimeMillis() - start );
        } catch (Throwable e) {
            logger.error(null, "Error during bind to port " + configure.getTelnetPort(), e);
            if (shellServer != null) {
                shellServer.close();
            }
            throw e;
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
        if (this.tunnelClient != null) {
            try {
                tunnelClient.stop();
            } catch (Throwable e) {
                logger.error("arthas", "stop tunnel client error", e);
            }
        }
        executorService.shutdownNow();
        UserStatUtil.destroy();
        // clear the reference in Spy class.
        cleanUpSpyReference();
        try {
            Runtime.getRuntime().removeShutdownHook(shutdown);
        } catch (Throwable t) {
            // ignore
        }
        logger.info("as-server destroy completed.");
        // see https://github.com/alibaba/arthas/issues/319
        LogUtil.closeResultLogger();
    }

    /**
     * 单例
     *
     * @param instrumentation JVM增强
     * @return ArthasServer单例
     */
    public synchronized static ArthasBootstrap getInstance(Instrumentation instrumentation) {
        if (arthasBootstrap == null) {
            arthasBootstrap = new ArthasBootstrap(instrumentation);
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
     * 清除spy中对classloader的引用，避免内存泄露
     */
    private void cleanUpSpyReference() {
        try {
            // 从ArthasClassLoader中加载Spy
            Class<?> spyClass = this.getClass().getClassLoader().loadClass(Constants.SPY_CLASSNAME);
            Method agentDestroyMethod = spyClass.getMethod("destroy");
            agentDestroyMethod.invoke(null);
        } catch (ClassNotFoundException e) {
            logger.error(null, "Spy load failed from ArthasClassLoader, which should not happen", e);
        } catch (Exception e) {
            logger.error(null, "Spy destroy failed: ", e);
        }
    }

    public TunnelClient getTunnelClient() {
        return tunnelClient;
    }
}
