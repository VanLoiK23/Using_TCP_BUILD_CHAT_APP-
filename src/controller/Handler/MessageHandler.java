package controller.Handler;

import com.google.gson.Gson;

import model.ChatMessage;
import model.Packet;
import service.ChatService;

public class MessageHandler {
	private ChatService chatService;
	private Gson gson;

	public MessageHandler(ChatService chatService, Gson gson) {
		this.chatService = chatService;
		this.gson = gson;
	}

	public Boolean handleSave(Packet packet) {
		ChatMessage msg = gson.fromJson(gson.toJson(packet.getData()), ChatMessage.class);
		
		return chatService.saveMessage(msg);
	}
}
