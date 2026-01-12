package com.pwmgr.repository;

import com.pwmgr.model.PasswordEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PasswordEntryRepository extends JpaRepository<PasswordEntry, Long> {

    @Query("SELECT p FROM PasswordEntry p WHERE " +
           "LOWER(p.siteName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.username) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<PasswordEntry> searchByQuery(@Param("query") String query);

    List<PasswordEntry> findAllByOrderBySiteNameAsc();
}
