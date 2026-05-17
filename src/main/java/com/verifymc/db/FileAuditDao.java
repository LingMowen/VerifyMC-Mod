package com.verifymc.db;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.verifymc.VerifyMC;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class FileAuditDao implements AuditDao {
    private final File auditFile;
    private final Gson gson;
    private List<AuditRecord> audits;
    private final AtomicLong nextId;

    public FileAuditDao(File dataFolder) {
        this.auditFile = new File(dataFolder, "audits.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.audits = new ArrayList<>();
        this.nextId = new AtomicLong(1);
        load();
    }

    private void load() {
        if (!auditFile.exists()) {
            return;
        }
        try (FileReader reader = new FileReader(auditFile)) {
            List<AuditRecord> loaded = gson.fromJson(reader, new TypeToken<List<AuditRecord>>(){}.getType());
            if (loaded != null) {
                audits = loaded;
                // Find max id
                long maxId = audits.stream()
                        .mapToLong(a -> a.id() != null ? a.id() : 0)
                        .max()
                        .orElse(0);
                nextId.set(maxId + 1);
            }
        } catch (IOException e) {
            VerifyMC.LOGGER.error("Failed to load audits", e);
        }
    }

    @Override
    public void addAudit(AuditRecord audit) {
        AuditRecord withId = new AuditRecord(
                nextId.getAndIncrement(),
                audit.action(),
                audit.operator(),
                audit.target(),
                audit.detail(),
                audit.timestamp()
        );
        audits.add(withId);
        save();
    }

    @Override
    public List<AuditRecord> getAllAudits() {
        return new ArrayList<>(audits);
    }

    @Override
    public void save() {
        try (FileWriter writer = new FileWriter(auditFile)) {
            gson.toJson(audits, writer);
        } catch (IOException e) {
            VerifyMC.LOGGER.error("Failed to save audits", e);
        }
    }
}
