package org.datamate.collaboration.chat.application.port.out;

import org.datamate.collaboration.chat.domain.model.Attachment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttachmentRepositoryPort {
    Attachment save(Attachment attachment);
    Optional<Attachment> findById(UUID id);
    List<Attachment> findAllById(List<UUID> ids);
}
