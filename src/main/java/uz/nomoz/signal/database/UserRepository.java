package uz.nomoz.signal.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.nomoz.signal.config.DatabaseConfig;
import uz.nomoz.signal.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class UserRepository {
    private static final Logger log = LoggerFactory.getLogger(UserRepository.class);

    public static void saveInitialUser(long chatId) {
        String sql = """
            INSERT OR IGNORE INTO users (chat_id, region, district, notifications_enabled, reminder_minutes, updated_at)
            VALUES (?, 'Toshkent shahri', 'Toshkent', 1, 0, datetime('now'));
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, chatId);
            pstmt.executeUpdate();
        } catch (Exception e) {
            log.error("Dastlabki foydalanuvchini kiritishda xatolik: chatId={}", chatId, e);
        }
    }

    public static void saveOrUpdateLocation(long chatId, String region, String district) {
        String sql = """
            INSERT INTO users (chat_id, region, district, notifications_enabled, reminder_minutes, updated_at)
            VALUES (?, ?, ?, 1, 0, datetime('now'))
            ON CONFLICT(chat_id) DO UPDATE SET
                region = excluded.region,
                district = excluded.district,
                updated_at = datetime('now');
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, chatId);
            pstmt.setString(2, region);
            pstmt.setString(3, district);
            pstmt.executeUpdate();

            log.debug("Foydalanuvchi joylashuvi saqlandi: chatId={}, region={}, district={}", chatId, region, district);
        } catch (Exception e) {
            log.error("Joylashuvni saqlashda xatolik: chatId={}", chatId, e);
        }
    }

    public static Optional<User> findByChatId(long chatId) {
        String sql = "SELECT chat_id, region, district, notifications_enabled, reminder_minutes, updated_at FROM users WHERE chat_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, chatId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setChatId(rs.getLong("chat_id"));
                    user.setRegion(rs.getString("region"));
                    user.setDistrict(rs.getString("district"));
                    user.setNotificationsEnabled(rs.getInt("notifications_enabled") == 1);
                    user.setReminderMinutes(rs.getInt("reminder_minutes"));
                    user.setUpdatedAt(rs.getTimestamp("updated_at"));
                    return Optional.of(user);
                }
            }
        } catch (Exception e) {
            log.error("Foydalanuvchini chatId bo'yicha olishda xatolik: chatId={}", chatId, e);
        }
        return Optional.empty();
    }

    public static void updateNotifications(long chatId, boolean enabled) {
        String sql = """
            INSERT INTO users (chat_id, region, district, notifications_enabled, reminder_minutes, updated_at)
            VALUES (?, 'Toshkent shahri', 'Toshkent', ?, 0, datetime('now'))
            ON CONFLICT(chat_id) DO UPDATE SET
                notifications_enabled = excluded.notifications_enabled,
                updated_at = datetime('now');
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, chatId);
            pstmt.setInt(2, enabled ? 1 : 0);
            pstmt.executeUpdate();

            log.info("Foydalanuvchi eslatma holati yangilandi: chatId={}, enabled={}", chatId, enabled);
        } catch (Exception e) {
            log.error("Eslatma holatini yangilashda xatolik: chatId={}", chatId, e);
        }
    }

    public static void updateReminderMinutes(long chatId, int minutes) {
        String sql = """
            INSERT INTO users (chat_id, region, district, notifications_enabled, reminder_minutes, updated_at)
            VALUES (?, 'Toshkent shahri', 'Toshkent', 1, ?, datetime('now'))
            ON CONFLICT(chat_id) DO UPDATE SET
                reminder_minutes = excluded.reminder_minutes,
                updated_at = datetime('now');
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, chatId);
            pstmt.setInt(2, minutes);
            pstmt.executeUpdate();

            log.info("Foydalanuvchi eslatma vaqti yangilandi: chatId={}, minutes={}", chatId, minutes);
        } catch (Exception e) {
            log.error("Eslatma vaqtini yangilashda xatolik: chatId={}", chatId, e);
        }
    }

    public static List<User> getAllActiveUsersForNotifications() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT chat_id, region, district, notifications_enabled, reminder_minutes, updated_at FROM users WHERE notifications_enabled = 1";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                User user = new User();
                user.setChatId(rs.getLong("chat_id"));
                user.setRegion(rs.getString("region"));
                user.setDistrict(rs.getString("district"));
                user.setNotificationsEnabled(true);
                user.setReminderMinutes(rs.getInt("reminder_minutes"));
                user.setUpdatedAt(rs.getTimestamp("updated_at"));
                users.add(user);
            }
        } catch (Exception e) {
            log.error("Faol foydalanuvchilarni olishda xatolik:", e);
        }
        return users;
    }

    // ========== STATISTIKA METODLARI ==========

    public static int getTotalUsersCount() {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            log.error("Jami foydalanuvchilar sonini olishda xatolik:", e);
        }
        return 0;
    }

    public static int getTodayNewUsersCount() {
        String sql = "SELECT COUNT(*) FROM users WHERE date(updated_at) = date('now')";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            log.error("Bugungi yangi foydalanuvchilar sonini olishda xatolik:", e);
        }
        return 0;
    }

    public static int getActiveNotificationsCount() {
        String sql = "SELECT COUNT(*) FROM users WHERE notifications_enabled = 1";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            log.error("Faol bildirishnomali foydalanuvchilar sonini olishda xatolik:", e);
        }
        return 0;
    }

    public static Map<String, Integer> getUsersByRegionStats() {
        Map<String, Integer> stats = new LinkedHashMap<>();
        String sql = "SELECT region, COUNT(*) as cnt FROM users GROUP BY region ORDER BY cnt DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                stats.put(rs.getString("region"), rs.getInt("cnt"));
            }
        } catch (Exception e) {
            log.error("Viloyatlar statistikasi olishda xatolik:", e);
        }
        return stats;
    }

    public static Map<Integer, Integer> getReminderMinutesDistribution() {
        Map<Integer, Integer> dist = new LinkedHashMap<>();
        String sql = "SELECT reminder_minutes, COUNT(*) as cnt FROM users GROUP BY reminder_minutes ORDER BY reminder_minutes ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                dist.put(rs.getInt("reminder_minutes"), rs.getInt("cnt"));
            }
        } catch (Exception e) {
            log.error("Eslatma daqiqalari taqsimotini olishda xatolik:", e);
        }
        return dist;
    }

    public static List<User> getRecentUsers(int limit) {
        List<User> list = new ArrayList<>();
        String sql = "SELECT chat_id, region, district, notifications_enabled, reminder_minutes, updated_at FROM users ORDER BY updated_at DESC LIMIT ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    User user = new User();
                    user.setChatId(rs.getLong("chat_id"));
                    user.setRegion(rs.getString("region"));
                    user.setDistrict(rs.getString("district"));
                    user.setNotificationsEnabled(rs.getInt("notifications_enabled") == 1);
                    user.setReminderMinutes(rs.getInt("reminder_minutes"));
                    user.setUpdatedAt(rs.getTimestamp("updated_at"));
                    list.add(user);
                }
            }
        } catch (Exception e) {
            log.error("Oxirgi foydalanuvchilarni olishda xatolik:", e);
        }
        return list;
    }

    public static List<Long> getAllChatIds() {
        List<Long> list = new ArrayList<>();
        String sql = "SELECT chat_id FROM users";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(rs.getLong("chat_id"));
            }
        } catch (Exception e) {
            log.error("Barcha chat_id larni olishda xatolik:", e);
        }
        return list;
    }
}
