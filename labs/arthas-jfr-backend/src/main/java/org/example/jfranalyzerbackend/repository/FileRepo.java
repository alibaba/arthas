package org.example.jfranalyzerbackend.repository;

import org.example.jfranalyzerbackend.entity.shared.file.FileEntity;
import org.example.jfranalyzerbackend.enums.FileType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepo extends JpaRepository<FileEntity, Long> {

    Page<FileEntity> findByUserIdOrderByCreatedTimeDesc(Long userId, Pageable pageable);

    Page<FileEntity> findByUserIdAndTypeOrderByCreatedTimeDesc(Long userId, FileType type, Pageable pageable);
}
