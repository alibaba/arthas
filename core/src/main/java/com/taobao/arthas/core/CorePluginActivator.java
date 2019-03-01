package com.taobao.arthas.core;

import java.arthas.Spy;
import java.io.File;
import java.lang.reflect.Method;
import java.util.jar.JarFile;

import com.taobao.arthas.common.PidUtils;
import com.taobao.arthas.core.advisor.AdviceWeaver;
import com.taobao.arthas.core.config.Configure;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.plugin.PluginActivator;
import com.taobao.arthas.plugin.PluginContext;

/**
 *
 * @author hengyunabc 2019-03-01
 *
 */
public class CorePluginActivator implements PluginActivator {
    private static final String ON_BEFORE = "methodOnBegin";
    private static final String ON_RETURN = "methodOnReturnEnd";
    private static final String ON_THROWS = "methodOnThrowingEnd";
    private static final String BEFORE_INVOKE = "methodOnInvokeBeforeTracing";
    private static final String AFTER_INVOKE = "methodOnInvokeAfterTracing";
    private static final String THROW_INVOKE = "methodOnInvokeThrowTracing";
    private static final String RESET = "resetArthasClassLoader";

    @Override
    public boolean enabled(PluginContext context) {
        return true;
    }

    @Override
    public void start(PluginContext context) throws Exception {
        // TODO arthas-spy.jar 是应该在插件里，还是在公共的地方？
        String arthasHome = context.getProperty("arthas.home");

        File arthasSpyFile = new File(arthasHome, "arthas-spy.jar");
     // 将Spy添加到BootstrapClassLoader
        context.getInstrumentation().appendToBootstrapClassLoaderSearch(new JarFile(arthasSpyFile));
        initSpy();

        ArthasBootstrap bootstrap = ArthasBootstrap.getInstance(Integer.parseInt(PidUtils.currentPid()), context.getInstrumentation());

        String args = "telnetPort=3658;httpPort=8563;ip=127.0.0.1;sessionTimeout=1800";
        Configure configure = Configure.toConfigure(args);
        try {
            bootstrap.bind(configure);
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private void initSpy() throws NoSuchMethodException, SecurityException {
        Class<?> adviceWeaverClass = AdviceWeaver.class;
        Method onBefore = adviceWeaverClass.getMethod(ON_BEFORE, int.class, ClassLoader.class, String.class,
                String.class, String.class, Object.class, Object[].class);
        Method onReturn = adviceWeaverClass.getMethod(ON_RETURN, Object.class);
        Method onThrows = adviceWeaverClass.getMethod(ON_THROWS, Throwable.class);
        Method beforeInvoke = adviceWeaverClass.getMethod(BEFORE_INVOKE, int.class, String.class, String.class, String.class);
        Method afterInvoke = adviceWeaverClass.getMethod(AFTER_INVOKE, int.class, String.class, String.class, String.class);
        Method throwInvoke = adviceWeaverClass.getMethod(THROW_INVOKE, int.class, String.class, String.class, String.class);

        Method reset = null;
        Spy.initForAgentLauncher(CorePluginActivator.class.getClassLoader(), onBefore, onReturn, onThrows, beforeInvoke, afterInvoke, throwInvoke, reset);
    }

    @Override
    public void stop(PluginContext context) throws Exception {

    }

}
