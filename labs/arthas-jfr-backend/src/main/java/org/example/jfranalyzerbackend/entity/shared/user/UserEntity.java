
package org.example.jfranalyzerbackend.entity.shared.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.example.jfranalyzerbackend.entity.shared.BaseEntity;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class UserEntity extends BaseEntity {

    @Column(length = 64)
    private String name;

    @Column(nullable = false)
    private boolean admin;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime lastModifiedTime;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public LocalDateTime getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(LocalDateTime lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }
}
