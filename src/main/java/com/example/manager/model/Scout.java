package com.example.manager.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "scouts")
public class Scout {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "team_id")
    private Long teamId;
    
    @Column(name = "region")
    private String region; // Z.B. "WestEuropa", "SüdostAsien", etc.
    
    @Column(name = "days_remaining")
    private int daysRemaining; // Verbleibende Tage
    
    @Column(name = "started_at")
    private Instant startedAt; // Wann wurde der Scout gestartet
    
    @Column(name = "last_player_generated_at")
    private Instant lastPlayerGeneratedAt; // Wann wurde der letzte Jugenspieler generiert
    
    @Column(name = "is_active")
    private boolean isActive = true;
    
    public Scout() {
    }
    
    public Scout(Long teamId, String region, int daysRemaining) {
        this.teamId = teamId;
        this.region = region;
        this.daysRemaining = daysRemaining;
        this.startedAt = Instant.now();
        this.isActive = true;
    }
    
    // Getter und Setter
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
    
    public String getRegion() {
        return region;
    }
    
    public void setRegion(String region) {
        this.region = region;
    }
    
    public int getDaysRemaining() {
        return daysRemaining;
    }
    
    public void setDaysRemaining(int daysRemaining) {
        this.daysRemaining = daysRemaining;
    }
    
    public Instant getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }
    
    public Instant getLastPlayerGeneratedAt() {
        return lastPlayerGeneratedAt;
    }
    
    public void setLastPlayerGeneratedAt(Instant lastPlayerGeneratedAt) {
        this.lastPlayerGeneratedAt = lastPlayerGeneratedAt;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
}
