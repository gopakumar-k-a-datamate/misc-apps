package org.datamate.collaboration.chat.application.port.out;

import org.datamate.collaboration.chat.domain.model.Thread;

import java.util.Optional;
import java.util.UUID;

/**
 * Outgoing Port for Thread persistence.
 * <p>
 * Defines what the application layer needs from the persistence infrastructure
 * for managing Thread records. Dependency Inversion ensures the domain
 * and application layers never know about JPA.
 */
public interface ThreadRepositoryPort {

    Thread save(Thread thread);

    Optional<Thread> findById(UUID id);

    boolean existsById(UUID id);
}
