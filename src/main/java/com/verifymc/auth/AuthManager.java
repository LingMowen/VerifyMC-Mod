package com.verifymc.auth;

import com.verifymc.core.OpsManager;
import com.verifymc.db.User;
import com.verifymc.db.UserDao;
import com.verifymc.VerifyMC;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AuthManager {
    
    private final UserDao userDao;
    private final Map<String, UUID> sessionTokens;
    private final Set<String> adminTokens;
    private OpsManager opsManager;
    
    public AuthManager(UserDao userDao) {
        this.userDao = userDao;
        this.sessionTokens = new HashMap<>();
        this.adminTokens = new HashSet<>();
    }
    
    public void setOpsManager(OpsManager opsManager) {
        this.opsManager = opsManager;
    }
    
    public AuthResult register(String username, String email, String password) {
        if (userDao.existsByUsername(username)) {
            return new AuthResult(false, "Username already exists", null, false);
        }
        
        if (userDao.existsByEmail(email)) {
            return new AuthResult(false, "Email already registered", null, false);
        }
        
        String salt = PasswordUtil.generateSalt();
        String passwordHash = PasswordUtil.hashPassword(password, salt);
        
        User user = new User(username, email, passwordHash, salt);
        userDao.save(user);
        
        String token = generateToken();
        sessionTokens.put(token, user.getId());
        
        // Check if user is admin (in case they are in ops.json)
        boolean isAdmin = opsManager != null && opsManager.isOp(user.getUsername());
        if (isAdmin) {
            adminTokens.add(token);
        }
        
        VerifyMC.LOGGER.info("User registered: {} (admin: {})", username, isAdmin);
        return new AuthResult(true, "Registration successful", token, isAdmin);
    }
    
    public AuthResult login(String username, String password) {
        return login(username, password, false);
    }
    
    public AuthResult login(String username, String password, boolean isAdminLogin) {
        var userOpt = userDao.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            return new AuthResult(false, "Invalid username or password", null, false);
        }
        
        User user = userOpt.get();
        
        // Check if admin login is requested but user is not an op
        if (isAdminLogin) {
            if (opsManager == null || opsManager.getOps().isEmpty()) {
                VerifyMC.LOGGER.warn("Admin login attempted but no ops configured in ops.json");
                return new AuthResult(false, "System configuration error, please contact the server administrator", null, false);
            }
            if (!opsManager.isOp(user.getUsername())) {
                VerifyMC.LOGGER.warn("Non-op user attempted admin login: {}", username);
                return new AuthResult(false, "You do not have administrator privileges", null, false);
            }
        }
        
        if (user.isBanned() && !user.isBanExpired()) {
            return new AuthResult(false, "Account is banned: " + user.getBanReason(), null, false);
        }
        
        if (!PasswordUtil.verifyPassword(password, user.getSalt(), user.getPasswordHash())) {
            return new AuthResult(false, "Invalid username or password", null, false);
        }
        
        String token = generateToken();
        sessionTokens.put(token, user.getId());
        
        // Check if user is admin
        boolean isAdmin = opsManager != null && opsManager.isOp(user.getUsername());
        if (isAdmin) {
            adminTokens.add(token);
        }
        
        VerifyMC.LOGGER.info("User logged in: {} (admin: {})", username, isAdmin);
        return new AuthResult(true, "Login successful", token, isAdmin);
    }
    
    public AuthResult loginWithEmail(String email, String password) {
        return loginWithEmail(email, password, false);
    }
    
    public AuthResult loginWithEmail(String email, String password, boolean isAdminLogin) {
        var userOpt = userDao.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return new AuthResult(false, "Invalid email or password", null, false);
        }
        
        User user = userOpt.get();
        
        // Check if admin login is requested but user is not an op
        if (isAdminLogin) {
            if (opsManager == null || opsManager.getOps().isEmpty()) {
                VerifyMC.LOGGER.warn("Admin login attempted but no ops configured in ops.json");
                return new AuthResult(false, "System configuration error, please contact the server administrator", null, false);
            }
            if (!opsManager.isOp(user.getUsername())) {
                VerifyMC.LOGGER.warn("Non-op user attempted admin login: {}", user.getUsername());
                return new AuthResult(false, "You do not have administrator privileges", null, false);
            }
        }
        
        if (user.isBanned() && !user.isBanExpired()) {
            return new AuthResult(false, "Account is banned: " + user.getBanReason(), null, false);
        }
        
        if (!PasswordUtil.verifyPassword(password, user.getSalt(), user.getPasswordHash())) {
            return new AuthResult(false, "Invalid email or password", null, false);
        }
        
        String token = generateToken();
        sessionTokens.put(token, user.getId());
        
        // Check if user is admin
        boolean isAdmin = opsManager != null && opsManager.isOp(user.getUsername());
        if (isAdmin) {
            adminTokens.add(token);
        }
        
        VerifyMC.LOGGER.info("User logged in with email: {} (admin: {})", email, isAdmin);
        return new AuthResult(true, "Login successful", token, isAdmin);
    }
    
    public void logout(String token) {
        sessionTokens.remove(token);
        adminTokens.remove(token);
    }
    
    public boolean isAdmin(String token) {
        return adminTokens.contains(token);
    }
    
    public boolean validateAdminToken(String token) {
        return sessionTokens.containsKey(token) && adminTokens.contains(token);
    }
    
    public boolean validateToken(String token) {
        return sessionTokens.containsKey(token);
    }

    public UUID getUserIdByToken(String token) {
        return sessionTokens.get(token);
    }

    public User getUserByToken(String token) {
        UUID userId = sessionTokens.get(token);
        if (userId == null) {
            return null;
        }
        return userDao.findById(userId).orElse(null);
    }
    
    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    public static class AuthResult {
        private final boolean success;
        private final String message;
        private final String token;
        private final boolean isAdmin;
        
        public AuthResult(boolean success, String message, String token, boolean isAdmin) {
            this.success = success;
            this.message = message;
            this.token = token;
            this.isAdmin = isAdmin;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getToken() { return token; }
        public boolean isAdmin() { return isAdmin; }
    }
}
