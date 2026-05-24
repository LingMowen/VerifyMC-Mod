package com.verifymc.db;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.verifymc.VerifyMC;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileUserDao implements UserDao {
    
    private static final String DEFAULT_STORAGE_PATH = "config/verifymc/users.json";
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(Instant.class, new InstantAdapter())
            .create();
    
    private final Path storagePath;
    private final ConcurrentHashMap<UUID, User> userCache;
    private final ReadWriteLock lock;
    
    public FileUserDao() {
        this(DEFAULT_STORAGE_PATH);
    }
    
    public FileUserDao(String storagePath) {
        this.storagePath = Paths.get(storagePath);
        this.userCache = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
        loadFromFile();
    }
    
    private void loadFromFile() {
        lock.writeLock().lock();
        try {
            if (Files.exists(storagePath)) {
                String json = Files.readString(storagePath);
                Type type = new TypeToken<List<User>>(){}.getType();
                List<User> users = GSON.fromJson(json, type);
                if (users != null) {
                    for (User user : users) {
                        userCache.put(user.getId(), user);
                    }
                }
                VerifyMC.LOGGER.info("Loaded {} users from file", userCache.size());
            } else {
                Files.createDirectories(storagePath.getParent());
                saveToFile();
            }
        } catch (Exception e) {
            VerifyMC.LOGGER.error("Failed to load users from file", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private void saveToFile() {
        lock.readLock().lock();
        try {
            String json = GSON.toJson(new ArrayList<>(userCache.values()));
            Files.writeString(storagePath, json);
        } catch (Exception e) {
            VerifyMC.LOGGER.error("Failed to save users to file", e);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(userCache.get(id));
    }
    
    @Override
    public Optional<User> findByUsername(String username) {
        return userCache.values().stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }
    
    @Override
    public Optional<User> findByEmail(String email) {
        return userCache.values().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }
    
    @Override
    public Optional<User> findByDiscordId(String discordId) {
        return userCache.values().stream()
                .filter(u -> discordId != null && discordId.equals(u.getDiscordId()))
                .findFirst();
    }
    
    @Override
    public List<User> findAll() {
        return new ArrayList<>(userCache.values());
    }
    
    @Override
    public List<User> findByStatus(User.UserStatus status) {
        return userCache.values().stream()
                .filter(u -> u.getStatus() == status)
                .toList();
    }
    
    @Override
    public List<User> findBanned() {
        return userCache.values().stream()
                .filter(User::isBanned)
                .toList();
    }
    
    @Override
    public boolean save(User user) {
        lock.writeLock().lock();
        try {
            user.setUpdatedAt(Instant.now());
            userCache.put(user.getId(), user);
            saveToFile();
            return true;
        } catch (Exception e) {
            VerifyMC.LOGGER.error("Failed to save user", e);
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public boolean delete(UUID id) {
        lock.writeLock().lock();
        try {
            userCache.remove(id);
            saveToFile();
            return true;
        } catch (Exception e) {
            VerifyMC.LOGGER.error("Failed to delete user", e);
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }
    
    @Override
    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }
    
    @Override
    public void banUser(UUID id, String reason, long durationMinutes) {
        findById(id).ifPresent(user -> {
            user.setBanned(true);
            user.setBanReason(reason);
            user.setBannedAt(Instant.now());
            if (durationMinutes > 0) {
                user.setBanExpiresAt(Instant.now().plusSeconds(durationMinutes * 60));
            }
            save(user);
        });
    }
    
    @Override
    public void unbanUser(UUID id) {
        findById(id).ifPresent(user -> {
            user.setBanned(false);
            user.setBanReason(null);
            user.setBannedAt(null);
            user.setBanExpiresAt(null);
            save(user);
        });
    }
    
    @Override
    public void updateStatus(UUID id, User.UserStatus status) {
        findById(id).ifPresent(user -> {
            user.setStatus(status);
            save(user);
        });
    }

    @Override
    public void updateStatus(UUID id, User.UserStatus status, String reason) {
        findById(id).ifPresent(user -> {
            user.setStatus(status);
            if (status == User.UserStatus.REJECTED && reason != null) {
                user.setRejectReason(reason);
            }
            save(user);
        });
    }

    @Override
    public void cleanupExpiredBans() {
        userCache.values().stream()
                .filter(User::isBanExpired)
                .forEach(user -> {
                    user.setBanned(false);
                    user.setBanReason(null);
                    user.setBannedAt(null);
                    user.setBanExpiresAt(null);
                    save(user);
                    VerifyMC.LOGGER.info("Auto-unbanned user: {}", user.getUsername());
                });
    }
    
    @Override
    public long count() {
        return userCache.size();
    }

    @Override
    public boolean updatePassword(String username, String newPasswordHash) {
        Optional<User> userOpt = findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(newPasswordHash);
            return save(user);
        }
        return false;
    }

    @Override
    public int countUsersByEmail(String email) {
        String emailLower = email.toLowerCase();
        return (int) userCache.values().stream()
                .filter(u -> u.getEmail() != null && u.getEmail().toLowerCase().equals(emailLower))
                .count();
    }

    @Override
    public boolean updateUserEmail(String username, String newEmail, boolean caseSensitive) {
        Optional<User> userOpt = findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setEmail(newEmail);
            return save(user);
        }
        return false;
    }

    private static class InstantAdapter implements com.google.gson.JsonSerializer<Instant>, com.google.gson.JsonDeserializer<Instant> {
        @Override
        public com.google.gson.JsonElement serialize(Instant src, Type typeOfSrc, com.google.gson.JsonSerializationContext context) {
            return src == null ? null : new com.google.gson.JsonPrimitive(src.toString());
        }
        
        @Override
        public Instant deserialize(com.google.gson.JsonElement json, Type typeOfT, com.google.gson.JsonDeserializationContext context) throws com.google.gson.JsonParseException {
            return json == null || json.isJsonNull() ? null : Instant.parse(json.getAsString());
        }
    }
}
