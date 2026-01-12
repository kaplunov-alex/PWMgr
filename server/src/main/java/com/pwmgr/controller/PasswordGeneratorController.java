package com.pwmgr.controller;

import com.pwmgr.dto.ApiResponse;
import com.pwmgr.security.EncryptionService;
import com.pwmgr.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/generate")
public class PasswordGeneratorController {

    private final EncryptionService encryptionService;
    private final AuthService authService;

    public PasswordGeneratorController(EncryptionService encryptionService, AuthService authService) {
        this.encryptionService = encryptionService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> generatePassword(
            @RequestParam(defaultValue = "16") int length,
            @RequestParam(defaultValue = "true") boolean uppercase,
            @RequestParam(defaultValue = "true") boolean lowercase,
            @RequestParam(defaultValue = "true") boolean numbers,
            @RequestParam(defaultValue = "true") boolean special,
            HttpSession session) {

        if (!authService.isAuthenticated(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        if (length < 8 || length > 128) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Password length must be between 8 and 128"));
        }

        String password = encryptionService.generateSecurePassword(
                length, uppercase, lowercase, numbers, special);

        Map<String, Object> data = new HashMap<>();
        data.put("password", password);
        data.put("strength", calculateStrength(password));

        return ResponseEntity.ok(ApiResponse.success("Password generated", data));
    }

    private Map<String, Object> calculateStrength(String password) {
        Map<String, Object> strength = new HashMap<>();

        int score = 0;
        StringBuilder feedback = new StringBuilder();

        if (password.length() >= 8) score += 1;
        if (password.length() >= 12) score += 1;
        if (password.length() >= 16) score += 1;

        if (password.matches(".*[a-z].*")) score += 1;
        if (password.matches(".*[A-Z].*")) score += 1;
        if (password.matches(".*\\d.*")) score += 1;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?].*")) score += 1;

        String label;
        if (score <= 2) {
            label = "Weak";
        } else if (score <= 4) {
            label = "Fair";
        } else if (score <= 6) {
            label = "Strong";
        } else {
            label = "Very Strong";
        }

        strength.put("score", score);
        strength.put("maxScore", 7);
        strength.put("label", label);

        return strength;
    }
}
