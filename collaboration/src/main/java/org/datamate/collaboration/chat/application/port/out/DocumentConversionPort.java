package org.datamate.collaboration.chat.application.port.out;

import java.io.InputStream;

/**
 * Outbound Port for document-to-PDF conversion in Collaboration.
 * <p>
 * Clean Architecture & Hexagonal compliant: decoupled from web frameworks
 * and transport objects (operates purely on standard I/O streams).
 */
public interface DocumentConversionPort {

    /**
     * Checks if the given file name or extension can be converted to PDF.
     *
     * @param fileNameOrExtension file name (e.g. "report.docx") or raw extension ("docx")
     * @return true if convertible by the underlying converter
     */
    boolean isConvertible(String fileNameOrExtension);

    /**
     * Converts an office document input stream into a PDF byte array.
     *
     * @param sourceStream the input stream of the original document
     * @param extension the file extension or file name (e.g. "docx", "xlsx", "txt")
     * @return the converted PDF bytes
     */
    byte[] convertToPdf(InputStream sourceStream, String extension);
}