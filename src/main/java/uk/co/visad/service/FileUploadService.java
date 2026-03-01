package uk.co.visad.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.co.visad.dto.locker.LockerDtos.FileUploadResponse;
import uk.co.visad.entity.Dependent;
import uk.co.visad.entity.Traveler;
import uk.co.visad.entity.TravelerQuestions;
import uk.co.visad.exception.ResourceNotFoundException;
import uk.co.visad.exception.UnauthorizedException;
import uk.co.visad.repository.DependentRepository;
import uk.co.visad.repository.TravelerQuestionsRepository;
import uk.co.visad.repository.TravelerRepository;
import uk.co.visad.util.FileEncryptionUtil;
import uk.co.visad.util.NamedByteArrayResource;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final TravelerRepository travelerRepository;
    private final DependentRepository dependentRepository;
    private final TravelerQuestionsRepository travelerQuestionsRepository;
    private final ObjectMapper objectMapper;
    private final LockerActivityService lockerActivityService;

    // New uploads go to:  uploadRoot/locker/YYYY/MM/
    @Value("${app.upload.root:/home/VisaD/visad.co.uk/vault_uploads}")
    private String uploadRoot;

    // Legacy PHP-era files live under the old vault.visad.co.uk/uploads/documents/ tree.
    // We only READ from here (download fallback) — never write.
    @Value("${app.upload.legacy-dir:/home/VisaD/visad.co.uk/vault.visad.co.uk/uploads/documents}")
    private String legacyDir;

    @Value("${app.upload.allowed-extensions:pdf,doc,docx,jpg,jpeg,png}")
    private String allowedExtensionsString;

    @Value("${app.upload.max-file-size:10485760}")
    private long maxFileSize;

    @Autowired(required = false)
    private FileEncryptionUtil encryptionUtil;

    private static final Map<String, String> FIELD_MAPPING = Map.of(
            "evisa_document_path", "evisaDocument",
            "share_code_document_path", "shareCodeDocument",
            "booking_documents_path", "bookingDocument",
            "passport_front", "passportFront",
            "passport_back", "passportBack",
            "schengen_visa_image", "schengenVisaImage");

    public FileUploadResponse uploadFiles(String token, String dbField, MultipartFile[] files) {
        log.info("Uploading {} files for field: {}", files.length, dbField);

        if (!FIELD_MAPPING.containsKey(dbField)) {
            throw new IllegalArgumentException("Invalid field: " + dbField);
        }

        RecordWrapper record = findRecordByToken(token);

        if (record.questions == null) {
            throw new ResourceNotFoundException("Questions record not found");
        }
        TravelerQuestions questions = record.questions;

        if (Boolean.TRUE.equals(questions.getFormComplete())) {
            throw new IllegalStateException("Application is locked");
        }

        String entityField = FIELD_MAPPING.get(dbField);

        List<String> existingFiles = getExistingFiles(questions, entityField);
        List<String> uploadedFiles = new ArrayList<>(existingFiles);
        List<String> errors = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            try {
                validateFile(file);
                String filename = saveFile(file, record.getId());
                uploadedFiles.add(filename);
            } catch (Exception e) {
                log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
                errors.add("Failed to upload " + file.getOriginalFilename() + ": " + e.getMessage());
            }
        }

        updateQuestionField(questions, entityField, uploadedFiles);
        travelerQuestionsRepository.save(questions);
        lockerActivityService.record(token, "FILE_UPLOADED",
                "Uploaded " + (uploadedFiles.size() - existingFiles.size()) + " file(s) to: " + dbField);

        return FileUploadResponse.builder()
                .filenames(uploadedFiles)
                .errors(errors)
                .build();
    }

    public List<String> deleteFile(String token, String dbField, String filename) {
        log.info("Deleting file: field={}, filename={}", dbField, filename);

        if (!FIELD_MAPPING.containsKey(dbField)) {
            throw new IllegalArgumentException("Invalid field: " + dbField);
        }

        RecordWrapper record = findRecordByToken(token);
        TravelerQuestions questions = record.questions;

        if (questions == null) throw new ResourceNotFoundException("Questions not found");

        if (Boolean.TRUE.equals(questions.getFormComplete())) {
            throw new IllegalStateException("Application is locked");
        }

        String entityField = FIELD_MAPPING.get(dbField);
        List<String> existingFiles = getExistingFiles(questions, entityField);

        if (existingFiles.remove(filename)) {
            try {
                Path lockerBase = Paths.get(uploadRoot, "locker").normalize();
                Path filePath = lockerBase.resolve(filename).normalize();
                if (filePath.startsWith(lockerBase)) {
                    Files.deleteIfExists(filePath);
                    log.info("Deleted physical file: {}", filename);
                }
            } catch (IOException e) {
                log.error("Failed to delete physical file: {}", filename, e);
            }

            updateQuestionField(questions, entityField, existingFiles);
            travelerQuestionsRepository.save(questions);
            lockerActivityService.record(token, "FILE_DELETED", "Deleted file in: " + dbField);
        }

        return existingFiles;
    }

    public Resource getFileAsResource(String token, String filename) {
        findRecordByToken(token);

        try {
            // Primary: new unified location  →  uploadRoot/locker/YYYY/MM/filename
            Path lockerBase = Paths.get(uploadRoot, "locker").normalize();
            Path filePath = lockerBase.resolve(filename).normalize();
            if (!filePath.startsWith(lockerBase)) {
                throw new UnauthorizedException("Invalid file path");
            }
            if (Files.exists(filePath)) {
                byte[] bytes = Files.readAllBytes(filePath);
                if (encryptionUtil != null && encryptionUtil.isEncrypted(bytes)) {
                    try {
                        bytes = encryptionUtil.decrypt(bytes);
                        log.info("Decrypted file for download: {}", filename);
                    } catch (java.security.GeneralSecurityException e) {
                        throw new IOException("Failed to decrypt file: " + filename, e);
                    }
                }
                String leafName = filePath.getFileName().toString();
                return new NamedByteArrayResource(bytes, leafName);
            }

            // Fallback: legacy PHP-era category folders under legacyDir
            Path legacyBase = Paths.get(legacyDir).normalize();
            String[] legacyFolders = {"bookings", "evisa", "share_code", "flight", "hotel",
                                      "insurance", "application", "appointment", "forms",
                                      "client_documents"};
            for (String folder : legacyFolders) {
                Path legacyPath = legacyBase.resolve(folder).resolve(filename).normalize();
                if (!legacyPath.startsWith(legacyBase)) continue;
                Resource legacyResource = new UrlResource(legacyPath.toUri());
                if (legacyResource.exists() && legacyResource.isReadable()) {
                    log.info("Served legacy file from {}/{}", folder, filename);
                    return legacyResource;
                }
            }

            // Last-resort fallback: legacy client_documents with year/month sub-dirs
            Path clientDocsBase = legacyBase.resolve("client_documents").normalize();
            Path cdFilePath = clientDocsBase.resolve(filename).normalize();
            if (cdFilePath.startsWith(clientDocsBase)) {
                Resource cdResource = new UrlResource(cdFilePath.toUri());
                if (cdResource.exists() && cdResource.isReadable()) {
                    log.info("Served pre-consolidation file from client_documents/{}", filename);
                    return cdResource;
                }
            }

            throw new ResourceNotFoundException("File not found: " + filename);
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File not found: " + filename);
        } catch (IOException e) {
            throw new ResourceNotFoundException("Could not read file: " + e.getMessage());
        }
    }

    // --- Helpers ---

    private void validateFile(MultipartFile file) {
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File too large");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty())
            throw new IllegalArgumentException("Invalid filename");

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        Set<String> allowed = Arrays.stream(allowedExtensionsString.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());

        if (!allowed.contains(extension)) {
            throw new IllegalArgumentException("Invalid file type: " + extension);
        }
    }

    private String saveFile(MultipartFile file, Long recordId) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.lastIndexOf(".") >= 0) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String uniqueFilename = recordId + "_" + System.currentTimeMillis() + "_"
                + UUID.randomUUID().toString().substring(0, 8) + extension;

        LocalDate now = LocalDate.now();
        String year = String.valueOf(now.getYear());
        String month = String.format("%02d", now.getMonthValue());

        Path uploadDir = Paths.get(uploadRoot, "locker", year, month);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        Path filePath = uploadDir.resolve(uniqueFilename);

        if (encryptionUtil != null) {
            try {
                byte[] encrypted = encryptionUtil.encrypt(file.getInputStream().readAllBytes());
                Files.write(filePath, encrypted);
                log.info("Encrypted and saved file: {}", uniqueFilename);
            } catch (java.security.GeneralSecurityException e) {
                throw new IOException("Failed to encrypt file", e);
            }
        } else {
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        // Stored key is relative to uploadRoot/locker/ so the download path
        // resolves correctly via: lockerBase.resolve("YYYY/MM/filename")
        return year + "/" + month + "/" + uniqueFilename;
    }

    private List<String> getExistingFiles(TravelerQuestions questions, String entityField) {
        String json = null;
        switch (entityField) {
            case "evisaDocument":      json = questions.getEvisaDocument(); break;
            case "shareCodeDocument":  json = questions.getShareCodeDocument(); break;
            case "bookingDocument":    json = questions.getBookingDocument(); break;
            case "passportFront":      json = questions.getPassportFront(); break;
            case "passportBack":       json = questions.getPassportBack(); break;
            case "schengenVisaImage":  json = questions.getSchengenVisaImage(); break;
        }

        if (json == null || json.trim().isEmpty()) return new ArrayList<>();

        try {
            if (json.startsWith("[")) {
                return objectMapper.readValue(json, new TypeReference<List<String>>() {});
            } else {
                return new ArrayList<>(Collections.singletonList(json));
            }
        } catch (Exception e) {
            log.warn("Failed to parse file list: {}", json);
            return new ArrayList<>(Collections.singletonList(json));
        }
    }

    private void updateQuestionField(TravelerQuestions questions, String entityField, List<String> files) {
        try {
            String val = files.isEmpty() ? null : objectMapper.writeValueAsString(files);

            switch (entityField) {
                case "evisaDocument":      questions.setEvisaDocument(val); break;
                case "shareCodeDocument":  questions.setShareCodeDocument(val); break;
                case "bookingDocument":    questions.setBookingDocument(val); break;
                case "passportFront":      questions.setPassportFront(val); break;
                case "passportBack":       questions.setPassportBack(val); break;
                case "schengenVisaImage":  questions.setSchengenVisaImage(val); break;
            }
        } catch (Exception e) {
            log.error("Failed to update field", e);
            throw new RuntimeException("Failed to update field");
        }
    }

    private static class RecordWrapper {
        TravelerQuestions questions;
        Long recordId;

        Long getId() { return recordId; }
    }

    private RecordWrapper findRecordByToken(String token) {
        RecordWrapper wrapper = new RecordWrapper();
        Optional<Traveler> traveler = travelerRepository.findByPublicUrlToken(token);
        if (traveler.isPresent()) {
            wrapper.recordId = traveler.get().getId();
            wrapper.questions = travelerQuestionsRepository.findByRecordIdAndRecordType(wrapper.recordId, "traveler")
                    .orElse(null);
            return wrapper;
        }

        Optional<Dependent> dependent = dependentRepository.findByPublicUrlToken(token);
        if (dependent.isPresent()) {
            wrapper.recordId = dependent.get().getId();
            wrapper.questions = travelerQuestionsRepository.findByRecordIdAndRecordType(wrapper.recordId, "dependent")
                    .orElse(null);
            return wrapper;
        }
        throw new UnauthorizedException("Invalid token");
    }
}
