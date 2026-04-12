package com.example.manager.model;

import jakarta.persistence.*;

/**
 * User entity for authentication.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "username")
    private String username;

    @Column(name = "passwordHash")
    private String passwordHash;

    @Column(name = "teamId")
    private Long teamId;

    public User() {
    }

    public User(String username, String passwordHash, Long teamId) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.teamId = teamId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }
}
