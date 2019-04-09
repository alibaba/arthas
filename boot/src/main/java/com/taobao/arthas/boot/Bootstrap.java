package com.taobao.arthas.boot;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.InputMismatchException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.taobao.arthas.common.AnsiLog;
import com.taobao.arthas.common.JavaVersionUtils;
import com.taobao.arthas.common.SocketUtils;
import com.taobao.arthas.common.UsageRender;
import com.taobao.middleware.cli.CLI;
import com.taobao.middleware.cli.CommandLine;
import com.taobao.middleware.cli.UsageMessageFormatter;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.CLIConfigurator;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * @author hengyunabc 2018-10-26
 *
 */
@Name("arthas-boot")
@Summary("Bootstrap Arthas")
@Description("EXAMPLES:\n" + "  java -jar arthas-boot.jar <pid>\n" + "  java -jar arthas-boot.jar --target-ip 0.0.0.0\n"
                + "  java -jar arthas-boot.jar --telnet-port 9999 --http-port -1\n"
                + "  java -jar arthas-boot.jar -c 'sysprop; thread' <pid>\n"
                + "  java -jar arthas-boot.jar -f batch.as <pid>\n"
                + "  java -jar arthas-boot.jar --use-version 3.1.0\n"
                + "  java -jar arthas-boot.jar --versions\n"
                + "  java -jar arthas-boot.jar --session-timeout 3600\n" + "  java -jar arthas-boot.jar --attach-only\n"
                + "  java -jar arthas-boot.jar --repo-mirror aliyun --use-http\n" + "WIKI:\n"
                + "  https://alibaba.github.io/arthas\n")
public class Bootstrap {
    private static final int DEFAULT_TELNET_PORT = 3658;
    private static final int DEFAULT_HTTP_PORT = 8563;
    private static final String DEFAULT_TARGET_IP = "127.0.0.1";
    private static final File ARTHAS_LIB_DIR = new File(
                    System.getProperty("user.home") + File.separator + ".arthas" + File.separator + "lib");

    private boolean help = false;

    private int pid = -1;
    private String targetIp = DEFAULT_TARGET_IP;
    private int telnetPort = DEFAULT_TELNET_PORT;
    private int httpPort = DEFAULT_HTTP_PORT;
    /**
     * @see com.taobao.arthas.core.config.Configure#DEFAULT_SESSION_TIMEOUT_SECONDS
     */
    private Long sessionTimeout;

    private Integer height = null;
    private Integer width = null;

    private boolean verbose = false;

    /**
     * <pre>
     * The directory contains arthas-core.jar/arthas-client.jar/arthas-spy.jar.
     * 1. When use-version is not empty, try to find arthas home under ~/.arthas/lib
     * 2. Try set the directory where arthas-boot.jar is located to arthas home
     * 3. Try to download from maven repo
     * </pre>
     */
    private String arthasHome;

    /**
     * under ~/.arthas/lib
     */
    private String useVersion;

    /**
     * list local and remote versions
     */
    private boolean versions;

    /**
     * download from maven repository. if timezone is +0800, default value is 'aliyun', else is 'center'.
     */
    private String repoMirror;

    /**
     * enforce use http to download arthas. default use https
     */
    private boolean useHttp = false;

    private boolean attachOnly = false;

    private String command;
    private String batchFile;

    @Argument(argName = "pid", index = 0, required = false)
    @Description("Target pid")
    public void setPid(int pid) {
        this.pid = pid;
    }

    @Option(shortName = "h", longName = "help", flag = true)
    @Description("Print usage")
    public void setHelp(boolean help) {
        this.help = help;
    }

    @Option(longName = "target-ip")
    @Description("The target jvm listen ip, default 127.0.0.1")
    public void setTargetIp(String targetIp) {
        this.targetIp = targetIp;
    }

    @Option(longName = "telnet-port")
    @Description("The target jvm listen telnet port, default 3658")
    public void setTelnetPort(int telnetPort) {
        this.telnetPort = telnetPort;
    }

    @Option(longName = "http-port")
    @Description("The target jvm listen http port, default 8563")
    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    @Option(longName = "session-timeout")
    @Description("The session timeout seconds, default 1800 (30min)")
    public void setSessionTimeout(Long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    @Option(longName = "arthas-home")
    @Description("The arthas home")
    public void setArthasHome(String arthasHome) {
        this.arthasHome = arthasHome;
    }

    @Option(longName = "use-version")
    @Description("Use special version arthas")
    public void setUseVersion(String useVersion) {
        this.useVersion = useVersion;
    }

    @Option(longName = "repo-mirror")
    @Description("Use special maven repository mirror, value is center/aliyun or http repo url.")
    public void setRepoMirror(String repoMirror) {
        this.repoMirror = repoMirror;
    }

    @Option(longName = "versions", flag = true)
    @Description("List local and remote arthas versions")
    public void setVersions(boolean versions) {
        this.versions = versions;
    }

    @Option(longName = "use-http", flag = true)
    @Description("Enforce use http to download, default use https")
    public void setuseHttp(boolean useHttp) {
        this.useHttp = useHttp;
    }

    @Option(longName = "attach-only", flag = true)
    @Description("Attach target process only, do not connect")
    public void setAttachOnly(boolean attachOnly) {
        this.attachOnly = attachOnly;
    }

    @Option(shortName = "c", longName = "command")
    @Description("Command to execute, multiple commands separated by ;")
    public void setCommand(String command) {
        this.command = command;
    }

    @Option(shortName = "f", longName = "batch-file")
    @Description("The batch file to execute")
    public void setBatchFile(String batchFile) {
        this.batchFile = batchFile;
    }

    @Option(longName = "height")
    @Description("arthas-client terminal height")
    public void setHeight(int height) {
        this.height = height;
    }

    @Option(longName = "width")
    @Description("arthas-client terminal width")
    public void setWidth(int width) {
        this.width = width;
    }

    @Option(shortName = "v", longName = "verbose", flag = true)
    @Description("Verbose, print debug info.")
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException,
                    ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException,
                    IllegalArgumentException, InvocationTargetException {
        Package bootstrapPackage = Bootstrap.class.getPackage();
        if (bootstrapPackage != null) {
            String arthasBootVersion = bootstrapPackage.getImplementationVersion();
            if (arthasBootVersion != null) {
                AnsiLog.info("arthas-boot version: " + arthasBootVersion);
            }
        }

        String mavenMetaData = null;

        Bootstrap bootstrap = new Bootstrap();

        CLI cli = CLIConfigurator.define(Bootstrap.class);
        CommandLine commandLine = cli.parse(Arrays.asList(args));

        try {
            CLIConfigurator.inject(commandLine, bootstrap);
        } catch (Throwable e) {
            e.printStackTrace();
            System.out.println(usage(cli));
            System.exit(1);
        }

        if (bootstrap.isVerbose()) {
            AnsiLog.level(Level.ALL);
        }
        if (bootstrap.isHelp()) {
            System.out.println(usage(cli));
            System.exit(0);
        }

        if (bootstrap.getRepoMirror() == null || bootstrap.getRepoMirror().trim().isEmpty()) {
            bootstrap.setRepoMirror("center");
            // if timezone is +0800, default repo mirror is aliyun
            if (TimeUnit.MILLISECONDS.toHours(TimeZone.getDefault().getOffset(System.currentTimeMillis())) == 8) {
                bootstrap.setRepoMirror("aliyun");
            }
        }
        AnsiLog.debug("Repo mirror:" + bootstrap.getRepoMirror());

        if (bootstrap.isVersions()) {
            if (mavenMetaData == null) {
                mavenMetaData = DownloadUtils.readMavenMetaData(bootstrap.getRepoMirror(), bootstrap.isuseHttp());
            }
            System.out.println(UsageRender.render(listVersions(mavenMetaData)));
            System.exit(0);
        }

        if (JavaVersionUtils.isJava6() || JavaVersionUtils.isJava7()) {
            bootstrap.setuseHttp(true);
            AnsiLog.debug("Java version is {}, only support http, set useHttp to true.",
                            JavaVersionUtils.javaVersionStr());
        }

        // check telnet/http port
        int telnetPortPid = -1;
        int httpPortPid = -1;
        if (bootstrap.getTelnetPort() > 0) {
            telnetPortPid = SocketUtils.findTcpListenProcess(bootstrap.getTelnetPort());
            if (telnetPortPid > 0) {
                AnsiLog.info("Process {} already using port {}", telnetPortPid, bootstrap.getTelnetPort());
            }
        }
        if (bootstrap.getHttpPort() > 0) {
            httpPortPid = SocketUtils.findTcpListenProcess(bootstrap.getHttpPort());
            if (httpPortPid > 0) {
                AnsiLog.info("Process {} already using port {}", httpPortPid, bootstrap.getHttpPort());
            }
        }

        int pid = bootstrap.getPid();
        // select pid
        if (pid < 0) {
            try {
                pid = ProcessUtils.select(bootstrap.isVerbose(), telnetPortPid);
            } catch (InputMismatchException e) {
                System.out.println("Please input an integer to select pid.");
                System.exit(1);
            }
            if (pid < 0) {
                System.out.println("Please select an available pid.");
                System.exit(1);
            }
        }

        if (telnetPortPid > 0 && pid != telnetPortPid) {
            AnsiLog.error("Target process {} is not the process using port {}, you will connect to an unexpected process.",
                            pid, bootstrap.getTelnetPort());
            AnsiLog.error("1. Try to restart arthas-boot, select process {}, shutdown it first.",
                            telnetPortPid);
            AnsiLog.error("2. Or try to use different telnet port, for example: java -jar arthas-boot.jar --telnet-port 9998 --http-port -1");
            System.exit(1);
        }

        if (httpPortPid > 0 && pid != httpPortPid) {
            AnsiLog.error("Target process {} is not the process using port {}, you will connect to an unexpected process.",
                            pid, bootstrap.getHttpPort());
            AnsiLog.error("1. Try to restart arthas-boot, select process {}, shutdown it first.",
                            httpPortPid);
            AnsiLog.error("2. Or try to use different http port, for example: java -jar arthas-boot.jar --telnet-port 9998 --http-port 9999", httpPortPid);
            System.exit(1);
        }

        // find arthas home
        File arthasHomeDir = null;
        if (bootstrap.getArthasHome() != null) {
            verifyArthasHome(bootstrap.getArthasHome());
            arthasHomeDir = new File(bootstrap.getArthasHome());
        }
        if (arthasHomeDir == null && bootstrap.getUseVersion() != null) {
            // try to find from ~/.arthas/lib
            File specialVersionDir = new File(System.getProperty("user.home"), ".arthas" + File.separator + "lib"
                            + File.separator + bootstrap.getUseVersion() + File.separator + "arthas");
            if (!specialVersionDir.exists()) {
                // try to download arthas from remote server.
                DownloadUtils.downArthasPackaging(bootstrap.getRepoMirror(), bootstrap.isuseHttp(),
                                bootstrap.getUseVersion(), ARTHAS_LIB_DIR.getAbsolutePath());
            }
            verifyArthasHome(specialVersionDir.getAbsolutePath());
            arthasHomeDir = specialVersionDir;
        }

        // Try set the directory where arthas-boot.jar is located to arhtas home
        if (arthasHomeDir == null) {
            CodeSource codeSource = Bootstrap.class.getProtectionDomain().getCodeSource();
            if (codeSource != null) {
                try {
                    // https://stackoverflow.com/a/17870390
                    File bootJarPath = new File(codeSource.getLocation().toURI().getSchemeSpecificPart());
                    verifyArthasHome(bootJarPath.getParent());
                    arthasHomeDir = bootJarPath.getParentFile();
                } catch (Throwable e) {
                    // ignore
                }

            }
        }

        // try to download from remote server
        if (arthasHomeDir == null) {
            boolean checkFile =  ARTHAS_LIB_DIR.exists() || ARTHAS_LIB_DIR.mkdirs();
            if(!checkFile){
                AnsiLog.error("cannot create directory {}: maybe permission denied", ARTHAS_LIB_DIR.getAbsolutePath());
                System.exit(1);
            }

            /**
             * <pre>
             * 1. get local latest version
             * 2. get remote latest version
             * 3. compare two version
             * </pre>
             */
            List<String> versionList = listNames(ARTHAS_LIB_DIR);
            Collections.sort(versionList);

            String localLastestVersion = null;
            if (!versionList.isEmpty()) {
                localLastestVersion = versionList.get(versionList.size() - 1);
            }

            if (mavenMetaData == null) {
                mavenMetaData = DownloadUtils.readMavenMetaData(bootstrap.getRepoMirror(), bootstrap.isuseHttp());
            }

            String remoteLastestVersion = DownloadUtils.readMavenReleaseVersion(mavenMetaData);

            boolean needDownload = false;
            if (localLastestVersion == null) {
                if (remoteLastestVersion == null) {
                    // exit
                    AnsiLog.error("Can not find Arthas under local: {} and remote: {}", ARTHAS_LIB_DIR,
                                    bootstrap.getRepoMirror());
                    System.exit(1);
                } else {
                    needDownload = true;
                }
            } else {
                if (remoteLastestVersion != null) {
                    if (localLastestVersion.compareTo(remoteLastestVersion) < 0) {
                        AnsiLog.info("local lastest version: {}, remote lastest version: {}, try to download from remote.",
                                        localLastestVersion, remoteLastestVersion);
                        needDownload = true;
                    }
                }
            }
            if (needDownload) {
                // try to download arthas from remote server.
                DownloadUtils.downArthasPackaging(bootstrap.getRepoMirror(), bootstrap.isuseHttp(),
                                remoteLastestVersion, ARTHAS_LIB_DIR.getAbsolutePath());
                localLastestVersion = remoteLastestVersion;
            }

            // get the latest version
            arthasHomeDir = new File(ARTHAS_LIB_DIR, localLastestVersion + File.separator + "arthas");
        }

        verifyArthasHome(arthasHomeDir.getAbsolutePath());

        AnsiLog.info("arthas home: " + arthasHomeDir);

        if (telnetPortPid > 0 && pid == telnetPortPid) {
            AnsiLog.info("The target process already listen port {}, skip attach.", bootstrap.getTelnetPort());
        } else {
            // start arthas-core.jar
            List<String> attachArgs = new ArrayList<String>();
            attachArgs.add("-jar");
            attachArgs.add(new File(arthasHomeDir, "arthas-core.jar").getAbsolutePath());
            attachArgs.add("-pid");
            attachArgs.add("" + pid);
            attachArgs.add("-target-ip");
            attachArgs.add(bootstrap.getTargetIp());
            attachArgs.add("-telnet-port");
            attachArgs.add("" + bootstrap.getTelnetPort());
            attachArgs.add("-http-port");
            attachArgs.add("" + bootstrap.getHttpPort());
            attachArgs.add("-core");
            attachArgs.add(new File(arthasHomeDir, "arthas-core.jar").getAbsolutePath());
            attachArgs.add("-agent");
            attachArgs.add(new File(arthasHomeDir, "arthas-agent.jar").getAbsolutePath());
            if (bootstrap.getSessionTimeout() != null) {
                attachArgs.add("-session-timeout");
                attachArgs.add("" + bootstrap.getSessionTimeout());
            }

            AnsiLog.info("Try to attach process " + pid);
            AnsiLog.debug("Start arthas-core.jar args: " + attachArgs);
            ProcessUtils.startArthasCore(pid, attachArgs);

            AnsiLog.info("Attach process {} success.", pid);
        }

        if (bootstrap.isAttachOnly()) {
            System.exit(0);
        }

        // start java telnet client
        // find arthas-client.jar
        URLClassLoader classLoader = new URLClassLoader(
                        new URL[] { new File(arthasHomeDir, "arthas-client.jar").toURI().toURL() });
        Class<?> telnetConsoleClas = classLoader.loadClass("com.taobao.arthas.client.TelnetConsole");
        Method mainMethod = telnetConsoleClas.getMethod("main", String[].class);
        List<String> telnetArgs = new ArrayList<String>();

        if (bootstrap.getCommand() != null) {
            telnetArgs.add("-c");
            telnetArgs.add(bootstrap.getCommand());
        }
        if (bootstrap.getBatchFile() != null) {
            telnetArgs.add("-f");
            telnetArgs.add(bootstrap.getBatchFile());
        }
        if (bootstrap.getHeight() != null) {
            telnetArgs.add("--height");
            telnetArgs.add("" + bootstrap.getHeight());
        }
        if (bootstrap.getWidth() != null) {
            telnetArgs.add("--width");
            telnetArgs.add("" + bootstrap.getWidth());
        }

        // telnet port ,ip
        telnetArgs.add(bootstrap.getTargetIp());
        telnetArgs.add("" + bootstrap.getTelnetPort());

        AnsiLog.info("arthas-client connect {} {}", bootstrap.getTargetIp(), bootstrap.getTelnetPort());
        AnsiLog.debug("Start arthas-client.jar args: " + telnetArgs);
        mainMethod.invoke(null, new Object[] { telnetArgs.toArray(new String[0]) });
    }

    private static String listVersions(String mavenMetaData) {
        StringBuilder result = new StringBuilder(1024);
        List<String> versionList = listNames(ARTHAS_LIB_DIR);
        Collections.sort(versionList);

        result.append("Local versions:\n");
        for (String version : versionList) {
            result.append(" " + version).append('\n');
        }
        result.append("Remote versions:\n");
        if (mavenMetaData != null) {
            List<String> remoteVersions = DownloadUtils.readAllMavenVersion(mavenMetaData);
            Collections.reverse(remoteVersions);
            for (String version : remoteVersions) {
                result.append(" " + version).append('\n');
            }
        }
        return result.toString();
    }

    private static List<String> listNames(File dir) {
        List<String> names = new ArrayList<String>();
        if (!dir.exists()) {
            return names;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return names;
        }
        for (File file : files) {
            String name = file.getName();
            if (name.startsWith(".") || file.isFile()) {
                continue;
            }
            names.add(name);
        }
        return names;
    }

    private static void verifyArthasHome(String arthasHome) {
        File home = new File(arthasHome);
        if (home.isDirectory()) {
            String[] fileList = { "arthas-core.jar", "arthas-agent.jar", "arthas-spy.jar" };

            for (String fileName : fileList) {
                if (!new File(home, fileName).exists()) {
                    throw new IllegalArgumentException(
                                    fileName + " do not exist, arthas home: " + home.getAbsolutePath());
                }
            }
            return;
        }

        throw new IllegalArgumentException("illegal arthas home: " + home.getAbsolutePath());
    }

    private static String usage(CLI cli) {
        StringBuilder usageStringBuilder = new StringBuilder();
        UsageMessageFormatter usageMessageFormatter = new UsageMessageFormatter();
        usageMessageFormatter.setOptionComparator(null);
        cli.usage(usageStringBuilder, usageMessageFormatter);
        return UsageRender.render(usageStringBuilder.toString());
    }

    public String getArthasHome() {
        return arthasHome;
    }

    public String getUseVersion() {
        return useVersion;
    }

    public String getRepoMirror() {
        return repoMirror;
    }

    public boolean isuseHttp() {
        return useHttp;
    }

    public String getTargetIp() {
        return targetIp;
    }

    public int getTelnetPort() {
        return telnetPort;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public String getCommand() {
        return command;
    }

    public String getBatchFile() {
        return batchFile;
    }

    public boolean isAttachOnly() {
        return attachOnly;
    }

    public int getPid() {
        return pid;
    }

    public boolean isHelp() {
        return help;
    }

    public Long getSessionTimeout() {
        return sessionTimeout;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isVersions() {
        return versions;
    }

    public Integer getHeight() {
        return height;
    }

    public Integer getWidth() {
        return width;
    }
}
