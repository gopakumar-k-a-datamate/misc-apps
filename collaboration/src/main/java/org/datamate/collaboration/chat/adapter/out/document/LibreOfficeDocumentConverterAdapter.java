package org.datamate.collaboration.chat.adapter.out.document;

import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import org.datamate.collaboration.chat.application.port.out.DocumentConversionPort;
import org.datamate.collaboration.exception.DocumentConversionException;
import org.jodconverter.core.DocumentConverter;
import org.jodconverter.core.document.DefaultDocumentFormatRegistry;
import org.jodconverter.core.document.DocumentFormat;
import org.jodconverter.core.office.OfficeException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Outgoing Adapter implementing {@link DocumentConversionPort} using JODConverter and LibreOffice.
 * <p>
 * Converts office documents (.docx, .xlsx, .pptx, etc.) to standard PDF bytes
 * for inline preview generation. Decoupled completely from Web/Multipart layers.
 * Uses Bedrock Logger and throws {@link DocumentConversionException} for conversion/infrastructure errors.
 */
@Component
public class LibreOfficeDocumentConverterAdapter implements DocumentConversionPort {

    @EnableLogger
    private Logger logger;

    private final DocumentConverter jodConverter;

    public LibreOfficeDocumentConverterAdapter(ObjectProvider<DocumentConverter> jodConverterProvider) {
        this.jodConverter = jodConverterProvider.getIfAvailable();
    }

    @Override
    public boolean isConvertible(String fileNameOrExtension) {
        if (fileNameOrExtension == null || fileNameOrExtension.isBlank() || jodConverter == null) {
            return false;
        }
        String ext = extractExtension(fileNameOrExtension);
        return DefaultDocumentFormatRegistry.getFormatByExtension(ext.toLowerCase()) != null;
    }

    @Override
    public byte[] convertToPdf(InputStream sourceStream, String extension) {
        if (sourceStream == null) {
            throw new DocumentConversionException("sourceStream must not be null");
        }
        if (jodConverter == null) {
            throw new DocumentConversionException("JODConverter is not enabled or available in current environment");
        }

        String ext = extractExtension(extension);
        DocumentFormat sourceFormat = DefaultDocumentFormatRegistry.getFormatByExtension(ext.toLowerCase());
        if (sourceFormat == null) {
            throw new DocumentConversionException("Unsupported document format for PDF conversion: " + ext);
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (logger != null) {
                logger.info("Converting document with extension [{}] to PDF preview", ext);
            }

            jodConverter.convert(sourceStream)
                    .as(sourceFormat)
                    .to(outputStream)
                    .as(DefaultDocumentFormatRegistry.PDF)
                    .execute();

            return outputStream.toByteArray();
        } catch (OfficeException e) {
            if (logger != null) {
                logger.error("LibreOffice conversion failed for extension [{}]: {}", ext, e.getMessage());
            }
            throw new DocumentConversionException("Document conversion to PDF failed: " + e.getMessage(), e);
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Unexpected error during document conversion: {}", e.getMessage());
            }
            throw new DocumentConversionException("Unexpected error during document conversion: " + e.getMessage(), e);
        }
    }

    private String extractExtension(String filenameOrExtension) {
        if (filenameOrExtension == null) return "";
        int dotIndex = filenameOrExtension.lastIndexOf('.');
        return (dotIndex != -1) ? filenameOrExtension.substring(dotIndex + 1) : filenameOrExtension;
    }
}