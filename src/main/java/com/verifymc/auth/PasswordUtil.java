package com.verifymc.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtil {
    
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SALT_LENGTH = 16;
    
    public static String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
    
    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash password", e);
        }
    }
    
    public static boolean verifyPassword(String password, String salt, String hash) {
        String computedHash = hashPassword(password, salt);
        return computedHash.equals(hash);
    }

    /**
     * Verify password with stored hash (format: hash:salt or plain hash)
     */
    public static boolean verify(String password, String storedHash) {
        if (storedHash == null || storedHash.isEmpty()) {
            return false;
        }

        // Check if it's in format hash:salt
        if (storedHash.contains(":")) {
            String[] parts = storedHash.split(":", 2);
            if (parts.length == 2) {
                return verifyPassword(password, parts[1], parts[0]);
            }
        }

        // Legacy: plain SHA-256 hash without salt
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            String computedHash = Base64.getEncoder().encodeToString(hash);
            return computedHash.equals(storedHash);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Hash password with generated salt (returns hash:salt)
     */
    public static String hash(String password) {
        String salt = generateSalt();
        String hash = hashPassword(password, salt);
        return hash + ":" + salt;
    }

    /**
     * Check if password needs migration to new format
     */
    public static boolean needsMigration(String storedHash) {
        if (storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        // Needs migration if it doesn't contain salt separator
        return !storedHash.contains(":");
    }

    /**
     * Check if stored password is plaintext
     */
    public static boolean isPlaintext(String storedHash) {
        if (storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        // Plaintext if it's not a valid Base64 SHA-256 hash
        // SHA-256 Base64 is always 44 characters
        if (storedHash.length() == 44 && !storedHash.contains(":")) {
            try {
                Base64.getDecoder().decode(storedHash);
                return false; // Valid Base64, likely hashed
            } catch (IllegalArgumentException e) {
                return true; // Not valid Base64, likely plaintext
            }
        }
        // If contains colon, it's hash:salt format
        if (storedHash.contains(":")) {
            return false;
        }
        return true; // Likely plaintext
    }
}
