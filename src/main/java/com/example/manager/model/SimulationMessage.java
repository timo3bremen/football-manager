package com.example.manager.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity für temporäre Nachrichten während der Live-Simulation
 * Diese werden nach der Simulation gelöscht
 */
@Entity
@Table(name = "simulation_messages")
public class SimulationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "matchId")
    private Long matchId;

    @Column(name = "teamId")
    private Long teamId;

    private String type; // goal, chance, yellow_card, red_card, injury, substitution, match_start, match_end, halftime

    @Column(name = "game_minute")
    private Integer gameMinute;

    private String teamName;

    private String playerName;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer homeGoals;

    private Integer awayGoals;

    private boolean isHomeTeam;

    @Column(name = "createdAt")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public SimulationMessage() {
    }

    public SimulationMessage(Long matchId, Long teamId, String type, Integer gameMinute, String teamName,
                            String playerName, String description, Integer homeGoals, Integer awayGoals,
                            boolean isHomeTeam) {
        this.matchId = matchId;
        this.teamId = teamId;
        this.type = type;
        this.gameMinute = gameMinute;
        this.teamName = teamName;
        this.playerName = playerName;
        this.description = description;
        this.homeGoals = homeGoals;
        this.awayGoals = awayGoals;
        this.isHomeTeam = isHomeTeam;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getMinute() {
        return gameMinute;
    }

    public void setMinute(Integer gameMinute) {
        this.gameMinute = gameMinute;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getHomeGoals() {
        return homeGoals;
    }

    public void setHomeGoals(Integer homeGoals) {
        this.homeGoals = homeGoals;
    }

    public Integer getAwayGoals() {
        return awayGoals;
    }

    public void setAwayGoals(Integer awayGoals) {
        this.awayGoals = awayGoals;
    }

    public boolean isHomeTeam() {
        return isHomeTeam;
    }

    public void setHomeTeam(boolean homeTeam) {
        isHomeTeam = homeTeam;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
