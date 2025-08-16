package com.taobao.arthas.mcp.server.tool.function;

import com.taobao.arthas.mcp.server.protocol.server.McpNettyServerExchange;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流式工具的工具类
 * 提供通用的结果拉取和进度通知功能
 * 
 * @author Yeaury
 */
public final class StreamableToolUtils {

    private static final Logger logger = LoggerFactory.getLogger(StreamableToolUtils.class);

    private static final int DEFAULT_POLL_INTERVAL_MS = 200;    // 默认轮询间隔200ms
    private static final int ERROR_RETRY_INTERVAL_MS = 500;     // 错误重试间隔500ms

    private static final int MAX_POLL_ATTEMPTS = 100;           // 最大轮询尝试次数，避免无限循环
    private static final int MAX_ERROR_RETRIES = 10;            // 最大错误重试次数

    private StreamableToolUtils() {
    }

    /**
     * 同步拉取命令执行结果并发送进度通知
     * 
     * @param exchange MCP服务器交换对象
     * @param commandContext Arthas命令上下文
     * @param totalExecutions 总执行次数
     * @param intervalMs 命令执行间隔（毫秒）
     * @param progressToken 进度令牌
     * @return 是否成功完成
     */
    public static boolean pullResultsSync(McpNettyServerExchange exchange, ArthasCommandContext commandContext, 
                                        int totalExecutions, Integer intervalMs, Integer progressToken) {
        int currentExecution = 0;
        int pollAttempts = 0;
        int errorRetries = 0;
        int allowInputCount = 0;
        // 轮询间隔使用命令执行间隙的 1/10,事件驱动则在命令中自定义默认轮询间隔
        // 工具中默认轮询间隔为200ms
        int pullIntervalMs = (intervalMs != null && intervalMs > 0) ? intervalMs : DEFAULT_POLL_INTERVAL_MS;

        try {
            while (currentExecution < totalExecutions && pollAttempts < MAX_POLL_ATTEMPTS) {
                pollAttempts++;
                
                try {
                    Map<String, Object> results = commandContext.pullResults();
                    if (results == null) {
                        Thread.sleep(pullIntervalMs);
                        continue;
                    }

                    errorRetries = 0;

                    boolean commandCompleted = checkCommandCompletion(results, allowInputCount);
                    if (commandCompleted) {
                        allowInputCount++;
                    }

                    currentExecution++;
                    sendProgressNotification(exchange, results, currentExecution, totalExecutions, progressToken);

                    String jobStatus = (String) results.get("jobStatus");
                    // 需要检测到至少2次 ALLOW_INPUT 才认为真正结束
                    if ((commandCompleted && allowInputCount >= 2) || "TERMINATED".equals(jobStatus) || currentExecution >= totalExecutions) {
                        return true;
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                } catch (Exception e) {
                    if (++errorRetries >= MAX_ERROR_RETRIES) {
                        logger.error("Maximum error retries exceeded", e);
                        return false;
                    }
                    
                    try {
                        Thread.sleep(ERROR_RETRY_INTERVAL_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }

            return pollAttempts < MAX_POLL_ATTEMPTS;
            
        } catch (Exception e) {
            logger.error("Error in result pulling", e);
            return false;
        }
    }


    private static boolean checkCommandCompletion(Map<String, Object> results, int currentAllowInputCount) {
        if (results == null) {
            return false;
        }
        
        @SuppressWarnings("unchecked")
        List<Object> resultList = (List<Object>) results.get("results");
        if (resultList == null || resultList.isEmpty()) {
            return false;
        }

        for (Object result : resultList) {
            if (result.getClass().getSimpleName().equals("InputStatusModel")) {
                try {
                    java.lang.reflect.Method getInputStatusMethod = result.getClass().getMethod("getInputStatus");
                    Object inputStatusObj = getInputStatusMethod.invoke(result);
                    if (inputStatusObj != null) {
                        String inputStatusName = inputStatusObj.getClass().getSimpleName().equals("InputStatus") ? 
                            inputStatusObj.toString() : String.valueOf(inputStatusObj);
                        if ("ALLOW_INPUT".equals(inputStatusName)) {
                            logger.debug("Command completion detected via InputStatusModel: ALLOW_INPUT (count: {})", currentAllowInputCount + 1);
                            return true;
                        }
                    }
                } catch (Exception e) {
                    logger.debug("Failed to get inputStatus from InputStatusModel", e);
                }
            }
        }
        
        return false;
    }

    public static void sendProgressNotification(McpNettyServerExchange exchange, Map<String, Object> data, 
                                              int currentExecution, int totalExecutions, Integer progressToken) {
        try {
            Map<String, Object> enhancedData = new HashMap<>(data);
            enhancedData.put("stage", "progress");
            enhancedData.put("executionNumber", currentExecution);
            enhancedData.put("totalExecutions", totalExecutions);
            
            exchange.loggingNotification(new McpSchema.LoggingMessageNotification(
                    McpSchema.LoggingLevel.INFO,
                    "中间结果",
                    enhancedData
            )).join();

            if (progressToken != null) {
                exchange.progressNotification(new McpSchema.ProgressNotification(
                        progressToken,
                        currentExecution,
                        (double) totalExecutions
                )).join();
            }
            
        } catch (Exception e) {
            logger.error("Error sending progress notification", e);
            throw new RuntimeException("Failed to send progress notification", e);
        }
    }

    public static Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", true);
        response.put("message", message);
        return response;
    }

    public static Map<String, Object> createCompletedResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "completed");
        response.put("message", message);
        response.put("stage", "final");
        return response;
    }

}