package com.pwmgr.service;

import com.pwmgr.dto.PasswordEntryRequest;
import com.pwmgr.dto.PasswordEntryResponse;
import com.pwmgr.model.PasswordEntry;
import com.pwmgr.repository.PasswordEntryRepository;
import com.pwmgr.security.EncryptionService;
import com.pwmgr.security.EncryptionService.EncryptedData;
import com.pwmgr.service.PasswordEntryService.EntryNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordEntryServiceTest {

    @Mock
    private PasswordEntryRepository entryRepository;

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private PasswordEntryService entryService;

    private SecretKey mockKey;
    private PasswordEntry sampleEntry;

    @BeforeEach
    void setUp() {
        mockKey = mock(SecretKey.class);

        sampleEntry = new PasswordEntry();
        sampleEntry.setId(1L);
        sampleEntry.setSiteName("github.com");
        sampleEntry.setUsername("testuser");
        sampleEntry.setEncryptedPassword("encryptedPass");
        sampleEntry.setIv("iv123");
        sampleEntry.setEncryptedNotes("encryptedNotes");
        sampleEntry.setNotesIv("notesIv123");
    }

    @Test
    void createEntry_encryptsPasswordAndSaves() throws Exception {
        PasswordEntryRequest request = new PasswordEntryRequest();
        request.setSiteName("example.com");
        request.setUsername("user@example.com");
        request.setPassword("plainPassword");
        request.setNotes("My notes");

        EncryptedData encryptedPassword = new EncryptedData("ciphertext1", "iv1");
        EncryptedData encryptedNotes = new EncryptedData("ciphertext2", "iv2");

        when(encryptionService.encryptWithIv("plainPassword", mockKey))
                .thenReturn(encryptedPassword);
        when(encryptionService.encryptWithIv("My notes", mockKey))
                .thenReturn(encryptedNotes);
        when(entryRepository.save(any(PasswordEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(encryptionService.decryptWithIv("ciphertext1", "iv1", mockKey))
                .thenReturn("plainPassword");
        when(encryptionService.decryptWithIv("ciphertext2", "iv2", mockKey))
                .thenReturn("My notes");

        PasswordEntryResponse result = entryService.createEntry(request, mockKey);

        assertNotNull(result);
        assertEquals("example.com", result.getSiteName());
        assertEquals("user@example.com", result.getUsername());

        verify(encryptionService).encryptWithIv("plainPassword", mockKey);
        verify(encryptionService).encryptWithIv("My notes", mockKey);
        verify(entryRepository).save(argThat(entry ->
                entry.getSiteName().equals("example.com") &&
                entry.getUsername().equals("user@example.com") &&
                entry.getEncryptedPassword().equals("ciphertext1") &&
                entry.getIv().equals("iv1") &&
                entry.getEncryptedNotes().equals("ciphertext2") &&
                entry.getNotesIv().equals("iv2")
        ));
    }

    @Test
    void createEntry_withNullNotes_doesNotEncryptNotes() throws Exception {
        PasswordEntryRequest request = new PasswordEntryRequest();
        request.setSiteName("example.com");
        request.setUsername("user");
        request.setPassword("password");
        request.setNotes(null);

        EncryptedData encryptedPassword = new EncryptedData("ciphertext1", "iv1");

        when(encryptionService.encryptWithIv("password", mockKey))
                .thenReturn(encryptedPassword);
        when(entryRepository.save(any(PasswordEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(encryptionService.decryptWithIv("ciphertext1", "iv1", mockKey))
                .thenReturn("password");

        entryService.createEntry(request, mockKey);

        verify(encryptionService).encryptWithIv("password", mockKey);
        verify(encryptionService, times(1)).encryptWithIv(anyString(), any());
        verify(entryRepository).save(argThat(entry ->
                entry.getEncryptedNotes() == null &&
                entry.getNotesIv() == null
        ));
    }

    @Test
    void updateEntry_updatesAndReEncryptsPassword() throws Exception {
        PasswordEntryRequest request = new PasswordEntryRequest();
        request.setSiteName("updated.com");
        request.setUsername("newuser");
        request.setPassword("newpassword");
        request.setNotes("new notes");

        EncryptedData newEncryptedPassword = new EncryptedData("newCiphertext1", "newIv1");
        EncryptedData newEncryptedNotes = new EncryptedData("newCiphertext2", "newIv2");

        when(entryRepository.findById(1L)).thenReturn(Optional.of(sampleEntry));
        when(encryptionService.encryptWithIv("newpassword", mockKey))
                .thenReturn(newEncryptedPassword);
        when(encryptionService.encryptWithIv("new notes", mockKey))
                .thenReturn(newEncryptedNotes);
        when(entryRepository.save(any(PasswordEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(encryptionService.decryptWithIv("newCiphertext1", "newIv1", mockKey))
                .thenReturn("newpassword");
        when(encryptionService.decryptWithIv("newCiphertext2", "newIv2", mockKey))
                .thenReturn("new notes");

        PasswordEntryResponse result = entryService.updateEntry(1L, request, mockKey);

        assertNotNull(result);
        assertEquals("updated.com", result.getSiteName());
        verify(entryRepository).findById(1L);
        verify(entryRepository).save(sampleEntry);
    }

    @Test
    void updateEntry_withInvalidId_throwsEntryNotFoundException() {
        PasswordEntryRequest request = new PasswordEntryRequest();
        request.setSiteName("test.com");
        request.setUsername("user");
        request.setPassword("pass");

        when(entryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntryNotFoundException.class,
                () -> entryService.updateEntry(999L, request, mockKey));

        verify(entryRepository, never()).save(any());
    }

    @Test
    void deleteEntry_withValidId_deletesEntry() {
        when(entryRepository.existsById(1L)).thenReturn(true);

        entryService.deleteEntry(1L);

        verify(entryRepository).deleteById(1L);
    }

    @Test
    void deleteEntry_withInvalidId_throwsEntryNotFoundException() {
        when(entryRepository.existsById(999L)).thenReturn(false);

        assertThrows(EntryNotFoundException.class,
                () -> entryService.deleteEntry(999L));

        verify(entryRepository, never()).deleteById(anyLong());
    }

    @Test
    void getEntry_withValidId_returnsDecryptedEntry() throws Exception {
        when(entryRepository.findById(1L)).thenReturn(Optional.of(sampleEntry));
        when(encryptionService.decryptWithIv("encryptedPass", "iv123", mockKey))
                .thenReturn("decryptedPassword");
        when(encryptionService.decryptWithIv("encryptedNotes", "notesIv123", mockKey))
                .thenReturn("decrypted notes");

        PasswordEntryResponse result = entryService.getEntry(1L, mockKey);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("github.com", result.getSiteName());
        assertEquals("testuser", result.getUsername());
        assertEquals("decryptedPassword", result.getPassword());
        assertEquals("decrypted notes", result.getNotes());
    }

    @Test
    void getEntry_withInvalidId_throwsEntryNotFoundException() {
        when(entryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntryNotFoundException.class,
                () -> entryService.getEntry(999L, mockKey));
    }

    @Test
    void getAllEntries_returnsAllEntriesSortedBySiteName() throws Exception {
        PasswordEntry entry1 = new PasswordEntry();
        entry1.setId(1L);
        entry1.setSiteName("apple.com");
        entry1.setUsername("user1");
        entry1.setEncryptedPassword("enc1");
        entry1.setIv("iv1");

        PasswordEntry entry2 = new PasswordEntry();
        entry2.setId(2L);
        entry2.setSiteName("zebra.com");
        entry2.setUsername("user2");
        entry2.setEncryptedPassword("enc2");
        entry2.setIv("iv2");

        when(entryRepository.findAllByOrderBySiteNameAsc())
                .thenReturn(Arrays.asList(entry1, entry2));
        when(encryptionService.decryptWithIv("enc1", "iv1", mockKey))
                .thenReturn("pass1");
        when(encryptionService.decryptWithIv("enc2", "iv2", mockKey))
                .thenReturn("pass2");

        List<PasswordEntryResponse> results = entryService.getAllEntries(mockKey);

        assertEquals(2, results.size());
        assertEquals("apple.com", results.get(0).getSiteName());
        assertEquals("zebra.com", results.get(1).getSiteName());
    }

    @Test
    void searchEntries_returnsMatchingEntries() throws Exception {
        when(entryRepository.searchByQuery("github"))
                .thenReturn(Arrays.asList(sampleEntry));
        when(encryptionService.decryptWithIv("encryptedPass", "iv123", mockKey))
                .thenReturn("decryptedPassword");
        when(encryptionService.decryptWithIv("encryptedNotes", "notesIv123", mockKey))
                .thenReturn("decrypted notes");

        List<PasswordEntryResponse> results = entryService.searchEntries("github", mockKey);

        assertEquals(1, results.size());
        assertEquals("github.com", results.get(0).getSiteName());
        verify(entryRepository).searchByQuery("github");
    }
}
