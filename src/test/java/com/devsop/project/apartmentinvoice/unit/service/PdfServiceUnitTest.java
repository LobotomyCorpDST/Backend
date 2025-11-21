package com.devsop.project.apartmentinvoice.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.devsop.project.apartmentinvoice.entity.Lease;
import com.devsop.project.apartmentinvoice.entity.Room;
import com.devsop.project.apartmentinvoice.entity.Tenant;
import com.devsop.project.apartmentinvoice.service.PdfService;

/**
 * Unit tests for PdfService focusing on PDF generation and merging.
 */
@ExtendWith(MockitoExtension.class)
class PdfServiceUnitTest {

    @Mock
    private SpringTemplateEngine templateEngine;

    @InjectMocks
    private PdfService pdfService;

    private Lease testLease;
    private Room testRoom;
    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        // Create test data
        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setNumber(201);

        testTenant = new Tenant();
        testTenant.setId(1L);
        testTenant.setName("สมชาย ใจดี"); // Thai name

        testLease = new Lease();
        testLease.setId(1L);
        testLease.setRoom(testRoom);
        testLease.setTenant(testTenant);
        testLease.setMonthlyRent(new BigDecimal("5000.00"));
        testLease.setDepositBaht(new BigDecimal("10000.00"));
        testLease.setStartDate(LocalDate.of(2025, 1, 1));
        testLease.setEndDate(LocalDate.of(2025, 12, 31));
    }

    @Test
    void testRenderTemplateToPdf_validTemplate_returnsPdfBytes() {
        // Arrange
        String templateName = "test-template";
        Map<String, Object> model = new HashMap<>();
        model.put("title", "Test Document");
        model.put("content", "This is a test");

        // Mock Thymeleaf to return simple HTML
        String mockHtml = "<!DOCTYPE html><html><head><title>Test</title></head><body><h1>Test Document</h1></body></html>";
        when(templateEngine.process(eq(templateName), any(Context.class)))
                .thenReturn(mockHtml);

        // Act
        byte[] pdfBytes = pdfService.renderTemplateToPdf(templateName, model);

        // Assert
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        // PDF files start with "%PDF-" signature
        String pdfHeader = new String(Arrays.copyOfRange(pdfBytes, 0, Math.min(5, pdfBytes.length)));
        assertTrue(pdfHeader.startsWith("%PDF"), "Generated file should be a valid PDF");

        verify(templateEngine).process(eq(templateName), any(Context.class));
    }

    @Test
    void testRenderTemplateToPdf_withThaiContent_handlesThai() {
        // Arrange
        String templateName = "thai-template";
        Map<String, Object> model = new HashMap<>();
        model.put("thaiText", "สวัสดีครับ");

        String mockHtml = "<!DOCTYPE html><html><head><meta charset='UTF-8'/></head><body><p>สวัสดีครับ</p></body></html>";
        when(templateEngine.process(eq(templateName), any(Context.class)))
                .thenReturn(mockHtml);

        // Act
        byte[] pdfBytes = pdfService.renderTemplateToPdf(templateName, model);

        // Assert
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        String pdfHeader = new String(Arrays.copyOfRange(pdfBytes, 0, 5));
        assertTrue(pdfHeader.startsWith("%PDF"));
    }

    @Test
    void testRenderTemplateToPdf_withLocale_usesCorrectLocale() {
        // Arrange
        String templateName = "localized-template";
        Map<String, Object> model = Collections.emptyMap();
        Locale thaiLocale = Locale.forLanguageTag("th-TH");

        String mockHtml = "<!DOCTYPE html><html><body><p>Localized content</p></body></html>";
        when(templateEngine.process(eq(templateName), any(Context.class)))
                .thenReturn(mockHtml);

        // Act
        byte[] pdfBytes = pdfService.renderTemplateToPdf(templateName, model, thaiLocale);

        // Assert
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        // Verify Context was created with Thai locale
        verify(templateEngine).process(eq(templateName), argThat((Context ctx) ->
            ctx.getLocale().equals(thaiLocale)
        ));
    }

    @Test
    void testRenderTemplateToPdf_withNullModel_handlesGracefully() {
        // Arrange
        String templateName = "simple-template";
        String mockHtml = "<!DOCTYPE html><html><body><p>Simple</p></body></html>";
        when(templateEngine.process(eq(templateName), any(Context.class)))
                .thenReturn(mockHtml);

        // Act
        byte[] pdfBytes = pdfService.renderTemplateToPdf(templateName, null);

        // Assert
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testRenderTemplateToPdf_templateException_throwsRuntimeException() {
        // Arrange
        String templateName = "invalid-template";
        Map<String, Object> model = Collections.emptyMap();

        when(templateEngine.process(eq(templateName), any(Context.class)))
                .thenThrow(new RuntimeException("Template not found"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            pdfService.renderTemplateToPdf(templateName, model);
        });
    }

    @Test
    void testGenerateLeasePdf_validLease_returnsPdf() {
        // Arrange
        String mockHtml = "<!DOCTYPE html><html><body>" +
                "<h1>สัญญาเช่า</h1>" +
                "<p>ห้อง: 201</p>" +
                "<p>ผู้เช่า: สมชาย ใจดี</p>" +
                "<p>ค่าเช่า: 5000 บาท/เดือน</p>" +
                "</body></html>";

        when(templateEngine.process(eq("lease/print"), any(Context.class)))
                .thenReturn(mockHtml);

        // Act
        byte[] pdfBytes = pdfService.generateLeasePdf(testLease);

        // Assert
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        String pdfHeader = new String(Arrays.copyOfRange(pdfBytes, 0, 5));
        assertTrue(pdfHeader.startsWith("%PDF"));

        // Verify correct template and model used
        verify(templateEngine).process(eq("lease/print"), argThat((Context ctx) -> {
            Object leaseObj = ctx.getVariable("lease");
            Object roomObj = ctx.getVariable("room");
            Object tenantObj = ctx.getVariable("tenant");
            return leaseObj != null && roomObj != null && tenantObj != null;
        }));
    }

    @Test
    void testMergePdfs_singlePdf_returnsSamePdf() {
        // Arrange
        byte[] singlePdf = "%PDF-1.4\ntest content".getBytes();
        List<byte[]> pdfList = Collections.singletonList(singlePdf);

        // Act
        byte[] result = pdfService.mergePdfs(pdfList);

        // Assert
        assertArrayEquals(singlePdf, result);
    }

    @Test
    void testMergePdfs_multiplePdfs_mergesSuccessfully() {
        // Arrange: Create minimal valid PDF byte arrays
        byte[] pdf1 = createMinimalPdf();
        byte[] pdf2 = createMinimalPdf();
        byte[] pdf3 = createMinimalPdf();

        List<byte[]> pdfList = Arrays.asList(pdf1, pdf2, pdf3);

        // Act
        byte[] merged = pdfService.mergePdfs(pdfList);

        // Assert
        assertNotNull(merged);
        assertTrue(merged.length > 0);
        // Merged PDF should be larger than individual PDFs (generally)
        assertTrue(merged.length >= pdf1.length);
    }

    @Test
    void testMergePdfs_emptyList_throwsException() {
        // Arrange
        List<byte[]> emptyList = Collections.emptyList();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            pdfService.mergePdfs(emptyList);
        });
    }

    @Test
    void testMergePdfs_nullList_throwsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            pdfService.mergePdfs(null);
        });
    }

    @Test
    void testMergePdfs_withNullPdf_skipsNull() {
        // Arrange
        byte[] validPdf = createMinimalPdf();
        List<byte[]> pdfList = Arrays.asList(validPdf, null, validPdf);

        // Act
        byte[] merged = pdfService.mergePdfs(pdfList);

        // Assert
        assertNotNull(merged);
        assertTrue(merged.length > 0);
    }

    @Test
    void testMergePdfs_withEmptyPdf_skipsEmpty() {
        // Arrange
        byte[] validPdf = createMinimalPdf();
        byte[] emptyPdf = new byte[0];
        List<byte[]> pdfList = Arrays.asList(validPdf, emptyPdf, validPdf);

        // Act
        byte[] merged = pdfService.mergePdfs(pdfList);

        // Assert
        assertNotNull(merged);
        assertTrue(merged.length > 0);
    }

    /**
     * Helper method to create a minimal valid PDF for testing.
     * This creates a very basic PDF structure that PDFBox can process.
     */
    private byte[] createMinimalPdf() {
        // Create a minimal PDF structure
        StringBuilder sb = new StringBuilder();
        sb.append("%PDF-1.4\n");
        sb.append("1 0 obj\n");
        sb.append("<< /Type /Catalog /Pages 2 0 R >>\n");
        sb.append("endobj\n");
        sb.append("2 0 obj\n");
        sb.append("<< /Type /Pages /Kids [3 0 R] /Count 1 >>\n");
        sb.append("endobj\n");
        sb.append("3 0 obj\n");
        sb.append("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] >>\n");
        sb.append("endobj\n");
        sb.append("xref\n");
        sb.append("0 4\n");
        sb.append("0000000000 65535 f \n");
        sb.append("0000000009 00000 n \n");
        sb.append("0000000058 00000 n \n");
        sb.append("0000000115 00000 n \n");
        sb.append("trailer\n");
        sb.append("<< /Size 4 /Root 1 0 R >>\n");
        sb.append("startxref\n");
        sb.append("190\n");
        sb.append("%%EOF\n");

        return sb.toString().getBytes();
    }
}
