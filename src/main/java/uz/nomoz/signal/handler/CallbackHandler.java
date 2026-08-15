package uz.nomoz.signal.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;
import uz.nomoz.signal.database.UserRepository;
import uz.nomoz.signal.keyboard.KeyboardFactory;
import uz.nomoz.signal.model.PrayerTimes;
import uz.nomoz.signal.model.User;
import uz.nomoz.signal.service.PrayerTimeService;
import uz.nomoz.signal.service.TasbehService;

import java.util.Optional;

public class CallbackHandler {
    private static final Logger log = LoggerFactory.getLogger(CallbackHandler.class);

    public static void handle(CallbackQuery callbackQuery, AbsSender sender) {
        String data = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();
        int messageId = callbackQuery.getMessage().getMessageId();

        log.debug("Callback qabul qilindi: chatId={}, data={}", chatId, data);

        if (data.startsWith("REGION_")) {
            handleRegionSelected(chatId, messageId, data.replace("REGION_", ""), sender);
        } else if (data.startsWith("DISTRICT_")) {
            handleDistrictSelected(chatId, messageId, data.replace("DISTRICT_", ""), sender);
        } else if (data.equals("BACK_TO_REGIONS")) {
            handleBackToRegions(chatId, messageId, sender);
        } else if (data.startsWith("PRAYER_")) {
            handlePrayerPeriod(chatId, messageId, data.replace("PRAYER_", ""), sender);
        } else if (data.equals("TASBEH_CLICK")) {
            handleTasbehClick(chatId, messageId, sender);
        } else if (data.equals("TASBEH_RESET")) {
            handleTasbehReset(chatId, messageId, sender);
        } else if (data.startsWith("SETTING_")) {
            handleSettingChange(chatId, messageId, data, sender);
        } else if (data.startsWith("QAZA_")) {
            QazaHandler.handleCallback(chatId, messageId, data, sender);
        }
    }

    private static void handleRegionSelected(long chatId, int messageId, String region, AbsSender sender) {
        EditMessageText edit = new EditMessageText();
        edit.setChatId(String.valueOf(chatId));
        edit.setMessageId(messageId);
        edit.setText("📍 *" + region + "* tanlandi.\n\nEndi tumaningizni tanlang:");
        edit.setParseMode("Markdown");
        edit.setReplyMarkup(KeyboardFactory.getDistrictsKeyboard(region));

        try {
            sender.execute(edit);
        } catch (Exception e) {
            log.error("Tumanlar ro'yxatini chiqarishda xatolik:", e);
        }
    }

    private static void handleDistrictSelected(long chatId, int messageId, String rawData, AbsSender sender) {
        String[] parts = rawData.split("::");
        String district = parts[0];
        String region = parts.length > 1 ? parts[1] : "Toshkent shahri";

        // BAZAGA SAQLASH
        UserRepository.saveOrUpdateLocation(chatId, region, district);

        PrayerTimes pt = PrayerTimeService.getTodayPrayerTimes(region, district);
        String msgText = "✅ *Hududingiz muvaffaqiyatli saqlandi!*\n\n" + PrayerTimeService.formatDailyMessage(pt);

        EditMessageText edit = new EditMessageText();
        edit.setChatId(String.valueOf(chatId));
        edit.setMessageId(messageId);
        edit.setText(msgText);
        edit.setParseMode("Markdown");
        edit.setReplyMarkup(KeyboardFactory.getPrayerPeriodKeyboard(district, region));

        try {
            sender.execute(edit);
        } catch (Exception e) {
            log.error("Tuman tanlangach namoz vaqtini ko'rsatishda xatolik:", e);
        }
    }

    private static void handleBackToRegions(long chatId, int messageId, AbsSender sender) {
        EditMessageText edit = new EditMessageText();
        edit.setChatId(String.valueOf(chatId));
        edit.setMessageId(messageId);
        edit.setText("📍 *Viloyatingizni tanlang:*");
        edit.setParseMode("Markdown");
        edit.setReplyMarkup(KeyboardFactory.getRegionsKeyboard());

        try {
            sender.execute(edit);
        } catch (Exception e) {
            log.error("Viloyatlarga qaytishda xatolik:", e);
        }
    }

    private static void handlePrayerPeriod(long chatId, int messageId, String period, AbsSender sender) {
        Optional<User> userOpt = UserRepository.findByChatId(chatId);
        String region = userOpt.map(User::getRegion).orElse("Toshkent shahri");
        String district = userOpt.map(User::getDistrict).orElse("Toshkent");

        String text;
        if ("DAILY".equals(period)) {
            PrayerTimes pt = PrayerTimeService.getTodayPrayerTimes(region, district);
            text = PrayerTimeService.formatDailyMessage(pt);
        } else if ("WEEKLY".equals(period)) {
            text = PrayerTimeService.formatWeeklyMessage(region, district);
        } else if ("MONTHLY".equals(period)) {
            text = PrayerTimeService.formatMonthlyMessage(region, district);
        } else {
            text = "Noto'g'ri tanlov.";
        }

        EditMessageText edit = new EditMessageText();
        edit.setChatId(String.valueOf(chatId));
        edit.setMessageId(messageId);
        edit.setText(text);
        edit.setParseMode("Markdown");
        edit.setReplyMarkup(KeyboardFactory.getPrayerPeriodKeyboard(district, region));

        try {
            sender.execute(edit);
        } catch (Exception e) {
            log.error("Davriy namoz vaqtini yangilashda xatolik:", e);
        }
    }

    private static void handleTasbehClick(long chatId, int messageId, AbsSender sender) {
        boolean completed = TasbehService.increment(chatId);

        EditMessageText edit = new EditMessageText();
        edit.setChatId(String.valueOf(chatId));
        edit.setMessageId(messageId);
        edit.setParseMode("Markdown");

        if (completed) {
            edit.setText(TasbehService.getCompletedText());
            edit.setReplyMarkup(KeyboardFactory.getTasbehCompletedKeyboard());
        } else {
            edit.setText(TasbehService.getTasbehText(chatId));
            edit.setReplyMarkup(KeyboardFactory.getTasbehKeyboard(TasbehService.getCount(chatId)));
        }

        try {
            sender.execute(edit);
        } catch (Exception e) {
            log.error("Tasbeh click xatolik:", e);
        }
    }

    private static void handleTasbehReset(long chatId, int messageId, AbsSender sender) {
        TasbehService.start(chatId);

        EditMessageText edit = new EditMessageText();
        edit.setChatId(String.valueOf(chatId));
        edit.setMessageId(messageId);
        edit.setText(TasbehService.getTasbehText(chatId));
        edit.setParseMode("Markdown");
        edit.setReplyMarkup(KeyboardFactory.getTasbehKeyboard(0));

        try {
            sender.execute(edit);
        } catch (Exception e) {
            log.error("Tasbeh reset xatolik:", e);
        }
    }

    private static void handleSettingChange(long chatId, int messageId, String action, AbsSender sender) {
        Optional<User> userOpt = UserRepository.findByChatId(chatId);
        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();
        if ("SETTING_TOGGLE_NOTIF".equals(action)) {
            boolean newState = !user.isNotificationsEnabled();
            user.setNotificationsEnabled(newState);
            UserRepository.updateNotifications(chatId, newState);
        } else if ("SETTING_MINUTES_0".equals(action)) {
            user.setReminderMinutes(0);
            UserRepository.updateReminderMinutes(chatId, 0);
        } else if ("SETTING_MINUTES_15".equals(action)) {
            user.setReminderMinutes(15);
            UserRepository.updateReminderMinutes(chatId, 15);
        }

        String text = String.format(
                """
                ⚙️ *Sozlamalar yangilandi:*
                
                📍 Tanlangan hudud: *%s, %s*
                🔔 Signal holati: *%s*
                ⏰ Eslatma vaqti: *%s*
                """,
                user.getDistrict(), user.getRegion(),
                user.isNotificationsEnabled() ? "Yoqilgan ✅" : "O'chirilgan ❌",
                user.getReminderMinutes() == 0 ? "Azon vaqtida (0 daqiqa)" : user.getReminderMinutes() + " daqiqa oldin"
        );

        EditMessageText edit = new EditMessageText();
        edit.setChatId(String.valueOf(chatId));
        edit.setMessageId(messageId);
        edit.setText(text);
        edit.setParseMode("Markdown");
        edit.setReplyMarkup(KeyboardFactory.getSettingsKeyboard(user));

        try {
            sender.execute(edit);
        } catch (Exception e) {
            log.error("Sozlamalarni yangilashda xatolik:", e);
        }
    }
}
