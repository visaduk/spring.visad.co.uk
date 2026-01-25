package uk.co.visad.service;

import org.springframework.stereotype.Service;
import java.io.ByteArrayInputStream;

@Service
public class PdfService {

    public ByteArrayInputStream generatePdf(String content) {
        // Stub implementation
        // In a real app, use iText or PDFBox
        return new ByteArrayInputStream(("PDF Content for: " + content).getBytes());
    }
}
