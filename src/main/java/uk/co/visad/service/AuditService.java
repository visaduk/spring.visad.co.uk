package uk.co.visad.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.visad.entity.AuditLog;
import uk.co.visad.repository.AuditLogRepository;
import uk.co.visad.security.UserPrincipal;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void logChange(String recordType, Long recordId, String recordName,
                          String fieldChanged, String oldValue, String newValue) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        String username = "system";

        if (auth != null && auth.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            userId = principal.getId();
            username = principal.getUsername();
        }

        AuditLog log = AuditLog.builder()
                .userId(userId)
                .username(username)
                .recordType(recordType)
                .recordId(recordId)
                .recordName(recordName)
                .fieldChanged(fieldChanged)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();

        auditLogRepository.save(log);
    }

    public void logChange(Long userId, String username, String recordType, Long recordId,
                          String recordName, String fieldChanged, String oldValue, String newValue) {
        AuditLog log = AuditLog.builder()
                .userId(userId)
                .username(username)
                .recordType(recordType)
                .recordId(recordId)
                .recordName(recordName)
                .fieldChanged(fieldChanged)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();

        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByRecord(Long recordId, String recordType) {
        return auditLogRepository.findByRecordIdAndRecordTypeOrderByTimestampDesc(recordId, recordType);
    }
}
