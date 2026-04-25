package com.example.manager.dto;

/**
 * DTO für gespeicherte Simulationsnachrichten
 */
public class SimulationMessageDTO {
    
    private Long id;
    private Long matchId;
    private Long teamId;
    private String type;
    private Integer minute;
    private String teamName;
    private String playerName;
    private String description;
    private Integer homeGoals;
    private Integer awayGoals;
    private boolean isHomeTeam;
    
    public SimulationMessageDTO() {
    }
    
    public SimulationMessageDTO(Long matchId, Long teamId, String type, Integer minute, 
                               String teamName, String playerName, String description,
                               Integer homeGoals, Integer awayGoals, boolean isHomeTeam) {
        this.matchId = matchId;
        this.teamId = teamId;
        this.type = type;
        this.minute = minute;
        this.teamName = teamName;
        this.playerName = playerName;
        this.description = description;
        this.homeGoals = homeGoals;
        this.awayGoals = awayGoals;
        this.isHomeTeam = isHomeTeam;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getMatchId() {
        return matchId;
    }
    
    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }
    
    public Long getTeamId() {
        return teamId;
    }
    
    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Integer getMinute() {
        return minute;
    }
    
    public void setMinute(Integer minute) {
        this.minute = minute;
    }
    
    public String getTeamName() {
        return teamName;
    }
    
    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Integer getHomeGoals() {
        return homeGoals;
    }
    
    public void setHomeGoals(Integer homeGoals) {
        this.homeGoals = homeGoals;
    }
    
    public Integer getAwayGoals() {
        return awayGoals;
    }
    
    public void setAwayGoals(Integer awayGoals) {
        this.awayGoals = awayGoals;
    }
    
    public boolean isHomeTeam() {
        return isHomeTeam;
    }
    
    public void setHomeTeam(boolean homeTeam) {
        isHomeTeam = homeTeam;
    }
}
