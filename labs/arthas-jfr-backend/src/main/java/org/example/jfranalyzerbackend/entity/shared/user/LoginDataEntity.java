package org.example.jfranalyzerbackend.entity.shared.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.example.jfranalyzerbackend.entity.shared.BaseEntity;

@SuppressWarnings("JpaDataSourceORMInspection")
@Entity
@Table(name = "login_data")
public class LoginDataEntity extends BaseEntity {

    @OneToOne(optional = false)
    private UserEntity user;

    @Column(unique = true, nullable = false, updatable = false, length = 128)
    private String username;

    @Column(length = 64)
    private String passwordHash;

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
