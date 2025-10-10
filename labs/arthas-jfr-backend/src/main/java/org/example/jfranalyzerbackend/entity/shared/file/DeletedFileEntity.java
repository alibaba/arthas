package org.example.jfranalyzerbackend.entity.shared.file;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@SuppressWarnings("JpaDataSourceORMInspection")
@Table(name = "deleted_files")
@Entity
public class DeletedFileEntity extends BaseFileEntity {

    @Column(nullable = false, updatable = false)
    private long size;

    @Column(nullable = false, updatable = false)
    private LocalDateTime originalCreatedTime;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public LocalDateTime getOriginalCreatedTime() {
        return originalCreatedTime;
    }

    public void setOriginalCreatedTime(LocalDateTime originalCreatedTime) {
        this.originalCreatedTime = originalCreatedTime;
    }
}
