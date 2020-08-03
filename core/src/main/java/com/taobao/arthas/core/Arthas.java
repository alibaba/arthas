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
                    "\ncurl -sLk https://alibaba.github.io/arthas/install.sh | sh");
        }
        configure.setIp((String) commandLine.getOptionValue("target-ip"));
        configure.setTelnetPort((Integer) commandLine.getOptionValue("telnet-port"));
        configure.setHttpPort((Integer) commandLine.getOptionValue("http-port"));

        configure.setTunnelServer((String) commandLine.getOptionValue("tunnel-server"));
        configure.setAgentId((String) commandLine.getOptionValue("agent-id"));
        configure.setStatUrl((String) commandLine.getOptionValue("stat-url"));
        return configure;
    }

    private void attachAgent(Configure configure) throws Exception {
        VirtualMachineDescriptor virtualMachineDescriptor = null;
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
