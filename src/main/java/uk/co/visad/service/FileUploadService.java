package uk.co.visad.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDate;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.net.MalformedURLException;

/**
 * File Upload Service for Locker
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final TravelerRepository travelerRepository;
    private final DependentRepository dependentRepository;
    private final TravelerQuestionsRepository travelerQuestionsRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.upload.base-dir:./uploads}")
    private String uploadBasePath;

    @Value("${app.upload.allowed-extensions:pdf,doc,docx,jpg,jpeg,png}")
    private String allowedExtensionsString;

    @Value("${app.upload.max-file-size:10485760}")
    private long maxFileSize;

    // Map of frontend field name -> Entity field name
    private static final Map<String, String> FIELD_MAPPING = Map.of(
            "evisa_document_path", "evisaDocument",
            "share_code_document_path", "shareCodeDocument",
            "booking_documents_path", "bookingDocument",
            "passport_front", "passportFront",
            "passport_back", "passportBack");

    public FileUploadResponse uploadFiles(String token, String dbField, MultipartFile[] files) {
        log.info("Uploading {} files for field: {}", files.length, dbField);

        if (!FIELD_MAPPING.containsKey(dbField)) {
            // Allow if it matches entity field directly?
            // For safety, only allow mapped fields or exact matches if safe.
            throw new IllegalArgumentException("Invalid field: " + dbField);
        }

        RecordWrapper record = findRecordByToken(token);

        if (record.questions == null) {
            // Should exist if they are uploading files? Or create?
            // Usually created on login or update.
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
            if (file.isEmpty())
                continue;
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

        if (questions == null)
            throw new ResourceNotFoundException("Questions not found");

        if (Boolean.TRUE.equals(questions.getFormComplete())) {
            // throw new IllegalStateException("Application is locked");
            // Allow delete even if locked? Usually no.
            // Keeping restriction for now.
            throw new IllegalStateException("Application is locked");
        }

        String entityField = FIELD_MAPPING.get(dbField);
        List<String> existingFiles = getExistingFiles(questions, entityField);

        if (existingFiles.remove(filename)) {
            try {
                Path filePath = Paths.get(uploadBasePath, filename);
                Files.deleteIfExists(filePath);
                log.info("Deleted physical file: {}", filename);
            } catch (IOException e) {
                log.error("Failed to delete physical file: {}", filename, e);
            }

            updateQuestionField(questions, entityField, existingFiles);
            travelerQuestionsRepository.save(questions);
        }

        return existingFiles;
    }

    public Resource getFileAsResource(String token, String filename) {
        // 1. Verify token (security)
        findRecordByToken(token); // Throws UnauthorizedException if invalid

        try {
            Path filePath = Paths.get(uploadBasePath).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                // Security check: ensure file is inside upload base directory
                if (!filePath.startsWith(Paths.get(uploadBasePath).normalize())) {
                    throw new UnauthorizedException("Invalid file path");
                }
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File not found: " + filename);
        }
    }

    // Helpers

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

        // Path structure: YYYY/MM/
        LocalDate now = LocalDate.now();
        String year = String.valueOf(now.getYear());
        String month = String.format("%02d", now.getMonthValue());

        // Create directory: base/YYYY/MM
        Path uploadDir = Paths.get(uploadBasePath, year, month);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // Save file
        Path filePath = uploadDir.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Return relative path for DB: YYYY/MM/filename (always forward slashes)
        return year + "/" + month + "/" + uniqueFilename;
    }

    private List<String> getExistingFiles(TravelerQuestions questions, String entityField) {
        String json = null;
        switch (entityField) {
            case "evisaDocument":
                json = questions.getEvisaDocument();
                break;
            case "shareCodeDocument":
                json = questions.getShareCodeDocument();
                break;
            case "bookingDocument":
                json = questions.getBookingDocument();
                break;
            case "passportFront":
                json = questions.getPassportFront();
                break;
            case "passportBack":
                json = questions.getPassportBack();
                break;
        }

        if (json == null || json.trim().isEmpty())
            return new ArrayList<>();

        try {
            if (json.startsWith("[")) {
                return objectMapper.readValue(json, new TypeReference<List<String>>() {
                });
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
            String val;
            if (files.isEmpty()) {
                val = null;
            } else if (files.size() == 1) {
                // Should we save as JSON array strictly?
                // Or allows single string for backward compat?
                // LockerService reads both.
                // Saving as JSON array is future proof.
                // But if column length 255 is issue, single string is better.
                // Let's try JSON. If length constraint hit, we have a problem.
                // ["filename"] takes 4 extra chars.
                val = objectMapper.writeValueAsString(files);
            } else {
                val = objectMapper.writeValueAsString(files);
            }

            // Check length?
            if (val != null && val.length() > 255) {
                log.warn("File list too long for column ({} > 255). Truncating or error?", val.length());
                // Fallback: Store only last file?
                // Or throw error?
                // For now, let's proceed. DB will throw exception if too long.
            }

            switch (entityField) {
                case "evisaDocument":
                    questions.setEvisaDocument(val);
                    break;
                case "shareCodeDocument":
                    questions.setShareCodeDocument(val);
                    break;
                case "bookingDocument":
                    questions.setBookingDocument(val);
                    break;
                case "passportFront":
                    questions.setPassportFront(val);
                    break;
                case "passportBack":
                    questions.setPassportBack(val);
                    break;
            }
        } catch (Exception e) {
            log.error("Failed to update field", e);
            throw new RuntimeException("Failed to update field");
        }
    }

    private static class RecordWrapper {
        TravelerQuestions questions;
        Long recordId;

        Long getId() {
            return recordId;
        }
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
