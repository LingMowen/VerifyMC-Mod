package com.verifymc.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.verifymc.VerifyMC;
import com.verifymc.config.VerifyMCConfig;
import com.verifymc.db.UserDao;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Discord OAuth2 integration service
 * Handles Discord account linking and guild membership verification
 */
public class DiscordService {
    private UserDao userDao;

    // OAuth2 configuration
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String guildId;
    private boolean required;

    // State tokens for OAuth2 flow (state -> username)
    private final Map<String, StateData> stateTokens = new ConcurrentHashMap<>();

    private static final String DISCORD_API_BASE = "https://discord.com/api/v10";
    private static final String DISCORD_OAUTH_AUTHORIZE = "https://discord.com/oauth2/authorize";
    private static final String DISCORD_OAUTH_TOKEN = DISCORD_API_BASE + "/oauth2/token";

    // State token expiry: 10 minutes
    private static final long STATE_EXPIRY_MS = 600000;

    private final Gson gson = new Gson();

    public DiscordService() {
        loadConfig();
    }

    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    public void loadConfig() {
        clientId = VerifyMCConfig.DISCORD_CLIENT_ID.get();
        clientSecret = VerifyMCConfig.DISCORD_CLIENT_SECRET.get();
        redirectUri = VerifyMCConfig.DISCORD_REDIRECT_URI.get();
        debugLog("Discord config loaded: clientId=" + (clientId.isEmpty() ? "not set" : "***"));
    }

    public boolean isEnabled() {
        return !clientId.isEmpty() && !clientSecret.isEmpty() && !redirectUri.isEmpty();
    }

    public boolean isRequired() {
        return isEnabled() && required;
    }

    public String generateAuthUrl(String username) {
        if (!isEnabled()) {
            return null;
        }

        // Generate state token
        String state = generateState();
        stateTokens.put(state, new StateData(username, System.currentTimeMillis()));

        // Build authorization URL
        StringBuilder url = new StringBuilder(DISCORD_OAUTH_AUTHORIZE);
        url.append("?client_id=").append(clientId);
        url.append("&redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8));
        url.append("&response_type=code");
        url.append("&scope=identify%20guilds");
        url.append("&state=").append(state);

        debugLog("Generated auth URL for " + username);
        return url.toString();
    }

    public DiscordCallbackResult handleCallback(String code, String state) {
        // Validate state
        StateData stateData = stateTokens.remove(state);
        if (stateData == null || stateData.isExpired()) {
            debugLog("Invalid or expired state token: " + state);
            return new DiscordCallbackResult(false, "Invalid or expired state", null, null);
        }

        String username = stateData.username;

        try {
            // Exchange code for access token
            DiscordToken token = exchangeCodeForToken(code);
            if (token == null) {
                return new DiscordCallbackResult(false, "Failed to exchange code for token", username, null);
            }

            // Get user info
            DiscordUser user = getUserInfo(token.accessToken);
            if (user == null) {
                return new DiscordCallbackResult(false, "Failed to get user info", username, null);
            }

            // Check guild membership if required
            if (guildId != null && !guildId.isEmpty()) {
                boolean inGuild = checkGuildMembership(token.accessToken, guildId);
                if (!inGuild) {
                    debugLog("User " + user.username + " is not in guild " + guildId);
                    return new DiscordCallbackResult(false, "You must be a member of the Discord server", username, user);
                }
            }

            // Persist Discord ID to database
            if (userDao != null) {
                var userOpt = userDao.findByUsername(username);
                if (userOpt.isPresent()) {
                    var dbUser = userOpt.get();
                    dbUser.setDiscordId(user.id);
                    userDao.save(dbUser);
                    debugLog("Persisted Discord ID to database for " + username + ": " + user.id);
                }
            }

            debugLog("Successfully linked Discord for " + username + ": " + user.username);

            return new DiscordCallbackResult(true, "Discord account linked successfully", username, user);

        } catch (Exception e) {
            debugLog("OAuth callback error: " + e.getMessage());
            return new DiscordCallbackResult(false, "OAuth error: " + e.getMessage(), username, null);
        }
    }

    private DiscordToken exchangeCodeForToken(String code) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(DISCORD_OAUTH_TOKEN).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        String body = "client_id=" + clientId +
                "&client_secret=" + clientSecret +
                "&grant_type=authorization_code" +
                "&code=" + code +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        if (conn.getResponseCode() != 200) {
            debugLog("Token exchange failed with status: " + conn.getResponseCode());
            return null;
        }

        String response = readResponse(conn);
        JsonObject json = gson.fromJson(response, JsonObject.class);

        return new DiscordToken(
                json.get("access_token").getAsString(),
                json.has("refresh_token") ? json.get("refresh_token").getAsString() : "",
                System.currentTimeMillis() + json.get("expires_in").getAsLong() * 1000
        );
    }

    private DiscordUser getUserInfo(String accessToken) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(DISCORD_API_BASE + "/users/@me").toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        if (conn.getResponseCode() != 200) {
            debugLog("Get user info failed with status: " + conn.getResponseCode());
            return null;
        }

        String response = readResponse(conn);
        JsonObject json = gson.fromJson(response, JsonObject.class);

        return new DiscordUser(
                json.get("id").getAsString(),
                json.get("username").getAsString(),
                json.has("discriminator") ? json.get("discriminator").getAsString() : "0",
                json.has("avatar") && !json.get("avatar").isJsonNull() ? json.get("avatar").getAsString() : null,
                json.has("global_name") && !json.get("global_name").isJsonNull() ? json.get("global_name").getAsString() : null
        );
    }

    private boolean checkGuildMembership(String accessToken, String guildId) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(DISCORD_API_BASE + "/users/@me/guilds").toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        if (conn.getResponseCode() != 200) {
            debugLog("Get guilds failed with status: " + conn.getResponseCode());
            return false;
        }

        String response = readResponse(conn);
        JsonObject[] guilds = gson.fromJson(response, JsonObject[].class);

        for (JsonObject guild : guilds) {
            if (guildId.equals(guild.get("id").getAsString())) {
                return true;
            }
        }

        return false;
    }

    public boolean isLinked(String username) {
        if (userDao != null) {
            var userOpt = userDao.findByUsername(username);
            if (userOpt.isPresent()) {
                String discordId = userOpt.get().getDiscordId();
                return discordId != null && !discordId.isEmpty();
            }
        }
        return false;
    }

    public boolean unlinkUser(String username) {
        if (userDao != null) {
            var userOpt = userDao.findByUsername(username);
            if (userOpt.isPresent()) {
                var user = userOpt.get();
                user.setDiscordId(null);
                userDao.save(user);
                return true;
            }
        }
        return false;
    }

    private String readResponse(HttpURLConnection conn) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
        )) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private String generateState() {
        byte[] bytes = new byte[16];
        new java.security.SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void debugLog(String msg) {
        if (VerifyMCConfig.DEBUG.get()) {
            VerifyMC.LOGGER.info("[DEBUG] DiscordService: {}", msg);
        }
    }

    // --- Data Classes ---

    private static class StateData {
        final String username;
        final long timestamp;

        StateData(String username, long timestamp) {
            this.username = username;
            this.timestamp = timestamp;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > STATE_EXPIRY_MS;
        }
    }

    public static class DiscordToken {
        public final String accessToken;
        public final String refreshToken;
        public final long expiresAt;

        public DiscordToken(String accessToken, String refreshToken, long expiresAt) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresAt = expiresAt;
        }
    }

    public static class DiscordUser {
        public final String id;
        public final String username;
        public final String discriminator;
        public final String avatar;
        public final String globalName;

        public DiscordUser(String id, String username, String discriminator, String avatar, String globalName) {
            this.id = id;
            this.username = username;
            this.discriminator = discriminator;
            this.avatar = avatar;
            this.globalName = globalName;
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("id", id);
            if (username != null) json.addProperty("username", username);
            json.addProperty("discriminator", discriminator);
            if (avatar != null) json.addProperty("avatar", avatar);
            if (globalName != null) json.addProperty("globalName", globalName);
            return json;
        }
    }

    public static class DiscordCallbackResult {
        public final boolean success;
        public final String message;
        public final String username;
        public final DiscordUser user;

        public DiscordCallbackResult(boolean success, String message, String username, DiscordUser user) {
            this.success = success;
            this.message = message;
            this.username = username;
            this.user = user;
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("success", success);
            json.addProperty("message", message);
            if (username != null) json.addProperty("username", username);
            if (user != null) json.add("user", user.toJson());
            return json;
        }
    }
}
