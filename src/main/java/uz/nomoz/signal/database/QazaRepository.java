package uz.nomoz.signal.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.nomoz.signal.config.DatabaseConfig;
import uz.nomoz.signal.model.QazaRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class QazaRepository {
    private static final Logger log = LoggerFactory.getLogger(QazaRepository.class);

    public static QazaRecord getOrCreate(long chatId) {
        String selectSql = "SELECT chat_id, fajr, dhuhr, asr, maghrib, isha, witr FROM qaza_prayers WHERE chat_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSql)) {

            pstmt.setLong(1, chatId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new QazaRecord(
                            rs.getLong("chat_id"),
                            rs.getInt("fajr"),
                            rs.getInt("dhuhr"),
                            rs.getInt("asr"),
                            rs.getInt("maghrib"),
                            rs.getInt("isha"),
                            rs.getInt("witr")
                    );
                }
            }
        } catch (Exception e) {
            log.error("Qaza yozuvini olishda xatolik: chatId={}", chatId, e);
        }

        // Agar mavjud bo'lmasa, yangi yozuv ochamiz
        String insertSql = "INSERT OR IGNORE INTO qaza_prayers (chat_id, fajr, dhuhr, asr, maghrib, isha, witr, updated_at) VALUES (?, 0, 0, 0, 0, 0, 0, datetime('now'))";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            pstmt.setLong(1, chatId);
            pstmt.executeUpdate();
        } catch (Exception e) {
            log.error("Qaza yozuvini yaratishda xatolik: chatId={}", chatId, e);
        }

        return new QazaRecord(chatId, 0, 0, 0, 0, 0, 0);
    }

    public static void changeCount(long chatId, String prayerType, int delta) {
        // prayerType tekshirish (SQL Injectiondan himoya)
        if (!prayerType.matches("^(fajr|dhuhr|asr|maghrib|isha|witr)$")) {
            return;
        }

        // Qazolar soni noldan kamayib ketmasligi kerak (MAX(0, count + delta))
        String sql = String.format("""
            INSERT INTO qaza_prayers (chat_id, %s, updated_at)
            VALUES (?, MAX(0, ?), datetime('now'))
            ON CONFLICT(chat_id) DO UPDATE SET
                %s = MAX(0, %s + ?),
                updated_at = datetime('now');
            """, prayerType, prayerType, prayerType);

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, chatId);
            pstmt.setInt(2, delta);
            pstmt.setInt(3, delta);
            pstmt.executeUpdate();

            log.debug("Qaza yangilandi: chatId={}, prayer={}, delta={}", chatId, prayerType, delta);
        } catch (Exception e) {
            log.error("Qaza yangilashda xatolik: chatId={}, prayer={}", chatId, prayerType, e);
        }
    }
}
