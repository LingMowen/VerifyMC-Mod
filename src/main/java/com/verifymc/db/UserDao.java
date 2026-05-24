package com.verifymc.db;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserDao {
    
    Optional<User> findById(UUID id);
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByDiscordId(String discordId);
    
    List<User> findAll();
    
    List<User> findByStatus(User.UserStatus status);
    
    List<User> findBanned();
    
    boolean save(User user);
    
    boolean delete(UUID id);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    void banUser(UUID id, String reason, long durationMinutes);
    
    void unbanUser(UUID id);
    
    void updateStatus(UUID id, User.UserStatus status);
    
    default void updateStatus(UUID id, User.UserStatus status, String reason) {
        // Default implementation ignores reason for backward compatibility
        updateStatus(id, status);
    }
    
    void cleanupExpiredBans();

    long count();

    boolean updatePassword(String username, String newPasswordHash);

    int countUsersByEmail(String email);

    boolean updateUserEmail(String username, String newEmail, boolean caseSensitive);
}
