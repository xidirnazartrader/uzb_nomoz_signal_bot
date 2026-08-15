package uz.nomoz.signal.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.nomoz.signal.model.PrayerTimes;
import uz.nomoz.signal.model.RegionData;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PrayerTimeService {
    private static final Logger log = LoggerFactory.getLogger(PrayerTimeService.class);
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    // Kunlik va oylik kesh (Har kuni yangilanadi)
    private static final Map<String, PrayerTimes> dailyCache = new ConcurrentHashMap<>();
    private static final Map<String, List<PrayerTimes>> monthlyCache = new ConcurrentHashMap<>();
    private static String lastCachedDate = "";

    private static synchronized void checkCacheExpiry() {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        if (!today.equals(lastCachedDate)) {
            dailyCache.clear();
            monthlyCache.clear();
            lastCachedDate = today;
            log.info("Namoz vaqtlari keshi yangi kun uchun tozalandi: {}", today);
        }
    }

    public static PrayerTimes getTodayPrayerTimes(String region, String district) {
        checkCacheExpiry();
        String cacheKey = region + "_" + district;
        if (dailyCache.containsKey(cacheKey)) {
            return dailyCache.get(cacheKey);
        }

        String apiCity = RegionData.getApiCity(region);
        // method=3 (MWL), school=1 (Hanafi Asr)
        String url = String.format("https://api.aladhan.com/v1/timingsByCity?city=%s&country=Uzbekistan&method=3&school=1", apiCity);

        try {
            Request request = new Request.Builder().url(url).build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    JsonObject root = JsonParser.parseString(body).getAsJsonObject();
                    JsonObject data = root.getAsJsonObject("data");
                    JsonObject timings = data.getAsJsonObject("timings");
                    JsonObject dateObj = data.getAsJsonObject("date");

                    String readableDate = dateObj.get("readable").getAsString();
                    PrayerTimes pt = new PrayerTimes(
                            readableDate,
                            cleanTime(timings.get("Fajr").getAsString()),
                            cleanTime(timings.get("Sunrise").getAsString()),
                            cleanTime(timings.get("Dhuhr").getAsString()),
                            cleanTime(timings.get("Asr").getAsString()),
                            cleanTime(timings.get("Maghrib").getAsString()),
                            cleanTime(timings.get("Isha").getAsString()),
                            region,
                            district
                    );

                    dailyCache.put(cacheKey, pt);
                    return pt;
                }
            }
        } catch (Exception e) {
            log.error("Bugungi namoz vaqtlarini olishda xatolik: region={}, city={}", region, apiCity, e);
        }

        // Fallback default vaqtlar (Tarmoq uzilganda bot qulab tushmasligi uchun)
        return getFallbackPrayerTimes(region, district);
    }

    public static List<PrayerTimes> getMonthlyPrayerTimes(String region, String district) {
        checkCacheExpiry();
        String cacheKey = region + "_" + district;
        if (monthlyCache.containsKey(cacheKey)) {
            return monthlyCache.get(cacheKey);
        }

        String apiCity = RegionData.getApiCity(region);
        LocalDate now = LocalDate.now();
        String url = String.format("https://api.aladhan.com/v1/calendarByCity?city=%s&country=Uzbekistan&method=3&school=1&month=%d&year=%d",
                apiCity, now.getMonthValue(), now.getYear());

        List<PrayerTimes> list = new ArrayList<>();
        try {
            Request request = new Request.Builder().url(url).build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    JsonObject root = JsonParser.parseString(body).getAsJsonObject();
                    JsonArray data = root.getAsJsonArray("data");

                    for (int i = 0; i < data.size(); i++) {
                        JsonObject dayObj = data.get(i).getAsJsonObject();
                        JsonObject timings = dayObj.getAsJsonObject("timings");
                        JsonObject dateObj = dayObj.getAsJsonObject("date");

                        String readableDate = dateObj.get("readable").getAsString();
                        PrayerTimes pt = new PrayerTimes(
                                readableDate,
                                cleanTime(timings.get("Fajr").getAsString()),
                                cleanTime(timings.get("Sunrise").getAsString()),
                                cleanTime(timings.get("Dhuhr").getAsString()),
                                cleanTime(timings.get("Asr").getAsString()),
                                cleanTime(timings.get("Maghrib").getAsString()),
                                cleanTime(timings.get("Isha").getAsString()),
                                region,
                                district
                        );
                        list.add(pt);
                    }
                    if (!list.isEmpty()) {
                        monthlyCache.put(cacheKey, list);
                        return list;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Oylik namoz vaqtlarini olishda xatolik: region={}", region, e);
        }

        list.add(getTodayPrayerTimes(region, district));
        return list;
    }

    public static String formatDailyMessage(PrayerTimes pt) {
        return String.format(
                """
                📍 Joylashuv: *%s, %s*
                📅 Sana: *%s*
                
                🕌 *Bugungi namoz vaqtlari:*
                
                🌅 Bomdod:  *%s*
                ☀️ Quyosh:   *%s*
                🏙 Peshin:   *%s*
                🌇 Asr:      *%s*
                🌆 Shom:     *%s*
                🌃 Hufton:   *%s*
                
                _Eslatma: Namoz vaqtlari O'zbekiston hisob-kitob standartlari bo'yicha taqdim etiladi._
                """,
                pt.getDistrict(), pt.getRegion(),
                pt.getDate(),
                pt.getFajr(),
                pt.getSunrise(),
                pt.getDhuhr(),
                pt.getAsr(),
                pt.getMaghrib(),
                pt.getIsha()
        );
    }

    public static String formatWeeklyMessage(String region, String district) {
        List<PrayerTimes> monthly = getMonthlyPrayerTimes(region, district);
        LocalDate today = LocalDate.now();
        int dayOfMonth = today.getDayOfMonth();

        StringBuilder sb = new StringBuilder();
        sb.append("📍 *").append(district).append(" (").append(region).append(")*\n");
        sb.append("🗓 *1 Haftalik namoz vaqtlari:*\n\n");

        int count = 0;
        for (int i = dayOfMonth - 1; i < monthly.size() && count < 7; i++) {
            PrayerTimes pt = monthly.get(i);
            sb.append("📅 *").append(pt.getDate()).append("*\n")
                    .append(" Bomdod: `").append(pt.getFajr()).append("` | Peshin: `").append(pt.getDhuhr()).append("`\n")
                    .append(" Asr: `").append(pt.getAsr()).append("` | Shom: `").append(pt.getMaghrib()).append("` | Hufton: `").append(pt.getIsha()).append("`\n\n");
            count++;
        }
        return sb.toString();
    }

    public static String formatMonthlyMessage(String region, String district) {
        List<PrayerTimes> monthly = getMonthlyPrayerTimes(region, district);
        StringBuilder sb = new StringBuilder();
        sb.append("📍 *").append(district).append(" (").append(region).append(")*\n");
        sb.append("📆 *Oylik ixcham taqvim (Sana | B | P | A | Sh | H):*\n\n");
        sb.append("```\n");
        sb.append(String.format("%-11s %-5s %-5s %-5s %-5s %-5s\n", "Sana", "Bomd", "Pesh", "Asr", "Shom", "Huft"));
        sb.append("---------------------------------------\n");

        for (PrayerTimes pt : monthly) {
            String shortDate = pt.getDate().length() > 6 ? pt.getDate().substring(0, 6) : pt.getDate();
            sb.append(String.format("%-11s %-5s %-5s %-5s %-5s %-5s\n",
                    shortDate, pt.getFajr(), pt.getDhuhr(), pt.getAsr(), pt.getMaghrib(), pt.getIsha()));
        }
        sb.append("```\n");
        return sb.toString();
    }

    private static String cleanTime(String rawTime) {
        if (rawTime == null) return "00:00";
        return rawTime.split(" ")[0].trim();
    }

    private static PrayerTimes getFallbackPrayerTimes(String region, String district) {
        return new PrayerTimes(
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                "04:30", "06:00", "12:30", "16:45", "19:15", "20:45",
                region, district
        );
    }
}
