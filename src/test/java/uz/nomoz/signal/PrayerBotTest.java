package uz.nomoz.signal;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uz.nomoz.signal.config.DatabaseConfig;
import uz.nomoz.signal.database.DatabaseManager;
import uz.nomoz.signal.database.UserRepository;
import uz.nomoz.signal.model.PrayerTimes;
import uz.nomoz.signal.model.RegionData;
import uz.nomoz.signal.model.User;
import uz.nomoz.signal.service.MosqueFinderService;
import uz.nomoz.signal.service.PrayerTimeService;
import uz.nomoz.signal.service.QiblaService;
import uz.nomoz.signal.service.TasbehService;

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
        long testChatId = System.currentTimeMillis();
        String testRegion = "Samarqand";
        String testDistrict = "Urgut";

        UserRepository.saveOrUpdateLocation(testChatId, testRegion, testDistrict);

        Optional<User> userOpt = UserRepository.findByChatId(testChatId);
        assertTrue(userOpt.isPresent(), "Foydalanuvchi bazadan topilishi kerak");
        User user = userOpt.get();
        assertEquals(testRegion, user.getRegion());
        assertEquals(testDistrict, user.getDistrict());
        assertTrue(user.isNotificationsEnabled());

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
    public void testQiblaService() {
        double angle = QiblaService.calculateQiblaAngle(41.2995, 69.2401);
        assertTrue(angle > 220 && angle < 260, "Toshkentdan Qibla Janubi-G'arbda (~238-245°) bo'lishi kerak");

        String cardinal = QiblaService.getCardinalDirection(angle);
        assertNotNull(cardinal);
        assertTrue(cardinal.contains("Janubi-G'arb") || cardinal.contains("G'arb"));

        double dist = QiblaService.calculateDistanceToKaabaKm(41.2995, 69.2401);
        assertTrue(dist > 3000 && dist < 4500, "Ka'bagacha masofa ~3600 km bo'lishi kerak");
    }

    @Test
    public void testMosqueDistanceCalculation() {
        double dist = MosqueFinderService.calculateDistanceMeters(41.2995, 69.2401, 39.6542, 66.9597);
        assertTrue(dist > 250000 && dist < 320000);
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
    }

    @Test
    public void testTasbehService() {
        long testChatId = 123456789L;
        TasbehService.start(testChatId);

        assertEquals(0, TasbehService.getCount(testChatId));
        assertEquals(0, TasbehService.getStage(testChatId));

        for (int i = 0; i < 32; i++) {
            boolean completed = TasbehService.increment(testChatId);
            assertFalse(completed);
        }
        assertEquals(32, TasbehService.getCount(testChatId));

        boolean completed = TasbehService.increment(testChatId);
        assertFalse(completed);
        assertEquals(0, TasbehService.getCount(testChatId));
        assertEquals(1, TasbehService.getStage(testChatId));
    }
}
