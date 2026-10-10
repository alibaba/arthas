package com.taobao.arthas.core.command.startup;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.command.model.ResultModel;

public class StartupCommandResultWriterTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldWriteLifecycleResultAndStdoutAsJsonLines() throws Exception {
        File output = new File(temporaryFolder.getRoot(), "command-001.jsonl");
        StartupCommandResultWriter writer = new StartupCommandResultWriter(1, "echo hello", output);

        writer.appendLifecycle("commandStarted");
        writer.appendResult(new MessageModel("hello"));
        writer.appendStdout("plain text\n");
        writer.close();

        assertThat(writer.awaitClosed(5, TimeUnit.SECONDS)).isTrue();
        List<String> lines = Files.readAllLines(output.toPath(), StandardCharsets.UTF_8);
        assertThat(lines).hasSize(3);

        JSONObject lifecycle = JSON.parseObject(lines.get(0));
        JSONObject result = JSON.parseObject(lines.get(1));
        JSONObject stdout = JSON.parseObject(lines.get(2));
        assertThat(lifecycle.getString("event")).isEqualTo("commandStarted");
        assertThat(result.getString("event")).isEqualTo("result");
        assertThat(result.getString("resultType")).isEqualTo("message");
        assertThat(result.getJSONObject("result").getString("message")).isEqualTo("hello");
        assertThat(stdout.getString("event")).isEqualTo("stdout");
        assertThat(stdout.getString("stdout")).isEqualTo("plain text\n");
        assertThat(stdout.getIntValue("commandIndex")).isEqualTo(1);
    }

    @Test
    public void shouldRecordDroppedEventsWhenQueueOverflows() throws Exception {
        File output = new File(temporaryFolder.getRoot(), "command-002.jsonl");
        CountDownLatch serializationStarted = new CountDownLatch(1);
        CountDownLatch releaseSerialization = new CountDownLatch(1);
        StartupCommandResultWriter writer = new StartupCommandResultWriter(2, "watch demo.Test run", output, 1);

        writer.appendResult(new BlockingResultModel(serializationStarted, releaseSerialization));
        assertThat(serializationStarted.await(5, TimeUnit.SECONDS)).isTrue();
        writer.appendStdout("first");
        writer.appendStdout("second");
        writer.close();
        releaseSerialization.countDown();

        assertThat(writer.awaitClosed(5, TimeUnit.SECONDS)).isTrue();
        List<String> lines = Files.readAllLines(output.toPath(), StandardCharsets.UTF_8);
        assertThat(lines).extracting(line -> JSON.parseObject(line).getString("event"))
                        .contains("result", "resultsDropped");
        JSONObject dropped = lines.stream().map(JSON::parseObject)
                        .filter(line -> "resultsDropped".equals(line.getString("event")))
                        .findFirst().get();
        assertThat(dropped.getLongValue("count")).isGreaterThanOrEqualTo(1);
    }

    public static class BlockingResultModel extends ResultModel {
        private final CountDownLatch serializationStarted;
        private final CountDownLatch releaseSerialization;

        BlockingResultModel(CountDownLatch serializationStarted, CountDownLatch releaseSerialization) {
            this.serializationStarted = serializationStarted;
            this.releaseSerialization = releaseSerialization;
        }

        @Override
        public String getType() {
            return "blocking";
        }

        public String getValue() throws InterruptedException {
            serializationStarted.countDown();
            releaseSerialization.await(5, TimeUnit.SECONDS);
            return "value";
        }
    }
}
