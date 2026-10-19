package org.datamate.collaboration.chat.application.usecase;

import com.datamate.bedrock.framework.storage.application.port.StorageService;
import com.datamate.bedrock.framework.storage.domain.model.StorageObject;
import lombok.RequiredArgsConstructor;
import org.datamate.collaboration.chat.application.dto.AttachmentDto;
import org.datamate.collaboration.chat.application.port.in.UploadAttachmentUseCase;
import org.datamate.collaboration.chat.application.port.out.AttachmentRepositoryPort;
import org.datamate.collaboration.chat.application.port.out.DocumentConversionPort;
import org.datamate.collaboration.chat.domain.model.Attachment;
import org.datamate.collaboration.exception.ApplicationValidationException;
import org.datamate.collaboration.exception.CollaborationErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadAttachmentService implements UploadAttachmentUseCase {

    private static final Logger logger = LoggerFactory.getLogger(UploadAttachmentService.class);

    private final StorageService storageService;
    private final DocumentConversionPort documentConversionPort;
    private final AttachmentRepositoryPort attachmentRepository;

    @Value("${bedrock.storage.minio.endpoint:http://localhost:9000}")
    private String minioEndpoint;

    @Value("${collaboration.storage.default-bucket:chat-attachments}")
    private String defaultBucket;

    private static final List<String> BLOCKED_EXTENSIONS = List.of("exe", "bat", "sh", "dll", "msi");
    private static final List<String> PREVIEWABLE_DOCUMENT_EXTENSIONS = List.of("docx", "doc", "xlsx", "xls", "ppt", "pptx", "txt", "rtf", "odt");

    @Override
    @Transactional
    public AttachmentDto uploadAttachment(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApplicationValidationException(CollaborationErrorCodes.REQUIRED_FIELD_MISSING.code(), "file");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new ApplicationValidationException(CollaborationErrorCodes.REQUIRED_FIELD_MISSING.code(), "fileName");
        }

        validateAllowedExtension(originalFilename);

        try {
            // 1. Upload original file to MinIO
            if (!storageService.bucketExists(defaultBucket)) {
                storageService.createBucket(defaultBucket);
            }

            byte[] fileBytes = file.getBytes();
            String objectKey = System.currentTimeMillis() + "_" + originalFilename;

            StorageObject storageObject = storageService.upload(
                    defaultBucket,
                    objectKey,
                    new ByteArrayInputStream(fileBytes),
                    file.getContentType() != null ? file.getContentType() : inferContentType(originalFilename),
                    fileBytes.length
            );

            String uploadUrl = String.format("%s/%s/%s", minioEndpoint, defaultBucket, storageObject.getObjectKey());
            
            // 2. Convert to PDF for preview if eligible
            String previewUrl = null;
            if (isEligibleForPdfPreview(originalFilename)) {
                try {
                    byte[] pdfBytes = documentConversionPort.convertToPdf(new ByteArrayInputStream(fileBytes), originalFilename);
                    if (pdfBytes != null && pdfBytes.length > 0) {
                        String previewKey = "previews/" + UUID.randomUUID() + "/" + originalFilename + ".pdf";
                        StorageObject previewObject = storageService.upload(
                                defaultBucket,
                                previewKey,
                                new ByteArrayInputStream(pdfBytes),
                                "application/pdf",
                                pdfBytes.length
                        );
                        previewUrl = String.format("%s/%s/%s", minioEndpoint, defaultBucket, previewObject.getObjectKey());
                        logger.info("Generated PDF preview for [{}] at [{}]", originalFilename, previewUrl);
                    }
                } catch (Exception e) {
                    logger.warn("PDF preview conversion skipped or failed for [{}]: {}", originalFilename, e.getMessage());
                }
            }

            // 3. Save Attachment Entity
            Attachment attachment = Attachment.create(
                    originalFilename,
                    file.getContentType() != null ? file.getContentType() : inferContentType(originalFilename),
                    file.getSize(),
                    uploadUrl,
                    previewUrl
            );
            attachmentRepository.save(attachment);

            return new AttachmentDto(
                    attachment.getId(),
                    attachment.getFileName(),
                    attachment.getMimeType(),
                    attachment.getFileSize(),
                    attachment.getUploadUrl(),
                    attachment.getPreviewUrl()
            );

        } catch (ApplicationValidationException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            String cause = e.getCause() != null && e.getCause().getMessage() != null ? e.getCause().getMessage().toLowerCase() : "";
            if (msg.contains("403") || msg.contains("forbidden") || msg.contains("virus") || cause.contains("403") || cause.contains("forbidden") || cause.contains("virus")) {
                logger.warn("Upload rejected (possible virus or forbidden) for file [{}]: {}", originalFilename, e.getMessage());
                throw new ApplicationValidationException(CollaborationErrorCodes.VIRUS_DETECTED.code(), originalFilename);
            }
            logger.error("Failed to upload attachment", e);
            throw new RuntimeException("Failed to upload attachment", e);
        }
    }

    private void validateAllowedExtension(String fileName) {
        String lower = fileName.toLowerCase();
        String matchedExtension = BLOCKED_EXTENSIONS.stream()
                .filter(lower::endsWith)
                .findFirst()
                .orElse(null);
        if (matchedExtension != null) {
            throw new ApplicationValidationException(
                    CollaborationErrorCodes.FILE_TYPE_BLOCKED.code(), fileName, matchedExtension);
        }
    }

    private boolean isEligibleForPdfPreview(String fileName) {
        if (fileName == null || fileName.isBlank() || documentConversionPort == null) {
            return false;
        }
        String ext = extractExtension(fileName);
        return PREVIEWABLE_DOCUMENT_EXTENSIONS.contains(ext.toLowerCase())
                && documentConversionPort.isConvertible(fileName);
    }

    private String extractExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex != -1) ? fileName.substring(dotIndex + 1) : fileName;
    }

    private String inferContentType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".txt")) return "text/plain";
        return "application/octet-stream";
    }
}

