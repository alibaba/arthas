package org.example.jfranalyzerbackend.service;

import org.example.jfranalyzerbackend.dto.FileView;
import org.example.jfranalyzerbackend.enums.FileType;
import org.example.jfranalyzerbackend.vo.PageView;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * 文件服务接口
 * 定义文件管理相关的核心业务方法
 */
public interface FileService {

    /**
     * 检索用户文件视图
     */
    PageView<FileView> retrieveUserFileViews(FileType fileType, int pageNumber, int pageSize);

    /**
     * 处理文件上传请求
     */
    Long processFileUpload(FileType fileType, MultipartFile uploadedFile) throws IOException;

    /**
     * 根据ID删除文件
     */
    void removeFileById(Long fileId);

    /**
     * 根据ID获取文件视图
     */
    FileView retrieveFileViewById(Long fileId);

    /**
     * 根据ID获取文件路径
     */
    String retrieveFilePathById(Long fileId);

    // 向后兼容的方法
    PageView<FileView> getUserFileViews(FileType type, int page, int pageSize);
    Long handleUploadRequest(FileType type, MultipartFile file) throws IOException;
    void deleteById(Long fileId);
    FileView getFileViewById(Long fileId);
    String getFilePathById(Long fileId);
}
