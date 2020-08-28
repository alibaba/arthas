package com.taobao.arthas.core;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.taobao.arthas.common.AnsiLog;
import com.taobao.arthas.common.JavaVersionUtils;
import com.taobao.arthas.core.config.Configure;
import com.taobao.middleware.cli.CLI;
import com.taobao.middleware.cli.CLIs;
import com.taobao.middleware.cli.CommandLine;
import com.taobao.middleware.cli.Option;
import com.taobao.middleware.cli.TypedOption;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Properties;

/**
 * Arthas启动器
 */
public class Arthas {

    private static final String DEFAULT_TELNET_PORT = "3658";
    private static final String DEFAULT_HTTP_PORT = "8563";

    private Arthas(String[] args) throws Exception {
        attachAgent(parse(args));
    }

    private Configure parse(String[] args) {
        Option pid = new TypedOption<Long>().setType(Long.class).setShortName("pid").setRequired(true);
        Option core = new TypedOption<String>().setType(String.class).setShortName("core").setRequired(true);
        Option agent = new TypedOption<String>().setType(String.class).setShortName("agent").setRequired(true);
        Option target = new TypedOption<String>().setType(String.class).setShortName("target-ip");
        Option telnetPort = new TypedOption<Integer>().setType(Integer.class)
                .setShortName("telnet-port").setDefaultValue(DEFAULT_TELNET_PORT);
        Option httpPort = new TypedOption<Integer>().setType(Integer.class)
                .setShortName("http-port").setDefaultValue(DEFAULT_HTTP_PORT);
        Option sessionTimeout = new TypedOption<Integer>().setType(Integer.class)
                        .setShortName("session-timeout").setDefaultValue("" + Configure.DEFAULT_SESSION_TIMEOUT_SECONDS);

        Option tunnelServer = new TypedOption<String>().setType(String.class).setShortName("tunnel-server");
        Option agentId = new TypedOption<String>().setType(String.class).setShortName("agent-id");

        Option statUrl = new TypedOption<String>().setType(String.class).setShortName("stat-url");

        CLI cli = CLIs.create("arthas").addOption(pid).addOption(core).addOption(agent).addOption(target)
                .addOption(telnetPort).addOption(httpPort).addOption(sessionTimeout).addOption(tunnelServer).addOption(agentId).addOption(statUrl);
        CommandLine commandLine = cli.parse(Arrays.asList(args));

        Configure configure = new Configure();
        configure.setJavaPid((Long) commandLine.getOptionValue("pid"));
        configure.setArthasAgent((String) commandLine.getOptionValue("agent"));
        configure.setArthasCore((String) commandLine.getOptionValue("core"));
        configure.setSessionTimeout((Integer)commandLine.getOptionValue("session-timeout"));
        if (commandLine.getOptionValue("target-ip") == null) {
            throw new IllegalStateException("as.sh is too old to support web console, " +
                    "please run the following command to upgrade to latest version:" +
                    "\ncurl -sLk https://arthas.aliyun.com/install.sh | sh");
        }
        configure.setIp((String) commandLine.getOptionValue("target-ip"));
        configure.setTelnetPort((Integer) commandLine.getOptionValue("telnet-port"));
        configure.setHttpPort((Integer) commandLine.getOptionValue("http-port"));

        configure.setTunnelServer((String) commandLine.getOptionValue("tunnel-server"));
        configure.setAgentId((String) commandLine.getOptionValue("agent-id"));
        configure.setStatUrl((String) commandLine.getOptionValue("stat-url"));
        return configure;
    }

    /**
     * 核心就是通过 {@link }
     * 1 {@link VirtualMachine#attach(VirtualMachineDescriptor)}  获取虚机后， 2 {@link VirtualMachine#loadAgent(String, String)} 通过虚机加载代理
     * @see VirtualMachine
     * @param configure
     * @throws Exception
     */
    private void attachAgent(Configure configure) throws Exception {
        VirtualMachineDescriptor virtualMachineDescriptor = null;
        //#### 获取所有java进程，等同jps，查找指定pid的进程
        for (VirtualMachineDescriptor descriptor : VirtualMachine.list()) {
            String pid = descriptor.id();
            if (pid.equals(Long.toString(configure.getJavaPid()))) {
                virtualMachineDescriptor = descriptor;
                break;
            }
        }
        VirtualMachine virtualMachine = null;
        try {
            if (null == virtualMachineDescriptor) { // 使用 attach(String pid) 这种方式
                virtualMachine = VirtualMachine.attach("" + configure.getJavaPid());
            } else { //#### 使用attach方式，附着到指定java进程上，区别于 java -agent参数静态指定premain方式
                virtualMachine = VirtualMachine.attach(virtualMachineDescriptor);
            }

            Properties targetSystemProperties = virtualMachine.getSystemProperties();
            String targetJavaVersion = JavaVersionUtils.javaVersionStr(targetSystemProperties);
            String currentJavaVersion = JavaVersionUtils.javaVersionStr();
            if (targetJavaVersion != null && currentJavaVersion != null) {
                if (!targetJavaVersion.equals(currentJavaVersion)) {
                    AnsiLog.warn("Current VM java version: {} do not match target VM java version: {}, attach may fail.",
                                    currentJavaVersion, targetJavaVersion);
                    AnsiLog.warn("Target VM JAVA_HOME is {}, arthas-boot JAVA_HOME is {}, try to set the same JAVA_HOME.",
                                    targetSystemProperties.getProperty("java.home"), System.getProperty("java.home"));
                }
            }

            String arthasAgentPath = configure.getArthasAgent();
            //convert jar path to unicode string
            configure.setArthasAgent(encodeArg(arthasAgentPath));
            configure.setArthasCore(encodeArg(configure.getArthasCore()));

            /**
             * 使用jdk-tools里面的VirtualMachine.loadAgent，其中第一个参数为agent路径， 第二个参数向jar包中的agentmain()方法传递参数（此处{@link Configure}序列化之后的字符串），
             */
            //参数1,2例子分别如下
            // C:\Users\zhanghongjun\.arthas\lib\3.3.9\arthas\arthas-agent.jar
            // C:\Users\zhanghongjun\.arthas\lib\3.3.9\arthas\arthas-core.jar;;telnetPort=3659;httpPort=8564;ip=127.0.0.1;arthasAgent=C:\Users\zhanghongjun\.arthas\lib\3.3.9\arthas\arthas-agent.jar;sessionTimeout=1800;arthasCore=C:\Users\zhanghongjun\.arthas\lib\3.3.9\arthas\arthas-core.jar;javaPid=31860;
            virtualMachine.loadAgent(arthasAgentPath,
                    configure.getArthasCore() + ";" + configure.toString());
        } finally {
            if (null != virtualMachine) {
                virtualMachine.detach();
            }
        }
    }

    private static String encodeArg(String arg) {
        try {
            return URLEncoder.encode(arg, "utf-8");
        } catch (UnsupportedEncodingException e) {
            return arg;
        }
    }
    //直接通过boot启动参数为  [-jar, C:\Users\zhanghongjun\.arthas\lib\3.3.9\arthas\arthas-core.jar, -pid, 56120, -target-ip, 127.0.0.1, -telnet-port, 3658, -http-port, 8563, -core, C:\Users\zhanghongjun\.arthas\lib\3.3.9\arthas\arthas-core.jar, -agent, C:\Users\zhanghongjun\.arthas\lib\3.3.9\arthas\arthas-agent.jar]
    //所以可以自己构建如上参数来调试  -pid 56120 -target-ip 127.0.0.1 -telnet-port 3659 -http-port 8564 -core C:\Users\zhanghongjun\.arthas\lib\3.3.9\arthas\arthas-core.jar -agent C:\Users\zhanghongjun\.arthas\lib\3.3.9\arthas\arthas-agent.jar
    //指定pid和期望绑定端口以及代理
    public static void main(String[] args) {
        try {
            new Arthas(args);
        } catch (Throwable t) {
            AnsiLog.error("Start arthas failed, exception stack trace: ");
            t.printStackTrace();
            System.exit(-1);
        }
    }
}
