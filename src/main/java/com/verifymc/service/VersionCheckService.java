package com.verifymc.service;

import com.google.gson.JsonObject;
import com.verifymc.VerifyMC;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Version check service for VerifyMC mod
 * Checks for updates from GitHub repository
 */
public class VersionCheckService {
    private static final String GITHUB_POM_URL = "https://raw.githubusercontent.com/KiteMC/VerifyMC/refs/heads/master/plugin/pom.xml";
    private static final String GITHUB_RELEASES_URL = "https://github.com/KiteMC/VerifyMC/releases";
    private static final Pattern VERSION_PATTERN = Pattern.compile("<version>([^<]+)</version>");
    private static final int TIMEOUT_MS = 10000;

    private final String currentVersion;
    private String latestVersion;
    private boolean updateAvailable = false;
    private long lastCheckTime = 0;
    private static final long CHECK_INTERVAL = 3600000;

    public VersionCheckService(VerifyMC mod) {
        this.currentVersion = "1.0.0";
        debugLog("VersionCheckService initialized with current version: " + currentVersion);
    }

    private void debugLog(String message) {
        if (Boolean.getBoolean("verifymc.debug")) {
            VerifyMC.LOGGER.debug("[VersionCheck] " + message);
        }
    }

    public CompletableFuture<UpdateCheckResult> checkForUpdatesAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                debugLog("Starting version check...");

                long currentTime = System.currentTimeMillis();
                if (currentTime - lastCheckTime < CHECK_INTERVAL && latestVersion != null) {
                    debugLog("Using cached version check result");
                    return new UpdateCheckResult(true, currentVersion, latestVersion, updateAvailable, null);
                }

                String fetchedVersion = fetchLatestVersionFromGitHub();
                if (fetchedVersion == null) {
                    debugLog("Failed to fetch latest version");
                    return new UpdateCheckResult(false, currentVersion, null, false, "Failed to fetch version information");
                }

                latestVersion = fetchedVersion;
                lastCheckTime = currentTime;
                updateAvailable = isNewerVersion(fetchedVersion, currentVersion);

                debugLog("Version check completed. Current: " + currentVersion + ", Latest: " + latestVersion + ", Update available: " + updateAvailable);

                if (updateAvailable) {
                    VerifyMC.LOGGER.info("A new version of VerifyMC is available! Current: {}, Latest: {}", currentVersion, latestVersion);
                    VerifyMC.LOGGER.info("Download from: {}", GITHUB_RELEASES_URL);
                }

                return new UpdateCheckResult(true, currentVersion, latestVersion, updateAvailable, null);

            } catch (Exception e) {
                debugLog("Error during version check: " + e.getMessage());
                return new UpdateCheckResult(false, currentVersion, null, false, e.getMessage());
            }
        });
    }

    public CompletableFuture<UpdateCheckResult> checkAsync() {
        return checkForUpdatesAsync();
    }

    private String fetchLatestVersionFromGitHub() {
        HttpURLConnection connection = null;
        try {
            debugLog("Fetching version from: " + GITHUB_POM_URL);

            connection = (HttpURLConnection) URI.create(GITHUB_POM_URL).toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", "VerifyMC-Mod/" + currentVersion);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                debugLog("HTTP request failed with response code: " + responseCode);
                return null;
            }

            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }

            String pomContent = content.toString();
            Matcher matcher = VERSION_PATTERN.matcher(pomContent);

            if (matcher.find()) {
                String version = matcher.group(1).trim();
                debugLog("Found version in pom.xml: " + version);
                return version;
            } else {
                debugLog("No version found in pom.xml content");
                return null;
            }

        } catch (Exception e) {
            debugLog("Exception while fetching version: " + e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private boolean isNewerVersion(String version1, String version2) {
        try {
            debugLog("Comparing versions: " + version1 + " vs " + version2);

            String v1 = version1.replaceAll("^[vV]", "");
            String v2 = version2.replaceAll("^[vV]", "");

            String[] parts1 = v1.split("\\.");
            String[] parts2 = v2.split("\\.");

            int maxLength = Math.max(parts1.length, parts2.length);
            for (int i = 0; i < maxLength; i++) {
                int num1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
                int num2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;

                if (num1 > num2) {
                    debugLog("Version " + version1 + " is newer than " + version2);
                    return true;
                } else if (num1 < num2) {
                    debugLog("Version " + version1 + " is older than " + version2);
                    return false;
                }
            }

            debugLog("Versions are equal");
            return false;

        } catch (Exception e) {
            debugLog("Error comparing versions: " + e.getMessage());
            return false;
        }
    }

    private int parseVersionPart(String part) {
        try {
            String numericPart = part.replaceAll("[^0-9].*", "");
            return numericPart.isEmpty() ? 0 : Integer.parseInt(numericPart);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getReleasesUrl() {
        return GITHUB_RELEASES_URL;
    }

    public JsonObject getVersionInfoJson() {
        JsonObject json = new JsonObject();
        json.addProperty("currentVersion", currentVersion);
        json.addProperty("latestVersion", latestVersion);
        json.addProperty("updateAvailable", updateAvailable);
        json.addProperty("releasesUrl", GITHUB_RELEASES_URL);
        json.addProperty("lastCheckTime", lastCheckTime);
        return json;
    }

    public static class UpdateCheckResult {
        private final boolean success;
        private final String currentVersion;
        private final String latestVersion;
        private final boolean updateAvailable;
        private final String errorMessage;

        public UpdateCheckResult(boolean success, String currentVersion, String latestVersion, 
                                boolean updateAvailable, String errorMessage) {
            this.success = success;
            this.currentVersion = currentVersion;
            this.latestVersion = latestVersion;
            this.updateAvailable = updateAvailable;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getCurrentVersion() {
            return currentVersion;
        }

        public String getLatestVersion() {
            return latestVersion;
        }

        public boolean isUpdateAvailable() {
            return updateAvailable;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
