package com.verifymc.mail;

import com.verifymc.VerifyMC;
import com.verifymc.config.VerifyMCConfig;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MailService {
    
    private static MailService instance;
    private final ConcurrentHashMap<String, VerificationCode> pendingCodes = new ConcurrentHashMap<>();
    private static final long CODE_EXPIRY_MS = 5 * 60 * 1000;
    
    private MailService() {
    }
    
    public static synchronized MailService getInstance() {
        if (instance == null) {
            instance = new MailService();
        }
        return instance;
    }
    
    public boolean sendVerificationCode(String email) {
        String code = generateCode();
        long expiryTime = System.currentTimeMillis() + CODE_EXPIRY_MS;
        pendingCodes.put(email, new VerificationCode(code, expiryTime));
        
        try {
            String host = VerifyMCConfig.SMTP_HOST.get();
            String username = VerifyMCConfig.SMTP_USERNAME.get();
            String password = VerifyMCConfig.SMTP_PASSWORD.get();
            
            if (host.isEmpty() || username.isEmpty() || password.isEmpty()) {
                VerifyMC.LOGGER.warn("SMTP not configured, skipping email send to: {}", email);
                return true;
            }
            
            // Use HTTP API for email sending (e.g., SendGrid, Mailgun, or custom API)
            // For now, just log the code
            VerifyMC.LOGGER.info("Verification code for {}: {}", email, code);
            
            // TODO: Implement actual email sending via HTTP API
            // Example: SendGrid API, Mailgun API, or your own email service
            
            return true;
        } catch (Exception e) {
            pendingCodes.remove(email);
            VerifyMC.LOGGER.error("Failed to send verification code to: {}", email, e);
            return false;
        }
    }
    
    public boolean verifyCode(String email, String code) {
        VerificationCode storedCode = pendingCodes.get(email);
        if (storedCode == null) {
            return false;
        }
        
        if (System.currentTimeMillis() > storedCode.expiryTime) {
            pendingCodes.remove(email);
            return false;
        }
        
        boolean valid = storedCode.code.equals(code);
        if (valid) {
            pendingCodes.remove(email);
        }
        
        return valid;
    }
    
    private String generateCode() {
        return String.format("%06d", Math.abs(UUID.randomUUID().hashCode()) % 1000000);
    }
    
    private static class VerificationCode {
        final String code;
        final long expiryTime;
        
        VerificationCode(String code, long expiryTime) {
            this.code = code;
            this.expiryTime = expiryTime;
        }
    }
}
