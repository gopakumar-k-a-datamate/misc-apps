package org.datamate.collaboration.chat.adapter.out.document;

import org.datamate.collaboration.exception.DocumentConversionException;
import org.jodconverter.core.DocumentConverter;
import org.jodconverter.core.office.OfficeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LibreOfficeDocumentConverterAdapter Unit Tests")
class LibreOfficeDocumentConverterAdapterTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DocumentConverter documentConverter;

    @Mock
    private ObjectProvider<DocumentConverter> provider;

    private LibreOfficeDocumentConverterAdapter adapter;

    @BeforeEach
    void setUp() {
        when(provider.getIfAvailable()).thenReturn(documentConverter);
        adapter = new LibreOfficeDocumentConverterAdapter(provider);
    }

    @Test
    @DisplayName("isConvertible should return false when filename is null or blank")
    void isConvertible_ShouldReturnFalse_WhenFilenameNullOrBlank() {
        assertThat(adapter.isConvertible(null)).isFalse();
        assertThat(adapter.isConvertible("   ")).isFalse();
    }

    @Test
    @DisplayName("isConvertible should return false when jodConverter is unavailable")
    void isConvertible_ShouldReturnFalse_WhenConverterUnavailable() {
        when(provider.getIfAvailable()).thenReturn(null);
        LibreOfficeDocumentConverterAdapter nullAdapter = new LibreOfficeDocumentConverterAdapter(provider);

        assertThat(nullAdapter.isConvertible("document.docx")).isFalse();
    }

    @Test
    @DisplayName("isConvertible should return true for standard formats supported by JODConverter registry")
    void isConvertible_ShouldReturnTrue_ForSupportedFormats() {
        assertThat(adapter.isConvertible("report.docx")).isTrue();
        assertThat(adapter.isConvertible("data.xlsx")).isTrue();
        assertThat(adapter.isConvertible("presentation.pptx")).isTrue();
        assertThat(adapter.isConvertible("notes.txt")).isTrue();
    }

    @Test
    @DisplayName("isConvertible should return false for unsupported formats")
    void isConvertible_ShouldReturnFalse_ForUnsupportedFormats() {
        assertThat(adapter.isConvertible("image.xyzunknown123")).isFalse();
    }

    @Test
    @DisplayName("convertToPdf should throw DocumentConversionException when sourceStream is null")
    void convertToPdf_ShouldThrowDocumentConversionException_WhenStreamNull() {
        assertThatThrownBy(() -> adapter.convertToPdf(null, "docx"))
                .isInstanceOf(DocumentConversionException.class)
                .hasMessageContaining("sourceStream must not be null");
    }

    @Test
    @DisplayName("convertToPdf should throw DocumentConversionException when converter is unavailable")
    void convertToPdf_ShouldThrowDocumentConversionException_WhenConverterUnavailable() {
        when(provider.getIfAvailable()).thenReturn(null);
        LibreOfficeDocumentConverterAdapter nullAdapter = new LibreOfficeDocumentConverterAdapter(provider);
        InputStream is = new ByteArrayInputStream("content".getBytes());

        assertThatThrownBy(() -> nullAdapter.convertToPdf(is, "docx"))
                .isInstanceOf(DocumentConversionException.class)
                .hasMessageContaining("JODConverter is not enabled");
    }

    @Test
    @DisplayName("convertToPdf should throw DocumentConversionException for unknown format")
    void convertToPdf_ShouldThrowDocumentConversionException_WhenFormatUnknown() {
        InputStream is = new ByteArrayInputStream("content".getBytes());

        assertThatThrownBy(() -> adapter.convertToPdf(is, "unknown_ext_999"))
                .isInstanceOf(DocumentConversionException.class)
                .hasMessageContaining("Unsupported document format");
    }

    @Test
    @DisplayName("convertToPdf should throw DocumentConversionException when OfficeException occurs")
    void convertToPdf_ShouldThrowDocumentConversionException_WhenOfficeExceptionOccurs() {
        InputStream is = new ByteArrayInputStream("content".getBytes());

        when(documentConverter.convert(any(InputStream.class))).thenAnswer(inv -> {
            throw new OfficeException("LibreOffice process crashed");
        });

        assertThatThrownBy(() -> adapter.convertToPdf(is, "docx"))
                .isInstanceOf(DocumentConversionException.class)
                .hasMessageContaining("Document conversion to PDF failed");
    }

    @Test
    @DisplayName("convertToPdf should successfully return bytes when execution succeeds")
    void convertToPdf_ShouldReturnBytes_WhenSuccessful() {
        InputStream is = new ByteArrayInputStream("content".getBytes());

        byte[] result = adapter.convertToPdf(is, "docx");

        assertThat(result).isNotNull();
    }
}