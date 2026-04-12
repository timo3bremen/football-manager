package com.example.manager.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a lineup/formation for a team.
 * slotIndex -> playerId mapping (1..17)
 */
public class Lineup {

    private String formationId; // e.g. "4-4-2" or UUID
    private final Map<Integer, Long> slots = new ConcurrentHashMap<>();

    public Lineup(){}

    public Lineup(String formationId){
        this.formationId = formationId;
    }

    public String getFormationId() {
        return formationId;
    }

    public void setFormationId(String formationId) {
        this.formationId = formationId;
    }

    /**
     * get player id assigned to slot index (1..17), or null
     */
    public Long getPlayerAt(int slotIndex){
        return slots.get(slotIndex);
    }

    public void setPlayerAt(int slotIndex, Long playerId){
        if (slotIndex < 1) throw new IllegalArgumentException("slotIndex must be >= 1");
        slots.put(slotIndex, playerId);
    }

    public Map<Integer, Long> getSlots() {
        return slots;
    }

    @Override
    public String toString(){
        return "Lineup{" + "formationId='" + formationId + '\'' + ", slots=" + slots + '}';
    }
}
