package com.pwmgr.service;

import com.pwmgr.model.MasterPassword;
import com.pwmgr.repository.MasterPasswordRepository;
import com.pwmgr.security.EncryptionService;
import com.pwmgr.security.RateLimitService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Optional;

@Service
public class AuthService {

    private static final String SESSION_KEY_ATTR = "encryptionKey";
    private static final String SESSION_SALT_ATTR = "masterSalt";

    private final MasterPasswordRepository masterPasswordRepository;
    private final EncryptionService encryptionService;
    private final RateLimitService rateLimitService;

    public AuthService(MasterPasswordRepository masterPasswordRepository,
                       EncryptionService encryptionService,
                       RateLimitService rateLimitService) {
        this.masterPasswordRepository = masterPasswordRepository;
        this.encryptionService = encryptionService;
        this.rateLimitService = rateLimitService;
    }

    public boolean isSetupRequired() {
        return masterPasswordRepository.findFirstByOrderByIdAsc().isEmpty();
    }

    public void setupMasterPassword(String masterPassword) throws Exception {
        if (!isSetupRequired()) {
            throw new IllegalStateException("Master password already configured");
        }

        String salt = encryptionService.generateSalt();
        String verificationHash = encryptionService.deriveVerificationHash(masterPassword, salt);

        MasterPassword mp = new MasterPassword(verificationHash, salt);
        masterPasswordRepository.save(mp);
    }

    public boolean authenticate(String masterPassword, String clientId, HttpSession session) throws Exception {
        if (rateLimitService.isBlocked(clientId)) {
            throw new RateLimitException("Too many failed attempts. Please try again later.");
        }

        Optional<MasterPassword> mpOpt = masterPasswordRepository.findFirstByOrderByIdAsc();
        if (mpOpt.isEmpty()) {
            throw new IllegalStateException("Master password not configured");
        }

        MasterPassword mp = mpOpt.get();
        String verificationHash = encryptionService.deriveVerificationHash(masterPassword, mp.getSalt());

        if (!verificationHash.equals(mp.getVerificationHash())) {
            rateLimitService.recordFailedAttempt(clientId);
            return false;
        }

        rateLimitService.recordSuccessfulAttempt(clientId);

        SecretKey key = encryptionService.deriveKey(masterPassword, mp.getSalt());
        session.setAttribute(SESSION_KEY_ATTR, key);
        session.setAttribute(SESSION_SALT_ATTR, mp.getSalt());

        return true;
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

    public SecretKey getSessionKey(HttpSession session) {
        return (SecretKey) session.getAttribute(SESSION_KEY_ATTR);
    }

    public boolean isAuthenticated(HttpSession session) {
        return session.getAttribute(SESSION_KEY_ATTR) != null;
    }

    public int getRemainingAttempts(String clientId) {
        return rateLimitService.getRemainingAttempts(clientId);
    }

    public static class RateLimitException extends RuntimeException {
        public RateLimitException(String message) {
            super(message);
        }
    }
}
