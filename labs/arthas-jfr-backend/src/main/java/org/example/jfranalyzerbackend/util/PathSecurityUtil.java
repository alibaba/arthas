package org.example.jfranalyzerbackend.util;

import org.example.jfranalyzerbackend.exception.CommonException;
import org.example.jfranalyzerbackend.enums.ServerErrorCode;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * 路径安全工具类
 * 提供安全的路径操作，防止路径遍历攻击
 */
public class PathSecurityUtil {
    
    // 允许的文件名模式：只允许字母、数字、点、连字符、下划线
    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");
    
    // 禁止的路径模式：包含 .. 或 \.. 或 /.. 等路径遍历字符
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(".*\\.\\..*|.*\\\\\\.\\..*|.*/\\.\\..*");
    
    /**
     * 安全地构建文件路径，防止路径遍历攻击
     * 
     * @param basePath 基础路径
     * @param filename 文件名
     * @return 安全的文件路径
     * @throws CommonException 如果路径不安全
     */
    public static Path buildSafePath(Path basePath, String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new CommonException(ServerErrorCode.INVALID_FILENAME, "文件名不能为空");
        }
        
        // 清理文件名，移除危险字符
        String sanitizedFilename = sanitizeFilename(filename);
        
        // 验证文件名是否安全
        if (!isSafeFilename(sanitizedFilename)) {
            throw new CommonException(ServerErrorCode.INVALID_FILENAME, 
                "文件名包含非法字符: " + sanitizedFilename);
        }
        
        // 检查是否包含路径遍历字符
        if (containsPathTraversal(sanitizedFilename)) {
            throw new CommonException(ServerErrorCode.INVALID_FILENAME, 
                "文件名包含路径遍历字符: " + sanitizedFilename);
        }
        
        // 构建路径
        Path targetPath = basePath.resolve(sanitizedFilename);
        
        // 确保目标路径在基础路径内
        if (!targetPath.normalize().startsWith(basePath.normalize())) {
            throw new CommonException(ServerErrorCode.INVALID_FILENAME, 
                "文件路径超出允许范围");
        }
        
        return targetPath;
    }
    
    /**
     * 验证文件路径是否安全
     * 
     * @param path 要验证的路径
     * @param allowedBasePath 允许的基础路径
     * @return 是否安全
     */
    public static boolean isPathSafe(Path path, Path allowedBasePath) {
        if (path == null || allowedBasePath == null) {
            return false;
        }
        
        try {
            Path normalizedPath = path.normalize();
            Path normalizedBasePath = allowedBasePath.normalize();
            
            return normalizedPath.startsWith(normalizedBasePath);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 清理文件名，移除危险字符
     * 
     * @param filename 原始文件名
     * @return 清理后的文件名
     */
    private static String sanitizeFilename(String filename) {
        if (filename == null) {
            return "";
        }
        
        // 移除路径分隔符和危险字符
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_")
                      .replaceAll("\\s+", "_")
                      .trim();
    }
    
    /**
     * 检查文件名是否安全
     * 
     * @param filename 文件名
     * @return 是否安全
     */
    private static boolean isSafeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return false;
        }
        
        // 检查长度
        if (filename.length() > 255) {
            return false;
        }
        
        // 检查是否匹配安全模式
        return SAFE_FILENAME_PATTERN.matcher(filename).matches();
    }
    
    /**
     * 检查是否包含路径遍历字符
     * 
     * @param filename 文件名
     * @return 是否包含路径遍历字符
     */
    private static boolean containsPathTraversal(String filename) {
        if (filename == null) {
            return false;
        }
        
        return PATH_TRAVERSAL_PATTERN.matcher(filename).matches();
    }
    
    /**
     * 验证JFR文件路径是否安全
     * 
     * @param filePath 文件路径
     * @return 是否安全
     */
    public static boolean isValidJFRFilePath(Path filePath) {
        if (filePath == null) {
            return false;
        }
        
        try {
            // 检查文件是否存在且为常规文件
            if (!java.nio.file.Files.exists(filePath) || !java.nio.file.Files.isRegularFile(filePath)) {
                return false;
            }
            
            // 检查文件扩展名
            String filename = filePath.getFileName().toString();
            if (!filename.toLowerCase().endsWith(".jfr")) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
