package com.pwmgr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pwmgr.dto.LoginRequest;
import com.pwmgr.dto.SetupRequest;
import com.pwmgr.service.AuthService.RateLimitException;
import com.pwmgr.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
    }

    @Test
    void getStatus_whenSetupRequired_returnsSetupRequiredTrue() throws Exception {
        when(authService.isSetupRequired()).thenReturn(true);
        when(authService.isAuthenticated(any())).thenReturn(false);

        mockMvc.perform(get("/api/auth/status").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.setupRequired").value(true))
                .andExpect(jsonPath("$.data.authenticated").value(false));

        verify(authService).isSetupRequired();
        verify(authService).isAuthenticated(any());
    }

    @Test
    void getStatus_whenAuthenticatedWithValidSession_returnsAuthenticatedTrue() throws Exception {
        when(authService.isSetupRequired()).thenReturn(false);
        when(authService.isAuthenticated(session)).thenReturn(true);

        mockMvc.perform(get("/api/auth/status").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.setupRequired").value(false))
                .andExpect(jsonPath("$.data.authenticated").value(true));
    }

    @Test
    void setup_withValidPassword_createsMasterPassword() throws Exception {
        SetupRequest request = new SetupRequest();
        request.setMasterPassword("SecurePass123");

        when(authService.isSetupRequired()).thenReturn(true);
        doNothing().when(authService).setupMasterPassword(anyString());

        mockMvc.perform(post("/api/auth/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Master password configured successfully"));

        verify(authService).setupMasterPassword("SecurePass123");
    }

    @Test
    void setup_withPasswordTooShort_returnsBadRequest() throws Exception {
        SetupRequest request = new SetupRequest();
        request.setMasterPassword("short");

        mockMvc.perform(post("/api/auth/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(authService, never()).setupMasterPassword(anyString());
    }

    @Test
    void setup_whenAlreadyConfigured_returnsError() throws Exception {
        SetupRequest request = new SetupRequest();
        request.setMasterPassword("SecurePass123");

        when(authService.isSetupRequired()).thenReturn(false);

        mockMvc.perform(post("/api/auth/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Master password already configured"));

        verify(authService, never()).setupMasterPassword(anyString());
    }

    @Test
    void login_withCorrectPassword_returnsSuccess() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setMasterPassword("CorrectPassword");

        when(authService.authenticate(eq("CorrectPassword"), anyString(), eq(session)))
                .thenReturn(true);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    void login_withIncorrectPassword_returnsUnauthorized() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setMasterPassword("WrongPassword");

        when(authService.authenticate(eq("WrongPassword"), anyString(), eq(session)))
                .thenReturn(false);
        when(authService.getRemainingAttempts(anyString())).thenReturn(4);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .session(session)
                        .header("X-Forwarded-For", "192.168.1.1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid master password"));
    }

    @Test
    void login_whenRateLimited_returnsTooManyRequests() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setMasterPassword("AnyPassword");

        when(authService.authenticate(eq("AnyPassword"), anyString(), eq(session)))
                .thenThrow(new RateLimitException("Too many failed attempts. Try again in 15 minutes"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Too many failed attempts. Try again in 15 minutes"));
    }

    @Test
    void login_extractsClientIdFromXForwardedForHeader() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setMasterPassword("TestPassword");

        when(authService.authenticate(eq("TestPassword"), eq("10.0.0.1"), eq(session)))
                .thenReturn(true);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .session(session)
                        .header("X-Forwarded-For", "10.0.0.1"))
                .andExpect(status().isOk());

        verify(authService).authenticate(eq("TestPassword"), eq("10.0.0.1"), eq(session));
    }

    @Test
    void login_whenNoXForwardedForHeader_usesRemoteAddr() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setMasterPassword("TestPassword");

        when(authService.authenticate(anyString(), anyString(), eq(session)))
                .thenReturn(true);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(authService).authenticate(eq("TestPassword"), anyString(), eq(session));
    }

    @Test
    void logout_invalidatesSession() throws Exception {
        doNothing().when(authService).logout(session);

        mockMvc.perform(post("/api/auth/logout").session(session).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        verify(authService).logout(session);
    }
}
