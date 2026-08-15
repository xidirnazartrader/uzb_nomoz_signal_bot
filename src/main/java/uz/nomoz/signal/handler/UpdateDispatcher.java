package uz.nomoz.signal.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UpdateDispatcher {
    private static final Logger log = LoggerFactory.getLogger(UpdateDispatcher.class);
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
        if (update.hasMessage()) {
            Message message = update.getMessage();
            long chatId = message.getChatId();

            if (message.hasLocation()) {
                LocationHandler.handle(chatId, message.getLocation(), sender);
            } else if (message.hasText()) {
                String text = message.getText().trim();

                // Agar Reply bo'lmasa va / bilan boshlansa CommandHandler ga
                if (text.startsWith("/") && message.getReplyToMessage() == null) {
                    CommandHandler.handle(chatId, text, sender);
                } else {
                    TextMessageHandler.handle(message, sender);
                }
            }
        } else if (update.hasCallbackQuery()) {
            CallbackHandler.handle(update.getCallbackQuery(), sender);
        }
    }

    public static void shutdown() {
        updateExecutor.shutdown();
    }
}
