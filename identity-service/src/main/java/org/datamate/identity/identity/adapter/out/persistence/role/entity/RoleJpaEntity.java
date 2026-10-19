package org.datamate.identity.identity.adapter.out.persistence.role.entity;

import com.datamate.bedrock.framework.common.auditing.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.datamate.identity.identity.domain.model.role.enums.RoleStatus;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

@Entity
@Table(name = "role")
@Audited
@SQLDelete(sql = "UPDATE role SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
public class RoleJpaEntity extends BaseAuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleStatus status;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem = false;

    @Column(name = "reference_system", length = 50)
    private String referenceSystem;

    @Column(name = "reference_value", length = 255)
    private String referenceValue;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "domain_version", nullable = false)
    private Long domainVersion;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_by_id")
    private UUID createdById;

    @Column(name = "created_by_system", length = 50)
    private String createdBySystem;

    @Column(name = "created_by_value", length = 255)
    private String createdByValue;

    @Column(name = "last_modified_by_id")
    private UUID lastModifiedById;

    @Column(name = "last_modified_by_system", length = 50)
    private String lastModifiedBySystem;

    @Column(name = "last_modified_by_value", length = 255)
    private String lastModifiedByValue;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoleJpaEntity that = (RoleJpaEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : getClass().hashCode();
    }
}
