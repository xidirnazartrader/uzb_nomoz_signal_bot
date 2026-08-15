package uz.nomoz.signal;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uz.nomoz.signal.config.DatabaseConfig;
import uz.nomoz.signal.database.DatabaseManager;
import uz.nomoz.signal.database.UserRepository;
import uz.nomoz.signal.model.PrayerTimes;
import uz.nomoz.signal.model.RegionData;
import uz.nomoz.signal.model.User;
import uz.nomoz.signal.service.PrayerTimeService;
import uz.nomoz.signal.service.TasbehService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class PrayerBotTest {

    @BeforeAll
    public static void setUp() {
        DatabaseConfig.init();
        DatabaseManager.initDatabase();
    }

    @Test
    public void testRegionData() {
        assertNotNull(RegionData.getRegions());
        assertTrue(RegionData.getRegions().size() >= 14);
        assertTrue(RegionData.getRegions().contains("Toshkent shahri"));
        assertTrue(RegionData.getRegions().contains("Samarqand"));

        String[] districts = RegionData.getDistricts("Samarqand");
        assertNotNull(districts);
        assertTrue(districts.length > 0);
    }

    @Test
    public void testDatabaseAndUserRepository() {
        long testChatId = 999888777L;
        String testRegion = "Samarqand";
        String testDistrict = "Urgut";

        UserRepository.saveOrUpdateLocation(testChatId, testRegion, testDistrict);

        Optional<User> userOpt = UserRepository.findByChatId(testChatId);
        assertTrue(userOpt.isPresent(), "Foydalanuvchi bazadan topilishi kerak");
        User user = userOpt.get();
        assertEquals(testRegion, user.getRegion());
        assertEquals(testDistrict, user.getDistrict());
        assertTrue(user.isNotificationsEnabled());

        // Sozlamalarni o'zgartirish
        UserRepository.updateReminderMinutes(testChatId, 15);
        userOpt = UserRepository.findByChatId(testChatId);
        assertTrue(userOpt.isPresent());
        assertEquals(15, userOpt.get().getReminderMinutes());

        UserRepository.updateNotifications(testChatId, false);
        userOpt = UserRepository.findByChatId(testChatId);
        assertTrue(userOpt.isPresent());
        assertFalse(userOpt.get().isNotificationsEnabled());
    }

    @Test
    public void testPrayerTimeService() {
        PrayerTimes pt = PrayerTimeService.getTodayPrayerTimes("Toshkent shahri", "Yunusobod");
        assertNotNull(pt);
        assertNotNull(pt.getFajr());
        assertNotNull(pt.getDhuhr());
        assertNotNull(pt.getAsr());
        assertNotNull(pt.getMaghrib());
        assertNotNull(pt.getIsha());

        String dailyMsg = PrayerTimeService.formatDailyMessage(pt);
        assertNotNull(dailyMsg);
        assertTrue(dailyMsg.contains("Bomdod"));
        assertTrue(dailyMsg.contains("Peshin"));

        List<PrayerTimes> monthly = PrayerTimeService.getMonthlyPrayerTimes("Toshkent shahri", "Yunusobod");
        assertNotNull(monthly);
        assertFalse(monthly.isEmpty());

        String monthlyMsg = PrayerTimeService.formatMonthlyMessage("Toshkent shahri", "Yunusobod");
        assertNotNull(monthlyMsg);
        assertTrue(monthlyMsg.length() < 4000, "Xabar uzunligi Telegram chegarasidan (4096) oshmasligi kerak");
    }

    @Test
    public void testTasbehService() {
        long testChatId = 123456789L;
        TasbehService.start(testChatId);

        assertEquals(0, TasbehService.getCount(testChatId));
        assertEquals(0, TasbehService.getStage(testChatId));

        // 32 marta bosish
        for (int i = 0; i < 32; i++) {
            boolean completed = TasbehService.increment(testChatId);
            assertFalse(completed);
        }
        assertEquals(32, TasbehService.getCount(testChatId));
        assertEquals(0, TasbehService.getStage(testChatId));

        // 33-marta bosganda keyingi bosqichga o'tishi kerak
        boolean completed = TasbehService.increment(testChatId);
        assertFalse(completed);
        assertEquals(0, TasbehService.getCount(testChatId));
        assertEquals(1, TasbehService.getStage(testChatId));
    }
}
