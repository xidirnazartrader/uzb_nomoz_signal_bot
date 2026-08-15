package uz.nomoz.signal.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UpdateDispatcher {
    private static final Logger log = LoggerFactory.getLogger(UpdateDispatcher.class);
    // Ko'p foydalanuvchilar bilan ishlash uchun 10 ta oqimli thread pool
    private static final ExecutorService updateExecutor = Executors.newFixedThreadPool(10);

    public static void dispatch(Update update, AbsSender sender) {
        updateExecutor.submit(() -> {
            try {
                processUpdate(update, sender);
            } catch (Exception e) {
                log.error("Update qayta ishlashda xatolik:", e);
            }
        });
    }

    private static void processUpdate(Update update, AbsSender sender) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText().trim();
            long chatId = update.getMessage().getChatId();

            if (text.startsWith("/")) {
                CommandHandler.handle(chatId, text, sender);
            } else {
                TextMessageHandler.handle(chatId, text, sender);
            }
        } else if (update.hasCallbackQuery()) {
            CallbackHandler.handle(update.getCallbackQuery(), sender);
        }
    }

    public static void shutdown() {
        updateExecutor.shutdown();
    }
}
