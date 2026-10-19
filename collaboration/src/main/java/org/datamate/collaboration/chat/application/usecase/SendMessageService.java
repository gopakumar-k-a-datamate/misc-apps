package org.datamate.collaboration.chat.application.usecase;

import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import com.datamate.bedrock.framework.storage.application.port.StorageService;
import com.datamate.bedrock.framework.storage.domain.model.StorageObject;
import lombok.RequiredArgsConstructor;
import org.datamate.collaboration.chat.application.dto.SendMessageCommand;
import org.datamate.collaboration.chat.application.dto.SendMessageRequest;
import org.datamate.collaboration.chat.application.port.in.SendMessageUseCase;
import org.datamate.collaboration.chat.application.port.out.AttachmentRepositoryPort;
import org.datamate.collaboration.chat.application.dto.MessageDto;
import org.datamate.collaboration.chat.application.mapper.MessageMapper;
import org.datamate.collaboration.chat.application.port.out.MessageBroadcastPort;
import org.datamate.collaboration.chat.application.port.out.DocumentConversionPort;
import org.datamate.collaboration.chat.application.port.out.MessageRepositoryPort;
import org.datamate.collaboration.chat.application.port.out.ThreadRepositoryPort;
import org.datamate.collaboration.chat.domain.model.Attachment;
import org.datamate.collaboration.chat.domain.model.Message;
import org.datamate.collaboration.chat.domain.model.Thread;
import org.datamate.collaboration.exception.ApplicationValidationException;
import org.datamate.collaboration.exception.CollaborationErrorCodes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Application Service for sending messages.
 * <p>
 * Implements {@link SendMessageUseCase}. Handles the command side of messaging.
 * Follows the RMS standard: the frontend uploads files to cloud storage (MinIO) first,
 * and sends a JSON payload containing the message text and/or attachmentUrls.
 * Encapsulates application-level validation and business policies for preview generation,
 * delegating conversion to {@link DocumentConversionPort}.
 */
@Service
@RequiredArgsConstructor
public class SendMessageService implements SendMessageUseCase {

    private static final List<String> BLOCKED_EXTENSIONS = List.of(".xml", ".bpmn", ".exe", ".bat", ".sh");

    /**
     * Business policy: File extensions eligible for inline PDF preview generation.
     */
    private static final Set<String> PREVIEWABLE_DOCUMENT_EXTENSIONS = Set.of(
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp", "rtf", "txt"
    );

    @EnableLogger
    private Logger logger;

    private final MessageRepositoryPort messageRepository;
    private final ThreadRepositoryPort threadRepository;
    private final AttachmentRepositoryPort attachmentRepository;
    private final StorageService storageService;
    private final DocumentConversionPort documentConversionPort;
    private final MessageMapper messageMapper;
    private final MessageBroadcastPort messageBroadcastPort;

    @Value("${bedrock.storage.minio.bucket}")
    private String defaultBucket;

    @Override
    @Transactional
    public void sendMessage(UUID threadId, String senderId, SendMessageCommand command) {
        validateCommand(command);
        ensureThreadExists(threadId);

        UUID attachmentId = resolveAttachmentId(threadId, command);
        boolean isFile = attachmentId != null;

        Message message = Message.create(
                threadId,
                senderId,
                command.text(),
                isFile,
                false,
                attachmentId,
                command.parentId()
        );

        Message savedMessage = messageRepository.save(message);

        Attachment attachment = null;
        if (attachmentId != null) {
            attachment = attachmentRepository.findById(attachmentId).orElse(null);
        }

        MessageDto messageDto = messageMapper.toDto(savedMessage != null ? savedMessage : message, attachment);
        messageBroadcastPort.broadcastMessage(threadId, messageDto);
    }

    @Override
    @Transactional
    public void sendMessage(UUID threadId, String senderId, SendMessageRequest request) {
        sendMessage(threadId, senderId, request != null ? request.toCommand() : null);
    }

    /**
     * Validates use-case command invariants at the application layer.
     */
    private void validateCommand(SendMessageCommand command) {
        if (command == null) {
            throw new ApplicationValidationException(
                    CollaborationErrorCodes.REQUIRED_FIELD_MISSING.code(), "command");
        }

        boolean hasText = command.text() != null && !command.text().trim().isEmpty();
        boolean hasAttachmentUrls = command.attachmentUrls() != null && !command.attachmentUrls().isEmpty();
        boolean hasAttachmentId = command.attachmentId() != null;

        if (!hasText && !hasAttachmentUrls && !hasAttachmentId) {
            throw new ApplicationValidationException(
                    CollaborationErrorCodes.REQUIRED_FIELD_MISSING.code(), "text or attachmentUrls");
        }
    }

    /**
     * Resolves the attachment ID from the command, either by using a direct attachment ID
     * or by processing an uploaded attachment URL.
     */
    private UUID resolveAttachmentId(UUID threadId, SendMessageCommand command) {
        if (command.attachmentId() != null) {
            return command.attachmentId();
        }

        if (command.attachmentUrls() != null && !command.attachmentUrls().isEmpty()) {
            return processAttachmentUrl(threadId, command.attachmentUrls().get(0));
        }

        return null;
    }

    /**
     * Processes an uploaded attachment URL: validates security constraints, fetches metadata,
     * triggers PDF preview generation if eligible, and persists the attachment entity.
     */
    private UUID processAttachmentUrl(UUID threadId, String attachmentUrl) {
        validateAttachmentUrl(attachmentUrl);

        

        String objectKeyOrFileName = extractObjectKey(attachmentUrl);
        validateAllowedExtension(objectKeyOrFileName);

        String fileName = extractFriendlyFileName(objectKeyOrFileName);
        AttachmentMetadata metadata = resolveAttachmentMetadata(objectKeyOrFileName, fileName);
        String previewUrl = generatePdfPreview(threadId, fileName, objectKeyOrFileName);

        Attachment attachment = Attachment.create(
                fileName,
                metadata.contentType(),
                metadata.fileSize(),
                attachmentUrl,
                previewUrl
        );

        attachmentRepository.save(attachment);
        return attachment.getId();
    }

    private void validateAttachmentUrl(String attachmentUrl) {
        if (attachmentUrl == null || attachmentUrl.isBlank()) {
            throw new ApplicationValidationException(
                    CollaborationErrorCodes.FIELD_BLANK.code(), "attachmentUrl");
        }
    }

    /**
     * Security check: rejects forbidden executable/script file extensions.
     */
    private void validateAllowedExtension(String objectKeyOrFileName) {
        String lower = objectKeyOrFileName.toLowerCase();
        String matchedExtension = BLOCKED_EXTENSIONS.stream()
                .filter(lower::endsWith)
                .findFirst()
                .orElse(null);
        if (matchedExtension != null) {
            throw new ApplicationValidationException(
                    CollaborationErrorCodes.FILE_TYPE_BLOCKED.code(), objectKeyOrFileName, matchedExtension);
        }
    }

    private String extractObjectKey(String attachmentUrl) {
        return attachmentUrl.contains("/")
                ? attachmentUrl.substring(attachmentUrl.lastIndexOf('/') + 1)
                : attachmentUrl;
    }

    private String extractFriendlyFileName(String objectKeyOrFileName) {
        return objectKeyOrFileName.contains("_")
                ? objectKeyOrFileName.substring(objectKeyOrFileName.indexOf('_') + 1)
                : objectKeyOrFileName;
    }

    /**
     * Resolves attachment MIME type and file size from storage metadata or fallback inference.
     */
    private AttachmentMetadata resolveAttachmentMetadata(String objectKey, String fileName) {
        String contentType = inferContentType(fileName);
        long fileSize = 0L;

        try {
            StorageObject metadata = storageService.getMetadata(defaultBucket, objectKey);
            if (metadata != null) {
                if (metadata.getContentType() != null) {
                    contentType = metadata.getContentType();
                }
                fileSize = metadata.getSize();
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.debug("Could not fetch metadata for objectKey: {}. Using inferred values.", objectKey);
            }
        }

        return new AttachmentMetadata(contentType, fileSize);
    }

    /**
     * Converts office documents to PDF preview if applicable according to business policies.
     * Preview generation failures are logged as warnings and do not fail message sending.
     */
    private String generatePdfPreview(UUID threadId, String fileName, String objectKeyOrFileName) {
        if (!isEligibleForPdfPreview(fileName)) {
            return null;
        }

        try {
            InputStream originalStream = storageService.download(defaultBucket, objectKeyOrFileName);
            if (originalStream == null) {
                return null;
            }

            byte[] pdfBytes = documentConversionPort.convertToPdf(originalStream, fileName);
            if (pdfBytes == null || pdfBytes.length == 0) {
                return null;
            }

            String previewKey = String.format("previews/%s/%s.pdf", threadId, fileName);
            StorageObject previewObject = storageService.upload(
                    defaultBucket,
                    previewKey,
                    new ByteArrayInputStream(pdfBytes),
                    "application/pdf",
                    pdfBytes.length
            );

            if (logger != null) {
                logger.info("Generated PDF preview for [{}] at [{}]", fileName, previewObject.getObjectKey());
            }
            return previewObject.getObjectKey();
        } catch (Exception e) {
            if (logger != null) {
                logger.warn("PDF preview conversion skipped or failed for [{}]: {}", fileName, e.getMessage());
            }
            return null;
        }
    }

    /**
     * Determines whether an attachment is eligible for inline PDF preview generation based on domain policies.
     */
    private boolean isEligibleForPdfPreview(String fileName) {
        if (fileName == null || fileName.isBlank() || documentConversionPort == null) {
            return false;
        }
        String ext = extractExtension(fileName);
        return PREVIEWABLE_DOCUMENT_EXTENSIONS.contains(ext.toLowerCase())
                && documentConversionPort.isConvertible(fileName);
    }

    private String extractExtension(String fileName) {
        if (fileName == null) return "";
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex != -1) ? fileName.substring(dotIndex + 1) : fileName;
    }

    private String inferContentType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".txt")) return "text/plain";
        return "application/octet-stream";
    }

    /**
     * Idempotent lazy Thread creation.
     */
    private void ensureThreadExists(UUID threadId) {
        if (!threadRepository.existsById(threadId)) {
            try {
                threadRepository.save(new Thread(threadId));
                if (logger != null) {
                    logger.info("Lazily created thread [{}]", threadId);
                }
            } catch (DataIntegrityViolationException e) {
                if (logger != null) {
                    logger.debug("Thread [{}] was concurrently created by another transaction, proceeding.", threadId);
                }
            }
        }
    }

    /**
     * Immutable value object holding resolved metadata for an attachment.
     */
    private record AttachmentMetadata(String contentType, long fileSize) {}
}


