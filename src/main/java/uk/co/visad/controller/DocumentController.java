package uk.co.visad.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
            @RequestParam(value = "record_id", required = false) Long recordId,
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "record_type", required = false) String recordType,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "category", required = false) String category) {

        Long finalId = (recordId != null) ? recordId : id;
        String finalType = (recordType != null) ? recordType : type;

        if (finalId == null || finalType == null) {
             return ResponseEntity.badRequest().body(ApiResponse.error("Missing id or type"));
        }

        List<Document> documents;
        if (category != null && !category.isEmpty()) {
            documents = documentService.getDocumentsByCategory(finalId, finalType, category);
        } else {
            documents = documentService.getDocuments(finalId, finalType);
        }

        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Document>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "record_id", required = false) Long recordId,
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "record_type", required = false) String recordType,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam("category") String category) throws IOException {
        Long finalId = (recordId != null) ? recordId : id;
        String finalType = (recordType != null) ? recordType : type;
        Document document = documentService.upload(file, finalId, finalType, category);
        return ResponseEntity.ok(ApiResponse.success(document, "File uploaded successfully"));
    }

    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> delete(@RequestParam Long id) {
        documentService.delete(id);
        return ResponseEntity.ok(ApiResponse.successMessage("Document deleted successfully"));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        DocumentService.DocumentDownload dl = documentService.getForDownload(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(dl.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + dl.filename() + "\"")
                .body(dl.resource());
    }
}
