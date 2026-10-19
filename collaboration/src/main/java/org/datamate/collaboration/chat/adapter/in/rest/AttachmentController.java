package org.datamate.collaboration.chat.adapter.in.rest;

import lombok.RequiredArgsConstructor;
import org.datamate.collaboration.chat.application.dto.AttachmentDto;
import org.datamate.collaboration.chat.application.port.in.DownloadAttachmentUseCase;
import org.datamate.collaboration.chat.application.port.in.UploadAttachmentUseCase;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@CrossOrigin
@RestController
@RequestMapping("/api/v1/collaboration/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final UploadAttachmentUseCase uploadAttachmentUseCase;
    private final DownloadAttachmentUseCase downloadAttachmentUseCase;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentDto uploadAttachment(@RequestParam("file") MultipartFile file) {
        return uploadAttachmentUseCase.uploadAttachment(file);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable UUID id) {
        return downloadAttachmentUseCase.downloadAttachment(id);
    }
}
