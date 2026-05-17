package com.verifymc.db;

import java.util.List;

public interface AuditDao {
    void addAudit(AuditRecord audit);
    List<AuditRecord> getAllAudits();
    void save();

    /**
     * Closes any resources held by this DAO (e.g., database connections).
     * Default implementation does nothing.
     */
    default void close() {
        // Default: no-op
    }
}
