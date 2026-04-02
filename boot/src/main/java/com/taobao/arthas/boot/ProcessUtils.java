package com.taobao.arthas.boot;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.InputMismatchException;

import com.taobao.arthas.common.AnsiLog;
import com.taobao.arthas.common.ExecutingCommand;
import com.taobao.arthas.common.IOUtils;
import com.taobao.arthas.common.JavaVersionUtils;
import com.taobao.arthas.common.PidUtils;

/**
 * 进程工具类
 *
 * 提供了进程查找、选择、启动Arthas核心和客户端等功能。
 * 主要用于Arthas启动器与目标Java进程的交互。
 *
 * @author hengyunabc 2018-11-06
 *
 */
public class ProcessUtils {
    // 缓存已找到的JAVA_HOME路径，避免重复查找
    private static String FOUND_JAVA_HOME = null;

    //状态码来自 com.taobao.arthas.client.TelnetConsole
    /**
     * 进程执行成功
     */
    public static final int STATUS_OK = 0;
    /**
     * 通用错误
     */
    public static final int STATUS_ERROR = 1;
    /**
     * 执行命令超时
     */
    public static final int STATUS_EXEC_TIMEOUT = 100;
    /**
     * 执行命令错误
     */
    public static final int STATUS_EXEC_ERROR = 101;

    /**
     * 选择目标Java进程
     *
     * 该方法会列出当前系统中的所有Java进程，供用户选择要attach的目标进程。
     * 支持通过--select参数自动匹配唯一进程，也支持交互式选择。
     *
     * @param v 是否显示详细信息（-v参数）
     * @param telnetPortPid 已经在监听telnet端口的进程ID，如果存在会排在第一位
     * @param select 通过关键字选择进程，如果只匹配到一个进程则自动选择
     * @return 选中的进程ID，如果选择失败返回-1
     * @throws InputMismatchException 当用户输入无效时抛出
     */
    @SuppressWarnings("resource")
    public static long select(boolean v, long telnetPortPid, String select) throws InputMismatchException {
        // 首先尝试使用jps命令列出Java进程
        Map<Long, String> processMap = listProcessByJps(v);
        // 如果jps没有找到进程，尝试使用jcmd命令
        if (processMap.isEmpty()) {
            processMap = listProcessByJcmd();
            // 如果jcmd也没有找到进程，输出错误提示
            if (processMap.isEmpty()) {
                AnsiLog.error("Cannot find java process. Try to run `jps` or `jcmd` commands to list the instrumented Java HotSpot VMs on the target system.");
                return -1;
            }
        }

        // 将已经在监听telnet端口的进程放在列表第一位
        // 这样用户可以直接按回车选择该进程
        if (telnetPortPid > 0 && processMap.containsKey(telnetPortPid)) {
            // 获取telnet端口对应的进程信息
            String telnetPortProcess = processMap.get(telnetPortPid);
            // 从原映射中移除
            processMap.remove(telnetPortPid);
            // 创建新的有序映射
            Map<Long, String> newProcessMap = new LinkedHashMap<Long, String>();
            // 将telnet端口进程放在第一位
            newProcessMap.put(telnetPortPid, telnetPortProcess);
            // 添加其他进程
            newProcessMap.putAll(processMap);
            // 更新进程映射
            processMap = newProcessMap;
        }

        // 通过--select选项自动选择目标进程（当只匹配一个进程时）
        if (select != null && !select.trim().isEmpty()) {
            int matchedSelectCount = 0;  // 匹配到的进程数量
            Long matchedPid = null;      // 匹配到的进程ID
            // 遍历所有进程，查找包含select关键字的进程
            for (Entry<Long, String> entry : processMap.entrySet()) {
                if (entry.getValue().contains(select)) {
                    matchedSelectCount++;
                    matchedPid = entry.getKey();
                }
            }
            // 如果只匹配到一个进程，直接返回该进程ID
            if (matchedSelectCount == 1) {
                return matchedPid;
            }
        }

        // 提示用户选择进程
        AnsiLog.info("Found existing java process, please choose one and input the serial number of the process, eg : 1. Then hit ENTER.");

        // 打印进程列表，第一个进程标记为*
        int count = 1;
        for (String process : processMap.values()) {
            if (count == 1) {
                // 第一个进程用*标记，表示默认选择
                System.out.println("* [" + count + "]: " + process);
            } else {
                System.out.println("  [" + count + "]: " + process);
            }
            count++;
        }

        // 读取用户输入
        String line = new Scanner(System.in).nextLine();
        // 如果用户直接按回车，选择第一个进程
        if (line.trim().isEmpty()) {
            // 获取第一个进程ID
            return processMap.keySet().iterator().next();
        }

        // 解析用户输入的数字
        int choice = new Scanner(line).nextInt();

        // 检查输入是否在有效范围内
        if (choice <= 0 || choice > processMap.size()) {
            return -1;
        }

        // 根据用户选择找到对应的进程ID
        Iterator<Long> idIter = processMap.keySet().iterator();
        for (int i = 1; i <= choice; ++i) {
            if (i == choice) {
                // 找到用户选择的进程，返回其ID
                return idIter.next();
            }
            idIter.next();
        }

        return -1;
    }

    /**
     * 使用jcmd命令列出Java进程
     *
     * 该方法通过执行jcmd -l命令获取当前系统中的所有Java进程信息。
     * 会自动过滤掉当前进程和jcmd进程本身。
     *
     * @return 进程ID到进程信息的映射
     */
    private static Map<Long, String> listProcessByJcmd() {
        // 使用LinkedHashMap保持进程列表的顺序
        Map<Long, String> result = new LinkedHashMap<>();

        // 默认使用系统PATH中的jcmd命令
        String jcmd = "jcmd";
        // 尝试查找jcmd可执行文件
        File jcmdFile = findJcmd();
        if (jcmdFile != null) {
            jcmd = jcmdFile.getAbsolutePath();
        }

        AnsiLog.debug("Try use jcmd to list java process, jcmd: " + jcmd);

        // 构造jcmd命令：jcmd -l 列出所有Java虚拟机
        String[] command = new String[] { jcmd, "-l" };

        // 执行命令并获取输出
        List<String> lines = ExecutingCommand.runNative(command);

        AnsiLog.debug("jcmd result: " + lines);

        // 获取当前进程ID，用于过滤
        long currentPid = Long.parseLong(PidUtils.currentPid());
        // 解析jcmd输出
        for (String line : lines) {
            // 按空白字符分割每一行
            String[] strings = line.trim().split("\\s+");
            // 跳过空行
            if (strings.length < 1) {
                continue;
            }
            try {
                // 第一个字段是进程ID
                long pid = Long.parseLong(strings[0]);
                // 过滤掉当前进程
                if (pid == currentPid) {
                    continue;
                }
                // 过滤掉jcmd进程本身
                if (strings.length >= 2 && isJcmdProcess(strings[1])) {
                    continue;
                }

                // 将进程信息添加到结果中
                result.put(pid, line);
            } catch (Throwable e) {
                // https://github.com/alibaba/arthas/issues/970
                // 忽略解析错误，继续处理其他行
            }
        }
        return result;
    }

    /**
     * 使用jps命令列出Java进程
     *
     * 该方法通过执行jps命令获取当前系统中的所有Java进程信息。
     * 会自动过滤掉当前进程和jps进程本身。
     *
     * @param v 是否显示详细信息（-v参数，显示JVM参数）
     * @return 进程ID到进程信息的映射
     * @deprecated 使用 {@link #listProcessByJcmd()} 替代
     */
    @Deprecated
    private static Map<Long, String> listProcessByJps(boolean v) {
        // 使用LinkedHashMap保持进程列表的顺序
        Map<Long, String> result = new LinkedHashMap<Long, String>();

        // 默认使用系统PATH中的jps命令
        String jps = "jps";
        // 尝试查找jps可执行文件
        File jpsFile = findJps();
        if (jpsFile != null) {
            jps = jpsFile.getAbsolutePath();
        }

        AnsiLog.debug("Try use jps to list java process, jps: " + jps);

        // 根据v参数构造命令
        String[] command = null;
        if (v) {
            // 显示详细信息：输出完整类名和JVM参数
            command = new String[] { jps, "-v", "-l" };
        } else {
            // 只输出完整类名
            command = new String[] { jps, "-l" };
        }

        // 执行命令并获取输出
        List<String> lines = ExecutingCommand.runNative(command);

        AnsiLog.debug("jps result: " + lines);

        // 获取当前进程ID，用于过滤
        long currentPid = Long.parseLong(PidUtils.currentPid());
        // 解析jps输出
        for (String line : lines) {
            // 按空白字符分割每一行
            String[] strings = line.trim().split("\\s+");
            // 跳过空行
            if (strings.length < 1) {
                continue;
            }
            try {
                // 第一个字段是进程ID
                long pid = Long.parseLong(strings[0]);
                // 过滤掉当前进程
                if (pid == currentPid) {
                    continue;
                }
                // 过滤掉jps进程本身
                if (strings.length >= 2 && isJpsProcess(strings[1])) {
                    continue;
                }

                // 将进程信息添加到结果中
                result.put(pid, line);
            } catch (Throwable e) {
                // https://github.com/alibaba/arthas/issues/970
                // 忽略解析错误，继续处理其他行
            }
        }

        return result;
    }

    /**
     * 查找Java主目录
     *
     * 该方法用于查找正确的JAVA_HOME路径，特别是在需要tools.jar的JDK 8及以下版本。
     *
     * <pre>
     * 查找策略：
     * 1. 尝试从系统属性 java.home 获取Java主目录
     * 2. 如果JDK版本 > 8，直接使用 java.home 作为 JAVA_HOME
     * 3. 如果JDK版本 <= 8，尝试在 java.home 下查找 tools.jar
     * 4. 如果 java.home 下没有 tools.jar，尝试从环境变量 JAVA_HOME 查找
     * 5. 如果JDK <= 8 且在 JAVA_HOME 下也找不到 tools.jar，抛出异常
     * </pre>
     *
     * @return JAVA_HOME路径
     * @throws IllegalArgumentException 如果找不到tools.jar（JDK 8及以下）
     */
    public static String findJavaHome() {
        // 如果已经找到过JAVA_HOME，直接返回缓存值
        if (FOUND_JAVA_HOME != null) {
            return FOUND_JAVA_HOME;
        }

        // 获取Java系统属性中的java.home
        String javaHome = System.getProperty("java.home");

        // 对于Java 9之前的版本，需要查找tools.jar
        if (JavaVersionUtils.isLessThanJava9()) {
            // 尝试在 java.home/lib/ 下查找 tools.jar
            File toolsJar = new File(javaHome, "lib/tools.jar");
            if (!toolsJar.exists()) {
                // 尝试在 java.home/../lib/ 下查找（可能是jre目录）
                toolsJar = new File(javaHome, "../lib/tools.jar");
            }
            if (!toolsJar.exists()) {
                // 尝试在 java.home/../../lib/ 下查找（可能是jre内的jdk目录结构）
                toolsJar = new File(javaHome, "../../lib/tools.jar");
            }

            // 如果找到了tools.jar，使用当前java.home
            if (toolsJar.exists()) {
                FOUND_JAVA_HOME = javaHome;
                return FOUND_JAVA_HOME;
            }

            // 如果在java.home下没找到tools.jar，尝试使用环境变量JAVA_HOME
            if (!toolsJar.exists()) {
                AnsiLog.debug("Can not find tools.jar under java.home: " + javaHome);
                // 获取环境变量JAVA_HOME
                String javaHomeEnv = System.getenv("JAVA_HOME");
                if (javaHomeEnv != null && !javaHomeEnv.isEmpty()) {
                    AnsiLog.debug("Try to find tools.jar in System Env JAVA_HOME: " + javaHomeEnv);
                    // 尝试 $JAVA_HOME/lib/tools.jar
                    toolsJar = new File(javaHomeEnv, "lib/tools.jar");
                    if (!toolsJar.exists()) {
                        // 尝试 $JAVA_HOME/../lib/tools.jar（可能是jre）
                        toolsJar = new File(javaHomeEnv, "../lib/tools.jar");
                    }
                }

                // 在环境变量JAVA_HOME下找到了tools.jar
                if (toolsJar.exists()) {
                    AnsiLog.info("Found java home from System Env JAVA_HOME: " + javaHomeEnv);
                    FOUND_JAVA_HOME = javaHomeEnv;
                    return FOUND_JAVA_HOME;
                }

                // 如果最终找不到tools.jar，抛出异常
                throw new IllegalArgumentException("Can not find tools.jar under java home: " + javaHome
                        + ", please try to start arthas-boot with full path java. Such as /opt/jdk/bin/java -jar arthas-boot.jar");
            }
        } else {
            // Java 9+ 版本不需要tools.jar，直接使用java.home
            FOUND_JAVA_HOME = javaHome;
        }
        return FOUND_JAVA_HOME;
    }

    /**
     * 启动Arthas核心进程
     *
     * 该方法用于启动Arthas的core模块，attach到目标Java进程。
     * 会创建新的Java进程，并重定向其输出到当前控制台。
     *
     * @param targetPid 目标Java进程的PID
     * @param attachArgs attach参数列表
     */
    public static void startArthasCore(long targetPid, List<String> attachArgs) {
        // 查找JAVA_HOME（会查找java/java.exe，然后尝试查找tools.jar）
        String javaHome = findJavaHome();

        // 查找java可执行文件
        File javaPath = findJava(javaHome);
        if (javaPath == null) {
            throw new IllegalArgumentException(
                    "Can not find java/java.exe executable file under java home: " + javaHome);
        }

        // 查找tools.jar（JDK 9+返回null）
        File toolsJar = findToolsJar(javaHome);

        // 对于Java 9之前的版本，必须找到tools.jar
        if (JavaVersionUtils.isLessThanJava9()) {
            if (toolsJar == null || !toolsJar.exists()) {
                throw new IllegalArgumentException("Can not find tools.jar under java home: " + javaHome);
            }
        }

        // 构造启动命令
        List<String> command = new ArrayList<String>();
        // 第一个参数是java可执行文件路径
        command.add(javaPath.getAbsolutePath());

        // 如果找到tools.jar，添加到bootclasspath
        if (toolsJar != null && toolsJar.exists()) {
            command.add("-Xbootclasspath/a:" + toolsJar.getAbsolutePath());
        }

        // 添加attach参数
        command.addAll(attachArgs);

        // 完整的启动命令格式：
        // "${JAVA_HOME}"/bin/java \
        // ${opts} \
        // -jar "${arthas_lib_dir}/arthas-core.jar" \
        // -pid ${TARGET_PID} \
        // -target-ip ${TARGET_IP} \
        // -telnet-port ${TELNET_PORT} \
        // -http-port ${HTTP_PORT} \
        // -core "${arthas_lib_dir}/arthas-core.jar" \
        // -agent "${arthas_lib_dir}/arthas-agent.jar"

        // 创建进程构建器
        ProcessBuilder pb = new ProcessBuilder(command);
        // https://github.com/alibaba/arthas/issues/2166
        // 清空JAVA_TOOL_OPTIONS环境变量，避免干扰
        pb.environment().put("JAVA_TOOL_OPTIONS", "");
        try {
            // 启动进程
            final Process proc = pb.start();

            // 创建线程重定向标准输出
            Thread redirectStdout = new Thread(new Runnable() {
                @Override
                public void run() {
                    InputStream inputStream = proc.getInputStream();
                    try {
                        // 将进程的输出复制到System.out
                        IOUtils.copy(inputStream, System.out);
                    } catch (IOException e) {
                        // 发生异常时关闭输入流
                        IOUtils.close(inputStream);
                    }

                }
            });

            // 创建线程重定向标准错误输出
            Thread redirectStderr = new Thread(new Runnable() {
                @Override
                public void run() {
                    InputStream inputStream = proc.getErrorStream();
                    try {
                        // 将进程的错误输出复制到System.err
                        IOUtils.copy(inputStream, System.err);
                    } catch (IOException e) {
                        // 发生异常时关闭输入流
                        IOUtils.close(inputStream);
                    }

                }
            });

            // 启动重定向线程
            redirectStdout.start();
            redirectStderr.start();
            // 等待重定向线程完成
            redirectStdout.join();
            redirectStderr.join();

            // 获取进程退出码
            int exitValue = proc.exitValue();
            // 如果退出码非0，表示attach失败
            if (exitValue != 0) {
                AnsiLog.error("attach fail, targetPid: " + targetPid);
                // 退出当前程序
                System.exit(1);
            }
        } catch (Throwable e) {
            // 忽略异常
        }
    }

    /**
     * 启动Arthas客户端
     *
     * 该方法通过反射加载arthas-client.jar中的TelnetConsole类，
     * 并调用其process方法启动Telnet客户端连接到Arthas服务端。
     *
     * @param arthasHomeDir Arthas主目录，包含arthas-client.jar
     * @param telnetArgs Telnet客户端参数
     * @param out 输出流，用于重定向客户端输出
     * @return 进程状态码
     * @throws Throwable 如果发生严重错误
     */
    public static int startArthasClient(String arthasHomeDir, List<String> telnetArgs, OutputStream out) throws Throwable {
        // 启动Java telnet客户端
        // 查找arthas-client.jar
        URLClassLoader classLoader = new URLClassLoader(
                new URL[]{new File(arthasHomeDir, "arthas-client.jar").toURI().toURL()});
        // 加载TelnetConsole类
        Class<?> telnetConsoleClass = classLoader.loadClass("com.taobao.arthas.client.TelnetConsole");
        // 获取process方法
        Method processMethod = telnetConsoleClass.getMethod("process", String[].class);

        // 重定向 System.out/System.err 到指定的输出流
        PrintStream originSysOut = System.out;
        PrintStream originSysErr = System.err;
        PrintStream newOut = new PrintStream(out);
        PrintStream newErr = new PrintStream(out);

        // 调用 TelnetConsole.process()
        // 修复 https://github.com/alibaba/arthas/issues/833
        // 保存当前的线程上下文类加载器
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            // 设置新的输出流
            System.setOut(newOut);
            System.setErr(newErr);
            // 设置线程上下文类加载器为客户端类加载器
            Thread.currentThread().setContextClassLoader(classLoader);
            // 通过反射调用process方法
            return (Integer) processMethod.invoke(null, new Object[]{telnetArgs.toArray(new String[0])});
        } catch (Throwable e) {
            // 处理反射调用中的异常
            // java.lang.reflect.InvocationTargetException : java.net.ConnectException
            // 获取真实异常原因
            e = e.getCause();
            // 如果是IO异常或中断异常，忽略
            if (e instanceof IOException || e instanceof InterruptedException) {
                // 忽略连接错误和中断错误
                return STATUS_ERROR;
            } else {
                // 处理其他错误
                AnsiLog.error("process error: {}", e.toString());
                AnsiLog.error(e);
                return STATUS_EXEC_ERROR;
            }
        } finally {
            // 恢复线程上下文类加载器
            Thread.currentThread().setContextClassLoader(tccl);

            // 恢复 System.out/System.err
            System.setOut(originSysOut);
            System.setErr(originSysErr);
            // 刷新输出流
            newOut.flush();
            newErr.flush();
        }
    }

    /**
     * 查找Java可执行文件
     *
     * 在指定的JAVA_HOME下查找java或java.exe可执行文件。
     * 会尝试多个可能的路径，并选择路径最短的一个（JDK路径比JRE路径短）。
     *
     * @param javaHome Java主目录
     * @return 找到的Java可执行文件，如果找不到返回null
     */
    private static File findJava(String javaHome) {
        // 可能的java可执行文件路径
        String[] paths = { "bin/java", "bin/java.exe", "../bin/java", "../bin/java.exe" };

        // 收集所有存在的java文件
        List<File> javaList = new ArrayList<File>();
        for (String path : paths) {
            File javaFile = new File(javaHome, path);
            if (javaFile.exists()) {
                AnsiLog.debug("Found java: " + javaFile.getAbsolutePath());
                javaList.add(javaFile);
            }
        }

        // 如果没有找到java文件
        if (javaList.isEmpty()) {
            AnsiLog.debug("Can not find java/java.exe under current java home: " + javaHome);
            return null;
        }

        // 如果找到多个java文件，选择路径最短的一个
        // 因为jre路径比jdk路径长，所以选择最短的即是jdk
        if (javaList.size() > 1) {
            Collections.sort(javaList, new Comparator<File>() {
                @Override
                public int compare(File file1, File file2) {
                    try {
                        // 比较规范路径的长度
                        return file1.getCanonicalPath().length() - file2.getCanonicalPath().length();
                    } catch (IOException e) {
                        // 忽略异常
                    }
                    return -1;
                }
            });
        }
        // 返回路径最短的java文件
        return javaList.get(0);
    }

    /**
     * 查找tools.jar文件
     *
     * 在指定的JAVA_HOME下查找tools.jar。
     * JDK 9+不需要tools.jar，返回null。
     *
     * @param javaHome Java主目录
     * @return tools.jar文件，JDK 9+返回null
     * @throws IllegalArgumentException 如果找不到tools.jar（JDK 8及以下）
     */
    private static File findToolsJar(String javaHome) {
        // JDK 9+ 不需要tools.jar
        if (JavaVersionUtils.isGreaterThanJava8()) {
            return null;
        }

        // 尝试多个可能的路径
        File toolsJar = new File(javaHome, "lib/tools.jar");
        if (!toolsJar.exists()) {
            // 尝试 ../lib/tools.jar（可能是jre目录）
            toolsJar = new File(javaHome, "../lib/tools.jar");
        }
        if (!toolsJar.exists()) {
            // 尝试 ../../lib/tools.jar（可能是jre内的jdk目录结构）
            toolsJar = new File(javaHome, "../../lib/tools.jar");
        }

        // 如果找不到tools.jar，抛出异常
        if (!toolsJar.exists()) {
            throw new IllegalArgumentException("Can not find tools.jar under java home: " + javaHome);
        }

        AnsiLog.debug("Found tools.jar: " + toolsJar.getAbsolutePath());
        return toolsJar;
    }

    /**
     * 查找jcmd可执行文件
     *
     * 在java.home和JAVA_HOME环境变量指定的目录下查找jcmd可执行文件。
     * 会尝试多个可能的路径，并选择路径最短的一个。
     *
     * @return 找到的jcmd文件，如果找不到返回null
     */
    private static File findJcmd() {
        // 尝试在java.home和系统环境变量JAVA_HOME下查找jcmd
        String javaHome = System.getProperty("java.home");
        // 可能的jcmd路径
        String[] paths = { "bin/jcmd", "bin/jcmd.exe", "../bin/jcmd", "../bin/jcmd.exe" };

        // 收集所有存在的jcmd文件
        List<File> jcmdList = new ArrayList<>();
        for (String path : paths) {
            File jcmdFile = new File(javaHome, path);
            if (jcmdFile.exists()) {
                AnsiLog.debug("Found jcmd: " + jcmdFile.getAbsolutePath());
                jcmdList.add(jcmdFile);
            }
        }

        // 如果在java.home下没找到，尝试使用JAVA_HOME环境变量
        if (jcmdList.isEmpty()) {
            AnsiLog.debug("Can not find jcmd under :" + javaHome);
            String javaHomeEnv = System.getenv("JAVA_HOME");
            AnsiLog.debug("Try to find jcmd under env JAVA_HOME :" + javaHomeEnv);
            if (javaHomeEnv != null) {
                for (String path : paths) {
                    File jcmdFile = new File(javaHomeEnv, path);
                    if (jcmdFile.exists()) {
                        AnsiLog.debug("Found jcmd: " + jcmdFile.getAbsolutePath());
                        jcmdList.add(jcmdFile);
                    }
                }
            } else {
                AnsiLog.debug("JAVA_HOME environment variable is not set.");
            }
        }

        // 如果没有找到jcmd
        if (jcmdList.isEmpty()) {
            AnsiLog.debug("Can not find jcmd under current java home: " + javaHome);
            return null;
        }

        // 如果找到多个jcmd文件，选择路径最短的一个
        // 因为jre路径比jdk路径长，所以选择最短的即是jdk
        if (jcmdList.size() > 1) {
            jcmdList.sort((file1, file2) -> {
                try {
                    // 比较规范路径的长度
                    return file1.getCanonicalPath().length() - file2.getCanonicalPath().length();
                } catch (IOException e) {
                    // 忽略异常
                    // 降级为绝对路径长度比较
                    return file1.getAbsolutePath().length() - file2.getAbsolutePath().length();
                }
            });
        }
        // 返回路径最短的jcmd文件
        return jcmdList.get(0);
    }

    /**
     * 判断是否为jcmd进程
     *
     * 根据主类名判断是否是jcmd命令本身的进程。
     * 支持Java 8和Java 9+的模块化类名格式。
     *
     * @param mainClassName 主类名
     * @return 如果是jcmd进程返回true，否则返回false
     */
    private static boolean isJcmdProcess(String mainClassName) {
        // Java 8 或 Java 9+ 的模块化格式
        return "sun.tools.jcmd.JCmd".equals(mainClassName) || "jdk.jcmd/sun.tools.jcmd.JCmd".equals(mainClassName);
    }

    /**
     * 查找jps可执行文件
     *
     * 在java.home和JAVA_HOME环境变量指定的目录下查找jps可执行文件。
     * 会尝试多个可能的路径，并选择路径最短的一个。
     *
     * @return 找到的jps文件，如果找不到返回null
     * @deprecated 使用 {@link #findJcmd()} 替代
     */
    @Deprecated
    private static File findJps() {
        // 尝试在java.home和系统环境变量JAVA_HOME下查找jps
        String javaHome = System.getProperty("java.home");
        // 可能的jps路径
        String[] paths = { "bin/jps", "bin/jps.exe", "../bin/jps", "../bin/jps.exe" };

        // 收集所有存在的jps文件
        List<File> jpsList = new ArrayList<File>();
        for (String path : paths) {
            File jpsFile = new File(javaHome, path);
            if (jpsFile.exists()) {
                AnsiLog.debug("Found jps: " + jpsFile.getAbsolutePath());
                jpsList.add(jpsFile);
            }
        }

        // 如果在java.home下没找到，尝试使用JAVA_HOME环境变量
        if (jpsList.isEmpty()) {
            AnsiLog.debug("Can not find jps under :" + javaHome);
            String javaHomeEnv = System.getenv("JAVA_HOME");
            AnsiLog.debug("Try to find jps under env JAVA_HOME :" + javaHomeEnv);
            for (String path : paths) {
                File jpsFile = new File(javaHomeEnv, path);
                if (jpsFile.exists()) {
                    AnsiLog.debug("Found jps: " + jpsFile.getAbsolutePath());
                    jpsList.add(jpsFile);
                }
            }
        }

        // 如果没有找到jps
        if (jpsList.isEmpty()) {
            AnsiLog.debug("Can not find jps under current java home: " + javaHome);
            return null;
        }

        // 如果找到多个jps文件，选择路径最短的一个
        // 因为jre路径比jdk路径长，所以选择最短的即是jdk
        if (jpsList.size() > 1) {
            Collections.sort(jpsList, new Comparator<File>() {
                @Override
                public int compare(File file1, File file2) {
                    try {
                        // 比较规范路径的长度
                        return file1.getCanonicalPath().length() - file2.getCanonicalPath().length();
                    } catch (IOException e) {
                        // 忽略异常
                    }
                    return -1;
                }
            });
        }
        // 返回路径最短的jps文件
        return jpsList.get(0);
    }

    /**
     * 判断是否为jps进程
     *
     * 根据主类名判断是否是jps命令本身的进程。
     * 支持Java 8和Java 9+的模块化类名格式。
     *
     * @param mainClassName 主类名
     * @return 如果是jps进程返回true，否则返回false
     * @deprecated 使用 {@link #isJcmdProcess(String)} 替代
     */
    @Deprecated
    private static boolean isJpsProcess(String mainClassName) {
        // Java 8 或 Java 9+ 的模块化格式
        return "sun.tools.jps.Jps".equals(mainClassName) || "jdk.jcmd/sun.tools.jps.Jps".equals(mainClassName);
    }
}
