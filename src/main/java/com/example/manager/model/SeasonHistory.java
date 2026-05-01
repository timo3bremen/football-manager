package com.example.manager.model;

import jakarta.persistence.*;

/**
 * Speichert die Ligaplatzierungen einer abgeschlossenen Saison
 */
@Entity
@Table(name = "season_history")
public class SeasonHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "season")
    private int season;
    
    @Column(name = "league_id")
    private Long leagueId;
    
    @Column(name = "league_name")
    private String leagueName;
    
    @Column(name = "country")
    private String country;
    
    @Column(name = "division")
    private int division;
    
    @Column(name = "team_id")
    private Long teamId;
    
    @Column(name = "team_name")
    private String teamName;
    
    @Column(name = "position")
    private int position;
    
    @Column(name = "points")
    private int points;
    
    @Column(name = "goals_for")
    private int goalsFor;
    
    @Column(name = "goals_against")
    private int goalsAgainst;
    
    @Column(name = "wins")
    private int wins;
    
    @Column(name = "draws")
    private int draws;
    
    @Column(name = "losses")
    private int losses;
    
    public SeasonHistory() {
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

    public Long getLeagueId() {
        return leagueId;
    }

    public void setLeagueId(Long leagueId) {
        this.leagueId = leagueId;
    }

    public String getLeagueName() {
        return leagueName;
    }

    public void setLeagueName(String leagueName) {
        this.leagueName = leagueName;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public int getDivision() {
        return division;
    }

    public void setDivision(int division) {
        this.division = division;
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

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getGoalsFor() {
        return goalsFor;
    }

    public void setGoalsFor(int goalsFor) {
        this.goalsFor = goalsFor;
    }

    public int getGoalsAgainst() {
        return goalsAgainst;
    }

    public void setGoalsAgainst(int goalsAgainst) {
        this.goalsAgainst = goalsAgainst;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getDraws() {
        return draws;
    }

    public void setDraws(int draws) {
        this.draws = draws;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }
}
