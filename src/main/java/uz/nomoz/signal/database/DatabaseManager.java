package uz.nomoz.signal.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.nomoz.signal.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

public class DatabaseManager {
    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    public static void initDatabase() {
        String createTableSql = """
            CREATE TABLE IF NOT EXISTS users (
                chat_id INTEGER PRIMARY KEY,
                region TEXT NOT NULL,
                district TEXT NOT NULL,
                notifications_enabled INTEGER DEFAULT 1,
                reminder_minutes INTEGER DEFAULT 0,
                updated_at TEXT
            );
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createTableSql);

            // Migratsiya: eski baza bo'lsa yangi ustunlarni tekshirish va qo'shish
            DatabaseMetaData meta = conn.getMetaData();
            Set<String> columns = new HashSet<>();
            try (ResultSet rs = meta.getColumns(null, null, "users", null)) {
                while (rs.next()) {
                    columns.add(rs.getString("COLUMN_NAME").toLowerCase());
                }
            }

            if (!columns.contains("notifications_enabled")) {
                stmt.execute("ALTER TABLE users ADD COLUMN notifications_enabled INTEGER DEFAULT 1");
                log.info("Migratsiya: 'notifications_enabled' ustuni qo'shildi.");
            }
            if (!columns.contains("reminder_minutes")) {
                stmt.execute("ALTER TABLE users ADD COLUMN reminder_minutes INTEGER DEFAULT 0");
                log.info("Migratsiya: 'reminder_minutes' ustuni qo'shildi.");
            }
            if (!columns.contains("updated_at")) {
                stmt.execute("ALTER TABLE users ADD COLUMN updated_at TEXT");
                log.info("Migratsiya: 'updated_at' ustuni qo'shildi.");
            }

            log.info("SQLite 'users' jadvali muvaffaqiyatli tekshirildi va tayyorlandi.");

        } catch (Exception e) {
            log.error("Ma'lumotlar bazasini initsializatsiya qilishda xatolik:", e);
            throw new RuntimeException("Database init failed", e);
        }
    }
}
