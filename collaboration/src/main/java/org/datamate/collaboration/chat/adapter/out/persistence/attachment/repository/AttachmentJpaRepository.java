package org.datamate.collaboration.chat.adapter.out.persistence.attachment.repository;

import org.datamate.collaboration.chat.adapter.out.persistence.attachment.entity.AttachmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link AttachmentJpaEntity}.
 */
@Repository
public interface AttachmentJpaRepository extends JpaRepository<AttachmentJpaEntity, UUID> {
}
