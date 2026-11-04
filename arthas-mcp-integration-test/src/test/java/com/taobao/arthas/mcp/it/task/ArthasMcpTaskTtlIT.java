package com.taobao.arthas.mcp.it.task;

import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import org.assertj.core.api.Assertions;
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
class ArthasMcpTaskTtlIT {

    private ArthasMcpTaskTestSupport.Environment env;
    private ArthasMcpTaskTestSupport.StreamableMcpHttpClient client;

    @BeforeAll
    void setUp() throws Exception {
        ArthasMcpTaskTestSupport.assumeSupportedPlatform();
        this.env = ArthasMcpTaskTestSupport.Environment.start("arthas-mcp-task-ttl-it-home");
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
    void should_return_not_found_after_task_expires_by_ttl() throws Exception {
        ArthasMcpTaskTestSupport.InitializedSession session = this.client.initializeSession("arthas-mcp-task-ttl-expire-it");

        McpSchema.CreateTaskResult createTaskResult = this.client.createTask(
                session.sessionId,
                "watch",
                ArthasMcpTaskTestSupport.createWatchArguments(1, 10),
                1_000L
        );
        assertThat(createTaskResult).isNotNull();
        assertThat(createTaskResult.getTask()).isNotNull();
        assertThat(createTaskResult.getTask().getTtl()).isEqualTo(1_000L);

        String taskId = createTaskResult.getTask().getTaskId();

        McpSchema.CallToolResult taskResult = this.client.getTaskResult(session.sessionId, taskId, Duration.ofSeconds(60));
        String body = ArthasMcpTaskTestSupport.assertCallToolSuccess("watch(task ttl)", taskResult);
        ArthasMcpTaskTestSupport.assertStreamableResultCountGreaterThanZero("watch(task ttl)", body);

        ArthasMcpTaskTestSupport.waitForTaskExpiration(this.client, session.sessionId, taskId, Duration.ofSeconds(80));

        Assertions.assertThatThrownBy(() -> this.client.getTask(session.sessionId, taskId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Task not found");

        Assertions.assertThatThrownBy(() -> this.client.getTaskResult(session.sessionId, taskId, Duration.ofSeconds(10)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Task not found");
    }
}
