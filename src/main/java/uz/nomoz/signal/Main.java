package uz.nomoz.signal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import uz.nomoz.signal.bot.PrayerSignalBot;
import uz.nomoz.signal.config.BotConfig;
import uz.nomoz.signal.config.DatabaseConfig;
import uz.nomoz.signal.database.DatabaseManager;
import uz.nomoz.signal.handler.UpdateDispatcher;
import uz.nomoz.signal.service.NotificationScheduler;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("==============================================");
        log.info("   Uzb Namoz Signal Bot ishga tushmoqda...    ");
        log.info("==============================================");

        try {
            // 1. Konfiguratsiyani yuklash
            BotConfig config = BotConfig.getInstance();
            log.info("Bot sozlamalari yuklandi: @{}", config.getBotUsername());

            // 2. Ma'lumotlar bazasi va Connection Poolni ishga tushirish
            DatabaseConfig.init();
            DatabaseManager.initDatabase();

            // 3. Telegram Botni ro'yxatdan o'tkazish
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            PrayerSignalBot bot = new PrayerSignalBot();
            botsApi.registerBot(bot);
            log.info("Telegram Bot muvaffaqiyatli ulandi va tinglashni boshladi.");

            // 4. Namoz vaqtlari avtomatik signal (eslatma) xizmatini ishga tushirish
            NotificationScheduler.start(bot);

            // 5. JVM to'xtaganda resurslarni xavfsiz yopish (Graceful Shutdown)
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Bot to'xtatilmoqda, resurslar tozalanmoqda...");
                NotificationScheduler.stop();
                UpdateDispatcher.shutdown();
                DatabaseConfig.close();
                log.info("Barcha xizmatlar xavfsiz to'xtatildi. Xayr!");
            }));

            log.info("✅ Bot to'liq ishga tushdi va faol holatda!");

        } catch (Exception e) {
            log.error("Botni ishga tushirishda jiddiy xatolik yuz berdi:", e);
            System.exit(1);
        }
    }
}
