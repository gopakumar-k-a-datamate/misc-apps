package org.datamate.collaboration.chat.application.port.in;

import org.datamate.collaboration.chat.application.dto.AttachmentDto;
import org.springframework.web.multipart.MultipartFile;

public interface UploadAttachmentUseCase {
    AttachmentDto uploadAttachment(MultipartFile file);
}
