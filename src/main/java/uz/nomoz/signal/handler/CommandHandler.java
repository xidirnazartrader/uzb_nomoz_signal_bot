package uz.nomoz.signal.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.bots.AbsSender;
import uz.nomoz.signal.database.UserRepository;
import uz.nomoz.signal.keyboard.KeyboardFactory;
import uz.nomoz.signal.model.PrayerTimes;
import uz.nomoz.signal.model.User;
import uz.nomoz.signal.service.FeedbackService;
import uz.nomoz.signal.service.PrayerTimeService;
import uz.nomoz.signal.service.TasbehService;

import java.util.List;
import java.util.Optional;

public class CommandHandler {
    private static final Logger log = LoggerFactory.getLogger(CommandHandler.class);

    public static void handle(long chatId, String commandText, AbsSender sender) {
        String[] parts = commandText.trim().split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String argument = parts.length > 1 ? parts[1] : "";

        log.debug("Command qabul qilindi: chatId={}, command={}, arg={}", chatId, command, argument);

        switch (command) {
            case "/start" -> handleStart(chatId, sender);
            case "/times", "/vaqtlar" -> handleTimes(chatId, sender);
            case "/mosque", "/masjid", "/qibla" -> handleMosque(chatId, sender);
            case "/tasbeh" -> handleTasbeh(chatId, sender);
            case "/settings", "/sozlamalar" -> handleSettings(chatId, sender);
            case "/feedback", "/murojaat", "/support" -> FeedbackService.startFeedback(chatId, sender);
            case "/cancel", "/bekor" -> {
                FeedbackService.cancelState(chatId);
                sendDirectMessage(chatId, "❌ Bekor qilindi. Asosiy menyudasiz 👇", sender);
            }
            case "/help", "/yordam" -> handleHelp(chatId, sender);
            case "/admin", "/stat", "/stats", "/dashboard" -> AdminHandler.showDashboard(chatId, sender);
            case "/broadcast", "/sendall" -> handleBroadcast(chatId, argument, sender);
            default -> handleUnknown(chatId, sender);
        }
    }

    private static void handleStart(long chatId, AbsSender sender) {
        UserRepository.saveInitialUser(chatId);
        Optional<User> userOpt = UserRepository.findByChatId(chatId);

        String welcomeText = """
                Assalomu alaykum va rahmatullohi va barakatuh! 🌙
                
                *Uzb Namoz Signal Bot*iga xush kelibsiz!
                
                Ushbu bot orqali siz:
                🕌 O'zbekiston hududlari bo'yicha aniq namoz vaqtlarini bilishingiz;
                🔔 Namoz vaqtlari kirganda avtomatik signal (eslatma) olishingiz;
                📍 Eng yaqin masjidlar va Ka'ba (Qibla) yo'nalishini topishingiz;
                📿 Elektron tasbehdan foydalanishingiz;
                ✍️ Adminga savol va murojaat yo'llashingiz mumkin.
                
                Kerakli bo'limni tanlang 👇
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

        if (userOpt.isEmpty() || userOpt.get().getRegion() == null) {
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

    private static void handleTimes(long chatId, AbsSender sender) {
        User user = UserRepository.findByChatId(chatId).orElse(null);
        String region = (user != null) ? user.getRegion() : "Toshkent shahri";
        String district = (user != null) ? user.getDistrict() : "Toshkent";

        PrayerTimes pt = PrayerTimeService.getTodayPrayerTimes(region, district);
        String msgText = PrayerTimeService.formatDailyMessage(pt);

        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText(msgText);
        msg.setParseMode("Markdown");
        msg.setReplyMarkup(KeyboardFactory.getPrayerPeriodKeyboard(district, region));

        try {
            sender.execute(msg);
        } catch (Exception e) {
            log.error("Namoz vaqtlarini yuborishda xatolik: chatId={}", chatId, e);
        }
    }

    private static void handleMosque(long chatId, AbsSender sender) {
        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText("""
                📍 *Eng yaqin masjid va Qibla yo'nalishini aniqlash:*
                
                Siz turgan joydagi eng yaqin masjidlarni va Ka'ba tomonga yo'nalishni aniqlash uchun pastdagi *«📍 Joylashuvimni yuborish (GPS)»* tugmasini bosing:
                """);
        msg.setParseMode("Markdown");
        msg.setReplyMarkup(KeyboardFactory.getLocationRequestKeyboard());

        try {
            sender.execute(msg);
        } catch (Exception e) {
            log.error("Lokatsiya so'rovini yuborishda xatolik: chatId={}", chatId, e);
        }
    }

    private static void handleTasbeh(long chatId, AbsSender sender) {
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

    private static void handleHelp(long chatId, AbsSender sender) {
        String helpText = """
                📖 *Botdan foydalanish bo'yicha qo'llanma:*
                
                🕌 *Namoz vaqtlari* - Bugungi, 1 haftalik va 1 oylik namoz taqvimi.
                🔔 *Signallar/Eslatmalar* - Namoz vaqti kirganda (yoki 5, 10, 15, 30 daqiqa oldin) avtomatik eslatma.
                📍 *Eng yaqin masjid & Qibla* - GPS orqali atrofdagi masjidlarni va Ka'ba yo'nalishini topish.
                📿 *Tasbeh* - Har namozdan keyingi 33 talik zikrlarni sanash.
                ⚙️ *Sozlamalar* - Hududni va eslatma vaqtlarini sozlash.
                ✍️ *Adminga murojaat* - Savol va takliflaringizni to'g'ridan-to'g'ri adminga yuborish.
                
                *Mavjud buyruqlar:*
                /start - Botni qayta ishga tushirish
                /times - Namoz vaqtlari
                /mosque - Eng yaqin masjid & Qibla
                /tasbeh - Elektron tasbeh
                /settings - Sozlamalar & Signallar
                /feedback - Adminga murojaat
                /admin - Admin boshqaruv paneli
                /help - Yordam
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
                
                _Signal vaqtini yoki hududni pastdagi tugmalar orqali o'zgartirishingiz mumkin:_
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
            log.error("/settings xabarini yuborishda xatolik: chatId={}", chatId, e);
        }
    }

    private static void handleBroadcast(long chatId, String messageText, AbsSender sender) {
        if (messageText == null || messageText.trim().isEmpty()) {
            sendDirectMessage(chatId, "⚠️ Xabar matnini kiriting. Masalan:\n`/sendall Hurmatli foydalanuvchilar, bugun Juma ayyomi muborak bo'lsin!`", sender);
            return;
        }

        List<Long> allUsers = UserRepository.getAllChatIds();
        int sentCount = 0;
        int failCount = 0;

        for (Long targetChatId : allUsers) {
            try {
                SendMessage msg = new SendMessage();
                msg.setChatId(String.valueOf(targetChatId));
                msg.setText(messageText);
                msg.setParseMode("Markdown");
                sender.execute(msg);
                sentCount++;
                Thread.sleep(35); // Rate limiting
            } catch (Exception e) {
                failCount++;
            }
        }

        String report = String.format("📢 *Xabar tarqatish yakunlandi!*\n\n✅ Muvaffaqiyatli: *%d ta*\n❌ Yetib bormadi: *%d ta*", sentCount, failCount);
        sendDirectMessage(chatId, report, sender);
    }

    private static void sendDirectMessage(long chatId, String text, AbsSender sender) {
        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText(text);
        msg.setParseMode("Markdown");
        try {
            sender.execute(msg);
        } catch (Exception e) {
            log.error("Xabar yuborishda xatolik:", e);
        }
    }

    private static void handleUnknown(long chatId, AbsSender sender) {
        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText("Kechirasiz, bunday buyruq topilmadi. Yordam uchun /help buyrug'idan foydalaning.");
        msg.setReplyMarkup(KeyboardFactory.getMainKeyboard());

        try {
            sender.execute(msg);
        } catch (Exception e) {
            log.error("Unknown command xabarini yuborishda xatolik: chatId={}", chatId, e);
        }
    }
}
