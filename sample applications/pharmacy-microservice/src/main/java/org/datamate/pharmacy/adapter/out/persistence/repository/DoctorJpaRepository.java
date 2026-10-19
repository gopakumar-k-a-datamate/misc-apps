package org.datamate.pharmacy.adapter.out.persistence.repository;

import org.datamate.pharmacy.adapter.out.persistence.entity.DoctorJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DoctorJpaRepository extends JpaRepository<DoctorJpaEntity, String> {
    Page<DoctorJpaEntity> findByActiveTrue(Pageable pageable);
    Page<DoctorJpaEntity> findByActiveTrueAndNameContainingIgnoreCase(String search, Pageable pageable);
}
