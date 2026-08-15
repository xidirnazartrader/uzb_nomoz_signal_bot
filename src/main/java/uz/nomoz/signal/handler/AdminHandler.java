package uz.nomoz.signal.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;
import uz.nomoz.signal.database.UserRepository;
import uz.nomoz.signal.model.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminHandler {
    private static final Logger log = LoggerFactory.getLogger(AdminHandler.class);

    // 1. Asosiy Admin Dashboard
    public static void showDashboard(long chatId, AbsSender sender) {
        int total = UserRepository.getTotalUsersCount();
        int todayNew = UserRepository.getTodayNewUsersCount();
        int active = UserRepository.getActiveNotificationsCount();
        int inactive = total - active;

        double activePercent = total > 0 ? (active * 100.0 / total) : 0;

        String text = String.format(
                """
                👑 *Admin Boshqaruv Paneli (Dashboard)*
                
                👥 *Foydalanuvchilar holati:*
                • Jami foydalanuvchilar: *%d ta*
                • Bugun qo'shilganlar: *+%d ta*
                • 🔔 Signali faol: *%d ta* (%.1f%%)
                • 🔕 Signali o'chiq: *%d ta*
                
                Kerakli bo'limni tanlang 👇
                """,
                total, todayNew, active, activePercent, inactive
        );

        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText(text);
        msg.setParseMode("Markdown");
        msg.setReplyMarkup(getAdminDashboardKeyboard());

        try {
            sender.execute(msg);
        } catch (Exception e) {
            log.error("Admin dashboard yuborishda xatolik: chatId={}", chatId, e);
        }
    }

    // 2. Callbacklarni qayta ishlash
    public static void handleCallback(long chatId, int messageId, String data, AbsSender sender) {
        switch (data) {
            case "ADMIN_DASHBOARD" -> editToDashboard(chatId, messageId, sender);
            case "ADMIN_REGIONS" -> showRegionStats(chatId, messageId, sender);
            case "ADMIN_REMINDERS" -> showReminderStats(chatId, messageId, sender);
            case "ADMIN_RECENT_USERS" -> showRecentUsers(chatId, messageId, sender);
            case "ADMIN_SYSTEM" -> showSystemStatus(chatId, messageId, sender);
            case "ADMIN_BROADCAST_HELP" -> showBroadcastHelp(chatId, messageId, sender);
        }
    }

    private static void editToDashboard(long chatId, int messageId, AbsSender sender) {
        int total = UserRepository.getTotalUsersCount();
        int todayNew = UserRepository.getTodayNewUsersCount();
        int active = UserRepository.getActiveNotificationsCount();
        int inactive = total - active;
        double activePercent = total > 0 ? (active * 100.0 / total) : 0;

        String text = String.format(
                """
                👑 *Admin Boshqaruv Paneli (Dashboard)*
                
                👥 *Foydalanuvchilar holati:*
                • Jami foydalanuvchilar: *%d ta*
                • Bugun qo'shilganlar: *+%d ta*
                • 🔔 Signali faol: *%d ta* (%.1f%%)
                • 🔕 Signali o'chiq: *%d ta*
                
                Kerakli bo'limni tanlang 👇
                """,
                total, todayNew, active, activePercent, inactive
        );

        EditMessageText edit = new EditMessageText();
        edit.setChatId(String.valueOf(chatId));
        edit.setMessageId(messageId);
        edit.setText(text);
        edit.setParseMode("Markdown");
        edit.setReplyMarkup(getAdminDashboardKeyboard());

        try {
            sender.execute(edit);
        } catch (Exception e) {
            log.error("Dashboard tahrirlashda xatolik:", e);
        }
    }

    private static void showRegionStats(long chatId, int messageId, AbsSender sender) {
        int total = UserRepository.getTotalUsersCount();
        Map<String, Integer> regionStats = UserRepository.getUsersByRegionStats();

        StringBuilder sb = new StringBuilder();
        sb.append("📍 *Viloyatlar Kesimidagi Statistika:*\n\n");

        if (regionStats.isEmpty()) {
            sb.append("_Hozircha ma'lumot mavjud emas._\n");
        } else {
            for (Map.Entry<String, Integer> entry : regionStats.entrySet()) {
                double percent = total > 0 ? (entry.getValue() * 100.0 / total) : 0;
                sb.append(String.format("• *%s*: %d ta _(%.1f%%)_\n", entry.getKey(), entry.getValue(), percent));
            }
        }

        EditMessageText edit = new EditMessageText();
        edit.setChatId(String.valueOf(chatId));
        edit.setMessageId(messageId);
        edit.setText(sb.toString());
        edit.setParseMode("Markdown");
        edit.setReplyMarkup(getBackToAdminKeyboard());

        try {
            sender.execute(edit);
        } catch (Exception e) {
            log.error("Viloyat statistikasida xatolik:", e);
        }
    }

    private static void showReminderStats(long chatId, int messageId, AbsSender sender) {
        Map<Integer, Integer> dist = UserRepository.getReminderMinutesDistribution();
        int total = UserRepository.getTotalUsersCount();

        StringBuilder sb = new StringBuilder();
        sb.append("⏰ *Eslatma Vaqtlari Taqsimoti:*\n\n");

        if (dist.isEmpty()) {
            sb.append("_Ma'lumot topilmadi._\n");
        } else {
            for (Map.Entry<Integer, Integer> entry : dist.entrySet()) {
                int min = entry.getKey();
                int cnt = entry.getValue();
                double pct = total > 0 ? (cnt * 100.0 / total) : 0;

                String label = (min == 0) ? "Azon vaqtida (0 daq)" : min + " daqiqa oldin";
                sb.append(String.format("• *%s*: %d ta _(%.1f%%)_\n", label, cnt, pct));
            }
        }

        EditMessageText edit = new EditMessageText();
        edit.setChatId(String.valueOf(chatId));
        edit.setMessageId(messageId);
        edit.setText(sb.toString());
        edit.setParseMode("Markdown");
        edit.setReplyMarkup(getBackToAdminKeyboard());

        try {
            sender.execute(edit);
        } catch (Exception e) {
            log.error("Eslatma statistikasida xatolik:", e);
        }
    }

    private static void showRecentUsers(long chatId, int messageId, AbsSender sender) {
        List<User> recent = UserRepository.getRecentUsers(10);
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM HH:mm");

        StringBuilder sb = new StringBuilder();
        sb.append("👥 *Oxirgi 10 ta Foydalanuvchi:*\n\n");

        if (recent.isEmpty()) {
            sb.append("_Foydalanuvchilar topilmadi._\n");
        } else {
            for (int i = 0; i < recent.size(); i++) {
                User u = recent.get(i);
                String dateStr = (u.getUpdatedAt() != null) ? sdf.format(u.getUpdatedAt()) : "-";
                String notif = u.isNotificationsEnabled() ? "🔔" : "🔕";

                sb.append(String.format("%d. `%d` | %s, %s | %s | _%s_\n",
                        i + 1, u.getChatId(), u.getDistrict(), u.getRegion(), notif, dateStr));
            }
        }

        EditMessageText edit = new EditMessageText();
        edit.setChatId(String.valueOf(chatId));
        edit.setMessageId(messageId);
        edit.setText(sb.toString());
        edit.setParseMode("Markdown");
        edit.setReplyMarkup(getBackToAdminKeyboard());

        try {
            sender.execute(edit);
        } catch (Exception e) {
            log.error("Oxirgi userlar xatolik:", e);
        }
    }

    private static void showSystemStatus(long chatId, int messageId, AbsSender sender) {
        Runtime rt = Runtime.getRuntime();
        long totalMem = rt.totalMemory() / (1024 * 1024);
        long freeMem = rt.freeMemory() / (1024 * 1024);
        long usedMem = totalMem - freeMem;
        long maxMem = rt.maxMemory() / (1024 * 1024);

        int activeThreads = Thread.activeCount();

        String text = String.format(
                """
                ⚙️ *Server & Tizim Holati:*
                
                ☕ *Java Versiyasi:* `%s` (%s)
                💻 *Operatsion Tizim:* `%s %s`
                🧵 *Faol Oqimlar (Threads):* `%d`
                
                📊 *Xotira (RAM) Holati:*
                • Ishlatilmoqda: *%d MB*
                • Bo'sh xotira: *%d MB*
                • Jami ajratilgan: *%d MB*
                • Maksimal chegara: *%d MB*
                
                🟢 *Status:* Barcha xizmatlar barqaror ishlamoqda.
                """,
                System.getProperty("java.version"), System.getProperty("java.vendor"),
                System.getProperty("os.name"), System.getProperty("os.arch"),
                activeThreads,
                usedMem, freeMem, totalMem, maxMem
        );

        EditMessageText edit = new EditMessageText();
        edit.setChatId(String.valueOf(chatId));
        edit.setMessageId(messageId);
        edit.setText(text);
        edit.setParseMode("Markdown");
        edit.setReplyMarkup(getBackToAdminKeyboard());

        try {
            sender.execute(edit);
        } catch (Exception e) {
            log.error("Tizim holatida xatolik:", e);
        }
    }

    private static void showBroadcastHelp(long chatId, int messageId, AbsSender sender) {
        int total = UserRepository.getTotalUsersCount();
        String text = String.format(
                """
                📢 *Ommaviy Xabar Tarqatish (Broadcast)*
                
                Hozirda botda *%d ta* foydalanuvchi mavjud.
                
                Barcha foydalanuvchilarga xabar yuborish uchun quyidagi buyruqdan foydalaning:
                
                👉 `/sendall Xabaringiz matni`
                
                *Masalan:*
                `/sendall Assalomu alaykum! Bugun Juma ayyomi muborak bo'lsin!`
                
                _Eslatma: Xabarlar Telegram qoidalariga mos holda navbat bilan xavfsiz tarqatiladi._
                """,
                total
        );

        EditMessageText edit = new EditMessageText();
        edit.setChatId(String.valueOf(chatId));
        edit.setMessageId(messageId);
        edit.setText(text);
        edit.setParseMode("Markdown");
        edit.setReplyMarkup(getBackToAdminKeyboard());

        try {
            sender.execute(edit);
        } catch (Exception e) {
            log.error("Broadcast yordamida xatolik:", e);
        }
    }

    // ========== ADMIN KLAVIATURALARI ==========

    private static InlineKeyboardMarkup getAdminDashboardKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder().text("📍 Viloyatlar statistikasi").callbackData("ADMIN_REGIONS").build());
        row1.add(InlineKeyboardButton.builder().text("⏰ Eslatma vaqtlari").callbackData("ADMIN_REMINDERS").build());

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(InlineKeyboardButton.builder().text("👥 Oxirgi foydalanuvchilar").callbackData("ADMIN_RECENT_USERS").build());
        row2.add(InlineKeyboardButton.builder().text("⚙️ Server & RAM holati").callbackData("ADMIN_SYSTEM").build());

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(InlineKeyboardButton.builder().text("📢 Xabar tarqatish (/sendall)").callbackData("ADMIN_BROADCAST_HELP").build());

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);

        markup.setKeyboard(rows);
        return markup;
    }

    private static InlineKeyboardMarkup getBackToAdminKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(InlineKeyboardButton.builder().text("⬅️ Admin Dashboard'ga qaytish").callbackData("ADMIN_DASHBOARD").build());
        rows.add(row);

        markup.setKeyboard(rows);
        return markup;
    }
}
