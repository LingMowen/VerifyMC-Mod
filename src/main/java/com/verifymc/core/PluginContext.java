package com.verifymc.core;

import com.google.gson.JsonObject;
import com.verifymc.VerifyMC;
import com.verifymc.config.VerifyMCConfig;
import com.verifymc.db.AuditDao;
import com.verifymc.db.User;
import com.verifymc.db.UserDao;
import com.verifymc.mail.MailService;
import com.verifymc.service.CaptchaService;
import com.verifymc.service.DiscordService;
import com.verifymc.service.QuestionnaireService;
import com.verifymc.service.VerifyCodeService;
import com.verifymc.service.VersionCheckService;
import com.verifymc.web.NotificationWebSocketServer;

import java.util.Optional;

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
    private CaptchaService captchaService;
    private DiscordService discordService;
    private QuestionnaireService questionnaireService;
    private VerifyCodeService verifyCodeService;
    private VersionCheckService versionCheckService;
    private NotificationWebSocketServer wsServer;

    // Configuration
    private ConfigManager configManager;
    private ResourceManager resourceManager;
    private I18nManager i18nManager;
    private boolean usernameCaseSensitive = false;

    public PluginContext(VerifyMC plugin) {
        this.plugin = plugin;
    }

    // --- Getters ---
    public VerifyMC getPlugin() { return plugin; }
    public OpsManager getOpsManager() { return opsManager; }

    public UserDao getUserDao() { return userDao; }
    public AuditDao getAuditDao() { return auditDao; }
    public MailService getMailService() { return mailService; }
    public CaptchaService getCaptchaService() { return captchaService; }
    public DiscordService getDiscordService() { return discordService; }
    public QuestionnaireService getQuestionnaireService() { return questionnaireService; }
    public VerifyCodeService getVerifyCodeService() { return verifyCodeService; }
    public VersionCheckService getVersionCheckService() { return versionCheckService; }
    public NotificationWebSocketServer getWsServer() { return wsServer; }
    public ConfigManager getConfigManager() { return configManager; }
    public ResourceManager getResourceManager() { return resourceManager; }
    public I18nManager getI18nManager() { return i18nManager; }

    // --- Setters (for initialization phase) ---
    public void setUserDao(UserDao userDao) { this.userDao = userDao; }
    public void setAuditDao(AuditDao auditDao) { this.auditDao = auditDao; }
    public void setMailService(MailService mailService) { this.mailService = mailService; }
    public void setCaptchaService(CaptchaService captchaService) { this.captchaService = captchaService; }
    public void setDiscordService(DiscordService discordService) { this.discordService = discordService; }
    public void setQuestionnaireService(QuestionnaireService questionnaireService) { this.questionnaireService = questionnaireService; }
    public void setVerifyCodeService(VerifyCodeService verifyCodeService) { this.verifyCodeService = verifyCodeService; }
    public void setVersionCheckService(VersionCheckService versionCheckService) { this.versionCheckService = versionCheckService; }
    public void setWsServer(NotificationWebSocketServer wsServer) { this.wsServer = wsServer; }
    public void setConfigManager(ConfigManager configManager) { this.configManager = configManager; }
    public void setResourceManager(ResourceManager resourceManager) { this.resourceManager = resourceManager; }
    public void setI18nManager(I18nManager i18nManager) { this.i18nManager = i18nManager; }
    public void setOpsManager(OpsManager opsManager) { this.opsManager = opsManager; }
    public void setUsernameCaseSensitive(boolean usernameCaseSensitive) { this.usernameCaseSensitive = usernameCaseSensitive; }

    // --- Helper methods ---
    public boolean isDebug() {
        return VerifyMCConfig.DEBUG.get();
    }

    public void debugLog(String msg) {
        if (isDebug()) {
            VerifyMC.LOGGER.info("[DEBUG] {}", msg);
        }
    }

    public boolean isUsernameCaseSensitive() {
        return usernameCaseSensitive;
    }

    /**
     * Gets a user by username with case sensitivity configuration.
     */
    public Optional<User> getUserByConfiguredUsername(String username) {
        if (userDao == null) return Optional.empty();
        return userDao.findByUsername(username);
    }

    /**
     * Resolves a stored username (handles case sensitivity).
     */
    public String resolveStoredUsername(String input) {
        if (input == null || input.isBlank()) return null;
        Optional<User> user = getUserByConfiguredUsername(input);
        return user.map(User::getUsername).orElse(null);
    }

    /**
     * Gets a localized message.
     */
    public String getMessage(String key, String language) {
        if (i18nManager != null) {
            return i18nManager.getMessage(key, language);
        }
        return key;
    }
}
