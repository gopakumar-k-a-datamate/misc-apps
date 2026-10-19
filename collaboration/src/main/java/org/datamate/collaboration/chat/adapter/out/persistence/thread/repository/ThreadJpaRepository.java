package org.datamate.collaboration.chat.adapter.out.persistence.thread.repository;

import org.datamate.collaboration.chat.adapter.out.persistence.thread.entity.ThreadJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ThreadJpaEntity}.
 */
public interface ThreadJpaRepository extends JpaRepository<ThreadJpaEntity, UUID> {
}
