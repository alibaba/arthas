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
        Option pid = new TypedOption<Integer>().setType(Integer.class).setShortName("pid").setRequired(true);
        Option core = new TypedOption<String>().setType(String.class).setShortName("core").setRequired(true);
        Option agent = new TypedOption<String>().setType(String.class).setShortName("agent").setRequired(true);
        Option target = new TypedOption<String>().setType(String.class).setShortName("target-ip");
        Option telnetPort = new TypedOption<Integer>().setType(Integer.class)
                .setShortName("telnet-port").setDefaultValue(DEFAULT_TELNET_PORT);
        Option httpPort = new TypedOption<Integer>().setType(Integer.class)
                .setShortName("http-port").setDefaultValue(DEFAULT_HTTP_PORT);
        Option sessionTimeout = new TypedOption<Integer>().setType(Integer.class)
                        .setShortName("session-timeout").setDefaultValue("" + Configure.DEFAULT_SESSION_TIMEOUT_SECONDS);
        CLI cli = CLIs.create("arthas").addOption(pid).addOption(core).addOption(agent).addOption(target)
                .addOption(telnetPort).addOption(httpPort).addOption(sessionTimeout);
        CommandLine commandLine = cli.parse(Arrays.asList(args));

        Configure configure = new Configure();
        configure.setJavaPid((Integer) commandLine.getOptionValue("pid"));
        configure.setArthasAgent((String) commandLine.getOptionValue("agent"));
        configure.setArthasCore((String) commandLine.getOptionValue("core"));
        configure.setSessionTimeout((Integer)commandLine.getOptionValue("session-timeout"));
        if (commandLine.getOptionValue("target-ip") == null) {
            throw new IllegalStateException("as.sh is too old to support web console, " +
                    "please run the following command to upgrade to latest version:" +
                    "\ncurl -sLk https://alibaba.github.io/arthas/install.sh | sh");
        }
        configure.setIp((String) commandLine.getOptionValue("target-ip"));
        configure.setTelnetPort((Integer) commandLine.getOptionValue("telnet-port"));
        configure.setHttpPort((Integer) commandLine.getOptionValue("http-port"));
        return configure;
    }

    private void attachAgent(Configure configure) throws Exception {
        // 获取pid访问到的虚拟机
        VirtualMachineDescriptor virtualMachineDescriptor = null;
        for (VirtualMachineDescriptor descriptor : VirtualMachine.list()) {
            String pid = descriptor.id();
            if (pid.equals(Integer.toString(configure.getJavaPid()))) {
                virtualMachineDescriptor = descriptor;
            }
        }
        VirtualMachine virtualMachine = null;
        try {
            if (null == virtualMachineDescriptor) { // 使用 attach(String pid) 这种方式
                virtualMachine = VirtualMachine.attach("" + configure.getJavaPid());
            } else {
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


            // 目标虚拟机加载 代理jar 以及 描述信息
            // sun.tools.attach.HotSpotVirtualMachine.loadAgent
            //  操作系统底层相关加载AgentLibrary sun.tools.attach.LinuxVirtualMachine.execute
            // virtualMachine.loadAgent(configure.getArthasAgent(),
            //                configure.getArthasCore() + ";" + configure.toString());
            // 因此需要跳转到arthas-agent.jar 去了 参数: ${arthas_lib_dir}/arthas-agent.jar=${arthas_lib_dir}/arthas-core.jar;com.taobao.arthas.core.config.Configure.toString()
            // agent#pom.xml
            // <Premain-Class>com.taobao.arthas.agent.AgentBootstrap</Premain-Class>
            //      com.taobao.arthas.agent.AgentBootstrap.premain
            // <Agent-Class>com.taobao.arthas.agent.AgentBootstrap</Agent-Class>
            //     com.taobao.arthas.agent.AgentBootstrap.agentmain

            String arthasAgentPath = configure.getArthasAgent();
            //convert jar path to unicode string
            configure.setArthasAgent(encodeArg(arthasAgentPath));
            configure.setArthasCore(encodeArg(configure.getArthasCore()));
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

    /**
     * arthas#version > 3.0
     * -jar ${arthas_lib_dir}/arthas-core.jar \
     *                     -pid ${TARGET_PID} \
     *                     -target-ip ${TARGET_IP} \
     *                     -telnet-port ${TELNET_PORT} \
     *                     -http-port ${HTTP_PORT} \
     *                     -core "${arthas_lib_dir}/arthas-core.jar" \
     *                     -agent "${arthas_lib_dir}/arthas-agent.jar"
     * arthas#version <= 3.0
     * -jar ${arthas_lib_dir}/arthas-core.jar \
     *                     -pid ${TARGET_PID} \
     *                     -target ${TARGET_IP}":"${TELNET_PORT} \
     *                     -core "${arthas_lib_dir}/arthas-core.jar" \
     *                     -agent "${arthas_lib_dir}/arthas-agent.jar"
     *
     * -Xbootclasspath 不要有空格
     * vmoptioin: -Dfile.encoding=UTF-8 -Xbootclasspath/a:D:\tools.jar
     * 指定pid core agent路径
     * program arg: -pid 380 -target-ip 127.0.0.1 -telnet-port 3658 -http-port 8563 -core C:\workspace\arthas\core\target\arthas-core.jar -agent C:\workspace\arthas\agent\target\arthas-agent.jar
     * @param args
     */
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
