package com.taobao.arthas.agent334;

import java.arthas.SpyAPI;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLDecoder;
import java.security.CodeSource;

import com.taobao.arthas.agent.ArthasClassloader;
import net.bytebuddy.agent.ByteBuddyAgent;

/**
 * 代理启动类
 *arthas-agent.jar
 * 既可以使用premain方式（在目标进程启动之前，通过-agent参数静态指定），
 *      java -javaagent:MyAgent2.jar=thisIsAgentArgs -jar MyProgram.jar
 *      比如 java -javaagent:C:\Users\zhanghongjun\.arthas\lib\3.3.9\arthas\arthas-agent.jar=C:\Users\zhanghongjun\.arthas\lib\3.3.9\arthas\arthas-core.jar;;telnetPort=3659;httpPort=8564;ip=127.0.0.1;arthasAgent=C:\Users\zhanghongjun\.arthas\lib\3.3.9\arthas\arthas-agent.jar;sessionTimeout=1800;arthasCore=C:\Users\zhanghongjun\.arthas\lib\3.3.9\arthas\arthas-core.jar;javaPid=31860;
 *      要想本地调试，只要命名行正常如上启动，然后工程中有arthas代码(通过mvn引入)，对arthas打上端点即可(因为调试的本质就是通过jdwp向目标jvm进程发送些调试点，本地调试在启动时会将所有端点通过jdwp进行设置，执行到断点时，会回调给被调试进程(比如idea，它当然可以找到代码的位置，然后逐步调试))
 * 也可以通过agentmain方式（在进程启动之后attach上去）。
 *      {@link Arthas#attachAgent}
 * @author vlinux on 15/5/19.
 */
public class AgentBootstrap {
    private static final String ARTHAS_CORE_JAR = "arthas-core.jar";
    private static final String ARTHAS_BOOTSTRAP = "com.taobao.arthas.core.server.ArthasBootstrap";
    private static final String GET_INSTANCE = "getInstance";
    private static final String IS_BIND = "isBind";

    private static PrintStream ps = System.err;
    static {
        try {
            File arthasLogDir = new File(System.getProperty("user.home") + File.separator + "logs" + File.separator
                    + "arthas" + File.separator);
            if (!arthasLogDir.exists()) {
                arthasLogDir.mkdirs();
            }
            if (!arthasLogDir.exists()) {
                // #572
                arthasLogDir = new File(System.getProperty("java.io.tmpdir") + File.separator + "logs" + File.separator
                        + "arthas" + File.separator);
                if (!arthasLogDir.exists()) {
                    arthasLogDir.mkdirs();
                }
            }

            File log = new File(arthasLogDir, "arthas.log");

            if (!log.exists()) {
                log.createNewFile();
            }
            ps = new PrintStream(new FileOutputStream(log, true));
        } catch (Throwable t) {
            t.printStackTrace(ps);
        }
    }

    /**
     * <pre>
     * 1. 全局持有classloader用于隔离 Arthas 实现，防止多次attach重复初始化
     * 2. ClassLoader在arthas停止时会被reset
     * 3. 如果ClassLoader一直没变，则 com.taobao.arthas.core.server.ArthasBootstrap#getInstance 返回结果一直是一样的
     * 4 异步调用bind方法，该方法最终启动server监听线程，监听客户端的连接，包括telnet和websocket两种通信方式
     * </pre>
     */
    private static volatile ClassLoader arthasClassLoader;
    //附着方式1
    public static void premain(String args, Instrumentation inst) {
        main(args, inst);
    }
    //附着方式2
    public static void agentmain(String args, Instrumentation inst) {
        main(args, inst);
    }

    /**
     * 让下次再次启动时有机会重新加载
     */
    public synchronized static void resetArthasClassLoader() {
        arthasClassLoader = null;
    }

    private static ClassLoader getClassLoader(Instrumentation inst, File arthasCoreJarFile) throws Throwable {
        // 构造自定义的类加载器，尽量减少Arthas对现有工程的侵蚀
        return loadOrDefineClassLoader(arthasCoreJarFile);
    }

    private static ClassLoader loadOrDefineClassLoader(File arthasCoreJarFile) throws Throwable {
        if (arthasClassLoader == null) {
            arthasClassLoader = new ArthasClassloader(new URL[]{arthasCoreJarFile.toURI().toURL()});
        }
        return arthasClassLoader;
    }

    /**
     * 1 找到arthas-spy.jar路径，并调用Instrumentation#appendToBootstrapClassLoaderSearch方法，使用bootstrapClassLoader来加载arthas-spy.jar里的Spy类。
     * 2 arthas-agent路径传递给自定义的classloader(ArthasClassloader)，用来隔离arthas本身的类和目标进程的类。
     * 3 使用 ArthasClassloader#loadClass方法，加载com.taobao.arthas.core.advisor.AdviceWeaver类，并将里面的methodOnBegin、methodOnReturnEnd、methodOnThrowingEnd等方法取出赋值给Spy类对应的方法。同时Spy类里面的方法又会通过ASM字节码增强的方式，编织到目标代码的方法里面。使得Spy 间谍类可以关联由AppClassLoader加载的目标进程的业务类和ArthasClassloader加载的arthas类，因此Spy类可以看做两者之间的桥梁。根据classloader双亲委派特性，子classloader可以访问父classloader加载的类
     *
     * @param args
     * @param inst
     */
    private static synchronized void main(String args, final Instrumentation inst) {
        // 尝试判断arthas是否已在运行，如果是的话，直接就退出
        try {
            Class.forName("java.arthas.SpyAPI"); // 加载不到会抛异常
            if (SpyAPI.isInited()) {
                ps.println("Arthas server already stared, skip attach.");
                ps.flush();
                return;
            }
        } catch (Throwable e) {
            // ignore
        }
        try {
            ps.println("Arthas server agent start...");
            // 传递的args参数分两个部分:arthasCoreJar路径和agentArgs, 分别是Agent的JAR包路径和期望传递到服务端的参数
            if (args == null) {
                args = "";
            }
            args = decodeArg(args);

            String arthasCoreJar;
            final String agentArgs;
            int index = args.indexOf(';');
            if (index != -1) {
                arthasCoreJar = args.substring(0, index);
                agentArgs = args.substring(index);
            } else {
                arthasCoreJar = "";
                agentArgs = args;
            }

            File arthasCoreJarFile = new File(arthasCoreJar);
            if (!arthasCoreJarFile.exists()) {
                ps.println("Can not find arthas-core jar file from args: " + arthasCoreJarFile);
                // try to find from arthas-agent.jar directory
                CodeSource codeSource = AgentBootstrap.class.getProtectionDomain().getCodeSource();
                if (codeSource != null) {
                    try {
                        File arthasAgentJarFile = new File(codeSource.getLocation().toURI().getSchemeSpecificPart());
                        arthasCoreJarFile = new File(arthasAgentJarFile.getParentFile(), ARTHAS_CORE_JAR);
                        if (!arthasCoreJarFile.exists()) {
                            ps.println("Can not find arthas-core jar file from agent jar directory: " + arthasAgentJarFile);
                        }
                    } catch (Throwable e) {
                        ps.println("Can not find arthas-core jar file from " + codeSource.getLocation());
                        e.printStackTrace(ps);
                    }
                }
            }
            if (!arthasCoreJarFile.exists()) {
                return;
            }
            //#### 手动将core加载到
            /**
             * Use a dedicated thread to run the binding logic to prevent possible memory leak. #195
             */
            final ClassLoader agentLoader = getClassLoader(inst, arthasCoreJarFile);

            Thread bindingThread = new Thread() {
                @Override
                public void run() {
                    try {
                        bind(inst, agentLoader, agentArgs);
                    } catch (Throwable throwable) {
                        throwable.printStackTrace(ps);
                    }
                }
            };

            bindingThread.setName("arthas-binding-thread");
            bindingThread.start();
            bindingThread.join();
        } catch (Throwable t) {
            t.printStackTrace(ps);
            try {
                if (ps != System.err) {
                    ps.close();
                }
            } catch (Throwable tt) {
                // ignore
            }
            throw new RuntimeException(t);
        }
    }

    private static void bind(Instrumentation inst, ClassLoader agentLoader, String args) throws Throwable {
        /**
         * <pre>
         * ArthasBootstrap bootstrap = ArthasBootstrap.getInstance(inst);
         * </pre>
         */
        Class<?> bootstrapClass = agentLoader.loadClass(ARTHAS_BOOTSTRAP);
        Object bootstrap = bootstrapClass.getMethod(GET_INSTANCE, Instrumentation.class, String.class).invoke(null, inst, args);
        boolean isBind = (Boolean) bootstrapClass.getMethod(IS_BIND).invoke(bootstrap);
        if (!isBind) {
            String errorMsg = "Arthas server port binding failed! Please check $HOME/logs/arthas/arthas.log for more details.";
            ps.println(errorMsg);
            throw new RuntimeException(errorMsg);
        }
        ps.println("Arthas server already bind.");
    }

    private static String decodeArg(String arg) {
        try {
            return URLDecoder.decode(arg, "utf-8");
        } catch (UnsupportedEncodingException e) {
            return arg;
        }
    }

    /**
     * 模拟代理启动  参考core包中{@link Arthas#attachAgent(Configure configure)}
     * @param args
     */
//    public static void main(String[] args) {
//        String arg="C:\\Users\\zhanghongjun\\.arthas\\lib\\3.3.9\\arthas\\arthas-core.jar;;telnetPort=3659;httpPort=8564;ip=127.0.0.1;arthasAgent=C:\\Users\\zhanghongjun\\.arthas\\lib\\3.3.9\\arthas\\arthas-agent.jar;sessionTimeout=1800;arthasCore=C:\\Users\\zhanghongjun\\.arthas\\lib\\3.3.9\\arthas\\arthas-core.jar;javaPid=31860;";
//        main(arg, ByteBuddyAgent.install());
//    }
}
