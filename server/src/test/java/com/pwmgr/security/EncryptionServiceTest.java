package com.pwmgr.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
    }

    @Test
    void generateSalt_produces32ByteBase64String() {
        String salt = encryptionService.generateSalt();

        assertNotNull(salt);
        byte[] saltBytes = Base64.getDecoder().decode(salt);
        assertEquals(32, saltBytes.length, "Salt should be 32 bytes");
    }

    @Test
    void generateSalt_producesUniqueSalts() {
        Set<String> salts = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            salts.add(encryptionService.generateSalt());
        }

        assertEquals(100, salts.size(), "All generated salts should be unique");
    }

    @Test
    void generateIv_produces12ByteBase64String() {
        String iv = encryptionService.generateIv();

        assertNotNull(iv);
        byte[] ivBytes = Base64.getDecoder().decode(iv);
        assertEquals(12, ivBytes.length, "IV should be 12 bytes for GCM");
    }

    @Test
    void generateIv_producesUniqueIVs() {
        Set<String> ivs = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            ivs.add(encryptionService.generateIv());
        }

        assertEquals(100, ivs.size(), "All generated IVs should be unique");
    }

    @Test
    void deriveKey_producesDeterministicKey() throws Exception {
        String password = "TestPassword123";
        String salt = encryptionService.generateSalt();

        SecretKey key1 = encryptionService.deriveKey(password, salt);
        SecretKey key2 = encryptionService.deriveKey(password, salt);

        assertArrayEquals(key1.getEncoded(), key2.getEncoded(),
                "Same password and salt should produce same key");
    }

    @Test
    void deriveKey_produces256BitKey() throws Exception {
        String password = "TestPassword123";
        String salt = encryptionService.generateSalt();

        SecretKey key = encryptionService.deriveKey(password, salt);

        assertEquals("AES", key.getAlgorithm());
        assertEquals(32, key.getEncoded().length, "Key should be 256 bits (32 bytes)");
    }

    @Test
    void deriveKey_differentSaltsProduceDifferentKeys() throws Exception {
        String password = "TestPassword123";
        String salt1 = encryptionService.generateSalt();
        String salt2 = encryptionService.generateSalt();

        SecretKey key1 = encryptionService.deriveKey(password, salt1);
        SecretKey key2 = encryptionService.deriveKey(password, salt2);

        assertFalse(
                java.util.Arrays.equals(key1.getEncoded(), key2.getEncoded()),
                "Different salts should produce different keys"
        );
    }

    @Test
    void deriveVerificationHash_isDeterministic() throws Exception {
        String password = "TestPassword123";
        String salt = encryptionService.generateSalt();

        String hash1 = encryptionService.deriveVerificationHash(password, salt);
        String hash2 = encryptionService.deriveVerificationHash(password, salt);

        assertEquals(hash1, hash2, "Same password and salt should produce same hash");
    }

    @Test
    void deriveVerificationHash_differentPasswordsProduceDifferentHashes() throws Exception {
        String salt = encryptionService.generateSalt();

        String hash1 = encryptionService.deriveVerificationHash("Password1", salt);
        String hash2 = encryptionService.deriveVerificationHash("Password2", salt);

        assertNotEquals(hash1, hash2, "Different passwords should produce different hashes");
    }

    @Test
    void encrypt_decrypt_roundtripPreservesData() throws Exception {
        String originalText = "This is a secret password!";
        String password = "MasterPassword123";
        String salt = encryptionService.generateSalt();

        SecretKey key = encryptionService.deriveKey(password, salt);

        String encrypted = encryptionService.encrypt(originalText, key);
        String decrypted = encryptionService.decrypt(encrypted, key);

        assertEquals(originalText, decrypted, "Decrypted text should match original");
    }

    @Test
    void encrypt_producesUniqueOutputsWithSameInput() throws Exception {
        String text = "Secret";
        String password = "Password";
        String salt = encryptionService.generateSalt();
        SecretKey key = encryptionService.deriveKey(password, salt);

        String encrypted1 = encryptionService.encrypt(text, key);
        String encrypted2 = encryptionService.encrypt(text, key);

        assertNotEquals(encrypted1, encrypted2,
                "Same input should produce different ciphertext due to unique IV");
    }

    @Test
    void encrypt_outputContainsIvAndCiphertext() throws Exception {
        String text = "Secret";
        String password = "Password";
        String salt = encryptionService.generateSalt();
        SecretKey key = encryptionService.deriveKey(password, salt);

        String encrypted = encryptionService.encrypt(text, key);

        assertTrue(encrypted.contains(":"), "Encrypted text should contain IV separator");
        String[] parts = encrypted.split(":");
        assertEquals(2, parts.length, "Should have IV and ciphertext parts");

        // Verify IV part is valid base64
        assertDoesNotThrow(() -> {
            try {
                Base64.getDecoder().decode(parts[0]);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        // Verify ciphertext part is valid base64
        assertDoesNotThrow(() -> {
            try {
                Base64.getDecoder().decode(parts[1]);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void decrypt_withWrongKey_throwsException() throws Exception {
        String text = "Secret";
        String salt1 = encryptionService.generateSalt();
        String salt2 = encryptionService.generateSalt();

        SecretKey key1 = encryptionService.deriveKey("Password1", salt1);
        SecretKey key2 = encryptionService.deriveKey("Password2", salt2);

        String encrypted = encryptionService.encrypt(text, key1);

        assertThrows(Exception.class, () -> encryptionService.decrypt(encrypted, key2),
                "Decryption with wrong key should fail");
    }

    @Test
    void decrypt_withTamperedCiphertext_throwsException() throws Exception {
        String text = "Secret";
        String password = "Password";
        String salt = encryptionService.generateSalt();
        SecretKey key = encryptionService.deriveKey(password, salt);

        String encrypted = encryptionService.encrypt(text, key);
        String tampered = encrypted.substring(0, encrypted.length() - 1) + "X";

        assertThrows(Exception.class, () -> encryptionService.decrypt(tampered, key),
                "Decryption of tampered data should fail (GCM authentication)");
    }

    @Test
    void encryptWithIv_decryptWithIv_roundtripPreservesData() throws Exception {
        String originalText = "Confidential information";
        String password = "SecurePassword";
        String salt = encryptionService.generateSalt();
        SecretKey key = encryptionService.deriveKey(password, salt);

        EncryptionService.EncryptedData encrypted = encryptionService.encryptWithIv(originalText, key);
        String iv = encrypted.getIv();
        String ciphertext = encrypted.getCiphertext();

        String decrypted = encryptionService.decryptWithIv(ciphertext, iv, key);

        assertEquals(originalText, decrypted, "Decrypted text should match original");
    }

    @Test
    void encryptWithIv_returnsIvAndCiphertext() throws Exception {
        String text = "Secret";
        String password = "Password";
        String salt = encryptionService.generateSalt();
        SecretKey key = encryptionService.deriveKey(password, salt);

        EncryptionService.EncryptedData result = encryptionService.encryptWithIv(text, key);

        assertNotNull(result, "Should return EncryptedData object");
        assertNotNull(result.getIv(), "IV should not be null");
        assertNotNull(result.getCiphertext(), "Ciphertext should not be null");

        // Verify both are valid base64
        assertDoesNotThrow(() -> {
            try {
                Base64.getDecoder().decode(result.getIv());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        assertDoesNotThrow(() -> {
            try {
                Base64.getDecoder().decode(result.getCiphertext());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void generateSecurePassword_respectsLength() {
        int[] lengths = {8, 12, 16, 24, 32, 64};

        for (int length : lengths) {
            String password = encryptionService.generateSecurePassword(
                    length, true, true, true, true);

            assertEquals(length, password.length(),
                    "Generated password should have requested length: " + length);
        }
    }

    @Test
    void generateSecurePassword_includesUppercase() {
        String password = encryptionService.generateSecurePassword(
                20, true, false, false, false);

        assertTrue(password.matches(".*[A-Z].*"),
                "Password should contain uppercase letters");
        assertFalse(password.matches(".*[a-z].*"),
                "Password should not contain lowercase when disabled");
    }

    @Test
    void generateSecurePassword_includesLowercase() {
        String password = encryptionService.generateSecurePassword(
                20, false, true, false, false);

        assertTrue(password.matches(".*[a-z].*"),
                "Password should contain lowercase letters");
    }

    @Test
    void generateSecurePassword_includesNumbers() {
        String password = encryptionService.generateSecurePassword(
                20, false, false, true, false);

        assertTrue(password.matches(".*[0-9].*"),
                "Password should contain numbers");
    }

    @Test
    void generateSecurePassword_includesSpecialCharacters() {
        String password = encryptionService.generateSecurePassword(
                20, false, false, false, true);

        assertTrue(password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?].*"),
                "Password should contain special characters");
    }

    @Test
    void generateSecurePassword_includesAllRequestedCharacterTypes() {
        String password = encryptionService.generateSecurePassword(
                50, true, true, true, true);

        assertTrue(password.matches(".*[A-Z].*"), "Should include uppercase");
        assertTrue(password.matches(".*[a-z].*"), "Should include lowercase");
        assertTrue(password.matches(".*[0-9].*"), "Should include numbers");
        assertTrue(password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?].*"),
                "Should include special characters");
    }

    @RepeatedTest(10)
    void generateSecurePassword_producesUniquePasswords() {
        Set<String> passwords = new HashSet<>();

        for (int i = 0; i < 100; i++) {
            passwords.add(encryptionService.generateSecurePassword(
                    16, true, true, true, true));
        }

        assertTrue(passwords.size() > 95,
                "Generated passwords should be highly unique");
    }

    @Test
    void generateSecurePassword_withNoOptionsEnabled_stillGeneratesPassword() {
        // Should fall back to some default when all options are false
        String password = encryptionService.generateSecurePassword(
                16, false, false, false, false);

        assertNotNull(password);
        assertEquals(16, password.length());
    }
}
