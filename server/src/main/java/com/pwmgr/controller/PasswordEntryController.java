package com.pwmgr.controller;

import com.pwmgr.dto.ApiResponse;
import com.pwmgr.dto.PasswordEntryRequest;
import com.pwmgr.dto.PasswordEntryResponse;
import com.pwmgr.service.AuthService;
import com.pwmgr.service.PasswordEntryService;
import com.pwmgr.service.PasswordEntryService.EntryNotFoundException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.util.List;

@RestController
@RequestMapping("/api/entries")
public class PasswordEntryController {

    private final PasswordEntryService passwordEntryService;
    private final AuthService authService;

    public PasswordEntryController(PasswordEntryService passwordEntryService, AuthService authService) {
        this.passwordEntryService = passwordEntryService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PasswordEntryResponse>>> getAllEntries(HttpSession session) {
        SecretKey key = authService.getSessionKey(session);
        if (key == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        try {
            List<PasswordEntryResponse> entries = passwordEntryService.getAllEntries(key);
            return ResponseEntity.ok(ApiResponse.success("Entries retrieved", entries));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve entries: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PasswordEntryResponse>> getEntry(
            @PathVariable Long id, HttpSession session) {
        SecretKey key = authService.getSessionKey(session);
        if (key == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        try {
            PasswordEntryResponse entry = passwordEntryService.getEntry(id, key);
            return ResponseEntity.ok(ApiResponse.success("Entry retrieved", entry));
        } catch (EntryNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve entry: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PasswordEntryResponse>> createEntry(
            @Valid @RequestBody PasswordEntryRequest request, HttpSession session) {
        SecretKey key = authService.getSessionKey(session);
        if (key == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        try {
            PasswordEntryResponse entry = passwordEntryService.createEntry(request, key);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Entry created", entry));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create entry: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PasswordEntryResponse>> updateEntry(
            @PathVariable Long id,
            @Valid @RequestBody PasswordEntryRequest request,
            HttpSession session) {
        SecretKey key = authService.getSessionKey(session);
        if (key == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        try {
            PasswordEntryResponse entry = passwordEntryService.updateEntry(id, request, key);
            return ResponseEntity.ok(ApiResponse.success("Entry updated", entry));
        } catch (EntryNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update entry: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEntry(
            @PathVariable Long id, HttpSession session) {
        SecretKey key = authService.getSessionKey(session);
        if (key == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        try {
            passwordEntryService.deleteEntry(id);
            return ResponseEntity.ok(ApiResponse.success("Entry deleted"));
        } catch (EntryNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete entry: " + e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PasswordEntryResponse>>> searchEntries(
            @RequestParam String q, HttpSession session) {
        SecretKey key = authService.getSessionKey(session);
        if (key == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        try {
            List<PasswordEntryResponse> entries = passwordEntryService.searchEntries(q, key);
            return ResponseEntity.ok(ApiResponse.success("Search completed", entries));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Search failed: " + e.getMessage()));
        }
    }
}
