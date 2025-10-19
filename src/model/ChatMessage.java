package model;

import java.time.LocalDateTime;
import org.bson.Document;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class ChatMessage {

    private Id _id; 

    @SerializedName("sender_id")
    private String senderId;

    @SerializedName("receiver_id")
    private String receiverId; // dùng cho chat cá nhân

    @SerializedName("group_id")
    private String groupId; // nếu là chat nhóm

    private String content;

    private String type; // "text", "image", "file", "sticker", "call"
    
    @SerializedName("chat_type")
    private String chatType; // "private", "group", "community"

    private LocalDateTime timestamp;

    @SerializedName("is_read")
    private boolean isRead;

    public Document toDocument() {
        return new Document("sender_id", senderId)
                .append("receiver_id", receiverId)
                .append("group_id", groupId)
                .append("content", content)
                .append("type", type)
                .append("chat_type", chatType)
                .append("timestamp", timestamp.toString())
                .append("is_read", isRead);
    }

    static class Id {
        @SerializedName("$oid")
        private String oid;

        public String getOid() {
            return oid;
        }
    }

    public String getIdHex() {
        return _id != null ? _id.oid : null;
    }
}
