package com.pwmgr.repository;

import com.pwmgr.model.MasterPassword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MasterPasswordRepository extends JpaRepository<MasterPassword, Long> {
    Optional<MasterPassword> findFirstByOrderByIdAsc();
}
