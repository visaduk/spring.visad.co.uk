package uk.co.visad.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uk.co.visad.dto.ApiResponse;
import uk.co.visad.entity.Document;
import uk.co.visad.service.DocumentService;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping("/get_documents")
    public ResponseEntity<ApiResponse<List<Document>>> getDocuments(
            @RequestParam("record_id") Long recordId,
            @RequestParam("record_type") String recordType) {
        List<Document> documents = documentService.getDocuments(recordId, recordType);
        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Document>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("record_id") Long recordId,
            @RequestParam("record_type") String recordType,
            @RequestParam("category") String category) throws IOException {
        Document document = documentService.upload(file, recordId, recordType, category);
        return ResponseEntity.ok(ApiResponse.success(document, "File uploaded successfully"));
    }

    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> delete(@RequestParam Long id) {
        documentService.delete(id);
        return ResponseEntity.ok(ApiResponse.successMessage("Document deleted successfully"));
    }
}
