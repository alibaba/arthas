package org.example.jfranalyzerbackend.service.impl;

import jakarta.transaction.Transactional;
import org.example.jfranalyzerbackend.config.ArthasConfig;
import org.example.jfranalyzerbackend.dto.FileView;
import org.example.jfranalyzerbackend.entity.shared.file.DeletedFileEntity;
import org.example.jfranalyzerbackend.entity.shared.file.FileEntity;
import org.example.jfranalyzerbackend.enums.FileType;
import org.example.jfranalyzerbackend.exception.CommonException;
import org.example.jfranalyzerbackend.repository.FileRepo;
import org.example.jfranalyzerbackend.repository.DeletedFileRepo;
import org.example.jfranalyzerbackend.service.FileService;
import org.example.jfranalyzerbackend.service.UserService;
import org.example.jfranalyzerbackend.util.FileViewConverter;
import org.example.jfranalyzerbackend.vo.PageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Component
public class FileServiceImpl implements FileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    @Autowired
    private UserService userService;

    @Autowired
    private FileRepo fileRepo;

    @Autowired
    private ArthasConfig arthasConfig;

    @Autowired
    private DeletedFileRepo deletedFileRepo;


    @Override
    public PageView<FileView> getUserFileViews(FileType type, int page, int pageSize) {
        log.info("getUserFileViews");
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize);
        //Todo: 现在写死状态，之后修改为用户--handshake
        Long userId = userService.getCurrentUserId();

        //获取page实体
        Page<FileEntity> files = (type == null)
                ? fileRepo.findByUserIdOrderByCreatedTimeDesc(userId, pageRequest)
                : fileRepo.findByUserIdAndTypeOrderByCreatedTimeDesc(userId, type, pageRequest);

        List<FileView> views = files.getContent().stream()
                .map(FileViewConverter::convert)
                .toList();

        return new PageView<>(page, pageSize, (int) files.getTotalElements(), views);
    }

    @Override
    @Transactional
    public Long handleUploadRequest(FileType type, MultipartFile file) throws IOException {
        //TODO: 目前不支持分布式存储--不需要支持
        String uniqueName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown.file";
        long size = file.getSize();
        //Todo: 现在写死状态，之后修改为用户--handshake
        Long userId = userService.getCurrentUserId();

//        // 文件保存路径 存在本地
//        Path savePath = Paths.get("upload", uniqueName);
//        Files.createDirectories(savePath.getParent());
//        file.transferTo(savePath.toFile());
        // 使用配置的路径
        Path savePath = Paths.get(arthasConfig.getJfrStoragePath(), uniqueName);
        Files.createDirectories(savePath.getParent());
        file.transferTo(savePath.toFile());

        // 数据库存储
        FileEntity entity = new FileEntity();
        entity.setUniqueName(uniqueName);
        entity.setOriginalName(originalName);
        entity.setType(type);
        entity.setSize(size);
        entity.setUserId(userId);

        return fileRepo.save(entity).getId();
    }

    @Override
    public void deleteById(Long fileId) {
        FileEntity entity = fileRepo.findById(fileId)
                .orElseThrow(() -> new CommonException(org.example.jfranalyzerbackend.enums.ServerErrorCode.FILE_NOT_FOUND));

        if (!entity.getUserId().equals(userService.getCurrentUserId())) {
            throw new CommonException(org.example.jfranalyzerbackend.enums.ServerErrorCode.FILE_NOT_FOUND);
        }

        // 构建删除记录
        DeletedFileEntity deletedFile = new DeletedFileEntity();
        deletedFile.setUniqueName(entity.getUniqueName());
        deletedFile.setOriginalName(entity.getOriginalName());
        deletedFile.setType(entity.getType());
        deletedFile.setSize(entity.getSize());
        deletedFile.setOriginalCreatedTime(entity.getCreatedTime());

        fileRepo.deleteById(fileId);
        deletedFileRepo.save(deletedFile);

        // 清理实际文件（磁盘）
        try {
            Path filePath = Paths.get(arthasConfig.getJfrStoragePath(), entity.getUniqueName());
            Files.deleteIfExists(filePath);
        } catch (Exception e) {
            log.warn("Failed to delete file from disk: {}", e.getMessage());
        }
    }

    @Override
    public FileView getFileViewById(Long fileId) {
        FileEntity entity = fileRepo.findById(fileId)
                .orElse(null);
        
        if (entity == null) {
            return null;
        }
        
        // 检查用户权限
        if (!entity.getUserId().equals(userService.getCurrentUserId())) {
            return null;
        }
        
        return FileViewConverter.convert(entity);
    }

    @Override
    public String getFilePathById(Long fileId) {
        FileEntity entity = fileRepo.findById(fileId)
                .orElseThrow(() -> new CommonException(org.example.jfranalyzerbackend.enums.ServerErrorCode.FILE_NOT_FOUND));
        return Paths.get(arthasConfig.getJfrStoragePath(), entity.getUniqueName()).toString();
    }
}
