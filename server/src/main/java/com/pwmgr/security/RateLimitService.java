package com.pwmgr.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    @Value("${app.auth.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.auth.lockout-minutes:15}")
    private int lockoutMinutes;

    private final ConcurrentHashMap<String, AttemptInfo> attemptCache = new ConcurrentHashMap<>();

    public boolean isBlocked(String clientId) {
        AttemptInfo info = attemptCache.get(clientId);
        if (info == null) {
            return false;
        }

        if (info.blockedUntil != null && LocalDateTime.now().isBefore(info.blockedUntil)) {
            return true;
        }

        if (info.blockedUntil != null && LocalDateTime.now().isAfter(info.blockedUntil)) {
            attemptCache.remove(clientId);
            return false;
        }

        return false;
    }

    public void recordFailedAttempt(String clientId) {
        attemptCache.compute(clientId, (key, info) -> {
            if (info == null) {
                info = new AttemptInfo();
            }
            info.failedAttempts++;

            if (info.failedAttempts >= maxAttempts) {
                info.blockedUntil = LocalDateTime.now().plusMinutes(lockoutMinutes);
            }

            return info;
        });
    }

    public void recordSuccessfulAttempt(String clientId) {
        attemptCache.remove(clientId);
    }

    public int getRemainingAttempts(String clientId) {
        AttemptInfo info = attemptCache.get(clientId);
        if (info == null) {
            return maxAttempts;
        }
        return Math.max(0, maxAttempts - info.failedAttempts);
    }

    public LocalDateTime getBlockedUntil(String clientId) {
        AttemptInfo info = attemptCache.get(clientId);
        if (info == null) {
            return null;
        }
        return info.blockedUntil;
    }

    private static class AttemptInfo {
        int failedAttempts = 0;
        LocalDateTime blockedUntil = null;
    }
}
