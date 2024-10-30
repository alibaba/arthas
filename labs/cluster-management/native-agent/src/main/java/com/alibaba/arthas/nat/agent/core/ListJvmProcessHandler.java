package com.alibaba.arthas.nat.agent.core;

import com.taobao.arthas.common.ExecutingCommand;
import com.taobao.arthas.common.PidUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @description: list java process via invoke com.taobao.arthas.boot.ProcessUtils#listProcessByJps
 * @author：flzjkl
 * @date: 2024-07-18 8:25
 */
public class ListJvmProcessHandler {

    private static final Logger logger = LoggerFactory.getLogger(ListJvmProcessHandler.class);

    public static Map<Long, String> listProcessByJps() {
        Map<Long, String> result = new LinkedHashMap<Long, String>();

        String jps = "jps";

        String[] command = new String[] { jps, "-l" };

        List<String> lines = ExecutingCommand.runNative(command);


        long currentPid = Long.parseLong(PidUtils.currentPid());
        for (String line : lines) {
            String[] strings = line.trim().split("\\s+");
            if (strings.length < 1) {
                continue;
            }
            try {
                long pid = Long.parseLong(strings[0]);
                if (pid == currentPid) {
                    continue;
                }
                if (strings.length >= 2 && isJpsProcess(strings[1])) { // skip jps
                    continue;
                }

                result.put(pid, line);
            } catch (Throwable e) {

            }
        }

        return result;
    }

    private static boolean isJpsProcess(String mainClassName) {
        return "sun.tools.jps.Jps".equals(mainClassName) || "jdk.jcmd/sun.tools.jps.Jps".equals(mainClassName);
    }

}
