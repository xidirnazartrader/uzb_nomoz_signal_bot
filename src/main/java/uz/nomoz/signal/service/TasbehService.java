package uz.nomoz.signal.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TasbehService {
    private static final Map<Long, Integer> tasbehCounter = new ConcurrentHashMap<>();
    private static final Map<Long, Integer> tasbehStage = new ConcurrentHashMap<>();

    public static void start(long chatId) {
        tasbehCounter.put(chatId, 0);
        tasbehStage.put(chatId, 0);
    }

    public static int getCount(long chatId) {
        return tasbehCounter.getOrDefault(chatId, 0);
    }

    public static int getStage(long chatId) {
        return tasbehStage.getOrDefault(chatId, 0);
    }

    public static boolean increment(long chatId) {
        int count = getCount(chatId) + 1;
        int stage = getStage(chatId);

        if (count >= 33) {
            count = 0;
            stage++;
        }

        tasbehCounter.put(chatId, count);
        tasbehStage.put(chatId, stage);

        // Agar stage > 2 bo'lsa (barcha 3 ta zikr tugagan)
        return stage > 2;
    }

    public static String getTasbehText(long chatId) {
        int stage = getStage(chatId);
        int count = getCount(chatId);

        String zikr = switch (stage) {
            case 0 -> "Subhanalloh (سُبْحَانَ ٱللَّٰهِ)";
            case 1 -> "Alhamdulillah (ٱلْحَمْدُ لِلَّٰهِ)";
            case 2 -> "Allohu Akbar (ٱللَّٰهُ أَكْبَرُ)";
            default -> "La ilaha illalloh";
        };

        String progress = "▓".repeat(count / 3) + "░".repeat(11 - (count / 3));

        return String.format(
                """
                📿 *Elektron Tasbeh*
                
                Aytiladigan zikr:
                ✨ *%s*
                
                Sanoq: *%d / 33*
                Progress: `[%s]`
                """,
                zikr, count, progress
        );
    }

    public static String getCompletedText() {
        return """
               ✨ *Alhamdulillah!*
               Barcha 99 ta zikrni muvaffaqiyatli yakunladingiz.
               
               _«Kim har namozdan keyin 33 marta Subhanalloh, 33 marta Alhamdulillah, 33 marta Allohu Akbar desa va yuzinchisini "Laa ilaaha illallohu..." bilan to'ldirsa, gunohlari dengiz ko'pigicha bo'lsa ham kechirilur.» (Muslim rivoyati)_
               """;
    }
}
