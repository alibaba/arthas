package com.taobao.arthas.core.command.klass100;

import com.taobao.arthas.core.command.model.ClassLoaderMetaspaceModel.Row;
import jdk.jfr.Event;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class ClassLoaderMetaspaceCommandTest {

    @Test
    public void testParseTimeMillis() {
        Assert.assertEquals(2500, ClassLoaderMetaspaceCommand.parseTimeMillis("2500", "duration"));
        Assert.assertEquals(2500, ClassLoaderMetaspaceCommand.parseTimeMillis("2500ms", "duration"));
        Assert.assertEquals(3000, ClassLoaderMetaspaceCommand.parseTimeMillis("3s", "duration"));
        Assert.assertEquals(120000, ClassLoaderMetaspaceCommand.parseTimeMillis("2m", "duration"));
    }

    @Test
    public void testParseTimeMillisRejectInvalidValue() {
        try {
            ClassLoaderMetaspaceCommand.parseTimeMillis("abc", "duration");
            Assert.fail("invalid duration should fail");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("duration"));
        }

        try {
            ClassLoaderMetaspaceCommand.parseTimeMillis("0", "period");
            Assert.fail("zero period should fail");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("period"));
        }
    }

    @Test
    public void testReadDisplayNameFallbackOrder() {
        Assert.assertEquals("jfr-loader",
                ClassLoaderMetaspaceCommand.selectDisplayName("jfr-loader", "fallback-loader", "loader.Type"));
        Assert.assertEquals("fallback-loader",
                ClassLoaderMetaspaceCommand.selectDisplayName(null, "fallback-loader", "loader.Type"));
        Assert.assertEquals("loader.Type",
                ClassLoaderMetaspaceCommand.selectDisplayName(null, null, "loader.Type"));
    }

    @Test
    public void testSortAndLimit() {
        Row small = row("small", 100, 90);
        Row largeB = row("b-large", 300, 100);
        Row largeA = row("a-large", 300, 100);
        Row middle = row("middle", 300, 90);

        List<Row> rows = ClassLoaderMetaspaceCommand.sortAndLimit(
                Arrays.asList(small, largeB, largeA, middle), 3);

        Assert.assertEquals("a-large", rows.get(0).getName());
        Assert.assertEquals("b-large", rows.get(1).getName());
        Assert.assertEquals("middle", rows.get(2).getName());
        Assert.assertEquals(3, rows.size());
    }

    @Test
    public void testReadHiddenBlockSize() throws Exception {
        Assert.assertEquals(123, ClassLoaderMetaspaceCommand.readHiddenBlockSize(recordHiddenBlockSizeEvent(123)));
    }

    @Test
    public void testReadHiddenBlockSizeFallbackToAnonymousBlockSize() throws Exception {
        Assert.assertEquals(456,
                ClassLoaderMetaspaceCommand.readHiddenBlockSize(recordAnonymousBlockSizeEvent(456)));
    }

    @Test
    public void testReadHiddenBlockSizeFallbackToZero() throws Exception {
        Assert.assertEquals(0, ClassLoaderMetaspaceCommand.readHiddenBlockSize(recordNoBlockSizeEvent()));
    }

    private static Row row(String name, long chunkSize, long blockSize) {
        return new Row().setName(name).setChunkSize(chunkSize).setBlockSize(blockSize);
    }

    private static RecordedEvent recordHiddenBlockSizeEvent(long value) throws IOException {
        Recording recording = new Recording();
        Path output = Files.createTempFile("arthas-hidden-block-size-", ".jfr");
        try {
            recording.enable(HiddenBlockSizeEvent.class).withoutStackTrace();
            recording.start();
            HiddenBlockSizeEvent event = new HiddenBlockSizeEvent();
            event.hiddenBlockSize = value;
            event.commit();
            recording.stop();
            recording.dump(output);
            return readOnlyEvent(output);
        } finally {
            recording.close();
            Files.deleteIfExists(output);
        }
    }

    private static RecordedEvent recordAnonymousBlockSizeEvent(long value) throws IOException {
        Recording recording = new Recording();
        Path output = Files.createTempFile("arthas-anonymous-block-size-", ".jfr");
        try {
            recording.enable(AnonymousBlockSizeEvent.class).withoutStackTrace();
            recording.start();
            AnonymousBlockSizeEvent event = new AnonymousBlockSizeEvent();
            event.anonymousBlockSize = value;
            event.commit();
            recording.stop();
            recording.dump(output);
            return readOnlyEvent(output);
        } finally {
            recording.close();
            Files.deleteIfExists(output);
        }
    }

    private static RecordedEvent recordNoBlockSizeEvent() throws IOException {
        Recording recording = new Recording();
        Path output = Files.createTempFile("arthas-no-block-size-", ".jfr");
        try {
            recording.enable(NoBlockSizeEvent.class).withoutStackTrace();
            recording.start();
            NoBlockSizeEvent event = new NoBlockSizeEvent();
            event.value = 789;
            event.commit();
            recording.stop();
            recording.dump(output);
            return readOnlyEvent(output);
        } finally {
            recording.close();
            Files.deleteIfExists(output);
        }
    }

    private static RecordedEvent readOnlyEvent(Path output) throws IOException {
        RecordingFile file = new RecordingFile(output);
        try {
            Assert.assertTrue(file.hasMoreEvents());
            return file.readEvent();
        } finally {
            file.close();
        }
    }

    @Name("arthas.test.HiddenBlockSizeEvent")
    public static class HiddenBlockSizeEvent extends Event {
        long hiddenBlockSize;
    }

    @Name("arthas.test.AnonymousBlockSizeEvent")
    public static class AnonymousBlockSizeEvent extends Event {
        long anonymousBlockSize;
    }

    @Name("arthas.test.NoBlockSizeEvent")
    public static class NoBlockSizeEvent extends Event {
        long value;
    }

}
