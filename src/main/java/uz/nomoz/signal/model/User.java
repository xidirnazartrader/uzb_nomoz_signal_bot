package uz.nomoz.signal.model;

import java.sql.Timestamp;

public class User {
    private long chatId;
    private String region;
    private String district;
    private boolean notificationsEnabled;
    private int reminderMinutes;
    private Timestamp updatedAt;

    public User() {}

    public User(long chatId, String region, String district, boolean notificationsEnabled, int reminderMinutes) {
        this.chatId = chatId;
        this.region = region;
        this.district = district;
        this.notificationsEnabled = notificationsEnabled;
        this.reminderMinutes = reminderMinutes;
    }

    public long getChatId() { return chatId; }
    public void setChatId(long chatId) { this.chatId = chatId; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }

    public int getReminderMinutes() { return reminderMinutes; }
    public void setReminderMinutes(int reminderMinutes) { this.reminderMinutes = reminderMinutes; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "User{" +
                "chatId=" + chatId +
                ", region='" + region + '\'' +
                ", district='" + district + '\'' +
                ", notificationsEnabled=" + notificationsEnabled +
                ", reminderMinutes=" + reminderMinutes +
                '}';
    }
}
