package org.datamate.collaboration.chat.application.usecase;

import com.datamate.bedrock.framework.common.pagination.PageQuery;
import com.datamate.bedrock.framework.common.pagination.Paged;
import lombok.RequiredArgsConstructor;
import org.datamate.collaboration.chat.application.dto.GetMessagesQuery;
import org.datamate.collaboration.chat.application.dto.MessageDto;
import org.datamate.collaboration.chat.application.mapper.MessageMapper;
import org.datamate.collaboration.chat.application.port.in.GetMessagesUseCase;
import org.datamate.collaboration.chat.application.port.out.AttachmentRepositoryPort;
import org.datamate.collaboration.chat.application.port.out.MessageRepositoryPort;
import org.datamate.collaboration.chat.domain.model.Attachment;
import org.datamate.collaboration.chat.domain.model.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetMessagesService implements GetMessagesUseCase {

    private final MessageRepositoryPort messageRepository;
    private final AttachmentRepositoryPort attachmentRepository;
    private final MessageMapper messageMapper;

    @Override
    @Transactional(readOnly = true)
    public Paged<MessageDto> getMessages(UUID threadId, PageQuery query) {
        Paged<Message> messagesPaged = messageRepository.findByThreadId(threadId, query);
        
        List<UUID> allAttachmentIds = messagesPaged.content().stream()
                .filter(Message::isFile)
                .flatMap(msg -> msg.getAttachmentIds().stream())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        Map<UUID, Attachment> attachmentMap = allAttachmentIds.isEmpty() ? Map.of() :
                attachmentRepository.findAllById(allAttachmentIds).stream()
                        .collect(Collectors.toMap(Attachment::getId, a -> a, (a1, a2) -> a1));

        return messagesPaged.map(msg -> {
            List<Attachment> attachments = msg.getAttachmentIds().stream()
                    .map(attachmentMap::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();
            return messageMapper.toDto(msg, attachments);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Paged<MessageDto> getMessages(UUID threadId, GetMessagesQuery query) {
        PageQuery pageQuery = query != null ? query.toPageQuery() : new PageQuery(1, 10);
        return getMessages(threadId, pageQuery);
    }
}

