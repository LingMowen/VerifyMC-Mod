package com.verifymc.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.verifymc.VerifyMC;
import com.verifymc.db.User;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Optional metrics service for collecting anonymous usage statistics.
 * This is a simplified implementation for NeoForge.
 * Users can opt-out by setting metrics.enabled=false in config.
 */
public class MetricsService {
    private static final String METRICS_VERSION = "1.0.0";
    private static final String REPORT_URL = "https://bStats.org/api/v2/data/%d";
    private static final int SERVICE_ID = 23654; // VerifyMC bStats service ID

    private final VerifyMC mod;
    private final Gson gson = new Gson();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final AtomicBoolean enabled = new AtomicBoolean(false);
    private final String serverUUID;

    public MetricsService(VerifyMC mod) {
        this.mod = mod;
        this.serverUUID = java.util.UUID.randomUUID().toString();
    }

    public void start() {
        if (!enabled.compareAndSet(false, true)) {
            return;
        }

        // Submit initial data after 5 minutes
        scheduler.scheduleAtFixedRate(
            this::submitData,
            5,
            30,
            TimeUnit.MINUTES
        );

        VerifyMC.LOGGER.info("Metrics service started (anonymous usage statistics)");
    }

    public void stop() {
        if (enabled.compareAndSet(true, false)) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            VerifyMC.LOGGER.info("Metrics service stopped");
        }
    }

    private void submitData() {
        if (!enabled.get()) {
            return;
        }

        try {
            JsonObject data = buildMetricsData();
            sendMetrics(data);
        } catch (Exception e) {
            // Silently fail - metrics are optional
            debugLog("Failed to submit metrics: " + e.getMessage());
        }
    }

    private JsonObject buildMetricsData() {
        JsonObject data = new JsonObject();

        // Service data
        JsonObject serviceData = new JsonObject();
        serviceData.addProperty("id", SERVICE_ID);
        serviceData.addProperty("uuid", serverUUID);
        serviceData.addProperty("metricsVersion", METRICS_VERSION);

        // Platform data
        JsonObject platformData = new JsonObject();
        platformData.addProperty("playerAmount", getPlayerCount());
        platformData.addProperty("onlineMode", isOnlineMode() ? 1 : 0);
        platformData.addProperty("minecraftVersion", getMinecraftVersion());
        platformData.addProperty("modLoader", "NeoForge");
        platformData.addProperty("javaVersion", System.getProperty("java.version"));
        platformData.addProperty("osName", System.getProperty("os.name"));
        platformData.addProperty("osArch", System.getProperty("os.arch"));
        platformData.addProperty("osVersion", System.getProperty("os.version"));
        platformData.addProperty("coreCount", Runtime.getRuntime().availableProcessors());

        // Plugin/Mod data
        JsonObject modData = new JsonObject();
        modData.addProperty("pluginVersion", getModVersion());
        modData.addProperty("whitelistCount", getWhitelistCount());
        modData.addProperty("pendingApplications", getPendingApplicationCount());

        data.add("service", serviceData);
        data.add("platform", platformData);
        data.add("customCharts", new JsonObject().getAsJsonArray());

        return data;
    }

    private void sendMetrics(JsonObject data) {
        try {
            HttpURLConnection conn = (HttpURLConnection) 
                URI.create(String.format(REPORT_URL, SERVICE_ID)).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            String json = gson.toJson(data);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200 || responseCode == 204) {
                debugLog("Metrics submitted successfully");
            } else {
                debugLog("Metrics submission failed with status: " + responseCode);
            }
        } catch (Exception e) {
            debugLog("Failed to send metrics: " + e.getMessage());
        }
    }

    private int getPlayerCount() {
        try {
            var server = mod.getServer();
            if (server != null) {
                return server.getPlayerList().getPlayerCount();
            }
        } catch (Exception e) {
            // Ignore
        }
        return 0;
    }

    private boolean isOnlineMode() {
        try {
            var server = mod.getServer();
            if (server != null) {
                return server.usesAuthentication();
            }
        } catch (Exception e) {
            // Ignore
        }
        return true;
    }

    private String getMinecraftVersion() {
        try {
            return net.minecraft.SharedConstants.getCurrentVersion().getName();
        } catch (Exception e) {
            return "1.21.1";
        }
    }

    private String getModVersion() {
        return "1.0.0"; // TODO: Get from mod metadata
    }

    private int getWhitelistCount() {
        try {
            var userDao = mod.getUserDao();
            if (userDao != null) {
                return (int) userDao.findAll().stream()
                    .filter(u -> u.getStatus() == User.UserStatus.APPROVED)
                    .count();
            }
        } catch (Exception e) {
            // Ignore
        }
        return 0;
    }

    private int getPendingApplicationCount() {
        try {
            var userDao = mod.getUserDao();
            if (userDao != null) {
                return (int) userDao.findAll().stream()
                    .filter(u -> u.getStatus() == User.UserStatus.PENDING)
                    .count();
            }
        } catch (Exception e) {
            // Ignore
        }
        return 0;
    }

    private void debugLog(String message) {
        if (Boolean.getBoolean("verifymc.debug")) {
            VerifyMC.LOGGER.debug("[Metrics] " + message);
        }
    }
}
