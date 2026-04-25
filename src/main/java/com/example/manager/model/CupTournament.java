package com.example.manager.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;

/**
 * Represents a Cup/Pokal tournament for a country.
 * One tournament per country per season.
 */
@Entity
@Table(name = "cup_tournaments")
public class CupTournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "country", nullable = false)
    private String country;

    @Column(name = "season", nullable = false)
    private int season;

    @Column(name = "currentRound", nullable = false)
    private int currentRound; // 1-6 (6 Runden bis zum Finale)

    @Column(name = "status", nullable = false)
    private String status; // active, completed

    @Column(name = "winnerTeamId")
    private Long winnerTeamId;

    @Column(name = "winnerTeamName")
    private String winnerTeamName;

    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;

    @Column(name = "completedAt")
    private Instant completedAt;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CupMatch> matches;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CupTeam> teams;

    public CupTournament() {
    }

    public CupTournament(String country, int season) {
        this.country = country;
        this.season = season;
        this.currentRound = 0;
        this.status = "active";
        this.createdAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public int getSeason() {
        return season;
    }

    public void setSeason(int season) {
        this.season = season;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getWinnerTeamId() {
        return winnerTeamId;
    }

    public void setWinnerTeamId(Long winnerTeamId) {
        this.winnerTeamId = winnerTeamId;
    }

    public String getWinnerTeamName() {
        return winnerTeamName;
    }

    public void setWinnerTeamName(String winnerTeamName) {
        this.winnerTeamName = winnerTeamName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public List<CupMatch> getMatches() {
        return matches;
    }

    public void setMatches(List<CupMatch> matches) {
        this.matches = matches;
    }

    public List<CupTeam> getTeams() {
        return teams;
    }

    public void setTeams(List<CupTeam> teams) {
        this.teams = teams;
    }
}
