package com.verifymc.core;

import com.verifymc.VerifyMC;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified i18n management. Loads property-based resource bundles from the
 * data folder or from the JAR, with caching.
 */
public class I18nManager {
    private final ConcurrentHashMap<String, ResourceBundle> languageCache = new ConcurrentHashMap<>();
    private ResourceBundle defaultBundle;
    private File dataFolder;

    public I18nManager() {
    }

    public void setDataFolder(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    /**
     * Initialize with a default language bundle.
     */
    public void init(String defaultLanguage) {
        this.defaultBundle = loadBundle(defaultLanguage);
        if (this.defaultBundle == null) {
            VerifyMC.LOGGER.warn("Failed to load default language bundle: {}", defaultLanguage);
        }
    }

    /**
     * Get a localized message for the given key and language.
     */
    public String getMessage(String key, String language) {
        if (key == null) {
            return "";
        }
        ResourceBundle bundle = getBundle(language);
        if (bundle != null && bundle.containsKey(key)) {
            return bundle.getString(key);
        }
        // Fallback to default bundle
        if (defaultBundle != null && defaultBundle.containsKey(key)) {
            return defaultBundle.getString(key);
        }
        return key;
    }

    /**
     * Get or load the language bundle, with caching.
     */
    public ResourceBundle getBundle(String language) {
        if (language == null || language.isEmpty()) {
            return defaultBundle;
        }

        if (languageCache.containsKey(language)) {
            return languageCache.get(language);
        }

        ResourceBundle bundle = loadBundle(language);
        if (bundle != null) {
            languageCache.put(language, bundle);
            return bundle;
        }

        // Fallback to default
        return defaultBundle;
    }

    /**
     * Load a resource bundle for the given language.
     * First tries the i18n directory in the data folder,
     * then falls back to the JAR classpath.
     */
    private ResourceBundle loadBundle(String language) {
        // Try loading from data folder
        if (dataFolder != null) {
            File i18nDir = new File(dataFolder, "i18n");
            File propFile = new File(i18nDir, "messages_" + language + ".properties");

            if (propFile.exists()) {
                try (InputStreamReader reader = new InputStreamReader(
                        new FileInputStream(propFile), StandardCharsets.UTF_8)) {
                    VerifyMC.LOGGER.info("Loaded i18n file from data folder: {}", propFile.getName());
                    return new PropertyResourceBundle(reader);
                } catch (Exception e) {
                    VerifyMC.LOGGER.warn("Failed to load i18n file: {} - {}", propFile.getName(), e.getMessage());
                }
            }
        }

        // Try loading from JAR resources
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(
                    "assets/verifymc/i18n/messages_" + language + ".properties");
            if (is != null) {
                try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    VerifyMC.LOGGER.info("Loaded i18n file from JAR: messages_{}.properties", language);
                    return new PropertyResourceBundle(reader);
                }
            }
        } catch (Exception e) {
            VerifyMC.LOGGER.warn("Failed to load i18n from JAR: {}", e.getMessage());
        }

        // Try ResourceBundle.getBundle as last resort
        try {
            return ResourceBundle.getBundle("i18n.messages", new Locale.Builder().setLanguage(language).build());
        } catch (Exception e) {
            // Fallback
            return null;
        }
    }

    /**
     * Clear the language cache (e.g., after config reload).
     */
    public void clearCache() {
        languageCache.clear();
    }

    /**
     * Get the default resource bundle.
     * @return The default ResourceBundle, or null if not initialized.
     */
    public ResourceBundle getResourceBundle() {
        return defaultBundle;
    }

    /**
     * Get all available languages.
     */
    public java.util.Set<String> getAvailableLanguages() {
        java.util.Set<String> languages = new java.util.HashSet<>();
        languages.add("zh");
        languages.add("en");

        // Check data folder for additional languages
        if (dataFolder != null) {
            File i18nDir = new File(dataFolder, "i18n");
            if (i18nDir.exists() && i18nDir.isDirectory()) {
                File[] files = i18nDir.listFiles((dir, name) ->
                        name.startsWith("messages_") && name.endsWith(".properties"));
                if (files != null) {
                    for (File f : files) {
                        String name = f.getName();
                        // Extract language code from messages_xx.properties
                        int start = "messages_".length();
                        int end = name.length() - ".properties".length();
                        if (start < end) {
                            languages.add(name.substring(start, end));
                        }
                    }
                }
            }
        }

        return languages;
    }
}
