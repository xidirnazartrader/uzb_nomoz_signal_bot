package uz.nomoz.signal.model;

public class Mosque {
    private String name;
    private double latitude;
    private double longitude;
    private double distanceMeters;

    public Mosque(String name, double latitude, double longitude, double distanceMeters) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceMeters = distanceMeters;
    }

    public String getName() { return name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getDistanceMeters() { return distanceMeters; }

    public String getFormattedDistance() {
        if (distanceMeters < 1000) {
            return Math.round(distanceMeters) + " metr";
        } else {
            return String.format("%.1f km", distanceMeters / 1000.0);
        }
    }

    public String getGoogleMapsUrl() {
        return String.format("https://www.google.com/maps/dir/?api=1&destination=%f,%f", latitude, longitude);
    }

    public String getYandexMapsUrl() {
        return String.format("https://yandex.uz/maps/?rtext=~%f,%f&rtt=pd", latitude, longitude);
    }
}
