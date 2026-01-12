package com.pwmgr.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "Master password is required")
    private String masterPassword;

    public LoginRequest() {}

    public LoginRequest(String masterPassword) {
        this.masterPassword = masterPassword;
    }

    public String getMasterPassword() {
        return masterPassword;
    }

    public void setMasterPassword(String masterPassword) {
        this.masterPassword = masterPassword;
    }
}
