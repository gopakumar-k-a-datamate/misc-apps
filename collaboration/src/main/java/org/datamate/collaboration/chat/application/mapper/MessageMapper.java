package org.datamate.collaboration.chat.application.mapper;

import org.datamate.collaboration.chat.application.dto.AttachmentResponseDto;
import org.datamate.collaboration.chat.application.dto.MessageDto;
import org.datamate.collaboration.chat.domain.model.Attachment;
import org.datamate.collaboration.chat.domain.model.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MessageMapper {

    @Value("${collaboration.api.base-url:http://localhost:8085}")
    private String baseUrl;

    public MessageDto toDto(Message message, Attachment attachment) {
        if (message == null) {
            return null;
        }

        List<AttachmentResponseDto> attachments = null;
        if (attachment != null) {
            String uploadUrl = attachment.getUploadUrl();
            String minioFileId = uploadUrl != null && uploadUrl.contains("/") ? uploadUrl.substring(uploadUrl.lastIndexOf("/") + 1) : uploadUrl;
            String fileUrl = baseUrl + "/api/v1/collaboration/attachments/" + attachment.getId();

            AttachmentResponseDto attachmentDto = new AttachmentResponseDto(
                    attachment.getId(),
                    message.getId(),
                    minioFileId,
                    attachment.getFileName(),
                    attachment.getMimeType(),
                    fileUrl,
                    attachment.getPreviewUrl()
            );
            attachments = List.of(attachmentDto);
        }

        return new MessageDto(
                message.getId(),
                message.getParentId(),
                message.getSenderId(),
                message.getText(),
                message.isFile(),
                message.isSystemMessage(),
                attachments,
                message.getTimestamp()
        );
    }
}


