package com.example.manager.model;

import jakarta.persistence.*;

/**
 * Simple player model for the manager game.
 * Attributes are intentionally minimal to be expanded later.
 */
@Entity
@Table(name = "players")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "teamId")
    private Long teamId;
    
    private String name;
    
    // rating 10-100
    private int rating;
    
    // potential growth 0-100
    private int potential;
    
    // current form modifier -10 .. +10
    private int form;
    
    // player position: GK, DEF, MID, FWD
    private String position;

    public Player() {
    }

    public Player(String name, int rating, int potential, int form) {
        this.name = name;
        this.rating = rating;
        this.potential = potential;
        this.form = form;
    }

    /** create player with explicit id (used when loading from DB) */
    public Player(long id, String name, int rating, int potential, int form){
        this.id = id;
        this.name = name;
        this.rating = rating;
        this.potential = potential;
        this.form = form;
    }
    
    public Player(String name, int rating, int potential, int form, String position) {
        this.name = name;
        this.rating = rating;
        this.potential = potential;
        this.form = form;
        this.position = position;
    }
    
    public Player(long id, String name, int rating, int potential, int form, String position) {
        this.id = id;
        this.name = name;
        this.rating = rating;
        this.potential = potential;
        this.form = form;
        this.position = position;
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

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public int getPotential() {
        return potential;
    }

    public void setPotential(int potential) {
        this.potential = potential;
    }

    public int getForm() {
        return form;
    }

    public void setForm(int form) {
        this.form = form;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return "Player{" + "id=" + id + ", name='" + name + '\'' + ", rating=" + rating + ", potential=" + potential + ", form=" + form + ", position='" + position + '\'' + '}';
    }
}
