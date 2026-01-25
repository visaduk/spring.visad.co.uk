package uk.co.visad.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.co.visad.service.PdfService;

import java.io.ByteArrayInputStream;

@RestController
@RequestMapping("/pdf")
@RequiredArgsConstructor
public class PdfController {

    private final PdfService pdfService;

    @RequestMapping(value = "/generate", method = { RequestMethod.GET, RequestMethod.POST })
    public ResponseEntity<InputStreamResource> generatePdf(@RequestParam(defaultValue = "Document") String title) {
        ByteArrayInputStream bis = pdfService.generatePdf(title);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=generated.pdf");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(bis));
    }
}
