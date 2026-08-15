package uz.nomoz.signal.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

public class BotConfig {
    private static final Logger log = LoggerFactory.getLogger(BotConfig.class);
    private static BotConfig instance;

    private final String botToken;
    private final String botUsername;
    private final String dbUrl;
    private final int dbPoolMaxSize;
    private final int dbPoolMinIdle;
    private final int defaultReminderMinutes;
    private final long adminId;

    private BotConfig() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("bot.properties")) {
            if (is != null) {
                props.load(is);
            } else {
                log.warn("bot.properties fayli topilmadi, standart sozlamalar yoki muhit o'zgaruvchilari ishlatiladi.");
            }
        } catch (Exception e) {
            log.error("bot.properties faylini yuklashda xatolik:", e);
        }

        // Environment variables ustuvorlikka ega
        this.botToken = getEnvOrProp("BOT_TOKEN", props.getProperty("bot.token", "8980565563:AAHG3aHU-a98jAAX2yK0FuNd_5r2CTvdIoA"));
        this.botUsername = getEnvOrProp("BOT_USERNAME", props.getProperty("bot.username", "nomoz_signal_bot"));
        this.dbUrl = getEnvOrProp("DB_URL", props.getProperty("db.url", "jdbc:sqlite:namoz_bot.db"));
        this.dbPoolMaxSize = Integer.parseInt(getEnvOrProp("DB_POOL_MAX_SIZE", props.getProperty("db.pool.max-size", "10")));
        this.dbPoolMinIdle = Integer.parseInt(getEnvOrProp("DB_POOL_MIN_IDLE", props.getProperty("db.pool.min-idle", "2")));
        this.defaultReminderMinutes = Integer.parseInt(getEnvOrProp("NOTIFICATION_MINUTES", props.getProperty("notification.default.minutes-before", "0")));
        
        String adminStr = getEnvOrProp("ADMIN_ID", props.getProperty("admin.id", "0"));
        this.adminId = parseLongSafe(adminStr);

        log.info("BotConfig muvaffaqiyatli yuklandi. Bot: @{}, Admin ID: {}", botUsername, adminId);
    }

    public static synchronized BotConfig getInstance() {
        if (instance == null) {
            instance = new BotConfig();
        }
        return instance;
    }

    private String getEnvOrProp(String envKey, String defaultValue) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue.trim();
        }
        return defaultValue;
    }

    private long parseLongSafe(String str) {
        try {
            return Long.parseLong(str.trim());
        } catch (Exception e) {
            return 0L;
        }
    }

    public String getBotToken() { return botToken; }
    public String getBotUsername() { return botUsername; }
    public String getDbUrl() { return dbUrl; }
    public int getDbPoolMaxSize() { return dbPoolMaxSize; }
    public int getDbPoolMinIdle() { return dbPoolMinIdle; }
    public int getDefaultReminderMinutes() { return defaultReminderMinutes; }
    public long getAdminId() { return adminId; }
}
