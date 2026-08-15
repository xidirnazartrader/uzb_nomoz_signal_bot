package uz.nomoz.signal;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import uz.nomoz.signal.bot.PrayerSignalBot;
import uz.nomoz.signal.config.BotConfig;
import uz.nomoz.signal.config.DatabaseConfig;
import uz.nomoz.signal.database.DatabaseManager;
import uz.nomoz.signal.handler.UpdateDispatcher;
import uz.nomoz.signal.service.NotificationScheduler;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static HttpServer healthServer;

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

            // 4. Telegram menyu buyruqlarini (Bot Commands) o'rnatish
            registerBotCommands(bot);

            // 5. Namoz vaqtlari avtomatik signal (eslatma) xizmatini ishga tushirish
            NotificationScheduler.start(bot);

            // 6. Render.com Web Service Health Check serverini ishga tushirish
            startHealthCheckServer();

            // 7. JVM to'xtaganda resurslarni xavfsiz yopish (Graceful Shutdown)
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Bot to'xtatilmoqda, resurslar tozalanmoqda...");
                if (healthServer != null) {
                    healthServer.stop(0);
                }
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

    private static void registerBotCommands(PrayerSignalBot bot) {
        try {
            List<BotCommand> commands = new ArrayList<>();
            commands.add(new BotCommand("/start", "Botni ishga tushirish"));
            commands.add(new BotCommand("/times", "Namoz vaqtlari taqvimi"));
            commands.add(new BotCommand("/mosque", "Eng yaqin masjid & Qibla"));
            commands.add(new BotCommand("/tasbeh", "Elektron tasbeh"));
            commands.add(new BotCommand("/settings", "Sozlamalar & Signallar"));
            commands.add(new BotCommand("/feedback", "Adminga murojaat"));
            commands.add(new BotCommand("/admin", "Foydalanuvchilar statistikasi"));
            commands.add(new BotCommand("/help", "Ma'lumot va yordam"));

            SetMyCommands setMyCommands = new SetMyCommands(commands, new BotCommandScopeDefault(), null);
            bot.execute(setMyCommands);
            log.info("Telegram Bot komandalar menyusi muvaffaqiyatli o'rnatildi.");
        } catch (Exception e) {
            log.warn("Telegram komandalar menyusini o'rnatishda ogohlantirish:", e);
        }
    }

    private static void startHealthCheckServer() {
        String portEnv = System.getenv("PORT");
        int port = (portEnv != null && !portEnv.isBlank()) ? Integer.parseInt(portEnv) : 8080;

        try {
            healthServer = HttpServer.create(new InetSocketAddress(port), 0);
            healthServer.createContext("/", exchange -> {
                String response = "🕌 Uzb Namoz Signal Bot is healthy and running!";
                byte[] bytes = response.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            });
            healthServer.start();
            log.info("🌐 Health-check HTTP server ishga tushdi: port={}", port);
        } catch (Exception e) {
            log.warn("Health-check serverini ochishda ogohlantirish:", e);
        }
    }
}
