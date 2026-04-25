package com.example.manager.model;

import jakarta.persistence.*;

@Entity
@Table(name = "sponsors")
public class Sponsor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "team_id", nullable = false)
    private Long teamId;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "appearance_payout")
    private int appearancePayout; // Antritt (pro Spiel)
    
    @Column(name = "win_payout")
    private int winPayout; // Sieg (pro Sieg)
    
    @Column(name = "survive_payout")
    private int survivePayout; // Klassenerhalt (Saison-Ende, Platz 1-8)
    
    @Column(name = "title_payout")
    private int titlePayout; // Titel (Saison-Ende, Platz 1-2)

    public Sponsor() {
    }

    public Sponsor(Long teamId, String name, int appearancePayout, int winPayout, int survivePayout, int titlePayout) {
        this.teamId = teamId;
        this.name = name;
        this.appearancePayout = appearancePayout;
        this.winPayout = winPayout;
        this.survivePayout = survivePayout;
        this.titlePayout = titlePayout;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAppearancePayout() {
        return appearancePayout;
    }

    public void setAppearancePayout(int appearancePayout) {
        this.appearancePayout = appearancePayout;
    }

    public int getWinPayout() {
        return winPayout;
    }

    public void setWinPayout(int winPayout) {
        this.winPayout = winPayout;
    }

    public int getSurvivePayout() {
        return survivePayout;
    }

    public void setSurvivePayout(int survivePayout) {
        this.survivePayout = survivePayout;
    }

    public int getTitlePayout() {
        return titlePayout;
    }

    public void setTitlePayout(int titlePayout) {
        this.titlePayout = titlePayout;
    }
}
