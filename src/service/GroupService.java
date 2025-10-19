package service;

import static com.mongodb.client.model.Filters.eq;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;

import model.Group;
import model.User;
import util.MongoUtil;

public class GroupService {

	private final MongoCollection<Document> groupCollection;
	private final Gson gson = Converters.registerAll(new GsonBuilder()).setDateFormat("EEE MMM dd HH:mm:ss z yyyy")
			.create();

	public GroupService() {
		MongoDatabase db = MongoUtil.getDatabase();
		groupCollection = db.getCollection("groups");
	}

	public Boolean saveGroup(Group group) {
		InsertOneResult result = groupCollection.insertOne(group.toDocument());
		if (result.wasAcknowledged()) {
			return true;
		}
		return false;
	}

	public Boolean updateGroup(Group group) {
		Bson filter = Filters.eq("_id", new ObjectId(group.getIdHex()));

		Document update = new Document("$set", group.toDocument());

		UpdateResult result = groupCollection.updateOne(filter, update);

		return result.getMatchedCount() > 0 && result.getModifiedCount() > 0;
	}
	
	public Group getGroupById(String groupID) {
	    try {
	        ObjectId objectId = new ObjectId(groupID); // chuyển chuỗi sang ObjectId
	        Document doc = groupCollection.find(eq("_id", objectId)).first();
	        if (doc != null) {
	            return gson.fromJson(doc.toJson(), Group.class);
	        }
	    } catch (IllegalArgumentException e) {
	        System.out.println("ID không hợp lệ: " + groupID);
	    }
	    return null;
	}

	public List<Group> getAllGroups() {
		List<Group> result = new ArrayList<>();

		FindIterable<Document> documents = groupCollection.find();
		try (MongoCursor<Document> cursor = documents.iterator()) {
			while (cursor.hasNext()) {
				Group gr = gson.fromJson(cursor.next().toJson(), Group.class);
				result.add(gr);
			}
		}

		return result;
	}

	public List<Group> getGroupOfUser(String userId) {
		List<Group> groupIds = new ArrayList<>();

		FindIterable<Document> groupDocs = groupCollection.find(Filters.in("members", userId));
		try (MongoCursor<Document> cursor = groupDocs.iterator()) {
			while (cursor.hasNext()) {
				Group gr = gson.fromJson(cursor.next().toJson(), Group.class);
				groupIds.add(gr);
			}
		}

		return groupIds;
	}
}
