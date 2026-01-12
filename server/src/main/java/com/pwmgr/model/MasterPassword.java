package com.pwmgr.model;

import jakarta.persistence.*;

@Entity
@Table(name = "master_password")
public class MasterPassword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String verificationHash;

    @Column(nullable = false)
    private String salt;

    @Column(nullable = false)
    private int iterations = 600000;

    public MasterPassword() {}

    public MasterPassword(String verificationHash, String salt) {
        this.verificationHash = verificationHash;
        this.salt = salt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVerificationHash() {
        return verificationHash;
    }

    public void setVerificationHash(String verificationHash) {
        this.verificationHash = verificationHash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }
}
