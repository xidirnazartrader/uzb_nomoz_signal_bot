# 🕌 Uzb Namoz Signal Bot

O'zbekiston hududlari bo'yicha aniq namoz vaqtlarini ko'rsatuvchi, har bir namoz vaqtida avtomatik **Signal (Eslatma)** yuboruvchi hamda elektron tasbeh imkoniyatiga ega zamonaviy Telegram boti.

---

## 🌟 Asosiy Imkoniyatlar

- 📍 **O'zbekistonning barcha 14 ta hududi va tumanlari** qamrab olingan.
- 📅 **1 Kunlik, 1 Haftalik va 1 Oylik** namoz vaqtlari taqvimi.
- 🔔 **Avtomatik Namoz Signallari (Eslatma/Scheduler)**:
  - Har bir namoz (Bomdod, Quyosh, Peshin, Asr, Shom, Hufton) vaqtida yoki 15 daqiqa oldin avtomatik azon/namoz xabari.
  - Sozlamalar orqali signal vaqtini moslash yoki o'chirib qo'yish imkoniyati.
- 📿 **Elektron Tasbeh**:
  - 33 talik bosqichli zikrlar (*Subhanalloh* -> *Alhamdulillah* -> *Allohu Akbar*) va progress bar.
- ⚡ **Tezkor va Keshli API**:
  - Aladhan API bilan barqaror ulanish va ortiqcha so'rovlarni kamaytiruvchi kesh tizimi.
- 🗄 **SQLite + HikariCP Connection Pool**:
  - Foydalanuvchilar holati va sozlamalari xavfsiz va ishonchli saqlanadi.

---

## 🛠 Texnologiyalar

- **Til**: Java 17+
- **Freymvork**: [TelegramBots Java Library (6.8.0)](https://github.com/rubenlagus/TelegramBots)
- **Ma'lumotlar bazasi**: SQLite + [HikariCP](https://github.com/brettwooldridge/HikariCP)
- **HTTP Client**: [OkHttp 4](https://square.github.io/okhttp/)
- **JSON**: [Google Gson](https://github.com/google/gson)
- **Logging**: SLF4J + Logback
- **Build Tool**: Apache Maven

---

## 🚀 O'rnatish va Ishga tushirish

### 1. Loyihani klonlash
```bash
git clone https://github.com/xidirnazartrader/uzb_nomoz_signal_bot.git
cd uzb_nomoz_signal_bot
```

### 2. Sozlamalarni kiritish
`src/main/resources/bot.properties.example` faylidan nusxa olib, `bot.properties` yarating:
```properties
bot.token=SIZNING_BOT_TOKENINGIZ
bot.username=SIZNING_BOT_USERNAMINGIZ
db.url=jdbc:sqlite:namoz_bot.db
```
*(Yoki muhit o'zgaruvchilari orqali: `BOT_TOKEN`, `BOT_USERNAME`)*

### 3. Kompilyatsiya va Test
```bash
mvn clean test
```

### 4. Ishga tushirish
```bash
mvn compile exec:java -Dexec.mainClass="uz.nomoz.signal.Main"
```

---

## 📂 Loyiha Strukturasi (Clean Architecture)

```
src/main/java/uz/nomoz/signal/
├── Main.java                       # Kirish nuqtasi va lifecycle
├── config/                         # Bot va Baza konfiguratsiyasi
├── database/                       # SQLite va Repository qatlami
├── model/                          # DTO va modellar
├── service/                        # Namoz vaqtlari, Tasbeh va Scheduler
├── keyboard/                       # Tugmalar fabrikasi
├── handler/                        # Xabarlar dispetcheri va handlerlar
└── bot/                            # Telegram Bot klassi
```
