package com.example.manager.dto;

import java.time.LocalDateTime;

/**
 * DTO for Stadium Build status
 */
public class StadiumBuildDTO {
    
    private Long id;
    private Long teamId;
    private Integer totalSeats;
    private String seatType;
    private Long cost;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean completed;
    private Long timeRemainingMs; // calculated at time of response

    public StadiumBuildDTO() {
    }

    public StadiumBuildDTO(Long id, Long teamId, Integer totalSeats, String seatType, 
                          Long cost, LocalDateTime startTime, LocalDateTime endTime, Boolean completed) {
        this.id = id;
        this.teamId = teamId;
        this.totalSeats = totalSeats;
        this.seatType = seatType;
        this.cost = cost;
        this.startTime = startTime;
        this.endTime = endTime;
        this.completed = completed;
        
        // Calculate time remaining
        if (!completed && endTime != null) {
            this.timeRemainingMs = Math.max(0, endTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() 
                    - System.currentTimeMillis());
        } else {
            this.timeRemainingMs = 0L;
        }
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

    public Long getTimeRemainingMs() {
        return timeRemainingMs;
    }

    public void setTimeRemainingMs(Long timeRemainingMs) {
        this.timeRemainingMs = timeRemainingMs;
    }
}
