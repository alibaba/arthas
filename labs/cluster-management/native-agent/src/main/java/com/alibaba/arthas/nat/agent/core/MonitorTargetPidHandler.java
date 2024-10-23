package com.alibaba.arthas.nat.agent.core;

import com.alibaba.arthas.nat.agent.common.constants.NativeAgentConstants;
import com.taobao.arthas.common.SocketUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @description: monitor target pid
 * @author：flzjkl
 * @date: 2024-09-22 7:12
 */
public class MonitorTargetPidHandler {

    private static final Logger logger = LoggerFactory.getLogger(MonitorTargetPidHandler.class);

    public static boolean monitorTargetPid (Integer pid)  {
        long tcpListenProcess = SocketUtils.findTcpListenProcess(NativeAgentConstants.ARTHAS_SERVER_HTTP_PORT);

        if (tcpListenProcess == -1) {
            try {
                JvmAttachmentHandler.attachJvmByPid(pid);
                return true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (tcpListenProcess == pid) {
            return true;
        }

        if (tcpListenProcess != pid) {
            String errorMsg = "Target port: " + NativeAgentConstants.ARTHAS_SERVER_HTTP_PORT
                    + " has been occupied by pid: " + tcpListenProcess;
            logger.error(errorMsg);
            return false;
        }

        return false;
    }

}
