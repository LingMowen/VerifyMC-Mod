package com.verifymc.db;

import com.verifymc.VerifyMC;

import java.io.Closeable;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * MySQL implementation of AuditDao
 */
public class MysqlAuditDao implements AuditDao, Closeable {
    private Connection connection;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;

    public MysqlAuditDao(String host, int port, String database, String username, String password) {
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
            VerifyMC.LOGGER.info("MySQL AuditDao connection established: {}:{}", host, port);
        } catch (Exception e) {
            VerifyMC.LOGGER.error("Failed to connect to MySQL for AuditDao", e);
            throw new RuntimeException("Failed to connect to MySQL for AuditDao", e);
        }
    }

    private void createTables() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS audit_logs (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                action VARCHAR(64) NOT NULL,
                operator VARCHAR(64) NOT NULL,
                target VARCHAR(128),
                detail TEXT,
                timestamp BIGINT NOT NULL,
                INDEX idx_action (action),
                INDEX idx_operator (operator),
                INDEX idx_timestamp (timestamp)
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
    public void addAudit(AuditRecord audit) {
        String sql = "INSERT INTO audit_logs (action, operator, target, detail, timestamp) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, audit.action());
            stmt.setString(2, audit.operator());
            stmt.setString(3, audit.target());
            stmt.setString(4, audit.detail());
            stmt.setLong(5, audit.timestamp());
            stmt.executeUpdate();
        } catch (SQLException e) {
            VerifyMC.LOGGER.error("Failed to add audit record", e);
        }
    }

    @Override
    public List<AuditRecord> getAllAudits() {
        List<AuditRecord> audits = new ArrayList<>();
        String sql = "SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 1000";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                audits.add(new AuditRecord(
                        rs.getLong("id"),
                        rs.getString("action"),
                        rs.getString("operator"),
                        rs.getString("target"),
                        rs.getString("detail"),
                        rs.getLong("timestamp")
                ));
            }
        } catch (SQLException e) {
            VerifyMC.LOGGER.error("Failed to get all audits", e);
        }
        return audits;
    }

    @Override
    public void save() {
        // MySQL auto-commits, no need to save
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                VerifyMC.LOGGER.info("MySQL AuditDao connection closed");
            } catch (SQLException e) {
                VerifyMC.LOGGER.error("Failed to close MySQL AuditDao connection", e);
            }
        }
    }
}
