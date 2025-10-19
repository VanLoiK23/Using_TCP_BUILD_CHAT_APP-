package model;

import java.time.LocalDateTime;
import java.util.List;

import org.bson.Document;

import com.google.gson.annotations.SerializedName;

import lombok.Data;

@Data
public class Group {
	private Id _id;
	private String groupName;
	private String multicastIP;
	private String imageGroup;
	private List<String> members;
	private LocalDateTime timestamp;

	public Document toDocument() {
		return new Document("groupName", groupName).append("multicastIP", multicastIP).append("imageGroup", imageGroup).append("members", members)
				.append("timestamp", timestamp.toString());
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
