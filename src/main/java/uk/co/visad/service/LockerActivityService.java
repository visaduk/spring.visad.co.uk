package uk.co.visad.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import uk.co.visad.entity.LockerActivity;
import uk.co.visad.entity.Traveler;
import uk.co.visad.repository.LockerActivityRepository;
import uk.co.visad.repository.TravelerRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LockerActivityService {

    private final LockerActivityRepository repo;
    private final TravelerRepository travelerRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Records a locker form activity event:
     * - Resolves the traveler by public URL token
     * - Persists to locker_activities
     * - Broadcasts to /topic/locker-activity for real-time admin notification
     */
    public void record(String token, String eventType, String detail) {
        Traveler traveler = travelerRepository.findByPublicUrlToken(token).orElse(null);
        if (traveler == null) {
            log.warn("logActivity: unknown token '{}'", token.substring(0, Math.min(8, token.length())));
            return;
        }

        LockerActivity activity = LockerActivity.builder()
                .travelerId(traveler.getId())
                .travelerName(traveler.getFirstName() + " " + traveler.getLastName())
                .token(token.substring(0, Math.min(8, token.length())))
                .eventType(eventType)
                .detail(detail)
                .createdAt(LocalDateTime.now())
                .build();
        repo.save(activity);

        Map<String, Object> msg = Map.of(
                "travelerId",   activity.getTravelerId(),
                "travelerName", activity.getTravelerName(),
                "token",        activity.getToken(),
                "eventType",    activity.getEventType(),
                "detail",       activity.getDetail() != null ? activity.getDetail() : "",
                "timestamp",    activity.getCreatedAt().toString()
        );
        messagingTemplate.convertAndSend("/topic/locker-activity", msg);
        log.info("Locker activity [{}] {} – {}", eventType, activity.getTravelerName(), detail);
    }

    public List<LockerActivity> getRecent() {
        return repo.findTop50ByOrderByCreatedAtDesc();
    }
}
