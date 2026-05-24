package com.verifymc.core;

import com.verifymc.VerifyMC;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resource manager for handling static files, templates, and i18n resources.
 * Supports hot-reloading from the JAR or external data folder.
 */
public class ResourceManager {
    private File dataFolder;
    private final Map<String, String> templateCache = new ConcurrentHashMap<>();
    private final Map<String, byte[]> resourceCache = new ConcurrentHashMap<>();
    private volatile boolean cacheEnabled = true;

    public ResourceManager() {
    }

    public void setDataFolder(File dataFolder) {
        this.dataFolder = dataFolder;
        ensureDirectories();
    }

    private void ensureDirectories() {
        if (dataFolder != null) {
            new File(dataFolder, "i18n").mkdirs();
            new File(dataFolder, "email").mkdirs();
            new File(dataFolder, "templates").mkdirs();
        }
    }

    /**
     * Get a resource as string, with caching support.
     * First checks the data folder, then falls back to JAR resources.
     */
    public String getResourceAsString(String path) {
        if (cacheEnabled && templateCache.containsKey(path)) {
            return templateCache.get(path);
        }

        String content = loadResourceAsString(path);
        if (content != null && cacheEnabled) {
            templateCache.put(path, content);
        }
        return content;
    }

    /**
     * Get a resource as bytes.
     */
    public byte[] getResourceAsBytes(String path) {
        if (cacheEnabled && resourceCache.containsKey(path)) {
            return resourceCache.get(path);
        }

        byte[] content = loadResourceAsBytes(path);
        if (content != null && cacheEnabled) {
            resourceCache.put(path, content);
        }
        return content;
    }

    private String loadResourceAsString(String path) {
        byte[] bytes = loadResourceAsBytes(path);
        if (bytes != null) {
            return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        }
        return null;
    }

    private byte[] loadResourceAsBytes(String path) {
        // Normalize path
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;

        // Try data folder first
        if (dataFolder != null) {
            File file = new File(dataFolder, normalizedPath);
            if (file.exists()) {
                try {
                    return Files.readAllBytes(file.toPath());
                } catch (IOException e) {
                    VerifyMC.LOGGER.warn("Failed to read resource from data folder: {}", path);
                }
            }
        }

        // Try JAR resource
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("assets/verifymc/" + normalizedPath)) {
            if (is != null) {
                return is.readAllBytes();
            }
        } catch (IOException e) {
            VerifyMC.LOGGER.warn("Failed to read resource from JAR: {}", path);
        }

        return null;
    }

    /**
     * Get an email template with placeholders replaced.
     */
    public String getEmailTemplate(String templateName, String language, Map<String, String> placeholders) {
        String path = "email/" + templateName + "_" + language + ".html";
        String template = getResourceAsString(path);

        if (template == null) {
            // Fallback to English
            path = "email/" + templateName + "_en.html";
            template = getResourceAsString(path);
        }

        if (template == null) {
            return null;
        }

        // Replace placeholders
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            template = template.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return template;
    }

    /**
     * Extract default resources to data folder if they don't exist.
     */
    public void extractDefaultResources() {
        if (dataFolder == null) return;

        // Extract default i18n files
        extractResource("i18n/messages_zh.properties", "i18n/messages_zh.properties");
        extractResource("i18n/messages_en.properties", "i18n/messages_en.properties");

        // Extract default email templates
        extractResource("email/verify_code_zh.html", "email/verify_code_zh.html");
        extractResource("email/verify_code_en.html", "email/verify_code_en.html");
        extractResource("email/review_approved_zh.html", "email/review_approved_zh.html");
        extractResource("email/review_approved_en.html", "email/review_approved_en.html");
        extractResource("email/review_rejected_zh.html", "email/review_rejected_zh.html");
        extractResource("email/review_rejected_en.html", "email/review_rejected_en.html");
    }

    private void extractResource(String sourcePath, String targetPath) {
        File targetFile = new File(dataFolder, targetPath);
        if (targetFile.exists()) return;

        try (InputStream is = getClass().getClassLoader().getResourceAsStream("assets/verifymc/" + sourcePath)) {
            if (is != null) {
                targetFile.getParentFile().mkdirs();
                Files.copy(is, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                VerifyMC.LOGGER.info("Extracted default resource: {}", targetPath);
            }
        } catch (IOException e) {
            VerifyMC.LOGGER.warn("Failed to extract resource: {}", targetPath);
        }
    }

    /**
     * Clear the resource cache.
     */
    public void clearCache() {
        templateCache.clear();
        resourceCache.clear();
        VerifyMC.LOGGER.info("Resource cache cleared");
    }

    /**
     * Enable or disable caching.
     */
    public void setCacheEnabled(boolean enabled) {
        this.cacheEnabled = enabled;
        if (!enabled) {
            clearCache();
        }
    }

    /**
     * Check if a resource exists.
     */
    public boolean resourceExists(String path) {
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;

        // Check data folder
        if (dataFolder != null) {
            File file = new File(dataFolder, normalizedPath);
            if (file.exists()) return true;
        }

        // Check JAR
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("assets/verifymc/" + normalizedPath)) {
            return is != null;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Get the data folder path.
     */
    public File getDataFolder() {
        return dataFolder;
    }
}
