package com.verifymc.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.verifymc.VerifyMC;
import com.verifymc.auth.PasswordUtil;
import com.verifymc.db.User;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles the /vmc command. Refactored from the inline onCommand method
 * in the original 878-line VerifyMC class.
 * <p>
 * Subcommands:
 *   reload    — Reload configuration
 *   approve   — Approve a pending user
 *   reject    — Reject a pending user
 *   delete    — Delete a user
 *   ban       — Ban a user
 *   unban     — Unban a user
 *   list      — List users by status
 *   info      — Show user info
 *   version   — Show plugin version
 *   createadmin — Create an admin user (NeoForge specific)
 *   listops   — List operators from ops.json (NeoForge specific)
 */
public class VmcCommand {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "reload", "approve", "reject", "delete", "ban", "unban", "list", "info", "version", "createadmin", "listops"
    );

    private static final SuggestionProvider<CommandSourceStack> USERNAME_SUGGESTIONS = (context, builder) -> {
        VerifyMC mod = VerifyMC.getInstance();
        if (mod != null && mod.getUserDao() != null) {
            String remaining = builder.getRemaining().toLowerCase();
            mod.getUserDao().findAll().stream()
                    .map(User::getUsername)
                    .filter(name -> name.toLowerCase().startsWith(remaining))
                    .limit(20)
                    .forEach(builder::suggest);
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> LIST_STATUS_SUGGESTIONS = (context, builder) -> {
        return net.minecraft.commands.SharedSuggestionProvider.suggest(Arrays.asList("all", "pending", "approved", "rejected", "banned"), builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("vmc")
                .requires(source -> source.hasPermission(2)) // Requires OP permission level 2
                .executes(VmcCommand::showUsage)
                .then(Commands.literal("reload")
                    .executes(VmcCommand::handleReload)
                )
                .then(Commands.literal("approve")
                    .then(Commands.argument("username", StringArgumentType.word())
                        .suggests(USERNAME_SUGGESTIONS)
                        .executes(VmcCommand::handleApprove)
                    )
                )
                .then(Commands.literal("reject")
                    .then(Commands.argument("username", StringArgumentType.word())
                        .suggests(USERNAME_SUGGESTIONS)
                        .executes(VmcCommand::handleReject)
                    )
                )
                .then(Commands.literal("delete")
                    .then(Commands.argument("username", StringArgumentType.word())
                        .suggests(USERNAME_SUGGESTIONS)
                        .executes(VmcCommand::handleDelete)
                    )
                )
                .then(Commands.literal("ban")
                    .then(Commands.argument("username", StringArgumentType.word())
                        .suggests(USERNAME_SUGGESTIONS)
                        .executes(VmcCommand::handleBan)
                    )
                )
                .then(Commands.literal("unban")
                    .then(Commands.argument("username", StringArgumentType.word())
                        .suggests(USERNAME_SUGGESTIONS)
                        .executes(VmcCommand::handleUnban)
                    )
                )
                .then(Commands.literal("list")
                    .executes(context -> handleList(context, "all"))
                    .then(Commands.argument("status", StringArgumentType.word())
                        .suggests(LIST_STATUS_SUGGESTIONS)
                        .executes(context -> handleList(context, StringArgumentType.getString(context, "status")))
                    )
                )
                .then(Commands.literal("info")
                    .then(Commands.argument("username", StringArgumentType.word())
                        .suggests(USERNAME_SUGGESTIONS)
                        .executes(VmcCommand::handleInfo)
                    )
                )
                .then(Commands.literal("version")
                    .executes(VmcCommand::handleVersion)
                )
                .then(Commands.literal("createadmin")
                    .then(Commands.argument("username", StringArgumentType.word())
                        .then(Commands.argument("email", StringArgumentType.string())
                            .then(Commands.argument("password", StringArgumentType.greedyString())
                                .executes(VmcCommand::handleCreateAdmin)
                            )
                        )
                    )
                )
                .then(Commands.literal("listops")
                    .executes(VmcCommand::handleListOps)
                )
        );
    }

    private static int showUsage(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> 
            Component.literal("§6[VerifyMC] §fUsage: /vmc <" + String.join("|", SUBCOMMANDS) + ">"),
            false
        );
        return 1;
    }

    private static int handleReload(CommandContext<CommandSourceStack> context) {
        // Reload config logic here
        context.getSource().sendSuccess(() -> Component.literal("§6[VerifyMC] §aConfiguration reloaded."), true);
        return 1;
    }

    private static int handleApprove(CommandContext<CommandSourceStack> context) {
        String target = StringArgumentType.getString(context, "username");
        
        VerifyMC mod = VerifyMC.getInstance();
        if (mod == null || mod.getWhitelistManager() == null) {
            context.getSource().sendFailure(Component.literal("§6[VerifyMC] §cVerifyMC is not fully initialized yet!"));
            return 0;
        }

        var userOpt = mod.getUserDao().findByUsername(target);
        if (userOpt.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§6[VerifyMC] §cUser not found: " + target));
            return 0;
        }

        mod.getWhitelistManager().approveUser(userOpt.get().getId());
        context.getSource().sendSuccess(() -> Component.literal("§6[VerifyMC] §aUser " + target + " approved."), true);
        return 1;
    }

    private static int handleReject(CommandContext<CommandSourceStack> context) {
        String target = StringArgumentType.getString(context, "username");
        
        VerifyMC mod = VerifyMC.getInstance();
        if (mod == null || mod.getWhitelistManager() == null) {
            context.getSource().sendFailure(Component.literal("§6[VerifyMC] §cVerifyMC is not fully initialized yet!"));
            return 0;
        }

        var userOpt = mod.getUserDao().findByUsername(target);
        if (userOpt.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§6[VerifyMC] §cUser not found: " + target));
            return 0;
        }

        mod.getWhitelistManager().rejectUser(userOpt.get().getId());
        context.getSource().sendSuccess(() -> Component.literal("§6[VerifyMC] §cUser " + target + " rejected."), true);
        return 1;
    }

    private static int handleDelete(CommandContext<CommandSourceStack> context) {
        String target = StringArgumentType.getString(context, "username");
        
        VerifyMC mod = VerifyMC.getInstance();
        if (mod == null || mod.getUserDao() == null) {
            context.getSource().sendFailure(Component.literal("§6[VerifyMC] §cVerifyMC is not fully initialized yet!"));
            return 0;
        }

        var userOpt = mod.getUserDao().findByUsername(target);
        if (userOpt.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§6[VerifyMC] §cUser not found: " + target));
            return 0;
        }

        mod.getUserDao().delete(userOpt.get().getId());
        context.getSource().sendSuccess(() -> Component.literal("§6[VerifyMC] §aUser " + target + " deleted."), true);
        return 1;
    }

    private static int handleBan(CommandContext<CommandSourceStack> context) {
        String target = StringArgumentType.getString(context, "username");
        
        VerifyMC mod = VerifyMC.getInstance();
        if (mod == null || mod.getWhitelistManager() == null) {
            context.getSource().sendFailure(Component.literal("§6[VerifyMC] §cVerifyMC is not fully initialized yet!"));
            return 0;
        }

        var userOpt = mod.getUserDao().findByUsername(target);
        if (userOpt.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§6[VerifyMC] §cUser not found: " + target));
            return 0;
        }

        mod.getWhitelistManager().banUser(userOpt.get().getId(), "Banned by admin", 0);
        context.getSource().sendSuccess(() -> Component.literal("§6[VerifyMC] §cUser " + target + " banned."), true);
        return 1;
    }

    private static int handleUnban(CommandContext<CommandSourceStack> context) {
        String target = StringArgumentType.getString(context, "username");
        
        VerifyMC mod = VerifyMC.getInstance();
        if (mod == null || mod.getWhitelistManager() == null) {
            context.getSource().sendFailure(Component.literal("§6[VerifyMC] §cVerifyMC is not fully initialized yet!"));
            return 0;
        }

        var userOpt = mod.getUserDao().findByUsername(target);
        if (userOpt.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§6[VerifyMC] §cUser not found: " + target));
            return 0;
        }

        mod.getWhitelistManager().unbanUser(userOpt.get().getId());
        context.getSource().sendSuccess(() -> Component.literal("§6[VerifyMC] §aUser " + target + " unbanned."), true);
        return 1;
    }

    private static int handleList(CommandContext<CommandSourceStack> context, String statusFilter) {
        VerifyMC mod = VerifyMC.getInstance();
        if (mod == null || mod.getUserDao() == null) {
            context.getSource().sendFailure(Component.literal("§6[VerifyMC] §cVerifyMC is not fully initialized yet!"));
            return 0;
        }

        List<User> users;
        if ("all".equalsIgnoreCase(statusFilter)) {
            users = mod.getUserDao().findAll();
        } else if ("pending".equalsIgnoreCase(statusFilter)) {
            users = mod.getWhitelistManager().getPendingUsers();
        } else if ("banned".equalsIgnoreCase(statusFilter)) {
            users = mod.getWhitelistManager().getBannedUsers();
        } else {
            users = mod.getUserDao().findAll().stream()
                    .filter(u -> u.getStatus().name().equalsIgnoreCase(statusFilter))
                    .collect(Collectors.toList());
        }

        context.getSource().sendSuccess(() -> Component.literal("§6[VerifyMC] §f--- Users (" + statusFilter + ") ---"), false);
        if (users.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("§7  No users found."), false);
        } else {
            for (User user : users) {
                context.getSource().sendSuccess(() -> 
                    Component.literal("§7  " + user.getUsername() + " §f- §e" + user.getStatus().name()),
                    false
                );
            }
        }
        return 1;
    }

    private static int handleInfo(CommandContext<CommandSourceStack> context) {
        String target = StringArgumentType.getString(context, "username");
        
        VerifyMC mod = VerifyMC.getInstance();
        if (mod == null || mod.getUserDao() == null) {
            context.getSource().sendFailure(Component.literal("§6[VerifyMC] §cVerifyMC is not fully initialized yet!"));
            return 0;
        }

        var userOpt = mod.getUserDao().findByUsername(target);
        if (userOpt.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§6[VerifyMC] §cUser not found: " + target));
            return 0;
        }

        User user = userOpt.get();
        context.getSource().sendSuccess(() -> Component.literal("§6[VerifyMC] §f--- User Info ---"), false);
        context.getSource().sendSuccess(() -> Component.literal("§7  Username: §f" + user.getUsername()), false);
        context.getSource().sendSuccess(() -> Component.literal("§7  Email: §f" + user.getEmail()), false);
        context.getSource().sendSuccess(() -> Component.literal("§7  Status: §e" + user.getStatus().name()), false);
        context.getSource().sendSuccess(() -> Component.literal("§7  Banned: §e" + user.isBanned()), false);
        return 1;
    }

    private static int handleVersion(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("§6[VerifyMC] §fVersion: 1.0.0"), false);
        return 1;
    }

    private static int handleCreateAdmin(CommandContext<CommandSourceStack> context) {
        String username = StringArgumentType.getString(context, "username");
        String email = StringArgumentType.getString(context, "email");
        String password = StringArgumentType.getString(context, "password");

        VerifyMC mod = VerifyMC.getInstance();
        if (mod == null || mod.getAuthManager() == null) {
            context.getSource().sendFailure(Component.literal("§6[VerifyMC] §cVerifyMC is not fully initialized yet!"));
            return 0;
        }

        // Check if user already exists
        if (mod.getUserDao().existsByUsername(username)) {
            context.getSource().sendFailure(Component.literal("§6[VerifyMC] §cUser '" + username + "' already exists!"));
            return 0;
        }

        if (mod.getUserDao().existsByEmail(email)) {
            context.getSource().sendFailure(Component.literal("§6[VerifyMC] §cEmail '" + email + "' is already registered!"));
            return 0;
        }

        // Create the user
        String salt = PasswordUtil.generateSalt();
        String passwordHash = PasswordUtil.hashPassword(password, salt);

        User user = new User(username, email, passwordHash, salt);
        mod.getUserDao().save(user);

        // Check if user is in ops.json
        boolean isOp = mod.getOpsManager() != null && mod.getOpsManager().isOp(username);

        context.getSource().sendSuccess(() ->
            Component.literal("§6[VerifyMC] §aAdmin user '" + username + "' created successfully!" +
                (isOp ? " (User is in ops.json)" : " §e(WARNING: User is NOT in ops.json, add to ops.json to grant admin privileges)")),
            true
        );

        VerifyMC.LOGGER.info("Admin user created via command: {} (isOp: {})", username, isOp);
        return 1;
    }

    private static int handleListOps(CommandContext<CommandSourceStack> context) {
        VerifyMC mod = VerifyMC.getInstance();
        if (mod == null || mod.getOpsManager() == null) {
            context.getSource().sendFailure(Component.literal("§6[VerifyMC] §cOpsManager is not initialized yet!"));
            return 0;
        }

        var ops = mod.getOpsManager().getOps();
        if (ops.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("§6[VerifyMC] §fNo operators found in ops.json"), false);
        } else {
            context.getSource().sendSuccess(() -> Component.literal("§6[VerifyMC] §fOperators in ops.json:"), false);
            for (String op : ops) {
                // Check if this op has a web account
                boolean hasAccount = mod.getUserDao().existsByUsername(op);
                context.getSource().sendSuccess(() ->
                    Component.literal("§7  - " + op + (hasAccount ? " §a(has web account)" : " §c(no web account)")),
                    false
                );
            }
        }
        return 1;
    }
}
