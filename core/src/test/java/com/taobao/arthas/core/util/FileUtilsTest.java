package com.taobao.arthas.core.util;

import com.taobao.arthas.core.testtool.TestUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;

public class FileUtilsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private File getTestDirectory() {
        return temporaryFolder.getRoot();
    }


    @Test
    public void testGetTestDirectory(){
        Assert.assertNotNull(getTestDirectory());
    }

    @Test
    public void testOpenOutputStreamIsDirectory() throws IOException {
        thrown.expectMessage(allOf(startsWith("File '") ,endsWith("' exists but is a directory")));
        FileUtils.openOutputStream(getTestDirectory(), true);

        thrown.expectMessage(allOf(startsWith("File '") ,endsWith("' exists but is a directory")));
        FileUtils.openOutputStream(getTestDirectory(), false);
    }

    @Test
    public void testOpenOutputStreamCannotWrite() throws IOException {
        thrown.expectMessage(allOf(startsWith("File '") ,endsWith("' cannot be written to")));
        File targetFile = temporaryFolder.newFile("cannotWrite.txt");
        targetFile.setWritable(false);
        FileUtils.openOutputStream(targetFile, true);
    }

    @Test
    public void testOpenOutputStream() throws IOException {
        File targetFile = temporaryFolder.newFile("targetFile.txt");
        FileOutputStream outputStream = FileUtils.openOutputStream(targetFile, true);
        Assert.assertNotNull(outputStream);
        outputStream.close();
    }

    @Test
    public void testWriteByteArrayToFile() throws IOException {
        String data = "test data";
        File targetFile = temporaryFolder.newFile("targetFile.txt");
        FileUtils.writeByteArrayToFile(targetFile, data.getBytes());
        TestUtils.assertEqualContent(data.getBytes(), targetFile);
    }

    @Test
    public void testWriteByteArrayToFileWithAppend() throws IOException {
        String data = "test data";
        File targetFile = temporaryFolder.newFile("targetFile.txt");
        FileUtils.writeByteArrayToFile(targetFile, data.getBytes(), true);
        TestUtils.assertEqualContent(data.getBytes(), targetFile);
    }


    @Test
    public void testReadFileToString() throws IOException {
        String data = "test data";
        File targetFile = temporaryFolder.newFile("targetFile.txt");
        FileUtils.writeByteArrayToFile(targetFile, data.getBytes(), true);
        String content = FileUtils.readFileToString(targetFile, Charset.defaultCharset());
        TestUtils.assertEqualContent(content.getBytes(), targetFile);
    }


    @Test
    public void testSaveCommandHistory() throws IOException {
        //cls
        int[] command1 = new int[]{99,108,115};
        File targetFile = temporaryFolder.newFile("targetFile.txt");
        FileUtils.saveCommandHistory(TestUtils.newArrayList(command1), targetFile);
        TestUtils.assertEqualContent("cls\n".getBytes(), targetFile);
    }

    @Test
    public void testLoadCommandHistory() throws IOException {
        //cls
        int[] command1 = new int[]{99,108,115};
        File targetFile = temporaryFolder.newFile("targetFile.txt");
        FileUtils.saveCommandHistory(TestUtils.newArrayList(command1), targetFile);
        List<int[]> content = FileUtils.loadCommandHistory(targetFile);
        Assert.assertArrayEquals(command1, content.get(0));
    }







}
