package uz.nomoz.signal.model;

public class PrayerTimes {
    private String date;        // masalan "15 Aug 2026"
    private String fajr;        // Bomdod
    private String sunrise;     // Quyosh
    private String dhuhr;       // Peshin
    private String asr;         // Asr
    private String maghrib;     // Shom
    private String isha;        // Hufton
    private String region;
    private String district;

    public PrayerTimes() {}

    public PrayerTimes(String date, String fajr, String sunrise, String dhuhr, String asr, String maghrib, String isha, String region, String district) {
        this.date = date;
        this.fajr = fajr;
        this.sunrise = sunrise;
        this.dhuhr = dhuhr;
        this.asr = asr;
        this.maghrib = maghrib;
        this.isha = isha;
        this.region = region;
        this.district = district;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getFajr() { return fajr; }
    public void setFajr(String fajr) { this.fajr = fajr; }

    public String getSunrise() { return sunrise; }
    public void setSunrise(String sunrise) { this.sunrise = sunrise; }

    public String getDhuhr() { return dhuhr; }
    public void setDhuhr(String dhuhr) { this.dhuhr = dhuhr; }

    public String getAsr() { return asr; }
    public void setAsr(String asr) { this.asr = asr; }

    public String getMaghrib() { return maghrib; }
    public void setMaghrib(String maghrib) { this.maghrib = maghrib; }

    public String getIsha() { return isha; }
    public void setIsha(String isha) { this.isha = isha; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
}
