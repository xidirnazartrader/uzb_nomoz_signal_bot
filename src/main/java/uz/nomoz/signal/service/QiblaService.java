package uz.nomoz.signal.service;

public class QiblaService {
    // Ka'ba koordinatalari (Makkah al-Mukarramah)
    private static final double KAABA_LAT = 21.422487;
    private static final double KAABA_LON = 39.826206;

    public static double calculateQiblaAngle(double userLat, double userLon) {
        double lat1 = Math.toRadians(userLat);
        double lon1 = Math.toRadians(userLon);
        double lat2 = Math.toRadians(KAABA_LAT);
        double lon2 = Math.toRadians(KAABA_LON);

        double deltaLon = lon2 - lon1;

        double y = Math.sin(deltaLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLon);

        double bearing = Math.toDegrees(Math.atan2(y, x));
        double normalizedBearing = (bearing + 360) % 360;

        return Math.round(normalizedBearing * 10.0) / 10.0;
    }

    public static String getCardinalDirection(double angle) {
        if (angle >= 337.5 || angle < 22.5) return "Shimol (N) ⬆️";
        if (angle >= 22.5 && angle < 67.5) return "Shimoli-Sharq (NE) ↗️";
        if (angle >= 67.5 && angle < 112.5) return "Sharq (E) ➡️";
        if (angle >= 112.5 && angle < 157.5) return "Janubi-Sharq (SE) ↘️";
        if (angle >= 157.5 && angle < 202.5) return "Janub (S) ⬇️";
        if (angle >= 202.5 && angle < 247.5) return "Janubi-G'arb (SW) ↙️";
        if (angle >= 247.5 && angle < 292.5) return "G'arb (W) ⬅️";
        return "Shimoli-G'arb (NW) ↖️";
    }

    public static double calculateDistanceToKaabaKm(double userLat, double userLon) {
        double R = 6371.0; // Yer radiusi km
        double dLat = Math.toRadians(KAABA_LAT - userLat);
        double dLon = Math.toRadians(KAABA_LON - userLon);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(KAABA_LAT)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(R * c);
    }
}
