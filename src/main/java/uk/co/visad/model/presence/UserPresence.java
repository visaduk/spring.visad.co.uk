package uk.co.visad.model.presence;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserPresence {
    private String userId;
    private String username;
    private Map<String, SessionState> sessions = new ConcurrentHashMap<>();

    public UserPresence(String userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public Map<String, SessionState> getSessions() { return sessions; }

    public void addSession(SessionState session) {
        sessions.put(session.getSessionId(), session);
    }

    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
    }

    public boolean isOnline() {
        return !sessions.isEmpty();
    }
}
