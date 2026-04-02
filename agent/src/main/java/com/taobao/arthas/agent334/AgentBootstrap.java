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

/**
 * Arthas代理启动类
 *
 * <p>这个类是Arthas Java Agent的入口点，负责：
 * <ul>
 *   <li>在应用启动时(premain)或运行时(agentmain)加载Arthas</li>
 *   <li>创建隔离的类加载器以避免与应用类冲突</li>
 *   <li>初始化Arthas核心服务并绑定到指定端口</li>
 *   <li>防止重复attach导致的多次初始化</li>
 * </ul>
 *
 * @author vlinux on 15/5/19.
 */
public class AgentBootstrap {
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
     * 用于输出日志的PrintStream对象
     * 默认指向System.err，初始化成功后会重定向到日志文件
     */
    private static PrintStream ps = System.err;

    /**
     * 静态初始化块
     *
     * <p>在类加载时执行，负责设置Arthas的日志输出：
     * <ul>
     *   <li>优先在用户主目录下创建 logs/arthas/ 目录</li>
     *   <li>如果失败，则在系统临时目录下创建 logs/arthas/ 目录</li>
     *   <li>创建arthas.log文件用于记录日志</li>
     *   <li>将输出流重定向到日志文件</li>
     * </ul>
     */
    static {
        try {
            // 尝试在用户主目录下创建日志目录
            File arthasLogDir = new File(System.getProperty("user.home") + File.separator + "logs" + File.separator
                    + "arthas" + File.separator);
            if (!arthasLogDir.exists()) {
                arthasLogDir.mkdirs();
            }
            // 如果用户主目录下创建失败，尝试在系统临时目录下创建（问题#572的解决方案）
            if (!arthasLogDir.exists()) {
                // #572
                arthasLogDir = new File(System.getProperty("java.io.tmpdir") + File.separator + "logs" + File.separator
                        + "arthas" + File.separator);
                if (!arthasLogDir.exists()) {
                    arthasLogDir.mkdirs();
                }
            }

            // 创建日志文件
            File log = new File(arthasLogDir, "arthas.log");

            if (!log.exists()) {
                log.createNewFile();
            }
            // 创建以追加模式写入文件的PrintStream
            ps = new PrintStream(new FileOutputStream(log, true));
        } catch (Throwable t) {
            // 如果日志初始化失败，打印到System.err
            t.printStackTrace(ps);
        }
    }

    /**
     * Arthas类加载器的静态引用
     *
     * <p>这个类加载器的设计具有以下特点：
     * <pre>
     * 1. 全局持有classloader用于隔离 Arthas 实现，防止多次attach重复初始化
     * 2. ClassLoader在arthas停止时会被reset
     * 3. 如果ClassLoader一直没变，则 com.taobao.arthas.core.server.ArthasBootstrap#getInstance 返回结果一直是一样的
     * </pre>
     *
     * <p>使用volatile关键字确保多线程环境下的可见性
     */
    private static volatile ClassLoader arthasClassLoader;

    /**
     * Java Agent的premain入口方法
     *
     * <p>在应用启动时（JVM启动时）被调用，用于在应用启动前加载Arthas
     *
     * @param args 代理参数，包含arthas-core.jar路径和配置参数
     * @param inst Instrumentation实例，用于类转换和重定义
     */
    public static void premain(String args, Instrumentation inst) {
        main(args, inst);
    }

    /**
     * Java Agent的agentmain入口方法
     *
     * <p>在应用运行时（动态attach）被调用，用于在运行中的应用中加载Arthas
     *
     * @param args 代理参数，包含arthas-core.jar路径和配置参数
     * @param inst Instrumentation实例，用于类转换和重定义
     */
    public static void agentmain(String args, Instrumentation inst) {
        main(args, inst);
    }

    /**
     * 重置Arthas类加载器
     *
     * <p>将类加载器引用置为null，让下次再次启动时有机会重新加载
     * 通常在Arthas停止时调用，以便下次attach时可以重新初始化
     */
    public static void resetArthasClassLoader() {
        arthasClassLoader = null;
    }

    /**
     * 获取Arthas专用的类加载器
     *
     * <p>构造自定义的类加载器，尽量减少Arthas对现有工程的侵蚀
     * 通过隔离的类加载器，避免Arthas的类与应用类发生冲突
     *
     * @param inst Instrumentation实例（当前未使用，保留用于未来扩展）
     * @param arthasCoreJarFile arthas-core.jar文件
     * @return 配置好的Arthas类加载器
     * @throws Throwable 如果创建类加载器失败
     */
    private static ClassLoader getClassLoader(Instrumentation inst, File arthasCoreJarFile) throws Throwable {
        // 构造自定义的类加载器，尽量减少Arthas对现有工程的侵蚀
        return loadOrDefineClassLoader(arthasCoreJarFile);
    }

    /**
     * 加载或定义Arthas类加载器
     *
     * <p>使用单例模式确保只有一个Arthas类加载器实例
     * 如果类加载器还未创建，则创建一个新的；否则返回已有的实例
     *
     * @param arthasCoreJarFile arthas-core.jar文件
     * @return Arthas类加载器实例
     * @throws Throwable 如果创建类加载器失败
     */
    private static ClassLoader loadOrDefineClassLoader(File arthasCoreJarFile) throws Throwable {
        if (arthasClassLoader == null) {
            // 创建新的Arthas类加载器，使用arthas-core.jar作为类路径
            arthasClassLoader = new ArthasClassloader(new URL[]{arthasCoreJarFile.toURI().toURL()});
        }
        return arthasClassLoader;
    }

    /**
     * Agent的主入口方法
     *
     * <p>执行以下操作：
     * <ol>
     *   <li>检查Arthas是否已在运行，避免重复初始化</li>
     *   <li>解析传入的参数（包含arthas-core.jar路径和agent参数）</li>
     *   <li>定位arthas-core.jar文件</li>
     *   <li>创建隔离的类加载器</li>
     *   <li>在专用线程中绑定Arthas服务</li>
     * </ol>
     *
     * <p>使用synchronized确保线程安全
     *
     * @param args 代理参数，格式为 "arthasCoreJar路径;agentArgs" 或仅为 "agentArgs"
     * @param inst Instrumentation实例
     */
    private static synchronized void main(String args, final Instrumentation inst) {
        // 尝试判断arthas是否已在运行，如果是的话，直接就退出
        try {
            // 尝试加载SpyAPI类，如果加载成功且已初始化，说明Arthas已在运行
            Class.forName("java.arthas.SpyAPI"); // 加载不到会抛异常
            if (SpyAPI.isInited()) {
                ps.println("Arthas server already stared, skip attach.");
                ps.flush();
                return;
            }
        } catch (Throwable e) {
            // SpyAPI类不存在或未初始化，继续执行
            // ignore
        }
        try {
            ps.println("Arthas server agent start...");
            // 传递的args参数分两个部分:arthasCoreJar路径和agentArgs, 分别是Agent的JAR包路径和期望传递到服务端的参数
            if (args == null) {
                args = "";
            }
            // 对URL编码的参数进行解码
            args = decodeArg(args);

            String arthasCoreJar;
            final String agentArgs;
            // 参数格式：arthasCoreJar路径;agentArgs，使用分号分隔
            int index = args.indexOf(';');
            if (index != -1) {
                // 存在分号，分隔出jar路径和参数
                arthasCoreJar = args.substring(0, index);
                agentArgs = args.substring(index);
            } else {
                // 不存在分号，整个字符串都是参数
                arthasCoreJar = "";
                agentArgs = args;
            }

            // 验证arthas-core.jar文件是否存在
            File arthasCoreJarFile = new File(arthasCoreJar);
            if (!arthasCoreJarFile.exists()) {
                ps.println("Can not find arthas-core jar file from args: " + arthasCoreJarFile);
                // 如果从参数中找不到，尝试从arthas-agent.jar所在目录查找
                // try to find from arthas-agent.jar directory
                CodeSource codeSource = AgentBootstrap.class.getProtectionDomain().getCodeSource();
                if (codeSource != null) {
                    try {
                        // 获取当前类的JAR文件位置
                        File arthasAgentJarFile = new File(codeSource.getLocation().toURI().getSchemeSpecificPart());
                        // 在同一目录下查找arthas-core.jar
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
            // 如果最终还是找不到arthas-core.jar，直接返回
            if (!arthasCoreJarFile.exists()) {
                return;
            }

            /**
             * 使用专用线程来运行绑定逻辑，以防止可能的内存泄漏（问题#195）
             * Use a dedicated thread to run the binding logic to prevent possible memory leak. #195
             */
            final ClassLoader agentLoader = getClassLoader(inst, arthasCoreJarFile);

            // 创建并启动绑定线程
            Thread bindingThread = new Thread() {
                @Override
                public void run() {
                    try {
                        // 在独立线程中执行绑定操作
                        bind(inst, agentLoader, agentArgs);
                    } catch (Throwable throwable) {
                        throwable.printStackTrace(ps);
                    }
                }
            };

            // 设置线程名称便于调试
            bindingThread.setName("arthas-binding-thread");
            bindingThread.start();
            // 等待绑定线程完成
            bindingThread.join();
        } catch (Throwable t) {
            t.printStackTrace(ps);
            try {
                // 如果日志文件已打开，关闭它
                if (ps != System.err) {
                    ps.close();
                }
            } catch (Throwable tt) {
                // ignore
            }
            throw new RuntimeException(t);
        }
    }

    /**
     * 绑定Arthas服务
     *
     * <p>使用反射调用ArthasBootstrap.getInstance()方法来初始化Arthas服务
     * 然后检查服务是否成功绑定到监听端口
     *
     * <p>这段代码相当于：
     * <pre>
     * ArthasBootstrap bootstrap = ArthasBootstrap.getInstance(inst, args);
     * if (!bootstrap.isBind()) {
     *     throw new RuntimeException("Arthas server port binding failed!");
     * }
     * </pre>
     *
     * @param inst Instrumentation实例
     * @param agentLoader Arthas专用类加载器
     * @param args 传递给Arthas的配置参数
     * @throws Throwable 如果绑定失败
     */
    private static void bind(Instrumentation inst, ClassLoader agentLoader, String args) throws Throwable {
        /**
         * 使用反射加载ArthasBootstrap类并调用其getInstance方法
         * <pre>
         * ArthasBootstrap bootstrap = ArthasBootstrap.getInstance(inst, args);
         * </pre>
         */
        // 加载ArthasBootstrap类
        Class<?> bootstrapClass = agentLoader.loadClass(ARTHAS_BOOTSTRAP);
        // 调用getInstance方法获取单例实例
        Object bootstrap = bootstrapClass.getMethod(GET_INSTANCE, Instrumentation.class, String.class).invoke(null, inst, args);
        // 检查Arthas服务是否成功绑定到端口
        boolean isBind = (Boolean) bootstrapClass.getMethod(IS_BIND).invoke(bootstrap);
        if (!isBind) {
            // 绑定失败，抛出异常
            String errorMsg = "Arthas server port binding failed! Please check $HOME/logs/arthas/arthas.log for more details.";
            ps.println(errorMsg);
            throw new RuntimeException(errorMsg);
        }
        // 绑定成功
        ps.println("Arthas server already bind.");
    }

    /**
     * 解码URL编码的参数
     *
     * <p>将传递过来的参数从URL编码格式解码为原始字符串
     * 如果解码失败（不支持UTF-8编码），则返回原始字符串
     *
     * @param arg URL编码的参数字符串
     * @return 解码后的字符串
     */
    private static String decodeArg(String arg) {
        try {
            // 使用UTF-8编码进行URL解码
            return URLDecoder.decode(arg, "utf-8");
        } catch (UnsupportedEncodingException e) {
            // 如果不支持UTF-8编码，返回原始字符串
            return arg;
        }
    }
}
