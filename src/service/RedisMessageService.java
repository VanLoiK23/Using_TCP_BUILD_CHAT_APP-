package service;

import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.JedisPubSub;

public class RedisMessageService {
	private final JedisPooled jedisPooled;

	public RedisMessageService(JedisPooled jedisPooled) {
		this.jedisPooled = jedisPooled;
	}

	public void pushRecentMessage(String messageJson) {
		jedisPooled.lpush("chat:recent", messageJson);// catch message
		jedisPooled.ltrim("chat:recent", 0, 49); // giữ 50 tin nhắn gần nhất
	}

	public String getLatestMessage() {
		return jedisPooled.lindex("chat:recent", 0);
	}

	// Pub/Sub realtime
	//danh cho channel rieng phu hop cho 1 nhom
	public void publishMessage(String channel, String messageJson) {
		jedisPooled.publish(channel, messageJson);
	}

	public void subscribeToChannel(String channel, JedisPubSub listener) {
		new Thread(() -> jedisPooled.subscribe(listener, channel)).start();
	}

}
