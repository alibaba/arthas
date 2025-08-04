package org.example.jfranalyzerbackend.entity.shared.user;

import jakarta.persistence.*;
import org.example.jfranalyzerbackend.entity.shared.BaseEntity;

@Entity
@Table(name = "external_login_data",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"method", "provider", "principalName"})
        })
public class ExternalLoginDataEntity extends BaseEntity {

    @OneToOne(optional = false)
    private UserEntity user;

//    @Column(nullable = false, updatable = false)
//    @Enumerated(EnumType.STRING)
//    private ExternalLoginMethod method;

    @Column(nullable = false, updatable = false, length = 32)
    private String provider;

    @Column(nullable = false, updatable = false, length = 64)
    private String principalName;

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getPrincipalName() {
        return principalName;
    }

    public void setPrincipalName(String principalName) {
        this.principalName = principalName;
    }
}
