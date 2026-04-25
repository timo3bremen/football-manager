package com.example.manager.model;

import jakarta.persistence.*;

/**
 * Represents a match in the cup tournament.
 */
@Entity
@Table(name = "cup_matches")
public class CupMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private CupTournament tournament;

    @Column(name = "round", nullable = false)
    private int round; // 1-6

    @Column(name = "homeTeamId", nullable = false)
    private Long homeTeamId;

    @Column(name = "homeTeamName", nullable = false)
    private String homeTeamName;

    @Column(name = "awayTeamId", nullable = false)
    private Long awayTeamId;

    @Column(name = "awayTeamName", nullable = false)
    private String awayTeamName;

    @Column(name = "homeGoals")
    private Integer homeGoals;

    @Column(name = "awayGoals")
    private Integer awayGoals;

    @Column(name = "winnerId")
    private Long winnerId;

    @Column(name = "winnerName")
    private String winnerName;

    @Column(name = "status", nullable = false)
    private String status; // scheduled, completed

    @Column(name = "resultNote")
    private String resultNote; // i.V. (Verlängerung) oder i.E. (Elfmeterschießen)

    public CupMatch() {
    }

    public CupMatch(CupTournament tournament, int round, 
                   Long homeTeamId, String homeTeamName,
                   Long awayTeamId, String awayTeamName) {
        this.tournament = tournament;
        this.round = round;
        this.homeTeamId = homeTeamId;
        this.homeTeamName = homeTeamName;
        this.awayTeamId = awayTeamId;
        this.awayTeamName = awayTeamName;
        this.status = "scheduled";
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CupTournament getTournament() {
        return tournament;
    }

    public void setTournament(CupTournament tournament) {
        this.tournament = tournament;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResultNote() {
        return resultNote;
    }

    public void setResultNote(String resultNote) {
        this.resultNote = resultNote;
    }
}
