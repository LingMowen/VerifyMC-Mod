package com.verifymc.listener;

import com.verifymc.VerifyMC;
import com.verifymc.core.OpsManager;
import com.verifymc.whitelist.WhitelistManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.UUID;

public class PlayerLoginListener {
    
    private final WhitelistManager whitelistManager;
    private OpsManager opsManager;
    
    public PlayerLoginListener(WhitelistManager whitelistManager) {
        this.whitelistManager = whitelistManager;
    }
    
    public void setOpsManager(OpsManager opsManager) {
        this.opsManager = opsManager;
    }
    
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        String username = player.getName().getString();
        UUID uuid = player.getUUID();
        
        VerifyMC.LOGGER.info("Player attempting to login: {} ({})", username, uuid);
        
        if (isSinglePlayerOwner(player)) {
            VerifyMC.LOGGER.info("Player {} is the singleplayer host, bypassing whitelist", username);
            return;
        }
        
        // Check if player is an operator (admin)
        if (opsManager != null && opsManager.isOp(username)) {
            VerifyMC.LOGGER.info("Player {} is an operator, bypassing whitelist", username);
            return;
        }
        
        if (!whitelistManager.isWhitelisted(username)) {
            VerifyMC.LOGGER.info("Player {} is not whitelisted, kicking...", username);
            player.connection.disconnect(
                Component.literal("You are not whitelisted on this server. Please apply at the website.")
            );
            return;
        }
        
        VerifyMC.LOGGER.info("Player {} logged in successfully", username);
    }
    
    private boolean isSinglePlayerOwner(ServerPlayer player) {
        // Check if we're in singleplayer mode by checking the server type
        // In a dedicated server, this should always return false
        var server = player.getServer();
        if (server == null) {
            return false;
        }
        // Check if it's an integrated server (singleplayer) by checking the class name
        // without actually loading the client-side class
        String serverClassName = server.getClass().getName();
        if (serverClassName.contains("IntegratedServer")) {
            // Use reflection to avoid direct class reference
            try {
                java.lang.reflect.Method method = server.getClass().getMethod("isSingleplayerOwner", 
                    net.minecraft.world.entity.player.ProfilePublicKey.Data.class);
                return (boolean) method.invoke(server, player.getGameProfile());
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
}
