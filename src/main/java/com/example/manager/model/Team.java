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
    
    @Column(name = "budget")
    private long budget; // Budget in Euro
    
    @Column(name = "is_cpu", nullable = false)
    private boolean isCPU = true; // Flag ob das Team ein CPU-Team ist (default: true)

    @Column(name = "active_formation", length = 10)
    private String activeFormation = "4-4-2"; // Aktive Formation (default: 4-4-2)

    @Column(name = "stadium_capacity_standing", nullable = false)
    private Long stadiumCapacityStanding = 1000L; // Stehplätze (initial 1000)

    @Column(name = "stadium_capacity_seated", nullable = false)
    private Long stadiumCapacitySeated = 0L; // Sitzplätze (initial 0)

    @Column(name = "stadium_capacity_vip", nullable = false)
    private Long stadiumCapacityVip = 0L; // VIP-Plätze (initial 0)

    @Column(name = "fan_satisfaction", nullable = false)
    private int fanSatisfaction = 75; // Fanfreundschaft in % (initial 75%)

    @Column(name = "ticket_price_standing", nullable = false)
    private int ticketPriceStanding = 20; // Preis für Stehplätze in € (Standard: 20€)

    @Column(name = "ticket_price_seated", nullable = false)
    private int ticketPriceSeated = 40; // Preis für Sitzplätze in € (Standard: 40€)

    @Column(name = "ticket_price_vip", nullable = false)
    private int ticketPriceVip = 80; // Preis für VIP-Plätze in € (Standard: 80€)

    public Team() {
    }

    public Team(String name, long budget) {
        this.name = name;
        this.budget = budget;
        this.isCPU = true; // CPU-Teams per default
    }

    /**
     * Construct a team with an explicit id (used when loading from DB).
     */
    public Team(long id, String name, long budget) {
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
        return (int) budget;
    }

    public void setBudget(long budget) {
        this.budget = budget;
    }
    
    public long getBudgetAsLong() {
        return budget;
    }

    public void setBudgetAsLong(long budget) {
        this.budget = budget;
    }

    public boolean isCPU() {
        return isCPU;
    }

    public void setCPU(boolean isCPU) {
        this.isCPU = isCPU;
    }

    public String getActiveFormation() {
        return activeFormation;
    }

    public void setActiveFormation(String activeFormation) {
        this.activeFormation = activeFormation != null ? activeFormation : "4-4-2";
    }

    public void addPlayer(Player p) {
        this.squad.add(p);
    }

    public void removePlayer(Player p) {
        this.squad.removeIf(x -> x.getId() == p.getId());
    }

    public Long getStadiumCapacityStanding() {
        return stadiumCapacityStanding;
    }

    public void setStadiumCapacityStanding(Long stadiumCapacityStanding) {
        this.stadiumCapacityStanding = stadiumCapacityStanding != null ? stadiumCapacityStanding : 0L;
    }

    public Long getStadiumCapacitySeated() {
        return stadiumCapacitySeated;
    }

    public void setStadiumCapacitySeated(Long stadiumCapacitySeated) {
        this.stadiumCapacitySeated = stadiumCapacitySeated != null ? stadiumCapacitySeated : 0L;
    }

    public Long getStadiumCapacityVip() {
        return stadiumCapacityVip;
    }

    public void setStadiumCapacityVip(Long stadiumCapacityVip) {
        this.stadiumCapacityVip = stadiumCapacityVip != null ? stadiumCapacityVip : 0L;
    }

    // Backward compatibility - returns total capacity
    public Long getStadiumCapacity() {
        return (stadiumCapacityStanding != null ? stadiumCapacityStanding : 0L) +
               (stadiumCapacitySeated != null ? stadiumCapacitySeated : 0L) +
               (stadiumCapacityVip != null ? stadiumCapacityVip : 0L);
    }

    public void setStadiumCapacity(Long stadiumCapacity) {
        // Backward compatibility - set all to standing
        this.stadiumCapacityStanding = stadiumCapacity;
    }

    public int getFanSatisfaction() {
        return fanSatisfaction;
    }

    public void setFanSatisfaction(int fanSatisfaction) {
        this.fanSatisfaction = Math.max(0, Math.min(100, fanSatisfaction)); // Clamp 0-100
    }

    public int getTicketPriceStanding() {
        return ticketPriceStanding;
    }

    public void setTicketPriceStanding(int ticketPriceStanding) {
        this.ticketPriceStanding = Math.max(0, Math.min(60, ticketPriceStanding)); // Max 60€
    }

    public int getTicketPriceSeated() {
        return ticketPriceSeated;
    }

    public void setTicketPriceSeated(int ticketPriceSeated) {
        this.ticketPriceSeated = Math.max(0, Math.min(100, ticketPriceSeated)); // Max 100€
    }

    public int getTicketPriceVip() {
        return ticketPriceVip;
    }

    public void setTicketPriceVip(int ticketPriceVip) {
        this.ticketPriceVip = Math.max(0, Math.min(300, ticketPriceVip)); // Max 300€
    }
}
