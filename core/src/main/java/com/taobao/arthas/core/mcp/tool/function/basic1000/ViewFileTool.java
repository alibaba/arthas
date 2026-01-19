package com.taobao.arthas.core.mcp.tool.function.basic1000;

import com.fasterxml.jackson.core.type.TypeReference;
import com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;
import com.taobao.arthas.mcp.server.util.JsonParser;

import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.taobao.arthas.core.mcp.tool.function.StreamableToolUtils.createCompletedResponse;
import static com.taobao.arthas.core.mcp.tool.function.StreamableToolUtils.createErrorResponse;

/**
 * ViewFile MCP Tool: 在允许目录内分段查看文件内容
 */
public class ViewFileTool extends AbstractArthasTool {

    static final String ALLOWED_DIRS_ENV = "ARTHAS_MCP_VIEWFILE_ALLOWED_DIRS";

    static final int DEFAULT_MAX_BYTES = 8192;
    static final int MAX_MAX_BYTES = 65536;

    @Tool(
            name = "viewfile",
            description = "查看文件内容（仅允许在配置的目录白名单内查看），并支持 cursor/offset 分段读取，避免一次性返回大量内容。\n" +
                    "默认允许目录：当前工作目录下的 arthas-output（若存在）、用户目录下的 ~/logs/（若存在）。\n" +
                    "配置白名单目录：\n" +
                    "- 环境变量: " + ALLOWED_DIRS_ENV + "=/path/a,/path/b\n" +
                    "使用方式：\n" +
                    "- 首次读取：传 path（可传 offset/maxBytes）\n" +
                    "- 继续读取：传 cursor（由上一次返回结果提供）"
    )
    public String viewFile(
            @ToolParam(description = "文件路径（绝对路径或相对路径；相对路径会在允许目录下解析）。当提供 cursor 时可不传。", required = false)
            String path,

            @ToolParam(description = "游标（上一段返回的 nextCursor），用于继续读取。提供 cursor 时会忽略 path/offset。", required = false)
            String cursor,

            @ToolParam(description = "起始字节偏移量（默认 0）。", required = false)
            Long offset,

            @ToolParam(description = "本次最多读取字节数（默认 8192，最大 65536）。", required = false)
            Integer maxBytes,

            ToolContext toolContext
    ) {
        try {
            List<Path> allowedRoots = loadAllowedRoots();
            if (allowedRoots.isEmpty()) {
                return JsonParser.toJson(createErrorResponse("viewfile 未配置允许目录白名单，且默认目录 arthas-output、~/logs/ 不可用。" +
                        "请通过环境变量 " + ALLOWED_DIRS_ENV + "=/path/a,/path/b 进行配置。"));
            }

            CursorRequest cursorRequest = parseCursorOrArgs(path, cursor, offset);
            Path targetFile = resolveAllowedFile(cursorRequest.path, allowedRoots);

            int readMaxBytes = clampMaxBytes(maxBytes);
            long fileSize = Files.size(targetFile);

            long requestedOffset = cursorRequest.offset;
            long effectiveOffset = adjustOffset(cursorRequest.cursorUsed, requestedOffset, fileSize);

            byte[] bytes = readBytes(targetFile, effectiveOffset, readMaxBytes, fileSize);
            int safeLen = utf8SafeLength(bytes, bytes.length);
            String content = new String(bytes, 0, safeLen, StandardCharsets.UTF_8);

            long nextOffset = effectiveOffset + safeLen;
            boolean eof = nextOffset >= fileSize;

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("path", targetFile.toString());
            result.put("fileSize", fileSize);
            result.put("requestedOffset", requestedOffset);
            result.put("startOffset", effectiveOffset);
            result.put("maxBytes", readMaxBytes);
            result.put("readBytes", safeLen);
            result.put("nextOffset", nextOffset);
            result.put("eof", eof);
            result.put("nextCursor", encodeCursor(targetFile.toString(), nextOffset));
            result.put("content", content);

            if (cursorRequest.cursorUsed && requestedOffset > fileSize) {
                result.put("cursorReset", true);
                result.put("cursorResetReason", "offsetGreaterThanFileSize");
            }

            return JsonParser.toJson(createCompletedResponse("ok", result));
        } catch (Exception e) {
            logger.error("viewfile error", e);
            return JsonParser.toJson(createErrorResponse("viewfile 执行失败: " + e.getMessage()));
        }
    }

    private static final class CursorRequest {
        private final String path;
        private final long offset;
        private final boolean cursorUsed;

        private CursorRequest(String path, long offset, boolean cursorUsed) {
            this.path = path;
            this.offset = offset;
            this.cursorUsed = cursorUsed;
        }
    }

    private CursorRequest parseCursorOrArgs(String path, String cursor, Long offset) {
        if (cursor != null && !cursor.trim().isEmpty()) {
            CursorValue decoded = decodeCursor(cursor.trim());
            return new CursorRequest(decoded.path, decoded.offset, true);
        }
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("必须提供 path 或 cursor");
        }
        if (offset != null && offset < 0) {
            throw new IllegalArgumentException("offset 不允许为负数");
        }
        long resolvedOffset = (offset != null) ? offset : 0L;
        return new CursorRequest(path.trim(), resolvedOffset, false);
    }

    private static final class CursorValue {
        private final String path;
        private final long offset;

        private CursorValue(String path, long offset) {
            this.path = path;
            this.offset = offset;
        }
    }

    private CursorValue decodeCursor(String cursor) {
        try {
            byte[] jsonBytes = Base64.getUrlDecoder().decode(cursor);
            String json = new String(jsonBytes, StandardCharsets.UTF_8);

            Map<String, Object> map = JsonParser.fromJson(json, new TypeReference<Map<String, Object>>() {});
            Object pathObj = map.get("path");
            Object offsetObj = map.get("offset");
            if (!(pathObj instanceof String) || ((String) pathObj).trim().isEmpty()) {
                throw new IllegalArgumentException("cursor 缺少 path");
            }
            if (!(offsetObj instanceof Number)) {
                throw new IllegalArgumentException("cursor 缺少 offset");
            }
            long offset = ((Number) offsetObj).longValue();
            if (offset < 0) {
                throw new IllegalArgumentException("cursor offset 不允许为负数");
            }
            return new CursorValue(((String) pathObj).trim(), offset);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("cursor 解析失败: " + e.getMessage(), e);
        }
    }

    private String encodeCursor(String path, long offset) {
        Map<String, Object> cursor = new LinkedHashMap<>();
        cursor.put("v", 1);
        cursor.put("path", path);
        cursor.put("offset", offset);
        String json = JsonParser.toJson(cursor);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private List<Path> loadAllowedRoots() {
        String config = System.getenv(ALLOWED_DIRS_ENV);

        List<Path> roots = new ArrayList<>();
        if (config != null && !config.trim().isEmpty()) {
            String[] parts = config.split(",");
            for (String part : parts) {
                String p = (part != null) ? part.trim() : "";
                if (p.isEmpty()) {
                    continue;
                }
                try {
                    Path root = Paths.get(p).toAbsolutePath().normalize();
                    if (!Files.isDirectory(root)) {
                        logger.warn("viewfile allowed dir ignored (not a directory): {}", root);
                        continue;
                    }
                    roots.add(root.toRealPath());
                } catch (Exception e) {
                    logger.warn("viewfile allowed dir ignored (invalid): {}", p, e);
                }
            }
        }

        // 默认目录：arthas-output
        try {
            Path defaultRoot = Paths.get("arthas-output").toAbsolutePath().normalize();
            if (Files.isDirectory(defaultRoot)) {
                roots.add(defaultRoot.toRealPath());
            }
        } catch (Exception e) {
            logger.debug("viewfile default root ignored: arthas-output", e);
        }

        // 默认目录：~/logs/
        try {
            Path userLogsRoot = Paths.get(System.getProperty("user.home"), "logs").toAbsolutePath().normalize();
            if (Files.isDirectory(userLogsRoot)) {
                roots.add(userLogsRoot.toRealPath());
            }
        } catch (Exception e) {
            logger.debug("viewfile default root ignored: ~/logs/", e);
        }

        return deduplicate(roots);
    }

    private static List<Path> deduplicate(List<Path> roots) {
        if (roots == null || roots.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<Path> set = new LinkedHashSet<>(roots);
        return new ArrayList<>(set);
    }

    private Path resolveAllowedFile(String requestedPath, List<Path> allowedRoots) throws Exception {
        Path req = Paths.get(requestedPath);
        if (req.isAbsolute()) {
            Path real = req.toRealPath();
            assertRegularFile(real);
            if (!isUnderAllowedRoot(real, allowedRoots)) {
                throw new IllegalArgumentException("文件不在允许目录白名单内: " + requestedPath);
            }
            return real;
        }

        for (Path root : allowedRoots) {
            Path candidate = root.resolve(req).normalize();
            if (!candidate.startsWith(root)) {
                continue;
            }
            if (!Files.exists(candidate)) {
                continue;
            }
            Path real = candidate.toRealPath();
            if (!real.startsWith(root)) {
                continue;
            }
            assertRegularFile(real);
            return real;
        }
        throw new IllegalArgumentException("文件不存在或不在允许目录白名单内: " + requestedPath);
    }

    private static void assertRegularFile(Path file) {
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("不是普通文件: " + file);
        }
    }

    private static boolean isUnderAllowedRoot(Path file, List<Path> allowedRoots) {
        for (Path root : allowedRoots) {
            if (file.startsWith(root)) {
                return true;
            }
        }
        return false;
    }

    private static int clampMaxBytes(Integer maxBytes) {
        int value = (maxBytes != null && maxBytes > 0) ? maxBytes : DEFAULT_MAX_BYTES;
        return Math.min(value, MAX_MAX_BYTES);
    }

    private static long adjustOffset(boolean cursorUsed, long requestedOffset, long fileSize) {
        if (requestedOffset < 0) {
            throw new IllegalArgumentException("offset 不允许为负数");
        }
        if (requestedOffset <= fileSize) {
            return requestedOffset;
        }
        return cursorUsed ? 0L : fileSize;
    }

    private static byte[] readBytes(Path file, long offset, int maxBytes, long fileSize) throws Exception {
        if (offset < 0 || offset > fileSize) {
            return new byte[0];
        }
        long remaining = fileSize - offset;
        int toRead = (int) Math.min(maxBytes, Math.max(0, remaining));
        if (toRead <= 0) {
            return new byte[0];
        }

        byte[] buf = new byte[toRead];
        int read;
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            raf.seek(offset);
            read = raf.read(buf);
        }
        if (read <= 0) {
            return new byte[0];
        }
        return Arrays.copyOf(buf, read);
    }

    /**
     * 避免把 UTF-8 多字节字符截断在末尾，导致展示出现大量 �。
     */
    static int utf8SafeLength(byte[] bytes, int length) {
        if (bytes == null || length <= 0) {
            return 0;
        }
        int lastIndex = length - 1;
        int lastByte = bytes[lastIndex] & 0xFF;
        if ((lastByte & 0x80) == 0) {
            return length;
        }

        int i = lastIndex;
        int continuation = 0;
        while (i >= 0 && (bytes[i] & 0xC0) == 0x80) {
            continuation++;
            i--;
        }
        if (i < 0) {
            return Math.max(0, length - continuation);
        }

        int lead = bytes[i] & 0xFF;
        int expectedLen;
        if ((lead & 0xE0) == 0xC0) {
            expectedLen = 2;
        } else if ((lead & 0xF0) == 0xE0) {
            expectedLen = 3;
        } else if ((lead & 0xF8) == 0xF0) {
            expectedLen = 4;
        } else {
            return length;
        }

        int actualLen = continuation + 1;
        if (actualLen < expectedLen) {
            return i;
        }
        return length;
    }
}
