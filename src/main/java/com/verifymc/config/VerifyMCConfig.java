package com.verifymc.config;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = "verifymc", bus = EventBusSubscriber.Bus.MOD)
public class VerifyMCConfig {
    
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    
    public static final ModConfigSpec.IntValue WEB_PORT = BUILDER
            .comment("Web server port")
            .defineInRange("web_port", 8080, 1, 65535);
    
    public static final ModConfigSpec.ConfigValue<String> WEB_HOST = BUILDER
            .comment("Web server host")
            .define("web_host", "0.0.0.0");
    
    public static final ModConfigSpec.ConfigValue<String> LANGUAGE = BUILDER
            .comment("Language (en, zh)")
            .define("language", "en");
    
    public static final ModConfigSpec.BooleanValue DEBUG = BUILDER
            .comment("Enable debug mode")
            .define("debug", false);
    
    public static final ModConfigSpec.ConfigValue<String> SMTP_HOST = BUILDER
            .comment("SMTP server host")
            .define("smtp_host", "smtp.qq.com");
    
    public static final ModConfigSpec.IntValue SMTP_PORT = BUILDER
            .comment("SMTP server port")
            .defineInRange("smtp_port", 587, 1, 65535);
    
    public static final ModConfigSpec.ConfigValue<String> SMTP_USERNAME = BUILDER
            .comment("SMTP username")
            .define("smtp_username", "");
    
    public static final ModConfigSpec.ConfigValue<String> SMTP_PASSWORD = BUILDER
            .comment("SMTP password")
            .define("smtp_password", "");
    
    public static final ModConfigSpec.ConfigValue<String> SMTP_FROM = BUILDER
            .comment("SMTP from address")
            .define("smtp_from", "");
    
    public static final ModConfigSpec.BooleanValue SMTP_USE_TLS = BUILDER
            .comment("Use TLS for SMTP")
            .define("smtp_use_tls", true);
    
    public static final ModConfigSpec.ConfigValue<String> DISCORD_CLIENT_ID = BUILDER
            .comment("Discord OAuth client ID")
            .define("discord_client_id", "");
    
    public static final ModConfigSpec.ConfigValue<String> DISCORD_CLIENT_SECRET = BUILDER
            .comment("Discord OAuth client secret")
            .define("discord_client_secret", "");
    
    public static final ModConfigSpec.ConfigValue<String> DISCORD_REDIRECT_URI = BUILDER
            .comment("Discord OAuth redirect URI")
            .define("discord_redirect_uri", "");
    
    public static final ModConfigSpec.ConfigValue<String> ADMIN_INITIAL_PASSWORD = BUILDER
            .comment("Initial admin password")
            .define("admin_initial_password", "admin123");
    
    public static final ModConfigSpec.ConfigValue<String> STORAGE_TYPE = BUILDER
            .comment("Storage type (file, mysql)")
            .define("storage_type", "file");

    public static final ModConfigSpec.BooleanValue QUESTIONNAIRE_ENABLED = BUILDER
            .comment("Enable questionnaire for registration")
            .define("questionnaire.enabled", false);

    public static final ModConfigSpec.IntValue QUESTIONNAIRE_PASS_SCORE = BUILDER
            .comment("Minimum score to pass questionnaire")
            .defineInRange("questionnaire.pass_score", 60, 0, 100);

    public static final ModConfigSpec.BooleanValue EMAIL_VERIFICATION_ENABLED = BUILDER
            .comment("Enable email verification")
            .define("email_verification.enabled", false);

    // MySQL Configuration
    public static final ModConfigSpec.ConfigValue<String> MYSQL_HOST = BUILDER
            .comment("MySQL host")
            .define("mysql.host", "localhost");

    public static final ModConfigSpec.IntValue MYSQL_PORT = BUILDER
            .comment("MySQL port")
            .defineInRange("mysql.port", 3306, 1, 65535);

    public static final ModConfigSpec.ConfigValue<String> MYSQL_DATABASE = BUILDER
            .comment("MySQL database name")
            .define("mysql.database", "verifymc");

    public static final ModConfigSpec.ConfigValue<String> MYSQL_USERNAME = BUILDER
            .comment("MySQL username")
            .define("mysql.username", "root");

    public static final ModConfigSpec.ConfigValue<String> MYSQL_PASSWORD = BUILDER
            .comment("MySQL password")
            .define("mysql.password", "");

    // WebSocket Configuration
    public static final ModConfigSpec.BooleanValue WEBSOCKET_ENABLED = BUILDER
            .comment("Enable WebSocket for real-time notifications")
            .define("websocket.enabled", true);

    public static final ModConfigSpec.IntValue WEBSOCKET_PORT = BUILDER
            .comment("WebSocket server port")
            .defineInRange("websocket.port", 8081, 1, 65535);

    // LLM Scoring Configuration
    public static final ModConfigSpec.BooleanValue LLM_SCORING_ENABLED = BUILDER
            .comment("Enable LLM-based essay scoring for questionnaire")
            .define("llm.scoring_enabled", false);

    public static final ModConfigSpec.ConfigValue<String> LLM_PROVIDER = BUILDER
            .comment("LLM provider name (e.g., openai, azure, deepseek)")
            .define("llm.provider", "openai");

    public static final ModConfigSpec.ConfigValue<String> LLM_API_BASE = BUILDER
            .comment("LLM API base URL (e.g., https://api.openai.com/v1)")
            .define("llm.api_base", "");

    public static final ModConfigSpec.ConfigValue<String> LLM_API_KEY = BUILDER
            .comment("LLM API key")
            .define("llm.api_key", "");

    public static final ModConfigSpec.ConfigValue<String> LLM_MODEL = BUILDER
            .comment("LLM model name (e.g., gpt-4o-mini, gpt-3.5-turbo)")
            .define("llm.model", "");

    public static final ModConfigSpec.IntValue LLM_TIMEOUT_MS = BUILDER
            .comment("LLM API timeout in milliseconds")
            .defineInRange("llm.timeout_ms", 30000, 5000, 120000);

    public static final ModConfigSpec.IntValue LLM_RETRY = BUILDER
            .comment("Number of retries for LLM API calls")
            .defineInRange("llm.retry", 2, 0, 5);

    public static final ModConfigSpec.IntValue LLM_MAX_CONCURRENCY = BUILDER
            .comment("Maximum concurrent LLM API calls")
            .defineInRange("llm.max_concurrency", 5, 1, 20);

    public static final ModConfigSpec.IntValue LLM_ACQUIRE_TIMEOUT_MS = BUILDER
            .comment("Timeout to acquire LLM API semaphore in milliseconds")
            .defineInRange("llm.acquire_timeout_ms", 5000, 1000, 30000);

    public static final ModConfigSpec.IntValue LLM_CIRCUIT_BREAKER_THRESHOLD = BUILDER
            .comment("Number of consecutive failures before opening circuit breaker")
            .defineInRange("llm.circuit_breaker_threshold", 5, 1, 20);

    public static final ModConfigSpec.IntValue LLM_CIRCUIT_BREAKER_OPEN_MS = BUILDER
            .comment("Duration circuit breaker stays open in milliseconds")
            .defineInRange("llm.circuit_breaker_open_ms", 60000, 10000, 300000);

    public static final ModConfigSpec.ConfigValue<String> LLM_SYSTEM_PROMPT = BUILDER
            .comment("System prompt for LLM scoring")
            .define("llm.system_prompt", "You are an impartial essay scoring assistant. Score answers based on the provided scoring rules. Be fair and consistent.");

    // Optional Services Configuration
    public static final ModConfigSpec.BooleanValue ENABLE_METRICS = BUILDER
            .comment("Enable anonymous usage statistics collection via bStats")
            .define("metrics.enabled", true);

    public static final ModConfigSpec.BooleanValue ENABLE_VERSION_CHECK = BUILDER
            .comment("Enable automatic version check for updates")
            .define("version_check.enabled", true);

    public static final ModConfigSpec SPEC = BUILDER.build();
    
    @SubscribeEvent
    static void onLoad(final ModConfigEvent.Loading event) {
    }
}
