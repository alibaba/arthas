package com.taobao.arthas.core.mcp.tool.function;

import com.taobao.arthas.core.command.model.*;
import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.protocol.server.McpNettyServerExchange;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 可流式工具的工具类
 * 提供同步执行命令并收集所有结果的功能，支持进度通知
 *
 * 该工具类主要用于处理需要轮询执行的Arthas命令，
 * 能够持续收集命令执行结果直到命令完成、超时或达到预期结果数量。
 *
 * @author Yeaury
 */
public final class StreamableToolUtils {

    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(StreamableToolUtils.class);

    /**
     * 默认轮询间隔（毫秒）
     * 用于从命令上下文中拉取结果的默认时间间隔
     */
    private static final int DEFAULT_POLL_INTERVAL_MS = 100;

    /**
     * 错误重试间隔（毫秒）
     * 当拉取结果发生错误时，等待该时间后重试
     */
    private static final int ERROR_RETRY_INTERVAL_MS = 500;

    /**
     * 默认超时时间（毫秒）
     * 命令执行的最大等待时间，超过此时间将停止执行
     */
    public static final long DEFAULT_TIMEOUT_MS = 30000L;

    /**
     * 最大错误重试次数
     * 当连续发生错误的次数达到此值时，停止执行并返回null
     */
    private static final int MAX_ERROR_RETRIES = 10;

    /**
     * 最小允许输入次数
     * 用于判断命令是否完成的条件之一，当命令允许输入的次数达到此值时认为命令已完成
     */
    public static final int MIN_ALLOW_INPUT_COUNT_TO_COMPLETE = 2;

    /**
     * 私有构造函数，防止实例化
     * 这是一个工具类，所有方法都是静态的
     */
    private StreamableToolUtils() {
    }

    /**
     * 同步执行命令并收集所有结果，支持进度通知
     *
     * 该方法会持续轮询命令执行结果，直到满足以下条件之一：
     * 1. 命令状态变为TERMINATED
     * 2. 命令完成且允许输入次数达到最小值（未指定预期结果数量时）
     * 3. 实际结果数量达到预期结果数量
     * 4. 执行超时
     *
     * @param exchange MCP交换器，用于发送进度通知，可以为null
     * @param commandContext 命令上下文，用于拉取执行结果
     * @param expectedResultCount 预期结果数量，可以为null
     *                            如果为null，则根据命令完成状态判断
     * @param intervalMs 轮询间隔（毫秒），可以为null
     *                  如果为null或小于等于0，使用默认值100ms
     * @param timeoutMs 超时时间（毫秒），可以为null
     *                 如果为null或小于等于0，使用默认值30000ms
     * @param progressToken 进度令牌，用于发送进度通知，可以为null
     * @return 包含所有结果的Map，如果执行失败返回null
     *         返回的Map包含以下键：
     *         - results: 结果列表
     *         - resultCount: 结果数量
     *         - status: 状态（"completed"）
     *         - stage: 阶段（"final"）
     *         - timedOut: 是否超时
     *         - warning: 超时警告信息（如果超时）
     */
    public static Map<String, Object> executeAndCollectResults(McpNettyServerExchange exchange,
                                                             ArthasCommandContext commandContext,
                                                             Integer expectedResultCount, Integer intervalMs,
                                                             Integer timeoutMs,
                                                             String progressToken) {
        // 存储所有收集到的结果
        List<Object> allResults = new ArrayList<>();
        // 错误重试计数器
        int errorRetries = 0;
        // 允许输入次数计数器
        int allowInputCount = 0;
        // 总结果数量计数器
        int totalResultCount = 0;

        // 轮询间隔使用命令执行间隙的 1/10,事件驱动则在命令中自定义默认轮询间隔
        // 工具中默认轮询间隔为200ms
        // 如果指定的间隔有效，使用指定的间隔；否则使用默认值
        int pullIntervalMs = (intervalMs != null && intervalMs > 0) ? intervalMs : DEFAULT_POLL_INTERVAL_MS;

        // 计算截止时间
        // 如果没有指定超时时间，则使用默认超时时间
        long executionTimeoutMs = (timeoutMs != null && timeoutMs > 0) ? timeoutMs : DEFAULT_TIMEOUT_MS;
        long deadline = System.currentTimeMillis() + executionTimeoutMs;
        // 超时标志
        boolean timedOut = false;

        try {
            // 在超时时间内持续轮询结果
            while (System.currentTimeMillis() < deadline) {
                try {
                    // 从命令上下文中拉取结果
                    Map<String, Object> results = commandContext.pullResults();
                    // 如果结果为null，说明还没有新结果，等待一段时间后继续
                    if (results == null) {
                        Thread.sleep(pullIntervalMs);
                        continue;
                    }
                    // 成功获取结果，重置错误重试计数器
                    errorRetries = 0;

                    // 检查是否有错误消息
                    String errorMessage = checkForErrorMessages(results);
                    if (errorMessage != null) {
                        logger.warn("Command execution failed with error: {}", errorMessage);
                        // 如果有错误，创建错误响应并返回
                        return createErrorResponseWithResults(errorMessage, allResults, totalResultCount);
                    }

                    // 过滤掉辅助类型的结果，只保留命令特定的结果
                    Map<String, Object> filteredResults = filterCommandSpecificResults(results);
                    // 获取命令特定的结果列表
                    List<Object> currentBatchResults = getCommandSpecificResults(filteredResults);

                    // 如果当前批次有结果，添加到总结果列表中
                    if (currentBatchResults != null && !currentBatchResults.isEmpty()) {
                        allResults.addAll(currentBatchResults);
                        totalResultCount += currentBatchResults.size();
                        logger.debug("Collected {} results, total: {}", currentBatchResults.size(), totalResultCount);

                        // 如果提供了exchange和progressToken，发送进度通知
                        if (exchange != null) {
                            sendProgressNotification(exchange, totalResultCount,
                                                    expectedResultCount != null ? expectedResultCount : totalResultCount,
                                                    progressToken);
                        }
                    }

                    // 检查命令是否完成（通过检查是否有ALLOW_INPUT状态）
                    boolean commandCompleted = checkCommandCompletion(results, allowInputCount);
                    if (commandCompleted) {
                        allowInputCount++;
                    }

                    // 获取作业状态
                    String jobStatus = (String) results.get("jobStatus");

                    // 判断是否应该结束
                    // 如果是TERMINATED状态，或者命令已完成且允许输入次数大于等于2，或者实际结果数量达到预期结果数量
                    boolean hasExpectedResultCount = (expectedResultCount != null);
                    boolean reachedExpectedResultCount = hasExpectedResultCount && totalResultCount >= expectedResultCount;
                    boolean allowInputCompletion = !hasExpectedResultCount
                            && commandCompleted
                            && allowInputCount >= MIN_ALLOW_INPUT_COUNT_TO_COMPLETE;

                    // 满足任一结束条件则退出循环
                    if ("TERMINATED".equals(jobStatus) || allowInputCompletion || reachedExpectedResultCount) {
                        logger.info("Command completed. Total results collected: {}, Expected: {}", totalResultCount, expectedResultCount);
                        break;
                    }

                } catch (InterruptedException e) {
                    // 如果线程被中断，恢复中断状态并返回null
                    Thread.currentThread().interrupt();
                    logger.warn("Command execution interrupted");
                    return null;
                } catch (Exception e) {
                    // 发生其他异常，增加错误重试计数器
                    if (++errorRetries >= MAX_ERROR_RETRIES) {
                        // 超过最大重试次数，记录错误并返回null
                        logger.error("Maximum error retries exceeded", e);
                        return null;
                    }

                    // 等待一段时间后重试
                    try {
                        Thread.sleep(ERROR_RETRY_INTERVAL_MS);
                    } catch (InterruptedException ie) {
                        // 如果等待时被中断，恢复中断状态并返回null
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }

            // 检查是否超时
            if (System.currentTimeMillis() >= deadline) {
                timedOut = true;
            }

            // 创建并返回最终结果
            return createFinalResult(allResults, totalResultCount, timedOut, executionTimeoutMs);

        } catch (Exception e) {
            // 发生未预期的异常，记录错误并返回null
            logger.error("Error in command execution", e);
            return null;
        }
    }

    /**
     * 检查命令是否完成
     * 通过检查结果列表中是否存在ALLOW_INPUT状态的InputStatusModel来判断
     *
     * @param results 结果Map
     * @param currentAllowInputCount 当前允许输入次数
     * @return 如果命令完成返回true，否则返回false
     */
    private static boolean checkCommandCompletion(Map<String, Object> results, int currentAllowInputCount) {
        // 如果结果为null，返回false
        if (results == null) {
            return false;
        }

        // 获取结果列表
        @SuppressWarnings("unchecked")
        List<Object> resultList = (List<Object>) results.get("results");
        if (resultList == null || resultList.isEmpty()) {
            return false;
        }

        // 遍历结果列表，查找InputStatusModel
        for (Object result : resultList) {
            // 直接类型检查，不使用反射
            if (result instanceof InputStatusModel) {
                InputStatusModel inputStatusModel = (InputStatusModel) result;
                InputStatus inputStatus = inputStatusModel.getInputStatus();
                // 如果输入状态为ALLOW_INPUT，表示命令已完成（等待用户输入）
                if (inputStatus == InputStatus.ALLOW_INPUT) {
                    logger.debug("Command completion detected: ALLOW_INPUT (count: {})", currentAllowInputCount + 1);
                    return true;
                }
            }
        }

        // 没有找到ALLOW_INPUT状态，返回false
        return false;
    }

    /**
     * 检查结果中是否包含错误消息
     *
     * 该方法会遍历结果列表，检查各种可能包含错误消息的模型类型，
     * 包括MessageModel、EnhancerModel、StatusModel和CommandRequestModel。
     * 如果发现错误消息（包含"failed"、"error"、"exception"等关键词），
     * 则返回该错误消息。
     *
     * @param results 结果Map
     * @return 如果发现错误消息返回该消息，否则返回null
     */
    private static String checkForErrorMessages(Map<String, Object> results) {
        // 如果结果为null，返回null
        if (results == null) {
            return null;
        }

        // 获取结果列表
        @SuppressWarnings("unchecked")
        List<Object> resultList = (List<Object>) results.get("results");
        if (resultList == null || resultList.isEmpty()) {
            return null;
        }

        // 遍历结果列表，查找错误消息
        for (Object result : resultList) {
            String message = null;

            // 直接类型检查，提取各种模型的message字段
            if (result instanceof MessageModel) {
                message = ((MessageModel) result).getMessage();
            } else if (result instanceof EnhancerModel) {
                message = ((EnhancerModel) result).getMessage();
            } else if (result instanceof StatusModel) {
                message = ((StatusModel) result).getMessage();
            } else if (result instanceof CommandRequestModel) {
                message = ((CommandRequestModel) result).getMessage();
            }

            // 如果有消息且是错误消息，返回该消息
            if (message != null && isErrorMessage(message)) {
                return message;
            }
        }

        // 没有发现错误消息，返回null
        return null;
    }

    /**
     * 判断消息是否为错误消息
     *
     * 通过检查消息是否包含特定的错误关键词来判断：
     * - "failed"、"error"、"exception"（不区分大小写）
     * - "Malformed OGNL expression"
     * - "ParseException"
     * - "ExpressionSyntaxException"
     * - 包含"Exception"或"Error"字样
     *
     * @param message 要检查的消息
     * @return 如果是错误消息返回true，否则返回false
     */
    private static boolean isErrorMessage(String message) {
        return message.matches(".*\\b(failed|error|exception)\\b.*") ||
               message.contains("Malformed OGNL expression") ||
               message.contains("ParseException") ||
               message.contains("ExpressionSyntaxException") ||
               message.matches(".*Exception.*") ||
               message.matches(".*Error.*");
    }

    /**
     * 过滤命令特定的结果
     *
     * 该方法会从结果列表中过滤掉辅助类型的模型，只保留命令特定的结果。
     * 辅助类型包括：InputStatusModel、StatusModel、WelcomeModel、
     * MessageModel、CommandRequestModel、SessionModel、EnhancerModel
     *
     * @param results 原始结果Map
     * @return 过滤后的结果Map
     */
    private static Map<String, Object> filterCommandSpecificResults(Map<String, Object> results) {
        // 如果结果为null，返回空的Map
        if (results == null) {
            return new HashMap<>();
        }

        // 创建过滤后的结果Map
        Map<String, Object> filteredResults = new HashMap<>(results);
        // 获取结果列表
        @SuppressWarnings("unchecked")
        List<Object> resultList = (List<Object>) results.get("results");

        // 如果结果列表为空，直接返回
        if (resultList == null || resultList.isEmpty()) {
            return filteredResults;
        }

        // 使用直接类型检查过滤掉辅助模型类型
        List<Object> filteredResultList = resultList.stream()
            .filter(result -> !isAuxiliaryModel(result))
            .collect(Collectors.toList());

        // 更新过滤后的结果列表和数量
        filteredResults.put("results", filteredResultList);
        filteredResults.put("resultCount", filteredResultList.size());

        return filteredResults;
    }

    /**
     * 检查结果是否为应该被过滤掉的辅助模型类型
     *
     * 辅助模型类型包括：
     * - InputStatusModel：输入状态模型
     * - StatusModel：状态模型
     * - WelcomeModel：欢迎信息模型
     * - MessageModel：消息模型
     * - CommandRequestModel：命令请求模型
     * - SessionModel：会话模型
     * - EnhancerModel：增强器模型
     *
     * @param result 要检查的结果对象
     * @return 如果是辅助模型类型返回true，否则返回false
     */
    private static boolean isAuxiliaryModel(Object result) {
        return result instanceof InputStatusModel
            || result instanceof StatusModel
            || result instanceof WelcomeModel
            || result instanceof MessageModel
            || result instanceof CommandRequestModel
            || result instanceof SessionModel
            || result instanceof EnhancerModel;
    }

    /**
     * 从过滤后的结果中获取命令特定的结果列表
     *
     * @param filteredResults 过滤后的结果Map
     * @return 命令特定的结果列表，如果没有结果则返回空列表
     */
    private static List<Object> getCommandSpecificResults(Map<String, Object> filteredResults) {
        // 如果过滤后的结果为null，返回空列表
        if (filteredResults == null) {
            return new ArrayList<>();
        }

        // 获取结果列表
        @SuppressWarnings("unchecked")
        List<Object> resultList = (List<Object>) filteredResults.get("results");
        // 如果结果列表不为null则返回，否则返回空列表
        return resultList != null ? resultList : new ArrayList<>();
    }

    /**
     * 发送进度通知
     *
     * 通过MCP交换器向客户端发送进度通知，告知当前执行的进度。
     * 进度通知包含当前已收集的结果数量和预期总数量。
     *
     * @param exchange MCP交换器，用于发送通知
     * @param currentResultCount 当前已收集的结果数量
     * @param totalExpected 预期的总结果数量
     * @param progressToken 进度令牌，用于标识该进度通知
     */
    private static void sendProgressNotification(McpNettyServerExchange exchange, int currentResultCount,
                                               int totalExpected, String progressToken) {
        try {
            // 如果提供了进度令牌且不为空，发送进度通知
            if (progressToken != null && !progressToken.trim().isEmpty()) {
                // 使用exchange发送进度通知并等待完成
                exchange.progressNotification(new McpSchema.ProgressNotification(
                        progressToken,
                        currentResultCount,
                        (double) totalExpected
                )).join();
            }

        } catch (Exception e) {
            // 如果发送进度通知失败，记录错误
            logger.error("Error sending progress notification", e);
        }
    }

    /**
     * 创建错误响应
     *
     * @param message 错误消息
     * @return 包含错误信息的响应Map
     */
    public static Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", true);
        response.put("message", message);
        response.put("status", "error");
        response.put("stage", "final");
        return response;
    }

    /**
     * 创建包含已收集结果的错误响应
     *
     * 当命令执行失败时，可能已经收集了部分结果，
     * 该方法将这些结果包含在错误响应中返回给调用者。
     *
     * @param message 错误消息
     * @param collectedResults 已收集的结果列表
     * @param resultCount 已收集的结果数量
     * @return 包含错误信息和已收集结果的响应Map
     */
    public static Map<String, Object> createErrorResponseWithResults(String message, List<Object> collectedResults, int resultCount) {
        // 创建基本的错误响应
        Map<String, Object> response = createErrorResponse(message);
        // 添加已收集的结果
        response.put("results", collectedResults != null ? collectedResults : new ArrayList<>());
        response.put("resultCount", resultCount);
        return response;
    }

    /**
     * 创建最终结果
     *
     * @param allResults 所有收集到的结果列表
     * @param totalResultCount 总结果数量
     * @param timedOut 是否超时
     * @param timeoutMs 超时时间（毫秒）
     * @return 包含最终结果的Map
     */
    private static Map<String, Object> createFinalResult(List<Object> allResults, int totalResultCount, boolean timedOut, long timeoutMs) {
        Map<String, Object> finalResult = new HashMap<>();
        // 添加结果列表和数量
        finalResult.put("results", allResults);
        finalResult.put("resultCount", totalResultCount);
        // 标记为已完成和最终阶段
        finalResult.put("status", "completed");
        finalResult.put("stage", "final");
        // 添加超时标志
        finalResult.put("timedOut", timedOut);

        // 如果超时，添加警告信息
        if (timedOut) {
            logger.warn("Command execution timed out after {} ms", timeoutMs);
            finalResult.put("warning", "Command execution timed out after " + timeoutMs + " ms.");
        }

        return finalResult;
    }

    /**
     * 创建已完成响应
     *
     * @param message 完成消息
     * @param results 结果Map
     * @return 包含完成信息的响应Map
     */
    public static Map<String, Object> createCompletedResponse(String message, Map<String, Object> results) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "completed");
        response.put("message", message);
        response.put("stage", "final");

        // 如果提供了结果，合并到响应中
        if (results != null) {
            response.putAll(results);
        }

        return response;
    }
}
