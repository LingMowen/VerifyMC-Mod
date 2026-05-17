package com.verifymc.core;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.verifymc.VerifyMC;
import com.verifymc.config.VerifyMCConfig;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class OpsManager {
    private static final long CACHE_DURATION_MS = 30_000L;

    private final File opsFile;
    private final Gson gson;
    private volatile List<String> cachedOps;
    private volatile long lastLoadTime;
    private final boolean debug;

    public OpsManager(MinecraftServer server) {
        this.gson = new Gson();
        this.cachedOps = new ArrayList<>();
        this.lastLoadTime = 0;
        this.debug = VerifyMCConfig.DEBUG.get();

        // Get server root directory
        Path serverRoot = server.getServerDirectory();
        this.opsFile = serverRoot.resolve("ops.json").toFile();
        debugLog("OpsManager initialized, ops.json path: " + opsFile.getAbsolutePath());
    }

    private void debugLog(String msg) {
        if (debug) {
            VerifyMC.LOGGER.info("[DEBUG] OpsManager: {}", msg);
        }
    }

    private synchronized void loadOpsIfNeeded() {
        long now = System.currentTimeMillis();
        if (cachedOps != null && (now - lastLoadTime) < CACHE_DURATION_MS) {
            debugLog("Using cached ops list, cache age: " + (now - lastLoadTime) + "ms");
            return;
        }
        loadOps();
    }

    private synchronized void loadOps() {
        debugLog("Loading ops.json from: " + opsFile.getAbsolutePath());
        if (!opsFile.exists()) {
            debugLog("ops.json does not exist, returning empty list");
            cachedOps = new ArrayList<>();
            lastLoadTime = System.currentTimeMillis();
            return;
        }

        try (FileReader reader = new FileReader(opsFile)) {
            List<Map<String, Object>> opsList = gson.fromJson(reader, new TypeToken<List<Map<String, Object>>>(){}.getType());
            if (opsList == null || opsList.isEmpty()) {
                debugLog("ops.json is empty or null");
                cachedOps = new ArrayList<>();
            } else {
                List<String> names = new ArrayList<>();
                for (Map<String, Object> opEntry : opsList) {
                    Object name = opEntry.get("name");
                    if (name != null) {
                        names.add(name.toString());
                    }
                }
                cachedOps = names;
                debugLog("Loaded " + names.size() + " op(s): " + names);
            }
            lastLoadTime = System.currentTimeMillis();
        } catch (Exception e) {
            VerifyMC.LOGGER.warn("Failed to load ops.json: {}", e.getMessage());
            debugLog("Exception while loading ops.json: " + e.getClass().getName() + " - " + e.getMessage());
            cachedOps = new ArrayList<>();
            lastLoadTime = System.currentTimeMillis();
        }
    }

    public boolean isOp(String username) {
        if (username == null || username.isEmpty()) {
            debugLog("isOp called with null or empty username");
            return false;
        }
        loadOpsIfNeeded();
        // Take a local snapshot to avoid race condition with loadOps() replacing the list
        List<String> snapshot = cachedOps;
        for (String opName : snapshot) {
            if (opName.equalsIgnoreCase(username)) {
                debugLog("User '" + username + "' is an op");
                return true;
            }
        }
        debugLog("User '" + username + "' is not an op");
        return false;
    }

    public List<String> getOps() {
        loadOpsIfNeeded();
        return Collections.unmodifiableList(cachedOps);
    }

    public void refreshCache() {
        debugLog("Cache refresh requested");
        lastLoadTime = 0;
        loadOpsIfNeeded();
    }
}
