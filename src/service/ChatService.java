package service;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.or;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.InsertOneResult;

import model.ChatMessage;
import redis.clients.jedis.JedisPooled;
import util.MongoUtil;
import util.RedisUtil;

public class ChatService {

	private final MongoCollection<Document> messageCollection;
	private final MongoCollection<Document> userCollection;
	private final JedisPooled jedisPooled;
	private RedisMessageService redisMessageService;
	private RedisUserService redisUserService;
	private final Gson gson = Converters.registerAll(new GsonBuilder()).setDateFormat("EEE MMM dd HH:mm:ss z yyyy")
			.create();

	public ChatService() {
		MongoDatabase db = MongoUtil.getDatabase();
		messageCollection = db.getCollection("messages");
		userCollection = db.getCollection("user");
		jedisPooled = RedisUtil.getClient();
		redisMessageService = new RedisMessageService(jedisPooled);
		redisUserService = new RedisUserService(jedisPooled);
	}

	public Boolean saveMessage(ChatMessage msg) {
		InsertOneResult result = messageCollection.insertOne(msg.toDocument());

		if (msg.getType().equals("file")) {
			String fileName = msg.getContent();
			String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
			boolean isImage = extension.equals("jpg") || extension.equals("jpeg") || extension.equals("png");

			if (isImage) {
				msg.setContent("đã gửi 1 ảnh");
			} else {
				msg.setContent("đã gửi 1 file đính kèm");
			}
		}

		String lastMessageString = redisUserService.getCachedUsername(msg.getSenderId()) + ": " + msg.getContent();
		if (result.wasAcknowledged()) {
			redisMessageService.pushRecentMessage(msg.getSenderId(), lastMessageString); // catch lưu tin nhắn mới nhất

			return true;
		}
		return false;
	}

	public List<ChatMessage> getAllMessages() {
		List<ChatMessage> result = new ArrayList<>();

		FindIterable<Document> documents = messageCollection.find();
		try (MongoCursor<Document> cursor = documents.iterator()) {
			while (cursor.hasNext()) {
				ChatMessage message = gson.fromJson(cursor.next().toJson(), ChatMessage.class);
				result.add(message);
			}
		}

		return result;
	}

	public List<ChatMessage> getMessagesBetween(String userA, String userB) {
		List<ChatMessage> result = new ArrayList<>();
		Bson filter;
		// group community

		filter = or(and(eq("sender_id", userA), eq("receiver_id", userB)),
				and(eq("sender_id", userB), eq("receiver_id", userA)));

		FindIterable<Document> documents = messageCollection.find(filter).sort(Sorts.ascending("timestamp"));

		try (MongoCursor<Document> cursor = documents.iterator()) {
			while (cursor.hasNext()) {
				ChatMessage message = gson.fromJson(cursor.next().toJson(), ChatMessage.class);
				result.add(message);
			}
		}

		return result;
	}

	public List<ChatMessage> getMessagesCommunication() {
		List<ChatMessage> result = new ArrayList<>();
		Bson filter;
		// group community

		filter = eq("chat_type", "community");

		FindIterable<Document> documents = messageCollection.find(filter).sort(Sorts.ascending("timestamp"));

		try (MongoCursor<Document> cursor = documents.iterator()) {
			while (cursor.hasNext()) {
				ChatMessage message = gson.fromJson(cursor.next().toJson(), ChatMessage.class);
				result.add(message);
			}
		}

		return result;
	}

}
