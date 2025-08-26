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
     * 拉取命令执行结果并发送进度通知
     */
    public static boolean pullResultsSync(McpNettyServerExchange exchange, ArthasCommandContext commandContext, 
                                        Integer expectedResultCount, Integer intervalMs, String progressToken) {
        int actualResultCount = 0;
        int pollAttempts = 0;
        int errorRetries = 0;
        int allowInputCount = 0;
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

                    // filter and count command specific outcome models
                    Map<String, Object> filteredResults = filterCommandSpecificResults(results);
                    int currentBatchResultCount = getCommandSpecificResultCount(filteredResults);
                    
                    if (currentBatchResultCount > 0) {
                        actualResultCount += currentBatchResultCount;
                        sendProgressNotification(exchange, filteredResults, actualResultCount, 
                                                expectedResultCount != null ? expectedResultCount : actualResultCount, progressToken);
                    }

                    boolean commandCompleted = checkCommandCompletion(results, allowInputCount);
                    if (commandCompleted) {
                        allowInputCount++;
                    }

                    String jobStatus = (String) results.get("jobStatus");
                    
                    // 判断是否应该结束
                    // 如果是TERMINATED状态，或者命令已完成且允许输入次数大于等于2，或者实际结果数量达到预期结果数量
                    if ("TERMINATED".equals(jobStatus) || (commandCompleted && allowInputCount >= 2) || (actualResultCount >= expectedResultCount)) {
                        logger.info("Command completed. Total results: {}, Expected: {}", actualResultCount, expectedResultCount);
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
        
        // Define the types of secondary models that need to be excluded
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

    // progress is judged based on the number of results
    private static int getCommandSpecificResultCount(Map<String, Object> filteredResults) {
        if (filteredResults == null) {
            return 0;
        }
        
        @SuppressWarnings("unchecked")
        List<Object> resultList = (List<Object>) filteredResults.get("results");
        return resultList != null ? resultList.size() : 0;
    }

    public static void sendProgressNotification(McpNettyServerExchange exchange, Map<String, Object> data, 
                                              int currentResultCount, int totalExpected, String progressToken) {
        try {
            Map<String, Object> enhancedData = new HashMap<>(data);
            enhancedData.put("stage", "progress");
            enhancedData.put("currentResultCount", currentResultCount);
            enhancedData.put("totalExpected", totalExpected);

            // send logging notification with intermediate results
            exchange.loggingNotification(new McpSchema.LoggingMessageNotification(
                    McpSchema.LoggingLevel.INFO,
                    "intermediateResults",
                    enhancedData
            )).join();

            // send progress notifications
            if (progressToken != null) {
                exchange.progressNotification(new McpSchema.ProgressNotification(
                        progressToken,
                        currentResultCount,
                        (double) totalExpected
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
