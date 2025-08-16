package com.taobao.arthas.mcp.server.session;

import com.taobao.arthas.mcp.server.CommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Arthas命令执行Session管理器
 * 
 * @author Yeaury
 */
public class ArthasCommandSessionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ArthasCommandSessionManager.class);
    
    private final CommandExecutor commandExecutor;
    private final ConcurrentHashMap<String, CommandSessionBinding> sessionBindings = new ConcurrentHashMap<>();
    
    public ArthasCommandSessionManager(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    public static class CommandSessionBinding {
        private final String mcpSessionId;
        private final String arthasSessionId;
        private final String consumerId;
        
        public CommandSessionBinding(String mcpSessionId, String arthasSessionId, String consumerId) {
            this.mcpSessionId = mcpSessionId;
            this.arthasSessionId = arthasSessionId;
            this.consumerId = consumerId;
        }
        
        public String getMcpSessionId() {
            return mcpSessionId;
        }
        
        public String getArthasSessionId() {
            return arthasSessionId;
        }
        
        public String getConsumerId() {
            return consumerId;
        }
    }

    public CommandSessionBinding createCommandSession(String mcpSessionId) {
        Map<String, Object> result = commandExecutor.createSession();
        
        CommandSessionBinding binding = new CommandSessionBinding(
            mcpSessionId,
            (String) result.get("sessionId"),
            (String) result.get("consumerId")
        );

        return binding;
    }

    public CommandSessionBinding getCommandSession(String mcpSessionId) {
        return sessionBindings.computeIfAbsent(mcpSessionId, this::createCommandSession);
    }

    public void closeCommandSession(String mcpSessionId) {
        CommandSessionBinding binding = sessionBindings.remove(mcpSessionId);
        if (binding != null) {
            commandExecutor.closeSession(binding.getArthasSessionId());
            logger.debug("Closed command session: MCP={}, Arthas={}", mcpSessionId, binding.getArthasSessionId());
        }
    }

    public void closeAllSessions() {
        sessionBindings.keySet().forEach(this::closeCommandSession);
    }
} 