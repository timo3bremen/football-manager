package com.example.manager.model;

import jakarta.persistence.*;

/**
 * Represents a single slot in a team's lineup for a specific formation.
 */
@Entity
@Table(name = "lineups")
@IdClass(LineupSlotId.class)
public class LineupSlot {

    @Id
    @Column(name = "teamId")
    private Long teamId;

    @Id
    @Column(name = "formationId")
    private String formationId;

    @Id
    @Column(name = "slotIndex")
    private Integer slotIndex;

    @Column(name = "slotName")
    private String slotName;

    @Column(name = "playerId")
    private Long playerId;

    public LineupSlot() {
    }

    public LineupSlot(Long teamId, String formationId, Integer slotIndex, String slotName, Long playerId) {
        this.teamId = teamId;
        this.formationId = formationId;
        this.slotIndex = slotIndex;
        this.slotName = slotName;
        this.playerId = playerId;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getFormationId() {
        return formationId;
    }

    public void setFormationId(String formationId) {
        this.formationId = formationId;
    }

    public Integer getSlotIndex() {
        return slotIndex;
    }

    public void setSlotIndex(Integer slotIndex) {
        this.slotIndex = slotIndex;
    }

    public String getSlotName() {
        return slotName;
    }

    public void setSlotName(String slotName) {
        this.slotName = slotName;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }
}
