package com.example.manager.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite ID class for LineupSlot entity.
 */
public class LineupSlotId implements Serializable {

    private Long teamId;
    private String formationId;
    private Integer slotIndex;

    public LineupSlotId() {
    }

    public LineupSlotId(Long teamId, String formationId, Integer slotIndex) {
        this.teamId = teamId;
        this.formationId = formationId;
        this.slotIndex = slotIndex;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LineupSlotId that = (LineupSlotId) o;
        return Objects.equals(teamId, that.teamId) &&
               Objects.equals(formationId, that.formationId) &&
               Objects.equals(slotIndex, that.slotIndex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamId, formationId, slotIndex);
    }
}
