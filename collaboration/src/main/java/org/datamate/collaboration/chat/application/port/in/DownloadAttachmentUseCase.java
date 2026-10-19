package org.datamate.collaboration.chat.application.port.in;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface DownloadAttachmentUseCase {
    ResponseEntity<Resource> downloadAttachment(UUID attachmentId);
}
