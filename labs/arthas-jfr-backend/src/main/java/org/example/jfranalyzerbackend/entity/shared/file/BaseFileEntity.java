
package org.example.jfranalyzerbackend.entity.shared.file;

import jakarta.persistence.*;
import org.example.jfranalyzerbackend.entity.shared.BaseEntity;
import org.example.jfranalyzerbackend.enums.FileType;

@MappedSuperclass
public class BaseFileEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false, length = 64)
    private String uniqueName;

//    @ManyToOne(fetch = FetchType.LAZY)
//    private UserEntity user;

    @Column(nullable = false, updatable = false, length = 256)
    private String originalName;

    @Column(nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private FileType type;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getUniqueName() {
        return uniqueName;
    }

    public void setUniqueName(String uniqueName) {
        this.uniqueName = uniqueName;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public FileType getType() {
        return type;
    }

    public void setType(FileType type) {
        this.type = type;
    }
}
