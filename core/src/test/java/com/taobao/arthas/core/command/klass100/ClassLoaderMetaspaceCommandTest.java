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
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
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
    public void testFindUniqueFallbackMappingByTypeAndClassCount() {
        ClassLoaderMetaspaceCommand.LoaderMapping expected = mapping("hash-a", "loader.Type", 7);
        ClassLoaderMetaspaceCommand.LoaderMapping other = mapping("hash-b", "loader.Other", 7);

        ClassLoaderMetaspaceCommand.LoaderMapping actual = ClassLoaderMetaspaceCommand.findUniqueFallbackMapping(
                stats("loader.Type", 7), Arrays.asList(expected, other));

        Assert.assertSame(expected, actual);
    }

    @Test
    public void testFindUniqueFallbackMappingUsesHiddenClassCount() {
        ClassLoaderMetaspaceCommand.LoaderMapping expected = mapping("hash-a", "loader.Type", 8);
        ClassLoaderMetaspaceCommand.LoaderMapping other = mapping("hash-b", "loader.Other", 8);

        ClassLoaderMetaspaceCommand.LoaderMapping actual = ClassLoaderMetaspaceCommand.findUniqueFallbackMapping(
                stats("loader.Type", 7, 1), Arrays.asList(expected, other));

        Assert.assertSame(expected, actual);
    }

    @Test
    public void testFindUniqueFallbackMappingRejectsAmbiguousCandidates() {
        ClassLoaderMetaspaceCommand.LoaderMapping first = mapping("hash-a", "loader.Type", 7);
        ClassLoaderMetaspaceCommand.LoaderMapping second = mapping("hash-b", "loader.Type", 7);

        ClassLoaderMetaspaceCommand.LoaderMapping actual = ClassLoaderMetaspaceCommand.findUniqueFallbackMapping(
                stats("loader.Type", 7), Arrays.asList(first, second));

        Assert.assertNull(actual);
    }

    @Test
    public void testFindUniqueFallbackMappingRequiresExactClassCount() {
        ClassLoaderMetaspaceCommand.LoaderMapping candidate = mapping("hash-a", "loader.Type", 8);

        ClassLoaderMetaspaceCommand.LoaderMapping actual = ClassLoaderMetaspaceCommand.findUniqueFallbackMapping(
                stats("loader.Type", 7), Arrays.asList(candidate));

        Assert.assertNull(actual);
    }

    @Test
    public void testEmitMappingsIgnoresHashFilterForGlobalFallbackCandidates() throws Exception {
        ClassLoaderMetaspaceCommand command = new ClassLoaderMetaspaceCommand();
        command.setHashCode("missing-hash");

        Object summary = emitMappings(command, instrumentationFor(ClassLoaderMetaspaceCommandTest.class));

        Assert.assertEquals(1, summaryLong(summary, "candidateLoaderCount"));
        Assert.assertEquals(1, summaryLong(summary, "emittedMappingCount"));
    }

    @Test
    public void testReadHiddenBlockSize() throws Exception {
        Assert.assertEquals(123, ClassLoaderMetaspaceCommand.readHiddenBlockSize(recordHiddenBlockSizeEvent(123)));
    }

    @Test
    public void testReadHiddenClassCount() throws Exception {
        Assert.assertEquals(12, ClassLoaderMetaspaceCommand.readHiddenClassCount(recordHiddenClassCountEvent(12)));
    }

    @Test
    public void testReadHiddenClassCountFallbackToAnonymousClassCount() throws Exception {
        Assert.assertEquals(21,
                ClassLoaderMetaspaceCommand.readHiddenClassCount(recordAnonymousClassCountEvent(21)));
    }

    @Test
    public void testReadHiddenChunkSize() throws Exception {
        Assert.assertEquals(34, ClassLoaderMetaspaceCommand.readHiddenChunkSize(recordHiddenChunkSizeEvent(34)));
    }

    @Test
    public void testReadHiddenChunkSizeFallbackToAnonymousChunkSize() throws Exception {
        Assert.assertEquals(43,
                ClassLoaderMetaspaceCommand.readHiddenChunkSize(recordAnonymousChunkSizeEvent(43)));
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

    private static ClassLoaderMetaspaceCommand.LoaderMapping mapping(String hash, String type, long loadedClassCount) {
        return new ClassLoaderMetaspaceCommand.LoaderMapping(hash, type, type + "@" + hash, loadedClassCount);
    }

    private static ClassLoaderMetaspaceCommand.StatsRow stats(String type, long classCount) {
        return stats(type, classCount, 0);
    }

    private static ClassLoaderMetaspaceCommand.StatsRow stats(String type, long classCount, long hiddenClassCount) {
        return new ClassLoaderMetaspaceCommand.StatsRow(Instant.EPOCH, 1, 0, null, null, type, null, null, 0,
                classCount, hiddenClassCount, 0, 0, 0, 0);
    }

    private static Object emitMappings(ClassLoaderMetaspaceCommand command, Instrumentation instrumentation)
            throws Exception {
        Method method = ClassLoaderMetaspaceCommand.class.getDeclaredMethod("emitMappings", Instrumentation.class);
        method.setAccessible(true);
        return method.invoke(command, instrumentation);
    }

    private static long summaryLong(Object summary, String fieldName) throws Exception {
        Field field = summary.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getLong(summary);
    }

    private static Instrumentation instrumentationFor(final Class<?>... classes) {
        return (Instrumentation) Proxy.newProxyInstance(
                ClassLoaderMetaspaceCommandTest.class.getClassLoader(),
                new Class<?>[] { Instrumentation.class },
                (proxy, method, args) -> {
                    if ("getAllLoadedClasses".equals(method.getName())) {
                        return classes;
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) {
                        return false;
                    }
                    if (returnType == long.class) {
                        return 0L;
                    }
                    if (returnType == int.class) {
                        return 0;
                    }
                    if (returnType == Class[].class) {
                        return new Class<?>[0];
                    }
                    return null;
                });
    }

    private static RecordedEvent recordHiddenClassCountEvent(long value) throws IOException {
        Recording recording = new Recording();
        Path output = Files.createTempFile("arthas-hidden-class-count-", ".jfr");
        try {
            recording.enable(HiddenClassCountEvent.class).withoutStackTrace();
            recording.start();
            HiddenClassCountEvent event = new HiddenClassCountEvent();
            event.hiddenClassCount = value;
            event.commit();
            recording.stop();
            recording.dump(output);
            return readOnlyEvent(output);
        } finally {
            recording.close();
            Files.deleteIfExists(output);
        }
    }

    private static RecordedEvent recordHiddenChunkSizeEvent(long value) throws IOException {
        Recording recording = new Recording();
        Path output = Files.createTempFile("arthas-hidden-chunk-size-", ".jfr");
        try {
            recording.enable(HiddenChunkSizeEvent.class).withoutStackTrace();
            recording.start();
            HiddenChunkSizeEvent event = new HiddenChunkSizeEvent();
            event.hiddenChunkSize = value;
            event.commit();
            recording.stop();
            recording.dump(output);
            return readOnlyEvent(output);
        } finally {
            recording.close();
            Files.deleteIfExists(output);
        }
    }

    private static RecordedEvent recordAnonymousClassCountEvent(long value) throws IOException {
        Recording recording = new Recording();
        Path output = Files.createTempFile("arthas-anonymous-class-count-", ".jfr");
        try {
            recording.enable(AnonymousClassCountEvent.class).withoutStackTrace();
            recording.start();
            AnonymousClassCountEvent event = new AnonymousClassCountEvent();
            event.anonymousClassCount = value;
            event.commit();
            recording.stop();
            recording.dump(output);
            return readOnlyEvent(output);
        } finally {
            recording.close();
            Files.deleteIfExists(output);
        }
    }

    private static RecordedEvent recordAnonymousChunkSizeEvent(long value) throws IOException {
        Recording recording = new Recording();
        Path output = Files.createTempFile("arthas-anonymous-chunk-size-", ".jfr");
        try {
            recording.enable(AnonymousChunkSizeEvent.class).withoutStackTrace();
            recording.start();
            AnonymousChunkSizeEvent event = new AnonymousChunkSizeEvent();
            event.anonymousChunkSize = value;
            event.commit();
            recording.stop();
            recording.dump(output);
            return readOnlyEvent(output);
        } finally {
            recording.close();
            Files.deleteIfExists(output);
        }
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

    @Name("arthas.test.HiddenClassCountEvent")
    public static class HiddenClassCountEvent extends Event {
        long hiddenClassCount;
    }

    @Name("arthas.test.HiddenChunkSizeEvent")
    public static class HiddenChunkSizeEvent extends Event {
        long hiddenChunkSize;
    }

    @Name("arthas.test.AnonymousClassCountEvent")
    public static class AnonymousClassCountEvent extends Event {
        long anonymousClassCount;
    }

    @Name("arthas.test.AnonymousChunkSizeEvent")
    public static class AnonymousChunkSizeEvent extends Event {
        long anonymousChunkSize;
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
