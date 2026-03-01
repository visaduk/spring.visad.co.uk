package uk.co.visad.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uk.co.visad.entity.Document;
import uk.co.visad.repository.DocumentRepository;
import uk.co.visad.exception.ResourceNotFoundException;
import uk.co.visad.exception.BadRequestException;
import uk.co.visad.util.FileEncryptionUtil;
import uk.co.visad.util.NamedByteArrayResource;

import java.io.IOException;
import java.net.MalformedURLException;
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

    // All vault document uploads go to:  uploadRoot/documents/
    // The DB stores the relative path "documents/UUID.ext" so the root can change
    // without touching the database (just update VAULT_UPLOAD_ROOT env var).
    @Value("${app.upload.root:/home/VisaD/visad.co.uk/vault_uploads}")
    private String uploadRoot;

    @Autowired(required = false)
    private FileEncryptionUtil encryptionUtil;

    @Transactional(readOnly = true)
    public List<Document> getDocuments(Long recordId, String recordType) {
        return documentRepository.findByRecordIdAndRecordType(recordId, recordType);
    }

    @Transactional(readOnly = true)
    public List<Document> getDocumentsByCategory(Long recordId, String recordType, String category) {
        return documentRepository.findByRecordIdAndRecordTypeAndCategory(recordId, recordType, category);
    }

    @Transactional
    public Document upload(MultipartFile file, Long recordId, String recordType, String category) throws IOException {
        if (file.isEmpty()) {
            throw new BadRequestException("Failed to store empty file.");
        }

        Path docsDir = Paths.get(uploadRoot, "documents");
        if (!Files.exists(docsDir)) {
            Files.createDirectories(docsDir);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String filename = UUID.randomUUID().toString() + extension;
        Path filePath = docsDir.resolve(filename);

        if (encryptionUtil != null) {
            try {
                byte[] encrypted = encryptionUtil.encrypt(file.getInputStream().readAllBytes());
                Files.write(filePath, encrypted);
            } catch (java.security.GeneralSecurityException e) {
                throw new IOException("Failed to encrypt file", e);
            }
        } else {
            Files.copy(file.getInputStream(), filePath);
        }

        // Store relative path so the root can be changed via env var without a DB migration.
        String relativeFilePath = "documents/" + filename;

        Document document = Document.builder()
                .recordId(recordId)
                .recordType(recordType)
                .category(category)
                .filename(filename)
                .originalFilename(originalFilename)
                .filePath(relativeFilePath)
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

        try {
            Path filePath = Paths.get(uploadRoot).resolve(doc.getFilePath()).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Could not delete file: {}", doc.getFilePath());
        }

        documentRepository.delete(doc);
    }

    @Transactional(readOnly = true)
    public DocumentDownload getForDownload(Long id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        try {
            Path filePath = Paths.get(uploadRoot).resolve(doc.getFilePath()).normalize();
            if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
                throw new ResourceNotFoundException("File not found: " + doc.getOriginalFilename());
            }
            String contentType = doc.getFileType() != null ? doc.getFileType() : "application/octet-stream";
            String filename = doc.getOriginalFilename() != null ? doc.getOriginalFilename() : doc.getFilename();

            byte[] bytes = Files.readAllBytes(filePath);
            if (encryptionUtil != null && encryptionUtil.isEncrypted(bytes)) {
                try {
                    bytes = encryptionUtil.decrypt(bytes);
                    log.info("Decrypted document for download: {}", filename);
                } catch (java.security.GeneralSecurityException e) {
                    throw new IOException("Failed to decrypt file: " + filename, e);
                }
            }
            Resource resource = new NamedByteArrayResource(bytes, filename);
            return new DocumentDownload(resource, contentType, filename);
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File not found");
        } catch (IOException e) {
            throw new ResourceNotFoundException("Could not read file: " + e.getMessage());
        }
    }

    public record DocumentDownload(Resource resource, String contentType, String filename) {}
}
