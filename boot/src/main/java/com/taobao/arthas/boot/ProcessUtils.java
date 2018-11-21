package com.taobao.arthas.boot;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.taobao.arthas.common.AnsiLog;
import com.taobao.arthas.common.ExecutingCommand;
import com.taobao.arthas.common.IOUtils;
import com.taobao.arthas.common.JavaVersionUtils;

/**
 *
 * @author hengyunabc 2018-11-06
 *
 */
public class ProcessUtils {
    private static String PID = "-1";

    static {
        // https://stackoverflow.com/a/7690178
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        int index = jvmName.indexOf('@');

        if (index > 0) {
            try {
                PID = Long.toString(Long.parseLong(jvmName.substring(0, index)));
            } catch (Throwable e) {
                // ignore
            }
        }
    }

    public static String getPid() {
        return PID;
    }

    public static int select(boolean v) {
        Map<Integer, String> processMap = listProcessByJps(v);

        if (processMap.isEmpty()) {
            AnsiLog.info("Can not find java process. Try to pass pid in command line.");
            return -1;
        }

        // print list
        int count = 1;
        for (String process : processMap.values()) {
            if (count == 1) {
                System.out.println("* [" + count + "]: " + process);
            } else {
                System.out.println("  [" + count + "]: " + process);
            }
            count++;
        }

        // read choice
        String line = new Scanner(System.in).nextLine();
        if (line.trim().isEmpty()) {
            // get the first process id
            return processMap.keySet().iterator().next();
        }

        int choice = new Scanner(line).nextInt();

        if (choice <= 0 || choice > processMap.size()) {
            return -1;
        }

        Iterator<Integer> idIter = processMap.keySet().iterator();
        for (int i = 1; i <= choice; ++i) {
            if (i == choice) {
                return idIter.next();
            }
            idIter.next();
        }

        return -1;
    }

    private static Map<Integer, String> listProcessByJps(boolean v) {
        Map<Integer, String> result = new LinkedHashMap<Integer, String>();

        String jps = "jps";
        File jpsFile = findJps();
        if (jpsFile != null) {
            jps = jpsFile.getAbsolutePath();
        }

        String[] command = null;
        if (v) {
            command = new String[] { jps, "-v" };
        } else {
            command = new String[] { jps };
        }

        List<String> lines = ExecutingCommand.runNative(command);

        int currentPid = Integer.parseInt(ProcessUtils.getPid());
        for (String line : lines) {
            String[] strings = line.trim().split("\\s+");
            if (strings.length < 1) {
                continue;
            }
            int pid = Integer.parseInt(strings[0]);
            if (pid == currentPid) {
                continue;
            }
            if (strings.length >= 2 && strings[1].equals("Jps")) { // skip jps
                continue;
            }

            result.put(pid, line);
        }

        return result;
    }

    public static void startArthasCore(int targetPid, List<String> attachArgs) {
        // find java/java.exe, then try to find tools.jar
        String javaHome = System.getProperty("java.home");

        // find java/java.exe
        File javaPath = findJava();
        if (javaPath == null) {
            throw new IllegalArgumentException(
                            "Can not find java/java.exe executable file under java home: " + javaHome);
        }

        File toolsJar = new File(javaHome, "../lib/tools.jar");
        if (!toolsJar.exists()) {
            // maybe jre
            toolsJar = new File(javaHome, "../../lib/tools.jar");
        }

        if (JavaVersionUtils.isLessThanJava9()) {
            if (!toolsJar.exists()) {
                throw new IllegalArgumentException("Can not find tools.jar under java home: " + javaHome);
            }
        }

        List<String> command = new ArrayList<String>();
        command.add(javaPath.getAbsolutePath());

        if (toolsJar.exists()) {
            command.add("-Xbootclasspath/a:" + toolsJar.getAbsolutePath());
        }

        command.addAll(attachArgs);
        // "${JAVA_HOME}"/bin/java \
        // ${opts} \
        // -jar "${arthas_lib_dir}/arthas-core.jar" \
        // -pid ${TARGET_PID} \
        // -target-ip ${TARGET_IP} \
        // -telnet-port ${TELNET_PORT} \
        // -http-port ${HTTP_PORT} \
        // -core "${arthas_lib_dir}/arthas-core.jar" \
        // -agent "${arthas_lib_dir}/arthas-agent.jar"

        ProcessBuilder pb = new ProcessBuilder(command);
        try {
            final Process proc = pb.start();
            Thread redirectStdout = new Thread(new Runnable() {
                @Override
                public void run() {
                    InputStream inputStream = proc.getInputStream();
                    try {
                        IOUtils.copy(inputStream, System.out);
                    } catch (IOException e) {
                        IOUtils.close(inputStream);
                    }

                }
            });

            Thread redirectStderr = new Thread(new Runnable() {
                @Override
                public void run() {
                    InputStream inputStream = proc.getErrorStream();
                    try {
                        IOUtils.copy(inputStream, System.err);
                    } catch (IOException e) {
                        IOUtils.close(inputStream);
                    }

                }
            });
            redirectStdout.start();
            redirectStderr.start();
            redirectStdout.join();
            redirectStderr.join();

            int exitValue = proc.exitValue();
            if (exitValue != 0) {
                AnsiLog.error("attach fail, targetPid: " + targetPid);
                System.exit(1);
            }
        } catch (Throwable e) {
            // ignore
        }

    }

    private static File findJava() {
        String javaHome = System.getProperty("java.home");
        String[] paths = { "bin/java", "bin/java.exe", "../bin/java", "../bin/java.exe" };

        for (String path : paths) {
            File jpsFile = new File(javaHome, path);
            if (jpsFile.exists()) {
                return jpsFile;
            }
        }

        AnsiLog.debug("can not find java under current java home: " + javaHome);
        return null;
    }

    private static File findJps() {
        String javaHome = System.getProperty("java.home");
        String[] paths = { "bin/jps", "bin/jps.exe", "../bin/jps", "../bin/jps.exe" };

        for (String path : paths) {
            File jpsFile = new File(javaHome, path);
            if (jpsFile.exists()) {
                return jpsFile;
            }
        }

        AnsiLog.debug("can not find jps under current java home: " + javaHome);
        return null;
    }

}
