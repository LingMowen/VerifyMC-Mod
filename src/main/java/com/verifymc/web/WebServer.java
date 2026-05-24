package com.verifymc.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.verifymc.VerifyMC;
import com.verifymc.auth.AuthManager;
import com.verifymc.config.VerifyMCConfig;
import com.verifymc.core.OpsManager;
import com.verifymc.db.User;
import com.verifymc.db.UserDao;
import com.verifymc.mail.MailService;
import com.verifymc.service.CaptchaService;
import com.verifymc.service.DiscordService;
import com.verifymc.service.QuestionnaireService;
import com.verifymc.service.VerifyCodeService;
import com.verifymc.whitelist.WhitelistManager;
import net.minecraft.server.MinecraftServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class WebServer {

    private final int port;
    private final String host;
    private final HttpServer server;
    private final Gson gson;

    private final UserDao userDao;
    private final AuthManager authManager;
    private final WhitelistManager whitelistManager;
    private final OpsManager opsManager;
    private final CaptchaService captchaService;
    private final MinecraftServer minecraftServer;
    private final VerifyCodeService verifyCodeService;
    private final QuestionnaireService questionnaireService;
    private final DiscordService discordService;

    public WebServer(int port, String host, UserDao userDao, AuthManager authManager, WhitelistManager whitelistManager, OpsManager opsManager, MinecraftServer minecraftServer, File dataFolder) throws IOException {
        this.port = port;
        this.host = host;
        this.userDao = userDao;
        this.authManager = authManager;
        this.whitelistManager = whitelistManager;
        this.opsManager = opsManager;
        this.captchaService = new CaptchaService();
        this.minecraftServer = minecraftServer;
        this.verifyCodeService = new VerifyCodeService();
        this.questionnaireService = new QuestionnaireService(dataFolder);
        this.discordService = new DiscordService();
        this.discordService.setUserDao(userDao);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.server = HttpServer.create(new InetSocketAddress(host, port), 0);

        setupRoutes();
    }

    private void setupRoutes() {
        // --- Captcha endpoints ---
        server.createContext("/api/captcha/generate", this::handleCaptchaGenerate);
        server.createContext("/api/captcha", this::handleCaptchaValidate);

        // --- Email verification endpoints ---
        server.createContext("/api/email/send-code", this::handleEmailSendCode);
        server.createContext("/api/email/verify", this::handleEmailVerify);

        // --- Questionnaire endpoints ---
        server.createContext("/api/questionnaire", this::handleQuestionnaireGet);
        server.createContext("/api/questionnaire/submit", this::handleQuestionnaireSubmit);

        // --- Discord OAuth endpoints ---
        server.createContext("/api/discord/auth-url", this::handleDiscordAuthUrl);
        server.createContext("/api/discord/callback", this::handleDiscordCallback);
        server.createContext("/api/discord/status", this::handleDiscordStatus);
        server.createContext("/api/discord/unlink", this::handleDiscordUnlink);

        // --- Registration ---
        server.createContext("/api/register", this::handleRegister);

        // --- Login endpoints ---
        server.createContext("/api/login", this::handleLogin);
        server.createContext("/api/admin/login", this::handleAdminLogin);

        // --- User status ---
        server.createContext("/api/status", this::handleStatus);
        server.createContext("/api/user/status", this::handleStatus);

        // --- Server status ---
        server.createContext("/api/server/status", this::handleServerStatus);

        // --- Config endpoint ---
        server.createContext("/api/config", this::handleConfig);

        // --- Download resources endpoint ---
        server.createContext("/api/downloads", this::handleDownloads);

        // --- Admin endpoints ---
        server.createContext("/api/admin/verify", this::handleAdminVerify);
        server.createContext("/api/admin/users", this::handleAdminUsers);
        server.createContext("/api/admin/user/approve", this::handleAdminApprove);
        server.createContext("/api/admin/user/reject", this::handleAdminReject);
        server.createContext("/api/admin/user/delete", this::handleAdminDelete);
        server.createContext("/api/admin/user/ban", this::handleAdminBan);
        server.createContext("/api/admin/user/unban", this::handleAdminUnban);
        server.createContext("/api/admin/user/password", this::handleAdminPassword);
        server.createContext("/api/admin/audits", this::handleAdminAudits);

        // --- User profile management ---
        server.createContext("/api/user/update", this::handleUserUpdate);
        server.createContext("/api/user/password", this::handleUserPassword);

        // --- Static files ---
        server.createContext("/", this::handleStatic);

        server.setExecutor(null);
    }
    
    public void start() {
        server.start();
        VerifyMC.LOGGER.info("Web server started on {}:{}", host, port);
    }
    
    public void stop() {
        server.stop(0);
        VerifyMC.LOGGER.info("Web server stopped");
    }

    public MinecraftServer getServer() {
        return minecraftServer;
    }
    
    private void handleRegister(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }
        
        try {
            Map<String, String> params = parseJsonBody(exchange);
            String username = params.get("username");
            String email = params.get("email");
            String password = params.get("password");
            
            if (username == null || email == null || password == null) {
                sendError(exchange, 400, "Missing required fields");
                return;
            }
            
            var result = authManager.register(username, email, password);
            
            if (result.isSuccess()) {
                sendJson(exchange, 200, Map.of(
                    "success", true,
                    "message", result.getMessage(),
                    "token", result.getToken()
                ));
            } else {
                sendJson(exchange, 400, Map.of(
                    "success", false,
                    "message", result.getMessage()
                ));
            }
        } catch (Exception e) {
            VerifyMC.LOGGER.error("Registration error", e);
            sendError(exchange, 500, "Internal server error");
        }
    }
    
    private void handleLogin(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        try {
            Map<String, String> params = parseJsonBody(exchange);
            String username = params.get("username");
            String email = params.get("email");
            String password = params.get("password");

            var result = username != null
                ? authManager.login(username, password, false)
                : authManager.loginWithEmail(email, password, false);

            if (result.isSuccess()) {
                sendJson(exchange, 200, Map.of(
                    "success", true,
                    "message", result.getMessage(),
                    "token", result.getToken(),
                    "isAdmin", result.isAdmin()
                ));
            } else {
                sendJson(exchange, 401, Map.of(
                    "success", false,
                    "message", result.getMessage()
                ));
            }
        } catch (Exception e) {
            VerifyMC.LOGGER.error("Login error", e);
            sendError(exchange, 500, "Internal server error");
        }
    }

    private void handleAdminLogin(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        try {
            Map<String, String> params = parseJsonBody(exchange);
            String username = params.get("username");
            String email = params.get("email");
            String password = params.get("password");

            // Check if ops.json is empty
            if (opsManager == null || opsManager.getOps().isEmpty()) {
                VerifyMC.LOGGER.warn("Admin login attempted but no ops configured");
                sendJson(exchange, 503, Map.of(
                    "success", false,
                    "message", "System configuration error, please contact the server administrator"
                ));
                return;
            }

            var result = username != null
                ? authManager.login(username, password, true)
                : authManager.loginWithEmail(email, password, true);

            if (result.isSuccess()) {
                sendJson(exchange, 200, Map.of(
                    "success", true,
                    "message", result.getMessage(),
                    "token", result.getToken(),
                    "isAdmin", true
                ));
            } else {
                sendJson(exchange, 401, Map.of(
                    "success", false,
                    "message", result.getMessage()
                ));
            }
        } catch (Exception e) {
            VerifyMC.LOGGER.error("Admin login error", e);
            sendError(exchange, 500, "Internal server error");
        }
    }

    private void handleAdminVerify(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        String token = extractToken(exchange);
        if (token == null || !authManager.validateAdminToken(token)) {
            sendJson(exchange, 401, Map.of("success", false, "message", "Unauthorized"));
            return;
        }

        sendJson(exchange, 200, Map.of("success", true, "message", "Token valid"));
    }
    
    private void handleStatus(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }
        
        String token = extractToken(exchange);
        if (token == null || !authManager.validateToken(token)) {
            sendError(exchange, 401, "Unauthorized");
            return;
        }
        
        User user = authManager.getUserByToken(token);
        if (user == null) {
            sendError(exchange, 404, "User not found");
            return;
        }
        
        sendJson(exchange, 200, Map.of(
            "success", true,
            "data", Map.of(
                "id", user.getId().toString(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "status", user.getStatus().name().toLowerCase(),
                "banned", user.isBanned()
            )
        ));
    }
    
    private void handleAdminUsers(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        // Verify admin token
        String token = extractToken(exchange);
        if (token == null || !authManager.validateAdminToken(token)) {
            sendJson(exchange, 403, Map.of("success", false, "message", "Forbidden: Admin privileges required"));
            return;
        }

        String status = getQueryParam(exchange, "status");
        String search = getQueryParam(exchange, "search");
        
        // Parse pagination parameters
        int page = 1;
        int size = 10;
        try {
            String pageParam = getQueryParam(exchange, "page");
            if (pageParam != null) {
                page = Math.max(1, Integer.parseInt(pageParam));
            }
            String sizeParam = getQueryParam(exchange, "size");
            if (sizeParam != null) {
                size = Math.max(1, Math.min(100, Integer.parseInt(sizeParam)));
            }
        } catch (NumberFormatException e) {
            // Use default values
        }

        List<User> users;

        if ("pending".equalsIgnoreCase(status)) {
            users = whitelistManager.getPendingUsers();
        } else if ("banned".equalsIgnoreCase(status)) {
            users = whitelistManager.getBannedUsers();
        } else {
            users = userDao.findAll();
        }

        // Apply search filter if provided
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase();
            users = users.stream()
                .filter(u -> u.getUsername().toLowerCase().contains(searchLower) ||
                            u.getEmail().toLowerCase().contains(searchLower))
                .toList();
        }

        // Calculate pagination
        int total = users.size();
        int totalPages = (int) Math.ceil((double) total / size);
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);
        
        List<User> paginatedUsers = fromIndex < total 
            ? users.subList(fromIndex, toIndex) 
            : new ArrayList<>();

        List<Map<String, Object>> userList = new ArrayList<>();
        for (User u : paginatedUsers) {
            userList.add(Map.of(
                "id", u.getId().toString(),
                "username", u.getUsername(),
                "email", u.getEmail(),
                "status", u.getStatus().name(),
                "banned", u.isBanned(),
                "createdAt", u.getCreatedAt().toString()
            ));
        }

        // Build pagination object to match frontend expectations
        Map<String, Object> pagination = Map.of(
            "currentPage", page,
            "pageSize", size,
            "totalCount", total,
            "totalPages", totalPages,
            "hasNext", page < totalPages,
            "hasPrev", page > 1
        );

        sendJson(exchange, 200, Map.of(
            "success", true, 
            "users", userList,
            "pagination", pagination
        ));
    }
    
    private void handleAdminApprove(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        // Verify admin token
        String token = extractToken(exchange);
        if (token == null || !authManager.validateAdminToken(token)) {
            sendJson(exchange, 403, Map.of("success", false, "message", "Forbidden: Admin privileges required"));
            return;
        }

        try {
            Map<String, String> params = parseJsonBody(exchange);
            UUID userId = UUID.fromString(params.get("userId"));

            whitelistManager.approveUser(userId);
            sendJson(exchange, 200, Map.of("success", true, "message", "User approved"));
        } catch (Exception e) {
            VerifyMC.LOGGER.error("Failed to approve user", e);
            sendJson(exchange, 400, Map.of("success", false, "message", "Invalid request"));
        }
    }

    private void handleAdminReject(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        // Verify admin token
        String token = extractToken(exchange);
        if (token == null || !authManager.validateAdminToken(token)) {
            sendJson(exchange, 403, Map.of("success", false, "message", "Forbidden: Admin privileges required"));
            return;
        }

        try {
            Map<String, String> params = parseJsonBody(exchange);
            UUID userId = UUID.fromString(params.get("userId"));
            String reason = params.get("reason");

            whitelistManager.rejectUser(userId, reason);
            sendJson(exchange, 200, Map.of("success", true, "message", "User rejected"));
        } catch (Exception e) {
            VerifyMC.LOGGER.error("Failed to reject user", e);
            sendJson(exchange, 400, Map.of("success", false, "message", "Invalid request"));
        }
    }

    private void handleAdminBan(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        // Verify admin token
        String token = extractToken(exchange);
        if (token == null || !authManager.validateAdminToken(token)) {
            sendJson(exchange, 403, Map.of("success", false, "message", "Forbidden: Admin privileges required"));
            return;
        }

        try {
            Map<String, String> params = parseJsonBody(exchange);
            UUID userId = UUID.fromString(params.get("userId"));
            String reason = params.getOrDefault("reason", "No reason provided");
            long duration = Long.parseLong(params.getOrDefault("duration", "0"));

            whitelistManager.banUser(userId, reason, duration);
            sendJson(exchange, 200, Map.of("success", true, "message", "User banned"));
        } catch (Exception e) {
            sendError(exchange, 400, "Invalid request");
        }
    }

    private void handleAdminUnban(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        // Verify admin token
        String token = extractToken(exchange);
        if (token == null || !authManager.validateAdminToken(token)) {
            sendJson(exchange, 403, Map.of("success", false, "message", "Forbidden: Admin privileges required"));
            return;
        }

        try {
            Map<String, String> params = parseJsonBody(exchange);
            UUID userId = UUID.fromString(params.get("userId"));

            whitelistManager.unbanUser(userId);
            sendJson(exchange, 200, Map.of("success", true, "message", "User unbanned"));
        } catch (Exception e) {
            sendError(exchange, 400, "Invalid request");
        }
    }

    private void handleAdminDelete(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        // Verify admin token
        String token = extractToken(exchange);
        if (token == null || !authManager.validateAdminToken(token)) {
            sendJson(exchange, 403, Map.of("success", false, "message", "Forbidden: Admin privileges required"));
            return;
        }

        try {
            Map<String, String> params = parseJsonBody(exchange);
            UUID userId = UUID.fromString(params.get("userId"));

            userDao.delete(userId);
            sendJson(exchange, 200, Map.of("success", true, "message", "User deleted"));
        } catch (Exception e) {
            sendError(exchange, 400, "Invalid request");
        }
    }

    private void handleAdminPassword(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        // Verify admin token
        String token = extractToken(exchange);
        if (token == null || !authManager.validateAdminToken(token)) {
            sendJson(exchange, 403, Map.of("success", false, "message", "Forbidden: Admin privileges required"));
            return;
        }

        try {
            Map<String, String> params = parseJsonBody(exchange);
            UUID userId = UUID.fromString(params.get("userId"));
            String newPassword = params.get("password");

            if (newPassword == null || newPassword.isEmpty()) {
                sendJson(exchange, 400, Map.of("success", false, "message", "Password is required"));
                return;
            }

            var userOpt = userDao.findById(userId);
            if (userOpt.isEmpty()) {
                sendJson(exchange, 404, Map.of("success", false, "message", "User not found"));
                return;
            }

            User user = userOpt.get();
            user.setPassword(newPassword);
            userDao.save(user);

            sendJson(exchange, 200, Map.of("success", true, "message", "Password changed"));
        } catch (Exception e) {
            sendError(exchange, 400, "Invalid request");
        }
    }

    private void handleAdminAudits(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        // Verify admin token
        String token = extractToken(exchange);
        if (token == null || !authManager.validateAdminToken(token)) {
            sendJson(exchange, 403, Map.of("success", false, "message", "Forbidden: Admin privileges required"));
            return;
        }

        // Return empty audits for now
        sendJson(exchange, 200, Map.of("success", true, "audits", List.of()));
    }

    private void handleUserUpdate(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        String token = extractToken(exchange);
        if (token == null || !authManager.validateToken(token)) {
            sendError(exchange, 401, "Unauthorized");
            return;
        }

        User user = authManager.getUserByToken(token);
        if (user == null) {
            sendError(exchange, 404, "User not found");
            return;
        }

        try {
            Map<String, String> params = parseJsonBody(exchange);
            String email = params.get("email");

            if (email != null && !email.isEmpty()) {
                user.setEmail(email);
            }

            userDao.save(user);
            sendJson(exchange, 200, Map.of("success", true, "message", "Profile updated"));
        } catch (Exception e) {
            sendError(exchange, 400, "Invalid request");
        }
    }

    private void handleUserPassword(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        String token = extractToken(exchange);
        if (token == null || !authManager.validateToken(token)) {
            sendError(exchange, 401, "Unauthorized");
            return;
        }

        User user = authManager.getUserByToken(token);
        if (user == null) {
            sendError(exchange, 404, "User not found");
            return;
        }

        try {
            Map<String, String> params = parseJsonBody(exchange);
            String oldPassword = params.get("oldPassword");
            String newPassword = params.get("newPassword");

            if (oldPassword == null || newPassword == null) {
                sendJson(exchange, 400, Map.of("success", false, "message", "Both old and new passwords are required"));
                return;
            }

            // Verify old password
            if (!user.getPassword().equals(oldPassword)) {
                sendJson(exchange, 401, Map.of("success", false, "message", "Old password is incorrect"));
                return;
            }

            user.setPassword(newPassword);
            userDao.save(user);

            sendJson(exchange, 200, Map.of("success", true, "message", "Password changed"));
        } catch (Exception e) {
            sendError(exchange, 400, "Invalid request");
        }
    }

    private void handleCaptchaGenerate(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET") && !exchange.getRequestMethod().equals("POST")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        try {
            CaptchaService.CaptchaResult result = captchaService.generateCaptcha();
            sendJson(exchange, 200, Map.of(
                "success", true,
                "token", result.getToken(),
                "image", result.getImageBase64()
            ));
        } catch (Exception e) {
            VerifyMC.LOGGER.error("Captcha generation error", e);
            sendError(exchange, 500, "Failed to generate captcha");
        }
    }

    private void handleCaptchaValidate(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        try {
            Map<String, String> params = parseJsonBody(exchange);
            String token = params.get("token");
            String answer = params.get("answer");

            boolean valid = captchaService.validateCaptcha(token, answer);
            sendJson(exchange, 200, Map.of(
                "success", true,
                "valid", valid
            ));
        } catch (Exception e) {
            sendError(exchange, 400, "Invalid request");
        }
    }

    private void handleServerStatus(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        try {
            int onlinePlayers = minecraftServer.getPlayerCount();
            int maxPlayers = minecraftServer.getMaxPlayers();
            String motd = minecraftServer.getMotd();
            
            // Get online player list with their UUIDs
            List<Map<String, Object>> playerList = new ArrayList<>();
            for (var player : minecraftServer.getPlayerList().getPlayers()) {
                playerList.add(Map.of(
                    "name", player.getGameProfile().getName(),
                    "uuid", player.getGameProfile().getId().toString()
                ));
            }
            
            // Get server TPS (Ticks Per Second) using reflection
            double tps = getTPS(minecraftServer);
            
            // Get memory usage using MemoryMXBean for more accurate data
            java.lang.management.MemoryMXBean memoryBean = java.lang.management.ManagementFactory.getMemoryMXBean();
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax() / 1024 / 1024; // MB
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed() / 1024 / 1024; // MB

            // Build response data to match frontend expectations
            Map<String, Object> data = Map.of(
                "online", true,
                "players", Map.of(
                    "online", onlinePlayers,
                    "max", maxPlayers,
                    "list", playerList
                ),
                "motd", motd,
                "version", "1.21.1",
                "loader", "NeoForge",
                "tps", Math.round(tps * 10) / 10.0,
                "memory", Map.of(
                    "used", usedMemory,
                    "max", maxMemory
                )
            );

            sendJson(exchange, 200, Map.of(
                "success", true,
                "data", data
            ));
        } catch (Exception e) {
            sendError(exchange, 500, "Failed to get server status");
        }
    }

    private double getTPS(net.minecraft.server.MinecraftServer server) {
        try {
            // Try to get TPS using reflection from MinecraftServer
            java.lang.reflect.Method getTPSMethod = server.getClass().getMethod("getTPS");
            if (getTPSMethod != null) {
                double[] tpsArray = (double[]) getTPSMethod.invoke(server);
                if (tpsArray != null && tpsArray.length > 0) {
                    return Math.round(tpsArray[0] * 100.0) / 100.0;
                }
            }
        } catch (Exception e) {
            VerifyMC.LOGGER.debug("Could not retrieve TPS: " + e.getMessage());
        }
        return 20.0; // Default TPS if unable to retrieve
    }

    private void handleConfig(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        String language = getQueryParam(exchange, "lang");
        if (language == null) {
            language = VerifyMCConfig.LANGUAGE.get();
        }

        sendJson(exchange, 200, Map.of(
            "success", true,
            "language", language,
            "emailVerificationEnabled", VerifyMCConfig.EMAIL_VERIFICATION_ENABLED.get(),
            "questionnaireEnabled", VerifyMCConfig.QUESTIONNAIRE_ENABLED.get(),
            "debug", VerifyMCConfig.DEBUG.get()
        ));
    }

    private void handleEmailSendCode(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        try {
            Map<String, String> params = parseJsonBody(exchange);
            String email = params.get("email");

            if (email == null || email.isEmpty()) {
                sendJson(exchange, 400, Map.of("success", false, "message", "Email is required"));
                return;
            }

            // Check rate limit
            if (!verifyCodeService.canSendCode(email)) {
                long remaining = verifyCodeService.getRemainingCooldownSeconds(email);
                sendJson(exchange, 429, Map.of(
                    "success", false,
                    "message", "Please wait before requesting another code",
                    "remainingSeconds", remaining
                ));
                return;
            }

            // Generate code
            String code = verifyCodeService.generateCode(email);

            // Send email (or log for development)
            boolean sent = MailService.getInstance().sendVerificationCode(email);
            if (sent) {
                // Log the code for development
                VerifyMC.LOGGER.info("Verification code for {}: {}", email, code);
                sendJson(exchange, 200, Map.of(
                    "success", true,
                    "message", "Verification code sent"
                ));
            } else {
                sendJson(exchange, 500, Map.of("success", false, "message", "Failed to send email"));
            }
        } catch (Exception e) {
            VerifyMC.LOGGER.error("Email send code error", e);
            sendError(exchange, 500, "Internal server error");
        }
    }

    private void handleEmailVerify(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        try {
            Map<String, String> params = parseJsonBody(exchange);
            String email = params.get("email");
            String code = params.get("code");

            if (email == null || code == null) {
                sendJson(exchange, 400, Map.of("success", false, "message", "Email and code are required"));
                return;
            }

            boolean valid = verifyCodeService.checkCode(email, code);
            sendJson(exchange, 200, Map.of(
                "success", true,
                "valid", valid,
                "message", valid ? "Code verified" : "Invalid or expired code"
            ));
        } catch (Exception e) {
            sendError(exchange, 400, "Invalid request");
        }
    }

    private void handleQuestionnaireGet(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        String language = getQueryParam(exchange, "lang");
        if (language == null) {
            language = VerifyMCConfig.LANGUAGE.get();
        }

        try {
            var result = questionnaireService.getQuestionnaire(language);
            sendJson(exchange, 200, gson.fromJson(result, Map.class));
        } catch (Exception e) {
            VerifyMC.LOGGER.error("Questionnaire get error", e);
            sendError(exchange, 500, "Failed to get questionnaire");
        }
    }

    private void handleQuestionnaireSubmit(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        try {
            Map<String, Object> params = parseJsonBodyMap(exchange);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> answersList = (List<Map<String, Object>>) params.get("answers");

            if (answersList == null) {
                sendJson(exchange, 400, Map.of("success", false, "message", "Answers are required"));
                return;
            }

            // Convert answers to the format expected by QuestionnaireService
            Map<Integer, QuestionnaireService.QuestionAnswer> answers = new HashMap<>();
            for (Map<String, Object> answerMap : answersList) {
                Object questionIdObj = answerMap.get("questionId");
                if (questionIdObj == null) continue;

                int questionId = ((Number) questionIdObj).intValue();
                String type = (String) answerMap.getOrDefault("type", "single_choice");

                @SuppressWarnings("unchecked")
                List<Number> optionIds = (List<Number>) answerMap.get("selectedOptionIds");
                List<Integer> selectedOptionIds = new ArrayList<>();
                if (optionIds != null) {
                    for (Number n : optionIds) {
                        selectedOptionIds.add(n.intValue());
                    }
                }

                String textAnswer = (String) answerMap.getOrDefault("textAnswer", "");
                answers.put(questionId, new QuestionnaireService.QuestionAnswer(type, selectedOptionIds, textAnswer));
            }

            QuestionnaireService.QuestionnaireResult result = questionnaireService.scoreAnswers(answers);

            sendJson(exchange, 200, gson.fromJson(result.toJson(), Map.class));
        } catch (Exception e) {
            VerifyMC.LOGGER.error("Questionnaire submit error", e);
            sendError(exchange, 500, "Failed to process questionnaire");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonBodyMap(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return gson.fromJson(sb.toString(), Map.class);
    }

    // --- Discord OAuth handlers ---

    private void handleDiscordAuthUrl(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        try {
            Map<String, String> params = parseJsonBody(exchange);
            String username = params.get("username");

            if (username == null || username.isEmpty()) {
                sendJson(exchange, 400, Map.of("success", false, "message", "Username is required"));
                return;
            }

            if (!discordService.isEnabled()) {
                sendJson(exchange, 503, Map.of("success", false, "message", "Discord integration is not configured"));
                return;
            }

            String authUrl = discordService.generateAuthUrl(username);
            if (authUrl == null) {
                sendJson(exchange, 500, Map.of("success", false, "message", "Failed to generate auth URL"));
                return;
            }

            sendJson(exchange, 200, Map.of(
                "success", true,
                "authUrl", authUrl
            ));
        } catch (Exception e) {
            VerifyMC.LOGGER.error("Discord auth URL error", e);
            sendError(exchange, 500, "Internal server error");
        }
    }

    private void handleDiscordCallback(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        try {
            Map<String, String> params = parseJsonBody(exchange);
            String code = params.get("code");
            String state = params.get("state");

            if (code == null || state == null) {
                sendJson(exchange, 400, Map.of("success", false, "message", "Code and state are required"));
                return;
            }

            DiscordService.DiscordCallbackResult result = discordService.handleCallback(code, state);
            sendJson(exchange, 200, gson.fromJson(result.toJson(), Map.class));
        } catch (Exception e) {
            VerifyMC.LOGGER.error("Discord callback error", e);
            sendError(exchange, 500, "Internal server error");
        }
    }

    private void handleDiscordStatus(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET") && !exchange.getRequestMethod().equals("POST")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        String username = null;
        if (exchange.getRequestMethod().equals("POST")) {
            try {
                Map<String, String> params = parseJsonBody(exchange);
                username = params.get("username");
            } catch (Exception e) {
                // Ignore
            }
        } else {
            username = getQueryParam(exchange, "username");
        }

        sendJson(exchange, 200, Map.of(
            "success", true,
            "enabled", discordService.isEnabled(),
            "required", discordService.isRequired(),
            "linked", username != null && discordService.isLinked(username)
        ));
    }

    private void handleDiscordUnlink(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        // Verify user token
        String token = extractToken(exchange);
        if (token == null || !authManager.validateToken(token)) {
            sendError(exchange, 401, "Unauthorized");
            return;
        }

        User user = authManager.getUserByToken(token);
        if (user == null) {
            sendError(exchange, 404, "User not found");
            return;
        }

        boolean success = discordService.unlinkUser(user.getUsername());
        sendJson(exchange, 200, Map.of(
            "success", success,
            "message", success ? "Discord account unlinked" : "Failed to unlink Discord account"
        ));
    }

    private void handleStatic(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        
        if (path.startsWith("/api/")) {
            sendError(exchange, 404, "API endpoint not found");
            return;
        }
        
        String resourcePath = "/assets/verifymc/www" + path;
        
        if (path.equals("/") || path.equals("/index.html")) {
            resourcePath = "/assets/verifymc/www/index.html";
        }
        
        InputStream is = getClass().getResourceAsStream(resourcePath);
        
        if (is == null) {
            is = getClass().getResourceAsStream("/assets/verifymc/www/index.html");
            if (is == null) {
                sendError(exchange, 404, "Page not found");
                return;
            }
            resourcePath = "/assets/verifymc/www/index.html";
        }
        
        String contentType = getContentType(resourcePath);
        byte[] content = is.readAllBytes();
        is.close();
        
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.sendResponseHeaders(200, content.length);
        OutputStream os = exchange.getResponseBody();
        os.write(content);
        os.close();
    }
    
    private String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=UTF-8";
        if (path.endsWith(".css")) return "text/css; charset=UTF-8";
        if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (path.endsWith(".json")) return "application/json; charset=UTF-8";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".ico")) return "image/x-icon";
        if (path.endsWith(".woff") || path.endsWith(".woff2")) return "font/woff2";
        if (path.endsWith(".ttf")) return "font/ttf";
        return "application/octet-stream";
    }
    
    private Map<String, String> parseJsonBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        java.lang.reflect.Type type = new TypeToken<Map<String, String>>(){}.getType();
        return gson.fromJson(sb.toString(), type);
    }
    
    private String getQueryParam(HttpExchange exchange, String name) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) return null;
        
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair[0].equals(name)) {
                return pair.length > 1 ? pair[1] : "";
            }
        }
        return null;
    }
    
    private String extractToken(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null) return null;
        
        // Handle Bearer token format
        if (authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        
        return authHeader.trim();
    }
    
    private void sendJson(HttpExchange exchange, int code, Object data) throws IOException {
        String json = gson.toJson(data);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
    
    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        sendJson(exchange, code, Map.of("success", false, "message", message));
    }

    private void handleDownloads(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        try {
            // Get download resources from config
            String resourcesJson = VerifyMCConfig.DOWNLOAD_RESOURCES_JSON.get();
            
            // Parse the JSON array
            java.lang.reflect.Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
            List<Map<String, Object>> resources = gson.fromJson(resourcesJson, listType);
            
            if (resources == null) {
                resources = new ArrayList<>();
            }

            sendJson(exchange, 200, Map.of(
                "success", true,
                "resources", resources
            ));
        } catch (Exception e) {
            VerifyMC.LOGGER.error("Downloads get error", e);
            sendJson(exchange, 200, Map.of(
                "success", true,
                "resources", new ArrayList<>()
            ));
        }
    }
}
