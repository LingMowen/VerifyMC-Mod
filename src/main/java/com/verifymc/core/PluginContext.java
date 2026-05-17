package com.verifymc.core;

import com.verifymc.VerifyMC;
import com.verifymc.config.VerifyMCConfig;
import com.verifymc.db.AuditDao;
import com.verifymc.db.UserDao;
import com.verifymc.mail.MailService;

/**
 * Central service container that holds references to all services.
 * Simplified version for NeoForge port.
 */
public class PluginContext {
    private final VerifyMC plugin;
    private OpsManager opsManager;

    // Data access
    private UserDao userDao;
    private AuditDao auditDao;

    // Services
    private MailService mailService;

    public PluginContext(VerifyMC plugin) {
        this.plugin = plugin;
    }

    // --- Getters ---
    public VerifyMC getPlugin() { return plugin; }
    public OpsManager getOpsManager() { return opsManager; }

    public UserDao getUserDao() { return userDao; }
    public AuditDao getAuditDao() { return auditDao; }
    public MailService getMailService() { return mailService; }

    // --- Setters (for initialization phase) ---
    public void setUserDao(UserDao userDao) { this.userDao = userDao; }
    public void setAuditDao(AuditDao auditDao) { this.auditDao = auditDao; }
    public void setMailService(MailService mailService) { this.mailService = mailService; }
    public void setOpsManager(OpsManager opsManager) { this.opsManager = opsManager; }

    public boolean isDebug() {
        return VerifyMCConfig.DEBUG.get();
    }

    public void debugLog(String msg) {
        if (isDebug()) {
            VerifyMC.LOGGER.info("[DEBUG] {}", msg);
        }
    }
}
