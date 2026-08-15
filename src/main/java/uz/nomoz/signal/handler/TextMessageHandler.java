package uz.nomoz.signal.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import uz.nomoz.signal.database.UserRepository;
import uz.nomoz.signal.keyboard.KeyboardFactory;
import uz.nomoz.signal.model.PrayerTimes;
import uz.nomoz.signal.model.User;
import uz.nomoz.signal.service.FeedbackService;
import uz.nomoz.signal.service.PrayerTimeService;
import uz.nomoz.signal.service.TasbehService;

import java.util.Optional;

public class TextMessageHandler {
    private static final Logger log = LoggerFactory.getLogger(TextMessageHandler.class);

    public static void handle(Message message, AbsSender sender) {
        long chatId = message.getChatId();
        String text = message.getText();

        if (text == null) {
            return;
        }

        text = text.trim();
        log.debug("Matnli xabar keldi: chatId={}, text={}", chatId, text);

        // 1. Admin Telegram Reply (Ответить) qilganini tekshirish
        if (message.getReplyToMessage() != null) {
            boolean handled = FeedbackService.handleAdminTelegramReply(chatId, message.getReplyToMessage(), text, sender);
            if (handled) return;
        }

        // 2. Admin tugma orqali javob yozayotgan bo'lsa
        if (FeedbackService.isAdminReplying(chatId)) {
            if ("/cancel".equalsIgnoreCase(text)) {
                FeedbackService.cancelState(chatId);
                sendTextMessage(chatId, "❌ Javob yozish bekor qilindi.", sender);
                return;
            }
            FeedbackService.handleAdminButtonReply(chatId, text, sender);
            return;
        }

        // 3. Foydalanuvchi adminga murojaat yozayotgan bo'lsa
        if (FeedbackService.isUserWaitingForFeedback(chatId)) {
            if ("/cancel".equalsIgnoreCase(text)) {
                FeedbackService.cancelState(chatId);
                sendTextMessage(chatId, "❌ Murojaat bekor qilindi.", sender);
                return;
            }
            FeedbackService.handleUserMessage(chatId, message, sender);
            return;
        }

        // 4. Asosiy Menyu tugmalari
        switch (text) {
            case "🕌 Namoz vaqtlari" -> handlePrayerTimesMenu(chatId, sender);
            case "📿 Tasbeh" -> handleTasbehMenu(chatId, sender);
            case "📍 Eng yaqin masjid & Qibla" -> handleMosqueMenu(chatId, sender);
            case "⚙️ Sozlamalar & Signallar" -> handleSettingsMenu(chatId, sender);
            case "✍️ Adminga murojaat" -> FeedbackService.startFeedback(chatId, sender);
            case "ℹ️ Ma'lumot" -> CommandHandler.handle(chatId, "/help", sender);
            case "⬅️ Asosiy menyuga qaytish" -> handleBackToMain(chatId, sender);
            default -> {
                // Agar noma'lum matn kelsa va / bilan boshlansa CommandHandler ga uzatiladi
                if (text.startsWith("/")) {
                    CommandHandler.handle(chatId, text, sender);
                } else {
                    sendTextMessage(chatId, "Iltimos, quyidagi menyudan kerakli bo'limni tanlang 👇", sender);
                }
            }
        }
    }

    private static void handlePrayerTimesMenu(long chatId, AbsSender sender) {
        Optional<User> userOpt = UserRepository.findByChatId(chatId);

        if (userOpt.isEmpty() || userOpt.get().getRegion() == null) {
            SendMessage msg = new SendMessage();
            msg.setChatId(String.valueOf(chatId));
            msg.setText("📍 Namoz vaqtlarini ko'rish uchun avval o'z viloyatingizni tanlang:");
            msg.setParseMode("Markdown");
            msg.setReplyMarkup(KeyboardFactory.getRegionsKeyboard());

            try {
                sender.execute(msg);
            } catch (Exception e) {
                log.error("Viloyat tanlash xabarini yuborishda xatolik:", e);
            }
            return;
        }

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
            log.error("Namoz vaqtini ko'rsatishda xatolik:", e);
        }
    }

    private static void handleTasbehMenu(long chatId, AbsSender sender) {
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
            log.error("Tasbeh boshlashda xatolik:", e);
        }
    }

    private static void handleMosqueMenu(long chatId, AbsSender sender) {
        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText("""
                📍 *Eng yaqin masjid va Qibla yo'nalishini topish:*
                
                Atrofdagi masjidlarni va Ka'ba tomonga yo'nalishni (burchak va masofani) aniqlash uchun quyidagi *«📍 Joylashuvimni yuborish (GPS)»* tugmasini bosing:
                """);
        msg.setParseMode("Markdown");
        msg.setReplyMarkup(KeyboardFactory.getLocationRequestKeyboard());

        try {
            sender.execute(msg);
        } catch (Exception e) {
            log.error("Lokatsiya so'rovini chiqarishda xatolik:", e);
        }
    }

    private static void handleSettingsMenu(long chatId, AbsSender sender) {
        Optional<User> userOpt = UserRepository.findByChatId(chatId);
        if (userOpt.isEmpty()) {
            UserRepository.saveInitialUser(chatId);
            userOpt = UserRepository.findByChatId(chatId);
        }

        User user = userOpt.orElseGet(() -> new User(chatId, "Toshkent shahri", "Toshkent", true, 0));

        String reminderText = (user.getReminderMinutes() == 0)
                ? "Azon vaqtida (0 daqiqa)"
                : user.getReminderMinutes() + " daqiqa oldin";

        String text = String.format(
                """
                ⚙️ *Sozlamalar & Signallar:*
                
                📍 Tanlangan hudud: *%s, %s*
                🔔 Signal holati: *%s*
                ⏰ Eslatma vaqti: *%s*
                
                _Signal vaqtini yoki hududni pastdagi tugmalar orqali sozlashingiz mumkin:_
                """,
                user.getDistrict(), user.getRegion(),
                user.isNotificationsEnabled() ? "Yoqilgan ✅" : "O'chirilgan ❌",
                reminderText
        );

        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText(text);
        msg.setParseMode("Markdown");
        msg.setReplyMarkup(KeyboardFactory.getSettingsKeyboard(user));

        try {
            sender.execute(msg);
        } catch (Exception e) {
            log.error("Sozlamalar menyusini ko'rsatishda xatolik:", e);
        }
    }

    private static void handleBackToMain(long chatId, AbsSender sender) {
        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText("Asosiy menyudasiz 👇");
        msg.setReplyMarkup(KeyboardFactory.getMainKeyboard());

        try {
            sender.execute(msg);
        } catch (Exception e) {
            log.error("Asosiy menyuga qaytishda xatolik:", e);
        }
    }

    private static void sendTextMessage(long chatId, String text, AbsSender sender) {
        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText(text);
        msg.setReplyMarkup(KeyboardFactory.getMainKeyboard());

        try {
            sender.execute(msg);
        } catch (Exception e) {
            log.error("Oddiy xabar yuborishda xatolik:", e);
        }
    }
}
