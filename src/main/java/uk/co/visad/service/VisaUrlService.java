package uk.co.visad.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uk.co.visad.dto.VisaUrlDto;
import uk.co.visad.entity.VisaUrl;
import uk.co.visad.exception.BadRequestException;
import uk.co.visad.exception.ResourceNotFoundException;
import uk.co.visad.repository.VisaUrlRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisaUrlService {

    private final VisaUrlRepository visaUrlRepository;

    @Value("${app.upload.forms-dir:/home/VisaD/visad.co.uk/vault_uploads/forms}")
    private String formsDir;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx");

    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable("visa_urls")
    public List<VisaUrlDto> getAllUrls() {
        return visaUrlRepository.findAllByOrderByCountryAscVisaCenterAsc()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "visa_urls", allEntries = true)
    public void createUrl(VisaUrlDto.CreateRequest request, MultipartFile file) throws IOException {
        if (request.getCountry() == null || request.getCountry().isEmpty()) {
            throw new BadRequestException("Country is required");
        }
        if (request.getUrl() == null || request.getUrl().isEmpty()) {
            throw new BadRequestException("URL is required");
        }

        String applicationFormUrl = request.getApplicationFormUrl();
        boolean isUploadedFile = false;

        // Handle file upload
        if (file != null && !file.isEmpty()) {
            applicationFormUrl = handleFileUpload(file);
            isUploadedFile = true;
        }

        VisaUrl visaUrl = VisaUrl.builder()
                .country(request.getCountry())
                .visaCenter(request.getVisaCenter())
                .url(request.getUrl())
                .applicationFormUrl(applicationFormUrl)
                .isUploadedFile(isUploadedFile)
                .build();

        visaUrlRepository.save(visaUrl);
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "visa_urls", allEntries = true)
    public void updateUrl(VisaUrlDto.UpdateRequest request, MultipartFile file) throws IOException {
        VisaUrl visaUrl = visaUrlRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("URL not found"));

        if (request.getCountry() == null || request.getCountry().isEmpty()) {
            throw new BadRequestException("Country is required");
        }
        if (request.getUrl() == null || request.getUrl().isEmpty()) {
            throw new BadRequestException("URL is required");
        }

        String oldFormUrl = visaUrl.getApplicationFormUrl();
        boolean wasUploadedFile = Boolean.TRUE.equals(visaUrl.getIsUploadedFile());

        String applicationFormUrl = request.getApplicationFormUrl();
        boolean isUploadedFile = wasUploadedFile;

        // Handle new file upload
        if (file != null && !file.isEmpty()) {
            // Delete old file if it was uploaded
            if (wasUploadedFile && oldFormUrl != null) {
                deleteFile(oldFormUrl);
            }
            applicationFormUrl = handleFileUpload(file);
            isUploadedFile = true;
        } else if (applicationFormUrl != null && !applicationFormUrl.equals(oldFormUrl)) {
            // Text URL provided, replacing file
            if (wasUploadedFile && oldFormUrl != null) {
                deleteFile(oldFormUrl);
            }
            isUploadedFile = false;
        } else if (applicationFormUrl == null || applicationFormUrl.isEmpty()) {
            // Keep existing if file
            if (wasUploadedFile) {
                applicationFormUrl = oldFormUrl;
            }
        }

        visaUrl.setCountry(request.getCountry());
        visaUrl.setVisaCenter(request.getVisaCenter());
        visaUrl.setUrl(request.getUrl());
        visaUrl.setApplicationFormUrl(applicationFormUrl);
        visaUrl.setIsUploadedFile(isUploadedFile);

        visaUrlRepository.save(visaUrl);
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "visa_urls", allEntries = true)
    public void deleteUrl(Long id) {
        VisaUrl visaUrl = visaUrlRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found"));

        // Delete uploaded file if exists
        if (Boolean.TRUE.equals(visaUrl.getIsUploadedFile()) && visaUrl.getApplicationFormUrl() != null) {
            deleteFile(visaUrl.getApplicationFormUrl());
        }

        visaUrlRepository.delete(visaUrl);
    }

    private String handleFileUpload(MultipartFile file) throws IOException {
        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BadRequestException("Invalid file type. Only PDF, DOC, and DOCX are allowed.");
        }

        Path uploadPath = Paths.get(formsDir);
        Files.createDirectories(uploadPath);

        String uniqueFilename = "form_" + UUID.randomUUID().toString() + "." + extension;
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath);

        return "uploads/forms/" + uniqueFilename;
    }

    private void deleteFile(String relativePath) {
        try {
            Path filePath = Paths.get(formsDir).getParent().resolve(relativePath);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", relativePath, e);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null)
            return "";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }

    private VisaUrlDto mapToDto(VisaUrl entity) {
        return VisaUrlDto.builder()
                .id(entity.getId())
                .country(entity.getCountry())
                .visaCenter(entity.getVisaCenter())
                .url(entity.getUrl())
                .applicationFormUrl(entity.getApplicationFormUrl())
                .isUploadedFile(entity.getIsUploadedFile())
                .build();
    }
}
