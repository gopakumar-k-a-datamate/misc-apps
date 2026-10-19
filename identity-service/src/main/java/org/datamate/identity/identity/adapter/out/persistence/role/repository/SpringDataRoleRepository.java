package org.datamate.identity.identity.adapter.out.persistence.role.repository;

import org.datamate.identity.identity.adapter.out.persistence.role.entity.RoleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataRoleRepository extends JpaRepository<RoleJpaEntity, UUID>, JpaSpecificationExecutor<RoleJpaEntity> {
    boolean existsByName(String name);
    Optional<RoleJpaEntity> findByName(String name);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
    List<RoleJpaEntity> findAllByNameIn(Collection<String> names);

    @Query(value = "SELECT r.name FROM role r JOIN user_roles ur ON r.id = ur.role_id WHERE ur.user_id = :userId", nativeQuery = true)
    List<String> findRoleNamesByUserId(@Param("userId") UUID userId);
}
