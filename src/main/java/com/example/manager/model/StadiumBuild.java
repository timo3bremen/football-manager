package com.example.manager.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents an ongoing or completed stadium construction project
 */
@Entity
@Table(name = "stadium_builds")
public class StadiumBuild {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @Column(name = "seat_type", nullable = false)
    private String seatType; // 'standing', 'seated', 'vip'

    @Column(name = "cost", nullable = false)
    private Long cost;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "completed", nullable = false)
    private Boolean completed = false;

    public StadiumBuild() {
    }

    public StadiumBuild(Long teamId, Integer totalSeats, String seatType, Long cost, 
                       LocalDateTime startTime, LocalDateTime endTime) {
        this.teamId = teamId;
        this.totalSeats = totalSeats;
        this.seatType = seatType;
        this.cost = cost;
        this.startTime = startTime;
        this.endTime = endTime;
        this.completed = false;
    }

    // Getters and Setters
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

    public Integer getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }

    public Long getCost() {
        return cost;
    }

    public void setCost(Long cost) {
        this.cost = cost;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }
}
