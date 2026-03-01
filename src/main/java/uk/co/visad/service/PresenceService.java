package uk.co.visad.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.co.visad.model.presence.Activity;
import uk.co.visad.model.presence.SessionState;
import uk.co.visad.model.presence.UserPresence;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PresenceService {

    private final SimpMessagingTemplate messagingTemplate;
    
    // Key: UserId, Value: UserPresence
    private final Map<String, UserPresence> presences = new ConcurrentHashMap<>();
    
    // Reverse lookup: Key: SessionId, Value: UserId
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();

    private static final long HEARTBEAT_TIMEOUT_SECONDS = 90;

    public void registerSession(String sessionId, String userId, String username) {
        log.info("Registering session: {} for user: {}", sessionId, username);
        presences.compute(userId, (key, existing) -> {
            if (existing == null) {
                existing = new UserPresence(userId, username);
            }
            existing.addSession(new SessionState(sessionId, userId, username));
            return existing;
        });
        sessionUserMap.put(sessionId, userId);

        broadcastUpdate(userId, sessionId, "CONNECTED", null);
    }

    public void removeSession(String sessionId) {
        String userId = sessionUserMap.remove(sessionId);
        if (userId != null) {
            log.info("Removing session: {} for user: {}", sessionId, userId);
            // Track whether this user went fully offline so we can broadcast AFTER map removal
            final String[] offlineUsername = {null};
            presences.computeIfPresent(userId, (key, userPresence) -> {
                userPresence.removeSession(sessionId);
                if (!userPresence.isOnline()) {
                    offlineUsername[0] = userPresence.getUsername();
                    return null; // Remove user from map
                }
                return userPresence;
            });
            // Broadcast AFTER removal so activeUserCount/activeUsernames are accurate
            if (offlineUsername[0] != null) {
                broadcastUpdate(userId, sessionId, "OFFLINE", null, offlineUsername[0]);
            }
        }
    }

    public void updateActivity(String sessionId, Activity activity) {
        String userId = sessionUserMap.get(sessionId);
        if (userId != null) {
            UserPresence userPresence = presences.get(userId);
            if (userPresence != null) {
                SessionState session = userPresence.getSessions().get(sessionId);
                if (session != null) {
                    session.setCurrentActivity(activity);
                    session.setLastHeartbeat(Instant.now());
                    
                    broadcastUpdate(userId, sessionId, "ACTIVITY", activity);
                }
            }
        }
    }

    public void handleHeartbeat(String sessionId) {
        String userId = sessionUserMap.get(sessionId);
        if (userId != null) {
            UserPresence userPresence = presences.get(userId);
            if (userPresence != null) {
                SessionState session = userPresence.getSessions().get(sessionId);
                if (session != null) {
                    session.setLastHeartbeat(Instant.now());
                }
            }
        }
    }

    public Map<String, Object> getSnapshot() {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("type", "SNAPSHOT");
        
        List<Map<String, Object>> userList = new ArrayList<>();
        
        presences.values().forEach(up -> {
            Map<String, Object> userData = new HashMap<>();
            userData.put("userId", up.getUserId());
            userData.put("username", up.getUsername());
            userData.put("status", "ONLINE"); // Simplified for MVP
            
            List<Map<String, Object>> activities = up.getSessions().values().stream()
                    .map(s -> {
                        Map<String, Object> act = new HashMap<>();
                        act.put("sessionId", s.getSessionId());
                        act.put("activity", s.getCurrentActivity());
                        return act;
                    }).collect(Collectors.toList());
            
            userData.put("activities", activities);
            userList.add(userData);
        });
        
        snapshot.put("users", userList);
        snapshot.put("activeUserCount", getActiveUserCount());
        snapshot.put("activeUsernames", getActiveUsernames());
        return snapshot;
    }
    
    public void sendSnapshotToUser(String username) {
        // Send to specific user queue
        messagingTemplate.convertAndSendToUser(username, "/queue/presence", getSnapshot());
    }

    /**
     * Stateful count of distinct connected users — derived from the presences map.
     * Included in every broadcast so all clients stay in sync.
     */
    public int getActiveUserCount() {
        return presences.size();
    }

    public List<String> getActiveUsernames() {
        return presences.values().stream()
                .map(UserPresence::getUsername)
                .collect(Collectors.toList());
    }

    private void broadcastUpdate(String userId, String sessionId, String status, Activity activity) {
        broadcastUpdate(userId, sessionId, status, activity, null);
    }

    private void broadcastUpdate(String userId, String sessionId, String status, Activity activity, String explicitUsername) {
        Map<String, Object> update = new HashMap<>();
        update.put("type", "UPDATE");
        update.put("userId", userId);
        update.put("sessionId", sessionId);
        update.put("status", status);
        // Include username — use explicit if provided (user already removed from map), else look up
        if (explicitUsername != null) {
            update.put("username", explicitUsername);
        } else {
            UserPresence up = presences.get(userId);
            if (up != null) {
                update.put("username", up.getUsername());
            }
        }
        if (activity != null) {
            update.put("activity", activity);
        }

        // Always include authoritative active-user count + names
        update.put("activeUserCount", getActiveUserCount());
        update.put("activeUsernames", getActiveUsernames());

        messagingTemplate.convertAndSend("/topic/presence", update);
    }

    @Scheduled(fixedRate = 60000) // Run every minute
    public void performCleanup() {
        log.debug("Running presence cleanup task");
        Instant now = Instant.now();
        
        presences.values().forEach(userPresence -> {
            List<String> expiredSessions = userPresence.getSessions().values().stream()
                    .filter(s -> ChronoUnit.SECONDS.between(s.getLastHeartbeat(), now) > HEARTBEAT_TIMEOUT_SECONDS)
                    .map(SessionState::getSessionId)
                    .collect(Collectors.toList());
            
            expiredSessions.forEach(this::removeSession);
        });
    }
}
