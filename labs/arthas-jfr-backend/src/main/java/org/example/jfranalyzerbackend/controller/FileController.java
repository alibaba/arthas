package org.example.jfranalyzerbackend.controller;

import org.example.jfranalyzerbackend.config.Result;
import org.example.jfranalyzerbackend.dto.FileView;
import org.example.jfranalyzerbackend.enums.FileType;
import org.example.jfranalyzerbackend.service.FileService;
import org.example.jfranalyzerbackend.vo.PageView;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件管理控制器
 * 提供文件上传、下载、删除和查询等REST API接口
 */
@RestController
public class FileController {

    private final FileService fileManagementService;

    /**
     * 构造函数注入文件服务
     * @param fileManagementService 文件管理服务
     */
    public FileController(FileService fileManagementService) {
        this.fileManagementService = fileManagementService;
    }

    /**
     * 根据类型和分页信息查询当前用户的文件
     *
     * @param fileType 期望的文件类型
     * @param pageNumber 页码，从1开始
     * @param pageSize 页面大小
     * @return 文件的分页视图
     */
    @GetMapping("/files")
    public Result<PageView<FileView>> retrieveUserFiles(@RequestParam(required = false) FileType fileType,
                                         @RequestParam(defaultValue = "1") int pageNumber,
                                         @RequestParam(defaultValue = "10") int pageSize) {
        System.out.println("文件查询");
        PageView<FileView> pageView = fileManagementService.retrieveUserFileViews(fileType, pageNumber, pageSize);
        return Result.success(pageView);
    }

    /**
     * 上传文件
     *
     * @param fileType 文件类型
     * @param uploadedFile 上传的文件
     * @return 文件ID
     * @throws Throwable 异常
     */
    @PostMapping(value = "/files/upload")
    public Result<Long> processFileUpload(@RequestParam FileType fileType,
                       @RequestParam MultipartFile uploadedFile) throws Throwable {
        System.out.println("文件上传");
        long fileId = fileManagementService.processFileUpload(fileType, uploadedFile);
        return Result.success(fileId);
    }

    /**
     * 根据ID删除文件
     *
     * @param fileId 文件ID
     */
    @DeleteMapping("/files/{file-id}")
    public Result<Void> removeFile(@PathVariable("file-id") long fileId) {
        fileManagementService.removeFileById(fileId);
        return Result.success();
    }

    // 向后兼容的方法 - 使用不同的路径避免冲突
    @GetMapping("/files/legacy")
    public Result<PageView<FileView>> queryFiles(@RequestParam(required = false) FileType type,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int pageSize) {
        return retrieveUserFiles(type, page, pageSize);
    }

    @PostMapping(value = "/files/upload/legacy")
    public Result<Long> upload(@RequestParam FileType type,
                       @RequestParam MultipartFile file) throws Throwable {
        return processFileUpload(type, file);
    }

    @DeleteMapping("/files/legacy/{file-id}")
    public Result<Void> deleteFile(@PathVariable("file-id") long fileId) {
        return removeFile(fileId);
    }
}
