package uz.nomoz.signal.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.nomoz.signal.model.Mosque;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MosqueFinderService {
    private static final Logger log = LoggerFactory.getLogger(MosqueFinderService.class);
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build();

    public static List<Mosque> findNearbyMosques(double userLat, double userLon) {
        List<Mosque> mosques = new ArrayList<>();

        // OpenStreetMap Overpass API so'rovi (5 km radius)
        String query = String.format(
                """
                [out:json][timeout:5];
                (
                  node["amenity"="place_of_worship"]["religion"="muslim"](around:5000,%f,%f);
                  way["amenity"="place_of_worship"]["religion"="muslim"](around:5000,%f,%f);
                );
                out center 10;
                """, userLat, userLon, userLat, userLon);

        String url = "https://overpass-api.de/api/interpreter?data=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);

        try {
            Request request = new Request.Builder().url(url).build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    JsonObject root = JsonParser.parseString(body).getAsJsonObject();
                    JsonArray elements = root.getAsJsonArray("elements");

                    if (elements != null) {
                        for (int i = 0; i < elements.size(); i++) {
                            JsonObject elem = elements.get(i).getAsJsonObject();
                            double mLat;
                            double mLon;

                            if (elem.has("center")) {
                                JsonObject center = elem.getAsJsonObject("center");
                                mLat = center.get("lat").getAsDouble();
                                mLon = center.get("lon").getAsDouble();
                            } else if (elem.has("lat") && elem.has("lon")) {
                                mLat = elem.get("lat").getAsDouble();
                                mLon = elem.get("lon").getAsDouble();
                            } else {
                                continue;
                            }

                            String name = "Masjid";
                            if (elem.has("tags")) {
                                JsonObject tags = elem.getAsJsonObject("tags");
                                if (tags.has("name:uz")) {
                                    name = tags.get("name:uz").getAsString();
                                } else if (tags.has("name")) {
                                    name = tags.get("name").getAsString();
                                }
                            }

                            double distance = calculateDistanceMeters(userLat, userLon, mLat, mLon);
                            mosques.add(new Mosque(name, mLat, mLon, distance));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Overpass API orqali masjidlarni qidirishda ogohlantirish (Fallback ishlatiladi):", e);
        }

        // Masofa bo'yicha eng yaqindan saralash va top 5 tasini olish
        mosques.sort(Comparator.comparingDouble(Mosque::getDistanceMeters));
        if (mosques.size() > 5) {
            return mosques.subList(0, 5);
        }
        return mosques;
    }

    public static double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000; // Yer radiusi metrda
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
