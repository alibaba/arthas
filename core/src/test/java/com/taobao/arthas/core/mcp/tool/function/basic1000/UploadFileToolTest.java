package com.taobao.arthas.core.mcp.tool.function.basic1000;

import com.fasterxml.jackson.core.type.TypeReference;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.tool.DefaultToolCallbackProvider;
import com.taobao.arthas.mcp.server.tool.ToolCallback;
import com.taobao.arthas.mcp.server.tool.definition.ToolDefinition;
import com.taobao.arthas.mcp.server.tool.definition.ToolDefinitions;
import com.taobao.arthas.mcp.server.util.JsonParser;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Stream;

public class UploadFileToolTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private Path uploadRoot;
    private UploadFileTool tool;

    @Before
    public void setUp() throws Exception {
        uploadRoot = temporaryFolder.newFolder("uploads").toPath().toRealPath();
        tool = new UploadFileTool(uploadRoot, 64, 256);
    }

    @Test
    public void shouldUploadFileAndReturnMetadata() throws Exception {
        byte[] content = "compiled-class-content".getBytes(StandardCharsets.UTF_8);
        String expectedSha256 = sha256(content);

        Map<String, Object> result = parse(tool.uploadFile(
                "OrderService.class", Base64.getEncoder().encodeToString(content), expectedSha256));

        Assert.assertEquals("completed", result.get("status"));
        Assert.assertEquals("CLASS", result.get("kind"));
        Assert.assertEquals("OrderService.class", result.get("fileName"));
        Assert.assertEquals(expectedSha256, result.get("sha256"));
        Assert.assertEquals(content.length, ((Number) result.get("sizeBytes")).intValue());
        Assert.assertEquals(Boolean.FALSE, result.get("reused"));
        Assert.assertTrue(String.valueOf(result.get("artifactId")).startsWith("art_"));

        Path target = java.nio.file.Paths.get(String.valueOf(result.get("targetPath"))).toRealPath();
        Assert.assertEquals("OrderService.class", target.getFileName().toString());
        Assert.assertEquals(uploadRoot, target.getParent().getParent());
        Assert.assertEquals(expectedSha256, target.getParent().getFileName().toString());
        Assert.assertArrayEquals(content, Files.readAllBytes(target));
    }

    @Test
    public void shouldReuseIdenticalUpload() {
        byte[] content = "source-content".getBytes(StandardCharsets.UTF_8);
        String encoded = Base64.getEncoder().encodeToString(content);

        Map<String, Object> first = parse(tool.uploadFile("Demo.java", encoded, null));
        Map<String, Object> second = parse(tool.uploadFile("Demo.java", encoded, null));

        Assert.assertEquals(first.get("artifactId"), second.get("artifactId"));
        Assert.assertEquals(first.get("targetPath"), second.get("targetPath"));
        Assert.assertEquals(Boolean.FALSE, first.get("reused"));
        Assert.assertEquals(Boolean.TRUE, second.get("reused"));
        Assert.assertEquals("Demo.java", java.nio.file.Paths.get(
                String.valueOf(second.get("targetPath"))).getFileName().toString());
    }

    @Test
    public void shouldRejectUnsafeFileNames() {
        String encoded = Base64.getEncoder().encodeToString(new byte[] {1});
        String[] invalidNames = {"../Demo.class", "/tmp/Demo.class", "dir\\Demo.class", "Demo?.class", ".class"};

        for (String invalidName : invalidNames) {
            assertUploadError("INVALID_FILE_NAME", new UploadCall() {
                @Override
                public void run() {
                    tool.uploadFile(invalidName, encoded, null);
                }
            });
        }
    }

    @Test
    public void shouldRejectUnsupportedFileType() {
        assertUploadError("UNSUPPORTED_FILE_TYPE", new UploadCall() {
            @Override
            public void run() {
                tool.uploadFile("payload.txt", Base64.getEncoder().encodeToString(new byte[] {1}), null);
            }
        });
    }

    @Test
    public void shouldRejectNonLowercaseFileExtensions() {
        final String encoded = Base64.getEncoder().encodeToString(new byte[] {1});
        String[] invalidNames = {"Demo.CLASS", "Demo.JAVA", "profile.JFC"};

        for (final String invalidName : invalidNames) {
            assertUploadError("UNSUPPORTED_FILE_TYPE", new UploadCall() {
                @Override
                public void run() {
                    tool.uploadFile(invalidName, encoded, null);
                }
            });
        }
    }

    @Test
    public void shouldRejectInvalidBase64() {
        assertUploadError("INVALID_BASE64", new UploadCall() {
            @Override
            public void run() {
                tool.uploadFile("Demo.class", "%%%", null);
            }
        });
    }

    @Test
    public void shouldRejectInvalidChecksum() {
        assertUploadError("INVALID_CHECKSUM", new UploadCall() {
            @Override
            public void run() {
                tool.uploadFile("Demo.class", Base64.getEncoder().encodeToString(new byte[] {1}), "not-a-sha256");
            }
        });
    }

    @Test
    public void shouldRejectOversizedFile() {
        final UploadFileTool smallTool = new UploadFileTool(uploadRoot, 3, 16);
        final String encoded = Base64.getEncoder().encodeToString(new byte[] {1, 2, 3, 4});

        assertUploadError("FILE_TOO_LARGE", new UploadCall() {
            @Override
            public void run() {
                smallTool.uploadFile("Demo.class", encoded, null);
            }
        });
    }

    @Test
    public void shouldRejectChecksumMismatchWithoutWritingFile() throws Exception {
        final String encoded = Base64.getEncoder().encodeToString("source".getBytes(StandardCharsets.UTF_8));
        final String wrongSha256 = repeat('0', 64);

        assertUploadError("CHECKSUM_MISMATCH", new UploadCall() {
            @Override
            public void run() {
                tool.uploadFile("Demo.java", encoded, wrongSha256);
            }
        });

        try (Stream<Path> entries = Files.list(uploadRoot)) {
            Assert.assertEquals(0, entries.count());
        }
    }

    @Test
    public void shouldEnforceTotalSizeQuota() {
        final UploadFileTool quotaTool = new UploadFileTool(uploadRoot, 16, 16);
        quotaTool.uploadFile("One.class", Base64.getEncoder().encodeToString(new byte[8]), null);

        assertUploadError("QUOTA_EXCEEDED", new UploadCall() {
            @Override
            public void run() {
                quotaTool.uploadFile("Two.class", Base64.getEncoder().encodeToString(new byte[9]), null);
            }
        });
    }

    @Test
    public void shouldNotLimitArtifactCount() {
        for (int i = 0; i < 65; i++) {
            tool.uploadFile("Artifact" + i + ".class",
                    Base64.getEncoder().encodeToString(new byte[] {(byte) i}), null);
        }
    }

    @Test
    public void shouldUse200MiBDefaultTotalQuota() {
        Assert.assertEquals(200L * 1024 * 1024, UploadFileTool.DEFAULT_MAX_TOTAL_BYTES);
    }

    @Test
    public void shouldRejectSymlinkArtifactDirectory() throws Exception {
        final byte[] content = "class-content".getBytes(StandardCharsets.UTF_8);
        Path outside = temporaryFolder.newFolder("outside").toPath();
        Path digestDirectory = uploadRoot.resolve(sha256(content));
        try {
            Files.createSymbolicLink(digestDirectory, outside);
        } catch (UnsupportedOperationException e) {
            Assume.assumeNoException("The filesystem does not support symbolic links", e);
        } catch (IOException e) {
            Assume.assumeNoException("Unable to create a symbolic link for this test", e);
        }

        assertUploadError("UPLOAD_ROOT_UNSAFE", new UploadCall() {
            @Override
            public void run() {
                tool.uploadFile("Demo.class", Base64.getEncoder().encodeToString(content), null);
            }
        });
    }

    @Test
    public void shouldExposeFlatMcpSchema() throws Exception {
        Method method = UploadFileTool.class.getMethod(
                "uploadFile", String.class, String.class, String.class);
        ToolDefinition definition = ToolDefinitions.from(method);
        McpSchema.JsonSchema schema = definition.getInputSchema();

        Assert.assertEquals("upload_file", definition.getName());
        Assert.assertEquals(McpSchema.TaskSupportMode.FORBIDDEN, definition.taskSupport());
        Assert.assertTrue(schema.getProperties().containsKey("fileName"));
        Assert.assertTrue(schema.getProperties().containsKey("contentBase64"));
        Assert.assertTrue(schema.getProperties().containsKey("expectedSha256"));
        Assert.assertTrue(schema.getRequired().contains("fileName"));
        Assert.assertTrue(schema.getRequired().contains("contentBase64"));
        Assert.assertFalse(schema.getRequired().contains("expectedSha256"));
        Assert.assertEquals(Boolean.FALSE, schema.getAdditionalProperties());
    }

    @Test
    public void shouldBeDiscoveredByToolProvider() {
        DefaultToolCallbackProvider provider = new DefaultToolCallbackProvider();
        provider.setToolBasePackage("com.taobao.arthas.core.mcp.tool.function.basic1000");

        boolean found = false;
        for (ToolCallback callback : provider.getToolCallbacks()) {
            if ("upload_file".equals(callback.getToolDefinition().getName())) {
                found = true;
                break;
            }
        }

        Assert.assertTrue("upload_file should be discovered by the MCP tool scanner", found);
    }

    private static void assertUploadError(String expectedCode, UploadCall call) {
        try {
            call.run();
            Assert.fail("Expected upload error " + expectedCode);
        } catch (UploadFileTool.UploadFileException e) {
            Assert.assertEquals(expectedCode, e.getCode());
            Assert.assertTrue(e.getMessage().startsWith(expectedCode + ":"));
        }
    }

    private static Map<String, Object> parse(String json) {
        return JsonParser.fromJson(json, new TypeReference<Map<String, Object>>() {});
    }

    private static String sha256(byte[] content) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
        StringBuilder result = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            result.append(String.format("%02x", value & 0xff));
        }
        return result.toString();
    }

    private static String repeat(char value, int count) {
        StringBuilder result = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            result.append(value);
        }
        return result.toString();
    }

    private interface UploadCall {
        void run();
    }
}
