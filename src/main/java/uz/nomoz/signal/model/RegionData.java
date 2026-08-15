package uz.nomoz.signal.model;

import java.util.*;

public class RegionData {
    private static final Map<String, String[]> DISTRICTS_MAP = new LinkedHashMap<>();
    private static final Map<String, String> REGION_API_CITY_MAP = new HashMap<>();

    static {
        // Hududlar va tumanlar ro'yxati
        DISTRICTS_MAP.put("Toshkent shahri", new String[]{"Yunusobod", "Chilonzor", "Mirobod", "Mirzo Ulug'bek", "Yashnobod", "Olmazor", "Uchtepa", "Yakkasaroy", "Sergeli", "Yangi Hayot", "Bektemir"});
        DISTRICTS_MAP.put("Toshkent viloyati", new String[]{"Toshkent", "Chirchiq", "Olmaliq", "Angren", "Yangiyo'l", "Bekobod", "Oqqurg'on", "Bo'stonliq", "Buka", "Zangiota", "Qibray", "Parkent", "Pskent", "Chinaz"});
        DISTRICTS_MAP.put("Andijon", new String[]{"Andijon", "Asaka", "Shahrixon", "Xonobod", "Oltinko'l", "Baliqchi", "Bo'ston", "Buloqboshi", "Izboskan", "Jalaquduq", "Marhamat", "Paxtaobod", "Qurg'ontepa", "Xo'jaobod"});
        DISTRICTS_MAP.put("Buxoro", new String[]{"Buxoro", "Kogon", "G'ijduvon", "Olot", "Qorako'l", "Qorovulbozor", "Peshku", "Romitan", "Jondor", "Shofirkon", "Vobkent"});
        DISTRICTS_MAP.put("Farg'ona", new String[]{"Farg'ona", "Marg'ilon", "Qo'qon", "Oltiariq", "Rishton", "Buvayda", "Beshariq", "Quva", "Quvasoy", "Uchko'prik", "Toshloq", "Yozyovon", "So'x"});
        DISTRICTS_MAP.put("Jizzax", new String[]{"Jizzax", "Arnasoy", "Baxmal", "G'allaorol", "Do'stlik", "Zomin", "Zarbdor", "Zafarobod", "Mirzacho'l", "Paxtakor", "Forish", "Sharof Rashidov"});
        DISTRICTS_MAP.put("Xorazm", new String[]{"Urganch", "Xiva", "Bog'ot", "Gurlan", "Qo'shko'pir", "Shovot", "Xonqa", "Hazorasp", "Yangiariq", "Yangibozor", "Tuproqqal'a"});
        DISTRICTS_MAP.put("Namangan", new String[]{"Namangan", "Chortoq", "Chust", "Kosonsoy", "Mingbuloq", "Norin", "Pop", "To'raqo'rg'on", "Uchqo'rg'on", "Uychi", "Yangiqo'rg'on"});
        DISTRICTS_MAP.put("Navoiy", new String[]{"Navoiy", "Zarafshon", "Karmana", "Konimex", "Qiziltepa", "Navbahor", "Nurota", "Tomdi", "Uchquduq", "Xatirchi"});
        DISTRICTS_MAP.put("Qashqadaryo", new String[]{"Qarshi", "Shahrisabz", "G'uzor", "Dehqonobod", "Kasbi", "Kitob", "Koson", "Mirishkor", "Muborak", "Nishon", "Kamashi", "Chiroqchi", "Yakkabog'"});
        DISTRICTS_MAP.put("Samarqand", new String[]{"Samarqand", "Kattaqo'rg'on", "Bulung'ur", "Jomboy", "Ishtixon", "Oqdaryo", "Pastdarg'om", "Paxtachi", "Payariq", "Toyloq", "Nurobod", "Qo'shrabot", "Urgut"});
        DISTRICTS_MAP.put("Sirdaryo", new String[]{"Guliston", "Shirin", "Yangiyer", "Boyovut", "Oqoltin", "Sardoba", "Sayxunobod", "Sirdaryo", "Xavast"});
        DISTRICTS_MAP.put("Surxondaryo", new String[]{"Termiz", "Oltinsoy", "Angor", "Boysun", "Denov", "Jarqo'rg'on", "Qumqo'rg'on", "Qiziriq", "Sariosiyo", "Termiz tumani", "Uzun", "Sherobod", "Sho'rchi"});
        DISTRICTS_MAP.put("Qoraqalpog'iston", new String[]{"Nukus", "Amudaryo", "Beruniy", "Chimboy", "Ellikqala", "Kegeyli", "Mo'ynoq", "Qonliko'l", "Qorauzak", "Qo'ng'irot", "Taxtako'pir", "To'rtko'l", "Xo'jayli"});

        // Aladhan API uchun inglizcha shahar markazlari
        REGION_API_CITY_MAP.put("Toshkent shahri", "Tashkent");
        REGION_API_CITY_MAP.put("Toshkent viloyati", "Tashkent");
        REGION_API_CITY_MAP.put("Andijon", "Andijan");
        REGION_API_CITY_MAP.put("Buxoro", "Bukhara");
        REGION_API_CITY_MAP.put("Farg'ona", "Fergana");
        REGION_API_CITY_MAP.put("Jizzax", "Jizzakh");
        REGION_API_CITY_MAP.put("Xorazm", "Urgench");
        REGION_API_CITY_MAP.put("Namangan", "Namangan");
        REGION_API_CITY_MAP.put("Navoiy", "Navoi");
        REGION_API_CITY_MAP.put("Qashqadaryo", "Qarshi");
        REGION_API_CITY_MAP.put("Samarqand", "Samarkand");
        REGION_API_CITY_MAP.put("Sirdaryo", "Guliston");
        REGION_API_CITY_MAP.put("Surxondaryo", "Termez");
        REGION_API_CITY_MAP.put("Qoraqalpog'iston", "Nukus");
    }

    public static Set<String> getRegions() {
        return DISTRICTS_MAP.keySet();
    }

    public static String[] getDistricts(String region) {
        return DISTRICTS_MAP.getOrDefault(region, new String[]{"Markaz"});
    }

    public static String getApiCity(String region) {
        return REGION_API_CITY_MAP.getOrDefault(region, "Tashkent");
    }
}
