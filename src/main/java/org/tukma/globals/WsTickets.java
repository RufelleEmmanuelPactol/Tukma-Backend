package org.tukma.globals;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.concurrent.TimeUnit;

@Configuration
public class WsTickets {

    private static JedisPool jedisPool;

    @Value("${spring.data.redis.url}")
    public void setRedisUrl(String redisUrl) {
        jedisPool = new JedisPool(redisUrl);
    }

    private static final int EXPIRATION_TIME = (int) TimeUnit.HOURS.toSeconds(1);

    public static void addTicket(String ticket, long timestamp) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(ticket, EXPIRATION_TIME, String.valueOf(timestamp));
        }
    }

    public static Long getTicket(String ticket) {
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get(ticket);
            return value != null ? Long.parseLong(value) : null;
        }
    }

    public static void removeTicket(String ticket) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(ticket);
        }
    }

    public static boolean hasTicket(String ticket) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.exists(ticket);
        }
    }
}