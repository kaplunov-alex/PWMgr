package com.pwmgr.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitServiceTest {

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService();
        // Set default values
        ReflectionTestUtils.setField(rateLimitService, "maxAttempts", 5);
        ReflectionTestUtils.setField(rateLimitService, "lockoutMinutes", 15);
    }

    @Test
    void isBlocked_forNewClient_returnsFalse() {
        assertFalse(rateLimitService.isBlocked("192.168.1.1"));
    }

    @Test
    void isBlocked_afterMaxAttempts_returnsTrue() {
        String clientId = "192.168.1.1";

        for (int i = 0; i < 5; i++) {
            rateLimitService.recordFailedAttempt(clientId);
        }

        assertTrue(rateLimitService.isBlocked(clientId));
    }

    @Test
    void isBlocked_afterLockoutPeriodExpires_returnsFalse() throws InterruptedException {
        String clientId = "192.168.1.1";

        // Set very short lockout for testing
        ReflectionTestUtils.setField(rateLimitService, "lockoutMinutes", 0);

        for (int i = 0; i < 5; i++) {
            rateLimitService.recordFailedAttempt(clientId);
        }

        // With 0 minutes lockout, the blocking period is in the past immediately
        // So isBlocked should return false after checking the expiration
        assertFalse(rateLimitService.isBlocked(clientId));
    }

    @Test
    void recordFailedAttempt_incrementsAttemptCounter() {
        String clientId = "192.168.1.1";

        assertEquals(5, rateLimitService.getRemainingAttempts(clientId));

        rateLimitService.recordFailedAttempt(clientId);
        assertEquals(4, rateLimitService.getRemainingAttempts(clientId));

        rateLimitService.recordFailedAttempt(clientId);
        assertEquals(3, rateLimitService.getRemainingAttempts(clientId));
    }

    @Test
    void recordFailedAttempt_afterMaxAttempts_setsBlockedUntil() {
        String clientId = "192.168.1.1";
        java.time.LocalDateTime beforeBlock = java.time.LocalDateTime.now();

        for (int i = 0; i < 5; i++) {
            rateLimitService.recordFailedAttempt(clientId);
        }

        java.time.LocalDateTime blockedUntil = rateLimitService.getBlockedUntil(clientId);
        java.time.LocalDateTime afterBlock = java.time.LocalDateTime.now();

        assertNotNull(blockedUntil);
        assertTrue(blockedUntil.isAfter(beforeBlock));
        assertTrue(blockedUntil.isBefore(afterBlock.plusMinutes(16))); // Within 15-16 minutes
    }

    @Test
    void recordSuccessfulAttempt_clearsAttemptCount() {
        String clientId = "192.168.1.1";

        rateLimitService.recordFailedAttempt(clientId);
        rateLimitService.recordFailedAttempt(clientId);
        assertEquals(3, rateLimitService.getRemainingAttempts(clientId));

        rateLimitService.recordSuccessfulAttempt(clientId);
        assertEquals(5, rateLimitService.getRemainingAttempts(clientId));
    }

    @Test
    void getRemainingAttempts_forNewClient_returnsMax() {
        assertEquals(5, rateLimitService.getRemainingAttempts("192.168.1.1"));
    }

    @Test
    void getRemainingAttempts_afterFailures_decrementsCorrectly() {
        String clientId = "192.168.1.1";

        rateLimitService.recordFailedAttempt(clientId);
        assertEquals(4, rateLimitService.getRemainingAttempts(clientId));

        rateLimitService.recordFailedAttempt(clientId);
        rateLimitService.recordFailedAttempt(clientId);
        assertEquals(2, rateLimitService.getRemainingAttempts(clientId));
    }

    @Test
    void getRemainingAttempts_whenBlocked_returnsZero() {
        String clientId = "192.168.1.1";

        for (int i = 0; i < 5; i++) {
            rateLimitService.recordFailedAttempt(clientId);
        }

        assertEquals(0, rateLimitService.getRemainingAttempts(clientId));
    }

    @Test
    void getBlockedUntil_forNewClient_returnsNull() {
        assertNull(rateLimitService.getBlockedUntil("192.168.1.1"));
    }

    @Test
    void getBlockedUntil_whenNotBlocked_returnsNull() {
        String clientId = "192.168.1.1";

        rateLimitService.recordFailedAttempt(clientId);
        rateLimitService.recordFailedAttempt(clientId);

        assertNull(rateLimitService.getBlockedUntil(clientId));
    }

    @Test
    void differentClients_haveIndependentCounters() {
        String client1 = "192.168.1.1";
        String client2 = "192.168.1.2";

        rateLimitService.recordFailedAttempt(client1);
        rateLimitService.recordFailedAttempt(client1);
        rateLimitService.recordFailedAttempt(client1);

        assertEquals(2, rateLimitService.getRemainingAttempts(client1));
        assertEquals(5, rateLimitService.getRemainingAttempts(client2));
    }

    @Test
    void blockingOneClient_doesNotAffectOthers() {
        String client1 = "192.168.1.1";
        String client2 = "192.168.1.2";

        for (int i = 0; i < 5; i++) {
            rateLimitService.recordFailedAttempt(client1);
        }

        assertTrue(rateLimitService.isBlocked(client1));
        assertFalse(rateLimitService.isBlocked(client2));
    }
}
