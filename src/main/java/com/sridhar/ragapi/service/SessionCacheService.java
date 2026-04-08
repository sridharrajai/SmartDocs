package com.sridhar.ragapi.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class SessionCacheService {
    private final StringRedisTemplate redis;

    public SessionCacheService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void cacheSessionData(String userID, String sessionId) {
        redis.opsForValue().set("session:"+userID,sessionId, Duration.ofHours(1));
        redis.opsForValue().set("userid:" + sessionId, userID, Duration.ofHours(1));
    }

    public Optional<String> getSessionData(String userID) {
        return Optional.ofNullable(redis.opsForValue().get("session:"+userID));
    }
    public Optional<String> getUserIdBySession(String sessionId) {
        return Optional.ofNullable(redis.opsForValue().get("userid:" + sessionId));
    }
}
