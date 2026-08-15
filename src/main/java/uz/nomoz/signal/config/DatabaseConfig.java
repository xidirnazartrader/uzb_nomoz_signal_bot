package uz.nomoz.signal.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConfig {
    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);
    private static HikariDataSource dataSource;

    public static synchronized void init() {
        if (dataSource != null && !dataSource.isClosed()) {
            return;
        }

        BotConfig config = BotConfig.getInstance();
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getDbUrl());
        hikariConfig.setDriverClassName("org.sqlite.JDBC");
        hikariConfig.setMaximumPoolSize(config.getDbPoolMaxSize());
        hikariConfig.setMinimumIdle(config.getDbPoolMinIdle());
        hikariConfig.setIdleTimeout(30000);
        hikariConfig.setMaxLifetime(60000);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setPoolName("PrayerBotHikariPool");

        // SQLite pragmalari (WAL rejimi tezlik va ko'p oqimli xavfsizlikni oshiradi)
        hikariConfig.addDataSourceProperty("journal_mode", "WAL");
        hikariConfig.addDataSourceProperty("synchronous", "NORMAL");

        dataSource = new HikariDataSource(hikariConfig);
        log.info("HikariCP SQLite Connection Pool muvaffaqiyatli ishga tushdi: {}", config.getDbUrl());
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            init();
        }
        return dataSource.getConnection();
    }

    public static synchronized void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("HikariCP Connection Pool yopildi.");
        }
    }
}
