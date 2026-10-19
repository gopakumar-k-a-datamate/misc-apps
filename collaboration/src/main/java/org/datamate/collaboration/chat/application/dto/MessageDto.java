package org.datamate.collaboration.chat.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MessageDto(
        UUID id,
        UUID parentId,
        String senderId,
        String text,
        boolean isFile,
        boolean isSystemMessage,
        List<AttachmentResponseDto> attachments,
        Instant timestamp
) {}

