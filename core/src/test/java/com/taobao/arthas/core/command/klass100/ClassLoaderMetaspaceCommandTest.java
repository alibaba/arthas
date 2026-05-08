package com.taobao.arthas.core.command.klass100;

import com.taobao.arthas.core.command.model.ClassLoaderMetaspaceModel.Row;
import org.junit.Assert;
import org.junit.Test;

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

    private static Row row(String name, long chunkSize, long blockSize) {
        return new Row().setName(name).setChunkSize(chunkSize).setBlockSize(blockSize);
    }

}
