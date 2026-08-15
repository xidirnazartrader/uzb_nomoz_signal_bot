package uz.nomoz.signal.keyboard;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import uz.nomoz.signal.model.RegionData;
import uz.nomoz.signal.model.User;

import java.util.ArrayList;
import java.util.List;

public class KeyboardFactory {

    // 1. Pastki Asosiy Klaviatura
    public static ReplyKeyboardMarkup getMainKeyboard() {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        markup.setSelective(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("🕌 Namoz vaqtlari"));
        row1.add(new KeyboardButton("📿 Tasbeh"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("⚙️ Sozlamalar & Signallar"));
        row2.add(new KeyboardButton("ℹ️ Ma'lumot"));

        keyboard.add(row1);
        keyboard.add(row2);

        markup.setKeyboard(keyboard);
        return markup;
    }

    // 2. Viloyatlar ro'yxati (Inline 2 ustunli)
    public static InlineKeyboardMarkup getRegionsKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        String[] regions = RegionData.getRegions().toArray(new String[0]);
        for (int i = 0; i < regions.length; i += 2) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(InlineKeyboardButton.builder()
                    .text("📍 " + regions[i])
                    .callbackData("REGION_" + regions[i])
                    .build());

            if (i + 1 < regions.length) {
                row.add(InlineKeyboardButton.builder()
                        .text("📍 " + regions[i + 1])
                        .callbackData("REGION_" + regions[i + 1])
                        .build());
            }
            rows.add(row);
        }

        markup.setKeyboard(rows);
        return markup;
    }

    // 3. Tumanlar ro'yxati (Inline 2 ustunli)
    public static InlineKeyboardMarkup getDistrictsKeyboard(String region) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        String[] districts = RegionData.getDistricts(region);
        for (int i = 0; i < districts.length; i += 2) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(InlineKeyboardButton.builder()
                    .text(districts[i])
                    .callbackData("DISTRICT_" + districts[i] + "::" + region)
                    .build());

            if (i + 1 < districts.length) {
                row.add(InlineKeyboardButton.builder()
                        .text(districts[i + 1])
                        .callbackData("DISTRICT_" + districts[i + 1] + "::" + region)
                        .build());
            }
            rows.add(row);
        }

        // Orqaga qaytish tugmasi
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        backRow.add(InlineKeyboardButton.builder()
                .text("⬅️ Viloyatlarga qaytish")
                .callbackData("BACK_TO_REGIONS")
                .build());
        rows.add(backRow);

        markup.setKeyboard(rows);
        return markup;
    }

    // 4. Namoz vaqti davriyligi (Kunlik, Haftalik, Oylik)
    public static InlineKeyboardMarkup getPrayerPeriodKeyboard(String district, String region) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("📅 1 Kunlik (Bugun)")
                .callbackData("PRAYER_DAILY")
                .build());
        row1.add(InlineKeyboardButton.builder()
                .text("🗓 1 Haftalik")
                .callbackData("PRAYER_WEEKLY")
                .build());

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(InlineKeyboardButton.builder()
                .text("📆 1 Oylik taqvim")
                .callbackData("PRAYER_MONTHLY")
                .build());

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(InlineKeyboardButton.builder()
                .text("🔄 Tumanni o'zgartirish")
                .callbackData("REGION_" + region)
                .build());

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);

        markup.setKeyboard(rows);
        return markup;
    }

    // 5. Tasbeh tugmalari
    public static InlineKeyboardMarkup getTasbehKeyboard(int count) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("📿 Bosish (" + count + " / 33)")
                .callbackData("TASBEH_CLICK")
                .build());

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(InlineKeyboardButton.builder()
                .text("🔄 Qayta boshlash")
                .callbackData("TASBEH_RESET")
                .build());

        rows.add(row1);
        rows.add(row2);

        markup.setKeyboard(rows);
        return markup;
    }

    public static InlineKeyboardMarkup getTasbehCompletedKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(InlineKeyboardButton.builder()
                .text("🔄 Yangidan boshlash")
                .callbackData("TASBEH_RESET")
                .build());
        rows.add(row);

        markup.setKeyboard(rows);
        return markup;
    }

    // 6. Sozlamalar menyusi (Signal yoqish/o'chirish, vaqtini tanlash)
    public static InlineKeyboardMarkup getSettingsKeyboard(User user) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        boolean isEnabled = user != null && user.isNotificationsEnabled();
        int minutes = user != null ? user.getReminderMinutes() : 0;

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text(isEnabled ? "🔔 Signallar: YOQILGAN ✅" : "🔕 Signallar: O'CHIRILGAN ❌")
                .callbackData("SETTING_TOGGLE_NOTIF")
                .build());

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(InlineKeyboardButton.builder()
                .text(minutes == 0 ? "⏰ Vaqti: Azon kirganda (0 daqiqa) ✅" : "⏰ Vaqti: Azon kirganda (0 daqiqa)")
                .callbackData("SETTING_MINUTES_0")
                .build());

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(InlineKeyboardButton.builder()
                .text(minutes == 15 ? "⏰ Vaqti: 15 daqiqa oldin ✅" : "⏰ Vaqti: 15 daqiqa oldin")
                .callbackData("SETTING_MINUTES_15")
                .build());

        List<InlineKeyboardButton> row4 = new ArrayList<>();
        row4.add(InlineKeyboardButton.builder()
                .text("📍 Hududni o'zgartirish")
                .callbackData("BACK_TO_REGIONS")
                .build());

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        rows.add(row4);

        markup.setKeyboard(rows);
        return markup;
    }
}
