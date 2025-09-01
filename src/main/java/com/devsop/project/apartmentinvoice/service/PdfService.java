package com.devsop.project.apartmentinvoice.service;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PdfService {

    private final SpringTemplateEngine templateEngine;

    public byte[] renderTemplateToPdf(String templateName, Map<String, Object> model) {
        Context ctx = new Context();
        if (model != null) model.forEach(ctx::setVariable);

        String html = templateEngine.process(templateName, ctx);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.useDefaultPageSize(210, 297, PdfRendererBuilder.PageSizeUnits.MM);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to render PDF", e);
        }
    }
}
