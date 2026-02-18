package uk.co.visad.model.presence;

import java.time.Instant;

public class Activity {
    private ActivityType type;
    private TargetType targetType;
    private String targetId;
    private Instant timestamp;

    public enum ActivityType {
        IDLE, VIEWING, EDITING
    }

    public enum TargetType {
        TRAVELER, DEPENDENT, INVOICE
    }

    // Constructors, Getters, Setters
    public Activity() {}

    public Activity(ActivityType type, TargetType targetType, String targetId) {
        this.type = type;
        this.targetType = targetType;
        this.targetId = targetId;
        this.timestamp = Instant.now();
    }

    public ActivityType getType() { return type; }
    public void setType(ActivityType type) { this.type = type; }

    public TargetType getTargetType() { return targetType; }
    public void setTargetType(TargetType targetType) { this.targetType = targetType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    // Optional: Description of what is being done (e.g. field name)
    private String description;
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
