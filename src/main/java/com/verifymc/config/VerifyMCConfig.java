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

    // Username Configuration
    public static final ModConfigSpec.BooleanValue USERNAME_CASE_SENSITIVE = BUILDER
            .comment("Enable case-sensitive username matching")
            .define("username.case_sensitive", false);

    // Download Resources Configuration
    public static final ModConfigSpec.ConfigValue<String> DOWNLOAD_RESOURCES_JSON = BUILDER
            .comment("JSON configuration for download resources")
            .define("downloads.resources_json", "[]");

    // Registration Configuration
    public static final ModConfigSpec.BooleanValue REGISTER_AUTO_APPROVE = BUILDER
            .comment("Automatically approve new registrations")
            .define("register.auto_approve", false);

    public static final ModConfigSpec.IntValue MAX_ACCOUNTS_PER_EMAIL = BUILDER
            .comment("Maximum number of accounts per email address")
            .defineInRange("register.max_accounts_per_email", 2, 1, 10);

    // Username Configuration
    public static final ModConfigSpec.ConfigValue<String> USERNAME_REGEX = BUILDER
            .comment("Regex pattern for valid usernames")
            .define("username.regex", "^[a-zA-Z0-9_-]{3,16}$");

    // User Notification Configuration
    public static final ModConfigSpec.BooleanValue USER_NOTIFICATION_ENABLED = BUILDER
            .comment("Enable user notifications")
            .define("user_notification.enabled", true);

    public static final ModConfigSpec.BooleanValue USER_NOTIFICATION_ON_APPROVE = BUILDER
            .comment("Notify user when approved")
            .define("user_notification.on_approve", true);

    public static final ModConfigSpec.BooleanValue USER_NOTIFICATION_ON_REJECT = BUILDER
            .comment("Notify user when rejected")
            .define("user_notification.on_reject", true);

    // Frontend Configuration
    public static final ModConfigSpec.ConfigValue<String> FRONTEND_THEME = BUILDER
            .comment("Frontend theme name")
            .define("frontend.theme", "glassx");

    public static final ModConfigSpec.ConfigValue<String> FRONTEND_LOGO_URL = BUILDER
            .comment("URL to the server logo")
            .define("frontend.logo_url", "/logo.png");

    public static final ModConfigSpec.ConfigValue<String> FRONTEND_ANNOUNCEMENT = BUILDER
            .comment("Announcement message displayed on the frontend")
            .define("frontend.announcement", "Welcome to VerifyMC!");

    // Email Domain Whitelist Configuration
    public static final ModConfigSpec.BooleanValue ENABLE_EMAIL_DOMAIN_WHITELIST = BUILDER
            .comment("Enable email domain whitelist")
            .define("email.domain_whitelist.enabled", false);

    public static final ModConfigSpec.ConfigValue<String> EMAIL_DOMAIN_WHITELIST = BUILDER
            .comment("Comma-separated list of allowed email domains")
            .define("email.domain_whitelist.domains", "gmail.com,163.com,qq.com,outlook.com");

    public static final ModConfigSpec.BooleanValue ENABLE_EMAIL_ALIAS_LIMIT = BUILDER
            .comment("Enable email alias limit (prevent +alias trick)")
            .define("email.alias_limit.enabled", false);

    // Captcha Configuration
    public static final ModConfigSpec.ConfigValue<String> CAPTCHA_TYPE = BUILDER
            .comment("Captcha type (math, text)")
            .define("captcha.type", "math");

    public static final ModConfigSpec.IntValue CAPTCHA_LENGTH = BUILDER
            .comment("Captcha length (for text type)")
            .defineInRange("captcha.length", 4, 3, 8);

    public static final ModConfigSpec.IntValue CAPTCHA_EXPIRE_SECONDS = BUILDER
            .comment("Captcha expiration time in seconds")
            .defineInRange("captcha.expire_seconds", 300, 60, 1800);

    // Bedrock Configuration
    public static final ModConfigSpec.BooleanValue BEDROCK_ENABLED = BUILDER
            .comment("Enable Bedrock (Geyser) support")
            .define("bedrock.enabled", false);

    public static final ModConfigSpec.ConfigValue<String> BEDROCK_PREFIX = BUILDER
            .comment("Prefix for Bedrock usernames")
            .define("bedrock.prefix", ".");

    public static final ModConfigSpec.ConfigValue<String> BEDROCK_USERNAME_REGEX = BUILDER
            .comment("Regex pattern for valid Bedrock usernames")
            .define("bedrock.username_regex", "^[a-zA-Z0-9._-]{3,15}$");

    // LLM Additional Configuration
    public static final ModConfigSpec.IntValue LLM_INPUT_MAX_LENGTH = BUILDER
            .comment("Maximum input length for LLM")
            .defineInRange("llm.input_max_length", 2000, 500, 5000);

    public static final ModConfigSpec.ConfigValue<String> LLM_SCORING_RULE = BUILDER
            .comment("Scoring rules for LLM")
            .define("llm.scoring_rule", "Evaluate primarily: 1) Relevance to the question, 2) Completeness and level of detail, 3) Understanding of server rules");

    public static final ModConfigSpec.ConfigValue<String> LLM_SCORE_FORMAT = BUILDER
            .comment("Expected score format from LLM")
            .define("llm.score_format", "{\"score\": number, \"reason\": string, \"confidence\": number}");

    // Discord Additional Configuration
    public static final ModConfigSpec.ConfigValue<String> DISCORD_GUILD_ID = BUILDER
            .comment("Discord guild (server) ID for verification")
            .define("discord.guild_id", "");

    public static final ModConfigSpec.BooleanValue DISCORD_REQUIRED = BUILDER
            .comment("Require Discord linking for registration")
            .define("discord.required", false);

    // Auto Update Configuration
    public static final ModConfigSpec.BooleanValue AUTO_UPDATE_RESOURCES = BUILDER
            .comment("Automatically update frontend resources")
            .define("auto_update_resources", true);

    // Web Server Prefix
    public static final ModConfigSpec.ConfigValue<String> WEB_SERVER_PREFIX = BUILDER
            .comment("Prefix for web server messages")
            .define("web.server_prefix", "[VerifyMC]");

    public static final ModConfigSpec SPEC = BUILDER.build();
    
    @SubscribeEvent
    static void onLoad(final ModConfigEvent.Loading event) {
    }
}
