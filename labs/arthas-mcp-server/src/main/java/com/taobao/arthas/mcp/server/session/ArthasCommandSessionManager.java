package com.taobao.arthas.mcp.server.session;

import com.taobao.arthas.mcp.server.CommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for MCP-to-Command session bindings.
 * Handles the lifecycle of command sessions associated with MCP sessions.
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

    /**
     * 获取命令执行session，支持认证信息
     * 
     * @param mcpSessionId MCP session ID
     * @param authSubject 认证主体对象，可以为null
     * @return CommandSessionBinding
     */
    public CommandSessionBinding getCommandSession(String mcpSessionId, Object authSubject) {
        CommandSessionBinding binding = sessionBindings.get(mcpSessionId);

        if (binding == null) {
            binding = createCommandSession(mcpSessionId);
            sessionBindings.put(mcpSessionId, binding);
            logger.debug("Created new command session: MCP={}, Arthas={}", mcpSessionId, binding.getArthasSessionId());
        } else if (!isSessionValid(binding)) {
            logger.info("Session expired, recreating: MCP={}, Arthas={}", mcpSessionId, binding.getArthasSessionId());

            try {
                commandExecutor.closeSession(binding.getArthasSessionId());
            } catch (Exception e) {
                logger.debug("Failed to close expired session (may already be cleaned up): {}", e.getMessage());
            }

            CommandSessionBinding newBinding = createCommandSession(mcpSessionId);
            sessionBindings.put(mcpSessionId, newBinding);
            logger.info("Recreated command session: MCP={}, Old Arthas={}, New Arthas={}", 
                       mcpSessionId, binding.getArthasSessionId(), newBinding.getArthasSessionId());
            binding = newBinding;
        } else {
            logger.debug("Using existing valid session: MCP={}, Arthas={}", mcpSessionId, binding.getArthasSessionId());
        }

        if (authSubject != null) {
            try {
                commandExecutor.setSessionAuth(binding.getArthasSessionId(), authSubject);
                logger.debug("Applied auth to Arthas session: MCP={}, Arthas={}", 
                           mcpSessionId, binding.getArthasSessionId());
            } catch (Exception e) {
                logger.warn("Failed to apply auth to session: MCP={}, Arthas={}, error={}", 
                          mcpSessionId, binding.getArthasSessionId(), e.getMessage());
            }
        }
        
        return binding;
    }
    
    /**
     * 检查session是否有效
     * 通过尝试获取结果来验证session和consumer是否仍然存在
     */
    private boolean isSessionValid(CommandSessionBinding binding) {
        try {
            Map<String, Object> result = commandExecutor.pullResults(binding.getArthasSessionId(), binding.getConsumerId());

            if (result == null) {
                return true;
            }

            Boolean success = (Boolean) result.get("success");
            if (Boolean.TRUE.equals(success)) {
                return true;
            }

            String errorMessage = (String) result.get("error");
            if (errorMessage != null && (errorMessage.contains("Session not found") || 
                                        errorMessage.contains("Consumer not found") ||
                                        errorMessage.contains("session is inactive"))) {
                logger.debug("Session validation failed: {}", errorMessage);
                return false;
            }

            return true;
            
        } catch (Exception e) {
            logger.debug("Session validation error: {}", e.getMessage());
            return false;
        }
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
