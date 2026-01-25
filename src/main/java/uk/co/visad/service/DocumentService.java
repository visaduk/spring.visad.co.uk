package uk.co.visad.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uk.co.visad.entity.Document;
import uk.co.visad.repository.DocumentRepository;
import uk.co.visad.exception.ResourceNotFoundException;
import uk.co.visad.exception.BadRequestException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final String UPLOAD_DIR = "uploads/";

    @Transactional(readOnly = true)
    public List<Document> getDocuments(Long recordId, String recordType) {
        return documentRepository.findByRecordIdAndRecordType(recordId, recordType);
    }

    @Transactional
    public Document upload(MultipartFile file, Long recordId, String recordType, String category) throws IOException {
        if (file.isEmpty()) {
            throw new BadRequestException("Failed to store empty file.");
        }

        // Create upload dir if not exists
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String filename = UUID.randomUUID().toString() + extension;
        Path filePath = uploadPath.resolve(filename);

        Files.copy(file.getInputStream(), filePath);

        // Save to DB
        Document document = Document.builder()
                .recordId(recordId)
                .recordType(recordType)
                .category(category)
                .filename(filename)
                .originalFilename(originalFilename)
                .filePath(filePath.toString())
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .uploadedAt(LocalDateTime.now())
                .build();

        return documentRepository.save(document);
    }

    @Transactional
    public void delete(Long id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        // Delete file from disk
        try {
            Files.deleteIfExists(Paths.get(doc.getFilePath()));
        } catch (IOException e) {
            log.error("Could not delete file: {}", doc.getFilePath());
        }

        documentRepository.delete(doc);
    }
}
