package org.datamate.pharmacy.adapter.out.persistence.entity;

import com.datamate.bedrock.framework.common.auditing.entity.BaseAuditableEntity;
import jakarta.persistence.*;

/**
 * JPA Entity for the doctors table.
 * Extends BaseAuditableEntity for automatic audit fields
 * (createdDate, lastModifiedDate, createdBy, lastModifiedBy),
 * aligned with the dental project's entity convention.
 *
 * In Clean Architecture, JPA entities are an infrastructure concern and must
 * not leak into the Domain layer. Therefore, this class lives in the Adapter layer.
 */
@Entity
@Table(name = "doctors")
public class DoctorJpaEntity extends BaseAuditableEntity {

    @Id
    @Column(length = 50)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(length = 100)
    private String department;

    @Column(nullable = false)
    private boolean active = true;

    public DoctorJpaEntity() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
