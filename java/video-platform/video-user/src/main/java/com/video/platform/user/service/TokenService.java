package com.video.platform.user.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class TokenService {

    @Value("${token.secret:video-platform-secret-key-2025}")
    private String secret;

    @Value("${token.expire:86400}")
    private long expire;

    private final StringRedisTemplate redisTemplate;

    public TokenService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Map<String, Object> generateToken(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        
        // 存储到 Redis，key 为 token，value 为用户 ID
        String redisKey = "token:" + token;
        redisTemplate.opsForValue().set(redisKey, userId.toString(), expire, TimeUnit.SECONDS);
        
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("expire", expire);
        return result;
    }

    public Long getUserIdFromToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        
        String redisKey = "token:" + token;
        String userIdStr = redisTemplate.opsForValue().get(redisKey);
        
        if (userIdStr != null) {
            // 续期
            redisTemplate.expire(redisKey, expire, TimeUnit.SECONDS);
            return Long.parseLong(userIdStr);
        }
        
        return null;
    }

    public void removeToken(String token) {
        if (token != null && !token.isEmpty()) {
            String redisKey = "token:" + token;
            redisTemplate.delete(redisKey);
        }
    }

    public boolean validateToken(String token) {
        return getUserIdFromToken(token) != null;
    }
}
