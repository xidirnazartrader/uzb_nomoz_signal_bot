package uz.nomoz.signal.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.bots.AbsSender;
import uz.nomoz.signal.database.UserRepository;
import uz.nomoz.signal.keyboard.KeyboardFactory;
import uz.nomoz.signal.model.PrayerTimes;
import uz.nomoz.signal.model.User;
import uz.nomoz.signal.service.PrayerTimeService;
import uz.nomoz.signal.service.TasbehService;

import java.util.Optional;

public class TextMessageHandler {
    private static final Logger log = LoggerFactory.getLogger(TextMessageHandler.class);

    public static void handle(long chatId, String text, AbsSender sender) {
        log.debug("Matnli xabar qabul qilindi: chatId={}, text={}", chatId, text);

        switch (text) {
            case "🕌 Namoz vaqtlari" -> handlePrayerTimesRequest(chatId, sender);
            case "📿 Tasbeh" -> handleTasbehRequest(chatId, sender);
            case "📍 Eng yaqin masjid & Qibla" -> handleLocationPrompt(chatId, sender);
            case "⚙️ Sozlamalar & Signallar" -> CommandHandler.handle(chatId, "/settings", sender);
            case "ℹ️ Ma'lumot" -> CommandHandler.handle(chatId, "/help", sender);
            case "⬅️ Asosiy menyuga qaytish" -> handleBackToMain(chatId, sender);
            default -> handleDefaultText(chatId, text, sender);
        }
    }

    private static void handlePrayerTimesRequest(long chatId, AbsSender sender) {
        Optional<User> userOpt = UserRepository.findByChatId(chatId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            PrayerTimes pt = PrayerTimeService.getTodayPrayerTimes(user.getRegion(), user.getDistrict());
            String msgText = PrayerTimeService.formatDailyMessage(pt);

            SendMessage msg = new SendMessage();
            msg.setChatId(String.valueOf(chatId));
            msg.setText(msgText);
            msg.setParseMode("Markdown");
            msg.setReplyMarkup(KeyboardFactory.getPrayerPeriodKeyboard(user.getDistrict(), user.getRegion()));

            try {
                sender.execute(msg);
            } catch (Exception e) {
                log.error("Namoz vaqtlarini yuborishda xatolik: chatId={}", chatId, e);
            }
        } else {
            SendMessage msg = new SendMessage();
            msg.setChatId(String.valueOf(chatId));
            msg.setText("📍 Namoz vaqtlarini ko'rish uchun avval viloyatingizni tanlang:");
            msg.setParseMode("Markdown");
            msg.setReplyMarkup(KeyboardFactory.getRegionsKeyboard());

            try {
                sender.execute(msg);
            } catch (Exception e) {
                log.error("Viloyat tanlash xabarini yuborishda xatolik: chatId={}", chatId, e);
            }
        }
    }

    private static void handleTasbehRequest(long chatId, AbsSender sender) {
        TasbehService.start(chatId);
        String text = TasbehService.getTasbehText(chatId);

        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText(text);
        msg.setParseMode("Markdown");
        msg.setReplyMarkup(KeyboardFactory.getTasbehKeyboard(0));

        try {
            sender.execute(msg);
        } catch (Exception e) {
            log.error("Tasbeh boshlash xabarini yuborishda xatolik: chatId={}", chatId, e);
        }
    }

    private static void handleLocationPrompt(long chatId, AbsSender sender) {
        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText("""
                📍 *Eng yaqin masjid va Qibla yo'nalishini aniqlash:*
                
                Siz turgan joydagi eng yaqin masjidlarni va Ka'ba tomonga yo'nalishni (burchakni) aniqlash uchun pastdagi *«📍 Joylashuvimni yuborish (GPS)»* tugmasini bosing:
                """);
        msg.setParseMode("Markdown");
        msg.setReplyMarkup(KeyboardFactory.getLocationRequestKeyboard());

        try {
            sender.execute(msg);
        } catch (Exception e) {
            log.error("Lokatsiya so'rovini yuborishda xatolik: chatId={}", chatId, e);
        }
    }

    private static void handleBackToMain(long chatId, AbsSender sender) {
        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText("Asosiy menyu:");
        msg.setReplyMarkup(KeyboardFactory.getMainKeyboard());

        try {
            sender.execute(msg);
        } catch (Exception e) {
            log.error("Asosiy menyuga qaytish xabarida xatolik: chatId={}", chatId, e);
        }
    }

    private static void handleDefaultText(long chatId, String text, AbsSender sender) {
        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText("Tushunarsiz buyruq. Iltimos, pastdagi menyudan foydalaning.");
        msg.setReplyMarkup(KeyboardFactory.getMainKeyboard());

        try {
            sender.execute(msg);
        } catch (Exception e) {
            log.error("Default xabarni yuborishda xatolik: chatId={}", chatId, e);
        }
    }
}
