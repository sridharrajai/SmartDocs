package com.sridhar.ragapi.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SessionManager {
    private final SessionCacheService cacheService;
    public SessionManager(SessionCacheService cacheService) {
        this.cacheService = cacheService;
    }
    public String getOrCreateSession(String userId, String sessionId) {

        return cacheService.getSessionData(userId)
                .orElseGet(() -> {
                    cacheService.cacheSessionData(userId, sessionId);
                    return sessionId;
                });
    }
}
