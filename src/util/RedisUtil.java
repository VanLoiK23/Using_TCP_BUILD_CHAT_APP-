package util;

import redis.clients.jedis.JedisPooled;

public class RedisUtil {
    private static JedisPooled jedisPooled;

    static {
        String host = System.getenv("REDIS_HOST"); 
        String portStr = System.getenv("REDIS_PORT");
        String password = System.getenv("REDIS_PASS");

        if (host == null || portStr == null || password == null) {
            throw new RuntimeException("Thiếu biến môi trường Redis");
        }

        int port = Integer.parseInt(portStr);
//        jedis = new Jedis(host, port);
//        jedis.auth(password);
        
        try {
            String redisUrl = "rediss://default:"+password+"@"+host+":"+port;
            jedisPooled = new JedisPooled(redisUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static JedisPooled getClient() {
        return jedisPooled;
    }
}