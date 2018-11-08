package com.taobao.arthas.boot;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

/**
 *
 * @author hengyunabc 2018-11-06
 *
 */
public class ProcessUtils {
    private static final Logger logger = LoggerFactory.getLogger(ProcessUtils.class);
    public static int select(boolean v) {
        Map<Integer, String> processMap = listProcessByJps(v);
        if(processMap.isEmpty()) {
            processMap = listProcessByOshi();
        }

        if(processMap.isEmpty()) {
            System.out.println("Can not find java process.");
            return -1;
        }

        //print list
        int count = 1;
        for(String process : processMap.values()) {
            if(count == 1) {
                System.out.println("* [" + count + "]: " + process);
            }else {
                System.out.println("  [" + count + "]: " + process);
            }
            count++;
        }

        // read choice
        String line = new Scanner(System.in).nextLine();
        if(line.trim().isEmpty()) {
            // get the first process id
            return processMap.keySet().iterator().next();
        }

        int choice = new Scanner(line).nextInt();

        if(choice <= 0 || choice > processMap.size()) {
            return -1;
        }

        Iterator<Integer> idIter = processMap.keySet().iterator();
        for(int i = 1; i <= choice; ++i) {
            if(i == choice) {
                return idIter.next();
            }
            idIter.next();
        }

        return -1;
    }

    private static Map<Integer, String> listProcessByOshi() {
        SystemInfo info = new SystemInfo();
        OperatingSystem operatingSystem = info.getOperatingSystem();
        Map<Integer, String> result = new LinkedHashMap<Integer, String>();
        OSProcess[] processes = operatingSystem.getProcesses(-1, null);
        for (OSProcess p : processes) {
            System.err.println(p);
            System.err.println(p.getPath());
            String path = p.getPath();
            String name = new File(path).getName();
            if (name.equals("java") || name.equals("java.exe")) {
                result.put(p.getProcessID(), p.getProcessID() + " " + path);
            }
        }
        return result;
    }

    private static Map<Integer, String> listProcessByJps(boolean v) {
        Map<Integer, String> result = new LinkedHashMap<Integer, String>();

        File jps = findJps();
        if(jps == null) {
            return result;
        }

        String[] command = null;
        if (v) {
            command = new String[] { jps.getAbsolutePath(), "-v" };
        } else {
            command = new String[] { jps.getAbsolutePath() };
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        try {
            Process proc = pb.start();
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            // read the output from the command
            String line = null;
            while ((line = stdInput.readLine()) != null) {
                int pid = new Scanner(line).nextInt();
                result.put(pid, line);
            }
        } catch (Throwable e) {
            // ignore
        }

        return result;

    }

    public static void startArthasCore(int targetPid, List<String> attachArgs) {
        // find java/java.exe, then try to find tools.jar
        SystemInfo info = new SystemInfo();
        OperatingSystem operatingSystem = info.getOperatingSystem();
        OSProcess processe = operatingSystem.getProcess(targetPid);
        if(processe == null) {
            throw new IllegalArgumentException("process do not exist! pid: " + targetPid);
        }

        String path = processe.getPath();

        // some app like eclipse process path is not java/java.exe
        if(!path.endsWith("java") && path.endsWith("java.exe")) {
            OSProcess myselfProcess = operatingSystem.getProcess(operatingSystem.getProcessId());
            path = myselfProcess.getPath();
            logger.warn("The target process is not an normal java process. try to start by using current java.");
        }

        File javaBinDir = new File(path).getParentFile();

        // current/jre/bin/java
        // current/bin/java
        // current/lib/tools.jar
        // after jdk9, there is no tools.jar
        File toolsJar = new File(javaBinDir , "../lib/tools.jar");
        if(!toolsJar.exists()) {
            // maybe jre
            toolsJar = new File(javaBinDir , "../../lib/tools.jar");
        }

        List<String> command = new ArrayList<String>();
        command.add(path);

        if(toolsJar.exists()) {
            command.add("-Xbootclasspath/a:" + toolsJar.getAbsolutePath());
        }

        command.addAll(attachArgs);
//        "${JAVA_HOME}"/bin/java \
//        ${opts}  \
//        -jar "${arthas_lib_dir}/arthas-core.jar" \
//            -pid ${TARGET_PID} \
//            -target-ip ${TARGET_IP} \
//            -telnet-port ${TELNET_PORT} \
//            -http-port ${HTTP_PORT} \
//            -core "${arthas_lib_dir}/arthas-core.jar" \
//            -agent "${arthas_lib_dir}/arthas-agent.jar"

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
            if(exitValue != 0) {
                logger.error("attach fail, targetPid: " + targetPid);
                System.exit(1);
            }
        } catch (Throwable e) {
            // ignore
        }

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

        return null;
    }

}
