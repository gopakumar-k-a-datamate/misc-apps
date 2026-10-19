package org.datamate.collaboration.chat.domain.model;

import com.datamate.bedrock.framework.common.ddd.domain.AggregateRoot;
import lombok.Getter;
import org.datamate.collaboration.exception.CollaborationErrorCodes;
import org.datamate.collaboration.exception.DomainValidationException;

import java.util.UUID;

/**
 * Thread Aggregate Root.
 * <p>
 * Represents a conversation thread. The {@code id} is always generated externally
 * by Domain Services (e.g., EMR, Finance), enabling cross-domain chat inheritance
 * when domain entities transform (e.g., Purchase Request → Purchase Order).
 * <p>
 * Extends Bedrock Framework's {@link AggregateRoot} to support domain events.
 * Pure Domain Entity — no Spring, JPA, or framework annotations.
 */
@Getter
public class Thread extends AggregateRoot {

    private final UUID id;

    public Thread(UUID id) {
        super();
        if (id == null) {
            throw new DomainValidationException(
                    CollaborationErrorCodes.REQUIRED_FIELD_MISSING.code(), "threadId");
        }
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Thread that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
