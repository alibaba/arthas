package com.taobao.arthas.boot;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Properties;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.taobao.arthas.common.AnsiLog;
import com.taobao.arthas.common.JavaVersionUtils;
import com.taobao.middleware.cli.CLI;
import com.taobao.middleware.cli.CommandLine;
import com.taobao.middleware.cli.annotations.CLIConfigurator;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

/**
 *
 * @author hengyunabc 2019-03-26
 *
 */
@Name("arthas-attach")
@Summary("Arthas attach to target process tool.")
@Description("EXAMPLES:\n" + "  java -cp arthas-boot.jar com.taobao.arthas.boot.Attach -h\n"
                + "  java -cp arthas-boot.jar com.taobao.arthas.boot.Attach --agent arthas-agent.jar --config 'target-ip=127.0.0.1;http-port=8563;telnet-port=3658;'\n"
                + "WIKI:\n" + "  https://alibaba.github.io/arthas\n")
public class Attach {

    private boolean help = false;

    private int pid = -1;

    private String agent;

    private String config;

    @Option(shortName = "h", longName = "help", flag = true)
    @Description("Print usage")
    public void setHelp(boolean help) {
        this.help = help;
    }

    @Option(shortName = "p", longName = "pid")
    @Description("Target pid")
    public void setPid(int pid) {
        this.pid = pid;
    }

    @Option(shortName = "a", longName = "agent")
    @Description("The arthas agent path")
    public void setAgent(String agent) {
        this.agent = agent;
    }

    @Option(shortName = "c", longName = "config")
    @Description("The config string for arthas agent")
    public void setConfig(String config) {
        this.config = config;
    }

    public static void main(String[] args) throws Exception {
        Package attachPackage = Attach.class.getPackage();
        if (attachPackage != null) {
            String attachVersion = attachPackage.getImplementationVersion();
            if (attachVersion != null) {
                AnsiLog.info("arthas-attach version: " + attachVersion);
            }
        }

        Attach attach = new Attach();

        CLI cli = CLIConfigurator.define(Attach.class);
        CommandLine commandLine = cli.parse(Arrays.asList(args));

        try {
            CLIConfigurator.inject(commandLine, attach);
        } catch (Throwable e) {
            e.printStackTrace();
            System.out.println(CLIUtils.usage(cli));
            System.exit(1);
        }

        if (attach.isHelp()) {
            System.out.println(CLIUtils.usage(cli));
            System.exit(0);
        }

        Attach.attachAgent("" + attach.getPid(), attach.getAgent(), attach.getConfig());
    }

    static void attachAgent(String targetPid, String agentPath, String configString) throws Exception {
        VirtualMachineDescriptor virtualMachineDescriptor = null;
        for (VirtualMachineDescriptor descriptor : VirtualMachine.list()) {
            String pid = descriptor.id();
            if (pid.equals(targetPid)) {
                virtualMachineDescriptor = descriptor;
            }
        }
        VirtualMachine virtualMachine = null;
        try {
            if (null == virtualMachineDescriptor) { // 使用 attach(String pid) 这种方式
                virtualMachine = VirtualMachine.attach(targetPid);
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

            // convert jar path to unicode string
            virtualMachine.loadAgent(decode(agentPath), configString);
        } finally {
            if (null != virtualMachine) {
                virtualMachine.detach();
            }
        }
    }

    private static String decode(String str) {
        try {
            return URLDecoder.decode(str, "utf-8");
        } catch (UnsupportedEncodingException e) {
            return str;
        }
    }

    public boolean isHelp() {
        return help;
    }

    public int getPid() {
        return pid;
    }

    public String getAgent() {
        return agent;
    }

    public String getConfig() {
        return config;
    }
}
