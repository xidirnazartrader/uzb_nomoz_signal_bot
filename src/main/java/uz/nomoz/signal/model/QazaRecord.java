package uz.nomoz.signal.model;

public class QazaRecord {
    private long chatId;
    private int fajr;       // Bomdod
    private int dhuhr;      // Peshin
    private int asr;        // Asr
    private int maghrib;    // Shom
    private int isha;       // Hufton
    private int witr;       // Vitr

    public QazaRecord() {}

    public QazaRecord(long chatId, int fajr, int dhuhr, int asr, int maghrib, int isha, int witr) {
        this.chatId = chatId;
        this.fajr = fajr;
        this.dhuhr = dhuhr;
        this.asr = asr;
        this.maghrib = maghrib;
        this.isha = isha;
        this.witr = witr;
    }

    public long getChatId() { return chatId; }
    public void setChatId(long chatId) { this.chatId = chatId; }

    public int getFajr() { return fajr; }
    public void setFajr(int fajr) { this.fajr = Math.max(0, fajr); }

    public int getDhuhr() { return dhuhr; }
    public void setDhuhr(int dhuhr) { this.dhuhr = Math.max(0, dhuhr); }

    public int getAsr() { return asr; }
    public void setAsr(int asr) { this.asr = Math.max(0, asr); }

    public int getMaghrib() { return maghrib; }
    public void setMaghrib(int maghrib) { this.maghrib = Math.max(0, maghrib); }

    public int getIsha() { return isha; }
    public void setIsha(int isha) { this.isha = Math.max(0, isha); }

    public int getWitr() { return witr; }
    public void setWitr(int witr) { this.witr = Math.max(0, witr); }

    public int getTotal() {
        return fajr + dhuhr + asr + maghrib + isha + witr;
    }
}
