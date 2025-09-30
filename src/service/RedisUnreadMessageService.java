package service;

import java.util.List;

import redis.clients.jedis.JedisPooled;

public class RedisUnreadMessageService {
    private final JedisPooled jedisPooled;

    public RedisUnreadMessageService(JedisPooled jedisPooled) {
        this.jedisPooled = jedisPooled;
    }

    public void pushUnreadMessage(String receiverId, String messageJson) {
        jedisPooled.lpush("unread:" + receiverId, messageJson);
    }

    public List<String> getUnreadMessages(String receiverId) {
        return jedisPooled.lrange("unread:" + receiverId, 0, -1);
    }

    public void clearUnreadMessages(String receiverId) {
        jedisPooled.del("unread:" + receiverId);
    }
}
