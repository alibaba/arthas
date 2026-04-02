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
 *
 * <p>该工具提供了安全的文件查看功能，具有以下特点：
 * <ul>
 * <li>支持目录白名单机制，只允许查看指定目录下的文件</li>
 * <li>支持分段读取，避免一次性加载大文件导致内存溢出</li>
 * <li>使用 cursor 机制支持分页读取大文件</li>
 * <li>支持 UTF-8 编码的安全截断，避免多字节字符被截断</li>
 * </ul>
 *
 * @author Arthas Team
 * @see com.taobao.arthas.core.mcp.tool.function.AbstractArthasTool
 */
public class ViewFileTool extends AbstractArthasTool {

    /** 环境变量名称，用于配置允许查看的目录白名单 */
    static final String ALLOWED_DIRS_ENV = "ARTHAS_MCP_VIEWFILE_ALLOWED_DIRS";

    /** 默认每次读取的最大字节数：8KB */
    static final int DEFAULT_MAX_BYTES = 8192;

    /** 允许的最大读取字节数：64KB */
    static final int MAX_MAX_BYTES = 65536;

    /**
     * 查看文件内容的主方法
     *
     * <p>该方法支持两种读取模式：
     * <ul>
     * <li>首次读取：提供文件路径和可选的偏移量</li>
     * <li>继续读取：提供游标(cursor)从上次位置继续读取</li>
     * </ul>
     *
     * @param path      文件路径（绝对路径或相对路径；相对路径会在允许目录下解析）。当提供 cursor 时可不传
     * @param cursor    游标（上一段返回的 nextCursor），用于继续读取。提供 cursor 时会忽略 path/offset
     * @param offset    起始字节偏移量（默认 0）
     * @param maxBytes  本次最多读取字节数（默认 8192，最大 65536）
     * @param toolContext 工具上下文
     * @return JSON 格式的响应，包含文件内容和元数据
     */
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
            // 加载允许的根目录列表
            List<Path> allowedRoots = loadAllowedRoots();
            if (allowedRoots.isEmpty()) {
                return JsonParser.toJson(createErrorResponse("viewfile 未配置允许目录白名单，且默认目录 arthas-output、~/logs/ 不可用。" +
                        "请通过环境变量 " + ALLOWED_DIRS_ENV + "=/path/a,/path/b 进行配置。"));
            }

            // 解析游标或参数
            CursorRequest cursorRequest = parseCursorOrArgs(path, cursor, offset);
            // 解析目标文件的完整路径
            Path targetFile = resolveAllowedFile(cursorRequest.path, allowedRoots);

            // 限制最大读取字节数
            int readMaxBytes = clampMaxBytes(maxBytes);
            // 获取文件大小
            long fileSize = Files.size(targetFile);

            // 获取请求的偏移量
            long requestedOffset = cursorRequest.offset;
            // 调整有效的偏移量
            long effectiveOffset = adjustOffset(cursorRequest.cursorUsed, requestedOffset, fileSize);

            // 读取字节数据
            byte[] bytes = readBytes(targetFile, effectiveOffset, readMaxBytes, fileSize);
            // 计算 UTF-8 安全长度，避免截断多字节字符
            int safeLen = utf8SafeLength(bytes, bytes.length);
            // 转换为字符串
            String content = new String(bytes, 0, safeLen, StandardCharsets.UTF_8);

            // 计算下一个偏移量
            long nextOffset = effectiveOffset + safeLen;
            // 判断是否到达文件末尾
            boolean eof = nextOffset >= fileSize;

            // 构建结果对象
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

            // 如果使用了游标且偏移量超出文件大小，标记游标重置
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

    /**
     * 游标请求的内部类
     * 封装了从游标或参数解析出的请求信息
     */
    private static final class CursorRequest {
        /** 文件路径 */
        private final String path;
        /** 字节偏移量 */
        private final long offset;
        /** 是否使用了游标 */
        private final boolean cursorUsed;

        /**
         * 创建游标请求
         *
         * @param path      文件路径
         * @param offset    字节偏移量
         * @param cursorUsed 是否使用了游标
         */
        private CursorRequest(String path, long offset, boolean cursorUsed) {
            this.path = path;
            this.offset = offset;
            this.cursorUsed = cursorUsed;
        }
    }

    /**
     * 解析游标或参数
     * 如果提供了游标，则从游标中解析路径和偏移量
     * 否则从参数中获取路径和偏移量
     *
     * @param path   文件路径
     * @param cursor 游标字符串
     * @param offset 字节偏移量
     * @return 解析后的游标请求对象
     * @throws IllegalArgumentException 如果参数无效
     */
    private CursorRequest parseCursorOrArgs(String path, String cursor, Long offset) {
        // 优先处理游标
        if (cursor != null && !cursor.trim().isEmpty()) {
            CursorValue decoded = decodeCursor(cursor.trim());
            return new CursorRequest(decoded.path, decoded.offset, true);
        }
        // 如果没有游标，必须提供路径
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("必须提供 path 或 cursor");
        }
        // 验证偏移量不能为负数
        if (offset != null && offset < 0) {
            throw new IllegalArgumentException("offset 不允许为负数");
        }
        // 使用提供的偏移量或默认为 0
        long resolvedOffset = (offset != null) ? offset : 0L;
        return new CursorRequest(path.trim(), resolvedOffset, false);
    }

    /**
     * 游标值的内部类
     * 封装了从游标中解析出的路径和偏移量信息
     */
    private static final class CursorValue {
        /** 文件路径 */
        private final String path;
        /** 字节偏移量 */
        private final long offset;

        /**
         * 创建游标值
         *
         * @param path   文件路径
         * @param offset 字节偏移量
         */
        private CursorValue(String path, long offset) {
            this.path = path;
            this.offset = offset;
        }
    }

    /**
     * 解码游标字符串
     * 游标格式为 Base64 编码的 JSON，包含版本号、路径和偏移量
     *
     * @param cursor Base64 编码的游标字符串
     * @return 解码后的游标值对象
     * @throws IllegalArgumentException 如果游标格式无效
     */
    private CursorValue decodeCursor(String cursor) {
        try {
            // Base64 解码
            byte[] jsonBytes = Base64.getUrlDecoder().decode(cursor);
            String json = new String(jsonBytes, StandardCharsets.UTF_8);

            // 解析 JSON
            Map<String, Object> map = JsonParser.fromJson(json, new TypeReference<Map<String, Object>>() {});
            Object pathObj = map.get("path");
            Object offsetObj = map.get("offset");

            // 验证路径字段
            if (!(pathObj instanceof String) || ((String) pathObj).trim().isEmpty()) {
                throw new IllegalArgumentException("cursor 缺少 path");
            }
            // 验证偏移量字段
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

    /**
     * 编码游标
     * 将路径和偏移量编码为 Base64 格式的 JSON 字符串
     *
     * @param path   文件路径
     * @param offset 字节偏移量
     * @return Base64 编码的游标字符串
     */
    private String encodeCursor(String path, long offset) {
        Map<String, Object> cursor = new LinkedHashMap<>();
        cursor.put("v", 1);  // 版本号
        cursor.put("path", path);
        cursor.put("offset", offset);
        String json = JsonParser.toJson(cursor);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 加载允许的根目录列表
     * 从环境变量中读取配置的目录，并添加默认目录
     *
     * @return 允许的根目录列表
     */
    private List<Path> loadAllowedRoots() {
        String config = System.getenv(ALLOWED_DIRS_ENV);

        List<Path> roots = new ArrayList<>();
        // 处理环境变量配置的目录
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

        // 添加默认目录：arthas-output
        try {
            Path defaultRoot = Paths.get("arthas-output").toAbsolutePath().normalize();
            if (Files.isDirectory(defaultRoot)) {
                roots.add(defaultRoot.toRealPath());
            }
        } catch (Exception e) {
            logger.debug("viewfile default root ignored: arthas-output", e);
        }

        // 添加默认目录：~/logs/
        try {
            Path userLogsRoot = Paths.get(System.getProperty("user.home"), "logs").toAbsolutePath().normalize();
            if (Files.isDirectory(userLogsRoot)) {
                roots.add(userLogsRoot.toRealPath());
            }
        } catch (Exception e) {
            logger.debug("viewfile default root ignored: ~/logs/", e);
        }

        // 去重并返回
        return deduplicate(roots);
    }

    /**
     * 对路径列表去重
     * 使用 LinkedHashSet 保持插入顺序并去除重复项
     *
     * @param roots 原始路径列表
     * @return 去重后的路径列表
     */
    private static List<Path> deduplicate(List<Path> roots) {
        if (roots == null || roots.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<Path> set = new LinkedHashSet<>(roots);
        return new ArrayList<>(set);
    }

    /**
     * 解析允许访问的文件路径
     * 确保文件在允许的根目录白名单内
     *
     * @param requestedPath 请求的文件路径
     * @param allowedRoots  允许的根目录列表
     * @return 解析后的文件路径
     * @throws Exception 如果文件不存在或不在允许目录内
     */
    private Path resolveAllowedFile(String requestedPath, List<Path> allowedRoots) throws Exception {
        Path req = Paths.get(requestedPath);
        // 处理绝对路径
        if (req.isAbsolute()) {
            Path real = req.toRealPath();
            assertRegularFile(real);
            if (!isUnderAllowedRoot(real, allowedRoots)) {
                throw new IllegalArgumentException("文件不在允许目录白名单内: " + requestedPath);
            }
            return real;
        }

        // 处理相对路径，在所有允许的根目录中查找
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

    /**
     * 断言路径是普通文件
     *
     * @param file 要检查的文件路径
     * @throws IllegalArgumentException 如果不是普通文件
     */
    private static void assertRegularFile(Path file) {
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("不是普通文件: " + file);
        }
    }

    /**
     * 检查文件是否在允许的根目录下
     *
     * @param file         要检查的文件路径
     * @param allowedRoots 允许的根目录列表
     * @return 如果在允许目录内返回 {@code true}，否则返回 {@code false}
     */
    private static boolean isUnderAllowedRoot(Path file, List<Path> allowedRoots) {
        for (Path root : allowedRoots) {
            if (file.startsWith(root)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 限制最大读取字节数在允许范围内
     *
     * @param maxBytes 请求的最大字节数
     * @return 限制后的字节数
     */
    private static int clampMaxBytes(Integer maxBytes) {
        int value = (maxBytes != null && maxBytes > 0) ? maxBytes : DEFAULT_MAX_BYTES;
        return Math.min(value, MAX_MAX_BYTES);
    }

    /**
     * 调整偏移量确保在有效范围内
     * 如果使用了游标且偏移量超出文件大小，则重置为 0
     * 否则限制在文件大小范围内
     *
     * @param cursorUsed     是否使用了游标
     * @param requestedOffset 请求的偏移量
     * @param fileSize       文件大小
     * @return 调整后的偏移量
     * @throws IllegalArgumentException 如果偏移量为负数
     */
    private static long adjustOffset(boolean cursorUsed, long requestedOffset, long fileSize) {
        if (requestedOffset < 0) {
            throw new IllegalArgumentException("offset 不允许为负数");
        }
        if (requestedOffset <= fileSize) {
            return requestedOffset;
        }
        // 如果使用了游标且超出文件大小，重置为 0；否则限制在文件末尾
        return cursorUsed ? 0L : fileSize;
    }

    /**
     * 从文件中读取指定偏移量和长度的字节数据
     *
     * @param file     要读取的文件
     * @param offset   起始偏移量
     * @param maxBytes 最大读取字节数
     * @param fileSize 文件大小
     * @return 读取的字节数组
     * @throws Exception 如果读取失败
     */
    private static byte[] readBytes(Path file, long offset, int maxBytes, long fileSize) throws Exception {
        // 检查偏移量是否有效
        if (offset < 0 || offset > fileSize) {
            return new byte[0];
        }
        // 计算实际要读取的字节数
        long remaining = fileSize - offset;
        int toRead = (int) Math.min(maxBytes, Math.max(0, remaining));
        if (toRead <= 0) {
            return new byte[0];
        }

        // 使用 RandomAccessFile 进行随机访问读取
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
     * 计算 UTF-8 安全长度，避免多字节字符被截断
     * UTF-8 字符可能由 1-4 个字节组成，如果在字符中间截断会导致显示乱码（）
     * 该方法会检查末尾是否是不完整的 UTF-8 字符，如果是则调整长度
     *
     * @param bytes  字节数组
     * @param length 请求的长度
     * @return UTF-8 安全的长度
     */
    static int utf8SafeLength(byte[] bytes, int length) {
        if (bytes == null || length <= 0) {
            return 0;
        }
        // 检查最后一个字节
        int lastIndex = length - 1;
        int lastByte = bytes[lastIndex] & 0xFF;
        // 如果最高位为 0，说明是 ASCII 字符（单字节），直接返回
        if ((lastByte & 0x80) == 0) {
            return length;
        }

        // 向前查找 UTF-8 字符的起始字节和连续字节
        int i = lastIndex;
        int continuation = 0;
        while (i >= 0 && (bytes[i] & 0xC0) == 0x80) {
            continuation++;
            i--;
        }
        // 如果所有字节都是连续字节，说明整个序列无效
        if (i < 0) {
            return Math.max(0, length - continuation);
        }

        // 检查起始字节，判断字符应该有几个字节
        int lead = bytes[i] & 0xFF;
        int expectedLen;
        if ((lead & 0xE0) == 0xC0) {
            // 2 字节字符：110xxxxx
            expectedLen = 2;
        } else if ((lead & 0xF0) == 0xE0) {
            // 3 字节字符：1110xxxx
            expectedLen = 3;
        } else if ((lead & 0xF8) == 0xF0) {
            // 4 字节字符：11110xxx
            expectedLen = 4;
        } else {
            // 无效的 UTF-8 起始字节
            return length;
        }

        // 检查实际字节数是否足够
        int actualLen = continuation + 1;
        if (actualLen < expectedLen) {
            // 字符不完整，截断到字符之前
            return i;
        }
        // 字符完整，可以安全返回
        return length;
    }
}
