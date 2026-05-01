package com.taobao.arthas.mcp.it.task;

import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
class ArthasMcpTasksConcurrencyIT {

    private ArthasMcpTaskTestSupport.Environment env;
    private ArthasMcpTaskTestSupport.StreamableMcpHttpClient client;

    @BeforeAll
    void setUp() throws Exception {
        ArthasMcpTaskTestSupport.assumeSupportedPlatform();
        this.env = ArthasMcpTaskTestSupport.Environment.start("arthas-mcp-task-concurrency-it-home");
        this.client = new ArthasMcpTaskTestSupport.StreamableMcpHttpClient("127.0.0.1", this.env.httpPort, "/mcp");
    }

    @AfterAll
    void tearDown() {
        if (this.env != null) {
            this.env.close();
            this.env = null;
        }
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void should_execute_multiple_tasks_concurrently() throws Exception {
        ArthasMcpTaskTestSupport.InitializedSession session = this.client.initializeSession("arthas-mcp-task-concurrent-it");

        Map<String, String> taskIdsByLabel = new HashMap<String, String>();
        taskIdsByLabel.put("watch-hotMethodA", ArthasMcpTaskTestSupport.createTaskAndGetId(
                this.client,
                session.sessionId,
                "watch",
                ArthasMcpTaskTestSupport.createWatchArguments(
                        ArthasMcpTaskTestSupport.TARGET_METHOD_A_PATTERN, 3, 15),
                60_000L
        ));
        taskIdsByLabel.put("trace-hotMethodB", ArthasMcpTaskTestSupport.createTaskAndGetId(
                this.client,
                session.sessionId,
                "trace",
                ArthasMcpTaskTestSupport.createTraceArguments(
                        ArthasMcpTaskTestSupport.TARGET_METHOD_B_PATTERN, 1, 15),
                60_000L
        ));
        taskIdsByLabel.put("stack-hotMethodC", ArthasMcpTaskTestSupport.createTaskAndGetId(
                this.client,
                session.sessionId,
                "stack",
                ArthasMcpTaskTestSupport.createStackArguments(
                        ArthasMcpTaskTestSupport.TARGET_METHOD_C_PATTERN, 1, 15),
                60_000L
        ));

        Set<String> uniqueTaskIds = new HashSet<String>(taskIdsByLabel.values());
        assertThat(uniqueTaskIds).hasSize(3);

        McpSchema.ListTasksResult listTasksResult = this.client.listTasks(session.sessionId);
        assertThat(listTasksResult.getTasks()).isNotNull();
        assertThat(listTasksResult.getTasks())
                .extracting(McpSchema.Task::getTaskId)
                .containsAll(taskIdsByLabel.values());

        int workingCount = 0;
        for (String taskId : taskIdsByLabel.values()) {
            McpSchema.GetTaskResult task = this.client.getTask(session.sessionId, taskId);
            assertThat(task).isNotNull();
            assertThat(task.getTaskId()).isEqualTo(taskId);
            assertThat(task.getStatus()).isIn(McpSchema.TaskStatus.WORKING, McpSchema.TaskStatus.COMPLETED);
            if (task.getStatus() == McpSchema.TaskStatus.WORKING) {
                workingCount++;
            }
        }
        assertThat(workingCount).isGreaterThan(0);

        Map<String, CompletableFuture<McpSchema.CallToolResult>> resultFutures =
                new HashMap<String, CompletableFuture<McpSchema.CallToolResult>>();
        for (Map.Entry<String, String> entry : taskIdsByLabel.entrySet()) {
            resultFutures.put(entry.getKey(), CompletableFuture.supplyAsync(() -> {
                try {
                    return this.client.getTaskResult(session.sessionId, entry.getValue(), Duration.ofSeconds(60));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        CompletableFuture.allOf(resultFutures.values().toArray(new CompletableFuture[0])).join();

        for (Map.Entry<String, String> entry : taskIdsByLabel.entrySet()) {
            McpSchema.CallToolResult result = resultFutures.get(entry.getKey()).join();
            String body = ArthasMcpTaskTestSupport.assertCallToolSuccess(entry.getKey(), result);
            ArthasMcpTaskTestSupport.assertStreamableResultCountGreaterThanZero(entry.getKey(), body);

            McpSchema.GetTaskResult finalTask = ArthasMcpTaskTestSupport.waitForTaskStatus(
                    this.client, session.sessionId, entry.getValue(), Duration.ofSeconds(15));
            assertThat(finalTask.getStatus()).isEqualTo(McpSchema.TaskStatus.COMPLETED);
        }
    }
}
