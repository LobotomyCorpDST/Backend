package com.devsop.project.apartmentinvoice.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Entities;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.devsop.project.apartmentinvoice.entity.Lease;
import com.openhtmltopdf.extend.FSSupplier;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PdfService {

    private final SpringTemplateEngine templateEngine;

    /**
     * Render Thymeleaf template -> PDF (ใช้ Jsoup แปลงเป็น XHTML ก่อน)
     * รองรับภาษาไทยและฟอนต์ภายใน classpath (/resources/fonts/)
     */
    public byte[] renderTemplateToPdf(String templateName, Map<String, Object> model) {
        return renderTemplateToPdf(templateName, model, Locale.forLanguageTag("th-TH"));
    }

    public byte[] renderTemplateToPdf(String templateName, Map<String, Object> model, Locale locale) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // ---------- 1) Render HTML ด้วย Thymeleaf ----------
            Context ctx = new Context(locale);
            if (model != null) model.forEach(ctx::setVariable);
            String html = templateEngine.process(templateName, ctx);

            // ---------- 2) แปลง HTML -> XHTML ด้วย Jsoup ----------
            Document jsoupDoc = Jsoup.parse(html, "UTF-8");
            jsoupDoc.outputSettings(new OutputSettings()
                    .syntax(OutputSettings.Syntax.xml)
                    .charset(StandardCharsets.UTF_8)
                    .escapeMode(Entities.EscapeMode.xhtml)
                    .prettyPrint(false));
            org.w3c.dom.Document w3cDoc = new W3CDom().fromJsoup(jsoupDoc);

            // ---------- 3) Render PDF ด้วย OpenHTMLtoPDF ----------
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withW3cDocument(w3cDoc, null);
            builder.toStream(out);
            builder.useDefaultPageSize(210, 297, PdfRendererBuilder.PageSizeUnits.MM);

            // ---------- 4) โหลดฟอนต์ภาษาไทยจาก classpath ----------
            loadFont(builder, "/fonts/THSarabunNew.ttf", "TH Sarabun New");
            loadFont(builder, "/fonts/NotoSansThai-Regular.ttf", "Noto Sans Thai");
            loadFont(builder, "/fonts/NotoSansThai-Bold.ttf", "Noto Sans Thai Bold");

            builder.run();
            return out.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("❌ Failed to render PDF: " + e.getMessage(), e);
        }
    }

    /**
     * โหลดฟอนต์จาก classpath (ใช้ FSSupplier เพื่อรองรับใน container environment)
     */
    private void loadFont(PdfRendererBuilder builder, String path, String name) {
        try {
            FSSupplier<InputStream> supplier = () -> {
                InputStream is = PdfService.class.getResourceAsStream(path);
                if (is == null) {
                    System.err.println("⚠️ Font not found at " + path);
                }
                return is;
            };
            builder.useFont(supplier, name);
        } catch (Exception e) {
            System.err.println("⚠️ Cannot load font " + name + ": " + e.getMessage());
        }
    }

    /** สำหรับสัญญาเช่า (lease/print.html) */
    public byte[] generateLeasePdf(Lease lease) {
        Map<String, Object> model = Map.of(
            "lease", lease,
            "room", lease.getRoom(),
            "tenant", lease.getTenant()
        );
        return renderTemplateToPdf("lease/print", model);
    }

    /**
     * Merge multiple PDF byte arrays into a single PDF document
     * @param pdfList List of PDF documents as byte arrays
     * @return Combined PDF as byte array
     */
    public byte[] mergePdfs(List<byte[]> pdfList) {
        if (pdfList == null || pdfList.isEmpty()) {
            throw new IllegalArgumentException("PDF list cannot be null or empty");
        }

        if (pdfList.size() == 1) {
            return pdfList.get(0);
        }

        try (ByteArrayOutputStream mergedOutput = new ByteArrayOutputStream()) {
            PDFMergerUtility merger = new PDFMergerUtility();
            merger.setDestinationStream(mergedOutput);

            // Add each PDF to the merger
            for (byte[] pdfBytes : pdfList) {
                if (pdfBytes != null && pdfBytes.length > 0) {
                    merger.addSource(new ByteArrayInputStream(pdfBytes));
                }
            }

            merger.mergeDocuments(null);
            return mergedOutput.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("❌ Failed to merge PDFs: " + e.getMessage(), e);
        }
    }
}
