package com.verifymc.web;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.verifymc.VerifyMC;
import com.verifymc.config.VerifyMCConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

/**
 * WebSocket server for real-time notifications
 * Used to notify admins of new registration applications
 */
public class NotificationWebSocketServer extends WebSocketServer {
    private static final Gson gson = new Gson();

    // Store admin connections
    private final Set<WebSocket> adminConnections = ConcurrentHashMap.newKeySet();

    // Store user connections by username
    private final ConcurrentHashMap<String, WebSocket> userConnections = new ConcurrentHashMap<>();

    public NotificationWebSocketServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        debugLog("WebSocket connection opened: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        debugLog("WebSocket connection closed: " + conn.getRemoteSocketAddress());
        adminConnections.remove(conn);

        // Remove from user connections
        userConnections.entrySet().removeIf(entry -> entry.getValue() == conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        debugLog("WebSocket message received: " + message);

        try {
            JsonObject json = gson.fromJson(message, JsonObject.class);
            String type = json.has("type") ? json.get("type").getAsString() : "";

            switch (type) {
                case "auth":
                    handleAuth(conn, json);
                    break;
                case "ping":
                    handlePing(conn);
                    break;
                default:
                    debugLog("Unknown message type: " + type);
            }
        } catch (Exception e) {
            debugLog("Error processing message: " + e.getMessage());
        }
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        // Handle binary messages if needed
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        VerifyMC.LOGGER.error("WebSocket error", ex);
        if (conn != null) {
            adminConnections.remove(conn);
            userConnections.entrySet().removeIf(entry -> entry.getValue() == conn);
        }
    }

    @Override
    public void onStart() {
        VerifyMC.LOGGER.info("WebSocket server started on port {}", getPort());
    }

    private void handleAuth(WebSocket conn, JsonObject json) {
        String role = json.has("role") ? json.get("role").getAsString() : "";
        String username = json.has("username") ? json.get("username").getAsString() : "";

        if ("admin".equals(role)) {
            adminConnections.add(conn);
            debugLog("Admin authenticated: " + username);
            sendToConnection(conn, createMessage("auth_success", "Authenticated as admin"));
        } else if ("user".equals(role) && !username.isEmpty()) {
            userConnections.put(username, conn);
            debugLog("User authenticated: " + username);
            sendToConnection(conn, createMessage("auth_success", "Authenticated as user"));
        } else {
            sendToConnection(conn, createMessage("auth_failed", "Invalid authentication"));
        }
    }

    private void handlePing(WebSocket conn) {
        sendToConnection(conn, createMessage("pong", ""));
    }

    /**
     * Notify all admins of a new registration
     */
    public void notifyNewRegistration(String username, String email) {
        JsonObject data = new JsonObject();
        data.addProperty("username", username);
        data.addProperty("email", email);
        data.addProperty("timestamp", System.currentTimeMillis());

        broadcastToAdmins("new_registration", data);
        debugLog("Notified admins of new registration: " + username);
    }

    /**
     * Notify user of status change
     */
    public void notifyUserStatusChange(String username, String status, String message) {
        JsonObject data = new JsonObject();
        data.addProperty("username", username);
        data.addProperty("status", status);
        data.addProperty("message", message);
        data.addProperty("timestamp", System.currentTimeMillis());

        sendToUser(username, "status_change", data);
        debugLog("Notified user of status change: " + username + " -> " + status);
    }

    /**
     * Broadcast message to all admin connections
     */
    public void broadcastToAdmins(String type, JsonObject data) {
        JsonObject message = new JsonObject();
        message.addProperty("type", type);
        message.add("data", data);

        String jsonStr = gson.toJson(message);

        for (WebSocket conn : adminConnections) {
            sendToConnection(conn, jsonStr);
        }
    }

    /**
     * Send message to a specific user
     */
    public void sendToUser(String username, String type, JsonObject data) {
        WebSocket conn = userConnections.get(username);
        if (conn != null && conn.isOpen()) {
            JsonObject message = new JsonObject();
            message.addProperty("type", type);
            message.add("data", data);
            sendToConnection(conn, gson.toJson(message));
        }
    }

    private void sendToConnection(WebSocket conn, String message) {
        if (conn != null && conn.isOpen()) {
            conn.send(message);
        }
    }

    private String createMessage(String type, String message) {
        JsonObject json = new JsonObject();
        json.addProperty("type", type);
        json.addProperty("message", message);
        return gson.toJson(json);
    }

    private void debugLog(String msg) {
        if (VerifyMCConfig.DEBUG.get()) {
            VerifyMC.LOGGER.info("[DEBUG] WebSocket: {}", msg);
        }
    }

    /**
     * Get number of connected admins
     */
    public int getAdminCount() {
        return adminConnections.size();
    }

    /**
     * Get number of connected users
     */
    public int getUserCount() {
        return userConnections.size();
    }
}
