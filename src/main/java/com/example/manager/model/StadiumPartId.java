package com.example.manager.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite ID for StadiumPart entity.
 */
public class StadiumPartId implements Serializable {

    private Long teamId;
    private Integer partIndex;

    public StadiumPartId() {
    }

    public StadiumPartId(Long teamId, Integer partIndex) {
        this.teamId = teamId;
        this.partIndex = partIndex;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StadiumPartId that = (StadiumPartId) o;
        return Objects.equals(teamId, that.teamId) && Objects.equals(partIndex, that.partIndex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamId, partIndex);
    }
}
