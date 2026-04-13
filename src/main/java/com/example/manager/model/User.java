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
    
    @Column(name = "leagueId")
    private Long leagueId; // Die Liga, der der User angehört

    public User() {
    }

    public User(String username, String passwordHash, Long teamId) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.teamId = teamId;
    }
    
    public User(String username, String passwordHash, Long teamId, Long leagueId) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.teamId = teamId;
        this.leagueId = leagueId;
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
    
    public Long getLeagueId() {
        return leagueId;
    }

    public void setLeagueId(Long leagueId) {
        this.leagueId = leagueId;
    }
}
