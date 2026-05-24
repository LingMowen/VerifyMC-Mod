package com.verifymc.whitelist;

import com.verifymc.db.User;
import com.verifymc.db.UserDao;
import com.verifymc.VerifyMC;

import java.util.List;
import java.util.UUID;

public class WhitelistManager {
    
    private final UserDao userDao;
    
    public WhitelistManager(UserDao userDao) {
        this.userDao = userDao;
    }
    
    public boolean isWhitelisted(String username) {
        return userDao.findByUsername(username)
                .map(User::isApproved)
                .orElse(false);
    }
    
    public void approveUser(UUID userId) {
        userDao.updateStatus(userId, User.UserStatus.APPROVED);
        VerifyMC.LOGGER.info("User approved: {}", userId);
    }
    
    public void rejectUser(UUID userId) {
        rejectUser(userId, null);
    }
    
    public void rejectUser(UUID userId, String reason) {
        userDao.updateStatus(userId, User.UserStatus.REJECTED, reason);
        VerifyMC.LOGGER.info("User rejected: {} - Reason: {}", userId, reason != null ? reason : "N/A");
    }
    
    public List<User> getPendingUsers() {
        return userDao.findByStatus(User.UserStatus.PENDING);
    }
    
    public List<User> getApprovedUsers() {
        return userDao.findByStatus(User.UserStatus.APPROVED);
    }
    
    public void banUser(UUID userId, String reason, long durationMinutes) {
        userDao.banUser(userId, reason, durationMinutes);
        VerifyMC.LOGGER.info("User banned: {} - {}", userId, reason);
    }
    
    public void unbanUser(UUID userId) {
        userDao.unbanUser(userId);
        VerifyMC.LOGGER.info("User unbanned: {}", userId);
    }
    
    public List<User> getBannedUsers() {
        return userDao.findBanned();
    }
    
    public void cleanupExpiredBans() {
        userDao.cleanupExpiredBans();
    }
}
