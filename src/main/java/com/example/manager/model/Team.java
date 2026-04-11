package com.example.manager.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class Team {

    private static final AtomicLong ID_GEN = new AtomicLong(1);

    private final long id;
    private String name;
    private List<Player> squad = new ArrayList<>();
    private int budget; // simple integer budget

    public Team() {
        this.id = ID_GEN.getAndIncrement();
    }

    public Team(String name, int budget) {
        this.id = ID_GEN.getAndIncrement();
        this.name = name;
        this.budget = budget;
    }

    public long getId() {
        return id;
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
