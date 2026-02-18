package uk.co.visad.model.presence;

import java.time.Instant;

public class SessionState {
    private String sessionId;
    private String userId;
    private String username;
    private Activity currentActivity;
    private Instant lastHeartbeat;
    private boolean isConnected;

    public SessionState(String sessionId, String userId, String username) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.username = username;
        this.lastHeartbeat = Instant.now();
        this.isConnected = true;
        this.currentActivity = new Activity(Activity.ActivityType.IDLE, null, null);
    }

    // Getters and Setters
    public String getSessionId() { return sessionId; }
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    
    public Activity getCurrentActivity() { return currentActivity; }
    public void setCurrentActivity(Activity currentActivity) { this.currentActivity = currentActivity; }

    public Instant getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(Instant lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }

    public boolean isConnected() { return isConnected; }
    public void setConnected(boolean connected) { isConnected = connected; }
}
