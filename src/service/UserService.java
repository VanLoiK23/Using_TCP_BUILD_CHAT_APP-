package service;

import static com.mongodb.client.model.Filters.eq;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;

import model.User;
import util.CloudinaryUtil;
import util.MongoUtil;
import util.MultiDateDeserializer;
import util.PasswordUtil;
import util.RedisUtil;

public class UserService {
	private final MongoCollection<Document> userCollection;
//	private final Gson gson = 
////			Converters.registerAll(new GsonBuilder()).create();
////			 new GsonBuilder()
//			Converters.registerAll(new GsonBuilder())
//		    .setDateFormat("EEE MMM dd HH:mm:ss z yyyy")
//		    .create();

	private final Gson gson = Converters.registerAll(new GsonBuilder())
			.registerTypeAdapter(Date.class, new MultiDateDeserializer()).create();

	private RedisUserService redisUserService;
	private RedisOnlineService redisOnlineService;

	public UserService() {
		MongoDatabase db = MongoUtil.getDatabase();
		userCollection = db.getCollection("user");
		redisUserService = new RedisUserService(RedisUtil.getClient());
		redisOnlineService = new RedisOnlineService(RedisUtil.getClient());
	}

	public void setUpLogin(User user) {
		redisUserService.cacheUserInfo(user.getIdHex(), user.getUsername(), user.getAvatarUrl());
		redisOnlineService.setUserOnline(user.getIdHex());
	}

	public List<User> getAllUser() {
		List<User> result = new ArrayList<>();

		FindIterable<Document> documents = userCollection.find();
		try (MongoCursor<Document> cursor = documents.iterator()) {
			while (cursor.hasNext()) {
				User user = gson.fromJson(cursor.next().toJson(), User.class);
				result.add(user);
			}
		}

		return result;
	}

	public String upsertImg(File file) throws IOException {

		Cloudinary cloudinary = CloudinaryUtil.getCloudinary();

		Map uploadResult = cloudinary.uploader().upload(file, ObjectUtils.emptyMap());
		String imageUrl = (String) uploadResult.get("secure_url");

		System.out.println("img avatar :" + imageUrl);

		return imageUrl;
	}

	public User getUserById(String userId) {
		try {
			ObjectId objectId = new ObjectId(userId); // chuyển chuỗi sang ObjectId
			Document doc = userCollection.find(eq("_id", objectId)).first();
			if (doc != null) {
				return gson.fromJson(doc.toJson(), User.class);
			}
		} catch (IllegalArgumentException e) {
			System.out.println("ID không hợp lệ: " + userId);
		}
		return null;
	}

	public boolean updateUserById(String userId, User newData) {
		try {
			ObjectId objectId = new ObjectId(userId);

			List<Bson> updates = new ArrayList<>();

			updates.add(Updates.set("username", newData.getUsername()));
			updates.add(Updates.set("gender", newData.getGender()));
			updates.add(Updates.set("bod", newData.getBod().toString()));
			updates.add(Updates.set("email", newData.getEmail()));
			updates.add(Updates.set("avatarUrl", newData.getAvatarUrl()));

			if (newData.getPassword() != null && !newData.getPassword().isEmpty()) {
				updates.add(Updates.set("password", PasswordUtil.hashPassword(newData.getPassword())));
			}

			// Thực hiện cập nhật
			Bson update = Updates.combine(updates);
			UpdateResult result = userCollection.updateOne(eq("_id", objectId), update);

			Boolean isSuccess = result.getModifiedCount() > 0;

			if (isSuccess) {
				setUpLogin(newData);
			}
			
			return isSuccess;

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public User getUserByUserName(String username) {
//		 Bson bsonFilter = Filters.eq("name", "Troy");

		Document doc = userCollection.find(eq("username", username)).first();
		if (doc != null) {
			System.out.println(doc.toJson());
			return gson.fromJson(doc.toJson(), User.class);
		}
		return null;
	}

	public String insertOne(User user) {
		InsertOneResult result = userCollection.insertOne(user.toDocument());
		return result.getInsertedId().asObjectId().getValue().toHexString();
	}

	public Boolean authenticationLogin(String username, String plainPassword) {
		Document doc = userCollection.find(eq("username", username)).first();

		if (doc != null) {
			User user = gson.fromJson(doc.toJson(), User.class);

			return PasswordUtil.checkPassword(plainPassword, user.getPassword());
		}

		return false;
	}

	public Boolean isSameUserName(String username) {
		return (userCollection.find(eq("username", username)).first() != null);
	}

}
