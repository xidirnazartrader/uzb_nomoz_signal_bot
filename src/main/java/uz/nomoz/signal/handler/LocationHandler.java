package uz.nomoz.signal.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Location;
import org.telegram.telegrambots.meta.bots.AbsSender;
import uz.nomoz.signal.keyboard.KeyboardFactory;
import uz.nomoz.signal.model.Mosque;
import uz.nomoz.signal.service.MosqueFinderService;
import uz.nomoz.signal.service.QiblaService;

import java.util.List;

public class LocationHandler {
    private static final Logger log = LoggerFactory.getLogger(LocationHandler.class);

    public static void handle(long chatId, Location location, AbsSender sender) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();

        log.info("Lokatsiya qabul qilindi: chatId={}, lat={}, lon={}", chatId, lat, lon);

        // 1. Qibla hisoblash
        double qiblaAngle = QiblaService.calculateQiblaAngle(lat, lon);
        String cardinal = QiblaService.getCardinalDirection(qiblaAngle);
        double kaabaDistance = QiblaService.calculateDistanceToKaabaKm(lat, lon);

        // 2. Eng yaqin masjidlarni topish
        List<Mosque> nearbyMosques = MosqueFinderService.findNearbyMosques(lat, lon);

        StringBuilder sb = new StringBuilder();
        sb.append("🕋 *Qibla Yo'nalishi & Masofasi:*\n\n");
        sb.append("🧭 Qibla burchagi: *").append(qiblaAngle).append("°* (").append(cardinal).append(")\n");
        sb.append("📏 Ka'bagacha masofa: *").append(Math.round(kaabaDistance)).append(" km*\n\n");

        sb.append("📍 *Atrofdagi eng yaqin masjidlar:*\n\n");

        if (nearbyMosques.isEmpty()) {
            sb.append("_To'g'ridan-to'g'ri yaqin atrofda masjid topilmadi. Xaritadan qidirish uchun pastdagi tugmalardan foydalaning._\n\n");
        } else {
            for (int i = 0; i < nearbyMosques.size(); i++) {
                Mosque m = nearbyMosques.get(i);
                sb.append(i + 1).append(". 🕌 *").append(m.getName()).append("*\n")
                  .append("   🚶 Masofa: `").append(m.getFormattedDistance()).append("`\n\n");
            }
            sb.append("ℹ️ _Masjidga borish marshrutini ko'rish uchun pastdagi tugmani bosing:_");
        }

        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText(sb.toString());
        msg.setParseMode("Markdown");
        msg.setReplyMarkup(KeyboardFactory.getMosqueLinksKeyboard(nearbyMosques, lat, lon));

        try {
            sender.execute(msg);

            // Asosiy menyuni qayta chiqarib qo'yish
            SendMessage mainMsg = new SendMessage();
            mainMsg.setChatId(String.valueOf(chatId));
            mainMsg.setText("Asosiy bo'limlarga qaytish uchun pastdagi menyudan foydalaning 👇");
            mainMsg.setReplyMarkup(KeyboardFactory.getMainKeyboard());
            sender.execute(mainMsg);

        } catch (Exception e) {
            log.error("Lokatsiya ma'lumotlarini yuborishda xatolik: chatId={}", chatId, e);
        }
    }
}
