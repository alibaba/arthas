package com.taobao.arthas.core;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.taobao.arthas.common.AnsiLog;
import com.taobao.arthas.common.ArthasConstants;
import com.taobao.arthas.common.ExecutingCommand;
import com.taobao.arthas.common.JavaVersionUtils;
import com.taobao.arthas.common.PidUtils;
import com.taobao.arthas.core.config.Configure;
import com.taobao.middleware.cli.CLI;
import com.taobao.middleware.cli.CLIs;
import com.taobao.middleware.cli.CommandLine;
import com.taobao.middleware.cli.Option;
import com.taobao.middleware.cli.TypedOption;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Arthas启动器
 */
public class Arthas {

    private Arthas(String[] args) throws Exception {
        attachAgent(parse(args));
    }

    private Configure parse(String[] args) {
        Option pid = new TypedOption<Long>().setType(Long.class).setShortName("pid").setRequired(true);
        Option core = new TypedOption<String>().setType(String.class).setShortName("core").setRequired(true);
        Option agent = new TypedOption<String>().setType(String.class).setShortName("agent").setRequired(true);
        Option target = new TypedOption<String>().setType(String.class).setShortName("target-ip");
        Option telnetPort = new TypedOption<Integer>().setType(Integer.class)
                .setShortName("telnet-port");
        Option httpPort = new TypedOption<Integer>().setType(Integer.class)
                .setShortName("http-port");
        Option sessionTimeout = new TypedOption<Integer>().setType(Integer.class)
                        .setShortName("session-timeout");

        Option username = new TypedOption<String>().setType(String.class).setShortName("username");
        Option password = new TypedOption<String>().setType(String.class).setShortName("password");

        Option tunnelServer = new TypedOption<String>().setType(String.class).setShortName("tunnel-server");
        Option agentId = new TypedOption<String>().setType(String.class).setShortName("agent-id");
        Option appName = new TypedOption<String>().setType(String.class).setShortName(ArthasConstants.APP_NAME);

        Option statUrl = new TypedOption<String>().setType(String.class).setShortName("stat-url");
        Option disabledCommands = new TypedOption<String>().setType(String.class).setShortName("disabled-commands");
        Option commandLocations = new TypedOption<String>().setType(String.class).setShortName("command-locations");

        CLI cli = CLIs.create("arthas").addOption(pid).addOption(core).addOption(agent).addOption(target)
                .addOption(telnetPort).addOption(httpPort).addOption(sessionTimeout)
                .addOption(username).addOption(password)
                .addOption(tunnelServer).addOption(agentId).addOption(appName).addOption(statUrl)
                .addOption(disabledCommands).addOption(commandLocations);
        CommandLine commandLine = cli.parse(Arrays.asList(args));

        Configure configure = new Configure();
        configure.setJavaPid((Long) commandLine.getOptionValue("pid"));
        configure.setArthasAgent((String) commandLine.getOptionValue("agent"));
        configure.setArthasCore((String) commandLine.getOptionValue("core"));
        if (commandLine.getOptionValue("session-timeout") != null) {
            configure.setSessionTimeout((Integer) commandLine.getOptionValue("session-timeout"));
        }

        if (commandLine.getOptionValue("target-ip") != null) {
            configure.setIp((String) commandLine.getOptionValue("target-ip"));
        }

        if (commandLine.getOptionValue("telnet-port") != null) {
            configure.setTelnetPort((Integer) commandLine.getOptionValue("telnet-port"));
        }
        if (commandLine.getOptionValue("http-port") != null) {
            configure.setHttpPort((Integer) commandLine.getOptionValue("http-port"));
        }

        configure.setUsername((String) commandLine.getOptionValue("username"));
        configure.setPassword((String) commandLine.getOptionValue("password"));

        configure.setTunnelServer((String) commandLine.getOptionValue("tunnel-server"));
        configure.setAgentId((String) commandLine.getOptionValue("agent-id"));
        configure.setStatUrl((String) commandLine.getOptionValue("stat-url"));
        configure.setDisabledCommands((String) commandLine.getOptionValue("disabled-commands"));
        configure.setCommandLocations((String) commandLine.getOptionValue("command-locations"));
        configure.setAppName((String) commandLine.getOptionValue(ArthasConstants.APP_NAME));
        return configure;
    }

    private void attachAgent(Configure configure) throws Exception {
        long targetPid = configure.getJavaPid();

        // k8s 临时 debug 容器等场景：与目标进程共享 pid namespace，但 mount namespace 独立。
        // attach socket 与 agent/core jar 都需要跨 mount namespace 暴露给目标 JVM。
        boolean crossMountNamespace = inDifferentMountNamespace(targetPid);
        Path socketSymlink = null;
        if (crossMountNamespace) {
            socketSymlink = exposeTargetAttachSocket(targetPid);
        }

        VirtualMachineDescriptor virtualMachineDescriptor = null;
        for (VirtualMachineDescriptor descriptor : VirtualMachine.list()) {
            String pid = descriptor.id();
            if (pid.equals(Long.toString(targetPid))) {
                virtualMachineDescriptor = descriptor;
                break;
            }
        }
        VirtualMachine virtualMachine = null;
        try {
            if (null == virtualMachineDescriptor) { // 使用 attach(String pid) 这种方式
                virtualMachine = VirtualMachine.attach("" + targetPid);
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
            String arthasCorePath = configure.getArthasCore();
            if (crossMountNamespace) {
                // 让目标 JVM 在自己的 mount namespace 里也能访问到 agent/core jar
                String[] targetPaths = resolveAgentPathsForTarget(targetPid, arthasAgentPath, arthasCorePath);
                arthasAgentPath = targetPaths[0];
                arthasCorePath = targetPaths[1];
            }
            //convert jar path to unicode string
            configure.setArthasAgent(encodeArg(arthasAgentPath));
            configure.setArthasCore(encodeArg(arthasCorePath));
            try {
                virtualMachine.loadAgent(arthasAgentPath,
                        configure.getArthasCore() + ";" + configure.toString());
            } catch (IOException e) {
                if (e.getMessage() != null && e.getMessage().contains("Non-numeric value found")) {
                    AnsiLog.warn(e);
                    AnsiLog.warn("It seems to use the lower version of JDK to attach the higher version of JDK.");
                    AnsiLog.warn(
                            "This error message can be ignored, the attach may have been successful, and it will still try to connect.");
                } else {
                    throw e;
                }
            } catch (com.sun.tools.attach.AgentLoadException ex) {
                if ("0".equals(ex.getMessage())) {
                    // https://stackoverflow.com/a/54454418
                    AnsiLog.warn(ex);
                    AnsiLog.warn("It seems to use the higher version of JDK to attach the lower version of JDK.");
                    AnsiLog.warn(
                            "This error message can be ignored, the attach may have been successful, and it will still try to connect.");
                } else {
                    throw ex;
                }
            }

            // 健康校验：loadAgent 返回并不代表目标侧 arthas server 已绑定成功
            // (AgentBootstrap 的 binding 线程会吞掉 bind 异常，loadAgent 仍"成功")。跨 ns 场景下
            // 资源复制不全等原因会导致 server 静默不起，这里主动探测 telnet 端口，失败则大声报错并指路 arthas.log。
            if (crossMountNamespace) {
                checkServerStarted(configure, targetPid);
            }
        } finally {
            if (null != virtualMachine) {
                virtualMachine.detach();
            }
            cleanupSymlink(socketSymlink);
        }
    }

    private static boolean isLinux() {
        return System.getProperty("os.name", "").toLowerCase().contains("linux");
    }

    /*
     * =============================================================================================
     * 跨 mount namespace attach 支持（k8s 临时 debug 容器场景）——这是一套 workaround，不是标准能力
     * =============================================================================================
     *
     * 背景：
     *   用 `kubectl debug` 的临时容器调试应用容器时，两个容器【共享 pid namespace】但【mount namespace 相互独立】。
     *   JDK 的 attach 机制（sun.tools.attach.LinuxVirtualMachine）默认假设 attach 发起方与目标 JVM 在同一文件系统，
     *   它依赖：① 目标 tmpdir 下的 .java_pid<pid> socket；② loadAgent 时目标 JVM 能读取 agent/core jar 路径。
     *   这两点在跨 mount namespace 下都不成立，直接 attach 会报 "Unable to open socket file" / "Agent JAR not found"。
     *
     * 本 workaround 的做法：
     *   1. socket：在本进程 tmpdir 建软链指向 /proc/<targetPid>/root<targetTmp>/.java_pid<pid>（见 exposeTargetAttachSocket）。
     *   2. jar/资源：由本进程(通常 root)把整个 arthas home 复制进目标容器 tmpdir，返回目标侧本地路径
     *      （见 copyArthasHomeIntoTarget）。之所以要复制【整个 home】而非只复制 agent/core 两个 jar，是因为
     *      ArthasBootstrap 启动时还要从 home 读取 arthas-spy.jar(注入 bootstrap classloader，缺失即失败)、
     *      async-profiler、arthas.properties 等。复制的目录用 arthas-<targetPid> 命名(幂等、止漏)，
     *      并写入标记文件 CROSS_NS_TEMP_HOME_MARKER，供目标侧 stop/destroy 时清理。
     *
     * 前置条件（不满足则退回原有行为或直接失败）：
     *   - 仅 Linux；
     *   - 发起方与目标【共享 pid namespace】（否则 /proc/<targetPid> 不可见，inDifferentMountNamespace 返回 false）；
     *   - 当双方 UID 不同（如 root 的 debug 容器 attach 普通用户应用）时，发起方需要 root 权限，
     *     才能经 /proc/<targetPid>/root 写入目标文件系统；
     *   - 复制/软链依赖对目标 tmpdir 的正确探测（detectTargetTmpDir），探测不到时会告警并假定 /tmp。
     *
     * 局限：
     *   - 强耦合 JDK attach 的实现细节（.java_pid 命名、tmpdir 约定、.attach_pid+SIGQUIT 触发），JDK 版本变更可能失效；
     *   - 冷启动(目标 attach listener 未起)路径依赖 JDK 经 /proc/<targetPid>/cwd 写 .attach_pid 触发，受权限影响；
     *   - 更"正统"的替代方案是运维层面消除双文件系统：kubectl exec 进应用容器、两容器挂共享 emptyDir、或走 tunnel server。
     * =============================================================================================
     */

    /**
     * 判断当前进程(arthas-core)与目标进程是否处于不同的 mount namespace。
     * k8s 临时 debug 容器与目标应用容器共享 pid namespace，但 mount namespace 相互独立。
     */
    private static boolean inDifferentMountNamespace(long targetPid) {
        if (!isLinux()) {
            return false;
        }
        try {
            String selfNs = Files.readSymbolicLink(Paths.get("/proc/self/ns/mnt")).toString();
            String targetNs = Files.readSymbolicLink(Paths.get("/proc/" + targetPid + "/ns/mnt")).toString();
            return !selfNs.equals(targetNs);
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * attach socket 位于目标进程自己的 tmpdir 中，本进程在自己的 tmpdir 里看不到。
     * 通过 /proc/&lt;targetPid&gt;/root 建立软链，使 JDK 的 attach 机制能在本进程 tmpdir 找到它。
     * 若 socket 尚未创建(冷启动)，先建悬空软链，JDK 触发 attach listener 创建 socket 后即可解析成功。
     */
    private static Path exposeTargetAttachSocket(long targetPid) {
        String localTmp = System.getProperty("java.io.tmpdir");
        Path localSocket = Paths.get(localTmp, ".java_pid" + targetPid);
        if (Files.exists(localSocket)) {
            return null; // 同容器正常场景，socket 已可见
        }
        String targetTmp = detectTargetTmpDir(targetPid);
        Path targetSocket = Paths.get("/proc/" + targetPid + "/root" + targetTmp, ".java_pid" + targetPid);
        try {
            // 清理可能残留的(悬空)软链，避免 createSymbolicLink 因已存在而失败
            Files.deleteIfExists(localSocket);
            Files.createSymbolicLink(localSocket, targetSocket);
            // 内部实现细节，不在终端输出（仅 --verbose 时可见）
            AnsiLog.debug("Created symlink {} -> {} to expose target attach socket across mount namespaces",
                    localSocket, targetSocket);
            return localSocket;
        } catch (Throwable e) {
            AnsiLog.warn("Failed to create attach socket symlink: {}", e.toString());
            return null;
        }
    }

    /**
     * 让目标 JVM 能加载到 agent/core jar：
     * <pre>
     * 1. 首选：本进程(通常为 root 的 debug 容器)把 jar 复制进目标容器自己的 tmpdir，返回目标侧本地路径。
     *    适用于双方 UID 不同的场景——普通用户的应用 JVM 无权读取 root 容器的 /proc/&lt;pid&gt;/root。
     * 2. 兜底：同 UID 时直接用 /proc/&lt;本进程pid&gt;/root 前缀暴露本容器文件，无需复制。
     * </pre>
     */
    private static String[] resolveAgentPathsForTarget(long targetPid, String agentJar, String coreJar) {
        try {
            return copyArthasHomeIntoTarget(targetPid, agentJar, coreJar);
        } catch (Throwable e) {
            AnsiLog.warn("Copy arthas home into target failed ({}), fallback to /proc/self/root path", e.toString());
            String selfRoot = "/proc/" + PidUtils.currentPid() + "/root";
            return new String[] { selfRoot + agentJar, selfRoot + coreJar };
        }
    }

    /**
     * 把整个 arthas home(arthas-core.jar 所在目录)递归复制进目标容器的 tmpdir。
     * <p>
     * 不能只复制 agent/core 两个 jar——ArthasBootstrap 启动时还会从 arthas home 读取
     * arthas-spy.jar(注入 bootstrap classloader，缺失会直接抛异常)、async-profiler、
     * arthas.properties 等资源；资源不全会导致 bind 失败、telnet 端口连不上。
     */
    private static String[] copyArthasHomeIntoTarget(long targetPid, String agentJar, String coreJar)
            throws IOException {
        Path arthasHome = Paths.get(coreJar).toAbsolutePath().getParent();
        String targetTmp = detectTargetTmpDir(targetPid);
        // 用【目标 pid】命名(而非本进程 pid)：对同一目标幂等复用、原地覆盖，避免反复 attach 在目标 /tmp 堆积多份
        String dirName = "arthas-" + targetPid;
        Path targetHome = Paths.get("/proc/" + targetPid + "/root" + targetTmp, dirName);

        try (Stream<Path> stream = Files.walk(arthasHome)) {
            for (Path src : (Iterable<Path>) stream::iterator) {
                Path dest = targetHome.resolve(arthasHome.relativize(src).toString());
                if (Files.isDirectory(src)) {
                    Files.createDirectories(dest);
                    setPosixPermissions(dest, "rwxr-xr-x");
                } else {
                    Files.createDirectories(dest.getParent());
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                    // 放开 other 读权限，确保目标容器里的(普通用户)进程可读；源文件可执行则保留执行位
                    setPosixPermissions(dest, Files.isExecutable(src) ? "rwxr-xr-x" : "rw-r--r--");
                }
            }
        }

        // 写入标记文件：目标侧 ArthasBootstrap 在 stop/destroy 时据此清理整个临时目录
        Path marker = targetHome.resolve(ArthasConstants.CROSS_NS_TEMP_HOME_MARKER);
        Files.write(marker, new byte[0]);
        setPosixPermissions(marker, "rw-r--r--");

        // 本进程以 root 复制进来的文件属主是 root，目标侧普通用户在 stop 时无权删除。
        // 把整个临时目录属主改为目标进程用户，确保 stop 时能清理掉。
        chownToTargetUser(targetPid, targetHome);

        String base = targetTmp + "/" + dirName;
        return new String[] { base + "/" + Paths.get(agentJar).getFileName(),
                base + "/" + Paths.get(coreJar).getFileName() };
    }

    private static void setPosixPermissions(Path path, String perms) {
        try {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(perms));
        } catch (Throwable e) {
            // 非 POSIX 文件系统或权限不足时忽略，尽量不影响主流程
            AnsiLog.debug("Failed to set permissions {} on {}: {}", perms, path, e.toString());
        }
    }

    /**
     * 把复制进目标容器的临时目录属主改为目标进程的用户。
     * <p>
     * 本进程(通常 root)复制的文件属主是 root、目录 755，目标侧普通用户在 stop 时没有父目录写权限、
     * 删不掉这些文件，导致临时目录清理失败。改为目标用户属主后即可正常清理。
     * 仅在本进程为 root(跨 UID 场景)时 chown 才会成功；同 UID 时本就不需要，失败不影响主流程。
     */
    private static void chownToTargetUser(long targetPid, Path targetHome) {
        int[] uidGid = readTargetUidGid(targetPid);
        if (uidGid == null) {
            return;
        }
        try {
            ExecutingCommand.runNative(new String[] { "chown", "-R", uidGid[0] + ":" + uidGid[1],
                    targetHome.toString() });
        } catch (Throwable e) {
            AnsiLog.debug("Failed to chown {} to {}:{}: {}", targetHome, uidGid[0], uidGid[1], e.toString());
        }
    }

    /**
     * 读取目标进程的真实 uid/gid（/proc/&lt;pid&gt;/status 里 Uid/Gid 行的第一个值）。
     */
    private static int[] readTargetUidGid(long targetPid) {
        try {
            String status = new String(Files.readAllBytes(Paths.get("/proc/" + targetPid + "/status")));
            int uid = -1;
            int gid = -1;
            for (String line : status.split("\n")) {
                if (line.startsWith("Uid:")) {
                    uid = parseFirstInt(line);
                } else if (line.startsWith("Gid:")) {
                    gid = parseFirstInt(line);
                }
            }
            if (uid >= 0 && gid >= 0) {
                return new int[] { uid, gid };
            }
        } catch (Throwable e) {
            AnsiLog.debug("Failed to read uid/gid of target {}: {}", targetPid, e.toString());
        }
        return null;
    }

    private static int parseFirstInt(String line) {
        String[] parts = line.trim().split("\\s+");
        if (parts.length >= 2) {
            try {
                return Integer.parseInt(parts[1]);
            } catch (Throwable e) {
                // ignore
            }
        }
        return -1;
    }

    /**
     * 推断目标进程的 java.io.tmpdir：优先解析 /proc/&lt;pid&gt;/cmdline 里的 -Djava.io.tmpdir=，
     * 其次 environ 里的 TMPDIR，最后退回 /tmp。
     */
    private static String detectTargetTmpDir(long targetPid) {
        boolean cmdlineReadable = false;
        try {
            String cmdline = new String(Files.readAllBytes(Paths.get("/proc/" + targetPid + "/cmdline")));
            cmdlineReadable = true;
            for (String arg : cmdline.split("\0")) {
                if (arg.startsWith("-Djava.io.tmpdir=")) {
                    return arg.substring("-Djava.io.tmpdir=".length());
                }
            }
        } catch (Throwable e) {
            // ignore
        }
        try {
            String environ = new String(Files.readAllBytes(Paths.get("/proc/" + targetPid + "/environ")));
            for (String env : environ.split("\0")) {
                if (env.startsWith("TMPDIR=")) {
                    return env.substring("TMPDIR=".length());
                }
            }
        } catch (Throwable e) {
            // ignore
        }
        if (!cmdlineReadable) {
            // 连 cmdline 都读不到(通常是权限不足)，无法确认目标是否用了自定义 tmpdir——明确告警而非静默
            AnsiLog.warn("Cannot read /proc/{}/cmdline to detect target java.io.tmpdir (permission denied?), "
                    + "assuming /tmp. If the target uses a custom tmpdir, attach may fail.", targetPid);
        }
        return "/tmp";
    }

    private static void cleanupSymlink(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (Throwable e) {
                // ignore
            }
        }
    }

    /**
     * attach 之后主动探测目标侧 arthas telnet 端口是否真的起来了。
     * loadAgent 返回并不代表 server 绑定成功(AgentBootstrap 会吞掉 bind 异常)，
     * 探测失败则大声报错并指路目标的 arthas.log，避免"Attach success 但连不上"的假成功。
     */
    private static void checkServerStarted(Configure configure, long targetPid) {
        String ip = configure.getIp() != null ? configure.getIp() : "127.0.0.1";
        int telnetPort = configure.getTelnetPort() != null ? configure.getTelnetPort() : ArthasConstants.TELNET_PORT;

        // agent 内 binding 线程通常在 loadAgent 返回前已完成绑定，这里再做几次重试以防万一
        long deadline = System.currentTimeMillis() + 5000L;
        while (System.currentTimeMillis() < deadline) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(ip, telnetPort), 1000);
                return; // 连上了，server 已起
            } catch (Throwable e) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // 失败：大声报错，并给出可操作的排查路径
        String copiedHome = "/proc/" + targetPid + "/root" + detectTargetTmpDir(targetPid) + "/arthas-" + targetPid;
        AnsiLog.error("Arthas server did NOT start on {}:{} after attach. The attach handshake succeeded, but the "
                + "in-target server failed to bind.", ip, telnetPort);
        AnsiLog.error("Common cause under cross mount-namespace: incomplete resources copied into the target, or a "
                + "bind failure inside the target JVM.");
        AnsiLog.error("Copied arthas home in target: {}", copiedHome);
        AnsiLog.error("Check the target's arthas.log for the real cause, e.g.: find /proc/{}/root -name arthas.log",
                targetPid);
        throw new IllegalStateException("Arthas server did not start after attach (no listener on " + ip + ":"
                + telnetPort + "), see errors above and the target's arthas.log");
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
