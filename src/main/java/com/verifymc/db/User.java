package com.verifymc.db;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class User {
    private UUID id;
    private String username;
    private String email;
    private String passwordHash;
    private String salt;
    private UserStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private String discordId;
    private boolean banned;
    private String banReason;
    private Instant bannedAt;
    private Instant banExpiresAt;
    private List<String> questionnaireAnswers;
    private String rejectReason;

    public enum UserStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    public User() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.status = UserStatus.PENDING;
        this.banned = false;
        this.questionnaireAnswers = new ArrayList<>();
    }

    public User(String username, String email, String passwordHash, String salt) {
        this();
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.salt = salt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    // Alias methods for compatibility
    public String getPassword() { return passwordHash; }
    public void setPassword(String password) {
        this.passwordHash = password;
        this.updatedAt = Instant.now();
    }

    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getDiscordId() { return discordId; }
    public void setDiscordId(String discordId) { this.discordId = discordId; }

    public boolean isBanned() { return banned; }
    public void setBanned(boolean banned) { this.banned = banned; }

    public String getBanReason() { return banReason; }
    public void setBanReason(String banReason) { this.banReason = banReason; }

    public Instant getBannedAt() { return bannedAt; }
    public void setBannedAt(Instant bannedAt) { this.bannedAt = bannedAt; }

    public Instant getBanExpiresAt() { return banExpiresAt; }
    public void setBanExpiresAt(Instant banExpiresAt) { this.banExpiresAt = banExpiresAt; }

    // Alias for MySQL DAO compatibility
    public Instant getBanExpiry() { return banExpiresAt; }
    public void setBanExpiry(Instant banExpiry) { this.banExpiresAt = banExpiry; }

    public List<String> getQuestionnaireAnswers() { return questionnaireAnswers; }
    public void setQuestionnaireAnswers(List<String> answers) { this.questionnaireAnswers = answers != null ? answers : new ArrayList<>(); }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public boolean isApproved() {
        return status == UserStatus.APPROVED && !banned;
    }

    public boolean isBanExpired() {
        if (!banned || banExpiresAt == null) {
            return false;
        }
        return Instant.now().isAfter(banExpiresAt);
    }
}
