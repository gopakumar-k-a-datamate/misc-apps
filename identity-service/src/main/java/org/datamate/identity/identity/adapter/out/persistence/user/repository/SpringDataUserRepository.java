package org.datamate.identity.identity.adapter.out.persistence.user.repository;

import org.datamate.identity.identity.adapter.out.persistence.user.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, UUID>, JpaSpecificationExecutor<UserJpaEntity> {
    Optional<UserJpaEntity> findByUserName(String userName);
    Optional<UserJpaEntity> findByUserNameOrEmail(String userName, String email);
    boolean existsByUserName(String userName);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, UUID id);
    boolean existsByUserNameAndIdNot(String userName, UUID id);

    @Query("SELECT COUNT(u) FROM UserJpaEntity u JOIN u.roles r WHERE (r.name = :roleName OR CAST(r.id AS string) = :roleName) AND u.status = org.datamate.identity.identity.domain.model.user.enums.UserStatus.ACTIVE AND u.id != :exceptUserId")
    long countActiveUsersWithRoleExcept(@Param("roleName") String roleName, @Param("exceptUserId") UUID exceptUserId);
}
