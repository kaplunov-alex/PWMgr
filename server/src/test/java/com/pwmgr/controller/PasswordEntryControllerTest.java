package com.pwmgr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pwmgr.dto.PasswordEntryRequest;
import com.pwmgr.dto.PasswordEntryResponse;
import com.pwmgr.service.PasswordEntryService.EntryNotFoundException;
import com.pwmgr.service.AuthService;
import com.pwmgr.service.PasswordEntryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PasswordEntryController.class)
@AutoConfigureMockMvc(addFilters = false)
class PasswordEntryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PasswordEntryService entryService;

    @MockBean
    private AuthService authService;

    private MockHttpSession session;
    private SecretKey mockKey;
    private PasswordEntryResponse sampleEntry;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        mockKey = mock(SecretKey.class);

        sampleEntry = new PasswordEntryResponse();
        sampleEntry.setId(1L);
        sampleEntry.setSiteName("github.com");
        sampleEntry.setUsername("testuser");
        sampleEntry.setPassword("decryptedPassword");
        sampleEntry.setNotes("Test notes");
        sampleEntry.setCreatedAt(LocalDateTime.now());
        sampleEntry.setUpdatedAt(LocalDateTime.now());

        when(authService.getSessionKey(session)).thenReturn(mockKey);
    }

    @Test
    void getAllEntries_whenAuthenticated_returnsAllEntries() throws Exception {
        List<PasswordEntryResponse> entries = Arrays.asList(sampleEntry);
        when(entryService.getAllEntries(mockKey)).thenReturn(entries);

        mockMvc.perform(get("/api/entries").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].siteName").value("github.com"))
                .andExpect(jsonPath("$.data[0].username").value("testuser"))
                .andExpect(jsonPath("$.data[0].password").value("decryptedPassword"));

        verify(entryService).getAllEntries(mockKey);
    }

    @Test
    void getAllEntries_whenNotAuthenticated_returnsUnauthorized() throws Exception {
        when(authService.getSessionKey(session)).thenReturn(null);

        mockMvc.perform(get("/api/entries").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Not authenticated"));

        verify(entryService, never()).getAllEntries(any());
    }

    @Test
    void getEntry_withValidId_returnsEntry() throws Exception {
        when(entryService.getEntry(1L, mockKey)).thenReturn(sampleEntry);

        mockMvc.perform(get("/api/entries/1").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.siteName").value("github.com"));

        verify(entryService).getEntry(1L, mockKey);
    }

    @Test
    void getEntry_withInvalidId_returnsNotFound() throws Exception {
        when(entryService.getEntry(999L, mockKey))
                .thenThrow(new EntryNotFoundException("Entry not found"));

        mockMvc.perform(get("/api/entries/999").session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Entry not found"));
    }

    @Test
    void createEntry_withValidData_createsEntry() throws Exception {
        PasswordEntryRequest request = new PasswordEntryRequest();
        request.setSiteName("example.com");
        request.setUsername("user@example.com");
        request.setPassword("password123");
        request.setNotes("My notes");

        when(entryService.createEntry(any(PasswordEntryRequest.class), eq(mockKey)))
                .thenReturn(sampleEntry);

        mockMvc.perform(post("/api/entries")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Entry created"));

        verify(entryService).createEntry(any(PasswordEntryRequest.class), eq(mockKey));
    }

    @Test
    void createEntry_withMissingRequiredFields_returnsBadRequest() throws Exception {
        PasswordEntryRequest request = new PasswordEntryRequest();
        request.setSiteName(""); // Invalid: empty
        request.setUsername("user");
        request.setPassword("pass");

        mockMvc.perform(post("/api/entries")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(entryService, never()).createEntry(any(), any());
    }

    @Test
    void updateEntry_withValidData_updatesEntry() throws Exception {
        PasswordEntryRequest request = new PasswordEntryRequest();
        request.setSiteName("updated.com");
        request.setUsername("newuser");
        request.setPassword("newpass");

        when(entryService.updateEntry(eq(1L), any(PasswordEntryRequest.class), eq(mockKey)))
                .thenReturn(sampleEntry);

        mockMvc.perform(put("/api/entries/1")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Entry updated"));

        verify(entryService).updateEntry(eq(1L), any(PasswordEntryRequest.class), eq(mockKey));
    }

    @Test
    void updateEntry_withInvalidId_returnsNotFound() throws Exception {
        PasswordEntryRequest request = new PasswordEntryRequest();
        request.setSiteName("test.com");
        request.setUsername("user");
        request.setPassword("pass");

        when(entryService.updateEntry(eq(999L), any(PasswordEntryRequest.class), eq(mockKey)))
                .thenThrow(new EntryNotFoundException("Entry not found"));

        mockMvc.perform(put("/api/entries/999")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void deleteEntry_withValidId_deletesEntry() throws Exception {
        doNothing().when(entryService).deleteEntry(1L);

        mockMvc.perform(delete("/api/entries/1").session(session).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Entry deleted"));

        verify(entryService).deleteEntry(1L);
    }

    @Test
    void deleteEntry_withInvalidId_returnsNotFound() throws Exception {
        doThrow(new EntryNotFoundException("Entry not found"))
                .when(entryService).deleteEntry(999L);

        mockMvc.perform(delete("/api/entries/999").session(session).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void searchEntries_withQuery_returnsMatchingEntries() throws Exception {
        List<PasswordEntryResponse> results = Arrays.asList(sampleEntry);
        when(entryService.searchEntries("github", mockKey)).thenReturn(results);

        mockMvc.perform(get("/api/entries/search")
                        .session(session)
                        .param("q", "github"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].siteName").value("github.com"));

        verify(entryService).searchEntries("github", mockKey);
    }

    @Test
    void searchEntries_withEmptyResults_returnsEmptyArray() throws Exception {
        when(entryService.searchEntries("nonexistent", mockKey))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/entries/search")
                        .session(session)
                        .param("q", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
