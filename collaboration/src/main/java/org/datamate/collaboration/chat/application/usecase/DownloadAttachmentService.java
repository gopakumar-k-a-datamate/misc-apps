package org.datamate.collaboration.chat.application.usecase;

import com.datamate.bedrock.framework.storage.application.port.StorageService;
import lombok.RequiredArgsConstructor;
import org.datamate.collaboration.chat.application.port.in.DownloadAttachmentUseCase;
import org.datamate.collaboration.chat.application.port.out.AttachmentRepositoryPort;
import org.datamate.collaboration.chat.domain.model.Attachment;
import org.datamate.collaboration.exception.ApplicationValidationException;
import org.datamate.collaboration.exception.CollaborationErrorCodes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DownloadAttachmentService implements DownloadAttachmentUseCase {

    private final AttachmentRepositoryPort attachmentRepository;
    private final StorageService storageService;

    @Value("${collaboration.storage.default-bucket:chat-attachments}")
    private String defaultBucket;

    @Override
    public ResponseEntity<Resource> downloadAttachment(UUID attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ApplicationValidationException(
                        CollaborationErrorCodes.ATTACHMENT_NOT_FOUND.code(), attachmentId.toString()));

        String uploadUrl = attachment.getUploadUrl();
        // Extract object key from MinIO URL (e.g. http://localhost:9000/bucket/objectKey)
        String objectKey = uploadUrl.contains("/") ? uploadUrl.substring(uploadUrl.lastIndexOf("/") + 1) : uploadUrl;

        InputStream inputStream = storageService.download(defaultBucket, objectKey);
        if (inputStream == null) {
            throw new RuntimeException("File not found in storage: " + objectKey);
        }

        InputStreamResource resource = new InputStreamResource(inputStream);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + attachment.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(attachment.getMimeType()))
                .contentLength(attachment.getFileSize())
                .body(resource);
    }
}
