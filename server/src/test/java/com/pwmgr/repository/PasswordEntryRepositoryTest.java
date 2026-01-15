package com.pwmgr.repository;

import com.pwmgr.model.PasswordEntry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class PasswordEntryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PasswordEntryRepository repository;

    @Test
    void searchByQuery_matchesSiteName() {
        PasswordEntry entry1 = createEntry("github.com", "user1", "pass1", "iv1");
        PasswordEntry entry2 = createEntry("gitlab.com", "user2", "pass2", "iv2");
        PasswordEntry entry3 = createEntry("example.com", "user3", "pass3", "iv3");

        entityManager.persist(entry1);
        entityManager.persist(entry2);
        entityManager.persist(entry3);
        entityManager.flush();

        List<PasswordEntry> results = repository.searchByQuery("git");

        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(e -> e.getSiteName().equals("github.com")));
        assertTrue(results.stream().anyMatch(e -> e.getSiteName().equals("gitlab.com")));
    }

    @Test
    void searchByQuery_matchesUsername() {
        PasswordEntry entry1 = createEntry("site1.com", "alice@example.com", "pass1", "iv1");
        PasswordEntry entry2 = createEntry("site2.com", "bob@example.com", "pass2", "iv2");
        PasswordEntry entry3 = createEntry("site3.com", "alice123", "pass3", "iv3");

        entityManager.persist(entry1);
        entityManager.persist(entry2);
        entityManager.persist(entry3);
        entityManager.flush();

        List<PasswordEntry> results = repository.searchByQuery("alice");

        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(e -> e.getUsername().equals("alice@example.com")));
        assertTrue(results.stream().anyMatch(e -> e.getUsername().equals("alice123")));
    }

    @Test
    void searchByQuery_isCaseInsensitive() {
        PasswordEntry entry = createEntry("GitHub.com", "User@Example.COM", "pass", "iv");

        entityManager.persist(entry);
        entityManager.flush();

        List<PasswordEntry> results1 = repository.searchByQuery("github");
        List<PasswordEntry> results2 = repository.searchByQuery("GITHUB");
        List<PasswordEntry> results3 = repository.searchByQuery("user");

        assertEquals(1, results1.size());
        assertEquals(1, results2.size());
        assertEquals(1, results3.size());
    }

    @Test
    void searchByQuery_withNoMatches_returnsEmptyList() {
        PasswordEntry entry = createEntry("github.com", "user", "pass", "iv");

        entityManager.persist(entry);
        entityManager.flush();

        List<PasswordEntry> results = repository.searchByQuery("nonexistent");

        assertTrue(results.isEmpty());
    }

    @Test
    void findAllByOrderBySiteNameAsc_sortsByName() {
        PasswordEntry entry1 = createEntry("zebra.com", "user1", "pass1", "iv1");
        PasswordEntry entry2 = createEntry("apple.com", "user2", "pass2", "iv2");
        PasswordEntry entry3 = createEntry("middle.com", "user3", "pass3", "iv3");

        entityManager.persist(entry1);
        entityManager.persist(entry2);
        entityManager.persist(entry3);
        entityManager.flush();

        List<PasswordEntry> results = repository.findAllByOrderBySiteNameAsc();

        assertEquals(3, results.size());
        assertEquals("apple.com", results.get(0).getSiteName());
        assertEquals("middle.com", results.get(1).getSiteName());
        assertEquals("zebra.com", results.get(2).getSiteName());
    }

    @Test
    void save_setsCreatedAtAndUpdatedAt() {
        PasswordEntry entry = createEntry("test.com", "user", "pass", "iv");

        PasswordEntry saved = repository.save(entry);
        entityManager.flush();

        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals(saved.getCreatedAt(), saved.getUpdatedAt());
    }

    @Test
    void update_updatesOnlyUpdatedAt() throws InterruptedException {
        PasswordEntry entry = createEntry("test.com", "user", "pass", "iv");

        PasswordEntry saved = repository.save(entry);
        entityManager.flush();
        entityManager.clear();

        Thread.sleep(10); // Ensure time difference

        PasswordEntry found = repository.findById(saved.getId()).orElseThrow();
        found.setSiteName("updated.com");

        repository.save(found);
        entityManager.flush();

        PasswordEntry updated = repository.findById(saved.getId()).orElseThrow();

        // Compare timestamps truncated to milliseconds to avoid precision issues
        assertEquals(
            saved.getCreatedAt().truncatedTo(java.time.temporal.ChronoUnit.MILLIS),
            updated.getCreatedAt().truncatedTo(java.time.temporal.ChronoUnit.MILLIS)
        );
        assertTrue(updated.getUpdatedAt().isAfter(updated.getCreatedAt()));
    }

    private PasswordEntry createEntry(String siteName, String username, String encryptedPassword, String iv) {
        PasswordEntry entry = new PasswordEntry();
        entry.setSiteName(siteName);
        entry.setUsername(username);
        entry.setEncryptedPassword(encryptedPassword);
        entry.setIv(iv);
        return entry;
    }
}
