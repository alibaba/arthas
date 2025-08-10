
package org.example.jfranalyzerbackend.entity.shared.file;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@SuppressWarnings("JpaDataSourceORMInspection")
@Table(name = "files")
@Entity
public class FileEntity extends BaseFileEntity {

    @Column(nullable = false, updatable = false)
    private long size;

    //TODO:之后修改UserID
    @Column(nullable = false, updatable = false)
    private Long userId;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
