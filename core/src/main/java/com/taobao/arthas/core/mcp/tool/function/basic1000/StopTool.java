package com.taobao.arthas.core.mcp.tool.function.basic1000;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.util.JsonParser;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;

import java.util.HashMap;
import java.util.Map;

import static com.taobao.arthas.core.mcp.tool.function.StreamableToolUtils.createCompletedResponse;
import static com.taobao.arthas.core.mcp.tool.function.StreamableToolUtils.createErrorResponse;

/**
 * Stop 停止工具类
 *
 * <p>该类提供了彻底停止 Arthas 的功能。一旦调用 stop 工具，Arthas 将停止运行，
 * 之后不能再调用任何其他工具。</p>
 *
 * <p>为了确保 MCP 客户端能够正确接收到返回结果，该工具会先返回响应，
 * 然后在后台延迟执行实际的停止操作。这种设计避免了因为立即停止导致
 * 客户端无法收到响应结果的问题。</p>
 *
 * <p>该工具使用守护线程来延迟执行停止操作，确保即使主线程退出，
 * 停止操作仍能正常执行。</p>
 *
 * @author Arthas
 * @see com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool
 */
public class StopTool extends AbstractArthasTool {

    /**
     * 默认停止延迟时间（毫秒）
     *
     * <p>默认值为 1000 毫秒（1秒）。这个延迟时间用于确保 MCP 客户端
     * 有足够的时间接收和处理停止命令的返回结果。</p>
     *
     * <p>如果用户没有指定延迟时间，则使用此默认值。</p>
     */
    public static final int DEFAULT_SHUTDOWN_DELAY_MS = 1000;

    /**
     * 彻底停止 Arthas
     *
     * <p>该方法用于停止 Arthas 服务。为了确保 MCP 客户端能够收到返回结果，
     * 该方法会立即返回一个响应，然后在后台线程中延迟执行实际的停止操作。</p>
     *
     * <p>停止操作的执行流程：</p>
     * <ol>
     *   <li>获取或使用默认的延迟时间</li>
     *   <li>创建工具执行上下文</li>
     *   <li>在后台线程中调度停止任务</li>
     *   <li>立即返回响应给客户端</li>
     *   <li>在指定的延迟后执行实际的停止命令</li>
     * </ol>
     *
     * <p><strong>注意：</strong>一旦停止，MCP 连接会断开，之后不能再调用任何工具。</p>
     *
     * @param delayMs 延迟执行 stop 的毫秒数，默认为 1000 毫秒。
     *               该延迟时间用于确保 MCP 客户端有足够的时间接收返回结果。
     *               如果为 {@code null}，则使用 {@link #DEFAULT_SHUTDOWN_DELAY_MS}。
     *               该参数为可选参数。
     * @param toolContext 工具执行上下文，包含了执行该工具所需的所有上下文信息，
     *                    如会话信息、权限信息、命令上下文等。
     * @return 执行结果的 JSON 字符串格式。如果成功调度停止操作，返回包含
     *         停止信息的成功响应；如果发生错误，返回包含错误信息的错误响应。
     */
    @Tool(
        name = "stop",
        description = "彻底停止 Arthas。注意停止之后不能再调用任何 tool 了。为了确保 MCP client 能收到返回结果，本 tool 会先返回，再延迟执行 stop。"
    )
    public String stop(
            @ToolParam(description = "延迟执行 stop 的毫秒数，默认 1000ms。用于确保 MCP client 收到返回结果。", required = false)
            Integer delayMs,
            ToolContext toolContext) {
        try {
            // 获取延迟时间，如果用户未指定则使用默认值 1000 毫秒
            int shutdownDelayMs = getDefaultValue(delayMs, DEFAULT_SHUTDOWN_DELAY_MS);

            // 创建工具执行上下文，第二个参数 false 表示不需要流式响应
            ToolExecutionContext execContext = new ToolExecutionContext(toolContext, false);
            // 在后台线程中调度停止操作
            scheduleStop(execContext, shutdownDelayMs);

            // 构建返回结果，告知用户停止操作已被调度
            Map<String, Object> result = new HashMap<>();
            result.put("command", "stop");  // 命令名称
            result.put("scheduled", true);  // 已调度标志
            result.put("delayMs", shutdownDelayMs);  // 实际的延迟时间
            result.put("note", "Arthas 将在返回结果后停止，MCP 连接会断开。");  // 提示信息
            // 返回成功响应
            return JsonParser.toJson(createCompletedResponse("Stop scheduled", result));
        } catch (Exception e) {
            // 记录调度停止操作时发生的错误
            logger.error("Error scheduling stop", e);
            // 返回错误响应
            return JsonParser.toJson(createErrorResponse("Error scheduling stop: " + e.getMessage()));
        }
    }

    /**
     * 在后台线程中调度延迟停止操作
     *
     * <p>该方法创建一个守护线程来延迟执行停止操作。守护线程的设计
     * 确保了即使主线程退出，停止操作仍能在后台正常执行。</p>
     *
     * <p>执行流程：</p>
     * <ol>
     *   <li>从执行上下文中提取认证主题和用户 ID</li>
     *   <li>创建并启动一个守护线程</li>
     *   <li>守护线程先睡眠指定的延迟时间</li>
     *   <li>睡眠结束后，执行实际的 stop 命令</li>
     * </ol>
     *
     * @param execContext 工具执行上下文，包含执行停止操作所需的所有信息，
     *                    如认证信息、用户 ID、命令执行器等。
     * @param delayMs 延迟执行的毫秒数。如果大于 0，线程会先睡眠指定的时间；
     *               如果小于等于 0，则立即执行停止操作。
     */
    private void scheduleStop(ToolExecutionContext execContext, int delayMs) {
        // 从执行上下文中获取认证主题，用于权限验证
        Object authSubject = execContext.getAuthSubject();
        // 从执行上下文中获取用户 ID，用于标识执行停止操作的用户
        String userId = execContext.getUserId();

        // 创建守护线程来执行延迟停止操作
        Thread shutdownThread = new Thread(() -> {
            try {
                // 如果延迟时间大于 0，则先睡眠指定的毫秒数
                if (delayMs > 0) {
                    Thread.sleep(delayMs);
                }
            } catch (InterruptedException e) {
                // 如果线程被中断，恢复中断状态
                Thread.currentThread().interrupt();
                // 中断后退出线程，不执行停止操作
                return;
            }

            // 延迟时间结束后，执行实际的停止命令
            try {
                // 从命令上下文中获取命令执行器，执行 stop 命令
                // 超时时间设置为 300000 毫秒（5分钟）
                execContext.getCommandContext().getCommandExecutor()
                        .executeSync("stop", 300000L, null, authSubject, userId);
            } catch (Throwable t) {
                // 记录执行停止命令时发生的错误
                logger.error("Error executing stop command in background thread", t);
            }
        }, "arthas-mcp-stop");  // 线程名称为 "arthas-mcp-stop"
        // 设置为守护线程，这样当主线程退出时，该线程也会随之退出
        shutdownThread.setDaemon(true);
        // 启动守护线程
        shutdownThread.start();
    }
}
