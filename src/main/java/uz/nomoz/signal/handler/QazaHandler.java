package uz.nomoz.signal.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.bots.AbsSender;
import uz.nomoz.signal.database.QazaRepository;
import uz.nomoz.signal.keyboard.KeyboardFactory;
import uz.nomoz.signal.model.QazaRecord;

public class QazaHandler {
    private static final Logger log = LoggerFactory.getLogger(QazaHandler.class);

    public static void showQazaMenu(long chatId, AbsSender sender) {
        QazaRecord q = QazaRepository.getOrCreate(chatId);
        String text = formatQazaText(q);

        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText(text);
        msg.setParseMode("Markdown");
        msg.setReplyMarkup(KeyboardFactory.getQazaKeyboard(q));

        try {
            sender.execute(msg);
        } catch (Exception e) {
            log.error("Qazo menyusini yuborishda xatolik: chatId={}", chatId, e);
        }
    }

    public static void handleCallback(long chatId, int messageId, String data, AbsSender sender) {
        if (data.startsWith("QAZA_DEC_")) {
            String type = data.replace("QAZA_DEC_", "");
            QazaRepository.changeCount(chatId, type, -1);
        } else if (data.startsWith("QAZA_INC_")) {
            String type = data.replace("QAZA_INC_", "");
            QazaRepository.changeCount(chatId, type, 1);
        } else if (data.startsWith("QAZA_INC10_")) {
            String type = data.replace("QAZA_INC10_", "");
            QazaRepository.changeCount(chatId, type, 10);
        } else if (data.equals("QAZA_DEC_ALL")) {
            QazaRepository.changeCount(chatId, "fajr", -1);
            QazaRepository.changeCount(chatId, "dhuhr", -1);
            QazaRepository.changeCount(chatId, "asr", -1);
            QazaRepository.changeCount(chatId, "maghrib", -1);
            QazaRepository.changeCount(chatId, "isha", -1);
            QazaRepository.changeCount(chatId, "witr", -1);
        }

        QazaRecord updated = QazaRepository.getOrCreate(chatId);
        String text = formatQazaText(updated);

        EditMessageText edit = new EditMessageText();
        edit.setChatId(String.valueOf(chatId));
        edit.setMessageId(messageId);
        edit.setText(text);
        edit.setParseMode("Markdown");
        edit.setReplyMarkup(KeyboardFactory.getQazaKeyboard(updated));

        try {
            sender.execute(edit);
        } catch (Exception e) {
            log.error("Qazo holatini yangilashda xatolik: chatId={}", chatId, e);
        }
    }

    private static String formatQazaText(QazaRecord q) {
        return String.format(
                """
                📊 *Qazo Namozlari Hisoblagichi*
                
                Qazo bo'lgan namozlaringiz hisobi:
                
                🌅 Bomdod: *%d* ta
                🏙 Peshin: *%d* ta
                🌇 Asr: *%d* ta
                🌆 Shom: *%d* ta
                🌃 Hufton: *%d* ta
                ✨ Vitr: *%d* ta
                
                📌 Jami qazo namozlar: *%d* ta
                
                _Har safar qazo namozi o'qiganingizda tegishli `➖ 1` tugmasini bosing._
                """,
                q.getFajr(), q.getDhuhr(), q.getAsr(), q.getMaghrib(), q.getIsha(), q.getWitr(), q.getTotal()
        );
    }
}
