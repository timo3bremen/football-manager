package com.example.manager.model;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple player model for the manager game.
 * Attributes are intentionally minimal to be expanded later.
 */
public class Player {

    private static final AtomicLong ID_GEN = new AtomicLong(1);

    private final long id;
    private String name;
    // rating 10-100
    private int rating;
    // potential growth 0-100
    private int potential;
    // current form modifier -10 .. +10
    private int form;

    public Player() {
        this.id = ID_GEN.getAndIncrement();
    }

    public Player(String name, int rating, int potential, int form) {
        this.id = ID_GEN.getAndIncrement();
        this.name = name;
        this.rating = rating;
        this.potential = potential;
        this.form = form;
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

    @Override
    public String toString() {
        return "Player{" + "id=" + id + ", name='" + name + '\'' + ", rating=" + rating + ", potential=" + potential + ", form=" + form + '}';
    }
}
