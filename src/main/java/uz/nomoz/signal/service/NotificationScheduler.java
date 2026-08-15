package uz.nomoz.signal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.bots.AbsSender;
import uz.nomoz.signal.database.UserRepository;
import uz.nomoz.signal.model.PrayerTimes;
import uz.nomoz.signal.model.User;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public class NotificationScheduler {
    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final ZoneId TASHKENT_ZONE = ZoneId.of("Asia/Tashkent");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // Kunlik yuborilgan signallarni eslab qolish (chatId_prayerName_date)
    private static final Set<String> sentSignals = ConcurrentHashMap.newKeySet();
    private static String lastCleanDate = "";

    public static void start(AbsSender botSender) {
        log.info("Namoz vaqtlari avtomatik Signal (Notification) xizmati ishga tushirildi...");

        // Har 25 soniyada bir marta tekshiradi
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkAndSendSignals(botSender);
            } catch (Exception e) {
                log.error("Signal yuborish tekshiruvida kutilmagan xatolik:", e);
            }
        }, 5, 25, TimeUnit.SECONDS);
    }

    private static void checkAndSendSignals(AbsSender botSender) {
        LocalTime now = LocalTime.now(TASHKENT_ZONE);
        String currentTimeStr = now.format(TIME_FORMATTER);
        String todayDate = java.time.LocalDate.now(TASHKENT_ZONE).toString();

        if (!todayDate.equals(lastCleanDate)) {
            sentSignals.clear();
            lastCleanDate = todayDate;
            log.info("Signal yuborilganlar ro'yxati yangi kunda tozalandi: {}", todayDate);
        }

        List<User> activeUsers = UserRepository.getAllActiveUsersForNotifications();
        if (activeUsers.isEmpty()) {
            return;
        }

        for (User user : activeUsers) {
            try {
                processUserSignal(botSender, user, now, currentTimeStr, todayDate);
            } catch (Exception e) {
                log.error("Foydalanuvchiga signal yuborishda xatolik: chatId={}", user.getChatId(), e);
            }
        }
    }

    private static void processUserSignal(AbsSender botSender, User user, LocalTime now, String currentTimeStr, String todayDate) {
        PrayerTimes pt = PrayerTimeService.getTodayPrayerTimes(user.getRegion(), user.getDistrict());
        if (pt == null) return;

        Map<String, String> prayerMap = Map.of(
                "Bomdod", pt.getFajr(),
                "Quyosh", pt.getSunrise(),
                "Peshin", pt.getDhuhr(),
                "Asr", pt.getAsr(),
                "Shom", pt.getMaghrib(),
                "Hufton", pt.getIsha()
        );

        int minutesBefore = user.getReminderMinutes(); // 0, 5, 10, 15, 30

        for (Map.Entry<String, String> entry : prayerMap.entrySet()) {
            String prayerName = entry.getKey();
            String prayerTimeStr = entry.getValue();

            if (prayerTimeStr == null || !prayerTimeStr.matches("\\d{2}:\\d{2}")) {
                continue;
            }

            LocalTime prayerTime = LocalTime.parse(prayerTimeStr, TIME_FORMATTER);
            LocalTime triggerTime = prayerTime.minusMinutes(minutesBefore);
            String triggerTimeStr = triggerTime.format(TIME_FORMATTER);

            if (currentTimeStr.equals(triggerTimeStr)) {
                String signalKey = user.getChatId() + "_" + prayerName + "_" + todayDate;

                if (!sentSignals.contains(signalKey)) {
                    sentSignals.add(signalKey);
                    sendNotificationMessage(botSender, user, prayerName, prayerTimeStr, minutesBefore);
                }
            }
        }
    }

    private static void sendNotificationMessage(AbsSender botSender, User user, String prayerName, String prayerTimeStr, int minutesBefore) {
        String msgText;
        if (minutesBefore > 0) {
            msgText = String.format(
                    """
                    🔔 *Namoz vaqti yaqinlashmoqda!*
                    
                    📍 Joylashuv: *%s, %s*
                    ⏳ *%s* namoziga *%d daqiqa* qoldi (Vaqti: *%s*).
                    
                    _Tahorat olish va namozga hozirlik ko'rishni unutmang!_
                    """,
                    user.getDistrict(), user.getRegion(), prayerName, minutesBefore, prayerTimeStr
            );
        } else {
            msgText = String.format(
                    """
                    🕌 *Namoz vaqti kirdi!*
                    
                    📍 Joylashuv: *%s, %s*
                    ✨ *%s* namozi vaqti bo'ldi (*%s*).
                    
                    📖 _«Albatta, namoz mo'minlarga vaqtida farz qilingandir.» (Niso surasi, 103-oyat)_
                    """,
                    user.getDistrict(), user.getRegion(), prayerName, prayerTimeStr
            );
        }

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(user.getChatId()));
        message.setText(msgText);
        message.setParseMode("Markdown");

        try {
            botSender.execute(message);
            log.info("Signal yuborildi: chatId={}, prayer={}, time={}", user.getChatId(), prayerName, prayerTimeStr);
        } catch (Exception e) {
            log.error("Telegramga signal yuborishda xatolik: chatId={}", user.getChatId(), e);
        }
    }

    public static void stop() {
        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            log.info("NotificationScheduler to'xtatildi.");
        } catch (Exception e) {
            scheduler.shutdownNow();
        }
    }
}
