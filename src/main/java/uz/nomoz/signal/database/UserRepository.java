package uz.nomoz.signal.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.nomoz.signal.config.DatabaseConfig;
import uz.nomoz.signal.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository {
    private static final Logger log = LoggerFactory.getLogger(UserRepository.class);

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
        String sql = "UPDATE users SET notifications_enabled = ?, updated_at = datetime('now') WHERE chat_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, enabled ? 1 : 0);
            pstmt.setLong(2, chatId);
            pstmt.executeUpdate();

            log.info("Foydalanuvchi eslatma holati yangilandi: chatId={}, enabled={}", chatId, enabled);
        } catch (Exception e) {
            log.error("Eslatma holatini yangilashda xatolik: chatId={}", chatId, e);
        }
    }

    public static void updateReminderMinutes(long chatId, int minutes) {
        String sql = "UPDATE users SET reminder_minutes = ?, updated_at = datetime('now') WHERE chat_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, minutes);
            pstmt.setLong(2, chatId);
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
}
