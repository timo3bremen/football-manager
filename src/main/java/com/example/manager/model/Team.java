package com.example.manager.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.*;

@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    @Transient
    private List<Player> squad = new ArrayList<>();
    
    @Transient
    private List<Lineup> lineups = new ArrayList<>();
    
    private int budget; // simple integer budget

    public Team() {
    }

    public Team(String name, int budget) {
        this.name = name;
        this.budget = budget;
    }

    /**
     * Construct a team with an explicit id (used when loading from DB).
     */
    public Team(long id, String name, int budget) {
        this.id = id;
        this.name = name;
        this.budget = budget;
    }

    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Player> getSquad() {
        return squad;
    }

    public void setSquad(List<Player> squad) {
        this.squad = squad;
    }

    public List<Lineup> getLineups() {
        return lineups;
    }

    public void setLineups(List<Lineup> lineups) {
        this.lineups = lineups;
    }

    public Optional<Lineup> getLineupByFormation(String formationId){
        if (formationId == null) return Optional.empty();
        return lineups.stream().filter(l -> formationId.equals(l.getFormationId())).findFirst();
    }

    public void addOrReplaceLineup(Lineup lineup){
        if (lineup == null) return;
        this.lineups.removeIf(l -> l.getFormationId() != null && l.getFormationId().equals(lineup.getFormationId()));
        this.lineups.add(lineup);
    }

    public int getBudget() {
        return budget;
    }

    public void setBudget(int budget) {
        this.budget = budget;
    }

    public void addPlayer(Player p) {
        this.squad.add(p);
    }

    public void removePlayer(Player p) {
        this.squad.removeIf(x -> x.getId() == p.getId());
    }
}
