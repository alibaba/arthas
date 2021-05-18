package com.taobao.arthas.boot;

import java.io.ByteArrayOutputStream;
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
import java.util.Scanner;
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

import static com.taobao.arthas.boot.ProcessUtils.STATUS_EXEC_ERROR;
import static com.taobao.arthas.boot.ProcessUtils.STATUS_EXEC_TIMEOUT;

/**
 * @author hengyunabc 2018-10-26
 *
 */
@Name("arthas-boot")
@Summary("Bootstrap Arthas")
@Description("EXAMPLES:\n" + "  java -jar arthas-boot.jar <pid>\n" + "  java -jar arthas-boot.jar --target-ip 0.0.0.0\n"
                + "  java -jar arthas-boot.jar --telnet-port 9999 --http-port -1\n"
                + "  java -jar arthas-boot.jar --username admin --password <password>\n"
                + "  java -jar arthas-boot.jar --tunnel-server 'ws://192.168.10.11:7777/ws' --app-name demoapp\n"
                + "  java -jar arthas-boot.jar --tunnel-server 'ws://192.168.10.11:7777/ws' --agent-id bvDOe8XbTM2pQWjF4cfw\n"
                + "  java -jar arthas-boot.jar --stat-url 'http://192.168.10.11:8080/api/stat'\n"
                + "  java -jar arthas-boot.jar -c 'sysprop; thread' <pid>\n"
                + "  java -jar arthas-boot.jar -f batch.as <pid>\n"
                + "  java -jar arthas-boot.jar --use-version 3.5.1\n"
                + "  java -jar arthas-boot.jar --versions\n"
                + "  java -jar arthas-boot.jar --select math-game\n"
                + "  java -jar arthas-boot.jar --session-timeout 3600\n" + "  java -jar arthas-boot.jar --attach-only\n"
                + "  java -jar arthas-boot.jar --repo-mirror aliyun --use-http\n" + "WIKI:\n"
                + "  https://arthas.aliyun.com/doc\n")
public class Bootstrap {
    private static final int DEFAULT_TELNET_PORT = 3658;
    private static final int DEFAULT_HTTP_PORT = 8563;
    private static final String DEFAULT_TARGET_IP = "127.0.0.1";
    private static File ARTHAS_LIB_DIR;

    private boolean help = false;

    private long pid = -1;
    private String targetIp;
    private Integer telnetPort;
    private Integer httpPort;
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
     * 3. Try to download from remote repo
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
     * download from remo repository. if timezone is +0800, default value is 'aliyun', else is 'center'.
     */
    private String repoMirror;

    /**
     * enforce use http to download arthas. default use https
     */
    private boolean useHttp = false;

    private boolean attachOnly = false;

    private String command;
    private String batchFile;

    private String tunnelServer;
    private String agentId;

    private String appName;

    private String username;
    private String password;

    private String statUrl;

    private String select;

	static {
        ARTHAS_LIB_DIR = new File(
                System.getProperty("user.home") + File.separator + ".arthas" + File.separator + "lib");
        try {
            ARTHAS_LIB_DIR.mkdirs();
        } catch (Throwable t) {
            //ignore
        }
        if (!ARTHAS_LIB_DIR.exists()) {
            // try to set a temp directory
            ARTHAS_LIB_DIR = new File(System.getProperty("java.io.tmpdir") + File.separator + ".arthas" + File.separator + "lib");
            try {
                ARTHAS_LIB_DIR.mkdirs();
            } catch (Throwable e) {
                // ignore
            }
        }
        if (!ARTHAS_LIB_DIR.exists()) {
            System.err.println("Can not find directory to save arthas lib. please try to set user home by -Duser.home=");
        }
    }

    @Argument(argName = "pid", index = 0, required = false)
    @Description("Target pid")
    public void setPid(long pid) {
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
    @Description("Use special remote repository mirror, value is center/aliyun or http repo url.")
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

    @Option(longName = "tunnel-server")
    @Description("The tunnel server url")
    public void setTunnelServer(String tunnelServer) {
        this.tunnelServer = tunnelServer;
    }

    @Option(longName = "agent-id")
    @Description("The agent id register to tunnel server")
    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    @Option(longName = "app-name")
    @Description("The app name")
    public void setAppName(String appName) {
        this.appName = appName;
    }

    @Option(longName = "username")
    @Description("The username")
    public void setUsername(String username) {
        this.username = username;
    }
    @Option(longName = "password")
    @Description("The password")
    public void setPassword(String password) {
        this.password = password;
    }

    @Option(longName = "stat-url")
    @Description("The report stat url")
    public void setStatUrl(String statUrl) {
        this.statUrl = statUrl;
    }

    @Option(longName = "select")
    @Description("select target process by classname or JARfilename")
    public void setSelect(String select) {
        this.select = select;
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
            System.out.println(UsageRender.render(listVersions()));
            System.exit(0);
        }

        if (JavaVersionUtils.isJava6() || JavaVersionUtils.isJava7()) {
            bootstrap.setuseHttp(true);
            AnsiLog.debug("Java version is {}, only support http, set useHttp to true.",
                            JavaVersionUtils.javaVersionStr());
        }

        // check telnet/http port
        long telnetPortPid = -1;
        long httpPortPid = -1;
        if (bootstrap.getTelnetPortOrDefault() > 0) {
            telnetPortPid = SocketUtils.findTcpListenProcess(bootstrap.getTelnetPortOrDefault());
            if (telnetPortPid > 0) {
                AnsiLog.info("Process {} already using port {}", telnetPortPid, bootstrap.getTelnetPortOrDefault());
            }
        }
        if (bootstrap.getHttpPortOrDefault() > 0) {
            httpPortPid = SocketUtils.findTcpListenProcess(bootstrap.getHttpPortOrDefault());
            if (httpPortPid > 0) {
                AnsiLog.info("Process {} already using port {}", httpPortPid, bootstrap.getHttpPortOrDefault());
            }
        }

        long pid = bootstrap.getPid();
        // select pid
        if (pid < 0) {
            try {
                pid = ProcessUtils.select(bootstrap.isVerbose(), telnetPortPid, bootstrap.getSelect());
            } catch (InputMismatchException e) {
                System.out.println("Please input an integer to select pid.");
                System.exit(1);
            }
            if (pid < 0) {
                System.out.println("Please select an available pid.");
                System.exit(1);
            }
        }

        checkTelnetPortPid(bootstrap, telnetPortPid, pid);

        if (httpPortPid > 0 && pid != httpPortPid) {
            AnsiLog.error("Target process {} is not the process using port {}, you will connect to an unexpected process.",
                            pid, bootstrap.getHttpPortOrDefault());
            AnsiLog.error("1. Try to restart arthas-boot, select process {}, shutdown it first with running the 'stop' command.",
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

            String remoteLastestVersion = DownloadUtils.readLatestReleaseVersion();

            boolean needDownload = false;
            if (localLastestVersion == null) {
                if (remoteLastestVersion == null) {
                    // exit
                    AnsiLog.error("Can not find Arthas under local: {} and remote repo mirror: {}", ARTHAS_LIB_DIR,
                            bootstrap.getRepoMirror());
                    AnsiLog.error(
                            "Unable to download arthas from remote server, please download the full package according to wiki: https://github.com/alibaba/arthas");
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
            AnsiLog.info("The target process already listen port {}, skip attach.", bootstrap.getTelnetPortOrDefault());
        } else {
            //double check telnet port and pid before attach
            telnetPortPid = findProcessByTelnetClient(arthasHomeDir.getAbsolutePath(), bootstrap.getTelnetPortOrDefault());
            checkTelnetPortPid(bootstrap, telnetPortPid, pid);

            // start arthas-core.jar
            List<String> attachArgs = new ArrayList<String>();
            attachArgs.add("-jar");
            attachArgs.add(new File(arthasHomeDir, "arthas-core.jar").getAbsolutePath());
            attachArgs.add("-pid");
            attachArgs.add("" + pid);
            if (bootstrap.getTargetIp() != null) {
                attachArgs.add("-target-ip");
                attachArgs.add(bootstrap.getTargetIp());
            }

            if (bootstrap.getTelnetPort() != null) {
                attachArgs.add("-telnet-port");
                attachArgs.add("" + bootstrap.getTelnetPort());
            }

            if (bootstrap.getHttpPort() != null) {
                attachArgs.add("-http-port");
                attachArgs.add("" + bootstrap.getHttpPort());
            }

            attachArgs.add("-core");
            attachArgs.add(new File(arthasHomeDir, "arthas-core.jar").getAbsolutePath());
            attachArgs.add("-agent");
            attachArgs.add(new File(arthasHomeDir, "arthas-agent.jar").getAbsolutePath());
            if (bootstrap.getSessionTimeout() != null) {
                attachArgs.add("-session-timeout");
                attachArgs.add("" + bootstrap.getSessionTimeout());
            }

            if (bootstrap.getAppName() != null) {
                attachArgs.add("-app-name");
                attachArgs.add(bootstrap.getAppName());
            }

            if (bootstrap.getUsername() != null) {
                attachArgs.add("-username");
                attachArgs.add(bootstrap.getUsername());
            }
            if (bootstrap.getPassword() != null) {
                attachArgs.add("-password");
                attachArgs.add(bootstrap.getPassword());
            }

            if (bootstrap.getTunnelServer() != null) {
                attachArgs.add("-tunnel-server");
                attachArgs.add(bootstrap.getTunnelServer());
            }
            if (bootstrap.getAgentId() != null) {
                attachArgs.add("-agent-id");
                attachArgs.add(bootstrap.getAgentId());
            }
            if (bootstrap.getStatUrl() != null) {
                attachArgs.add("-stat-url");
                attachArgs.add(bootstrap.getStatUrl());
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
        telnetArgs.add(bootstrap.getTargetIpOrDefault());
        telnetArgs.add("" + bootstrap.getTelnetPortOrDefault());

        AnsiLog.info("arthas-client connect {} {}", bootstrap.getTargetIpOrDefault(), bootstrap.getTelnetPortOrDefault());
        AnsiLog.debug("Start arthas-client.jar args: " + telnetArgs);

        // fix https://github.com/alibaba/arthas/issues/833
        Thread.currentThread().setContextClassLoader(classLoader);
        mainMethod.invoke(null, new Object[] { telnetArgs.toArray(new String[0]) });
    }

    private static void checkTelnetPortPid(Bootstrap bootstrap, long telnetPortPid, long targetPid) {
        if (telnetPortPid > 0 && targetPid != telnetPortPid) {
            AnsiLog.error("The telnet port {} is used by process {} instead of target process {}, you will connect to an unexpected process.",
                    bootstrap.getTelnetPortOrDefault(), telnetPortPid, targetPid);
            AnsiLog.error("1. Try to restart arthas-boot, select process {}, shutdown it first with running the 'stop' command.",
                            telnetPortPid);
            AnsiLog.error("2. Or try to stop the existing arthas instance: java -jar arthas-client.jar 127.0.0.1 {} -c \"stop\"", bootstrap.getTelnetPortOrDefault());
            AnsiLog.error("3. Or try to use different telnet port, for example: java -jar arthas-boot.jar --telnet-port 9998 --http-port -1");
            System.exit(1);
        }
    }

    private static long findProcessByTelnetClient(String arthasHomeDir, int telnetPort) {
        // start java telnet client
        List<String> telnetArgs = new ArrayList<String>();
        telnetArgs.add("-c");
        telnetArgs.add("session");
        telnetArgs.add("--execution-timeout");
        telnetArgs.add("2000");
        // telnet port ,ip
        telnetArgs.add("127.0.0.1");
        telnetArgs.add("" + telnetPort);

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
            String error = null;
            int status = ProcessUtils.startArthasClient(arthasHomeDir, telnetArgs, out);
            if (status == STATUS_EXEC_TIMEOUT) {
                error = "detection timeout";
            } else if (status == STATUS_EXEC_ERROR) {
                error = "detection error";
                AnsiLog.error("process status: {}", status);
                AnsiLog.error("process output: {}", out.toString());
            } else {
                // ignore connect error
            }
            if (error != null) {
                AnsiLog.error("The telnet port {} is used, but process {}, you will connect to an unexpected process.", telnetPort, error);
                AnsiLog.error("Try to use a different telnet port, for example: java -jar arthas-boot.jar --telnet-port 9998 --http-port -1");
                System.exit(1);
            }

            //parse output, find java pid
            String output = out.toString("UTF-8");
            String javaPidLine = null;
            Scanner scanner = new Scanner(output);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.contains("JAVA_PID")) {
                    javaPidLine = line;
                    break;
                }
            }
            if (javaPidLine != null) {
                // JAVA_PID    10473
                try {
                    String[] strs = javaPidLine.split("JAVA_PID");
                    if (strs.length > 1) {
                        return Long.parseLong(strs[strs.length - 1].trim());
                    }
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        } catch (Throwable ex) {
            AnsiLog.error("Detection telnet port error");
            AnsiLog.error(ex);
        }

        return -1;
    }

    private static String listVersions() {
        StringBuilder result = new StringBuilder(1024);
        List<String> versionList = listNames(ARTHAS_LIB_DIR);
        Collections.sort(versionList);

        result.append("Local versions:\n");
        for (String version : versionList) {
            result.append(" " + version).append('\n');
        }
        result.append("Remote versions:\n");

		List<String> remoteVersions = DownloadUtils.readRemoteVersions();
		Collections.reverse(remoteVersions);
		for (String version : remoteVersions) {
			result.append(" " + version).append('\n');
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

    public String getTargetIpOrDefault() {
        if (this.targetIp == null) {
            return DEFAULT_TARGET_IP;
        } else {
            return this.targetIp;
        }
    }

    public Integer getTelnetPort() {
        return telnetPort;
    }
    
    public int getTelnetPortOrDefault() {
        if (this.telnetPort == null) {
            return DEFAULT_TELNET_PORT;
        } else {
            return this.telnetPort;
        }
    }

    public Integer getHttpPort() {
        return httpPort;
    }

    public int getHttpPortOrDefault() {
        if (this.httpPort == null) {
            return DEFAULT_HTTP_PORT;
        } else {
            return this.httpPort;
        }
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

    public long getPid() {
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

    public String getTunnelServer() {
        return tunnelServer;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getAppName() {
        return appName;
    }

    public String getStatUrl() {
        return statUrl;
    }

    public String getSelect() {
		return select;
	}

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
