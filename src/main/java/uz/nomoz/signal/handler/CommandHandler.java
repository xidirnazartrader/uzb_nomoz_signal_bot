package uz.nomoz.signal.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.bots.AbsSender;
import uz.nomoz.signal.database.UserRepository;
import uz.nomoz.signal.keyboard.KeyboardFactory;
import uz.nomoz.signal.model.User;

import java.util.Optional;

public class CommandHandler {
    private static final Logger log = LoggerFactory.getLogger(CommandHandler.class);

    public static void handle(long chatId, String command, AbsSender sender) {
        log.debug("Command qabul qilindi: chatId={}, command={}", chatId, command);

        switch (command) {
            case "/start" -> handleStart(chatId, sender);
            case "/help" -> handleHelp(chatId, sender);
            case "/settings" -> handleSettings(chatId, sender);
            default -> handleUnknown(chatId, sender);
        }
    }

    private static void handleStart(long chatId, AbsSender sender) {
        Optional<User> userOpt = UserRepository.findByChatId(chatId);

        String welcomeText = """
                Assalomu alaykum va rahmatullohi va barakatuh! 🌙
                
                *Uzb Namoz Signal Bot*iga xush kelibsiz!
                
                Ushbu bot orqali siz:
                🕌 O'zbekiston hududlari bo'yicha aniq namoz vaqtlarini ko'rishingiz;
                🔔 Har bir namoz vaqti kirganda avtomatik signal (eslatma) olishingiz;
                📿 Elektron tasbehdan foydalanishingiz mumkin.
                
                Quyidagi tugmalardan kerakli bo'limni tanlang:
                """;

        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText(welcomeText);
        msg.setParseMode("Markdown");
        msg.setReplyMarkup(KeyboardFactory.getMainKeyboard());

        try {
            sender.execute(msg);
        } catch (Exception e) {
            log.error("/start xabarini yuborishda xatolik: chatId={}", chatId, e);
        }

        // Agar foydalanuvchi yangi bo'lsa, viloyat tanlashni taklif qilamiz
        if (userOpt.isEmpty()) {
            SendMessage regionMsg = new SendMessage();
            regionMsg.setChatId(String.valueOf(chatId));
            regionMsg.setText("📍 *Iltimos, avval o'z viloyatingizni tanlang:*");
            regionMsg.setParseMode("Markdown");
            regionMsg.setReplyMarkup(KeyboardFactory.getRegionsKeyboard());

            try {
                sender.execute(regionMsg);
            } catch (Exception e) {
                log.error("Viloyat tanlash xabarini yuborishda xatolik: chatId={}", chatId, e);
            }
        }
    }

    private static void handleHelp(long chatId, AbsSender sender) {
        String helpText = """
                📖 *Botdan foydalanish bo'yicha qo'llanma:*
                
                🕌 *Namoz vaqtlari* - Viloyat va tumaningiz bo'yicha bugungi, haftalik va oylik taqvim.
                🔔 *Signallar/Eslatmalar* - Namoz vaqti kirganda yoki 15 daqiqa oldin avtomatik eslatma beradi.
                📿 *Tasbeh* - Har bir namozdan keyingi 33 talik zikrlarni qulay sanash uchun elektron tasbeh.
                ⚙️ *Sozlamalar* - Hududingizni o'zgartirish va signallarni yoqish/o'chirish.
                
                Savol va takliflar uchun administratorga murojaat qilishingiz mumkin.
                """;

        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText(helpText);
        msg.setParseMode("Markdown");

        try {
            sender.execute(msg);
        } catch (Exception e) {
            log.error("/help xabarini yuborishda xatolik: chatId={}", chatId, e);
        }
    }

    private static void handleSettings(long chatId, AbsSender sender) {
        User user = UserRepository.findByChatId(chatId).orElse(null);
        String text;
        if (user != null) {
            text = String.format(
                    """
                    ⚙️ *Sozlamalar & Signallar:*
                    
                    📍 Tanlangan hudud: *%s, %s*
                    🔔 Signal holati: *%s*
                    ⏰ Eslatma vaqti: *%s*
                    
                    Quyidagi tugmalar orqali sozlamalarni o'zgartirishingiz mumkin:
                    """,
                    user.getDistrict(), user.getRegion(),
                    user.isNotificationsEnabled() ? "Yoqilgan ✅" : "O'chirilgan ❌",
                    user.getReminderMinutes() == 0 ? "Azon vaqtida (0 daqiqa)" : user.getReminderMinutes() + " daqiqa oldin"
            );
        } else {
            text = "⚙️ Siz hali hududingizni tanlamagansiz. Iltimos, hududingizni tanlang:";
        }

        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText(text);
        msg.setParseMode("Markdown");
        msg.setReplyMarkup(KeyboardFactory.getSettingsKeyboard(user));

        try {
            sender.execute(msg);
        } catch (Exception e) {
            log.error("/settings xabarini yuborishda xatolik: chatId={}", chatId, e);
        }
    }

    private static void handleUnknown(long chatId, AbsSender sender) {
        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText("Kechirasiz, bunday buyruq topilmadi. Pastdagi menyudan foydalaning.");
        msg.setReplyMarkup(KeyboardFactory.getMainKeyboard());

        try {
            sender.execute(msg);
        } catch (Exception e) {
            log.error("Unknown command xabarini yuborishda xatolik: chatId={}", chatId, e);
        }
    }
}
