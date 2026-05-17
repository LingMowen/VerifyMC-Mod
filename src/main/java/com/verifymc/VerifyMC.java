package com.verifymc;

import com.mojang.logging.LogUtils;
import com.verifymc.auth.AuthManager;
import com.verifymc.command.VmcCommand;
import com.verifymc.config.VerifyMCConfig;
import com.verifymc.core.OpsManager;
import com.verifymc.db.FileUserDao;
import com.verifymc.db.User;
import com.verifymc.db.UserDao;
import com.verifymc.listener.PlayerLoginListener;
import com.verifymc.service.MetricsService;
import com.verifymc.service.VersionCheckService;
import com.verifymc.web.WebServer;
import com.verifymc.whitelist.WhitelistManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

import java.io.File;

@Mod("verifymc")
public class VerifyMC {
    public static final String MODID = "verifymc";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static VerifyMC instance;
    
    private UserDao userDao;
    private AuthManager authManager;
    private WhitelistManager whitelistManager;
    private OpsManager opsManager;
    private WebServer webServer;
    private PlayerLoginListener playerLoginListener;
    private MetricsService metricsService;
    private VersionCheckService versionCheckService;

    public VerifyMC(net.neoforged.bus.api.IEventBus modEventBus, ModContainer modContainer) {
        instance = this;

        modEventBus.addListener(this::commonSetup);
        
        modContainer.registerConfig(ModConfig.Type.COMMON, VerifyMCConfig.SPEC);

        NeoForge.EVENT_BUS.register(this);

        LOGGER.info("VerifyMC initialized!");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            userDao = new FileUserDao();
            authManager = new AuthManager(userDao);
            whitelistManager = new WhitelistManager(userDao);
            
            LOGGER.info("VerifyMC common setup completed!");
        });
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        LOGGER.info("Server started, initializing VerifyMC web server...");

        // Initialize OpsManager with the server instance
        opsManager = new OpsManager(event.getServer());
        authManager.setOpsManager(opsManager);

        // Create default admin account if ops exist but no users
        createDefaultAdminAccount();

        playerLoginListener = new PlayerLoginListener(whitelistManager);
        playerLoginListener.setOpsManager(opsManager);
        NeoForge.EVENT_BUS.register(playerLoginListener);

        try {
            int port = VerifyMCConfig.WEB_PORT.get();
            String host = VerifyMCConfig.WEB_HOST.get();

            // Get data folder for storing data files
            // Use server directory or fallback to current working directory
            File serverDir = event.getServer().getServerDirectory().toFile();
            // If server directory is root or invalid, use working directory
            if (serverDir == null || serverDir.getPath().equals("/") || !serverDir.isDirectory()) {
                serverDir = new File(System.getProperty("user.dir"));
                LOGGER.info("Using working directory as data folder base: {}", serverDir.getAbsolutePath());
            }
            File dataFolder = new File(serverDir, "verifymc");
            if (!dataFolder.exists()) {
                boolean created = dataFolder.mkdirs();
                if (created) {
                    LOGGER.info("Created data folder: {}", dataFolder.getAbsolutePath());
                } else {
                    LOGGER.warn("Failed to create data folder: {}", dataFolder.getAbsolutePath());
                }
            }

            webServer = new WebServer(port, host, userDao, authManager, whitelistManager, opsManager, event.getServer(), dataFolder);
            webServer.start();

            LOGGER.info("Web server started on {}:{}", host, port);

            // Start optional services
            if (VerifyMCConfig.ENABLE_METRICS.get()) {
                metricsService = new MetricsService(this);
                metricsService.start();
            }

            if (VerifyMCConfig.ENABLE_VERSION_CHECK.get()) {
                versionCheckService = new VersionCheckService(this);
                versionCheckService.checkAsync();
            }

        } catch (Exception e) {
            LOGGER.error("Failed to start web server", e);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Server stopping, shutting down VerifyMC...");

        if (metricsService != null) {
            metricsService.stop();
        }

        if (webServer != null) {
            webServer.stop();
        }

        if (playerLoginListener != null) {
            NeoForge.EVENT_BUS.unregister(playerLoginListener);
        }

        LOGGER.info("VerifyMC shutdown complete");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        VmcCommand.register(event.getDispatcher());
    }

    public static VerifyMC getInstance() {
        return instance;
    }
    
    public UserDao getUserDao() {
        return userDao;
    }
    
    public AuthManager getAuthManager() {
        return authManager;
    }
    
    public WhitelistManager getWhitelistManager() {
        return whitelistManager;
    }
    
    public WebServer getWebServer() {
        return webServer;
    }

    public OpsManager getOpsManager() {
        return opsManager;
    }

    public MetricsService getMetricsService() {
        return metricsService;
    }

    public VersionCheckService getVersionCheckService() {
        return versionCheckService;
    }

    public net.minecraft.server.MinecraftServer getServer() {
        return webServer != null ? webServer.getServer() : null;
    }

    private void createDefaultAdminAccount() {
        // Check if there are any users in the database
        if (userDao.count() > 0) {
            LOGGER.info("Users already exist, skipping default admin account creation");
            return;
        }

        // Check if there are ops configured
        if (opsManager == null || opsManager.getOps().isEmpty()) {
            LOGGER.warn("No ops configured in ops.json, skipping default admin account creation");
            return;
        }

        // Create default admin account for the first op
        String defaultAdmin = opsManager.getOps().get(0);
        String defaultPassword = "admin123"; // Default password
        String defaultEmail = defaultAdmin + "@verifymc.local";

        LOGGER.info("Creating default admin account for user: {}", defaultAdmin);

        var result = authManager.register(defaultAdmin, defaultEmail, defaultPassword);

        if (result.isSuccess()) {
            // Auto-approve the admin account
            userDao.findByUsername(defaultAdmin).ifPresent(user -> {
                userDao.updateStatus(user.getId(), User.UserStatus.APPROVED);
                LOGGER.info("Admin account auto-approved and added to whitelist");
            });
            
            LOGGER.info("========================================");
            LOGGER.info("Default admin account created!");
            LOGGER.info("Username: {}", defaultAdmin);
            LOGGER.info("Password: {}", defaultPassword);
            LOGGER.info("Email: {}", defaultEmail);
            LOGGER.info("Status: APPROVED");
            LOGGER.info("========================================");
            LOGGER.info("Please change the default password after first login!");
            
            // Create a regular user for testing whitelist
            createDefaultRegularUser();
        } else {
            LOGGER.error("Failed to create default admin account: {}", result.getMessage());
        }
    }
    
    private void createDefaultRegularUser() {
        String regularUsername = "testuser";
        String regularPassword = "test123";
        String regularEmail = "testuser@verifymc.local";
        
        LOGGER.info("Creating default regular user for whitelist testing: {}", regularUsername);
        
        var result = authManager.register(regularUsername, regularEmail, regularPassword);
        
        if (result.isSuccess()) {
            LOGGER.info("========================================");
            LOGGER.info("Default regular user created!");
            LOGGER.info("Username: {}", regularUsername);
            LOGGER.info("Password: {}", regularPassword);
            LOGGER.info("Email: {}", regularEmail);
            LOGGER.info("Status: PENDING (needs admin approval)");
            LOGGER.info("========================================");
            LOGGER.info("This user can be used to test the whitelist system!");
        } else {
            LOGGER.error("Failed to create default regular user: {}", result.getMessage());
        }
    }
}
