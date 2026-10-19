package org.datamate.collaboration.chat.adapter.out.persistence.attachment.adapter;

import lombok.RequiredArgsConstructor;
import org.datamate.collaboration.chat.adapter.out.persistence.attachment.entity.AttachmentJpaEntity;
import org.datamate.collaboration.chat.adapter.out.persistence.attachment.repository.AttachmentJpaRepository;
import org.datamate.collaboration.chat.application.port.out.AttachmentRepositoryPort;
import org.datamate.collaboration.chat.domain.model.Attachment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AttachmentPersistenceAdapter implements AttachmentRepositoryPort {

    private final AttachmentJpaRepository repository;

    @Override
    public Attachment save(Attachment attachment) {
        AttachmentJpaEntity entity = toEntity(attachment);
        repository.save(entity);
        return attachment;
    }

    @Override
    public Optional<Attachment> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Attachment> findAllById(List<UUID> ids) {
        return repository.findAllById(ids).stream().map(this::toDomain).toList();
    }

    private AttachmentJpaEntity toEntity(Attachment attachment) {
        AttachmentJpaEntity entity = new AttachmentJpaEntity();
        entity.setId(attachment.getId());
        entity.setFileName(attachment.getFileName());
        entity.setMimeType(attachment.getMimeType());
        entity.setFileSize(attachment.getFileSize());
        entity.setUploadUrl(attachment.getUploadUrl());
        entity.setPreviewUrl(attachment.getPreviewUrl());
        return entity;
    }

    private Attachment toDomain(AttachmentJpaEntity entity) {
        return Attachment.restore(
                entity.getId(),
                entity.getFileName(),
                entity.getMimeType(),
                entity.getFileSize(),
                entity.getUploadUrl(),
                entity.getPreviewUrl()
        );
    }
}
