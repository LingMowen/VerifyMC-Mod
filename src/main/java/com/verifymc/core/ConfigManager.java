package com.verifymc.core;

import com.verifymc.config.VerifyMCConfig;

/**
 * Configuration manager for handling plugin configuration.
 * Simplified version that wraps NeoForge's VerifyMCConfig.
 */
public class ConfigManager {
    
    public ConfigManager() {
    }

    public String getLanguage() {
        return VerifyMCConfig.LANGUAGE.get();
    }

    public boolean isDebugEnabled() {
        return VerifyMCConfig.DEBUG.get();
    }

    public int getWebPort() {
        return VerifyMCConfig.WEB_PORT.get();
    }

    public String getWebHost() {
        return VerifyMCConfig.WEB_HOST.get();
    }

    public boolean isEmailVerificationEnabled() {
        return VerifyMCConfig.EMAIL_VERIFICATION_ENABLED.get();
    }

    public boolean isQuestionnaireEnabled() {
        return VerifyMCConfig.QUESTIONNAIRE_ENABLED.get();
    }

    public int getQuestionnairePassScore() {
        return VerifyMCConfig.QUESTIONNAIRE_PASS_SCORE.get();
    }

    public String getStorageType() {
        return VerifyMCConfig.STORAGE_TYPE.get();
    }

    public String getMysqlHost() {
        return VerifyMCConfig.MYSQL_HOST.get();
    }

    public int getMysqlPort() {
        return VerifyMCConfig.MYSQL_PORT.get();
    }

    public String getMysqlDatabase() {
        return VerifyMCConfig.MYSQL_DATABASE.get();
    }

    public String getMysqlUsername() {
        return VerifyMCConfig.MYSQL_USERNAME.get();
    }

    public String getMysqlPassword() {
        return VerifyMCConfig.MYSQL_PASSWORD.get();
    }

    public boolean isWebSocketEnabled() {
        return VerifyMCConfig.WEBSOCKET_ENABLED.get();
    }

    public int getWebSocketPort() {
        return VerifyMCConfig.WEBSOCKET_PORT.get();
    }

    public boolean isLlmScoringEnabled() {
        return VerifyMCConfig.LLM_SCORING_ENABLED.get();
    }

    public String getLlmApiBase() {
        return VerifyMCConfig.LLM_API_BASE.get();
    }

    public String getLlmApiKey() {
        return VerifyMCConfig.LLM_API_KEY.get();
    }

    public String getLlmModel() {
        return VerifyMCConfig.LLM_MODEL.get();
    }
}
