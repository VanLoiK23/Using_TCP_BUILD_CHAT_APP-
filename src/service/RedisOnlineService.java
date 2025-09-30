package service;

import java.util.Set;

import redis.clients.jedis.JedisPooled;

public class RedisOnlineService {
    private final JedisPooled jedisPooled;

    public RedisOnlineService(JedisPooled jedisPooled) {
        this.jedisPooled = jedisPooled;
    }

    public void setUserOnline(String userId) {
        jedisPooled.sadd("online_users", userId);
    }

    public void setUserOffline(String userId) {
        jedisPooled.srem("online_users", userId);
    }

    public boolean isUserOnline(String userId) {
        return jedisPooled.sismember("online_users", userId);
    }

    public Set<String> getAllOnlineUsers() {
        return jedisPooled.smembers("online_users");
    }
}
