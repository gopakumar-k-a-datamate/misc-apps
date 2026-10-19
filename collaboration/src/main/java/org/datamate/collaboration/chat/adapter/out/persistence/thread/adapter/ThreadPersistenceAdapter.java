package org.datamate.collaboration.chat.adapter.out.persistence.thread.adapter;

import lombok.RequiredArgsConstructor;
import org.datamate.collaboration.chat.adapter.out.persistence.thread.entity.ThreadJpaEntity;
import org.datamate.collaboration.chat.adapter.out.persistence.thread.repository.ThreadJpaRepository;
import org.datamate.collaboration.chat.application.port.out.ThreadRepositoryPort;
import org.datamate.collaboration.chat.domain.model.Thread;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Outgoing Adapter implementing {@link ThreadRepositoryPort} using JPA.
 * <p>
 * Responsible for mapping between the pure domain {@link Thread} and
 * the JPA-managed {@link ThreadJpaEntity}. This adapter is the only
 * class allowed to touch JPA entities for the Thread aggregate.
 */
@Component
@RequiredArgsConstructor
public class ThreadPersistenceAdapter implements ThreadRepositoryPort {

    private final ThreadJpaRepository repository;

    @Override
    public Thread save(Thread thread) {
        ThreadJpaEntity entity = new ThreadJpaEntity(thread.getId());
        repository.save(entity);
        return thread;
    }

    @Override
    public Optional<Thread> findById(UUID id) {
        return repository.findById(id)
                .map(entity -> new Thread(entity.getId()));
    }

    @Override
    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }
}
