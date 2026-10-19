package org.datamate.collaboration.chat.adapter.out.persistence.message.repository;

import org.datamate.collaboration.chat.adapter.out.persistence.message.entity.MessageJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link MessageJpaEntity}.
 * <p>
 * Queries by raw {@code threadId} UUID — no entity joins involved,
 * so paginated queries execute as a single efficient SQL statement.
 */
public interface MessageJpaRepository extends JpaRepository<MessageJpaEntity, UUID> {

    Page<MessageJpaEntity> findByThreadIdOrderByTimestampDesc(UUID threadId, Pageable pageable);
}
