package com.taobao.arthas.mcp.server.tool.function;

import com.taobao.arthas.mcp.server.protocol.server.McpNettyServerExchange;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 同步工具的工具类
 * 提供同步执行命令并收集所有结果的功能
 * 
 * @author Yeaury
 */
public final class StreamableToolUtils {

    private static final Logger logger = LoggerFactory.getLogger(StreamableToolUtils.class);

    private static final int DEFAULT_POLL_INTERVAL_MS = 100;    // 默认轮询间隔100ms
    private static final int ERROR_RETRY_INTERVAL_MS = 500;     // 错误重试间隔500ms

    private static final int MAX_POLL_ATTEMPTS = 20;           // 最大轮询尝试次数，避免无限循环
    private static final int MAX_ERROR_RETRIES = 10;            // 最大错误重试次数

    public static final int MIN_ALLOW_INPUT_COUNT_TO_COMPLETE = 2;

    private StreamableToolUtils() {
    }

    /**
     * 同步执行命令并收集所有结果，支持进度通知
     * 
     * @param exchange MCP交换器，用于发送进度通知
     * @param commandContext 命令上下文
     * @param expectedResultCount 预期结果数量
     * @param intervalMs 轮询间隔
     * @param progressToken 进度令牌
     * @return 包含所有结果的Map，如果执行失败返回null
     */
    public static Map<String, Object> executeAndCollectResults(McpNettyServerExchange exchange, 
                                                             ArthasCommandContext commandContext, 
                                                             Integer expectedResultCount, Integer intervalMs, 
                                                             String progressToken) {
        List<Object> allResults = new ArrayList<>();
        int pollAttempts = 0;
        int errorRetries = 0;
        int allowInputCount = 0;
        int totalResultCount = 0;
        
        // 轮询间隔使用命令执行间隙的 1/10,事件驱动则在命令中自定义默认轮询间隔
        // 工具中默认轮询间隔为200ms
        int pullIntervalMs = (intervalMs != null && intervalMs > 0) ? intervalMs : DEFAULT_POLL_INTERVAL_MS;

        try {
            while (pollAttempts < MAX_POLL_ATTEMPTS) {
                pollAttempts++;
                
                try {
                    Map<String, Object> results = commandContext.pullResults();
                    if (results == null) {
                        Thread.sleep(pullIntervalMs);
                        continue;
                    }
                    errorRetries = 0;

                    // 检查是否有错误消息
                    String errorMessage = checkForErrorMessages(results);
                    if (errorMessage != null) {
                        logger.warn("Command execution failed with error: {}", errorMessage);
                        return createErrorResponseWithResults(errorMessage, allResults, totalResultCount);
                    }

                    Map<String, Object> filteredResults = filterCommandSpecificResults(results);
                    List<Object> currentBatchResults = getCommandSpecificResults(filteredResults);
                    
                    if (currentBatchResults != null && !currentBatchResults.isEmpty()) {
                        allResults.addAll(currentBatchResults);
                        totalResultCount += currentBatchResults.size();
                        logger.debug("Collected {} results, total: {}", currentBatchResults.size(), totalResultCount);

                        if (exchange != null) {
                            sendProgressNotification(exchange, totalResultCount, 
                                                    expectedResultCount != null ? expectedResultCount : totalResultCount, 
                                                    progressToken);
                        }
                    }

                    boolean commandCompleted = checkCommandCompletion(results, allowInputCount);
                    if (commandCompleted) {
                        allowInputCount++;
                    }

                    String jobStatus = (String) results.get("jobStatus");
                    
                    // 判断是否应该结束
                    // 如果是TERMINATED状态，或者命令已完成且允许输入次数大于等于2，或者实际结果数量达到预期结果数量
                    if ("TERMINATED".equals(jobStatus)
                            || (commandCompleted && allowInputCount >= MIN_ALLOW_INPUT_COUNT_TO_COMPLETE)
                            || (expectedResultCount != null && totalResultCount >= expectedResultCount)) {
                        logger.info("Command completed. Total results collected: {}, Expected: {}", totalResultCount, expectedResultCount);
                        break;
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Command execution interrupted");
                    return null;
                } catch (Exception e) {
                    if (++errorRetries >= MAX_ERROR_RETRIES) {
                        logger.error("Maximum error retries exceeded", e);
                        return null;
                    }
                    
                    try {
                        Thread.sleep(ERROR_RETRY_INTERVAL_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }

            return createFinalResult(allResults, totalResultCount, pollAttempts >= MAX_POLL_ATTEMPTS);
            
        } catch (Exception e) {
            logger.error("Error in command execution", e);
            return null;
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

    /**
     * 检查结果中是否包含错误消息
     */
    private static String checkForErrorMessages(Map<String, Object> results) {
        if (results == null) {
            return null;
        }
        
        @SuppressWarnings("unchecked")
        List<Object> resultList = (List<Object>) results.get("results");
        if (resultList == null || resultList.isEmpty()) {
            return null;
        }

        for (Object result : resultList) {
            String resultClassName = result.getClass().getSimpleName();
            
            // 检查包含消息的模型类中的错误信息
            if ("MessageModel".equals(resultClassName) || 
                "EnhancerModel".equals(resultClassName) || 
                "StatusModel".equals(resultClassName) || 
                "CommandRequestModel".equals(resultClassName)) {
                
                try {
                    java.lang.reflect.Method getMessageMethod = result.getClass().getMethod("getMessage");
                    Object messageObj = getMessageMethod.invoke(result);
                    if (messageObj != null) {
                        String message = String.valueOf(messageObj);
                        // 检查是否包含常见的错误关键词
                        if (message.contains("failed") || message.contains("error") || 
                            message.contains("exception") || message.contains("Exception") ||
                            message.contains("Malformed OGNL expression") || 
                            message.contains("ParseException") || 
                            message.contains("ExpressionSyntaxException")) {
                            return message;
                        }
                    }
                } catch (Exception e) {
                    logger.debug("Failed to get message from {}", resultClassName, e);
                }
            }
        }
        
        return null;
    }

    private static Map<String, Object> filterCommandSpecificResults(Map<String, Object> results) {
        if (results == null) {
            return new HashMap<>();
        }
        
        Map<String, Object> filteredResults = new HashMap<>(results);
        @SuppressWarnings("unchecked")
        List<Object> resultList = (List<Object>) results.get("results");
        
        if (resultList == null || resultList.isEmpty()) {
            return filteredResults;
        }
        
        // 定义需要排除的辅助模型类型
        String[] auxiliaryModelTypes = {
            "InputStatusModel", "StatusModel", "WelcomeModel", "MessageModel", 
            "CommandRequestModel", "SessionModel", "EnhancerModel"
        };
        
        List<Object> filteredResultList = resultList.stream()
            .filter(result -> {
                String resultClassName = result.getClass().getSimpleName();
 
                for (String auxiliaryType : auxiliaryModelTypes) {
                    if (resultClassName.equals(auxiliaryType)) {
                        return false;
                    }
                }
                
                return true;
            })
            .collect(java.util.stream.Collectors.toList());
        
        filteredResults.put("results", filteredResultList);
        filteredResults.put("resultCount", filteredResultList.size());
        
        return filteredResults;
    }

    private static List<Object> getCommandSpecificResults(Map<String, Object> filteredResults) {
        if (filteredResults == null) {
            return new ArrayList<>();
        }
        
        @SuppressWarnings("unchecked")
        List<Object> resultList = (List<Object>) filteredResults.get("results");
        return resultList != null ? resultList : new ArrayList<>();
    }

    /**
     * 发送进度通知
     */
    private static void sendProgressNotification(McpNettyServerExchange exchange, int currentResultCount, 
                                               int totalExpected, String progressToken) {
        try {
            if (progressToken != null) {
                exchange.progressNotification(new McpSchema.ProgressNotification(
                        progressToken,
                        currentResultCount,
                        (double) totalExpected
                )).join();
            }
            
        } catch (Exception e) {
            logger.error("Error sending progress notification", e);
        }
    }

    public static Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", true);
        response.put("message", message);
        response.put("status", "error");
        response.put("stage", "final");
        return response;
    }

    public static Map<String, Object> createErrorResponseWithResults(String message, List<Object> collectedResults, int resultCount) {
        Map<String, Object> response = createErrorResponse(message);
        response.put("results", collectedResults != null ? collectedResults : new ArrayList<>());
        response.put("resultCount", resultCount);
        return response;
    }

    private static Map<String, Object> createFinalResult(List<Object> allResults, int totalResultCount, boolean reachedMaxAttempts) {
        Map<String, Object> finalResult = new HashMap<>();
        finalResult.put("results", allResults);
        finalResult.put("resultCount", totalResultCount);
        finalResult.put("status", "completed");
        finalResult.put("stage", "final");
        
        if (reachedMaxAttempts) {
            logger.warn("Command execution reached maximum poll attempts: {}", MAX_POLL_ATTEMPTS);
            finalResult.put("warning", "Execution reached maximum poll attempts");
        }
        
        return finalResult;
    }

    public static Map<String, Object> createCompletedResponse(String message, Map<String, Object> results) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "completed");
        response.put("message", message);
        response.put("stage", "final");
        
        if (results != null) {
            response.putAll(results);
        }
        
        return response;
    }
}
