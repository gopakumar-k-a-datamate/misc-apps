package org.datamate.collaboration.chat.application.dto;

import java.util.UUID;

public record AttachmentDto(
        UUID id,
        String fileName,
        String contentType,
        long fileSize,
        String uploadUrl,
        String previewUrl
) {
}
