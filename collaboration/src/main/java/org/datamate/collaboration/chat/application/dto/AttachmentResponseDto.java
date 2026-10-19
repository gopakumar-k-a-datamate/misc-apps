package org.datamate.collaboration.chat.application.dto;

import java.util.UUID;

public record AttachmentResponseDto(
    UUID id,
    UUID messageId,
    String minioFileId,
    String fileName,
    String contentType,
    String fileUrl,
    String previewUrl
) {}
