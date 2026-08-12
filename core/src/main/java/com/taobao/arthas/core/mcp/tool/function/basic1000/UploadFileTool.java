package com.taobao.arthas.core.mcp.tool.function.basic1000;

import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;
import com.taobao.arthas.mcp.server.util.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;

/**
 * 将小文件上传到目标 JVM 文件系统，供其他 Arthas MCP 工具使用。
 */
public class UploadFileTool extends AbstractArthasTool {

    static final int DEFAULT_MAX_FILE_BYTES = 1024 * 1024;
    static final long DEFAULT_MAX_TOTAL_BYTES = 200L * 1024 * 1024;

    private static final int MAX_FILE_NAME_BYTES = 128;
    private static final Pattern SHA_256_PATTERN = Pattern.compile("[0-9a-fA-F]{64}");
    private static final Set<PosixFilePermission> DIRECTORY_PERMISSIONS = EnumSet.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE);
    private static final Set<PosixFilePermission> FILE_PERMISSIONS = EnumSet.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE);

    private final Path uploadRoot;
    private final int maxFileBytes;
    private final long maxTotalBytes;
    private final Semaphore uploadPermit = new Semaphore(1);

    public UploadFileTool() {
        this(DefaultUploadRootHolder.UPLOAD_ROOT,
                DEFAULT_MAX_FILE_BYTES,
                DEFAULT_MAX_TOTAL_BYTES);
    }

    UploadFileTool(Path uploadRoot, int maxFileBytes, long maxTotalBytes) {
        if (maxFileBytes <= 0 || maxTotalBytes < maxFileBytes) {
            throw new IllegalArgumentException("Invalid upload limits");
        }
        this.uploadRoot = prepareUploadRoot(uploadRoot);
        this.maxFileBytes = maxFileBytes;
        this.maxTotalBytes = maxTotalBytes;
    }

    @Tool(
            name = "upload_file",
            description = "上传一个小型 .class、.java 或 .jfc 文件到目标 JVM 的受控临时目录。"
                    + "内容必须使用 RFC 4648 Base64 编码；成功后返回 targetPath，可传给 mc、redefine、"
                    + "retransform 或 profiler。该工具只上传文件，不会自动执行文件。",
            taskSupport = McpSchema.TaskSupportMode.FORBIDDEN
    )
    public String uploadFile(
            @ToolParam(description = "文件叶子名称，仅允许字母、数字、点、下划线、连字符和 $；"
                    + "首版支持小写扩展名 .class、.java、.jfc，不能包含目录。")
            String fileName,

            @ToolParam(description = "RFC 4648 标准 Base64 文件内容；解码后默认最大 1 MiB。")
            String contentBase64,

            @ToolParam(description = "可选的文件 SHA-256，64 位十六进制；提供时必须与上传内容一致。",
                    required = false)
            String expectedSha256
    ) {
        if (!uploadPermit.tryAcquire()) {
            throw uploadError("UPLOAD_BUSY", "Another file upload is in progress");
        }

        try {
            return upload(fileName, contentBase64, expectedSha256);
        } finally {
            uploadPermit.release();
        }
    }

    private String upload(String fileName, String contentBase64, String expectedSha256) {
        String kind = validateFileNameAndGetKind(fileName);
        String normalizedExpectedSha256 = normalizeExpectedSha256(expectedSha256);
        byte[] content = decodeContent(contentBase64);
        String sha256 = sha256(content);

        if (normalizedExpectedSha256 != null && !normalizedExpectedSha256.equals(sha256)) {
            throw uploadError("CHECKSUM_MISMATCH", "The expected SHA-256 does not match the uploaded content");
        }

        ensureUploadRootIsSafe();
        Path artifactDirectory = uploadRoot.resolve(sha256).normalize();
        Path target = artifactDirectory.resolve(fileName).normalize();
        if (!uploadRoot.equals(artifactDirectory.getParent()) || !artifactDirectory.equals(target.getParent())) {
            throw uploadError("INVALID_FILE_NAME", "The file name must not contain a directory");
        }

        boolean reused;
        if (Files.exists(artifactDirectory, LinkOption.NOFOLLOW_LINKS)) {
            verifyArtifactDirectory(artifactDirectory);
        }
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            verifyExistingArtifact(target, sha256, content.length);
            reused = true;
        } else {
            assertQuotaAvailable(content.length);
            ensureArtifactDirectory(artifactDirectory);
            reused = commitAtomically(target, content, sha256);
        }

        String artifactId = "art_" + sha256((fileName + "\u0000" + sha256).getBytes(StandardCharsets.UTF_8));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "completed");
        result.put("stage", "final");
        result.put("message", reused ? "Upload artifact already exists" : "Upload artifact committed");
        result.put("artifactId", artifactId);
        result.put("fileName", fileName);
        result.put("kind", kind);
        result.put("targetPath", target.toString());
        result.put("sizeBytes", content.length);
        result.put("sha256", sha256);
        result.put("reused", reused);

        logger.info("MCP upload committed: artifactId={}, fileName={}, sizeBytes={}, sha256={}, reused={}",
                artifactId, fileName, content.length, sha256, reused);
        return JsonParser.toJson(result);
    }

    private String validateFileNameAndGetKind(String fileName) {
        if (fileName == null || fileName.isEmpty()
                || fileName.getBytes(StandardCharsets.UTF_8).length > MAX_FILE_NAME_BYTES) {
            throw uploadError("INVALID_FILE_NAME", "The file name is invalid");
        }

        for (int i = 0; i < fileName.length(); i++) {
            char ch = fileName.charAt(i);
            if (!(Character.isLetterOrDigit(ch) || ch == '.' || ch == '_' || ch == '-' || ch == '$')) {
                throw uploadError("INVALID_FILE_NAME", "The file name contains unsupported characters");
            }
        }

        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex <= 0) {
            throw uploadError("INVALID_FILE_NAME", "The file name must include a base name and extension");
        }

        if (fileName.endsWith(".class")) {
            return "CLASS";
        }
        if (fileName.endsWith(".java")) {
            return "JAVA_SOURCE";
        }
        if (fileName.endsWith(".jfc")) {
            return "JFC";
        }
        throw uploadError("UNSUPPORTED_FILE_TYPE",
                "Only lowercase .class, .java, and .jfc file extensions are supported");
    }

    private String normalizeExpectedSha256(String expectedSha256) {
        if (expectedSha256 == null || expectedSha256.trim().isEmpty()) {
            return null;
        }
        String normalized = expectedSha256.trim();
        if (!SHA_256_PATTERN.matcher(normalized).matches()) {
            throw uploadError("INVALID_CHECKSUM", "expectedSha256 must be 64 hexadecimal characters");
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private byte[] decodeContent(String contentBase64) {
        if (contentBase64 == null || contentBase64.isEmpty()) {
            throw uploadError("INVALID_BASE64", "contentBase64 must not be empty");
        }

        long maxEncodedLength = ((maxFileBytes + 2L) / 3L) * 4L;
        if (contentBase64.length() > maxEncodedLength) {
            throw uploadError("FILE_TOO_LARGE", "The decoded file exceeds the configured size limit");
        }

        byte[] content;
        try {
            content = Base64.getDecoder().decode(contentBase64);
        } catch (IllegalArgumentException e) {
            throw uploadError("INVALID_BASE64", "contentBase64 is not valid RFC 4648 Base64", e);
        }

        if (content.length == 0) {
            throw uploadError("EMPTY_FILE", "The uploaded file must not be empty");
        }
        if (content.length > maxFileBytes) {
            throw uploadError("FILE_TOO_LARGE", "The decoded file exceeds the configured size limit");
        }
        return content;
    }

    private void assertQuotaAvailable(long incomingBytes) {
        long totalBytes = 0;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(uploadRoot)) {
            for (Path artifactDirectory : stream) {
                if (Files.isSymbolicLink(artifactDirectory)) {
                    throw uploadError("UPLOAD_ROOT_UNSAFE", "The upload directory contains a symbolic link");
                }
                if (!Files.isDirectory(artifactDirectory, LinkOption.NOFOLLOW_LINKS)
                        || !SHA_256_PATTERN.matcher(artifactDirectory.getFileName().toString()).matches()) {
                    throw uploadError("UPLOAD_ROOT_UNSAFE", "The upload directory contains an unexpected entry");
                }
                try (DirectoryStream<Path> artifacts = Files.newDirectoryStream(artifactDirectory)) {
                    for (Path artifact : artifacts) {
                        if (Files.isSymbolicLink(artifact)
                                || !Files.isRegularFile(artifact, LinkOption.NOFOLLOW_LINKS)) {
                            throw uploadError("UPLOAD_ROOT_UNSAFE", "The artifact directory contains an unsafe entry");
                        }
                        totalBytes += Files.size(artifact);
                    }
                }
            }
        } catch (IOException e) {
            throw uploadError("IO_ERROR", "Failed to inspect the upload directory", e);
        }

        if (incomingBytes > maxTotalBytes - totalBytes) {
            throw uploadError("QUOTA_EXCEEDED", "The upload directory quota has been reached");
        }
    }

    private boolean commitAtomically(Path target, byte[] content, String expectedSha256) {
        Path temp = null;
        try {
            temp = Files.createTempFile(target.getParent(), ".upload-", ".part");
            setFilePermissions(temp);
            try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(content);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }

            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
                temp = null;
                return false;
            } catch (FileAlreadyExistsException e) {
                verifyExistingArtifact(target, expectedSha256, content.length);
                return true;
            } catch (AtomicMoveNotSupportedException e) {
                throw uploadError("IO_ERROR", "The upload directory does not support atomic file commits", e);
            }
        } catch (UploadFileException e) {
            throw e;
        } catch (IOException e) {
            throw uploadError("IO_ERROR", "Failed to commit the uploaded file", e);
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException e) {
                    logger.warn("Failed to remove temporary MCP upload file: {}", temp, e);
                }
            }
        }
    }

    private void ensureArtifactDirectory(Path artifactDirectory) {
        try {
            try {
                Files.createDirectory(artifactDirectory);
                setDirectoryPermissions(artifactDirectory);
            } catch (FileAlreadyExistsException ignored) {
                // 另一个相同内容的上传可能已经创建了该目录。
            }
            verifyArtifactDirectory(artifactDirectory);
        } catch (UploadFileException e) {
            throw e;
        } catch (IOException e) {
            throw uploadError("IO_ERROR", "Failed to prepare the artifact directory", e);
        }
    }

    private void verifyArtifactDirectory(Path artifactDirectory) {
        try {
            if (Files.isSymbolicLink(artifactDirectory)
                    || !Files.isDirectory(artifactDirectory, LinkOption.NOFOLLOW_LINKS)
                    || !artifactDirectory.equals(artifactDirectory.toRealPath())) {
                throw uploadError("UPLOAD_ROOT_UNSAFE", "The artifact directory is unavailable or unsafe");
            }
        } catch (UploadFileException e) {
            throw e;
        } catch (IOException e) {
            throw uploadError("IO_ERROR", "Failed to verify the artifact directory", e);
        }
    }

    private void verifyExistingArtifact(Path target, String expectedSha256, long expectedSize) {
        try {
            if (Files.isSymbolicLink(target) || !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)
                    || Files.size(target) != expectedSize || !expectedSha256.equals(sha256(target))) {
                throw uploadError("ARTIFACT_CONFLICT", "The destination already exists with different content");
            }
        } catch (UploadFileException e) {
            throw e;
        } catch (IOException e) {
            throw uploadError("IO_ERROR", "Failed to verify the existing upload artifact", e);
        }
    }

    private void ensureUploadRootIsSafe() {
        try {
            if (Files.isSymbolicLink(uploadRoot)
                    || !Files.isDirectory(uploadRoot, LinkOption.NOFOLLOW_LINKS)
                    || !uploadRoot.equals(uploadRoot.toRealPath())) {
                throw uploadError("UPLOAD_ROOT_UNSAFE", "The upload directory is unavailable or unsafe");
            }
        } catch (UploadFileException e) {
            throw e;
        } catch (IOException e) {
            throw uploadError("UPLOAD_ROOT_UNAVAILABLE", "The upload directory is unavailable", e);
        }
    }

    private static Path prepareUploadRoot(Path root) {
        if (root == null) {
            throw new IllegalArgumentException("uploadRoot must not be null");
        }
        try {
            Path normalized = root.toAbsolutePath().normalize();
            if (Files.isSymbolicLink(normalized)) {
                throw new IllegalArgumentException("uploadRoot must not be a symbolic link");
            }
            Files.createDirectories(normalized);
            if (!Files.isDirectory(normalized, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalArgumentException("uploadRoot must be a directory");
            }
            Path realRoot = normalized.toRealPath();
            setDirectoryPermissions(realRoot);
            return realRoot;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to prepare MCP upload directory", e);
        }
    }

    private static Path createDefaultUploadRoot() {
        try {
            return Files.createTempDirectory("arthas-mcp-uploads-");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create MCP upload directory", e);
        }
    }

    private static void setDirectoryPermissions(Path directory) throws IOException {
        try {
            Files.setPosixFilePermissions(directory, DIRECTORY_PERMISSIONS);
        } catch (UnsupportedOperationException ignored) {
            // 当前文件系统不支持 POSIX 权限。
        }
    }

    private static void setFilePermissions(Path file) throws IOException {
        try {
            Files.setPosixFilePermissions(file, FILE_PERMISSIONS);
        } catch (UnsupportedOperationException ignored) {
            // 当前文件系统不支持 POSIX 权限。
        }
    }

    private static String sha256(byte[] content) {
        MessageDigest digest = newSha256Digest();
        return toHex(digest.digest(content));
    }

    private static String sha256(Path path) throws IOException {
        MessageDigest digest = newSha256Digest();
        byte[] buffer = new byte[8192];
        try (InputStream input = Files.newInputStream(path)) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return toHex(digest.digest());
    }

    private static MessageDigest newSha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static String toHex(byte[] bytes) {
        char[] hex = new char[bytes.length * 2];
        char[] digits = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xff;
            hex[i * 2] = digits[value >>> 4];
            hex[i * 2 + 1] = digits[value & 0x0f];
        }
        return new String(hex);
    }

    private static UploadFileException uploadError(String code, String message) {
        return new UploadFileException(code, message, null);
    }

    private static UploadFileException uploadError(String code, String message, Throwable cause) {
        return new UploadFileException(code, message, cause);
    }

    static final class UploadFileException extends RuntimeException {
        private final String code;

        private UploadFileException(String code, String message, Throwable cause) {
            super(code + ": " + message, cause);
            this.code = code;
        }

        String getCode() {
            return code;
        }
    }

    private static final class DefaultUploadRootHolder {
        private static final Path UPLOAD_ROOT = createDefaultUploadRoot();
    }
}
