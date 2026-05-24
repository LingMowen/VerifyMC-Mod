package com.verifymc.db;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.verifymc.VerifyMC;

import java.io.Closeable;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * MySQL implementation of UserDao
 */
public class MysqlUserDao implements UserDao, Closeable {
    private final Gson gson = new Gson();
    private Connection connection;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;

    public MysqlUserDao(String host, int port, String database, String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        init();
    }

    private void init() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database +
                    "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
            connection = DriverManager.getConnection(url, username, password);
            createTables();
            VerifyMC.LOGGER.info("MySQL connection established: {}:{}", host, port);
        } catch (Exception e) {
            VerifyMC.LOGGER.error("Failed to connect to MySQL", e);
            throw new RuntimeException("Failed to connect to MySQL", e);
        }
    }

    private void createTables() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                id VARCHAR(36) PRIMARY KEY,
                username VARCHAR(64) NOT NULL UNIQUE,
                email VARCHAR(128) NOT NULL UNIQUE,
                password_hash VARCHAR(256) NOT NULL,
                salt VARCHAR(64) NOT NULL,
                status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
                banned BOOLEAN DEFAULT FALSE,
                ban_reason TEXT,
                ban_expiry BIGINT,
                discord_id VARCHAR(64),
                questionnaire_answers TEXT,
                created_at BIGINT NOT NULL,
                updated_at BIGINT NOT NULL,
                INDEX idx_username (username),
                INDEX idx_email (email),
                INDEX idx_status (status)
            )
            """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database +
                    "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
            connection = DriverManager.getConnection(url, username, password);
        }
        return connection;
    }

    @Override
    public boolean save(User user) {
        String sql = """
            INSERT INTO users (id, username, email, password_hash, salt, status, banned, ban_reason, ban_expiry, discord_id, questionnaire_answers, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                username = VALUES(username),
                email = VALUES(email),
                password_hash = VALUES(password_hash),
                salt = VALUES(salt),
                status = VALUES(status),
                banned = VALUES(banned),
                ban_reason = VALUES(ban_reason),
                ban_expiry = VALUES(ban_expiry),
                discord_id = VALUES(discord_id),
                questionnaire_answers = VALUES(questionnaire_answers),
                updated_at = VALUES(updated_at)
            """;
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, user.getId().toString());
            stmt.setString(2, user.getUsername());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getPasswordHash());
            stmt.setString(5, user.getSalt());
            stmt.setString(6, user.getStatus().name());
            stmt.setBoolean(7, user.isBanned());
            stmt.setString(8, user.getBanReason());
            stmt.setObject(9, user.getBanExpiry() != null ? user.getBanExpiry().toEpochMilli() : null);
            stmt.setString(10, user.getDiscordId());
            stmt.setString(11, user.getQuestionnaireAnswers() != null ?
                    gson.toJson(user.getQuestionnaireAnswers()) : null);
            stmt.setLong(12, user.getCreatedAt().toEpochMilli());
            stmt.setLong(13, user.getUpdatedAt().toEpochMilli());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            VerifyMC.LOGGER.error("Failed to save user: {}", user.getUsername(), e);
            return false;
        }
    }

    @Override
    public Optional<User> findById(UUID id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, id.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToUser(rs));
            }
        } catch (SQLException e) {
            VerifyMC.LOGGER.error("Failed to find user by id: {}", id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToUser(rs));
            }
        } catch (SQLException e) {
            VerifyMC.LOGGER.error("Failed to find user by username: {}", username, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToUser(rs));
            }
        } catch (SQLException e) {
            VerifyMC.LOGGER.error("Failed to find user by email: {}", email, e);
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapRowToUser(rs));
            }
        } catch (SQLException e) {
            VerifyMC.LOGGER.error("Failed to find all users", e);
        }
        return users;
    }

    @Override
    public List<User> findByStatus(User.UserStatus status) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE status = ? ORDER BY created_at DESC";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, status.name());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                users.add(mapRowToUser(rs));
            }
        } catch (SQLException e) {
            VerifyMC.LOGGER.error("Failed to find users by status: {}", status, e);
        }
        return users;
    }

    @Override
    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            VerifyMC.LOGGER.error("Failed to check username existence: {}", username, e);
        }
        return false;
    }

    @Override
    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            VerifyMC.LOGGER.error("Failed to check email existence: {}", email, e);
        }
        return false;
    }

    @Override
    public boolean delete(UUID id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, id.toString());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            VerifyMC.LOGGER.error("Failed to delete user: {}", id, e);
            return false;
        }
    }

    @Override
    public Optional<User> findByDiscordId(String discordId) {
        String sql = "SELECT * FROM users WHERE discord_id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, discordId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToUser(rs));
            }
        } catch (SQLException e) {
            VerifyMC.LOGGER.error("Failed to find user by discord id: {}", discordId, e);
        }
        return Optional.empty();
    }

    @Override
    public List<User> findBanned() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE banned = true ORDER BY created_at DESC";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapRowToUser(rs));
            }
        } catch (SQLException e) {
            VerifyMC.LOGGER.error("Failed to find banned users", e);
        }
        return users;
    }

    @Override
    public void banUser(UUID id, String reason, long durationMinutes) {
        String sql = "UPDATE users SET banned = true, ban_reason = ?, ban_expiry = ? WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, reason);
            stmt.setObject(2, durationMinutes > 0 ? 
                    System.currentTimeMillis() + durationMinutes * 60 * 1000 : null);
            stmt.setString(3, id.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            VerifyMC.LOGGER.error("Failed to ban user: {}", id, e);
        }
    }

    @Override
    public void unbanUser(UUID id) {
        String sql = "UPDATE users SET banned = false, ban_reason = null, ban_expiry = null WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, id.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            VerifyMC.LOGGER.error("Failed to unban user: {}", id, e);
        }
    }

    @Override
    public void updateStatus(UUID id, User.UserStatus status) {
        String sql = "UPDATE users SET status = ? WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setString(2, id.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            VerifyMC.LOGGER.error("Failed to update user status: {}", id, e);
        }
    }

    @Override
    public void cleanupExpiredBans() {
        String sql = "UPDATE users SET banned = false, ban_reason = null, ban_expiry = null WHERE banned = true AND ban_expiry IS NOT NULL AND ban_expiry < ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setLong(1, System.currentTimeMillis());
            int count = stmt.executeUpdate();
            if (count > 0) {
                VerifyMC.LOGGER.info("Cleaned up {} expired bans", count);
            }
        } catch (SQLException e) {
            VerifyMC.LOGGER.error("Failed to cleanup expired bans", e);
        }
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                VerifyMC.LOGGER.info("MySQL connection closed");
            } catch (SQLException e) {
                VerifyMC.LOGGER.error("Failed to close MySQL connection", e);
            }
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM users";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            VerifyMC.LOGGER.error("Failed to count users", e);
        }
        return 0;
    }

    @Override
    public boolean updatePassword(String username, String newPasswordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE username = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, newPasswordHash);
            stmt.setString(2, username);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            VerifyMC.LOGGER.error("Failed to update password for user: {}", username, e);
        }
        return false;
    }

    @Override
    public int countUsersByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, email.toLowerCase());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            VerifyMC.LOGGER.error("Failed to count users by email: {}", email, e);
        }
        return 0;
    }

    @Override
    public boolean updateUserEmail(String username, String newEmail, boolean caseSensitive) {
        String sql = "UPDATE users SET email = ? WHERE username = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, newEmail.toLowerCase());
            stmt.setString(2, username);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            VerifyMC.LOGGER.error("Failed to update email for user: {}", username, e);
        }
        return false;
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        User user = new User(
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("salt")
        );

        // Set ID
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, UUID.fromString(rs.getString("id")));
        } catch (Exception e) {
            // Ignore
        }

        // Set status
        user.setStatus(User.UserStatus.valueOf(rs.getString("status")));

        // Set ban info
        user.setBanned(rs.getBoolean("banned"));
        user.setBanReason(rs.getString("ban_reason"));
        Long banExpiry = (Long) rs.getObject("ban_expiry");
        if (banExpiry != null) {
            user.setBanExpiry(Instant.ofEpochMilli(banExpiry));
        }

        // Set Discord ID
        user.setDiscordId(rs.getString("discord_id"));

        // Set questionnaire answers
        String answersJson = rs.getString("questionnaire_answers");
        if (answersJson != null && !answersJson.isEmpty()) {
            try {
                List<String> answers = gson.fromJson(answersJson, new TypeToken<List<String>>(){}.getType());
                user.setQuestionnaireAnswers(answers);
            } catch (Exception e) {
                // Ignore
            }
        }

        // Set timestamps
        user.setCreatedAt(Instant.ofEpochMilli(rs.getLong("created_at")));
        user.setUpdatedAt(Instant.ofEpochMilli(rs.getLong("updated_at")));

        return user;
    }
}
