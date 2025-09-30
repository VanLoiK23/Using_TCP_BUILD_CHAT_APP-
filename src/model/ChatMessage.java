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
	private String receiverId; // hoặc roomId nếu chat nhóm
	private String content;
	private String type; // "text", "image", "file", "sticker", "call"
	private LocalDateTime timestamp;
	@SerializedName("is_read")
	private boolean isRead;

	public Document toDocument() {
		return new Document("sender_id", senderId).append("receiver_id", receiverId).append("content", content)
				.append("type", type).append("timestamp", timestamp.toString()).append("is_read", isRead);
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