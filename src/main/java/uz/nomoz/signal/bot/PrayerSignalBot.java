package uz.nomoz.signal.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.nomoz.signal.config.BotConfig;
import uz.nomoz.signal.handler.UpdateDispatcher;

public class PrayerSignalBot extends TelegramLongPollingBot {
    private static final Logger log = LoggerFactory.getLogger(PrayerSignalBot.class);
    private final BotConfig config;

    public PrayerSignalBot() {
        this.config = BotConfig.getInstance();
    }

    @Override
    public String getBotUsername() {
        return config.getBotUsername();
    }

    @Override
    public String getBotToken() {
        return config.getBotToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        UpdateDispatcher.dispatch(update, this);
    }
}
