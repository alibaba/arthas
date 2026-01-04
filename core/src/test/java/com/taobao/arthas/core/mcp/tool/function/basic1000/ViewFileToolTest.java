package com.taobao.arthas.core.mcp.tool.function.basic1000;

import com.fasterxml.jackson.core.type.TypeReference;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.util.JsonParser;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ViewFileToolTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final ToolContext toolContext = new ToolContext(Collections.emptyMap());

    private final ViewFileTool tool = new ViewFileTool();

    @Before
    public void setUp() {
        clearEnv(ViewFileTool.ALLOWED_DIRS_ENV);
    }

    @After
    public void tearDown() {
        clearEnv(ViewFileTool.ALLOWED_DIRS_ENV);
    }

    /**
     * 通过反射设置环境变量（仅用于测试）
     */
    @SuppressWarnings("unchecked")
    private static void setEnv(String key, String value) {
        try {
            Map<String, String> env = System.getenv();
            Field field = env.getClass().getDeclaredField("m");
            field.setAccessible(true);
            Map<String, String> writableEnv = (Map<String, String>) field.get(env);
            writableEnv.put(key, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set environment variable: " + key, e);
        }
    }

    /**
     * 通过反射清除环境变量（仅用于测试）
     */
    @SuppressWarnings("unchecked")
    private static void clearEnv(String key) {
        try {
            Map<String, String> env = System.getenv();
            Field field = env.getClass().getDeclaredField("m");
            field.setAccessible(true);
            Map<String, String> writableEnv = (Map<String, String>) field.get(env);
            writableEnv.remove(key);
        } catch (Exception e) {
            // ignore
        }
    }

    @Test
    public void should_error_when_file_not_found_or_not_allowed() {
        String json = tool.viewFile("a.txt", null, null, 10, toolContext);
        Map<String, Object> result = parse(json);
        Assert.assertEquals("error", result.get("status"));
        Assert.assertNotNull(result.get("message"));
    }

    @Test
    public void should_read_file_in_chunks_with_cursor() throws Exception {
        File allowedDir = temporaryFolder.newFolder("allowed");
        setEnv(ViewFileTool.ALLOWED_DIRS_ENV, allowedDir.getAbsolutePath());

        Path file = allowedDir.toPath().resolve("test.txt");
        Files.write(file, "abcdefghijklmnopqrstuvwxyz".getBytes(StandardCharsets.UTF_8));

        Map<String, Object> first = parse(tool.viewFile("test.txt", null, 0L, 5, toolContext));
        Assert.assertEquals("completed", first.get("status"));
        Assert.assertEquals("abcde", first.get("content"));

        String nextCursor = String.valueOf(first.get("nextCursor"));
        Assert.assertNotNull(nextCursor);
        Assert.assertFalse(nextCursor.trim().isEmpty());

        Map<String, Object> second = parse(tool.viewFile(null, nextCursor, null, 5, toolContext));
        Assert.assertEquals("completed", second.get("status"));
        Assert.assertEquals("fghij", second.get("content"));
    }

    @Test
    public void should_reject_absolute_path_outside_allowed_root() throws Exception {
        File allowedDir = temporaryFolder.newFolder("allowed");
        setEnv(ViewFileTool.ALLOWED_DIRS_ENV, allowedDir.getAbsolutePath());

        File outside = temporaryFolder.newFile("outside.txt");
        Files.write(outside.toPath(), "outside".getBytes(StandardCharsets.UTF_8));

        Map<String, Object> result = parse(tool.viewFile(outside.getAbsolutePath(), null, 0L, 10, toolContext));
        Assert.assertEquals("error", result.get("status"));
    }

    @Test
    public void should_reject_path_traversal_outside_allowed_root() throws Exception {
        File allowedDir = temporaryFolder.newFolder("allowed");
        setEnv(ViewFileTool.ALLOWED_DIRS_ENV, allowedDir.getAbsolutePath());

        File outside = temporaryFolder.newFile("outside.txt");
        Files.write(outside.toPath(), "outside".getBytes(StandardCharsets.UTF_8));

        Map<String, Object> result = parse(tool.viewFile("../outside.txt", null, 0L, 10, toolContext));
        Assert.assertEquals("error", result.get("status"));
    }

    @Test
    public void should_reject_cursor_tampering_outside_allowed_root() throws Exception {
        File allowedDir = temporaryFolder.newFolder("allowed");
        setEnv(ViewFileTool.ALLOWED_DIRS_ENV, allowedDir.getAbsolutePath());

        File outside = temporaryFolder.newFile("outside.txt");
        Files.write(outside.toPath(), "outside".getBytes(StandardCharsets.UTF_8));

        Map<String, Object> cursor = new LinkedHashMap<>();
        cursor.put("v", 1);
        cursor.put("path", outside.getAbsolutePath());
        cursor.put("offset", 0);
        String cursorJson = JsonParser.toJson(cursor);
        String encodedCursor = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(cursorJson.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> result = parse(tool.viewFile(null, encodedCursor, null, 10, toolContext));
        Assert.assertEquals("error", result.get("status"));
    }

    @Test
    public void should_error_for_negative_offset() throws Exception {
        File allowedDir = temporaryFolder.newFolder("allowed");
        setEnv(ViewFileTool.ALLOWED_DIRS_ENV, allowedDir.getAbsolutePath());

        Path file = allowedDir.toPath().resolve("test.txt");
        Files.write(file, "abc".getBytes(StandardCharsets.UTF_8));

        Map<String, Object> result = parse(tool.viewFile("test.txt", null, -1L, 10, toolContext));
        Assert.assertEquals("error", result.get("status"));
    }

    @Test
    public void should_reject_symlink_escape() throws Exception {
        File allowedDir = temporaryFolder.newFolder("allowed");
        setEnv(ViewFileTool.ALLOWED_DIRS_ENV, allowedDir.getAbsolutePath());

        File outside = temporaryFolder.newFile("outside.txt");
        Files.write(outside.toPath(), "outside".getBytes(StandardCharsets.UTF_8));

        Path link = allowedDir.toPath().resolve("link.txt");
        try {
            Files.createSymbolicLink(link, outside.toPath());
        } catch (UnsupportedOperationException e) {
            Assume.assumeNoException("当前平台不支持创建符号链接，跳过", e);
        } catch (Exception e) {
            Assume.assumeNoException("创建符号链接失败，跳过", e);
        }

        Map<String, Object> result = parse(tool.viewFile("link.txt", null, 0L, 10, toolContext));
        Assert.assertEquals("error", result.get("status"));
    }

    private static Map<String, Object> parse(String json) {
        return JsonParser.fromJson(json, new TypeReference<Map<String, Object>>() {});
    }
}
