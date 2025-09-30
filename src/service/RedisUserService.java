package service;

import redis.clients.jedis.JedisPooled;

public class RedisUserService {
	private final JedisPooled jedisPooled;

	public RedisUserService(JedisPooled jedisPooled) {
		this.jedisPooled = jedisPooled;
	}

	public void setUserStatus(String userId, String status) {
		jedisPooled.set("user:" + userId + ":status", status);
	}

	public String getUserStatus(String userId) {
		return jedisPooled.get("user:" + userId + ":status");
	}

	public void cacheUserInfo(String userId, String username, String avatarUrl) {
		if (username != null && !username.isEmpty()) {
			jedisPooled.hset("user:" + userId + ":info", "username", username);
		}
		if (avatarUrl != null && !avatarUrl.isEmpty()) {
			jedisPooled.hset("user:" + userId + ":info", "avatar", avatarUrl);
		}
	}

	public String getCachedUsername(String userId) {
		return jedisPooled.hget("user:" + userId + ":info", "username");

	}

	public String getCachedAvatar(String userId) {
		return jedisPooled.hget("user:" + userId + ":info", "avatar");
	}
}