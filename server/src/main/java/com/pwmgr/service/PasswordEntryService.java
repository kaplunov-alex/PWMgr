package com.pwmgr.service;

import com.pwmgr.dto.PasswordEntryRequest;
import com.pwmgr.dto.PasswordEntryResponse;
import com.pwmgr.model.PasswordEntry;
import com.pwmgr.repository.PasswordEntryRepository;
import com.pwmgr.security.EncryptionService;
import com.pwmgr.security.EncryptionService.EncryptedData;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PasswordEntryService {

    private final PasswordEntryRepository passwordEntryRepository;
    private final EncryptionService encryptionService;

    public PasswordEntryService(PasswordEntryRepository passwordEntryRepository,
                                EncryptionService encryptionService) {
        this.passwordEntryRepository = passwordEntryRepository;
        this.encryptionService = encryptionService;
    }

    public PasswordEntryResponse createEntry(PasswordEntryRequest request, SecretKey key) throws Exception {
        PasswordEntry entry = new PasswordEntry();
        entry.setSiteName(request.getSiteName());
        entry.setUsername(request.getUsername());

        EncryptedData encryptedPassword = encryptionService.encryptWithIv(request.getPassword(), key);
        entry.setEncryptedPassword(encryptedPassword.getCiphertext());
        entry.setIv(encryptedPassword.getIv());

        if (request.getNotes() != null && !request.getNotes().isEmpty()) {
            EncryptedData encryptedNotes = encryptionService.encryptWithIv(request.getNotes(), key);
            entry.setEncryptedNotes(encryptedNotes.getCiphertext());
            entry.setNotesIv(encryptedNotes.getIv());
        }

        PasswordEntry saved = passwordEntryRepository.save(entry);
        return toResponse(saved, key);
    }

    public PasswordEntryResponse updateEntry(Long id, PasswordEntryRequest request, SecretKey key) throws Exception {
        Optional<PasswordEntry> entryOpt = passwordEntryRepository.findById(id);
        if (entryOpt.isEmpty()) {
            throw new EntryNotFoundException("Password entry not found");
        }

        PasswordEntry entry = entryOpt.get();
        entry.setSiteName(request.getSiteName());
        entry.setUsername(request.getUsername());

        EncryptedData encryptedPassword = encryptionService.encryptWithIv(request.getPassword(), key);
        entry.setEncryptedPassword(encryptedPassword.getCiphertext());
        entry.setIv(encryptedPassword.getIv());

        if (request.getNotes() != null && !request.getNotes().isEmpty()) {
            EncryptedData encryptedNotes = encryptionService.encryptWithIv(request.getNotes(), key);
            entry.setEncryptedNotes(encryptedNotes.getCiphertext());
            entry.setNotesIv(encryptedNotes.getIv());
        } else {
            entry.setEncryptedNotes(null);
            entry.setNotesIv(null);
        }

        PasswordEntry saved = passwordEntryRepository.save(entry);
        return toResponse(saved, key);
    }

    public void deleteEntry(Long id) {
        if (!passwordEntryRepository.existsById(id)) {
            throw new EntryNotFoundException("Password entry not found");
        }
        passwordEntryRepository.deleteById(id);
    }

    public PasswordEntryResponse getEntry(Long id, SecretKey key) throws Exception {
        Optional<PasswordEntry> entryOpt = passwordEntryRepository.findById(id);
        if (entryOpt.isEmpty()) {
            throw new EntryNotFoundException("Password entry not found");
        }
        return toResponse(entryOpt.get(), key);
    }

    public List<PasswordEntryResponse> getAllEntries(SecretKey key) throws Exception {
        List<PasswordEntry> entries = passwordEntryRepository.findAllByOrderBySiteNameAsc();
        return entries.stream()
                .map(entry -> {
                    try {
                        return toResponse(entry, key);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to decrypt entry", e);
                    }
                })
                .collect(Collectors.toList());
    }

    public List<PasswordEntryResponse> searchEntries(String query, SecretKey key) throws Exception {
        List<PasswordEntry> entries = passwordEntryRepository.searchByQuery(query);
        return entries.stream()
                .map(entry -> {
                    try {
                        return toResponse(entry, key);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to decrypt entry", e);
                    }
                })
                .collect(Collectors.toList());
    }

    private PasswordEntryResponse toResponse(PasswordEntry entry, SecretKey key) throws Exception {
        String decryptedPassword = encryptionService.decryptWithIv(
                entry.getEncryptedPassword(), entry.getIv(), key);

        String decryptedNotes = null;
        if (entry.getEncryptedNotes() != null && entry.getNotesIv() != null) {
            decryptedNotes = encryptionService.decryptWithIv(
                    entry.getEncryptedNotes(), entry.getNotesIv(), key);
        }

        return new PasswordEntryResponse(
                entry.getId(),
                entry.getSiteName(),
                entry.getUsername(),
                decryptedPassword,
                decryptedNotes,
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }

    public static class EntryNotFoundException extends RuntimeException {
        public EntryNotFoundException(String message) {
            super(message);
        }
    }
}
