package com.example.manager.model;

import jakarta.persistence.*;

/**
 * Represents a stadium part for a team.
 */
@Entity
@Table(name = "stadium_parts")
@IdClass(StadiumPartId.class)
public class StadiumPart {

    @Id
    @Column(name = "teamId")
    private Long teamId;

    @Id
    @Column(name = "partIndex")
    private Integer partIndex;

    @Column(name = "built")
    private Boolean built = false;

    @Column(name = "type")
    private String type; // 'standing', 'seated', 'vip'

    public StadiumPart() {
    }

    public StadiumPart(Long teamId, Integer partIndex, Boolean built, String type) {
        this.teamId = teamId;
        this.partIndex = partIndex;
        this.built = built;
        this.type = type;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public Integer getPartIndex() {
        return partIndex;
    }

    public void setPartIndex(Integer partIndex) {
        this.partIndex = partIndex;
    }

    public Boolean getBuilt() {
        return built;
    }

    public void setBuilt(Boolean built) {
        this.built = built;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
