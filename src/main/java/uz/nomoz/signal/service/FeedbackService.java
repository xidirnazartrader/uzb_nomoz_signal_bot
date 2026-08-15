package uz.nomoz.signal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;
import uz.nomoz.signal.config.BotConfig;
import uz.nomoz.signal.database.UserRepository;
import uz.nomoz.signal.keyboard.KeyboardFactory;
import uz.nomoz.signal.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FeedbackService {
    private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);

    // Foydalanuvchi holatlari (chatId -> state)
    private static final Map<Long, String> userStates = new ConcurrentHashMap<>();
    
    // Admin Reply qilish uchun: adminga borgan messageId -> murojaat qilgan userChatId
    private static final Map<Integer, Long> adminMessageToUserChatId = new ConcurrentHashMap<>();

    // Admin tugma orqali javob yozayotgan bo'lsa: adminChatId -> targetUserChatId
    private static final Map<Long, Long> adminActiveReplyTarget = new ConcurrentHashMap<>();

    public static void startFeedback(long chatId, AbsSender sender) {
        userStates.put(chatId, "AWAITING_FEEDBACK");

        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText("""
                ✍️ *Adminga murojaat yo'llash:*
                
                Savol, taklif yoki fikringizni bitta xabarda yozib yuboring. Adminlarimiz tez orada sizga javob berishadi.
                
                _(Bekor qilish uchun /cancel deb yozing yoki pastdagi menyudan foydalaning)_
                """);
        msg.setParseMode("Markdown");

        try {
            sender.execute(msg);
        } catch (Exception e) {
            log.error("Feedback so'rovini yuborishda xatolik:", e);
        }
    }

    public static boolean isUserWaitingForFeedback(long chatId) {
        return "AWAITING_FEEDBACK".equals(userStates.get(chatId));
    }

    public static boolean isAdminReplying(long chatId) {
        return adminActiveReplyTarget.containsKey(chatId);
    }

    public static void cancelState(long chatId) {
        userStates.remove(chatId);
        adminActiveReplyTarget.remove(chatId);
    }

    // 1. Foydalanuvchining murojaatini qabul qilish va adminga yuborish
    public static void handleUserMessage(long userChatId, Message message, AbsSender sender) {
        userStates.remove(userChatId);
        long adminId = BotConfig.getInstance().getAdminId();

        User user = UserRepository.findByChatId(userChatId).orElse(null);
        String region = (user != null) ? user.getRegion() + ", " + user.getDistrict() : "Aniqlanmagan";
        String userName = message.getFrom().getFirstName();
        String userUsername = (message.getFrom().getUserName() != null) ? "@" + message.getFrom().getUserName() : "yo'q";
        String text = message.getText();

        String adminNotification = String.format(
                """
                📩 *Yangi Murojaat keldi!*
                
                👤 *Kimdan:* %s (%s)
                🆔 *Chat ID:* `%d`
                📍 *Hudud:* %s
                
                💬 *Murojaat matni:*
                «%s»
                
                _Javob berish uchun ushbu xabarga Telegram'da "Reply" (Ответить) qiling yoki pastdagi tugmani bosing._
                """,
                userName, userUsername, userChatId, region, text
        );

        // Agar adminId 0 bo'lsa (hali belgilanmagan bo'lsa), barcha admin bo'lishi mumkin bo'lganlarga yoki logga yozamiz
        long targetAdminChatId = (adminId != 0) ? adminId : userChatId; // Fallback

        SendMessage adminMsg = new SendMessage();
        adminMsg.setChatId(String.valueOf(targetAdminChatId));
        adminMsg.setText(adminNotification);
        adminMsg.setParseMode("Markdown");

        // Reply tugmasi
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(InlineKeyboardButton.builder()
                .text("✍️ Javob yozish")
                .callbackData("FEEDBACK_REPLY_" + userChatId)
                .build());
        rows.add(row);
        adminMsg.setReplyMarkup(markup);

        try {
            Message sent = sender.execute(adminMsg);
            if (sent != null) {
                adminMessageToUserChatId.put(sent.getMessageId(), userChatId);
            }

            // Foydalanuvchiga tasdiq xabari
            SendMessage confirmMsg = new SendMessage();
            confirmMsg.setChatId(String.valueOf(userChatId));
            confirmMsg.setText("✅ *Murojaatingiz adminga muvaffaqiyatli yetkazildi!*\n\nTez orada sizga javob beriladi. Rahmat!");
            confirmMsg.setParseMode("Markdown");
            confirmMsg.setReplyMarkup(KeyboardFactory.getMainKeyboard());
            sender.execute(confirmMsg);

        } catch (Exception e) {
            log.error("Murojaatni adminga yuborishda xatolik:", e);
        }
    }

    // 2. Admin Telegram "Reply" (Ответить) qilganda javobni foydalanuvchiga uzatish
    public static boolean handleAdminTelegramReply(long adminChatId, Message replyToMessage, String replyText, AbsSender sender) {
        if (replyToMessage == null) return false;

        Long targetUserChatId = adminMessageToUserChatId.get(replyToMessage.getMessageId());
        if (targetUserChatId == null) {
            // Matndan ID qidirish
            String originalText = replyToMessage.getText();
            if (originalText != null && originalText.contains("Chat ID:")) {
                try {
                    String[] lines = originalText.split("\n");
                    for (String line : lines) {
                        if (line.contains("Chat ID:")) {
                            String idStr = line.replaceAll("[^0-9]", "");
                            targetUserChatId = Long.parseLong(idStr);
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        if (targetUserChatId != null) {
            sendReplyToUser(targetUserChatId, replyText, sender);
            sendDirectMessage(adminChatId, "✅ Javobingiz foydalanuvchiga yetkazildi!", sender);
            return true;
        }

        return false;
    }

    // 3. Admin tugma orqali javob yozganda
    public static void handleAdminButtonReply(long adminChatId, String replyText, AbsSender sender) {
        Long targetUserChatId = adminActiveReplyTarget.remove(adminChatId);
        if (targetUserChatId != null) {
            sendReplyToUser(targetUserChatId, replyText, sender);
            sendDirectMessage(adminChatId, "✅ Javobingiz foydalanuvchiga yetkazildi!", sender);
        }
    }

    public static void setAdminReplyTarget(long adminChatId, long targetUserChatId, AbsSender sender) {
        adminActiveReplyTarget.put(adminChatId, targetUserChatId);
        sendDirectMessage(adminChatId, "✍️ Foydalanuvchiga yubormoqchi bo'lgan javobingizni yozing:\n_(Bekor qilish uchun /cancel deb yozing)_", sender);
    }

    // 4. Foydalanuvchiga admin javobini yuborish
    private static void sendReplyToUser(long targetUserChatId, String replyText, AbsSender sender) {
        String msg = String.format(
                """
                📩 *Admin Javobi:*
                
                %s
                
                _Agar yana savollaringiz bo'lsa, bemalol murojaat qilishingiz mumkin._
                """,
                replyText
        );

        SendMessage send = new SendMessage();
        send.setChatId(String.valueOf(targetUserChatId));
        send.setText(msg);
        send.setParseMode("Markdown");

        try {
            sender.execute(send);
            log.info("Admin javobi yuborildi: targetChatId={}", targetUserChatId);
        } catch (Exception e) {
            log.error("Foydalanuvchiga javob yuborishda xatolik: chatId={}", targetUserChatId, e);
        }
    }

    private static void sendDirectMessage(long chatId, String text, AbsSender sender) {
        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText(text);
        msg.setParseMode("Markdown");
        try {
            sender.execute(msg);
        } catch (Exception e) {
            log.error("Xabar yuborishda xatolik:", e);
        }
    }
}
