package org.datamate.pharmacy.adapter.out.persistence.mapper;

import org.datamate.pharmacy.adapter.out.persistence.entity.DoctorJpaEntity;
import org.datamate.pharmacy.domain.model.Doctor;

/**
 * Anti-Corruption Layer (ACL) Mapper.
 * Maps between the pure domain model {@link Doctor} and the JPA entity {@link DoctorJpaEntity}.
 * Includes mapping for BaseAuditableEntity audit fields.
 * Ensures that the application layer works purely with Domain objects.
 */
public class DoctorJpaMapper {

    private DoctorJpaMapper() {
        // utility class
    }

    public static Doctor toDomain(DoctorJpaEntity entity) {
        Doctor doctor = new Doctor(
                entity.getId(),
                entity.getName(),
                entity.getDepartment(),
                entity.isActive()
        );
        doctor.setCreatedDate(entity.getCreatedDate());
        doctor.setLastModifiedDate(entity.getLastModifiedDate());
        doctor.setCreatedBy(entity.getCreatedBy());
        doctor.setLastModifiedBy(entity.getLastModifiedBy());
        return doctor;
    }

    public static DoctorJpaEntity toJpaEntity(Doctor domain) {
        DoctorJpaEntity entity = new DoctorJpaEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setDepartment(domain.getDepartment());
        entity.setActive(domain.isActive());
        // Audit fields (createdDate, lastModifiedDate, etc.) are managed
        // by BaseAuditableEntity's @PrePersist/@PreUpdate callbacks
        return entity;
    }
}
