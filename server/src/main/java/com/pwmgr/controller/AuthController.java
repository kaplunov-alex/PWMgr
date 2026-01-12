package com.pwmgr.controller;

import com.pwmgr.dto.ApiResponse;
import com.pwmgr.dto.LoginRequest;
import com.pwmgr.dto.SetupRequest;
import com.pwmgr.service.AuthService;
import com.pwmgr.service.AuthService.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus(HttpSession session) {
        Map<String, Object> status = new HashMap<>();
        status.put("setupRequired", authService.isSetupRequired());
        status.put("authenticated", authService.isAuthenticated(session));

        return ResponseEntity.ok(ApiResponse.success("Status retrieved", status));
    }

    @PostMapping("/setup")
    public ResponseEntity<ApiResponse<Void>> setup(@Valid @RequestBody SetupRequest request) {
        try {
            if (!authService.isSetupRequired()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Master password already configured"));
            }

            authService.setupMasterPassword(request.getMasterPassword());
            return ResponseEntity.ok(ApiResponse.success("Master password configured successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to setup master password: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpSession session) {
        try {
            String clientId = getClientId(httpRequest);

            boolean authenticated = authService.authenticate(
                    request.getMasterPassword(), clientId, session);

            Map<String, Object> data = new HashMap<>();

            if (authenticated) {
                data.put("authenticated", true);
                return ResponseEntity.ok(ApiResponse.success("Login successful", data));
            } else {
                data.put("authenticated", false);
                data.put("remainingAttempts", authService.getRemainingAttempts(clientId));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Invalid master password"));
            }
        } catch (RateLimitException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Authentication failed: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpSession session) {
        authService.logout(session);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    private String getClientId(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
