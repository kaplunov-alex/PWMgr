package com.pwmgr.service;

import com.pwmgr.model.MasterPassword;
import com.pwmgr.repository.MasterPasswordRepository;
import com.pwmgr.security.EncryptionService;
import com.pwmgr.security.RateLimitService;
import com.pwmgr.service.AuthService.RateLimitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MasterPasswordRepository masterPasswordRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private RateLimitService rateLimitService;

    @InjectMocks
    private AuthService authService;

    private MockHttpSession session;
    private SecretKey mockKey;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        mockKey = mock(SecretKey.class);
    }

    @Test
    void isSetupRequired_whenNoMasterPasswordExists_returnsTrue() {
        when(masterPasswordRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.empty());

        assertTrue(authService.isSetupRequired());
        verify(masterPasswordRepository).findFirstByOrderByIdAsc();
    }

    @Test
    void isSetupRequired_whenMasterPasswordExists_returnsFalse() {
        MasterPassword mp = new MasterPassword();
        when(masterPasswordRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.of(mp));

        assertFalse(authService.isSetupRequired());
    }

    @Test
    void setupMasterPassword_createsNewMasterPassword() throws Exception {
        String password = "SecurePassword123";
        String salt = "generatedSalt";
        String hash = "generatedHash";

        when(masterPasswordRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.empty());
        when(encryptionService.generateSalt()).thenReturn(salt);
        when(encryptionService.deriveVerificationHash(password, salt)).thenReturn(hash);

        authService.setupMasterPassword(password);

        verify(encryptionService).generateSalt();
        verify(encryptionService).deriveVerificationHash(password, salt);
        verify(masterPasswordRepository).save(argThat(mp ->
                mp.getSalt().equals(salt) &&
                mp.getVerificationHash().equals(hash)
        ));
    }

    @Test
    void setupMasterPassword_whenAlreadyConfigured_throwsException() {
        MasterPassword existing = new MasterPassword();
        when(masterPasswordRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class,
                () -> authService.setupMasterPassword("password"));

        verify(masterPasswordRepository, never()).save(any());
    }

    @Test
    void authenticate_withCorrectPassword_createsSessionKeyAndReturnsTrue() throws Exception {
        String password = "CorrectPassword";
        String clientId = "192.168.1.1";
        String salt = "salt123";
        String hash = "hash123";

        MasterPassword mp = new MasterPassword();
        mp.setSalt(salt);
        mp.setVerificationHash(hash);

        when(masterPasswordRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.of(mp));
        when(rateLimitService.isBlocked(clientId)).thenReturn(false);
        when(encryptionService.deriveVerificationHash(password, salt)).thenReturn(hash);
        when(encryptionService.deriveKey(password, salt)).thenReturn(mockKey);

        boolean result = authService.authenticate(password, clientId, session);

        assertTrue(result);
        assertEquals(mockKey, session.getAttribute("encryptionKey"));
        verify(rateLimitService).recordSuccessfulAttempt(clientId);
        verify(rateLimitService, never()).recordFailedAttempt(clientId);
    }

    @Test
    void authenticate_withIncorrectPassword_recordsFailureAndReturnsFalse() throws Exception {
        String password = "WrongPassword";
        String clientId = "192.168.1.1";
        String salt = "salt123";
        String correctHash = "correctHash";
        String wrongHash = "wrongHash";

        MasterPassword mp = new MasterPassword();
        mp.setSalt(salt);
        mp.setVerificationHash(correctHash);

        when(masterPasswordRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.of(mp));
        when(rateLimitService.isBlocked(clientId)).thenReturn(false);
        when(encryptionService.deriveVerificationHash(password, salt)).thenReturn(wrongHash);

        boolean result = authService.authenticate(password, clientId, session);

        assertFalse(result);
        assertNull(session.getAttribute("encryptionKey"));
        verify(rateLimitService).recordFailedAttempt(clientId);
        verify(rateLimitService, never()).recordSuccessfulAttempt(clientId);
        verify(encryptionService, never()).deriveKey(anyString(), anyString());
    }

    @Test
    void authenticate_whenBlocked_throwsRateLimitException() {
        String clientId = "192.168.1.1";

        when(rateLimitService.isBlocked(clientId)).thenReturn(true);

        assertThrows(RateLimitException.class,
                () -> authService.authenticate("password", clientId, session));

        verify(masterPasswordRepository, never()).findFirstByOrderByIdAsc();
    }

    @Test
    void logout_invalidatesSession() {
        session.setAttribute("encryptionKey", mockKey);
        session.setAttribute("otherAttribute", "value");

        authService.logout(session);

        assertTrue(session.isInvalid());
    }

    @Test
    void getSessionKey_whenAuthenticated_returnsKey() {
        session.setAttribute("encryptionKey", mockKey);

        SecretKey result = authService.getSessionKey(session);

        assertEquals(mockKey, result);
    }

    @Test
    void getSessionKey_whenNotAuthenticated_returnsNull() {
        SecretKey result = authService.getSessionKey(session);

        assertNull(result);
    }

    @Test
    void isAuthenticated_whenSessionHasKey_returnsTrue() {
        session.setAttribute("encryptionKey", mockKey);

        assertTrue(authService.isAuthenticated(session));
    }

    @Test
    void isAuthenticated_whenSessionHasNoKey_returnsFalse() {
        assertFalse(authService.isAuthenticated(session));
    }

    @Test
    void getRemainingAttempts_delegatesToRateLimitService() {
        String clientId = "192.168.1.1";
        when(rateLimitService.getRemainingAttempts(clientId)).thenReturn(3);

        int remaining = authService.getRemainingAttempts(clientId);

        assertEquals(3, remaining);
        verify(rateLimitService).getRemainingAttempts(clientId);
    }
}
