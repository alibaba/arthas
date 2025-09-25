package com.alibaba.arthas.nat.agent.core;

import com.alibaba.arthas.nat.agent.common.constants.NativeAgentConstants;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static com.alibaba.arthas.nat.agent.core.ArthasHomeHandler.ARTHAS_HOME_DIR;

/**
 * @description: attach jvm via java agent
 * @author：flzjkl
 * @date: 2024-07-01 11:37
 */
public class JvmAttachmentHandler {

    private static final Logger logger = LoggerFactory.getLogger(JvmAttachmentHandler.class);
    private static final String ARTHAS_AGENT_JAR = "arthas-agent.jar";

    public static String attachJvmByPid (Integer pid) throws Exception {
        VirtualMachine vm = null;
        try {
            vm = VirtualMachine.attach(pid + "");
        } catch (AttachNotSupportedException e) {
            logger.error("attach pid failed");
            throw new RuntimeException("attach pid: " +  pid +" failed " + e.getMessage());
        }

        if (ARTHAS_HOME_DIR == null) {
            ArthasHomeHandler.findArthasHome();
        }

        if (ARTHAS_HOME_DIR == null) {
            throw new RuntimeException("arthas home was not found");
        }

        String agentPath = ARTHAS_HOME_DIR + File.separator + ARTHAS_AGENT_JAR;

        try {
            String args = ";httpPort=" + NativeAgentConstants.ARTHAS_SERVER_HTTP_PORT
                    + ";javaPid=" + pid + ";ip=localhost";
            vm.loadAgent(agentPath, args);
            logger.info("attach pid " + pid + " success, http server port is: " + NativeAgentConstants.ARTHAS_SERVER_HTTP_PORT);
        } catch (Exception e) {
            logger.error("attach pid " + pid + " success, http server port is: " + NativeAgentConstants.ARTHAS_SERVER_HTTP_PORT);
            throw new Exception("load agent failed, pid: " + pid + " " + e.getMessage());
        } finally {
            vm.detach();
        }
        return String.valueOf(NativeAgentConstants.ARTHAS_SERVER_HTTP_PORT);
    }

}
