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
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
class ArthasMcpTasksIT {

    private ArthasMcpTaskTestSupport.Environment env;
    private ArthasMcpTaskTestSupport.StreamableMcpHttpClient client;

    @BeforeAll
    void setUp() throws Exception {
        ArthasMcpTaskTestSupport.assumeSupportedPlatform();
        this.env = ArthasMcpTaskTestSupport.Environment.start("arthas-mcp-task-it-home");
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
    void should_advertise_task_capabilities_and_optional_task_support_tools() throws Exception {
        ArthasMcpTaskTestSupport.InitializedSession session = this.client.initializeSession("arthas-mcp-task-capabilities-it");

        assertThat(session.initializeResult.getCapabilities()).isNotNull();
        assertThat(session.initializeResult.getCapabilities().getTasks()).isNotNull();
        assertThat(session.initializeResult.getCapabilities().getTasks().getList()).isNotNull();
        assertThat(session.initializeResult.getCapabilities().getTasks().getCancel()).isNotNull();
        assertThat(session.initializeResult.getCapabilities().getTasks().getRequests()).isNotNull();
        assertThat(session.initializeResult.getCapabilities().getTasks().getRequests().getTools()).isNotNull();
        assertThat(session.initializeResult.getCapabilities().getTasks().getRequests().getTools().getCall()).isNotNull();

        McpSchema.ListToolsResult toolsResult = this.client.listTools(session.sessionId);
        ArthasMcpTaskTestSupport.assertTaskSupportMode(toolsResult, "watch", McpSchema.TaskSupportMode.OPTIONAL);
        ArthasMcpTaskTestSupport.assertTaskSupportMode(toolsResult, "trace", McpSchema.TaskSupportMode.OPTIONAL);
        ArthasMcpTaskTestSupport.assertTaskSupportMode(toolsResult, "monitor", McpSchema.TaskSupportMode.OPTIONAL);
        ArthasMcpTaskTestSupport.assertTaskSupportMode(toolsResult, "stack", McpSchema.TaskSupportMode.OPTIONAL);
        ArthasMcpTaskTestSupport.assertTaskSupportMode(toolsResult, "tt", McpSchema.TaskSupportMode.OPTIONAL);
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void should_create_task_and_fetch_result_via_tasks_api() throws Exception {
        ArthasMcpTaskTestSupport.InitializedSession session = this.client.initializeSession("arthas-mcp-task-success-it");

        McpSchema.CreateTaskResult createTaskResult = this.client.createTask(
                session.sessionId,
                "watch",
                ArthasMcpTaskTestSupport.createWatchArguments(1, 10),
                60_000L
        );
        assertThat(createTaskResult).isNotNull();
        assertThat(createTaskResult.getTask()).isNotNull();
        assertThat(createTaskResult.getTask().getTaskId()).isNotBlank();
        assertThat(createTaskResult.getTask().getStatus()).isEqualTo(McpSchema.TaskStatus.WORKING);
        assertThat(createTaskResult.getTask().getTtl()).isEqualTo(60_000L);

        String taskId = createTaskResult.getTask().getTaskId();

        McpSchema.ListTasksResult listTasksResult = this.client.listTasks(session.sessionId);
        assertThat(listTasksResult.getTasks()).isNotNull();
        assertThat(listTasksResult.getTasks())
                .extracting(McpSchema.Task::getTaskId)
                .contains(taskId);

        McpSchema.GetTaskResult beforeResult = this.client.getTask(session.sessionId, taskId);
        assertThat(beforeResult).isNotNull();
        assertThat(beforeResult.getTaskId()).isEqualTo(taskId);
        assertThat(beforeResult.getStatus()).isIn(McpSchema.TaskStatus.WORKING, McpSchema.TaskStatus.COMPLETED);

        McpSchema.CallToolResult taskResult = this.client.getTaskResult(session.sessionId, taskId, Duration.ofSeconds(60));
        String body = ArthasMcpTaskTestSupport.assertCallToolSuccess("watch(task)", taskResult);
        ArthasMcpTaskTestSupport.assertStreamableResultCountGreaterThanZero("watch(task)", body);

        McpSchema.GetTaskResult afterResult = ArthasMcpTaskTestSupport.waitForTaskStatus(
                this.client, session.sessionId, taskId, Duration.ofSeconds(15));
        assertThat(afterResult.getStatus()).isEqualTo(McpSchema.TaskStatus.COMPLETED);
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void should_cancel_task_via_tasks_api() throws Exception {
        ArthasMcpTaskTestSupport.InitializedSession session = this.client.initializeSession("arthas-mcp-task-cancel-it");

        McpSchema.CreateTaskResult createTaskResult = this.client.createTask(
                session.sessionId,
                "watch",
                ArthasMcpTaskTestSupport.createWatchArguments(200, 30),
                60_000L
        );
        assertThat(createTaskResult).isNotNull();
        assertThat(createTaskResult.getTask()).isNotNull();

        String taskId = createTaskResult.getTask().getTaskId();

        McpSchema.CancelTaskResult cancelTaskResult = this.client.cancelTask(session.sessionId, taskId);
        assertThat(cancelTaskResult).isNotNull();
        assertThat(cancelTaskResult.getTaskId()).isEqualTo(taskId);
        assertThat(cancelTaskResult.getStatus()).isEqualTo(McpSchema.TaskStatus.CANCELLED);
        assertThat(cancelTaskResult.getStatusMessage()).containsIgnoringCase("cancel");

        McpSchema.GetTaskResult cancelledTask = ArthasMcpTaskTestSupport.waitForTaskStatus(
                this.client, session.sessionId, taskId, Duration.ofSeconds(10));
        assertThat(cancelledTask.getStatus()).isEqualTo(McpSchema.TaskStatus.CANCELLED);

        McpSchema.CallToolResult taskResult = this.client.getTaskResult(session.sessionId, taskId, Duration.ofSeconds(15));
        assertThat(taskResult).isNotNull();
        assertThat(taskResult.getIsError()).isEqualTo(Boolean.TRUE);
        assertThat(ArthasMcpTaskTestSupport.extractTextContent(taskResult)).containsIgnoringCase("cancel");
    }
}
