package service;

import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.JedisPubSub;

public class RedisMessageService {
	private final JedisPooled jedisPooled;

	public RedisMessageService(JedisPooled jedisPooled) {
		this.jedisPooled = jedisPooled;
	}

	public void pushRecentMessage(String idGroup_User, String messageJson) {
		jedisPooled.lpush("chat:recent"+idGroup_User, messageJson);// catch message
		jedisPooled.ltrim("chat:recent"+idGroup_User, 0, 49); // giữ 50 tin nhắn gần nhất
	}

	public String getLatestMessage(String idGroup_User) {
	    String msg = jedisPooled.lindex("chat:recent" + idGroup_User, 0);
	    return msg != null ? msg : "";
	}


	// Pub/Sub realtime
	// danh cho channel rieng phu hop cho 1 nhom
	public void publishMessage(String channel, String messageJson) {
		jedisPooled.publish(channel, messageJson);
	}

	public void subscribeToChannel(String channel, JedisPubSub listener) {
		new Thread(() -> jedisPooled.subscribe(listener, channel)).start();
	}

}
