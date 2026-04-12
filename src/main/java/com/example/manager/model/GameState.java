package com.example.manager.model;

import jakarta.persistence.*;

/**
 * Stores game state as JSON blob per team.
 */
@Entity
@Table(name = "gamestates")
public class GameState {

    @Id
    @Column(name = "teamId")
    private Long teamId;

    @Column(name = "json", columnDefinition = "CLOB")
    private String json;

    public GameState() {
    }

    public GameState(Long teamId, String json) {
        this.teamId = teamId;
        this.json = json;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }
}
