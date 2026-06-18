package com.jo.agrisenseai;

/**
 * Data class representing a single farm notification.
 *
 * Stored in Firebase at:
 *   notifications/{notificationId}/
 *
 * Type constants are stable keys — future features (voice reading, hardware
 * LCD display, multilingual translation) reference TYPE_* to decide how to
 * present each notification without parsing display strings.
 */
public class NotificationModel {

    public static final String TYPE_WATER  = "water_required";
    public static final String TYPE_PUMP   = "pump_activated";
    public static final String TYPE_HEALTHY = "farm_healthy";
    public static final String TYPE_RISK   = "high_risk";
    public static final String TYPE_SYSTEM = "system_alert";

    private String notificationId;
    private String title;
    private String message;
    private String type;
    private long timestamp;
    private boolean isRead;

    // Required empty constructor for Firebase deserialization
    public NotificationModel() {
    }

    public NotificationModel(String notificationId, String title, String message,
                              String type, long timestamp) {
        this.notificationId = notificationId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.timestamp = timestamp;
        this.isRead = false;
    }

    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}
