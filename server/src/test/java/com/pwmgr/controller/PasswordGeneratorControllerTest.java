package com.pwmgr.controller;

import com.pwmgr.security.EncryptionService;
import com.pwmgr.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@WebMvcTest(PasswordGeneratorController.class)
@AutoConfigureMockMvc(addFilters = false)
class PasswordGeneratorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private EncryptionService encryptionService;

    private MockHttpSession session;
    private SecretKey mockKey;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        mockKey = mock(SecretKey.class);
        when(authService.getSessionKey(session)).thenReturn(mockKey);
        when(authService.isAuthenticated(session)).thenReturn(true);
    }

    @Test
    void generatePassword_whenNotAuthenticated_returnsUnauthorized() throws Exception {
        when(authService.isAuthenticated(session)).thenReturn(false);

        mockMvc.perform(get("/api/generate").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Not authenticated"));

        verify(encryptionService, never()).generateSecurePassword(anyInt(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean());
    }

    @Test
    void generatePassword_withDefaultOptions_generatesPassword() throws Exception {
        when(encryptionService.generateSecurePassword(16, true, true, true, true))
                .thenReturn("GeneratedPass123!");

        mockMvc.perform(get("/api/generate").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.password").value("GeneratedPass123!"))
                .andExpect(jsonPath("$.data.strength.score").isNumber())
                .andExpect(jsonPath("$.data.strength.maxScore").value(7))
                .andExpect(jsonPath("$.data.strength.label").isNotEmpty());

        verify(encryptionService).generateSecurePassword(16, true, true, true, true);
    }

    @Test
    void generatePassword_withCustomLength_generatesCorrectLength() throws Exception {
        when(encryptionService.generateSecurePassword(32, true, true, true, true))
                .thenReturn("A".repeat(32));

        mockMvc.perform(get("/api/generate")
                        .session(session)
                        .param("length", "32"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.password").value("A".repeat(32)));

        verify(encryptionService).generateSecurePassword(32, true, true, true, true);
    }

    @Test
    void generatePassword_withLengthTooSmall_usesMinimum() throws Exception {
        mockMvc.perform(get("/api/generate")
                        .session(session)
                        .param("length", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Password length must be between 8 and 128"));

        verify(encryptionService, never()).generateSecurePassword(anyInt(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean());
    }

    @Test
    void generatePassword_withLengthTooLarge_usesMaximum() throws Exception {
        mockMvc.perform(get("/api/generate")
                        .session(session)
                        .param("length", "200"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Password length must be between 8 and 128"));

        verify(encryptionService, never()).generateSecurePassword(anyInt(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean());
    }

    @Test
    void generatePassword_withOnlyUppercase_respectsOption() throws Exception {
        when(encryptionService.generateSecurePassword(16, true, false, false, false))
                .thenReturn("ABCDEFGHIJKLMNOP");

        mockMvc.perform(get("/api/generate")
                        .session(session)
                        .param("uppercase", "true")
                        .param("lowercase", "false")
                        .param("numbers", "false")
                        .param("special", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(encryptionService).generateSecurePassword(16, true, false, false, false);
    }

    @Test
    void generatePassword_withAllOptionsFalse_fallsBackToDefault() throws Exception {
        when(encryptionService.generateSecurePassword(16, false, false, false, false))
                .thenReturn("16CharPassword!!");

        mockMvc.perform(get("/api/generate")
                        .session(session)
                        .param("uppercase", "false")
                        .param("lowercase", "false")
                        .param("numbers", "false")
                        .param("special", "false"))
                .andExpect(status().isOk());

        verify(encryptionService).generateSecurePassword(16, false, false, false, false);
    }

    @Test
    void generatePassword_calculatesStrengthCorrectly_weak() throws Exception {
        when(encryptionService.generateSecurePassword(anyInt(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean()))
                .thenReturn("abc");

        mockMvc.perform(get("/api/generate")
                        .session(session)
                        .param("length", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.strength.label").value("Weak"));
    }

    @Test
    void generatePassword_calculatesStrengthCorrectly_veryStrong() throws Exception {
        when(encryptionService.generateSecurePassword(anyInt(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean()))
                .thenReturn("Abc123!@#$DefGhi456");

        mockMvc.perform(get("/api/generate").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.strength.label").value("Very Strong"))
                .andExpect(jsonPath("$.data.strength.score").value(7));
    }
}
