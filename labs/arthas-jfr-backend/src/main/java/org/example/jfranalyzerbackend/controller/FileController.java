package org.example.jfranalyzerbackend.controller;

import org.example.jfranalyzerbackend.config.Result;
import org.example.jfranalyzerbackend.dto.FileView;
import org.example.jfranalyzerbackend.enums.FileType;
import org.example.jfranalyzerbackend.service.FileService;
import org.example.jfranalyzerbackend.vo.PageView;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class FileController {

    private final FileService fileService;

    /**
     * @param fileService file service
     */
    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * Query the files of the current user by type and paging information.
     *
     * @param type     the expected file type
     * @param page     the page number, starts from 1
     * @param pageSize the page size
     * @return the page view of the files
     */
    @GetMapping("/files")
    public Result<PageView<FileView>> queryFiles(@RequestParam(required = false) FileType type,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int pageSize) {
        System.out.println("文件查询");
        PageView<FileView> pageView = fileService.getUserFileViews(type, page, pageSize);
        return Result.success(pageView);
    }

    /**
     * Upload a file
     *
     * @param type the file type
     * @param file the file
     * @return the file id
     * @throws Throwable the exception
     */
    @PostMapping(value = "/files/upload")
    public Result<Long> upload(@RequestParam FileType type,
                       @RequestParam MultipartFile file) throws Throwable {
        System.out.println("文件上传");
        long fileId = fileService.handleUploadRequest(type, file);
        return Result.success(fileId);
    }

    /**
     * Delete the file by id.
     *
     * @param fileId the file id
     */
    @DeleteMapping("/files/{file-id}")
    public Result<Void> deleteFile(@PathVariable("file-id") long fileId) {
        fileService.deleteById(fileId);
        return Result.success();
    }




}
