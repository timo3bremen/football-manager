package com.example.manager.model;

import jakarta.persistence.*;

/**
 * Represents a team in a cup tournament.
 */
@Entity
@Table(name = "cup_teams")
public class CupTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private CupTournament tournament;

    @Column(name = "teamId", nullable = false)
    private Long teamId;

    @Column(name = "teamName", nullable = false)
    private String teamName;

    @Column(name = "isActive", nullable = false)
    private boolean isActive; // false wenn ausgeschieden

    @Column(name = "eliminatedInRound")
    private Integer eliminatedInRound; // In welcher Runde ausgeschieden

    public CupTeam() {
    }

    public CupTeam(CupTournament tournament, Long teamId, String teamName) {
        this.tournament = tournament;
        this.teamId = teamId;
        this.teamName = teamName;
        this.isActive = true;
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

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Integer getEliminatedInRound() {
        return eliminatedInRound;
    }

    public void setEliminatedInRound(Integer eliminatedInRound) {
        this.eliminatedInRound = eliminatedInRound;
    }
}
