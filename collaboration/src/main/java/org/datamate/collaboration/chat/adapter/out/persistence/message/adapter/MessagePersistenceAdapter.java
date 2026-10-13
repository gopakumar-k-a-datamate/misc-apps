package org.datamate.collaboration.chat.adapter.out.persistence.message.adapter;

import lombok.RequiredArgsConstructor;
import org.datamate.collaboration.chat.adapter.out.persistence.message.entity.MessageJpaEntity;
import org.datamate.collaboration.chat.adapter.out.persistence.message.repository.MessageJpaRepository;
import org.datamate.collaboration.chat.application.port.out.MessageRepositoryPort;
import org.datamate.collaboration.chat.domain.model.Message;
import com.datamate.bedrock.framework.common.pagination.PageQuery;
import com.datamate.bedrock.framework.common.pagination.Paged;
import com.datamate.bedrock.framework.common.pagination.PaginationHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Outgoing Adapter implementing {@link MessageRepositoryPort} using JPA.
 * <p>
 * Handles bidirectional mapping between the pure domain {@link Message}
 * and the JPA-managed {@link MessageJpaEntity}.
 */
@Component
@RequiredArgsConstructor
public class MessagePersistenceAdapter implements MessageRepositoryPort {

    private final MessageJpaRepository repository;

    @Override
    public Message save(Message message) {
        MessageJpaEntity entity = toEntity(message);
        repository.save(entity);
        return message;
    }

    @Override
    public Paged<Message> findByThreadId(UUID threadId, PageQuery query) {
        int zeroBasedPage = PaginationHelper.toZeroBasedPage(query.page());
        int limit = PaginationHelper.validateLimit(query.size());

        PageRequest pageRequest = PageRequest.of(zeroBasedPage, limit);

        Page<MessageJpaEntity> springPage = repository.findByThreadIdOrderByTimestampDesc(threadId, pageRequest);

        List<Message> domainList = springPage.getContent().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());

        return new Paged<>(
                domainList,
                PaginationHelper.toOneIndexed(springPage.getNumber()),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages(),
                springPage.hasNext(),
                springPage.hasPrevious()
        );
    }

    private MessageJpaEntity toEntity(Message message) {
        MessageJpaEntity entity = new MessageJpaEntity();
        entity.setId(message.getId());
        entity.setThreadId(message.getThreadId());
        entity.setSenderId(message.getSenderId());
        entity.setText(message.getText());
        entity.setFile(message.isFile());
        entity.setSystemMessage(message.isSystemMessage());
        List<UUID> attachmentIds = message.getAttachmentIds() != null
                ? new java.util.ArrayList<>(message.getAttachmentIds())
                : new java.util.ArrayList<>();
        entity.setAttachmentIds(attachmentIds);
        entity.setAttachmentId(!attachmentIds.isEmpty() ? attachmentIds.get(0) : message.getAttachmentId());
        entity.setParentId(message.getParentId());
        entity.setTimestamp(message.getTimestamp());
        return entity;
    }

    private Message toDomain(MessageJpaEntity entity) {
        List<UUID> attachmentIds = entity.getAttachmentIds() != null && !entity.getAttachmentIds().isEmpty()
                ? entity.getAttachmentIds()
                : (entity.getAttachmentId() != null ? List.of(entity.getAttachmentId()) : List.of());

        return Message.restore(
                entity.getId(),
                entity.getThreadId(),
                entity.getSenderId(),
                entity.getText(),
                entity.isFile(),
                entity.isSystemMessage(),
                attachmentIds,
                entity.getParentId(),
                entity.getTimestamp()
        );
    }
}


