package com.example.manager.model;

import jakarta.persistence.*;

/**
 * Speichert die Pokalrunden einer abgeschlossenen Saison
 */
@Entity
@Table(name = "cup_history")
public class CupHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "season")
    private int season;
    
    @Column(name = "country")
    private String country;
    
    @Column(name = "round")
    private int round;
    
    @Column(name = "home_team_id")
    private Long homeTeamId;
    
    @Column(name = "home_team_name")
    private String homeTeamName;
    
    @Column(name = "away_team_id")
    private Long awayTeamId;
    
    @Column(name = "away_team_name")
    private String awayTeamName;
    
    @Column(name = "home_goals")
    private int homeGoals;
    
    @Column(name = "away_goals")
    private int awayGoals;
    
    @Column(name = "winner_id")
    private Long winnerId;
    
    @Column(name = "winner_name")
    private String winnerName;
    
    @Column(name = "result_note")
    private String resultNote;
    
    public CupHistory() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getSeason() {
        return season;
    }

    public void setSeason(int season) {
        this.season = season;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public Long getHomeTeamId() {
        return homeTeamId;
    }

    public void setHomeTeamId(Long homeTeamId) {
        this.homeTeamId = homeTeamId;
    }

    public String getHomeTeamName() {
        return homeTeamName;
    }

    public void setHomeTeamName(String homeTeamName) {
        this.homeTeamName = homeTeamName;
    }

    public Long getAwayTeamId() {
        return awayTeamId;
    }

    public void setAwayTeamId(Long awayTeamId) {
        this.awayTeamId = awayTeamId;
    }

    public String getAwayTeamName() {
        return awayTeamName;
    }

    public void setAwayTeamName(String awayTeamName) {
        this.awayTeamName = awayTeamName;
    }

    public int getHomeGoals() {
        return homeGoals;
    }

    public void setHomeGoals(int homeGoals) {
        this.homeGoals = homeGoals;
    }

    public int getAwayGoals() {
        return awayGoals;
    }

    public void setAwayGoals(int awayGoals) {
        this.awayGoals = awayGoals;
    }

    public Long getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(Long winnerId) {
        this.winnerId = winnerId;
    }

    public String getWinnerName() {
        return winnerName;
    }

    public void setWinnerName(String winnerName) {
        this.winnerName = winnerName;
    }

    public String getResultNote() {
        return resultNote;
    }

    public void setResultNote(String resultNote) {
        this.resultNote = resultNote;
    }
}
