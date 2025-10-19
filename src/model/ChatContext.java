package model;

import controller.MainController;
import lombok.Data;


@Data
public class ChatContext {
	private ConversationType type;
	private String targetId; // groupId hoặc userId tùy theo type
	public ChatContext(ConversationType type) {
		super();
		this.type = type;
	}
	public ChatContext(ConversationType type, String targetId) {
		super();
		this.type = type;
		this.targetId = targetId;
	}
	
	
}

