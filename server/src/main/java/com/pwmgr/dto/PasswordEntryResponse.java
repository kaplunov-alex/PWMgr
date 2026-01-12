package com.pwmgr.dto;

import java.time.LocalDateTime;

public class PasswordEntryResponse {

    private Long id;
    private String siteName;
    private String username;
    private String password;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PasswordEntryResponse() {}

    public PasswordEntryResponse(Long id, String siteName, String username, String password,
                                  String notes, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.siteName = siteName;
        this.username = username;
        this.password = password;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
